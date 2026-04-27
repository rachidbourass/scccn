package org.ascn.webservices;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sailpoint.object.Identity;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.tools.GeneralException;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.webservices.EndPoint;

public class TableauAuth {
	
	private SailPointContext context;
    private String tokenUrl;
    private String newEndPointUrl;
    private String arOperation;
    private String siteName;
    private  String patternToMatch = ".*\"token\"\\s*:\\s*\"([^\"]+)\".*";
	private  Pattern pat;
    
    public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}
	private String userName;
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
	private String userPassword;
	public String getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	public String getArOperation() {
		return arOperation;
	}

	public void setArOperation(String arOperation) {
		this.arOperation = arOperation;
	}

	public String getNewEndPointUrl() {
		return newEndPointUrl;
	}

	public void setNewEndPointUrl(String newEndPointUrl) {
		this.newEndPointUrl = newEndPointUrl;
	}

	public String getTokenUrl() {
		return tokenUrl;
	}

	public void setTokenUrl(String tokenUrl) throws Exception {
		this.tokenUrl = tokenUrl;
		if(null == tokenUrl) {
			throw new Exception("Token URL was not found or provided");
		}
	}
	private String contentType;
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
		if(null == contentType) {
			this.contentType = "application/xml";
		}
	}
	private String accept;
	
	public String getAccept() {
		return accept;
	}

	public void setAccept(String accept) {
		this.accept = accept;
		if(null == accept) {
			this.accept = "application/json";
		}
	}
	public TableauAuth() throws GeneralException {
		//this.context = SailPointFactory.getCurrentContext();
		/*if(null == context) {
			this.context = SailPointFactory.createContext();
		}
		*/
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
            creds.setAttribute("name", getUserName());
            //String decryptedPWD = context.decrypt(getUserPassword());
            String decryptedPWD = getUserPassword();
            creds.setAttribute("password", decryptedPWD);
            Element url = doc.createElement("site");
            url.setAttribute("contentUrl", getSiteName());
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
		 HttpPost httpPost = new HttpPost(getTokenUrl());
		 httpPost.setHeader("Accept", getAccept());
         httpPost.setHeader("Content-type", getContentType());
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
			 HttpPost httpPost = new HttpPost(getTokenUrl());
			 httpPost.setHeader("Accept", getAccept());
	         httpPost.setHeader("Content-type", getContentType());
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
		 System.out.println("SITE: "+siteObj);
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
