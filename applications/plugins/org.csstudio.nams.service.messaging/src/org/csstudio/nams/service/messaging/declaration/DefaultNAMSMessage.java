package org.csstudio.nams.service.messaging.declaration;

import org.csstudio.nams.common.contract.Contract;
import org.csstudio.nams.common.material.AlarmNachricht;
import org.csstudio.nams.common.material.SystemNachricht;
import org.csstudio.nams.service.messaging.exceptions.MessagingException;

public class DefaultNAMSMessage implements NAMSMessage {

	
	private final SystemNachricht sysNachricht;
	private final AcknowledgeHandler ackHandler;
	private final AlarmNachricht alarmNachricht;

	public DefaultNAMSMessage(SystemNachricht sysNachricht, AcknowledgeHandler ackHandler) {
		Contract.requireNotNull("ackHandler", ackHandler);
		Contract.requireNotNull("sysNachricht", sysNachricht);
		this.sysNachricht = sysNachricht;
		this.ackHandler = ackHandler;
		alarmNachricht = null;
	}
	
	public DefaultNAMSMessage(AlarmNachricht alarmNachricht, AcknowledgeHandler ackHandler) {
		Contract.requireNotNull("ackHandler", ackHandler);
		Contract.requireNotNull("alarmNachricht", alarmNachricht);
		this.alarmNachricht = alarmNachricht;
		this.ackHandler = ackHandler;
		sysNachricht = null;
	}
	
	public final void acknowledge() throws MessagingException {
		try {
			ackHandler.acknowledge();
		} catch (Throwable e) {
			throw new MessagingException("acknowledge failed",e);
		}
	}

	public final AlarmNachricht alsAlarmnachricht() {
		Contract.require(enthaeltAlarmnachricht(), "enthaeltAlarmnachricht()");
		return alarmNachricht;
	}

	public final SystemNachricht alsSystemachricht() {
		Contract.require(enthaeltSystemnachricht(), "enthaeltSystemnachricht()");
		return sysNachricht;
	}

	public final boolean enthaeltAlarmnachricht() {
		return alarmNachricht != null;
	}

	public final boolean enthaeltSystemnachricht() {
		return sysNachricht != null;
	}

	public static interface AcknowledgeHandler {
		public void acknowledge() throws Throwable;
	}
}


//package org.csstudio.nams.service.messaging.declaration;
//
//import java.util.Map;
//
//import org.csstudio.nams.common.contract.Contract;
//import org.csstudio.nams.common.fachwert.MessageKeyEnum;
//import org.csstudio.nams.common.material.AlarmNachricht;
//import org.csstudio.nams.common.material.SyncronisationsAufforderungsSystemNachchricht;
//import org.csstudio.nams.common.material.SyncronisationsBestaetigungSystemNachricht;
//import org.csstudio.nams.common.material.SystemNachricht;
//import org.csstudio.nams.service.messaging.exceptions.MessagingException;
//
///**
// * 
// * @author c1wps
// * @deprecated use {@link AbstractNAMSMessage}
// */
//@Deprecated
//public class DefaultNAMSMessage implements NAMSMessage {
//
//	final static String MSGPROP_COMMAND = "COMMAND"; 
//	final static String MSGVALUE_TCMD_RELOAD = "AMS_RELOAD_CFG";
//	final static String MSGVALUE_TCMD_RELOAD_CFG_START = MSGVALUE_TCMD_RELOAD + "_START";
//	final static String MSGVALUE_TCMD_RELOAD_CFG_END = MSGVALUE_TCMD_RELOAD + "_END";
//	
////	private final Map<String, String> map;
//	private final AcknowledgeHandler handler;
//	private SystemNachricht systemNachricht;
//	private AlarmNachricht alarmNachricht;
//
//	public DefaultNAMSMessage(Map<MessageKeyEnum, String> map) {
//		this(map, null);
//	}
//	
//	public DefaultNAMSMessage(Map<MessageKeyEnum, String> map, AcknowledgeHandler handler) {
////		this.map = map;
//		this.handler = handler;
//		
//		if (map.containsKey(MessageKeyEnum.MSGPROP_COMMAND)) {
//			String command = map.get(MessageKeyEnum.MSGPROP_COMMAND);
//			if (command.equals(MSGVALUE_TCMD_RELOAD_CFG_START)) {
//				systemNachricht = new SyncronisationsAufforderungsSystemNachchricht();
//				alarmNachricht = null;
//			} else if (command.equals(MSGVALUE_TCMD_RELOAD_CFG_END)) {
//				systemNachricht = new SyncronisationsBestaetigungSystemNachricht();
//				alarmNachricht = null;
//			} else {
//				alarmNachricht = new AlarmNachricht(map);
//				systemNachricht = null;
//			}
//		} else {
//			systemNachricht = null;
//			alarmNachricht = null;
//		}
//	}
//	
//	public void acknowledge() throws MessagingException {
//		if (this.handler != null) {
//			try {
//				this.handler.acknowledge();
//			} catch (Throwable e) {
//				throw new MessagingException(e);
//			}
//		}
//	}
//
//	public AlarmNachricht alsAlarmnachricht() {
//		Contract.ensure(enthaeltAlarmnachricht(), "NAMS Message is AlarmNachricht");
//		return alarmNachricht;
//	}
//
//	@Deprecated
//	public Map<String, String> alsMap() {
//		throw new UnsupportedOperationException();
//	}
//
//	public SystemNachricht alsSystemachricht() {
//		Contract.ensure(enthaeltSystemnachricht(), "NAMS Message is SystemNachricht");
//		return systemNachricht;
//	}
//
//	public boolean enthaeltAlarmnachricht() {
//		return alarmNachricht != null;
//	}
//
//	public boolean enthaeltSystemnachricht() {
//		return systemNachricht != null;
//	}
//
//	public static interface AcknowledgeHandler {
//		public void acknowledge() throws Throwable;
//	}
//	
//}
