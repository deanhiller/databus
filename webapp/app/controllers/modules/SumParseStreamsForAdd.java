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

public class SumParseStreamsForAdd {

	public static void main(String[] args) throws IOException {
		File f = new File("/Users/dhiller2/AAROOT/area1/sumbug");
		File[] listFiles = f.listFiles();
		for(File child : listFiles) {
			String name = child.getName();
			if(name.endsWith(".txt")) {
				processFile(f, child);
			}
		}
	}

	private static void processFile(File parent, File child) throws IOException {
		
		Reader reader = new FileReader(child);
		BufferedReader r = new BufferedReader(reader );
		
		File f = new File(parent, "p"+child.getName()+"2");
		if(f.exists()) {
			f.delete();
		}
		
		f.createNewFile();
		Writer writer = new FileWriter(f);

		String all = r.readLine();
		r.close();
		
		all = all.replace("{\"data\":[{", "");
		all = all.replace("}],\"error\":[]}", "");
		
		String[] allJson = all.split("\\},\\{");
		
		for(String line : allJson) {
			line = line.replace("\"time\":", "");
			line = line.replace(",\"value\":", "=");
			writer.write(line+"\n");
		}

		writer.flush();
		writer.close();
	}
}
