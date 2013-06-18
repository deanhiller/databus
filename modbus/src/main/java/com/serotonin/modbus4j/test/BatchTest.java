package com.serotonin.modbus4j.test;

import com.serotonin.modbus4j.BatchRead;
import com.serotonin.modbus4j.BatchResults;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.ip.IpParameters;

public class BatchTest {
    public static void main(String[] args) throws Exception {
        IpParameters tcpParameters = new IpParameters();
        tcpParameters.setHost("localhost");

        ModbusFactory modbusFactory = new ModbusFactory();
        ModbusMaster master = modbusFactory.createTcpMaster(tcpParameters, true);

        try {
            BatchRead<String> batchRead = new BatchRead<String>();
            int slaveId = 31;
            batchRead.addLocator("00011 sb true", slaveId, RegisterRange.COIL_STATUS, 10, DataType.BINARY);
            batchRead.addLocator("00012 sb false", slaveId, 12, DataType.BINARY);
            batchRead.addLocator("00013 sb true", slaveId, RegisterRange.COIL_STATUS, 12, DataType.BINARY);
            batchRead.addLocator("00014 sb true", slaveId, 14, DataType.BINARY);

            batchRead.addLocator("10011 sb false", slaveId, RegisterRange.INPUT_STATUS, 10, DataType.BINARY);
            batchRead.addLocator("10012 sb false", slaveId, 10012, DataType.BINARY);
            batchRead.addLocator("10013 sb true", slaveId, RegisterRange.INPUT_STATUS, 12, DataType.BINARY);
            batchRead.addLocator("10014 sb false", slaveId, 10014, DataType.BINARY);

            batchRead.addLocator("40016-0 sb true", slaveId, 40016, (byte) 0);
            batchRead.addLocator("40016-1 sb false", slaveId, 40016, (byte) 1);
            batchRead.addLocator("40016-2 sb false", slaveId, 40016, (byte) 2);
            batchRead.addLocator("40016-3 sb true", slaveId, 40016, (byte) 3);
            batchRead.addLocator("40016-4 sb false", slaveId, 40016, (byte) 4);
            batchRead.addLocator("40016-5 sb false", slaveId, 40016, (byte) 5);
            batchRead.addLocator("40016-6 sb false", slaveId, 40016, (byte) 6);
            batchRead.addLocator("40016-7 sb true", slaveId, 40016, (byte) 7);
            batchRead.addLocator("40016-8 sb true", slaveId, 40016, (byte) 8);
            batchRead.addLocator("40016-9 sb false", slaveId, 40016, (byte) 9);
            batchRead.addLocator("40016-a sb false", slaveId, 40016, (byte) 10);
            batchRead.addLocator("40016-b sb false", slaveId, 40016, (byte) 11);
            batchRead.addLocator("40016-c sb false", slaveId, 40016, (byte) 12);
            batchRead.addLocator("40016-d sb false", slaveId, 40016, (byte) 13);
            batchRead.addLocator("40016-e sb true", slaveId, 40016, (byte) 14);
            batchRead.addLocator("40016-f sb false", slaveId, 40016, (byte) 15);

            batchRead.addLocator("30016-0 sb true", slaveId, 30016, (byte) 0);
            batchRead.addLocator("30016-1 sb false", slaveId, 30016, (byte) 1);
            batchRead.addLocator("30016-2 sb false", slaveId, 30016, (byte) 2);
            batchRead.addLocator("30016-3 sb false", slaveId, 30016, (byte) 3);
            batchRead.addLocator("30016-4 sb false", slaveId, 30016, (byte) 4);
            batchRead.addLocator("30016-5 sb false", slaveId, 30016, (byte) 5);
            batchRead.addLocator("30016-6 sb false", slaveId, 30016, (byte) 6);
            batchRead.addLocator("30016-7 sb true", slaveId, 30016, (byte) 7);
            batchRead.addLocator("30016-8 sb true", slaveId, 30016, (byte) 8);
            batchRead.addLocator("30016-9 sb false", slaveId, 30016, (byte) 9);
            batchRead.addLocator("30016-a sb false", slaveId, 30016, (byte) 10);
            batchRead.addLocator("30016-b sb false", slaveId, 30016, (byte) 11);
            batchRead.addLocator("30016-c sb false", slaveId, 30016, (byte) 12);
            batchRead.addLocator("30016-d sb false", slaveId, 30016, (byte) 13);
            batchRead.addLocator("30016-e sb false", slaveId, 30016, (byte) 14);
            batchRead.addLocator("30016-f sb true", slaveId, 30016, (byte) 15);

            batchRead.addLocator("40017 sb -1968", slaveId, 40017, DataType.TWO_BYTE_INT_SIGNED);
            batchRead.addLocator("40018 sb -123456789", slaveId, 40018, DataType.FOUR_BYTE_INT_SIGNED);
            batchRead.addLocator("40020 sb -123456789", slaveId, 40020, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED);
            batchRead.addLocator("40022 sb 1968.1968", slaveId, 40022, DataType.FOUR_BYTE_FLOAT);
            batchRead.addLocator("40024 sb -123456789", slaveId, 40024, DataType.EIGHT_BYTE_INT_SIGNED);
            batchRead.addLocator("40028 sb -123456789", slaveId, 40028, DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED);
            batchRead.addLocator("40032 sb 1968.1968", slaveId, 40032, DataType.EIGHT_BYTE_FLOAT);

            batchRead.addLocator("30017 sb -1968 tc", slaveId, 30017, DataType.TWO_BYTE_INT_UNSIGNED);
            batchRead.addLocator("30018 sb -123456789 tc", slaveId, 30018, DataType.FOUR_BYTE_INT_UNSIGNED);
            batchRead.addLocator("30020 sb -123456789 tc", slaveId, 30020, DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED);
            batchRead.addLocator("30022 sb 1968.1968", slaveId, 30022, DataType.FOUR_BYTE_FLOAT_SWAPPED);
            batchRead.addLocator("30024 sb -123456789 tc", slaveId, 30024, DataType.EIGHT_BYTE_INT_UNSIGNED);
            batchRead.addLocator("30028 sb -123456789 tc", slaveId, 30028, DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED);
            batchRead.addLocator("30032 sb 1968.1968", slaveId, 30032, DataType.EIGHT_BYTE_FLOAT_SWAPPED);

            master.init();

            BatchResults<String> results = master.send(batchRead);

            System.out.println(results);
        }
        finally {
            master.destroy();
        }
    }
}
