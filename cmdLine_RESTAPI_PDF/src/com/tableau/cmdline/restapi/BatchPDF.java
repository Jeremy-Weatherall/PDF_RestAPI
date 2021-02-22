package com.tableau.cmdline.restapi;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tableau.cmdline.restapi.TableauRest.APIURL;



public class BatchPDF {


	
	private volatile boolean _ErrorStop = false;

	// holds properties file
	private static Properties s_properties = new Properties();

	private String _errors = "";

	public static void main(String[] args) throws Exception {
		
		
		BatchPDF _core = new BatchPDF();
		List<String> logList = Arrays.asList(
				"*******************Starting pdf generation using properties file: " + args[0] + "*******************");
		BlockingQueue<String> bq = new ArrayBlockingQueue<>(400, true, logList);

		LocalDateTime startTime = LocalDateTime.now();
		
		try {
			
			if (args.length == 1) {
				_core = new BatchPDF(args[0]);
			} else {
				_core.writeToLog("Pass in the location of the properties file",bq);
				return;
			}
			
			_core.writeToLog(String.format("Running for %s", args[0]),bq);
			
			String logFile = "PDFLog.log";
			if (_core.doesConfigFileHaveValue("logFile"))
				logFile=s_properties.getProperty("logFile");
			
			//using ArrayBlockingQueue so that multiple threads can write to log file
			LogWriter lw = new LogWriter(bq, logFile);
			lw.start();
			
			_core.checkPropValues();
		
			
			
			try {
				//sign on to site
				TableauServerLogonTokens tslt = new TableauServerLogonTokens(s_properties.getProperty("login.user"), s_properties.getProperty("login.password"), 
						s_properties.getProperty("server.url"), s_properties.getProperty("server.site"));
				
				TableauRest tr= new TableauRest(tslt,bq);
				
				tr.login();
				
				_core.writeToLog("Logged in to server: " + s_properties.getProperty("server.url") +", site: " + s_properties.getProperty("server.site"),bq);
				
				/*
				 * Get data to iterate over
				 * View and workbook names are not unique, but content URL is 
				 * Get workbookID using  contentURL
				 * Using workbook ID, get the view ID
				 * Using the view ID, get data back from this view
				 */
				tslt.setViewWorkbookID(tr.getWorkbookID(s_properties.getProperty("csv.url.workbook.name")));
				
				tslt.setViewID(tr.getViewID(s_properties.getProperty("csv.url.view")));
				//now get the data for this view
				String[] values = tr.getViewData();
				_core.writeToLog("There are " + (values.length-1) + " values to iterate over",bq);
				
				//now we need to iterate over list of values and generate PDF for each one
			
				/*
				 * Get dashboard to generate PDF for
				 * View and workbook names are not unique, but content URL is 
				 * Get workbookID using  contentURL
				 * Using workbook ID, get the view ID
				 * Use the view ID to generate PDF
				 */
				
				tslt.setViewWorkbookID(tr.getWorkbookID(s_properties.getProperty("pdf.url.workbook.name")));
				tslt.setViewID(tr.getViewID(s_properties.getProperty("pdf.url.view")));
				tslt.setViewURLpdfFilterColumn((s_properties.getProperty("pdf.Filter.Column")));
				
			    /*
			     * Get page layout details
			     */
			   
				if (_core.doesConfigFileHaveValue("page.layout"))
					tslt.setPageLayout(s_properties.getProperty("page.layout"));
				
				if (_core.doesConfigFileHaveValue("page.size"))
					tslt.setPageSize(s_properties.getProperty("page.size"));
				
			
				String localURL = APIURL.api_getViewPDF.getUrl();//  ("sites/{0}/views/{1}/pdf?type={2}&orientation={3}&vf_{4}={5}")
				
				localURL = MessageFormat.format(localURL, tslt.getSiteID(),tslt.getViewID(), tslt.getPageSize(),
						tslt.getPageLayout(),  URLEncoder.encode(s_properties.getProperty("pdf.Filter.Column"),StandardCharsets.UTF_8.toString())  );
				
				localURL= tslt.getUrlAndAPI() + localURL; 
				
				//see how many threads we should be using
				//max 10, min 1
				if (_core.doesConfigFileHaveValue("concurrent.requests"))
					tslt.setConcurrentThreads(Integer.parseInt(s_properties.getProperty("concurrent.requests")));
				
				//set up CountDownLatch with number of reports to run
				/*
				  CountDownLatch has a counter field, which we decrement as each PDF is created. We can then use it to block a calling thread until it's been counted down to zero.
				 */
				CountDownLatch doneSignal = new CountDownLatch(values.length-1);
				
				//number of threads to use
				ExecutorService executor = Executors.newFixedThreadPool(tslt.getConcurrentThreads());
				
				List<String> list = new ArrayList<>(Arrays.asList(values));
				
				Iterator<String> hmIterator = list.iterator();
				Integer counter=0;
				while (hmIterator.hasNext()) {
					String filter=(String) hmIterator.next();
					//first value is column name, ignore
					if (counter>0) {
						//check \r is not on the end of the filter value (not sure why this happens, only seen it on self hosted server)
						filter = filter.replaceAll("\\R$", "");  
						String pdfURL=localURL+URLEncoder.encode(filter,StandardCharsets.UTF_8.toString());
						String fileName = s_properties.getProperty("file.Output")+ URLEncoder.encode(filter,StandardCharsets.UTF_8.toString())+".pdf";
						String startMessage="Requesting PDF " + counter + " of " + (values.length-1) + " " +  URLEncoder.encode(filter,StandardCharsets.UTF_8.toString())+".pdf";
					
						PDFRunnerRESTAPI worker = new PDFRunnerRESTAPI(pdfURL,fileName, bq, doneSignal, startMessage,_core);
						executor.execute(worker);
					}
					counter++;
				}
				try {
					doneSignal.await();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				executor.shutdown();
				
				
			} catch (Error e) {
				// TODO Auto-generated catch block
				_core.writeToLog(e.toString(),bq);
			}
			
			

		} catch (final Exception e) {
			_core.writeToLog(e.toString(),bq);
		}
		
		LocalDateTime endTime = LocalDateTime.now();
		long hours = ChronoUnit.HOURS.between(startTime, endTime);
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime) -(hours*60);
        long seconds = ChronoUnit.SECONDS.between(startTime, endTime)-((hours*60) + (minutes*60));
		_core.writeToLog("Duration: "+ hours+ "h: " + minutes + "m: " + seconds +"s",bq);
		
		_core.writeToLog("End",bq);
	
	}

	public BatchPDF() {
	}

	private  void writeToLog(String str,BlockingQueue<String> bq) throws InterruptedException {
		System.out.println(str);
		bq.put(str);
		
	}
	
	public BatchPDF(String propFileLocation) throws Exception {
		// Loads the values from configuration file into the Properties instance
		try {
			s_properties.load(new FileInputStream(propFileLocation.trim()));

		} catch (IOException e) {
			throw new Exception(String.format("Failed to load configuration file %s. Error message: %s",
					propFileLocation, e.toString()));
		}

	}
	
	
	
	/**
	 * this called to see what the property file needs us to do, and we check to see
	 * if we have enough data
	 * 
	 * @return
	 * @throws Exception
	 */

	private boolean checkPropValues() throws Exception {

		if (doesConfigFileHaveValue("login.user") != true) {
			_errors = "Unable to find logon User name in properties file.\n";
		}
		if (doesConfigFileHaveValue("login.password") != true) {
			_errors += "Unable to find logon password in properties file.\n";
		}
	
		if (doesConfigFileHaveValue("csv.url.view") != true) {
			_errors += "Unable to find URL for CSV file download in properties file. See: 'csv.url.view'\n";
		}
		if (doesConfigFileHaveValue("pdf.Filter.Column") != true) {
			_errors += "Unable to find the column that we are using as a filter. See: 'pdf.Filter.Column'\n";
		}
		
		if (doesConfigFileHaveValue("pdf.url.view") != true) {
			_errors += "Unable to find worksheet name to genereate PDFs for in properties file. See: 'pdf.url.view'\n";
		}
		
		if (doesConfigFileHaveValue("server.url") != true) {
			_errors += "Unable to find Tableau Server URL in properties file. See: 'server.url'\n";
		}
		if (doesConfigFileHaveValue("server.site") != true) {
			_errors += "Unable to find the Site name in properties file. See: 'server.site'\n";
		}
		if (doesConfigFileHaveValue("file.Output") != true) {
			_errors += "Unable to find the output location for PDF files in the properties file. See: 'file.Output'\n";
		}
		
		
		
		
			if (_errors.length() > 0)
			throw new Exception(_errors);

		else
			return true;

	}

	private boolean doesConfigFileHaveValue(String _key) {

		if (s_properties.getProperty(_key) == null || (s_properties.getProperty(_key).trim().equals("")&& !_key.equals("server.site")))
			return false;
		return true;

	}


	public synchronized boolean getErrorSet() {
		return _ErrorStop;
	}

	public synchronized void setError(boolean _err) {
		this._ErrorStop = _err;
	}
	
	//Use this as writing to console does not work when coming from threads running PDF, so have them write back through here
	public synchronized void writeToSysOut(String msg ) {
		System.out.println(msg);
	}
}
