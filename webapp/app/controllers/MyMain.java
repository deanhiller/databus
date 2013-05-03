package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMain {

	private static final Logger log = LoggerFactory.getLogger(MyMain.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File f = new File("/Users/dhiller2/Downloads/file2");
		
		FileReader reader = new FileReader(f);
		BufferedReader r = new BufferedReader(reader);
		int counter = 0;
		String readLine = null;
		while(true) {
			readLine = r.readLine();
			if(readLine == null)
				break;

			String timestamp = readLine.substring(4);

			counter++;
			long time = Long.parseLong(timestamp);
			LocalDateTime t = new LocalDateTime(time);
			if (log.isInfoEnabled())
				log.info("date="+t+" total="+counter);
		} 
	}

}
