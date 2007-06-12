package org.csstudio.trends.databrowser.model;

import org.csstudio.swt.chart.TraceType;
import org.eclipse.swt.graphics.Color;

/** Public interface to one item of the Model.
 *  @see Model
 *  @author Kay Kasemir
 */
public interface IModelItem
{
	/** @return The name to use for this item in the legend or axis label. */
	public abstract String getName();

    /** @return The engineering units string. */
    public abstract String getUnits();
    
    /** Change the name of this entry. */
    public abstract void changeName(String new_name);
    
	/** @return The axis index. */
	public abstract int getAxisIndex();
    
    /** Set a new axis index. */
    public abstract void setAxisIndex(int axis);
    
    /** @return The lower Y-axis limit. */
    public abstract double getAxisLow();

    /** @return The upper Y-axis limit. */
    public abstract double getAxisHigh();

    /** Set lower Y-axis limit. */
    public abstract void setAxisLow(double limit);
    
    /** Set upper Y-axis limit. */
    public abstract void setAxisHigh(double limit);
    
    /** @return <code>true</code> if item is visible. */
    public boolean isVisible();
    
    /** Make item visible or hide it.
     *  <p>
     *  Making an item invisible might be useful to temporarily
     *  clear up the display, or for items that are used as
     *  formula inputs yet aren't to be shown themselves.
     *  @param yesno <code>true</code> means visible.
     */
    public void setVisible(boolean yesno);

    /** @return Returns <code>true</code> if trace should be auto-scaled. */
    public abstract boolean getAutoScale();
    
    /** Set auto-scale mode. */
    public abstract void setAutoScale(boolean auto_scale); 
    
    /** Get the color of this item.
     *  Note that the item own the color.
     *  Do NOT dispose this color!
     *  @return The color.
     */
	public abstract Color getColor();

	/** Set item to a new color. */
	public abstract void setColor(Color new_color);
	
	/** @return Returns the trace line width. */
    public abstract int getLineWidth();
    
    /** Set the trace to a new line width. */
    public abstract void setLineWidth(int new_width);
    
    /** @return Returns current trace type */
    public abstract TraceType getTraceType();
    
    /** Set new trace type for this model. */
    public abstract void setTraceType(TraceType new_trace_type);
    
    /** @return <code>true</code> if using log. scale */
    public abstract boolean getLogScale();

    /** Configure to use log. scale or not. */
    public abstract void setLogScale(boolean use_log_scale);

	/** Get the samples of this model item.
     *  <p>
     *  <b>Note:</b> The returned sample interface is passed to
     *  the chart. So when the data of this model item changes
     *  (new samples added, ...), the actual reference to this item's
     *  sample interface should stay the same, just the sample count
     *  and sample instances provided by that interface will differ!
     *  @return The samples.
     */
	public abstract IModelSamples getSamples();
}