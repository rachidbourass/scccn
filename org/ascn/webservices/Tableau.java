package org.ascn.webservices;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ascn.utils.Logger;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Custom;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.tools.GeneralException;

public class Tableau {
	private SailPointContext context;
	private String arOperation;
    private String siteName;
    private static String patternToMatch = ".*\"token\"\\s*:\\s*\"([^\"]+)\".*";
	private Pattern pat;
	private Logger log;
	private Map<String,String> loadMap;
	
	private static final String USER_NAME = "userName";
	private static final String USER_PASSWORD = "userPassword";
	private static final String TOKEN_URL = "tokenUrl";
	private static final String ACCEPT = "accept";
	private static final String CONTENT_TYPE = "contentType";
	
	public String getArOperation() {
		return arOperation;
	}

	public void setArOperation(String arOperation) {
		this.arOperation = arOperation;
	}

	public Tableau(String logLevel, String siteName){
		this.log = new Logger(this.getClass());
		if(siteName == null) {
			throw new RuntimeException("ERROR: Tableau(String logLevel, String siteName) siteName is NULL or EMPTY "+siteName);		}
		this.siteName = siteName;
		try {
			this.context = SailPointFactory.getCurrentContext();
			if(null == context) {
				this.context = SailPointFactory.createContext();
			}
			if(null == logLevel) {
				log.setLogLevel("info");
			}
			log.setLogLevel(logLevel);
		}catch(Exception e) {
			log.logError("Error in constructor in Tableau class "+e, e);
		}
		this.loadMap = loadAPIConfig();
		
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String> loadAPIConfig() {
		Map<String, String> configMap = new HashMap<>();
		log.logTrace("Entering loadAPIConfig()");
		try {
			Custom appConfig = context.getObjectByName(Custom.class, "ASCN-APIConfigurations");
			configMap = (Map<String, String>) appConfig.get(siteName);
		} catch (Exception e) {
			log.logError(arOperation, e);
		}
		log.logTrace("Exiting loadAPIConfig()");
		return configMap;
	}
	
	private  String createXMLPayload() {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        String outPayload = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            // add elements to Document
            Element rootElement = doc.createElement("tsRequest");
            // append root element to document
            doc.appendChild(rootElement);
            Element creds = doc.createElement("credentials");
            creds.setAttribute("name", loadMap.get(USER_NAME).toString());
            String decryptedPWD = context.decrypt(loadMap.get(USER_PASSWORD));
            creds.setAttribute("password", decryptedPWD);
            Element url = doc.createElement("site");
            url.setAttribute("contentUrl", loadMap.get(siteName));
            creds.appendChild(url);            
            rootElement.appendChild(creds);
            outPayload = writeXml(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }

		return outPayload;
	}
	
	 private  String writeXml(Document doc) throws TransformerException {
		    StringWriter writer = new StringWriter();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			 String strResult = writer.toString();
			 return strResult;

		 }
	
	
	 public  String getToken() throws IOException, JSONException {
		 pat = Pattern.compile(patternToMatch);
		 String responseBody = null;	
		 String outToken = null;
		 String payload = createXMLPayload();
		 try (CloseableHttpClient client = HttpClients.createDefault()){
		 HttpPost httpPost = new HttpPost(TOKEN_URL);
		 httpPost.setHeader("Accept", ACCEPT);
         httpPost.setHeader("Content-type", CONTENT_TYPE);
         StringEntity stringEntity = new StringEntity(payload);
		 httpPost.setEntity(stringEntity);
		 ResponseHandler<String> responseHandler = response -> {
         int status = response.getStatusLine().getStatusCode();
             if (status >= 200 && status < 300) {
                 HttpEntity entity = response.getEntity();
                 return entity != null ? EntityUtils.toString(entity) : null;
             } else {
                 throw new ClientProtocolException("Unexpected response status: " + status);
             }
         };
         responseBody = client.execute(httpPost, responseHandler);
		 }catch(Exception e) {
			e.printStackTrace();
		}
		
        Matcher m = pat.matcher(responseBody);
		 if(m.matches() && m.groupCount() > 0) {
			 outToken = m.group(1);
		 }
		 //Above I used the Regex to grab the 1st token value
		 //Below is a alternate method using GSON library to grab Token value from the 
		 //JSON response.
		//outToken = parseToken(responseBody);
		 return outToken;
		 //return responseHandler;
	 }
	 //BeanShell Friendly 
	  public  String getBeanShellToken() throws IOException, JSONException {
			 pat = Pattern.compile(patternToMatch);
			 HttpResponse responseBody = null;
			 String responseHandler = null;
			 String outToken = null;
			 String payload = createXMLPayload();
			 try{
				 HttpClient client = HttpClientBuilder.create().build();
			 HttpPost httpPost = new HttpPost(loadMap.get(TOKEN_URL));
			 httpPost.setHeader("Accept", loadMap.get(ACCEPT));
	         httpPost.setHeader("Content-type", loadMap.get(CONTENT_TYPE));
	         StringEntity stringEntity = new StringEntity(payload);
			 httpPost.setEntity(stringEntity);
			 responseBody =  (HttpResponse) client.execute(httpPost);
			 HttpEntity entity = ((org.apache.http.HttpResponse) responseBody).getEntity();
			 responseHandler = EntityUtils.toString(entity);
			 
	     }catch(Exception e) {
				e.printStackTrace();
			}
			
	        Matcher m = pat.matcher(responseHandler);
			 if(m.matches() && m.groupCount() > 0) {
				 outToken = m.group(1);
			 }
			 //Above I used the Regex to grab the 1st token value
			 //Below is a alternate method using GSON library to grab Token value from the 
			 //JSON response.
			//outToken = parseToken(responseBody);
			 return outToken;
			 //return responseHandler;
		 }
	 
	 @SuppressWarnings("unused")
	 private String parseToken(String responseBody) throws JSONException {
		 JsonElement outtoken = null;
		 JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
		 JsonObject siteObj =  (JsonObject) jsonObject.get("credentials");
		 outtoken = siteObj.get("token");
		 return outtoken.toString();
	  }
	 
	 public  String getDisablePayload() {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder dBuilder;
	        String outPayload = null;
	        try {
	            dBuilder = dbFactory.newDocumentBuilder();
	            Document doc = dBuilder.newDocument();
	            // add elements to Document
	            Element rootElement = doc.createElement("tsRequest");
	            // append root element to document
	            doc.appendChild(rootElement);
	            Element user = doc.createElement("user");
	            user.setAttribute("siteRole", "Unlicensed");
	            rootElement.appendChild(user);
	            outPayload = writeXml(doc);
	            
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

			return outPayload;
		}
	 
	 public String getAccountId(ProvisioningPlan plan) throws GeneralException {
			String accountId = null;
			try {
			List<AccountRequest> arList = plan.getAccountRequests();
			if(arList !=null && arList.size()>0) {
				for(AccountRequest ar : arList) {
					setArOperation(ar.getOperation().toString());
					String appName = ar.getApplicationName();
					accountId = ar.getNativeIdentity();
				}
			}
			}catch(Exception e) {
				e.getMessage();
			}
			return accountId;
		}
	 
}
