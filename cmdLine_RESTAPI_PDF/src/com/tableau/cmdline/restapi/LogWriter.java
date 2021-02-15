package com.tableau.cmdline.restapi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

public class LogWriter extends Thread {

	private BlockingQueue<String> queue;
	
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public final static String stopRequested = "stopRequested";
	public final static String reportID = "reportID";

	BufferedWriter bw = null;
	FileWriter fw = null;

	public LogWriter(BlockingQueue<String> queue, String logFile) {
		this.queue = queue;
	
		try {
			File _logFile = new File(logFile);
			if (!_logFile.exists()) {
				_logFile.createNewFile();
			}
			this.fw = new FileWriter(_logFile.getAbsoluteFile(), true);
			this.bw = new BufferedWriter(fw);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	// runs and listens to values placed in to BlockingQueue
	@Override
	public void run() {

		try {

			while (true) {
				String _str = queue.take();

				if (_str != null && !_str.equals("End")) {
					// test to see if PDF created
				//	if (!pdfCreated(_str))
						bw.write(dateFormat.format(new Date()) + ": " + _str + "\n");//
						bw.flush();
				}
				if (_str != null && _str.equals("End")) {
					bw.write(dateFormat.format(new Date()) + ": " +"**************End******************\n");
					bw.flush();
					break;
				}
			}

		} catch (InterruptedException | IOException ex) {
			ex.printStackTrace();
		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}

	}


	private boolean pdfCreated(String _line) {
		String reportID="";
		if (_line.startsWith(LogWriter.reportID)) {
			 reportID = _line.substring(9);
		} else if (_line.endsWith(LogWriter.stopRequested)) {
			//WriteToLog("stopRequested", payLoad.get("id")+":stopRequested" );
			reportID=_line;
		}
		
		if (reportID.length()>0) {
			
			
			return true;
		}
		return false;
	}

}
