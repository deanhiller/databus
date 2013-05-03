package controllers.api;

import java.nio.ByteBuffer;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;

public class DocumentUtil {

	public static StateDocument parseDocument(byte[] valueRaw) {
		ByteBuffer buffer = ByteBuffer.wrap(valueRaw);
		String state = readNextString(buffer);
		String user = readNextString(buffer);
		String description = readNextString(buffer);
		return new StateDocument(user, description, state);
	}
	
	private static String readNextString(ByteBuffer buffer) {
		int size = buffer.getInt();
		byte[] data = new byte[size];
		buffer.get(data);
		String value = StandardConverters.convertFromBytes(String.class, data);
		return value;
	}

	public static byte[] createDocument(String state, String user, String description) {
		int size = (state.length()+user.length()+description.length())*2;
		ByteBuffer buffer = ByteBuffer.allocate(size);
		putData(buffer, state);
		putData(buffer, user);
		putData(buffer, description);
		
		buffer.flip();
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		return data;
	}

	private static void putData(ByteBuffer buffer, String state) {
		byte[] data = StandardConverters.convertToBytes(state);
		buffer.putInt(data.length);
		buffer.put(data);
	}

	public static ColumnName parseName(byte[] namePrefix, byte[] nameRaw) {
		if(!startsWith(nameRaw, namePrefix))
			return null;
		int prefixLen = namePrefix.length;
		ByteBuffer buffer = ByteBuffer.wrap(nameRaw);
		buffer.position(prefixLen);
		long time = buffer.getLong();
		byte[] chars = new byte[buffer.remaining()];
		buffer.get(chars);
		String colName = new String(chars);
		return new ColumnName(time, colName);
	}
	
	private static boolean startsWith(byte[] nameRaw, byte[] namePrefix) {
		for(int i = 0; i < namePrefix.length; i++) {
			if(namePrefix[i] != nameRaw[i])
				return false;
		}
		
		return true;
	}

	public static byte[] combineName(byte[] namePrefix, long time, byte[] nameAsBytes) {
		int size = namePrefix.length+8+nameAsBytes.length;
		
		ByteBuffer buffer = ByteBuffer.allocate(size);
		buffer.put(namePrefix);
		buffer.putLong(time);
		buffer.put(nameAsBytes);
		
		buffer.flip();
		
		byte[] name = new byte[buffer.remaining()];
		buffer.get(name);
		return name;
	}

}
