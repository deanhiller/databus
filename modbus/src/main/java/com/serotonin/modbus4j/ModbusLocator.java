/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2006-2011 Serotonin Software Technologies Inc. http://serotoninsoftware.com
 * @author Matthew Lohbihler
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.modbus4j;

import java.math.BigInteger;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.base.SlaveAndRange;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.IllegalDataTypeException;
import com.serotonin.modbus4j.exception.ModbusIdException;
import com.serotonin.modbus4j.exception.ModbusTransportException;

public class ModbusLocator {
    private final SlaveAndRange slaveAndRange;
    private final int offset;
    private final int dataType;
    private byte bit = -1;

    public ModbusLocator(SlaveAndRange slaveAndRange, int offset, int dataType) {
        this(slaveAndRange, offset, dataType, (byte) -1);
    }

    public ModbusLocator(int slaveId, int range, int offset, int dataType) {
        this(new SlaveAndRange(slaveId, range), offset, dataType, (byte) -1);
    }

    public ModbusLocator(int slaveId, int range, int offset, byte bit) {
        this(new SlaveAndRange(slaveId, range), offset, DataType.BINARY, bit);
        if (range != RegisterRange.HOLDING_REGISTER && range != RegisterRange.INPUT_REGISTER)
            throw new ModbusIdException("Bit requests can only be made from holding registers and input registers");
    }

    public ModbusLocator(int slaveId, int range, int offset, int dataType, byte bit) {
        this(new SlaveAndRange(slaveId, range), offset, dataType, bit);
    }

    private ModbusLocator(SlaveAndRange slaveAndRange, int offset, int dataType, byte bit) {
        this.slaveAndRange = slaveAndRange;
        this.offset = offset;
        this.dataType = dataType;
        this.bit = bit;
        validate();
    }

    private void validate() {
        try {
            ModbusUtils.validateOffset(offset);
            ModbusUtils.validateEndOffset(offset + DataType.getRegisterCount(dataType) - 1);
        }
        catch (ModbusTransportException e) {
            throw new ModbusIdException(e);
        }

        int range = slaveAndRange.getRange();
        if (dataType != DataType.BINARY && (range == RegisterRange.COIL_STATUS || range == RegisterRange.INPUT_STATUS))
            throw new IllegalDataTypeException("Only binary values can be read from Coil and Input ranges");

        if ((range == RegisterRange.HOLDING_REGISTER || range == RegisterRange.INPUT_REGISTER)
                && dataType == DataType.BINARY)
            ModbusUtils.validateBit(bit);
    }

    public int getDataType() {
        return dataType;
    }

    public int getOffset() {
        return offset;
    }

    public SlaveAndRange getSlaveAndRange() {
        return slaveAndRange;
    }

    public int getEndOffset() {
        return offset + DataType.getRegisterCount(dataType) - 1;
    }

    public int getLength() {
        return DataType.getRegisterCount(dataType);
    }

    public byte getBit() {
        return bit;
    }

    /**
     * Converts data from the byte array into a java value according to this locator's data type.
     * 
     * @param data
     * @param requestOffset
     * @return the converted data
     */
    public Object bytesToValue(byte[] data, int requestOffset) {
        // Determined the offset normalized to the response data.
        return bytesToValue(data, slaveAndRange.getRange(), offset - requestOffset, dataType, bit);
    }

    public static Object bytesToValue(byte[] data, int range, int offset, int dataType, byte bit) {
        // If this is a coil or input, convert to boolean.
        if (range == RegisterRange.COIL_STATUS || range == RegisterRange.INPUT_STATUS)
            return new Boolean((((data[offset / 8] & 0xff) >> (offset % 8)) & 0x1) == 1);

        // For the rest of the types, we double the normalized offset to account for short to byte.
        offset *= 2;

        // We could still be asking for a binary if it's a bit in a register.
        if (dataType == DataType.BINARY)
            return new Boolean((((data[offset + 1 - bit / 8] & 0xff) >> (bit % 8)) & 0x1) == 1);

        // Handle the numeric types.
        // 2 bytes
        if (dataType == DataType.TWO_BYTE_INT_UNSIGNED)
            return new Integer(((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff));

        if (dataType == DataType.TWO_BYTE_INT_SIGNED)
            return new Short((short) (((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff)));

        if (dataType == DataType.TWO_BYTE_BCD) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 2; i++) {
                sb.append(bcdNibbleToInt(data[offset + i], true));
                sb.append(bcdNibbleToInt(data[offset + i], false));
            }
            return Short.parseShort(sb.toString());
        }

        // 4 bytes
        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED)
            return new Long(((long) ((data[offset] & 0xff)) << 24) | ((long) ((data[offset + 1] & 0xff)) << 16)
                    | ((long) ((data[offset + 2] & 0xff)) << 8) | ((data[offset + 3] & 0xff)));

        if (dataType == DataType.FOUR_BYTE_INT_SIGNED)
            return new Integer(((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16)
                    | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff));

        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED)
            return new Long(((long) ((data[offset + 2] & 0xff)) << 24) | ((long) ((data[offset + 3] & 0xff)) << 16)
                    | ((long) ((data[offset] & 0xff)) << 8) | ((data[offset + 1] & 0xff)));

        if (dataType == DataType.FOUR_BYTE_INT_SIGNED_SWAPPED)
            return new Integer(((data[offset + 2] & 0xff) << 24) | ((data[offset + 3] & 0xff) << 16)
                    | ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff));

        if (dataType == DataType.FOUR_BYTE_FLOAT)
            return Float.intBitsToFloat(((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16)
                    | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff));

        if (dataType == DataType.FOUR_BYTE_FLOAT_SWAPPED)
            return Float.intBitsToFloat(((data[offset + 2] & 0xff) << 24) | ((data[offset + 3] & 0xff) << 16)
                    | ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff));

        if (dataType == DataType.FOUR_BYTE_BCD) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(bcdNibbleToInt(data[offset + i], true));
                sb.append(bcdNibbleToInt(data[offset + i], false));
            }
            return Integer.parseInt(sb.toString());
        }

        // 8 bytes
        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED) {
            byte[] b9 = new byte[9];
            System.arraycopy(data, offset, b9, 1, 8);
            return new BigInteger(b9);
        }

        if (dataType == DataType.EIGHT_BYTE_INT_SIGNED)
            return new Long(((long) ((data[offset] & 0xff)) << 56) | ((long) ((data[offset + 1] & 0xff)) << 48)
                    | ((long) ((data[offset + 2] & 0xff)) << 40) | ((long) ((data[offset + 3] & 0xff)) << 32)
                    | ((long) ((data[offset + 4] & 0xff)) << 24) | ((long) ((data[offset + 5] & 0xff)) << 16)
                    | ((long) ((data[offset + 6] & 0xff)) << 8) | ((data[offset + 7] & 0xff)));

        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED) {
            byte[] b9 = new byte[9];
            b9[1] = data[offset + 6];
            b9[2] = data[offset + 7];
            b9[3] = data[offset + 4];
            b9[4] = data[offset + 5];
            b9[5] = data[offset + 2];
            b9[6] = data[offset + 3];
            b9[7] = data[offset];
            b9[8] = data[offset + 1];
            return new BigInteger(b9);
        }

        if (dataType == DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED)
            return new Long(((long) ((data[offset + 6] & 0xff)) << 56) | ((long) ((data[offset + 7] & 0xff)) << 48)
                    | ((long) ((data[offset + 4] & 0xff)) << 40) | ((long) ((data[offset + 5] & 0xff)) << 32)
                    | ((long) ((data[offset + 2] & 0xff)) << 24) | ((long) ((data[offset + 3] & 0xff)) << 16)
                    | ((long) ((data[offset] & 0xff)) << 8) | ((data[offset + 1] & 0xff)));

        if (dataType == DataType.EIGHT_BYTE_FLOAT)
            return Double.longBitsToDouble(((long) ((data[offset] & 0xff)) << 56)
                    | ((long) ((data[offset + 1] & 0xff)) << 48) | ((long) ((data[offset + 2] & 0xff)) << 40)
                    | ((long) ((data[offset + 3] & 0xff)) << 32) | ((long) ((data[offset + 4] & 0xff)) << 24)
                    | ((long) ((data[offset + 5] & 0xff)) << 16) | ((long) ((data[offset + 6] & 0xff)) << 8)
                    | ((data[offset + 7] & 0xff)));

        if (dataType == DataType.EIGHT_BYTE_FLOAT_SWAPPED)
            return Double.longBitsToDouble(((long) ((data[offset + 6] & 0xff)) << 56)
                    | ((long) ((data[offset + 7] & 0xff)) << 48) | ((long) ((data[offset + 4] & 0xff)) << 40)
                    | ((long) ((data[offset + 5] & 0xff)) << 32) | ((long) ((data[offset + 2] & 0xff)) << 24)
                    | ((long) ((data[offset + 3] & 0xff)) << 16) | ((long) ((data[offset] & 0xff)) << 8)
                    | ((data[offset + 1] & 0xff)));

        throw new RuntimeException("Unsupported data type: " + dataType);
    }

    private static int bcdNibbleToInt(byte b, boolean high) {
        int n;
        if (high)
            n = (b >> 4) & 0xf;
        else
            n = b & 0xf;
        if (n > 9)
            n = 0;
        return n;
    }

    public short[] valueToShorts(Number value) {
        return valueToShorts(value, dataType);
    }

    /**
     * Converts data from a java value into the byte array according to this locator's data type. This method does not
     * handle the binary type.
     * 
     * @param value
     * @param dataType
     * @return the converted data
     */
    public static short[] valueToShorts(Number value, int dataType) {
        // 2 bytes
        if (dataType == DataType.TWO_BYTE_INT_UNSIGNED || dataType == DataType.TWO_BYTE_INT_SIGNED)
            return new short[] { value.shortValue() };

        if (dataType == DataType.TWO_BYTE_BCD) {
            short s = value.shortValue();
            return new short[] { (short) ((((s / 1000) % 10) << 12) | (((s / 100) % 10) << 8) | (((s / 10) % 10) << 4) | (s % 10)) };
        }

        // 4 bytes
        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED || dataType == DataType.FOUR_BYTE_INT_SIGNED) {
            int i = value.intValue();
            return new short[] { (short) (i >> 16), (short) i };
        }

        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED || dataType == DataType.FOUR_BYTE_INT_SIGNED_SWAPPED) {
            int i = value.intValue();
            return new short[] { (short) i, (short) (i >> 16) };
        }

        if (dataType == DataType.FOUR_BYTE_FLOAT) {
            int i = Float.floatToIntBits(value.floatValue());
            return new short[] { (short) (i >> 16), (short) i };
        }

        if (dataType == DataType.FOUR_BYTE_FLOAT_SWAPPED) {
            int i = Float.floatToIntBits(value.floatValue());
            return new short[] { (short) i, (short) (i >> 16) };
        }

        if (dataType == DataType.FOUR_BYTE_BCD) {
            int i = value.intValue();
            return new short[] {
                    (short) ((((i / 10000000) % 10) << 12) | (((i / 1000000) % 10) << 8) | (((i / 100000) % 10) << 4) | ((i / 10000) % 10)),
                    (short) ((((i / 1000) % 10) << 12) | (((i / 100) % 10) << 8) | (((i / 10) % 10) << 4) | (i % 10)) };
        }

        // 8 bytes
        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED || dataType == DataType.EIGHT_BYTE_INT_SIGNED) {
            long l = value.longValue();
            return new short[] { (short) (l >> 48), (short) (l >> 32), (short) (l >> 16), (short) l };
        }

        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED || dataType == DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED) {
            long l = value.longValue();
            return new short[] { (short) l, (short) (l >> 16), (short) (l >> 32), (short) (l >> 48) };
        }

        if (dataType == DataType.EIGHT_BYTE_FLOAT) {
            long l = Double.doubleToLongBits(value.doubleValue());
            return new short[] { (short) (l >> 48), (short) (l >> 32), (short) (l >> 16), (short) l };
        }

        if (dataType == DataType.EIGHT_BYTE_FLOAT_SWAPPED) {
            long l = Double.doubleToLongBits(value.doubleValue());
            return new short[] { (short) l, (short) (l >> 16), (short) (l >> 32), (short) (l >> 48) };
        }

        throw new RuntimeException("Unsupported data type: " + dataType);
    }

    public static void main(String[] args) {
        ModbusLocator ml = new ModbusLocator(0, 0, 0, DataType.FOUR_BYTE_BCD);
        int i = 12345678;
        short[] sa = ml.valueToShorts(new Integer(i));
        for (int x = 0; x < sa.length; x++)
            System.out.println(sa[x]);

    }
}
