package org.ascn.webservices;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;
import sailpoint.object.Custom;
import sailpoint.object.Workflow;
import sailpoint.object.Workflow.Variable;
import sailpoint.server.Auditor;
import sailpoint.tools.GeneralException;

public class GSuiteAPI {
  String appName = "Gsuite";
  private SailPointContext context;
  private String gSuiteConfig = "Ascension-Custom-Gsuite";
  private Application application;
  private int responseCode;
  private String responseMsg;
  private String errorMsg;
  
  public int getResponseCode() {
	  return responseCode;
  }
  
  public String getResponseMessage() {
	  return responseMsg;
  }
  
  public String getErrorMessage() {
	  return errorMsg;
  }

  public GSuiteAPI() throws GeneralException {
	  this.context = SailPointFactory.getCurrentContext();
  }
  public String getToken() throws GeneralException {
    String token = null;
    Custom customObject = context.getObjectByName(Custom.class, gSuiteConfig);
    String tokenURL = customObject.getString("tokenURL");
    try {
      // Code to get the access token 
      application = context.getObjectByName(Application.class,appName);
      String clientID = application.getAttributeValue("clientID").toString();
      String clientSecret = context.decrypt(application.getAttributeValue("clientSecret").toString());
      String refreshToken = context.decrypt(application.getAttributeValue("refreshToken").toString());
      String gType = "refresh_token";
      URL accessTokenURL = new URL(tokenURL);
      HttpURLConnection conn = (HttpURLConnection) accessTokenURL.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode authRequestNode = mapper.createObjectNode();
      authRequestNode.put("refresh_token", refreshToken);
      authRequestNode.put("client_id", clientID);
      authRequestNode.put("client_secret", clientSecret);
      authRequestNode.put("grant_type", gType);
      DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
      dos.writeBytes(authRequestNode.toString());
      dos.flush();
      dos.close();
      int responseCode = conn.getResponseCode();
      if (responseCode != 200) {
        throw new RuntimeException("ERROR: Invalid Response Code: " + responseCode);
      }
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      JsonNode node = mapper.readTree(response.toString());
      if (node.has("access_token")) {
        token = node.get("access_token").asText();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return token;
  }

  public void addGSuiteGroupMember(String parentGroup, String memberGroup) {
    String groupMemberURL = null;
    Custom customObject = null;
    try {
      customObject = context.getObjectByName(Custom.class, gSuiteConfig);
      if (customObject == null) {
    	this.errorMsg = "ERROR: Could not load Custom Object: " + gSuiteConfig;
        throw new RuntimeException(errorMsg);
      }
      if (StringUtils.isBlank(parentGroup) || StringUtils.isBlank(memberGroup)) {
    	this.errorMsg = "ERROR: Invalid Input: Parent Group: " + parentGroup + " Member Group: " + parentGroup;
        throw new RuntimeException(errorMsg);
      }
      groupMemberURL = customObject.getString("groupMemberUrl");
      groupMemberURL = groupMemberURL + parentGroup + "/members";
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode groupMemberNode = mapper.createObjectNode();
      groupMemberNode.put("kind", "admin#directory#member");
      groupMemberNode.put("email", memberGroup);
      groupMemberNode.put("role", "MEMBER");
      groupMemberNode.put("type", "GROUP");
      URL url = new URL(groupMemberURL);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Authorization", "Bearer " + getToken());
      DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
      dos.writeBytes(groupMemberNode.toString());
      dos.flush();
      dos.close();
      this.responseCode = conn.getResponseCode();
      if (responseCode != 200) {
        this.errorMsg = "Failed Response Returned: " + responseCode + " Message: " + conn.getResponseMessage();
        throw new RuntimeException(errorMsg);
      }
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      responseMsg = response.toString();
    } catch (Exception e) {
    	responseMsg = "ERROR: " + e;
    }
    
  }
  
  public List<String> getRequiredVariables(Workflow wf){
	  List<Variable> vars = wf.getVariableDefinitions();
	  List<String> requiredVars = new ArrayList<>();
	  for (Variable var : vars) {
		  if (var.isInput()) { 
	        requiredVars.add(var.getName());
	      }
	  }
	  return requiredVars;
  }
  
  public List<String> validateArguments(Workflow wf, List<String> requiredArguents){
	 List<String> outMsg = new ArrayList<>();
	 for (String ra : requiredArguents) {
		 String thisParameter = wf.get(ra) != null ? wf.get(ra).toString() : null;
		 if (StringUtils.isBlank(thisParameter)) {
			 outMsg.add("ERROR: Required argument "+thisParameter+" is NULL");
		 }
	}
	return outMsg;
  }
  
  public void auditTransaction(Workflow workflow){
	AuditEvent auditEvent = new AuditEvent();
	Attributes attrsMap = new Attributes();
	attrsMap.put("requesterPrimaryEmail", workflow.get("requesterPrimaryEmail"));
	attrsMap.put("groupPrimaryEmail", workflow.get("groupPrimaryEmail"));
	attrsMap.put("collibraGUID", workflow.get("collibraGUID"));
	attrsMap.put("collibraRequestTimestamp", workflow.get("collibraRequestTimestamp"));
	attrsMap.put("groupCorrespondingDataset", workflow.get("groupCorrespondingDataset"));
	auditEvent = new AuditEvent("Collibra Request Record", "Comment", workflow.get("identityName").toString());
	auditEvent.setApplication("GSuite");
	auditEvent.setAccountName(workflow.get("identityName").toString());
	auditEvent.setAttributes(attrsMap);
	Auditor.log(auditEvent);
	try {
		context.commitTransaction();
	} catch (GeneralException e) {
		throw new RuntimeException("ERROR: Creating Audit Transaction "+e,e);
	}
 }
  
  public boolean checkGSuiteGroup(String groupEmail) {
	    boolean exists = false;
	    String groupMemberURL = null;
	    Custom customObject = null;
	    try {
	      customObject = context.getObjectByName(Custom.class, gSuiteConfig);
	      if (customObject == null) {
	    	this.errorMsg = "ERROR: Could not load Custom Object: " + gSuiteConfig;
	        throw new RuntimeException(errorMsg);
	      }
	      if (StringUtils.isBlank(groupEmail)) {
	    	this.errorMsg = "ERROR: Invalid Input: Group: " + groupEmail + " is null";
	        throw new RuntimeException(errorMsg);
	      }
	      groupMemberURL = customObject.getString("groupMemberUrl");
	      groupMemberURL = groupMemberURL + groupEmail;
	      URL url = new URL(groupMemberURL);
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setDoOutput(true);
	      conn.setRequestMethod("GET");
	      conn.setRequestProperty("Content-Type", "application/json");
	      conn.setRequestProperty("Authorization", "Bearer " + getToken());
	      DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
	      dos.flush();
	      dos.close();
	      this.responseCode = conn.getResponseCode();
	      if (responseCode != 200) {
	        this.errorMsg = "Failed Response Returned: " + responseCode + " Message: " + conn.getResponseMessage();
	        throw new RuntimeException(errorMsg);
	      }
	      exists = true;
	      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	      String inputLine;
	      StringBuffer response = new StringBuffer();
	      while ((inputLine = in.readLine()) != null) {
	        response.append(inputLine);
	      }
	      in.close();
	      responseMsg = response.toString();
	    } catch (Exception e) {
	    	responseMsg = "ERROR: " + e;
	    }
	    return exists;
	  }
}