package org.csstudio.saverestore.ui;

import java.util.List;
import java.util.Map;

import org.csstudio.saverestore.data.Threshold;

/**
 *
 * <code>ParametersProvider</code> provides additional parameters needed for displaying the snapshots, such as readback
 * pv names and threshold values for acceptable difference between setpoints and readbacks.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public interface ParametersProvider {

    /** The extension point id */
    public static final String EXT_POINT = "org.csstudio.saverestore.ui.parametersprovider";

    /**
     * Returns the list of readback PV names for the given list of setpoint PV names. For every setpoint PV that a
     * readback is known there should be an entry in the returned map.
     * </p>
     * <p>
     * This method will never be called from the UI thread.
     * </p>
     *
     * @param setpointNames the list of pv names for which the readback names are requested
     * @return a map of key-value pairs, where the key is a setpoint name and value is the corresponding readback name
     */
    Map<String, String> getReadbackNames(List<String> setpointNames);

    /**
     * Returns the list of threshold for the given pv names. For every PV in the list there should be an entry in the
     * returned map. If a specific PV does not have a threshold value or cannot have it (e.g. is a string type PV) the
     * value in the map for that PV should be null. These thresholds are used to determine whether two values of the
     * same PV are equal (they are within the threshold limits) or different.
     *
     * @param setpointNames the setpoint names
     * @return the map of PV name and threshold pairs
     */
    Map<String, Threshold<?>> getThresholds(List<String> setpointNames);

}
