package org.ascn.webservices;

import java.io.IOException;
import java.io.StringWriter;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ascn.utils.GeneralUtils;
import org.json.JSONException;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.tools.GeneralException;

public class TableauHelper {
	
	private Log log = LogFactory.getLog(this.getClass());
	private final static String PATTERN_TO_MATCH = ".*\"token\"\\s*:\\s*\"([^\"]+)\".*";
	private String arOperation;
	
	public String getArOperation() {
		return arOperation;
	}

	public void setArOperation(String arOperation) {
		this.arOperation = arOperation;
	}

	public String getToken(String sitePrefix) throws IOException, JSONException, GeneralException {
	    SailPointContext context = SailPointFactory.getCurrentContext();
	    String methodName = "getToken(String sitePrefix)";
	    log.trace("Entering " + methodName);
	    Pattern pat = Pattern.compile(PATTERN_TO_MATCH);
	    Map < String, String > configMap = GeneralUtils.loadAPIConfiguration("Tableau-Base");
	    Map < String, String > siteMap = GeneralUtils.loadAPIConfiguration("Tableau-" + sitePrefix);
	    String responseHandler = null;
	    String outToken = null;
	    String decryptedPwd = context.decrypt(siteMap.get("userPassword").toString());
	    String userName = siteMap.get("userName");
	    String contentUrl = siteMap.get("contentUrl");
	    if (StringUtils.isBlank(contentUrl) || StringUtils.isBlank(userName) || StringUtils.isBlank(decryptedPwd)) {
	        throw new RuntimeException("ERROR: " + methodName + "Site Credentials can not be null");
	    }
	    String payload = createXMLPayload(userName, decryptedPwd, contentUrl);
	    try (CloseableHttpClient client = HttpClients.createDefault()) {

	        HttpPost httpPost = new HttpPost(configMap.get("authUrl"));
	        httpPost.setHeader("Accept", configMap.get("acceptType"));
	        httpPost.setHeader("Content-type", configMap.get("contentType"));
	        StringEntity stringEntity = new StringEntity(payload);
	        httpPost.setEntity(stringEntity);
	        CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpPost);
	        responseHandler = EntityUtils.toString(response.getEntity());

	    } catch (Exception e) {
	        log.error("Error getting Authentication Token " + methodName + " " + e, e);
	        throw new RuntimeException("Error getting Authentication Token " + methodName + " " + e, e);
	    }
	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode node = mapper.readTree(responseHandler.toString());
	    if (node.has("credentials")) {
	        JsonNode creds = node.get("credentials");
	        if (creds.has("token")) {
	            outToken = creds.get("token").asText();
	        }
	    }
	    return outToken;

	}
	
	public String getToken(Map<String, String> configMap) throws IOException, JSONException {
		String methodName = "getToken(Map<String, String> configMap)";
		log.trace("Entering "+methodName);		
		Pattern pat = Pattern.compile(PATTERN_TO_MATCH);
		String responseHandler = null;
		String outToken = null;
		String decryptedPwd = configMap.get("userPassword").toString();
	    String userName = configMap.get("userName");
	    String contentUrl = configMap.get("contentUrl");
	    if (StringUtils.isBlank(contentUrl) || StringUtils.isBlank(userName) || StringUtils.isBlank(decryptedPwd)) {
	        throw new RuntimeException("ERROR: " + methodName + "Site Credentials can not be null");
	    }
	    String payload = createXMLPayload(userName, decryptedPwd, contentUrl);
	    CloseableHttpClient client = null;
		try {
			client = HttpClients.createDefault();
			//HttpClient client = HttpClientBuilder.create().build();
			HttpPost httpPost = new HttpPost(configMap.get("authUrl"));
			httpPost.setHeader("Accept", configMap.get("accept"));
			httpPost.setHeader("Content-type", configMap.get("contentType"));
			StringEntity stringEntity = new StringEntity(payload);
			httpPost.setEntity(stringEntity);
			CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpPost);
			responseHandler = EntityUtils.toString(response.getEntity());
			System.out.println(responseHandler);
		} catch (Exception e) {
			log.error("Error getting Authentication Token "+methodName+" "+e,e);
			throw new RuntimeException("Error getting Authentication Token "+methodName+" "+e,e);
		}
        String loginResponse = GeneralUtils.convertXMLToJSON(responseHandler);
        System.out.println(loginResponse);
        Matcher m = pat.matcher(loginResponse);
		if (m.matches() && m.groupCount() > 0) {
			outToken = m.group(1);
		}
		
		// Above I used the Regex to grab the 1st token value
		// Below is a alternate method using GSON library to grab Token value from the
		// JSON response.
  
       // outToken = parseToken(loginResponse);
		//log.trace("Exiting "+methodName);
		
		//return outToken;
		
		/*ObjectMapper mapper = new ObjectMapper();
	    JsonNode node = mapper.readTree(loginResponse.toString());
	    if (node.has("credentials")) {
	        JsonNode creds = node.get("credentials");
	        if (creds.has("token")) {
	            outToken = creds.get("token").asText();
	        }
	    }*/
	    return outToken;
		
	}
	
	private String createXMLPayload(String userName, String decryptedPwd, String contentUrl) {
		String methodName = "createXMLPayload(String userName, String decryptedPwd, String contentUrl)";
		log.trace("Entering "+methodName);	
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
			creds.setAttribute("name", userName);
			creds.setAttribute("password", decryptedPwd);
			Element url = doc.createElement("site");
			url.setAttribute("contentUrl", contentUrl);
			creds.appendChild(url);
			rootElement.appendChild(creds);
			outPayload = writeXml(doc);

		} catch (Exception e) {
			log.error("Error Access Token "+methodName+" "+e,e);
			throw new RuntimeException("Error Access Token  "+methodName+" "+e,e);
		}
		log.trace("Exiting "+methodName);	
		return outPayload;
	}

	private String writeXml(Document doc) throws TransformerException {
		String methodName = "writeXml(Document doc)";
		log.trace("Entering "+methodName);	
		StringWriter writer = new StringWriter();
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		String strResult = writer.toString();
		log.trace("Exiting "+methodName);	
		return strResult;
	}

	

	@SuppressWarnings("unused")
	private String parseToken(String responseBody) throws JSONException {
		JsonElement outtoken = null;
		JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
		JsonObject siteObj = (JsonObject) jsonObject.get("credentials");
		log.trace("SITE: " + siteObj);
		outtoken = siteObj.get("token");
		return outtoken.toString();
	}

	public String getDisablePayload() {
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
			if (arList != null && arList.size() > 0) {
				for (AccountRequest ar : arList) {
					setArOperation(ar.getOperation().toString());
					String appName = ar.getApplicationName();
					accountId = ar.getNativeIdentity();
				}
			}
		} catch (Exception e) {
			e.getMessage();
		}
		return accountId;
	}
}
