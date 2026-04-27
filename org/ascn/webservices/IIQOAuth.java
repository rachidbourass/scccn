package org.ascn.webservices;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;

public class IIQOAuth {

	private static String tokenURL = "https://idm-dev.ascension.org/identityiq/oauth2/token";
	private static String clientID = "2a0LJJRVNxHJ7wN4MaVLsmmsvpFARL4B";
	private static String clientSecrete = "N5c7UO7ftpsZLUHh";
	private static String grantType = "client_credentials";
	public static void auth() {
		  Client client = ClientBuilder.newClient();
		  MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
		  String secret = "Basic "+Base64.encodeBase64String(new String(clientID+":"+clientSecrete).getBytes()); // we should use Base64 encode to encode client id and client secret
		  formData.add("grant_type", grantType);
		  Response  response = (Response) client.target(tokenURL). // token URL to get access token
		  request(MediaType.APPLICATION_JSON). // JSON Request Type
		  header( "Authorization", secret ) // Authorization header goes here
		  .post(Entity.form(formData))  ;   // body with grant type
		   String output = response.readEntity(String.class); // reading response as string format
		   System.out.println(output);
	}
}
