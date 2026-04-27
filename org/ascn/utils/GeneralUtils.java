package org.ascn.utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.ConnectorClassLoader;
import sailpoint.object.Custom;

public class GeneralUtils {
	
	private static final String LOG_NAME = "org.ascn.utils.GeneralUtils";
	
	public static boolean validateEmail(String email) {
		boolean flag = false;
		if (EmailValidator.getInstance().isValid(email) && email.contains("@ascension")) {
			flag = true;
		}
		return flag;
	}

	public static void loadSPConnectorClass() {
		 ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		  ClassLoader bakcedUpCl = null;
		  if(currentCl instanceof ConnectorClassLoader){
			  bakcedUpCl = currentCl;
			  Thread.currentThread().setContextClassLoader(currentCl.getParent());
		  }
	}
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
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> loadCustomMap(String configName, String objectName) {
		String methodName = "loadConfiguration(String configName)";
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
}
