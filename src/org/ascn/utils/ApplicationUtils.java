package org.ascn.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Application;

public class ApplicationUtils {

	private Logger log;
	private static String domainDN;
	private String appName;
	private String jdbcSQL;

	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public static String getDomainDN() {
		return domainDN;
	}
	public void setDomainDN(String domainDN) {
		ApplicationUtils.domainDN = domainDN;
	}
	public String getJdbcSQL() {
		return jdbcSQL;
	}
	public static void setJdbcSQL(String jdbcSQL) {
		jdbcSQL = jdbcSQL;
	}

	/*
	 * getJDBCConfig() gets JDBC Connector connection configuration from the application
	 * String appName
	 * return DataSource
	 */
	public static DataSource getJDBCConfig(String appName){

		Map<String,Object> appConfigMap = new HashMap<>();
		DataSource ds = null;
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			Application application = context.getObjectByName(Application.class, appName);
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
			//TO-DO
		}

		return ds;
	}

	/*
	 * getADConfig() gets Active Director - Direct connection configuration from the
	 * application
	 * String appName
	 * return Map<String, Object> of Application Configuration
	 *
	 */
	public static Map<String,Object> getADConfig(String appName){

		Map<String,Object> ADSettingMap = null;
		String ldapProtocol = "ldap";
		String ldapUrl = null;
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			Application application = context.getObjectByName(Application.class, appName);
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

		}

		return ADSettingMap;
	}


}
