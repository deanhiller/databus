package sstable;

import static org.apache.cassandra.utils.ByteBufferUtil.bytesToHex;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamily;
import org.apache.cassandra.db.CounterColumn;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletedColumn;
import org.apache.cassandra.db.ExpiringColumn;
import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.db.OnDiskAtom;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.SuperColumn;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTableIdentityIterator;
import org.apache.cassandra.io.sstable.SSTableReader;
import org.apache.cassandra.io.sstable.SSTableScanner;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Hex;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;

public class ReadInSsTable {

	private static Map<String, BigDecimal> lastValues = new HashMap<String, BigDecimal>();
	private static Map<Key, List<RowFrag>> rowFrags = new HashMap<Key, List<RowFrag>>();
	private static Set<String> unchangingTables = new HashSet<String>();
	private static Map<Key, String> keyToFile = new HashMap<Key, String>();
	
	private final static byte[] charToByte = new byte[256];
    // package protected for use by ByteBufferUtil. Do not modify this array !!
    static final char[] byteToChar = new char[16];
    static
    {
        for (char c = 0; c < charToByte.length; ++c)
        {
            if (c >= '0' && c <= '9')
                charToByte[c] = (byte)(c - '0');
            else if (c >= 'A' && c <= 'F')
                charToByte[c] = (byte)(c - 'A' + 10);
            else if (c >= 'a' && c <= 'f')
                charToByte[c] = (byte)(c - 'a' + 10);
            else
                charToByte[c] = (byte)-1;
        }

        for (int i = 0; i < 16; ++i)
        {
            byteToChar[i] = Integer.toHexString(i).charAt(0);
        }
    }
    
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String directory = "/Users/dhiller2/AAROOT/area1/datafiles";
		System.setProperty("cassandra.config", "file:///Users/dhiller2/AAROOT/cassandra-1.2.2/conf/cassandra.yaml");
		File dir = new File(directory);

		DatabaseDescriptor.loadSchemas();
		PrintStream out = System.out;
		File[] files = dir.listFiles();
		RowCounter c = new RowCounter();
		for(File f : files) {
			if(f.getName().endsWith("Data.db")) {
				out.println("processing sstable="+f.getAbsolutePath());
				process(f.getAbsolutePath(), out, f.getName(), c);
			}
		}
		out.println("finished");
		System.exit(0);
	}

	private static void process(String name, PrintStream out, String fileName, RowCounter c) throws IOException {
		Descriptor descriptor = Descriptor.fromFilename(name);
		SSTableReader reader = SSTableReader.open(descriptor);
        SSTableScanner scanner = reader.getDirectScanner();
        
        int counter = 0;
        // collecting keys to export
        while (scanner.hasNext())
        {
        	SSTableIdentityIterator row = (SSTableIdentityIterator) scanner.next();
            String currentKey = bytesToHex(row.getKey().key);
            serializeRow(row, row.getKey(), out, fileName, c);
            c.increment();
            counter++;
            if(c.getRowCount() % 5000 == 0)
            	out.println("total row count so far="+c.getRowCount());
        }

        out.println("This file="+fileName+" rowcount="+counter+" dataPtCnt="+c.getDataPointCounter()+" null count="+c.getNullCounter()+" table cnt="+lastValues.size()+" unchangedstreams="+unchangingTables.size());
        scanner.close();
	}

    private static void serializeRow(SSTableIdentityIterator row, DecoratedKey keyData, PrintStream out, String fileName, RowCounter c)
    {
        ColumnFamily columnFamily = row.getColumnFamily();
        boolean isSuperCF = columnFamily.isSuper();
        CFMetaData cfMetaData = columnFamily.metadata();
        AbstractType<?> comparator = columnFamily.getComparator();

		byte[] key = new byte[keyData.key.remaining()];
		keyData.key.get(key);
		if (key.length==0) {
			out.println("GOT A KEY THAT IS SIZE 0!!  WHAT DOES THAT MEAN?");
			return;
		}
    	String virtTableName = DboColumnIdMeta.fetchTableNameIfVirtual(key);
		int tableLen = key[0] & 0xFF;
		byte[] bytesId = Arrays.copyOfRange(key, 1+tableLen, key.length);
		Object id;
		//This is a HUGE hack that doesn't even work since there are some table name strings that start with first byte == 1
		if(bytesId[0] == 1) {
			id = StandardConverters.convertFromBytes(Long.class, bytesId);
		} else {
			id = new String(bytesId);
		}

		keyData.key.rewind();
		String keyStr = bytesToHex(keyData.key);
		//This is a HUGE hack that doesn't even work since there are some table name strings that start with first byte == 1
		Key k = new Key(virtTableName, id, keyStr);
		//This is a HUGE hack that doesn't even work since there are some Strings that start with first byte == 1
		if(id instanceof Long) {
			out.print(k.getTableName()+"-"+k.getRowKey()+"=[");
			serializeColumns(row, out, comparator, cfMetaData, k, c, fileName);
			out.println("]");
		}
		

//		String name = keyToFile.get(k);
//		if(name != null) {
//			out.println("dup key="+k+" orig file="+name+" filenow="+fileName);
//		}
//		keyToFile.put(k, fileName);
//		String file = keyToFirstFile.get(k);
//		if(file != null) {
//			if(!"Wind".equals(virtTableName) && !"DataBusMon".equals(virtTableName))
//				out.println("Found key in another file="+file+" this file="+fileName+" key="+k);
//		} else {
//			keyToFirstFile.put(k, fileName);
//		}
		
		//out.println("table="+virtTableName+" timestamp="+id);
    }

    public static String bytesToHex(ByteBuffer bytes)
    {
        final int offset = bytes.position();
        final int size = bytes.remaining();
        final char[] c = new char[size * 2];
        for (int i = 0; i < size; i++)
        {
            final int bint = bytes.get(i+offset);
            c[i * 2] = byteToChar[(bint & 0xf0) >> 4];
            c[1 + i * 2] = byteToChar[bint & 0x0f];
        }
        return Hex.wrapCharArray(c);
    }

	private static void serializeColumns(Iterator<OnDiskAtom> columns, PrintStream out, AbstractType<?> comparator, CFMetaData cfMetaData, Key k, RowCounter c, String fileName) {
		RowFrag frag = new RowFrag(fileName);
        while (columns.hasNext())
        {
            String col = serializeColumn(columns.next(), comparator, cfMetaData, k, out, c);
            frag.addColumn(col);
        }
        
        if("bacnet8400BinaryValue9".equals(k.getTableName())) {
	        List<RowFrag> list = rowFrags.get(k);
	        if(list == null) {
	        	list = new ArrayList<RowFrag>();
	        	rowFrags.put(k, list);
	        }
	        list.add(frag);
	        
	        if(list.size() >= 2) {
	        	logRow(k, list, out);
	        }
        }
	}
	
    private static void logRow(Key k, List<RowFrag> list, PrintStream out) {
		out.println(k.getTableName()+"-"+k.getRowKey()+"=");
		for(RowFrag frag : list) {
			out.println("\t"+frag);
		}
		out.println();
	}

	private static String serializeColumn(OnDiskAtom column, AbstractType<?> comparator, CFMetaData cfMetaData, Key k, PrintStream out, RowCounter c)
    {
        if (column instanceof IColumn)
        {
            return serializeColumn((IColumn)column, comparator, cfMetaData, out, k, c);
        }
        else
        {
        	out.print("range tombstone");
            assert column instanceof RangeTombstone;
            RangeTombstone rt = (RangeTombstone)column;
            ArrayList<Object> serializedColumn = new ArrayList<Object>();
            serializedColumn.add(comparator.getString(rt.min));
            serializedColumn.add(comparator.getString(rt.max));
            serializedColumn.add(rt.data.markedForDeleteAt);
            serializedColumn.add("t");
            serializedColumn.add(rt.data.localDeletionTime);
            return "range tombstone";
        }
    }
    
    private static String serializeColumn(IColumn column, AbstractType<?> comparator, CFMetaData cfMetaData, PrintStream out, Key k, RowCounter c)
    {
        ByteBuffer name = ByteBufferUtil.clone(column.name());
        ByteBuffer value = ByteBufferUtil.clone(column.value());

    	byte[] d1 = new byte[name.remaining()];
    	name.get(d1);
    	BigInteger time = StandardConverters.convertFromBytes(BigInteger.class, d1);
    	String nameStr = StandardConverters.convertFromBytes(String.class, d1);
        String columnStr = time+":";
        c.incrementPoint();
        if(!value.hasRemaining()) {
        	c.incrementNullFound();
        	columnStr+="null";
        } else {
        	
        	byte[] data = new byte[value.remaining()];
        	value.get(data);
        	try {
	        	BigDecimal val = StandardConverters.convertFromBytes(BigDecimal.class, data);
	        	BigDecimal dec = lastValues.get(k.getTableName());
	        	if(dec == null) {
	        		unchangingTables.add(k.getTableName());
	        		lastValues.put(k.getTableName(), val);
	        	} else if(dec.compareTo(val) != 0){
	        		unchangingTables.remove(k.getTableName());
	        	}
	        	columnStr+=val;
	        	out.print(time+":"+val+",");
        	} catch(Exception e) {
        		//e.printStackTrace();
        	}
        }

        return columnStr;
    }
}
