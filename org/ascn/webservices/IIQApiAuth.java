package org.ascn.webservices;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class IIQApiAuth {
	
	static String fdqn = "idm-dev.ascension.org";
	
	private static String BASE_URL = "https://"+fdqn+"/identityiq";
	public static String TOKEN_URL = BASE_URL+"/oauth2/token";
	public static String GRANT_TYPE = "client_credentials";
    public static String CLIENT_ID="2a0LJJRVNxHJ7wN4MaVLsmmsvpFARL4B";
    public static String CLIENT_SECRET="N5c7UO7ftpsZLUHh";
    private static String ACCEPT = "application/json";
    
    static String patternMatcher = ".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*";
    
    public Object auth() {
    	Pattern pat = Pattern.compile(patternMatcher);
    	Client client = ClientBuilder.newClient();
    	MultivaluedMap<String,String> formData = new MultivaluedHashMap<>();
    	String secret = "Basic "+Base64.encodeBase64String(new String(CLIENT_ID+":"+CLIENT_SECRET).getBytes());
    	formData.add("grant_type", GRANT_TYPE);
    	Response response = (Response) client.target(TOKEN_URL).request(MediaType.APPLICATION_JSON).header("Authorization", secret).post(Entity.form(formData));
    	String output = response.readEntity(String.class);
    	Matcher m = pat.matcher(output);
    	String outStr = null;
		if(m.matches() && m.groupCount()>0) {
			outStr = m.group(1);
		}
		
    	System.out.println(outStr);
    	return "Bearer "+outStr;
    }
    
    public void IIQPing() {
    		
    }
    
    public void IIQGet(String endPoint) {
    	 Client client = ClientBuilder.newClient();
    	 String apiUrl = BASE_URL+"/"+endPoint;
    	 System.out.println(apiUrl);
    	  Response response = (Response)client.target(apiUrl).request(MediaType.APPLICATION_JSON).accept(ACCEPT).header("Authorization", auth()).get(); // header with access token as authorization value
    	  String output = response.readEntity(String.class); // reading response as string format
    	  response.getStatusInfo();
    	  System.out.println(output);
    	  
    }
    
    public String httpPost(String endPoint) throws ClientProtocolException, IOException {
    	String entityResponse =null;
    	String apiUrl = BASE_URL+"/"+endPoint;
    	Header header = null;
    	
    	
    	HttpPost post = new HttpPost(apiUrl);
    	
    	post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    	post.setHeader(HttpHeaders.ACCEPT,MediaType.APPLICATION_JSON);
    	post.setHeader(HttpHeaders.AUTHORIZATION,(String) auth());
    	try(CloseableHttpClient client = HttpClients.createDefault()){
    			CloseableHttpResponse response = client.execute(post);
    			System.out.println(response);
    			if(null != response) {
    				HttpEntity entity = response.getEntity();
    				entityResponse = EntityUtils.toString(entity);
    			}
    	}
    	return entityResponse;
    }
}

