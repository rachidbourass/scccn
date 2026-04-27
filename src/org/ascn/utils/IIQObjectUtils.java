package org.ascn.utils;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Custom;

public class IIQObjectUtils {

	private static final String LOG_NAME = "org.ascn.utils.IIQObjectUtils";

	/**
	 * createCustomObject
	 * @param objName String
	 * @param inData  String
	 */
    public static void createCustomObject(String objName, String inData) {
    	String methodName = "createCustomObject(String objName, String inData)";
    	Common.info(LOG_NAME,"Entering "+methodName);
    	Custom c = new Custom();
    	c.setName(objName);
    	c.put("data", inData);
    	try {
    		SailPointContext context = SailPointFactory.getCurrentContext();
    		context.saveObject(c);
    		context.commitTransaction();
    	} catch (Exception e) {
    		Common.error(LOG_NAME,"Error in "+methodName+" "+e,e);
    		throw new RuntimeException("Error Creating Custom Object");
    	}
    	Common.info(LOG_NAME,"Exiting "+methodName);
    }


}
