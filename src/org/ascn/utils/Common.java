package org.ascn.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.Aggregator;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.Connector;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.ResourceObject;
import sailpoint.object.TaskResult;

public class Common {

private static final String LOG_NAME = "org.ascn.utils.Common";
private static String LOG_TRACE = "trace";
private static String LOG_DEBUG = "debug";
private static String LOG_INFO = "info";
private static String LOG_WARN = "warn";
private static String LOG_ERROR = "error";

/**
 * Performs targetted Aggregation
 * @param nativeId
 * @param appName
 * @param aggArgs
 * @return
 */
public static boolean doTargetedAggregation(String nativeId, String appName, Attributes<String, Object> aggArgs) {
	String methodName = "Method doTargetedAggregation Native ID: "+nativeId+" "+appName;
	Logger.info(LOG_NAME, "Entering "+methodName);
	SailPointContext context = null;
	boolean isAggOK = true;
	try {
		context = SailPointFactory.getCurrentContext();
		Application app = context.getObjectByName(Application.class, appName);
		Connector connector = sailpoint.connector.ConnectorFactory.getConnector(app, null);
		if (aggArgs == null || aggArgs.isEmpty()) {
			aggArgs = new Attributes<>();
			aggArgs.put("noOptimizeReaggregation", "true");
			aggArgs.put("checkDeleted", "false");
		}
		ResourceObject rObj = connector.getObject("account", nativeId, null);
		if (null == rObj) { return false; }
		Aggregator agg = new Aggregator(context, aggArgs);
		TaskResult taskResult = agg.aggregate(app, rObj);
		if (taskResult == null) {
			isAggOK = false;
		} else {
			isAggOK = true;
		}
	} catch (Exception e) {
		isAggOK = false;
		Logger.error(LOG_NAME, "Error in "+methodName+" "+e,e);
	}
	Logger.info(LOG_NAME, "Exiting "+methodName);
	return isAggOK;
}

/**
 * Common log
 * @param name
 * @param msg
 * @param level
 */
private static void log(String name, String msg, String level) {
    Log log = LogFactory.getLog(name);
    if(LOG_INFO.equalsIgnoreCase(level)) {
        if(log.isInfoEnabled()) {
            log.info(msg);
        }
    }
    else if(LOG_WARN.equalsIgnoreCase(level)) {
        if(log.isWarnEnabled()) {
            log.warn(msg);
        }
    }
    else if(LOG_ERROR.equalsIgnoreCase(level)) {
        if(log.isErrorEnabled()) {
            log.error(msg);
        }
    }
    else if(LOG_TRACE.equalsIgnoreCase(level)) {
        if(log.isTraceEnabled()) {
            log.trace(msg);
        }
    }
    else {
        if(log.isDebugEnabled()) {
            log.debug(msg);
        }
    }
}

public static void trace(String logName,String msg) {
    log(logName,msg,LOG_TRACE);
}
public static void warn(String logName,String msg) {
    log(logName,msg,LOG_WARN);
}
public static void error(String logName,String msg) {
    log(logName,msg,LOG_ERROR);
}
public static void info(String logName,String msg) {
    log(logName,msg,LOG_INFO);
}
public static void debug(String logName,String msg) {
	log(logName,msg,LOG_DEBUG);
}
public static void error(String logName,String msg, Throwable t) {
    Log log = LogFactory.getLog(logName);
    log.error(msg,t);
}
public static void error(Object logName,String msg, Throwable t) {
    Log log = LogFactory.getLog(logName.toString());
    log.error(msg,t);
}

}
