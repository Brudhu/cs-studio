/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.csstudio.archive.engine.Messages;
import org.csstudio.archive.engine.model.ArchiveChannel;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.BufferStats;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.SampleBuffer;

/** Provide web page with detail for one channel.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ChannelResponse extends AbstractResponse
{
    /** Avoid serialization errors */
    private static final long serialVersionUID = 1L;

    ChannelResponse(final EngineModel model)
    {
        super(model);
    }

    @Override
    protected void fillResponse(final HttpServletRequest req,
                    final HttpServletResponse resp) throws Exception
    {   // Locate the group
        final String channel_name = req.getParameter("name");
        if (channel_name == null)
        {
            resp.sendError(400, "Missing channel name");
            return;
        }
        final ArchiveChannel channel = model.getChannel(channel_name);
        if (channel == null)
        {
            resp.sendError(400, "Unknown channel " + channel_name);
            return;
        }

        // HTML table similar to group's list of channels
        final HTMLWriter html =
            new HTMLWriter(resp, "Archive Engine Channel");
        html.openTable(2, new String[]
        { Messages.HTTP_ChannelInfo });

        html.tableLine(new String[]
        { Messages.HTTP_Channel, channel_name });

        final String connected = channel.isConnected()
                        ? Messages.HTTP_Connected
                        : HTMLWriter.makeRedText(Messages.HTTP_Disconnected);
        html.tableLine(new String[]
        { Messages.HTTP_Connected, connected });

        html.tableLine(new String[]
        { Messages.HTTP_InternalState, channel.getInternalState() });

        html.tableLine(new String[]
        { Messages.HTTP_Mechanism, channel.getMechanism() });

        html.tableLine(new String[]
        { Messages.HTTP_CurrentValue, channel.getCurrentValue() });

        html.tableLine(new String[]
        { Messages.HTTP_LastArchivedValue, channel.getLastArchivedValue() });

        html.tableLine(new String[]
        { Messages.HTTP_Enablement, channel.getEnablement().toString() });

        html.tableLine(new String[]
        {
            Messages.HTTP_State,
            channel.isEnabled() ? Messages.HTTP_Enabled
                                : HTMLWriter.makeRedText(Messages.HTTP_Disabled)
        });

        final SampleBuffer buffer = channel.getSampleBuffer();
        html.tableLine(new String[]
        { Messages.HTTP_QueueLen, Integer.toString(buffer.getQueueSize()) });

        final BufferStats stats = buffer.getBufferStats();
        html.tableLine(new String[]
        {
            Messages.HTTP_QueueAvg,
            String.format("%.1f", stats.getAverageSize())
        });

        html.tableLine(new String[]
        { Messages.HTTP_QueueMax, Integer.toString(stats.getMaxSize()) });

        html.tableLine(new String[]
        {
            Messages.HTTP_QueueCapacity,
            Integer.toString(buffer.getCapacity())
        });

        final int overrun_count = stats.getOverruns();
        String overruns = Integer.toString(overrun_count);
        if (overrun_count > 0)
            overruns = HTMLWriter.makeRedText(overruns);
        html.tableLine(new String[]
        { Messages.HTTP_QueueOverruns, overruns });

        html.closeTable();

        // Table of all the groups to which this channel belongs
        html.h2("Group Membership");
        html.openTable(1, new String[]
        {
            Messages.HTTP_Group,
            Messages.HTTP_Enabled,
        });
        for (int i=0; i<channel.getGroupCount(); ++i)
        {
            final ArchiveGroup group = channel.getGroup(i);
            html.tableLine(new String[]
            {
                HTMLWriter.makeLink("group?name=" + group.getName(), group.getName()),
                group.isEnabled() ? Messages.HTTP_Enabled
                                  : Messages.HTTP_Disabled,
            });
        }
        html.closeTable();

        html.close();
    }
}
