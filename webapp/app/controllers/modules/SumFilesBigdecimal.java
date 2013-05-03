package controllers.modules;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class SumFilesBigdecimal {

	public static void main(String[] args) throws IOException {
		File directory = new File("/Users/jcollins/AAROOT/area1/sumbug");
		File[] listFiles = directory.listFiles();
		List<File> files = new ArrayList<File>();
		for(File child : listFiles) {
			String name = child.getName();
			if(name.endsWith(".txt2") && name.startsWith("pagg")) {
				files.add(child);
			}
		}
		
		List<BufferedReader> readers = new ArrayList<BufferedReader>();
		for(File f : files) {
			Reader reader = new FileReader(f);
			BufferedReader r = new BufferedReader(reader );
			readers.add(r);
		}
		
		
		File f = new File(directory, "pgeneratedsumBD.txt2");
		if(f.exists()) {
			f.delete();
		}
		
		f.createNewFile();
		Writer writer = new FileWriter(f);
		
		while(processLine(readers, writer)) {
		}
		
		writer.flush();
		writer.close();
		
		for(BufferedReader r : readers) {
			r.close();
		}
	}

	private static boolean processLine(List<BufferedReader> readers, Writer writer) throws IOException {

		long firstTime = 0;
		BigDecimal total = new BigDecimal("0.0");
		total.setScale(20);
		for(int i = 0; i < readers.size(); i++) {
			BufferedReader r = readers.get(i);
			TimeValBigDecimal tv = readTimeValue(r);
			if(tv == null)
				return false; //stop processing
			
			if(i == 0) {
				firstTime = tv.getTime();
			} else if(tv.getTime() != firstTime){
				throw new RuntimeException("bug in files as times don't line up");
			}
			
			BigDecimal d = tv.getValue();
			if (tv.getTime()==1357747894259l)
				System.out.println(""+total+"+"+d+"="+total.add(d, new MathContext(20, RoundingMode.UNNECESSARY)));
			total = total.add(d, new MathContext(20, RoundingMode.UNNECESSARY));
		}

		writer.write(firstTime+"="+total+"\n");
		return true;
	}

	private static TimeValBigDecimal readTimeValue(BufferedReader r) throws IOException {
		String line = r.readLine();
		if(line == null)
			return null;
		String[] pieces = line.split("=");
		TimeValBigDecimal v = new TimeValBigDecimal();
		long time = Long.parseLong(pieces[0]);
		v.setTime(time);
		if (time == 1357747894259l)
		System.out.println("creating a new BD from "+pieces[1]);
		BigDecimal val = new BigDecimal(pieces[1]);
		v.setValue(val);
		
		return v;
	}

}
