package com.tableau.cmdline.restapi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class PDFRunnerRESTAPI extends Thread {

	private String url;
	private String fileName;
	public boolean timeOutError = false;
	private static int timeoutAttempts = 4;
	private int attempts = 1;
	private final CountDownLatch doneSignal;
	private String startMessage;
	private BatchPDF batchPDF;
	
	private BlockingQueue<String> bq;
	

	public PDFRunnerRESTAPI( String url,String fileName, BlockingQueue<String> bq,  CountDownLatch doneSignal,String startMessage,BatchPDF batchPDF) {
		super();
		this.bq = bq;
		this.doneSignal = doneSignal;
		this.url=url;
		this.fileName=fileName;
		this.startMessage = startMessage;
		this.batchPDF=batchPDF;
	}

	@Override
	public void run() {
			
		if (codeToRun()) {
			doneSignal.countDown();
			return;
		}

		// run timeoutAttempts again, if we get gateway errors

		while (attempts < PDFRunnerRESTAPI.timeoutAttempts && timeOutError) {
			WriteToLog("Warning", Thread.currentThread().getName() + ">" + url
					+ ": Resubmitting server error attempt: " + attempts + " of " + (PDFRunnerRESTAPI.timeoutAttempts-1));
			//
			if (codeToRun()) {
				doneSignal.countDown();
				return;
			}
			attempts++;
		}
		doneSignal.countDown();
	
	}

	private boolean codeToRun() {
	try {
		
			if (attempts<2)
				WriteToLog(startMessage,"Info");
			else 
				WriteToLog("attempt: " + attempts +". " +startMessage,"Warning");
			
			TableauRest tr = new TableauRest();
			tr.getPDF(url, fileName);
			WriteToLog("PDF " + fileName + " created","Success");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			WriteToLog( "Failed to create: " + fileName +"  "  + e.getMessage(),"Error");
			return false;
		}

		return true;

	}

	
	

	public void WriteToLog(String message, String messageType) {
		try {
			bq.put(Thread.currentThread().getName()+ ": " + messageType + " " + message);
			batchPDF.writeToSysOut(Thread.currentThread().getName()+ ": " + messageType + " " + message);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
