package org.ascn.utils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.ConnectorClassLoader;
import sailpoint.object.AuditEvent;
import sailpoint.object.Custom;
import sailpoint.object.Identity;
import sailpoint.server.Auditor;

public class GeneralUtils {

	private static final String LOG_NAME = "org.ascn.utils.GeneralUtils";

	/**
	 * validateEmail
	 * @param email String
	 * @return boolean
	 */
	public static boolean validateEmail(String email) {
		boolean flag = false;
		if (EmailValidator.getInstance().isValid(email) && email.contains("@ascension")) {
			flag = true;
		}
		return flag;
	}

	/**
	 * This method formats a full name for a given identity
	 * @param identity
	 * @return Full Name of a Identity cude
	 */
	public static String buildFullName(Identity identity) {
	Common.error(LOG_NAME, "Entering");
	String firstName = identity.getFirstname();
	String lastName = identity.getLastname();
	String middleName = identity.getStringAttribute("middleName");
	String fullName = firstName;
	if (middleName != null) {
	fullName = fullName + " " + middleName;
	}
	fullName = fullName + " " + lastName;
	Common.error(LOG_NAME, "Exiting");
	return fullName;
	}
	/**
	 * loadSPConnectorClass
	 */
	public static void loadSPConnectorClass() {
		 ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		  ClassLoader bakcedUpCl = null;
		  if(currentCl instanceof ConnectorClassLoader){
			  bakcedUpCl = currentCl;
			  Thread.currentThread().setContextClassLoader(currentCl.getParent());
		  }
	}
	/**
	 * loadAPIConfiguration
	 * @param configName String
	 * @return Map<String, String>
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> loadAPIConfiguration(String configName) {
		String methodName = "loadConfiguration(String configName)";
		Common.trace(LOG_NAME,"Entering "+methodName);
		Map<String, String> configMap = new HashMap<>();
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			Custom apiConfig = context.getObjectByName(Custom.class, "ASCN-APIConfigurations");
			configMap = (Map<String, String>) apiConfig.get(configName);
		} catch (Exception e) {
			Common.error(LOG_NAME,"Error loading "+methodName+" "+e,e);
			throw new RuntimeException(LOG_NAME+" Error loading "+methodName+" "+e,e);
		}
		Common.trace(LOG_NAME,"Exiting "+methodName);
		return configMap;
	}

	/**
	 * loadAPIConfiguration
	 * @param configName String
	 * @param objectName String
	 * @return Map<String, String>
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> loadAPIConfiguration(String configName, String objectName) {
		String methodName = "loadConfiguration(String configName, String objectName)";
		Common.info(LOG_NAME,"Entering "+methodName);
		Map<String, String> configMap = new HashMap<>();
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			Custom apiConfig = context.getObjectByName(Custom.class, objectName);
			configMap = (Map<String, String>) apiConfig.get(configName);
		} catch (Exception e) {
			Common.error(LOG_NAME,"Error loading "+methodName+" "+e,e);
			throw new RuntimeException("Error loading "+methodName+" "+e,e);
		}
		Common.info(LOG_NAME,"Exiting "+methodName);
		return configMap;
	}

	/**
	 * loadCustomMap
	 * @param configName String
	 * @param objectName String
	 * @return Map<String, String>
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> loadCustomMap(String configName, String objectName) {
		String methodName = "loadCustomMap(String configName, String objectName)";
		Common.info(LOG_NAME,"Entering "+methodName);
		Map<String, String> configMap = new HashMap<>();
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			Custom apiConfig = context.getObjectByName(Custom.class, objectName);
			configMap = (Map<String, String>) apiConfig.get(configName);
		} catch (Exception e) {
			Common.error(LOG_NAME,"Error loading "+methodName+" "+e,e);
			throw new RuntimeException(LOG_NAME+" Error loading "+methodName+" "+e,e);
		}
		Common.trace(LOG_NAME,"Exiting "+methodName);
		return configMap;
	}

	/**
	 * loadCustomList
	 * @param configName String
	 * @param objectName String
	 * @return List<String>
	 */
	@SuppressWarnings("unchecked")
	public static List<String> loadCustomList(String configName, String objectName) {
		String methodName = "loadCustomList(String configName, String objectName)";
		Common.trace(LOG_NAME,"Entering "+methodName);
		List<String> configList = new ArrayList<>();
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			Custom apiConfig = context.getObjectByName(Custom.class, objectName);
			configList = (List<String>) apiConfig.get(configName);
		} catch (Exception e) {
			Common.error(LOG_NAME,"Error loading "+methodName+" "+e,e);
			throw new RuntimeException("Error loading "+methodName+" "+e,e);
		}
		Common.trace(LOG_NAME,"Exiting "+methodName);
		return configList;
	}
	/**
	 * convertXMLToJSON
	 * @param xml String
	 * @return String
	 */
	public static String convertXMLToJSON(String xml) {
		String jsonString = null;
		try {
			JSONObject jo = XML.toJSONObject(xml);
			jsonString = jo.toString();
		} catch (JSONException e) {
			throw new RuntimeException("Error converting XML to JSON");
		}
		return jsonString;
	}

	/**
	 * generateList
	 * @param inStr String
	 * @param stringSeperator String
	 * @return List<Object>
	 */
	public static List<Object> generateList(String inStr, String stringSeperator) {
		List<Object> outList = Arrays.asList(inStr.split(stringSeperator));
		return outList;
	}

	/**
	 * Convert Object to String nullsafe
	 * @param o
	 * @return
	 */
	public static String OTOS(Object o) {
		String s = o != null ? o.toString() : null;
		return s;
	}

	/**
	 * createCustomObject
	 * @param objName String
	 * @param inData  String
	 */
    public static void createCustomObject(String objName, String inData) {
    	String methodName = "createCustomObject(String objName, String inData)";
    	Common.info(LOG_NAME,"Entering "+methodName);
    	Custom c = null;
    	try {
    		SailPointContext context = SailPointFactory.getCurrentContext();
    		c = context.getObjectByName(Custom.class, objName);
    		if (null == c) {
    			c = new Custom();
    		}
    		c.setName(objName);
	    	c.put("data", inData);
    		context.saveObject(c);
    		context.commitTransaction();
    	} catch (Exception e) {
    		Common.error(LOG_NAME,"Error in "+methodName+" "+e,e);
    		throw new RuntimeException("Error Creating Custom Object");
    	}
    	Common.info(LOG_NAME,"Exiting "+methodName);
    }

    /** Use this static method call to generate error logs in custom object
	 * createCustomErrors
	 * @param objName String
	 * @param inData  String
	 */
    public static void createCustomErrors(String objName, String errorKey, Object message) {
    	String methodName = "createCustomErrors(String objName, String errorKey, Object message)";
    	Common.info(LOG_NAME,"Entering "+methodName);
    	Custom custObj = null;
    	try {
    		SailPointContext context = SailPointFactory.getCurrentContext();
    		custObj = context.getObjectByName(Custom.class, objName);
    		if (null == custObj) {
    			custObj = new Custom();
    		}
    		custObj.setName(objName);
    		custObj.put(errorKey, message);
    		context.saveObject(custObj);
    		context.commitTransaction();
    	} catch (Exception e) {
    		Common.error(LOG_NAME,"Error in "+methodName+" "+e,e);
    		throw new RuntimeException("Error Creating Custom Error log: "+e);
    	}
    	Common.info(LOG_NAME,"Exiting "+methodName);
    }

    /**
     * parseJsonNodeForString, given a JSON String, and specific attribute name
     * in the jsonString,
     * this method will return the value of requested attribute
     * @param inStr, json string
     * @param attribName, the name of the attribute you need the value of
     * @return string value
     * Description: Use this method to extract access token or refresh token
     * from a call to authentication endpoint
     */
    public static String parseJsonNodeForString(String inStr, String attribName) {
	  String _token = null;
	  try {
		  ObjectMapper mapper = new ObjectMapper();
		  JsonNode node = mapper.readTree(inStr.toString());
	      if (node.has(attribName)) {
	    	  _token = node.get(attribName).asText();
	      }
	  }catch(Exception e) {
		  throw new RuntimeException("ERROR: "+e,e);
	  }
	  return _token;
	}

    /**
     * Method to extract values from URL
     * @param url
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> getUrlValues(String url) throws UnsupportedEncodingException {
        int i = url.indexOf("?");
        Map<String, String> paramsMap = new HashMap<>();
        if (i > -1) {
            String searchURL = url.substring(url.indexOf("?") + 1);
            String params[] = searchURL.split("&");
            if (isStringArrayEmpty(params)) {
            	params = searchURL.split("&amp;");
            }

            for (String param : params) {
                String temp[] = param.split("=");
                paramsMap.put(temp[0], java.net.URLDecoder.decode(temp[1], "UTF-8"));
            }
        }

        return paramsMap;
    }

    public static boolean isStringArrayEmpty(String[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Method to log Audit events
     * @param source
     * @param objName
     * @param identityName
     * @param appName
     * @param accnativeIdentity
     * @param inMap
     */
    public static void logAuditEvent(String objName,String source, String identityName,String appName,String accnativeIdentity, Map inMap) {
    	String methodName = "logAuditEvent(String objName,String identityName,String appName,String accnativeIdentity, Map inMap)";
    	Common.info(LOG_NAME,"Entering "+methodName);
		try {
	    	SailPointContext context = SailPointFactory.getCurrentContext();
	    	AuditEvent auditEvent = new AuditEvent();
			auditEvent = new AuditEvent(objName, "Comment", identityName);
			auditEvent.setApplication(appName);
			auditEvent.setSource(source);
			auditEvent.setAccountName(accnativeIdentity);
			auditEvent.setString1(inMap.get("string1")+identityName);
			auditEvent.setString2(inMap.get("string2").toString());
			auditEvent.setString3(inMap.get("string3").toString());
			auditEvent.setString4(inMap.get("string4").toString());
			//auditEvent.setAttributes(attrMap);
			Auditor.log(auditEvent);
			context.commitTransaction();
		} catch(Exception e) {
			  throw new RuntimeException("ERROR: "+e,e);
		  }
	}

    /**
     * Method to log Audit events
     * @param source
     * @param objName
     * @param identityName
     * @param appName
     * @param accnativeIdentity
     * @param inMap
     */
    public static void logAuditEvent(String source, String identityName,String appName,String accnativeIdentity, Map inMap) {
    	String methodName = "logAuditEvent(String objName,String identityName,String appName,String accnativeIdentity, Map inMap)";
    	Common.info(LOG_NAME,"Entering "+methodName);
		try {

	    	SailPointContext context = SailPointFactory.getCurrentContext();
	    	AuditEvent auditEvent = new AuditEvent();
			auditEvent = new AuditEvent();
			auditEvent.setSource(source);
			auditEvent.setAttribute("identityName", identityName);
			auditEvent.setAccountName(accnativeIdentity);
			auditEvent.setString1(inMap.get("string1")+identityName);
			auditEvent.setString2(inMap.get("string2").toString());
			auditEvent.setString3(inMap.get("string3").toString());
			auditEvent.setString4(inMap.get("string4").toString());
			//auditEvent.setAttributes(attrMap);
			Auditor.log(auditEvent);
			context.commitTransaction();
		} catch(Exception e) {
			  throw new RuntimeException("ERROR: "+e,e);
		  }
	}
}
