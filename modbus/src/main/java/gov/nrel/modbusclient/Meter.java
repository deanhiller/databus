/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nrel.modbusclient;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author abeach
 */
public class Meter {

    private static final Map<String, Integer> regTypeMap = new HashMap<String, Integer>();

    static {
        regTypeMap.put("BINARY", 1);
        regTypeMap.put("TWO_BYTE_INT_UNSIGNED", 2);
        regTypeMap.put("TWO_BYTE_INT_SIGNED", 3);
        regTypeMap.put("FOUR_BYTE_INT_UNSIGNED", 4);
        regTypeMap.put("FOUR_BYTE_INT_SIGNED", 5);
        regTypeMap.put("FOUR_BYTE_INT_UNSIGNED_SWAPPED", 6);
        regTypeMap.put("FOUR_BYTE_INT_SIGNED_SWAPPED", 7);
        regTypeMap.put("FOUR_BYTE_FLOAT", 8);
        regTypeMap.put("FOUR_BYTE_FLOAT_SWAPPED", 9);
        regTypeMap.put("EIGHT_BYTE_INT_UNSIGNED", 10);
        regTypeMap.put("EIGHT_BYTE_INT_SIGNED", 11);
        regTypeMap.put("EIGHT_BYTE_INT_UNSIGNED_SWAPPED", 12);
        regTypeMap.put("EIGHT_BYTE_INT_SIGNED_SWAPPED", 13);
        regTypeMap.put("EIGHT_BYTE_FLOAT", 14);
        regTypeMap.put("EIGHT_BYTE_FLOAT_SWAPPED", 15);
        regTypeMap.put("TWO_BYTE_BCD", 16);
        regTypeMap.put("FOUR_BYTE_BCD", 17);
    }
    private String serial;
    private String name;
    private String building;
    private String ip;
    private String model;
    private String phenomena;
    private String zone;
    private Integer slave;

    public Meter(String serial, String model, String name, String building,
            String phenomena, String zone, String ip, String slave) {
        this.serial = serial;
        this.name = name;
        this.building = building;
        this.ip = ip;
        this.model = model;
        this.phenomena = phenomena;
        this.zone = zone;
        this.slave = new Integer(slave.split(" ")[1]);
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhenomena() {
        return phenomena;
    }

    public void setPhenomena(String phenomena) {
        this.phenomena = phenomena;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getSlave() {
        return "Slave " + slave;
    }

    public void setSlave(String slave) {
        this.slave = new Integer(slave.split(" ")[1]);
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public long read(ModbusMaster master, JSONObject register) throws JSONException {
        master.setTimeout(2000);
        master.setRetries(0);

        int regNumber = register.getInt("register");
        int regType = regTypeMap.get(register.getString("type"));
        Double regModifier = register.getDouble("modifier");

        try {
            Double reading = 0d;
            Object val = master.getValue(this.slave, 4,
                    regNumber, regType);
            if(val.getClass().equals(Short.class)) {
                reading = ((Short)val).doubleValue();
            } else if(val.getClass().equals(Integer.class)) {
                reading = ((Integer)val).doubleValue();
            } else if(val.getClass().equals(Float.class)) {
                reading = ((Float)val).doubleValue();
            }
            
            return new Double(regModifier * reading).longValue();
        } catch (ModbusTransportException ex) {
            return Integer.MAX_VALUE;
        } catch (ErrorResponseException ex) {
            System.out.println(this.model + ","
                    + this.serial + "," + this.ip + "\n" + ex.getLocalizedMessage());
        }
        
        return Integer.MIN_VALUE;
    }

    // return watts
    public long read(ModbusMaster master) {
        try {
            if (this.model.startsWith("GEP")) {
                master.setTimeout(2000);
                master.setRetries(0);
                return 10l * new Long((Integer) master.getValue(this.slave, 4, 752,
                        DataType.FOUR_BYTE_INT_SIGNED));
            } else if (this.model.equals("GE750")) {
                master.setTimeout(2000);
                master.setRetries(0);
                Short realPower = ((Short) master.getValue(this.slave, 4, 784,
                        DataType.TWO_BYTE_INT_SIGNED));
                Short multiplier = ((Short) master.getValue(this.slave, 4, 790,
                        DataType.TWO_BYTE_INT_SIGNED));
                return 1000l * new Long(realPower * multiplier);
            } else if (this.model.equals("GE489")) {
                master.setTimeout(2000);
                master.setRetries(0);

                // No GE489 meter not showing current?
                return Long.MAX_VALUE;

                /*return (Integer) master.getValue(this.slave, 3, 1089,
                DataType.TWO_BYTE_INT_UNSIGNED);*/
            } else if (this.model.equals("GERMS9D")) {
                master.setTimeout(2000);
                master.setRetries(0);
                return new Float(1000f * (Float) master.getValue(this.slave, 4, 1028,
                        DataType.FOUR_BYTE_FLOAT_SWAPPED)).longValue();
            }
        } catch (ModbusTransportException ex) {
            return Integer.MAX_VALUE;
        } catch (ErrorResponseException ex) {
            System.out.println(this.model + ","
                    + this.serial + "," + this.ip + "\n" + ex.getLocalizedMessage());
        }

        return Integer.MIN_VALUE;
    }
}
