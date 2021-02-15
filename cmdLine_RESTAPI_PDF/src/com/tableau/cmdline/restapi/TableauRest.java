package com.tableau.cmdline.restapi;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.BlockingQueue;

import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;




public class TableauRest {

	

	private static BlockingQueue<String> bq;
	private static TableauServerLogonTokens tslt;
	
	private final String TABLEAU_AUTH_HEADER = "X-Tableau-Auth";

	
	public enum APIURL {
		api_signin("auth/signin"), 
		api_signinBody("'{\"credentials\": {\"name\": \"'{0}\",\"password\": \"{1}'\", " + " \"site\": {\r\n"
				+ "\"contentUrl\": \"\'{2}\"'}}}'"),
		apiURL_site_getSpecificView("sites/{0}/views?filter=contentUrl:eq:{1}"),
		api_FileUploadInitiate("sites/{0}/fileUploads"), 
		api_FileUploadAppendTo("sites/{0}/fileUploads/{1}"),
		api_FileUploadFinishAppendTo("sites/{0}/datasources?uploadSessionId={1}&datasourceType=hyper&overwrite=true"),
		api_publishDatasource("sites/{0}/datasources?overwrite=true"),
		api_getProjectID("sites/{0}/projects?filter=name:eq:{1}"), 
		api_getSite("sites/{0}"), api_getSites("sites"),
		api_Paging("?pageSize=100&pageNumber={0}"), 
		api_Paging_amp("&pageSize=100&pageNumber={0}"),
		api_siteGetUsers("sites/{0}/users"), 
		api_siteGetWorkbooks("/sites/{0}/workbooks"),
		api_siteGetGroups("sites/{0}/groups"), 
		api_sitesGetProjects("sites/{0}/projects?fields=_all_&sort=name:asc"),
		api_siteGetDatasources("sites/{0}/datasources?fields=_all_"), 
		api_groupGetUsers("sites/{0}/groups/{1}/users"),
		api_userGetWorkbooksThenCanView("sites/{0}/users/{1}/workbooks"),
		api_userGetGroupsTheyAreIn("sites/{0}/users/{1}/groups"),
		api_workbookGetViews("sites/{0}/workbooks/{1}/views"),
		api_groupAdd("sites/{0}/groups"),
		api_groupAddBody("<tsRequest><group name=\"{0}\" /></tsRequest>"),
		api_groupDelete("sites/{0}/groups/{1}"),
		api_userAdd("sites/{0}/users"),
		api_userAddBody("<tsRequest> <user name=\"{0}\"  siteRole=\"{1}\" /> </tsRequest>"),
		api_userDelete("sites/{0}/users/{1}"),
		api_groupAddUser("sites/{0}/groups/{1}/users"),
		api_groupAddUserBody("<tsRequest> <user id=\"{0}\" /> </tsRequest>"),
		api_groupDeleteUser("sites/{0}/groups/{1}/users/{2}"),
		api_getProjectPermissions("sites/{0}/projects/{1}/permissions"),
		api_getWorkbookPermissions("sites/{0}/workbooks/{1}/permissions"),
		api_getDatasourcePermissions("sites/{0}/datasources/{1}/permissions"),
		api_getViewData("sites/{0}/views/{1}/data"),
		api_getWorkbook("sites/{0}/workbooks?filter=name:eq:{1}"),
		api_getViewByPath("sites/{0}/views?filter=viewUrlName:eq:{1}"),
		api_getViewPDF("sites/{0}/views/{1}/pdf?type={2}&orientation={3}&vf_{4}=")
		
		;
		

		private String url;

		APIURL(String envUrl) {
			this.url = envUrl;
		}

		public String getUrl() {
			return url;
		}
	}
	
	public TableauRest() {
		super();
	}
	
	public TableauRest(TableauServerLogonTokens tslt, BlockingQueue<String> bq) {
		super();
		TableauRest.bq=bq;
		TableauRest.tslt = tslt;
		
		
	}

	
	public void login() throws Exception, Error {

		if (!tslt.getTokenID().equals(""))
			return;
		bq.put("logging in");

	
		String payload = APIURL.api_signinBody.getUrl();

		payload = MessageFormat.format(payload, tslt.getUserName(), tslt.getUserPassword(), tslt.getSite());
	
		ClientResponse clientResponse = genericGetorPost(tslt.getUrlAndAPI() + APIURL.api_signin.getUrl(), payload,
				null, false, MediaType.APPLICATION_JSON);

		/*
		 * {"credentials":{"site":{"id":"796b065d-2b34-4f93-9110-0611dffdb4ff",
		 * "contentUrl":""},"user":{"id":"2e838013-c8b1-419e-8d2b-66bd84717d48"},
		 * "token":"vNxOUbpUTVy-LCu5OA33Zw|V4nZlI2J2Ln9hCaD5WfYaCZCWK71eFX7"}}
		 * 
		 */
		// checks for errors and if OK, returns payload in JSON format
		String json = responseErrorCheck(clientResponse);

		try {

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(json);

			jsonObject = (JSONObject) jsonObject.get("credentials");
			tslt.setTokenID((String) jsonObject.get("token"));
			tslt.setUserID((String) ((JSONObject) jsonObject.get("user")).get("id"));
			tslt.setSiteID((String) ((JSONObject) jsonObject.get("site")).get("id"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private String responseErrorCheck(ClientResponse clientResponse) throws Exception, Error {

		try {

			String json = clientResponse.getEntity(String.class);
			bq.put(json);

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(json);

			if (clientResponse.getStatus() != 200) {
				jsonObject = (JSONObject) jsonObject.get("error");
				// confirm not null
				if (jsonObject != null) {
					bq.put((String) jsonObject.get("detail") + ". Code:" + (String) jsonObject.get("code"));
					throw new Error((String) jsonObject.get("detail") + ". Code:" + (String) jsonObject.get("code"));
				}
			}
			return json;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return null;
	}

	
	
	
	private ClientResponse genericGetorPost(String l_url, String payload, String token, boolean outputToPrintln, String mediaType) throws InterruptedException {
		
		if (mediaType.equals(""))
			mediaType=MediaType.APPLICATION_XML;
	
		
		if (outputToPrintln && payload != null)
			bq.put("Input payload: \n" + payload);

		Client client = Client.create();
		WebResource webResource = client.resource(l_url);

		ClientResponse clientResponse;
		
	  
		if (payload != null)
			clientResponse = webResource.header(TABLEAU_AUTH_HEADER, token).header("Accept", "application/json")
					.header("Content-type", "application/json").type(mediaType).post(ClientResponse.class, payload);
		else
			clientResponse = webResource.header(TABLEAU_AUTH_HEADER, token).header("Accept", "application/json")
					.header("Content-type", "application/json").get(ClientResponse.class);

		return clientResponse;

	}
	

	public String getWorkbookID( String workbook, String contentURL) throws UnsupportedEncodingException {
		
		
		
		if (contentURL == null ||contentURL.equals(""))
			contentURL=workbook;
		
		String localURL = tslt.getUrlAndAPI() +  APIURL.api_getWorkbook.getUrl();
		//catch spaces and non URL standard characters in project name
		localURL = MessageFormat.format(localURL, tslt.getSiteID(), URLEncoder.encode(workbook,StandardCharsets.UTF_8.toString()));

		
		try {

			ClientResponse clientResponse = genericGetorPost(localURL, null, tslt.getTokenID(), true,"");

			// checks for errors and if OK, returns payload in JSON format
			String json = responseErrorCheck(clientResponse);

			JSONParser jsonParser = new JSONParser();
			Object object = jsonParser.parse(json);
			JSONObject jsonObject = (JSONObject) object;
			
			if (((JSONObject) jsonObject.get("pagination")).get("totalAvailable").equals("0"))
				throw new Error("Workbook <" + workbook + "> was not found");
			
			 JSONObject child = (JSONObject) jsonObject.get("workbooks");
			 JSONArray childArray = (JSONArray) child.get("workbook");

		     for (int i = 0; i < childArray.size(); i++) {
				
					
					String wkbk = ((String) ((JSONObject) childArray.get(i)).get("contentUrl"));
					if (wkbk.equals(contentURL))
						return ((String) ((JSONObject) childArray.get(i)).get("id"));
			 }

			throw new Error("Workbook " + workbook + ", with contentURL " +  contentURL + " not found");
			
		} catch (Exception | Error e) {
			throw new Error("Error in getting Workbook ID: " + e.getMessage());
		}
	}
			
	
	public String getViewID(String view) throws UnsupportedEncodingException {
		//we have the workID, so search for that in returned values (if sheet name is duplicated)
	
		String localURL = tslt.getUrlAndAPI() +  APIURL.api_getViewByPath.getUrl();
		//catch spaces and non URL standard characters in project name
		localURL = MessageFormat.format(localURL, tslt.getSiteID(), URLEncoder.encode(view,StandardCharsets.UTF_8.toString()));

		
		try {

			ClientResponse clientResponse = genericGetorPost(localURL, null, tslt.getTokenID(), true,"");

			// checks for errors and if OK, returns payload in JSON format
			String json = responseErrorCheck(clientResponse);

			JSONParser jsonParser = new JSONParser();
			Object object = jsonParser.parse(json);
			JSONObject jsonObject = (JSONObject) object;;
			
			if (((JSONObject) jsonObject.get("pagination")).get("totalAvailable").equals("0"))
				throw new Error("View <" + view + "> was not found");

			
			//may get multiple values back, so search through looks for workbook name
			
			       JSONObject child = (JSONObject) jsonObject.get("views");
			
			       JSONArray childArray = (JSONArray) child.get("view");

					for (int i = 0; i < childArray.size(); i++) {
					
						
				
						String wrkbkID=		(String)	((JSONObject) ((JSONObject) childArray.get(i)).get("workbook")).get("id");
					
					if(tslt.getViewWorkbookID().equals(wrkbkID))
							return ((String) ((JSONObject) childArray.get(i)).get("id"));
					}
	
					throw new Error("View not found");
			
		} catch (Exception | Error e) {
			throw new Error("Error in getting View ID: " + e.getMessage());
		}
	}
	
	public String[]  getViewData() {
		
		String localURL = tslt.getUrlAndAPI() +  APIURL.api_getViewData.getUrl();
		//catch spaces and non URL standard characters in project name
		localURL = MessageFormat.format(localURL, tslt.getSiteID(),tslt.getViewID());
		try {	
			ClientResponse clientResponse = genericGetorPost(localURL, null, tslt.getTokenID(), true,"");
	
			//data comes back in body
			String output = clientResponse.getEntity(String.class);
			String[] values = output.split("\n");
			return values;
			
			
		} catch (Exception | Error e) {
			throw new Error("Error in getting Project ID: " + e.getMessage());
		}
		
		
	}
	
	public void getPDF(String localURL, String pdfPath) {
		
		try {	
			ClientResponse clientResponse = genericGetorPost(localURL, null, tslt.getTokenID(), true,"");
	
			//data comes back in body
			  InputStream is = clientResponse.getEntityInputStream();
		       OutputStream os = new FileOutputStream(pdfPath);

		        byte[] buffer = new byte[1024];
		        int bytesRead;

		        while((bytesRead = is.read(buffer)) != -1){
		            os.write(buffer, 0, bytesRead);
		        }
		        is.close();

		        //flush OutputStream to write any buffered data to file
		        os.flush();
		        os.close();
		
			
		} catch (Exception | Error e) {
			throw new Error("Error in getting Project ID: " + e.getMessage());
		}
		
		
	}
	

	
	
}
