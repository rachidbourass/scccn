package org.ascn.webservices;

public class GoogleAPIAuth {

	private String tokenUrl;
	private String clientID;
	private String clientSecret;
	private String refreshToken;
	
	public String getTokenUrl() {
		if(null == tokenUrl) {
			tokenUrl = "https://www.googleapis.com/oauth2/v3/token";
		}
		return tokenUrl;
	}
	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}
	public String getClientID() {
		return clientID;
	}
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	public String getClientSecret() {
		return clientSecret;
	}
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
}
