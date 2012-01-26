/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 * 
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.commandimpl;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.data.values.IStringValue;
import org.csstudio.data.values.ITimestamp;
import org.csstudio.data.values.IValue;
import org.csstudio.data.values.ValueUtil;
import org.csstudio.scan.command.LogCommand;
import org.csstudio.scan.data.ScanSampleFactory;
import org.csstudio.scan.device.Device;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;

/** {@link ScanCommandImpl} that reads data from devices and logs it
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LogCommandImpl extends ScanCommandImpl<LogCommand>
{
	/** Initialize
	 *  @param command Command description
	 */
	public LogCommandImpl(final LogCommand command)
    {
	    super(command);
    }

	/** {@inheritDoc} */
	@Override
	public void execute(final ScanContext context) throws Exception
	{
		final Logger logger = Logger.getLogger(getClass().getName());

		final long serial = ScanSampleFactory.getNextSerial();
		final String[] device_names = command.getDeviceNames();
		final int length = device_names.length;
		for (int i=0; i<length; ++i)
		{
			final String device_name = device_names[i];
			final Device device = context.getDevice(device_name);
			final IValue value = device.read();
			logger.log(Level.FINE, "Log: {0} = {1}",
					new Object[] { device.getName(), value });
			// Log strings as text, rest as double
			if (value instanceof IStringValue)
			    context.logSample(ScanSampleFactory.createSample(device_name,
			            getDate(value.getTime()), serial,
			            ValueUtil.getString(value)));
			else
                context.logSample(ScanSampleFactory.createSample(device_name,
                        getDate(value.getTime()), serial,
                        ValueUtil.getDouble(value)));
		}
        context.workPerformed(1);
	}

	/** @param time IValue timestamp
	 *  @return Date
	 */
	private Date getDate(final ITimestamp time)
    {
	    final long milli = time.seconds()*1000l + time.nanoseconds() / 1000000l;
        return new Date(milli);
    }
}