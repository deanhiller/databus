package misc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class GenerateCsv {

	public static void main(String[] args) throws IOException {
		File f = new File("/Users/jcollins/acsvfile2.csv");
		if(f.exists())
			f.delete();
		
		FileWriter writer = new FileWriter(f);

		writer.write("time, value  \n");
		for(int i = 0; i < 100000; i++) {
			writer.write(i+", "+(i+5)+"\n");
		}
		writer.close();
	}
}
