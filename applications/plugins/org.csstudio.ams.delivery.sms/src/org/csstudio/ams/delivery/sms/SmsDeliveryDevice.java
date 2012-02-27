
/*
 * Copyright (c) 2011 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 *
 * $Id: DesyKrykCodeTemplates.xml,v 1.7 2010/04/20 11:43:22 bknerr Exp $
 */

package org.csstudio.ams.delivery.sms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import org.csstudio.ams.AmsActivator;
import org.csstudio.ams.delivery.device.DeviceListener;
import org.csstudio.ams.delivery.device.DeviceObject;
import org.csstudio.ams.delivery.device.IDeliveryDevice;
import org.csstudio.ams.delivery.device.IReadableDevice;
import org.csstudio.ams.delivery.message.BaseAlarmMessage.State;
import org.csstudio.ams.delivery.service.Environment;
import org.csstudio.ams.delivery.sms.internal.SmsConnectorPreferenceKey;
import org.csstudio.ams.delivery.util.jms.JmsProperties;
import org.csstudio.ams.delivery.util.jms.JmsSender;
import org.csstudio.ams.internal.AmsPreferenceKey;
import org.csstudio.platform.utility.jms.JmsSimpleProducer;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smslib.AGateway;
import org.smslib.IInboundMessageNotification;
import org.smslib.InboundBinaryMessage;
import org.smslib.InboundMessage;
import org.smslib.InboundMessage.MessageClasses;
import org.smslib.OutboundMessage;
import org.smslib.Service;
import org.smslib.Message.MessageEncodings;
import org.smslib.Message.MessageTypes;
import org.smslib.modem.SerialModemGateway;

/**
 * @author mmoeller
 * @version 1.0
 * @since 18.12.2011
 */
public class SmsDeliveryDevice implements IDeliveryDevice<SmsAlarmMessage>,
                                          IReadableDevice<InboundMessage>,
                                          IInboundMessageNotification {

    /** The static class logger */
    private static final Logger LOG = LoggerFactory.getLogger(SmsDeliveryDevice.class);
    
    static Logger getLogger() {
        return LOG;
    }
    
    private static final int MAX_MODEM_NUMBER = 3;
    
    /** Text for the test SMS */
    private static final String SMS_TEST_TEXT = "[MODEMTEST{$CHECKID,$GATEWAYID}]";

    private JmsProperties jmsProps;
    
    private Service modemService;
    
    /** This class contains all modem ids (names) */
    private ModemInfoContainer modemInfo;
    
    /** This listener is informed if a inbound message has been receibved. */
    private ArrayList<DeviceListener> listener;
   
    /** Status information of the current modem test */
    private final ModemTestStatus testStatus;

    /** Reading period (in ms) for the modem */
    private long readWaitingPeriod;

    public SmsDeliveryDevice(ModemInfoContainer deviceInfo, JmsProperties jms, long readInterval) {
        modemService = null;
        modemInfo = deviceInfo;
        jmsProps = jms;
        listener = (ArrayList<DeviceListener>) Collections.synchronizedList(new ArrayList<DeviceListener>());
        testStatus = new ModemTestStatus();
        readWaitingPeriod = readInterval;
        initModem();
    }
    
    public void addDeviceListener(DeviceListener l) {
        listener.add(l);
    }
    
    public void removeDeviceListener(DeviceListener l) {
        listener.remove(l);
    }
    
    protected Service getDeviceService() {
        return modemService;
    }
    
    @Override
    public void process(final AGateway gateway, final MessageTypes msgType, final InboundMessage msg) {
        
        final Object[] param = { msg.getText(), msg.getOriginator(), gateway.getGatewayId() };
        LOG.info("Incoming message: {} from phone number {} received by gateway {}", param);
        
        final IncomingSmsMessage inMsg = new IncomingSmsMessage(msg);
        for (DeviceListener o : listener) {
            o.onIncomingMessage(new DeviceObject(this, inMsg));
        }

        if (deleteMessage(msg)) {
            LOG.info("Message has been deleted.");
        } else {
            LOG.warn("Message CANNOT be deleted.");
        }
    }
    
    public void setInboundMessageListener(IInboundMessageNotification notification) {
        modemService.setInboundMessageNotification(notification);
    }
    
    @Override
    public boolean deleteMessage(InboundMessage message) {
        boolean success = false;
        try {
            success = modemService.deleteMessage(message);
        } catch (Exception e) {
            LOG.error("[*** " + e.getClass().getSimpleName() + " ***]: " + e.getMessage());
        }
        return success;
    }
    
    @Override
    public int sendMessages(final Collection<SmsAlarmMessage> msgList) {
        int sent = 0;
        for (SmsAlarmMessage o : msgList) {
            if (sendMessage(o)) {
                sent++;
            }
        }
        return sent;
    }
    
    @Override
    public boolean sendMessage(SmsAlarmMessage message) {
        
        boolean success = false;
        
        OutboundMessage msg = new OutboundMessage(message.getReceiverAddress(), message.getMessageText());
        msg.setEncoding(MessageEncodings.ENC7BIT);
        // Changed by Markus Moeller, 2009-01-30
        // To avoid restarts of the modems
        // msg.setStatusReport(true);
        msg.setStatusReport(false);
        msg.setValidityPeriod(8);
        
        // Total number of outbound messages since restart
        int totalOutBefore = modemService.getOutboundMessageCount();
        
        // TODO: Eventuell die Liste aller Modems und ihre Zust�nde ausgeben
        try {
            LOG.info("Try to send SMS...");
            modemService.sendMessage(msg);
            int totalOutAfter = modemService.getOutboundMessageCount();
            if (totalOutBefore < totalOutAfter) {
                LOG.info("SMS sent to: '{}' with text: '{}'", message.getReceiverAddress(), message.getMessageText());
                message.setMessageState(State.SENT);
                success = true;
            } else {
                success = false;
                message.setMessageState(State.FAILED);

                // If no modems are defined, return true anyhow
                if(modemInfo.getModemCount() == 0) {
                    success = true;
                    message.setMessageState(State.SENT);
                }
            }
            LOG.info("Number of sent SMS: {}", totalOutAfter);
        } catch(Exception e) {
            LOG.error("[*** {} ***]: Could not send message: {}", e.getClass().getSimpleName(), e.getMessage());
        }

        return success;
    }

    public void announceDeviceTest(DeviceTestMessageContent msg) {
        if (!testStatus.isActive()) {
            testStatus.setDeviceTestMessageContent(msg);
        } else {
            LOG.info("A modem check is still active. Ignoring the new modem check.");
            return;
        }
        
        new Thread(new DeviceCheckWorker(testStatus, modemInfo, readWaitingPeriod)).start();
    }

    public void sendTestAnswer(String checkId, String text, String severity, String value) {
        
        JmsSimpleProducer producer = new JmsSimpleProducer("SmsDeliveryDevice@"
                                                           + Environment.getInstance().getHostName(),
                                                           jmsProps.getJmsUrl(),
                                                           jmsProps.getJmsFactoryClass(),
                                                           jmsProps.getJmsTopic());
        
        try {
            
            MapMessage mapMessage = producer.createMapMessage();
            mapMessage.setString("TYPE", "event");
            mapMessage.setString("EVENTTIME", producer.getCurrentDateAsString());
            mapMessage.setString("TEXT", text);
            mapMessage.setString("SEVERITY", severity);
            mapMessage.setString("VALUE", value);
            mapMessage.setString("CLASS", checkId);
            mapMessage.setString("HOST", Environment.getInstance().getHostName());
            mapMessage.setString("USER", Environment.getInstance().getUserName());
            mapMessage.setString("NAME", "AMS_SYSTEM_CHECK_ANSWER");
            mapMessage.setString("APPLICATION-ID", "SmsDeliveryWorker");
            mapMessage.setString("DESTINATION", "AmsSystemMonitor");
            
            producer.sendMessage(mapMessage);
        } catch(JMSException jmse) {
            LOG.error("Answer message could NOT be sent: {}", jmse.getMessage());
        }
    }

    /**
     * Reads the oldest message from any storage location.
     */
    @Override
    public InboundMessage readMessage() {
        
        ArrayList<InboundMessage> msgList = new ArrayList<InboundMessage>();
        try {
            modemService.readMessages(msgList, MessageClasses.ALL);
        } catch (Exception e) {
            LOG.error("[*** " + e.getClass().getSimpleName() + " ***]: " + e.getMessage());
        }
        
        InboundMessage result = null;
        if (msgList.size() > 0) {
            long timeStamp = System.currentTimeMillis();
            for (InboundMessage o : msgList) {
                if (o.getDate().getTime() <= timeStamp) {
                    result = o;
                    timeStamp = o.getDate().getTime();
                }
            }
        }
    
        return result;
    }

    @Override
    public int readMessages(Collection<InboundMessage> msgList) {
        
        int read = 0;
        try {
            read = modemService.readMessages(msgList, MessageClasses.ALL);
        } catch (Exception e) {
            LOG.error("[*** " + e.getClass().getSimpleName() + " ***]: " + e.getMessage());
        }
        
        return read;
    }

    @Override
    public void stopDevice() {
        if (modemService != null) {
            try {
                modemService.stopService();
            } catch (Exception e) {
                LOG.warn("[*** {} ***]: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    private boolean initModem() {
        
        String[] strComPort = null;
        String[] strManufac = null;
        String[] strModel = null;
        String[] strSimPin = null;
        String[] strPhoneNumber = null;
        String m = null;
        int[] iBaudRate = null;
        int modemCount = 1;
        
        boolean result = false;

        IPreferencesService prefs = Platform.getPreferencesService();
        try {
            ////////////////////////////////////////////////////////////////////////
            // strComPort   - COM-Port: "COM1", "COM2", "COM3", ... , "/dev/ttyS1", ...
            // iBaudRate        - Modem Baud-Rate: 9600, 57600, ... 
            // strManufac   - gsmDeviceManufacturer: "SonyEricsson", "Siemens", "Wavecom", "Nokia", ..., ""
            // strModel     - gsmDeviceModel: "GS64", "M1306B", "6310i", ..., ""
            // strSimPin        - SimCard Pin-Number: "1234", ...
            ////////////////////////////////////////////////////////////////////////
            
            modemCount = prefs.getInt(SmsDeliveryActivator.PLUGIN_ID,
                                      SmsConnectorPreferenceKey.P_MODEM_COUNT,
                                      0, null);
            modemCount = (modemCount < 0) ? 0 : modemCount;
            modemCount = (modemCount > MAX_MODEM_NUMBER) ? MAX_MODEM_NUMBER : modemCount;
            LOG.info("Number of modems: " + modemCount);

            strComPort = new String[modemCount];
            strManufac = new String[modemCount];
            strModel = new String[modemCount];
            strSimPin = new String[modemCount];
            strPhoneNumber = new String[modemCount];
            iBaudRate = new int[modemCount];
            
            // TODO: Better error handling and value checks
            for(int i = 0;i < modemCount;i++) {
                
                strComPort[i] = prefs.getString(SmsDeliveryActivator.PLUGIN_ID,
                                                SmsConnectorPreferenceKey.P_PREFERENCE_STRING
                                                + (i + 1) + "ComPort",
                                                "",
                                                null);
                
                iBaudRate[i] = prefs.getInt(SmsDeliveryActivator.PLUGIN_ID,
                                            SmsConnectorPreferenceKey.P_PREFERENCE_STRING
                                            + (i + 1) + "ComBaudrate",
                                            9600,
                                            null);
                
                strManufac[i] = prefs.getString(SmsDeliveryActivator.PLUGIN_ID,
                                                SmsConnectorPreferenceKey.P_PREFERENCE_STRING
                                                + (i + 1) + "Manufacture",
                                                "",
                                                null);
                
                strModel[i] = prefs.getString(SmsDeliveryActivator.PLUGIN_ID,
                                              SmsConnectorPreferenceKey.P_PREFERENCE_STRING
                                              + (i + 1) + "Model",
                                              "",
                                              null);
                
                strSimPin[i] = prefs.getString(SmsDeliveryActivator.PLUGIN_ID,
                                               SmsConnectorPreferenceKey.P_PREFERENCE_STRING
                                               + (i + 1) + "SimPin",
                                               "",
                                               null);

                strPhoneNumber[i] = prefs.getString(SmsDeliveryActivator.PLUGIN_ID,
                                                    SmsConnectorPreferenceKey.P_PREFERENCE_STRING
                                                    + (i + 1) + "Number",
                                                    "",
                                                    null);
            }
            
            modemService = Service.getInstance();
            
            for(int i = 0;i < modemCount;i++) {
                if(strComPort[i].length() > 0) {
                    LOG.info("Start initModem(" + strComPort[i] + ","
                            + iBaudRate[i] + ","
                            + strManufac[i] + ","
                            + strModel[i] + ")");
                    // modemService = new CSoftwareService(strComPort, iBaudRate, strManufac, strModel);
                    m = "modem." + strComPort[i].toLowerCase();
                    SerialModemGateway modem = new SerialModemGateway(m , strComPort[i], iBaudRate[i], strManufac[i], strModel[i]);
                    modem.setInbound(true);
                    modem.setOutbound(true);
                    modem.setSimPin(strSimPin[i]);
                    // modem.setOutboundNotification(outboundNotification);
                    modemService.addGateway(modem);
                    modemInfo.addModemName(m, strPhoneNumber[i]);
                                        
                    sleep(2000);
                } else {
                    LOG.warn("No COM port defined for modem " + (i + 1) + ".");
                }
            }
            
            result = true;
            
            LOG.info("Modem(s) are initialized");
            
            modemService.setGatewayStatusNotification(new GatewayStatusNotification(modemInfo.getModemNames()));
            modemService.setInboundMessageNotification(this);
            
            if((result == true) && (modemCount > 0)) {
                LOG.info("Try to start service");
                modemService.startService();
                LOG.info("Service started");
            }
        } catch(Exception e) {
            LOG.error("Could not init modem: {}", e);
            JmsSender sender = new JmsSender("SmsConnectorAlarmSender",
                                             prefs.getString(AmsActivator.PLUGIN_ID,
                                                             AmsPreferenceKey.P_JMS_AMS_SENDER_PROVIDER_URL,
                                                             "failover:(tcp://localhost:62616,tcp://localhost:64616)",
                                                             null),
                                                             "ALARM");
            if(sender.isConnected()) {
                if(sender.sendMessage("alarm",
                                      "SmsDeliveryWorker: Cannot init modem [" + e.getMessage() + "]",
                                      "MAJOR") == false) {
                    LOG.error("Cannot send alarm message.");
                } else {
                    LOG.info("Alarm message sent.");
                }
            } else {
                LOG.warn("Alarm message sender is NOT connected.");
            }
            sender.closeAll();
            sender = null;
            result = false;
        }
        
        return result;
    }
    
    private void sleep(long ms) {
        synchronized (this) {
            try {
                this.wait(ms);
            } catch (InterruptedException e) {
                // Ignore me
            }
        }
    }
    
    /**
     * Checks the modem.
     * 
     * @author mmoeller
     * @version 1.0
     * @since 27.02.2012
     */
    class DeviceCheckWorker implements Runnable, DeviceListener {

        private ModemTestStatus modemTestStatus;
        
        private ModemInfoContainer deviceInfo;
        
        private List<IncomingSmsMessage> inQueue;
        
        private Object lock;
        
        long readWaitingInterval;
        
        public DeviceCheckWorker(ModemTestStatus checkStatus, ModemInfoContainer info, long readInterval) {
            modemTestStatus = checkStatus;
            deviceInfo = info;
            inQueue = Collections.synchronizedList(new ArrayList<IncomingSmsMessage>());
            lock = new Object();
            readWaitingInterval = readInterval;
        }
        
        @Override
        public void run() {
            
            getLogger().info("Starting device check.");
            
            if (modemTestStatus.isDeviceTestInitiated()) {
                final DeviceTestMessageContent content = modemTestStatus.getDeviceTestMessageContent();
                modemTestStatus.finishedDeviceTestInitiated();
                if(modemTestStatus.isActive() == false || modemTestStatus.isTimeOut()) {
                    final String checkId = content.getCheckId();
                    modemTestStatus.reset();
                    modemTestStatus.setCheckId(checkId);
                    sendDeviceTestMessage();
                }
            }
            
            do {
                synchronized (lock) {
                    try {
                        lock.wait(readWaitingInterval);
                    } catch (InterruptedException e) {
                        getLogger().warn("DeviceCheckWorker has been interrupted.");
                    }
                }
    
                if (!inQueue.isEmpty()) {
                    for (IncomingSmsMessage o : inQueue) {
                        checkDeviceTest(o);
                    }
                } else {
                    checkDeviceTest(null);
                }
            } while (modemTestStatus.isActive() && !modemTestStatus.isTimeOut());
            
            removeDeviceListener(this);
            
            getLogger().info("Leaving device check.");
        }
        
        private void sendDeviceTestMessage() {
            
            OutboundMessage outMsg = null;
            String name = null;
            String number = null;
            String text = null;
            
            getLogger().info("Number of modems to test: {}", deviceInfo.getModemCount());
            for(int i = 0;i < deviceInfo.getModemCount();i++) {
                name = deviceInfo.getModemName(i);
                if(name != null) {
                    number = deviceInfo.getPhoneNumber(name);

                    text = SMS_TEST_TEXT;
                    text = text.replaceAll("\\$CHECKID", modemTestStatus.getCheckId());
                    text = text.replaceAll("\\$GATEWAYID", name);
                    
                    outMsg = new OutboundMessage(number, text);
                    outMsg.setEncoding(MessageEncodings.ENC7BIT);
                    outMsg.setStatusReport(false);
                    outMsg.setValidityPeriod(8);
                    
                    try {
                        getLogger().info("Sending to modem '{}': {}", name, text);
                        synchronized (getDeviceService()) {
                            if(getDeviceService().sendMessage(outMsg, name)) {
                                modemTestStatus.addGatewayId(name);
                            }
                        }
                    } catch(Exception e) {
                        getLogger().warn("Could not send SMS test message to modem '{}'.", name);
                        modemTestStatus.addBadModem(name);
                    }
                    
                    outMsg = null;
                }
            }
            
            if(modemTestStatus.getGatewayCount() > 0) {
                modemTestStatus.setActive(true);
                modemTestStatus.setTimeOut(System.currentTimeMillis() + 120000); // 2 minutes
            } else {
                sendTestAnswer(modemTestStatus.getCheckId(), "No modem could send the test SMS.", "MAJOR", "ERROR");
                modemTestStatus.reset();
            }
        }

        private boolean checkDeviceTest(final IncomingSmsMessage o) {

            boolean checked = false;
            // LOG.debug("Check for device test.");

            // Check modem test status first
            if(modemTestStatus.isActive()) {
                //LOG.info("Self test is active");
                if(modemTestStatus.isTimeOut()) {
                    getLogger().warn("Current test timed out.");
                    getLogger().debug("Remaining gateways: " + modemTestStatus.getGatewayCount());
                    getLogger().debug("Bad gateways before moving: " + modemTestStatus.getBadModemCount());
                    modemTestStatus.moveGatewayIdToBadModems();
                    getLogger().debug("Remaining gateways after moving: " + modemTestStatus.getGatewayCount());
                    getLogger().debug("Bad gateways after moving: " + modemTestStatus.getBadModemCount());
                    if(modemTestStatus.getBadModemCount() == deviceInfo.getModemCount()) {
                        getLogger().error("No modem is working properly.");
                        sendTestAnswer(modemTestStatus.getCheckId(), "No modem is working properly.", "MAJOR", "ERROR");
                    } else {
                        String list = "";
                        for(final String name : modemTestStatus.getBadModems()) {
                            list = list + name + " ";
                        }

                        getLogger().warn("Modems not working properly: " + list);
                        sendTestAnswer(modemTestStatus.getCheckId(), "Modems not working properly: " + list, "MINOR", "WARN");
                    }

                    getLogger().info("Reset current test.");
                    modemTestStatus.reset();
                }
            }

            if (o == null) {
                return true;
            }
            
            if (!(o.getOriginalMessage() instanceof InboundBinaryMessage)) {

                final InboundMessage msg = (InboundMessage) o.getOriginalMessage();
                if (o.isTestAnswer()) {
                    // Have a look at the current check status
                    if(modemTestStatus.isActive()) {
                        if(modemTestStatus.isTimeOut() == false) {

                            getLogger().info("Self test SMS");
                            getLogger().info("Gateways waiting for answer: " + modemTestStatus.getGatewayCount());
                            modemTestStatus.checkAndRemove(msg.getText());
                            getLogger().info("Gateways waiting for answer after remove: " + modemTestStatus.getGatewayCount());
                            if(modemTestStatus.getGatewayCount() == 0) {
                                if(modemTestStatus.getBadModemCount() == 0) {
                                    getLogger().info("All modems are working fine.");
                                    sendTestAnswer(modemTestStatus.getCheckId(),
                                                             "All modems are working fine.",
                                                             "NO_ALARM",
                                                             "OK");
                                } else {
                                    String list = "";
                                    for(final String name : modemTestStatus.getBadModems()) {
                                        list = list + name + " ";
                                    }

                                    getLogger().warn("Modems not working properly: " + list);
                                    sendTestAnswer(modemTestStatus.getCheckId(),
                                                             "Modems not working properly: " + list,
                                                             "MINOR",
                                                             "WARN");
                                }

                                getLogger().info("Reset current test.");
                                modemTestStatus.reset();
                            }
                        }
                    }
                }
                checked = true;
            } else {
                checked = true;
            }

            return checked;
        }

        @Override
        public void onIncomingMessage(DeviceObject event) {
            IncomingSmsMessage msg = (IncomingSmsMessage) event.getMessage();
            if (msg.isTestAnswer()) {
                inQueue.add(msg);
            }
        }
    }
}
