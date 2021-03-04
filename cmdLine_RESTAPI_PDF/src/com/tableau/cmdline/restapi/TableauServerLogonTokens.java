package com.tableau.cmdline.restapi;

public class TableauServerLogonTokens {

	private String tokenID="";
	private String siteID;
	private String userID;
	private String viewID;
	private String viewWorkbookID;
	private boolean stopRequested = false;
	private String userName;
	private String userPassword;
	private String url;
	private String site="";
	private String urlAndAPI;
	private String apiVersion="/api/3.7/";
	private String viewURLpdfFilterColumn="";
	private String pageLayout= "portrait";
	private String pageSize= "Legal";
	private int concurrentThreads=1;
	
	public TableauServerLogonTokens(String userName, String userPassword, String url, String site) {
		super();
		this.userName = userName;
		
		if (userPassword.startsWith("\""))
			userPassword = userPassword.substring(1);
		if (userPassword.endsWith("\""))
			userPassword = userPassword.substring(0,userPassword.length()-1);
		this.userPassword = userPassword;
		setServerURL(url);
		if (site.toLowerCase().equals("default"))
			site="";
		this.site = site;
		
	}
	
	
	public String getViewURLpdfFilterColumn() {
		return viewURLpdfFilterColumn;
	}


	public void setViewURLpdfFilterColumn(String viewURLpdfFilterColumn) {
		this.viewURLpdfFilterColumn = viewURLpdfFilterColumn;
	}


	public void setServerURL(String serverURL) {
		this.url = serverURL;
		createUrlAndAPI();
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
		createUrlAndAPI();
	}

	private void createUrlAndAPI() {

		if (url.length() > 0 && apiVersion.length() > 0)
			urlAndAPI = url + apiVersion;

	}
	
	
	public String getUrlAndAPI() {
		return urlAndAPI;
	}

	public void setUrlAndAPI(String urlAndAPI) {
		this.urlAndAPI = urlAndAPI;
	}
	

	public boolean isStopRequested() {
		return stopRequested;
	}


	public void setStopRequested(boolean stopRequested) {
		this.stopRequested = stopRequested;
	}


	public TableauServerLogonTokens() {
		super();
	}
	public String getTokenID() {
		return tokenID;
	}
	public void setTokenID(String tokenID) {
		this.tokenID = tokenID;
	}
	public String getSiteID() {
		return siteID;
	}
	public void setSiteID(String siteID) {
		this.siteID = siteID;
	}
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getViewID() {
		return viewID;
	}
	public void setViewID(String viewID) {
		this.viewID = viewID;
	}
	public String getViewWorkbookID() {
		return viewWorkbookID;
	}
	public void setViewWorkbookID(String viewWorkbookID) {
		this.viewWorkbookID = viewWorkbookID;
	}


	public String getUserName() {
		return userName;
	}


	public String getUserPassword() {
		return userPassword;
	}


	public String getUrl() {
		return url;
	}


	public String getSite() {
		return site;
	}


	public String getPageLayout() {
		return pageLayout;
	}


	public void setPageLayout(String pageLayout) {
		this.pageLayout = pageLayout;
	}


	public String getPageSize() {
		return pageSize;
	}


	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}


	public int getConcurrentThreads() {
		return concurrentThreads;
	}


	public void setConcurrentThreads(int concurrentThreads) {
		if (concurrentThreads>10)
				concurrentThreads=10;
		if (concurrentThreads<1)
				concurrentThreads=1;
			
		this.concurrentThreads = concurrentThreads;
	}
	
	
	
}
