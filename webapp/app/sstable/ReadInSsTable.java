package sstable;

import static org.apache.cassandra.utils.ByteBufferUtil.bytesToHex;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;

public class ReadInSsTable {

	private static Map<Key, String> keyToFirstFile = new HashMap<Key, String>();

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
		SSTableIdentityIterator row;
        SSTableScanner scanner = reader.getDirectScanner();
        
        int counter = 0;
        // collecting keys to export
        while (scanner.hasNext())
        {
            row = (SSTableIdentityIterator) scanner.next();
            String currentKey = bytesToHex(row.getKey().key);
            serializeRow(row, row.getKey(), out, fileName, c);
            c.increment();
            counter++;
            if(c.getRowCount() % 5000 == 0)
            	out.println("total row count="+c.getRowCount());
        }

        out.println("This file="+fileName+" rowcount="+counter+" dataPtCnt="+c.getDataPointCounter()+" null count="+c.getNullCounter());
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
		if(bytesId[0] == 1) {
			id = StandardConverters.convertFromBytes(Long.class, bytesId);
		} else {
			id = new String(bytesId);
		}

		Key k = new Key(virtTableName, id);
		if(id instanceof Long)
			serializeColumns(row, out, comparator, cfMetaData, k, c);
		

		String file = keyToFirstFile.get(k);
//		if(file != null) {
//			if(!"Wind".equals(virtTableName) && !"DataBusMon".equals(virtTableName))
//				out.println("Found key in another file="+file+" this file="+fileName+" key="+k);
//		} else {
//			keyToFirstFile.put(k, fileName);
//		}
		
		//out.println("table="+virtTableName+" timestamp="+id);
    }

	private static void serializeColumns(Iterator<OnDiskAtom> columns, PrintStream out, AbstractType<?> comparator, CFMetaData cfMetaData, Key k, RowCounter c) {
        while (columns.hasNext())
        {
            serializeColumn(columns.next(), comparator, cfMetaData, k, out, c);
        }		
	}
	
    private static List<Object> serializeColumn(OnDiskAtom column, AbstractType<?> comparator, CFMetaData cfMetaData, Key k, PrintStream out, RowCounter c)
    {
        if (column instanceof IColumn)
        {
            return serializeColumn((IColumn)column, comparator, cfMetaData, out, k, c);
        }
        else
        {
        	out.println("range tombstone");
            assert column instanceof RangeTombstone;
            RangeTombstone rt = (RangeTombstone)column;
            ArrayList<Object> serializedColumn = new ArrayList<Object>();
            serializedColumn.add(comparator.getString(rt.min));
            serializedColumn.add(comparator.getString(rt.max));
            serializedColumn.add(rt.data.markedForDeleteAt);
            serializedColumn.add("t");
            serializedColumn.add(rt.data.localDeletionTime);
            return serializedColumn;
        }
    }
    
    private static List<Object> serializeColumn(IColumn column, AbstractType<?> comparator, CFMetaData cfMetaData, PrintStream out, Key k, RowCounter c)
    {
        ArrayList<Object> serializedColumn = new ArrayList<Object>();

        ByteBuffer name = ByteBufferUtil.clone(column.name());
        ByteBuffer value = ByteBufferUtil.clone(column.value());

        c.incrementPoint();
        if(!value.hasRemaining()) {
        	c.incrementNullFound();
//        	byte[] data = new byte[name.remaining()];
//        	name.get(data);
//        	Long time = StandardConverters.convertFromBytes(Long.class, data);
//        	out.println("key="+k+" has null value at time="+time);
        }
        
        
        serializedColumn.add(comparator.getString(name));
        if (column instanceof DeletedColumn)
        {
            serializedColumn.add(ByteBufferUtil.bytesToHex(value));
        }
        else
        {
            AbstractType<?> validator = cfMetaData.getValueValidator(cfMetaData.getColumnDefinitionFromColumnName(name));
            serializedColumn.add(validator.getString(value));
        }
        serializedColumn.add(column.timestamp());

        if (column instanceof DeletedColumn)
        {
            serializedColumn.add("d");
        }
        else if (column instanceof ExpiringColumn)
        {
            serializedColumn.add("e");
            serializedColumn.add(((ExpiringColumn) column).getTimeToLive());
            serializedColumn.add(column.getLocalDeletionTime());
        }
        else if (column instanceof CounterColumn)
        {
            serializedColumn.add("c");
            serializedColumn.add(((CounterColumn) column).timestampOfLastDelete());
        }

        return serializedColumn;
    }
}
