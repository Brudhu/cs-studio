package org.csstudio.saverestore;

import org.csstudio.saverestore.data.VNoData;
import org.diirt.vtype.VBoolean;
import org.diirt.vtype.VBooleanArray;
import org.diirt.vtype.VByte;
import org.diirt.vtype.VByteArray;
import org.diirt.vtype.VDouble;
import org.diirt.vtype.VDoubleArray;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VFloat;
import org.diirt.vtype.VFloatArray;
import org.diirt.vtype.VInt;
import org.diirt.vtype.VIntArray;
import org.diirt.vtype.VLong;
import org.diirt.vtype.VLongArray;
import org.diirt.vtype.VShort;
import org.diirt.vtype.VShortArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VStringArray;
import org.diirt.vtype.VType;

/**
 * <code>ValueType</code> defines all possible value types that are supported by this data provider. The enumeration
 * provides a mapping between the VType instance and the string representation of the type.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
enum ValueType {
    DOUBLE_ARRAY("double_array", VDoubleArray.class),
    FLOAT_ARRAY("float_array", VFloatArray.class),
    LONG_ARRAY("long_array", VLongArray.class),
    INT_ARRAY("int_array", VIntArray.class),
    SHORT_ARRAY("short_array", VShortArray.class),
    BYTE_ARRAY("byte_array", VByteArray.class),
    ENUM_ARRAY("enum_array", VEnumArray.class),
    STRING_ARRAY("string_array", VStringArray.class),
    BOOLEAN_ARRAY("boolean_array", VBooleanArray.class),
    DOUBLE("double", VDouble.class),
    FLOAT("float", VFloat.class),
    LONG("long", VLong.class),
    INT("int", VInt.class),
    SHORT("short", VShort.class),
    BYTE("byte", VByte.class),
    BOOLEAN("boolean", VBoolean.class),
    STRING("string", VString.class),
    ENUM("enum",VEnum.class),
    NODATA("na",VNoData.class);

    /** Name of the value type */
    public final String name;
    /** Type of VType represented by this value type */
    public final Class<? extends VType> type;

    private ValueType(String name, Class<? extends VType> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Returns whether the vtype is an instance of this value type.
     *
     * @param type the type that which instance needs to be checked
     * @return true if vtype is instance of this or false if not
     */
    public boolean instanceOf(VType type) {
        return this.type.isAssignableFrom(type.getClass());
    }

    /**
     * Returns the value type that matches the given name.
     *
     * @param name the name to match
     * @return the value type, where {@link ValueType#name} is equals to <code>name</code> parameter
     */
    public static ValueType forName(String name) {
        ValueType[] values = values();
        for (ValueType v : values) {
            if (v.name.equals(name)) {
                return v;
            }
        }
        return null;
    }

}
