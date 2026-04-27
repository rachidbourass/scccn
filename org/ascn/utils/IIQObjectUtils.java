package org.ascn.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Custom;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IIQObjectUtils {

	private static final String LOG_NAME = "org.ascn.utils.IIQObjectUtils";
   
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
