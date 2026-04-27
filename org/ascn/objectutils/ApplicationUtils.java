package org.ascn.objectutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.ascn.utils.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Application;
import sailpoint.tools.GeneralException;

public class ApplicationUtils {
	
	private Logger log;
	private String logLevel;
	private SailPointContext context;
	private String domainDN;
	private String appName;
	private String jdbcSQL;
	public String getAppName() {
		return appName;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
		
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getDomainDN() {
		return domainDN;
	}
	public void setDomainDN(String domainDN) {
		this.domainDN = domainDN;
	}
	public String getJdbcSQL() {
		return jdbcSQL;
	}
	public void setJdbcSQL(String jdbcSQL) {
		this.jdbcSQL = jdbcSQL;
	}
	public ApplicationUtils() throws GeneralException {
		this.context = SailPointFactory.getCurrentContext();
		this.log = new Logger(getClass());
		if(null == logLevel) {
			setLogLevel("info");
		}
	}
	
	/*
	 * getJDBCConfig() gets JDBC Connector connection configuration from the application
	 */
	public DataSource getJDBCConfig() throws GeneralException{
		log.logMessage("BEGIN: Executing Method: getJDBCConnectorConfig()");
		Map<String,Object> appConfigMap = new HashMap<>();
		DataSource ds = null;
		try {
			Application application = context.getObjectByName(Application.class, getAppName());
			if(null != application){
				appConfigMap = new HashMap<>();
				appConfigMap.put("driverClass",application.getAttributeValue("authenticationType"));
				appConfigMap.put("url",application.getAttributeValue("user"));
				String bindPwd = (String) application.getAttributeValue("password");
				String deBindPwd = context.decrypt(bindPwd);
				appConfigMap.put("password",deBindPwd);
				appConfigMap.put("user",application.getAttributeValue("user"));
				setJdbcSQL((String) application.getAttributeValue("getObjectSQL"));
			}
			ds = ConnectorUtils.getJDBCConnectorDS(appConfigMap);
		}catch(Exception e) {
			log.logMessage("END: Executing Method: getJDBCConnectorConfig()",e);
		}
		log.logMessage("END: Executing Method: getJDBCConnectorConfig()");
		return ds;
	}
	/*
	 * getADConfig() gets Active Director - Direct connection configuration from the application
	 */
	public Map<String,Object> getADConfig() throws GeneralException{
		log.logMessage("BEGIN: Executing Method: getADConfig()");
		Map<String,Object> ADSettingMap = null;
		String ldapProtocol = "ldap";
		String ldapUrl = null;
		try {
		Application application = context.getObjectByName(Application.class, getAppName());
		Object domainSettings = application.getAttributeValue("domainSettings");
		
		if(domainSettings instanceof List){
			List<Map<String,Object>> domainSettingList = (List<Map<String, Object>>) domainSettings;
	    for(Map<String,Object> mapObj : domainSettingList){
	      
	      if(mapObj.get("domainDN").toString().equalsIgnoreCase(getDomainDN())){
	        ADSettingMap = new HashMap<>();
	        ADSettingMap.put("authType",mapObj.get("authenticationType"));
			ADSettingMap.put("userName",mapObj.get("user"));
			String bindPwd = (String) mapObj.get("password");
			String deBindPwd = context.decrypt(bindPwd);
			ADSettingMap.put("userPassword",deBindPwd);
			Object useSSLObj = mapObj.get("useSSL");
			String useSSL;
				if(useSSLObj instanceof Boolean) {
					if((boolean) useSSLObj) {
						ADSettingMap.put("userSSL", "ssl");
						ldapProtocol = "ldaps";
					}
				}
			Object hostObj = mapObj.get("servers");
			String port = (String) mapObj.get("port");
			ADSettingMap.put("ldapPort",port);
			if(hostObj instanceof List){
				List<Object> configList = (List<Object>) hostObj;
				 for(Object h:configList){
					    if(null == ldapUrl){
			          ldapUrl = ldapProtocol+"://"+h+":"+port;
			        }else{
			          ldapUrl = ldapProtocol+"://"+h+":"+port+" "+ldapUrl;
			        }
			      }
							ADSettingMap.put("ldapUrl",ldapUrl);
			}
	        
	    }
	  }
	  	  
	}
		}catch(Exception e) {
			log.logMessage("END: Executing Method: getADConfig()",e);
		}
		log.logMessage("END: Executing Method: getADConfig()");
		return ADSettingMap;
	}
	
}
