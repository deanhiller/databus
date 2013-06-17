package gov.nrel.consumer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataPointWriter {

	private static final Logger log = Logger.getLogger(DataPointWriter.class.getName());
	
	private DatabusSender sender;
	private FileWriter jsonw;
	private int counter = 0;
	
	public DataPointWriter(DatabusSender sender) {
		this.sender = sender;
	}
	
	public DatabusSender getSender() {
		return sender;
	}
	
	public synchronized void addDataPoint(String json) {
		try {
			if(jsonw == null || counter > 5000) {
				newFile();
			}
			jsonw.write(json+"\n");
			counter++;
		} catch (Exception e) {
			log.log(Level.WARNING, "Error opening output file", e);
		}
	}

	private void newFile() throws IOException {
		if(jsonw != null) {
			jsonw.close();
		}
		
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss.SSS");
		String time = fmt.format(new Date());
		
		File f = new File("../logs/dataPoints"+time+".json");
		f.createNewFile();
		jsonw = new FileWriter(f, true);
		counter = 0;
	}
}
