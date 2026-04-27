package org.ascn.utils;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

public class ConnectorUtils {

	public static DataSource getJDBCConnectorDS(Map<String,Object> inMap) {
		BasicDataSource bds = new BasicDataSource();
		bds.setUrl((String) inMap.get("url"));
		bds.setDriverClassName((String) inMap.get("driverClass"));
		bds.setUsername((String) inMap.get("user"));
		bds.setPassword((String) inMap.get("password"));
		bds.setInitialSize(10);
		bds.setMaxTotal(10);
		return bds;
	}
}

