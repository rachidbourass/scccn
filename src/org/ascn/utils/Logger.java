package org.ascn.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.SailPointContext;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

/**
 * Description: Ascension Logger class. To be used only in Java classes/methods
 * Usage:
 */
public class Logger {

      private SailPointContext context;
      private String logLevel;
      private Log log;
      private static Log logLogger = LogFactory.getLog(Logger.class);

      private static String LOG_TRACE = "trace";
      private static String LOG_DEBUG = "debug";
      private static String LOG_INFO = "info";
      private static String LOG_WARN = "warn";
      private static String LOG_ERROR = "error";

      /**
       * log
       * @param name String
       * @param msg String
       * @param level String
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

      /**
       * trace
       * @param logName String
       * @param msg String
       */
      public static void trace(String logName,String msg) {
          log(logName,msg,LOG_TRACE);
      }

      /**
       * warn
       * @param logName String
       * @param msg String
       */
      public static void warn(String logName,String msg) {
          log(logName,msg,LOG_WARN);
      }

      /**
       * error
       * @param logName String
       * @param msg String
       */
      public static void error(String logName,String msg) {
          log(logName,msg,LOG_ERROR);
      }

      /**
       * info
       * @param logName String
       * @param msg String
       */
      public static void info(String logName,String msg) {
          log(logName,msg,LOG_INFO);
      }

      /**
       * debug
       * @param logName String
       * @param msg String
       */
      public static void debug(String logName,String msg) {
          log(logName,msg,LOG_DEBUG);
      }

      /**
       * error
       * @param logName String
       * @param msg String
       * @param t Throwable
       */
      public static void error(String logName,String msg, Throwable t) {
          Log log = LogFactory.getLog(logName);
          log.error(msg,t);
      }

      /**
       * error
       * @param logName Object
       * @param msg String
       * @param t Throwable
       */
      public static void error(Object logName,String msg, Throwable t) {
          Log log = LogFactory.getLog(logName.toString());
          log.error(msg,t);
      }
      //DO NOT USE THE BELOW METHODs
      public Logger(Class T) {
          this.log = LogFactory.getLog(T);
      }

      public Logger(String logName) {
          this.log = LogFactory.getLog(logName);
      }
      public Logger(Log log) {
          this.log = log;
      }

      public String getLogLevel() {
            return logLevel;
      }

      public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
      }
      /**
       *
       * @param message
       * @param t
       * @throws GeneralException
       * Description:
       *
       */
      public void logMessage(String message,Throwable t)  {
            try {

                  if(Util.nullSafeCaseInsensitiveEq(getLogLevel(), "error")) {
                        if(null != t) {
                              log.error(message, t);
                        }else {
                              log.error(message);
                        }
                  }else if(Util.nullSafeCaseInsensitiveEq(getLogLevel(), "debug")) {
                        log.debug(message);
                  }else if(Util.nullSafeCaseInsensitiveEq(getLogLevel(), "trace")) {
                        log.trace(message);
                  }else{
                        log.info(message);
                  }
            }catch(LogConfigurationException e) {
                  logLogger.error("ERROR: "+this.getClass().getName(), e);
                  throw new LogConfigurationException("ERROR: "+this.getClass().getName(),e);
            }
      }
      public void logMessage(String message) throws GeneralException {
          try {

                if(Util.nullSafeCaseInsensitiveEq(getLogLevel(), "error")) {
                      if(null != message) {
                            log.error(message);
                      }else {
                            log.error(message);
                      }
                }else if(Util.nullSafeCaseInsensitiveEq(getLogLevel(), "debug")) {
                      log.debug(message);
                }else if(Util.nullSafeCaseInsensitiveEq(getLogLevel(), "trace")) {
                      log.trace(message);
                }else{
                      log.info(message);
                }
          }catch(LogConfigurationException e) {
                logLogger.error("ERROR: "+this.getClass().getName(), e);
                throw new LogConfigurationException("ERROR: "+this.getClass().getName(),e);
          }
    }
      public void logMessage(String message, String level) throws GeneralException {
    	  message = message != null ? message : "NO LOG MESSAGE PROVIDED";
          try {

                if(Util.nullSafeCaseInsensitiveEq(level, "error")) {
                     log.error(message);
                }else if(Util.nullSafeCaseInsensitiveEq(level, "debug")) {
                      log.debug(message);
                }else if(Util.nullSafeCaseInsensitiveEq(level, "trace")) {
                      log.trace(message);
                }else{
                      log.info(message);
                }
          }catch(LogConfigurationException e) {
                logLogger.error("ERROR: "+this.getClass().getName(), e);
                throw new LogConfigurationException("ERROR: "+this.getClass().getName(),e);
          }
      }
      public void logMessage(String logName, String message, String level) throws GeneralException {
    	  message = message != null ? message : "NO LOG MESSAGE PROVIDED";
          try {

                if(Util.nullSafeCaseInsensitiveEq(level, "error")) {
                     log.error(message);
                }else if(Util.nullSafeCaseInsensitiveEq(level, "debug")) {
                      log.debug(message);
                }else if(Util.nullSafeCaseInsensitiveEq(level, "trace")) {
                      log.trace(message);
                }else{
                      log.info(message);
                }
          }catch(LogConfigurationException e) {
                logLogger.error("ERROR: In org.ascn.", e);
                throw new LogConfigurationException("ERROR: "+this.getClass().getName(),e);
          }
      }
      public void logTrace(String message) {
    	  message = message != null ? message : "NO LOG MESSAGE PROVIDED";
          log.trace(message);
      }
      public void logError(String message,Throwable t) {
    	  message = message != null ? message : "NO LOG MESSAGE PROVIDED";
          log.error(message,t);

      }


}