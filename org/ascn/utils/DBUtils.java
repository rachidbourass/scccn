package org.ascn.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.ascn.objectutils.ApplicationUtils;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class DBUtils {

	private Logger log;
	private String logLevel;
	private SailPointContext context;
	private String tableName;
	private List<Object> singleFieldDataObject;
	private List<Map<String,Object>> multiFieldDataObject;
	private ApplicationUtils appUtils;
	public DBUtils() throws GeneralException {
		
		 this.context = SailPointFactory.getCurrentContext();
		 log = new Logger(this.getClass());
		 if(null == logLevel) {
				log.setLogLevel("info");
		}
         
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}
	
	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public List<Object> getDataList(String fieldName, String tableName,String whereClause, String orderBy) throws GeneralException{
		log.logMessage("BEGIN : getDataList");
		List<Object> out = new ArrayList<>();
		try{
		//Below we are getting SailPoint Database Connection
		//Do not close this connection, it will affect the entire 
		//SailPoint Instance
		String sql = "SELECT DISTINCT "+fieldName+" FROM "+tableName;
		if(Util.isNotNullOrEmpty(whereClause)) {
			sql = sql+" "+whereClause;
		}
		if(Util.isNotNullOrEmpty(orderBy)) {
			sql = sql+" ORDER BY "+orderBy;
		}
		 Connection conn = context.getJdbcConnection(); //context.getJdbcConnection();
		 PreparedStatement ps = null;
         ResultSet rs = null;
		if(Util.isNotNullOrEmpty(conn.toString())) {
			try{
	          ps = conn.prepareStatement(sql); 
	          rs = ps.executeQuery();
			  while(rs.next()) {
					out.add(rs.getObject(1));					
			  }
					
			}catch (SQLException e) {
				log.logMessage("ERROR: Executing Query: "+sql, e);
			}finally{
		        try {
					rs.close();
				} catch (SQLException e) {
					log.logMessage("ERROR: Closing ResultSet: "+tableName, e);
				}
		        try {
					ps.close();
				} catch (SQLException e) {
					log.logMessage("ERROR: Closing PreparedStatement: "+tableName, e);
				}
	       }
		}
		}catch(Exception e) {
			log.logMessage("ERROR: In Method: "+this.getClass().getName(), e);
		}
		log.logMessage("END : getDataList");
		return out;
	}
	
	public void prepareData(String fieldName, String tableName,String whereClause, String orderBy) throws GeneralException{
		log.logMessage("BEGIN : prepareData(String fieldName, String tableName,String whereClause, String orderBy)");
		List<Object> outList = new ArrayList<>();
		List<Map<String,Object>> outListMap = new ArrayList<>();
		try{
		//Below we are getting SailPoint Database Connection
		//Do not close this connection, it will affect the entire 
		//SailPoint Instance
		String sql = "SELECT DISTINCT "+fieldName+" FROM "+tableName;
		if(Util.isNotNullOrEmpty(whereClause)) {
			sql = sql+" "+whereClause;
		}
		if(Util.isNotNullOrEmpty(orderBy)) {
			sql = sql+" ORDER BY "+orderBy;
		}
		List<String> colNames = new ArrayList<>();
		 Connection conn = context.getJdbcConnection(); //context.getJdbcConnection();
		 if(Util.isNotNullOrEmpty(conn.toString())) {
			 PreparedStatement ps = conn.prepareStatement(sql); 
			 ResultSet rs = ps.executeQuery();
			try{
	           if(null != rs) {
	        	    ResultSetMetaData rsmd = rs.getMetaData();
	        	    int i = 0;
	        	    while(i < rsmd.getColumnCount()) {
	        	    i++;
	        	    	log.logMessage("COULUMNS: "+rsmd.getColumnName(i));
	        	    	colNames.add(rsmd.getColumnName(i));
	        	    }
	        	    int colSize = 0;
	        	    colSize = colNames.size();
					while(rs.next()) {
						Map<String,Object> recMap = new HashMap<>();
						for(String c : colNames){
				            if(colSize == 1){
				            	outList.add(rs.getObject(c));	
				             } else if(colSize > 1) {
				            	 recMap.put(c, rs.getObject(c));
				            	 outListMap.add(recMap);
							 }
				        }
					}
					
					if(!outList.isEmpty()) {
						setSingleColumnData(outList);
					}
					if(!outListMap.isEmpty()) {
						setMultiColumnData(outListMap);
						throw new Exception("");
					}
	           }
					
			}catch (SQLException e) {
				log.logMessage("ERROR: Executing Query: "+sql, e);
			}finally {
				log.logMessage("Closing PreparedStatement and ResultSet");
				rs.close();
				ps.close();
			}
		}
		}catch(Exception e) {
			log.logMessage("ERROR: In Method: "+this.getClass().getName(), e);
		}
		log.logMessage("END : getDataList");
		
	}
	
	public void prepareData(String sql) throws GeneralException{
		log.logMessage("BEGIN : prepareData");
		List<Object> outList = new ArrayList<>();
		List<Map<String,Object>> outListMap = new ArrayList<>();
		try{
		//Below we are getting SailPoint Database Connection
		//Do not close this connection, it will affect the entire 
		//SailPoint Instance
			List<String> colNames = new ArrayList<>();
			 Connection conn = context.getJdbcConnection(); //context.getJdbcConnection();
			 if(Util.isNotNullOrEmpty(conn.toString())) {
				 PreparedStatement ps = conn.prepareStatement(sql); 
				 ResultSet rs = ps.executeQuery();
				try{
		           if(null != rs) {
		        	    ResultSetMetaData rsmd = rs.getMetaData();
		        	    int i = 0;
		        	    while(i < rsmd.getColumnCount()) {
		        	    i++;
		        	    	log.logMessage("COULUMNS: "+rsmd.getColumnName(i));
		        	    	colNames.add(rsmd.getColumnName(i));
		        	    }
		        	    int colSize = 0;
		        	    colSize = colNames.size();
						while(rs.next()) {
							Map<String,Object> recMap = new HashMap<>();
							for(String c : colNames){
					            if(colSize == 1){
					            	outList.add(rs.getObject(c));	
					             } else if(colSize > 1) {
					            	 recMap.put(c, rs.getObject(c));
					            	 outListMap.add(recMap);
								 }
					        }
						}
						if(!outList.isEmpty()) {
							setSingleColumnData(outList);
						}
						if(!outListMap.isEmpty()) {
							setMultiColumnData(outListMap);
						}
						
		           }
						
				}catch (SQLException e) {
					log.logMessage("ERROR: Executing Query: "+sql, e);
				}finally {
					log.logMessage("Closing PreparedStatement and ResultSet");
					rs.close();
					ps.close();
				}
			}
			}catch(Exception e) {
				log.logMessage("ERROR: In Method: "+this.getClass().getName(), e);
			}
			log.logMessage("END : getDataList");
			
	}
	
	public void getJDBCConnectorData(String sql, String appName) throws GeneralException{
		log.logMessage("BEGIN : prepareData");
		List<Object> outList = new ArrayList<>();
		List<Map<String,Object>> outListMap = new ArrayList<>();
		Connection conn = null;
		try{
			List<String> colNames = new ArrayList<>();
			appUtils.setAppName(appName);
			DataSource ds = appUtils.getJDBCConfig();
			 conn = ds.getConnection();
			 if(Util.isNotNullOrEmpty(conn.toString())) {
				 if(null == sql) {
					 sql = appUtils.getJdbcSQL();
				 }
				 PreparedStatement ps = conn.prepareStatement(sql); 
				 ResultSet rs = ps.executeQuery();
				try{
		           if(null != rs) {
		        	    ResultSetMetaData rsmd = rs.getMetaData();
		        	    int i = 0;
		        	    while(i < rsmd.getColumnCount()) {
		        	    i++;
		        	    	log.logMessage("COULUMNS: "+rsmd.getColumnName(i));
		        	    	colNames.add(rsmd.getColumnName(i));
		        	    }
		        	    int colSize = 0;
		        	    colSize = colNames.size();
						while(rs.next()) {
							Map<String,Object> recMap = new HashMap<>();
							for(String c : colNames){
					            if(colSize == 1){
					            	outList.add(rs.getObject(c));	
					             } else if(colSize > 1) {
					            	 recMap.put(c, rs.getObject(c));
					            	 outListMap.add(recMap);
								 }
					        }
						}
						
						if(!outList.isEmpty()) {
							setSingleColumnData(outList);
						}
						if(!outListMap.isEmpty()) {
							setMultiColumnData(outListMap);
						}
		           }
						
				}catch (SQLException e) {
					log.logMessage("ERROR: Executing Query: "+sql, e);
				}finally {
					log.logMessage("Closing PreparedStatement and ResultSet");
					rs.close();
					ps.close();
				}
			}
			}catch(Exception e) {
				log.logMessage("ERROR: In Method: "+this.getClass().getName(), e);
			}
			log.logMessage("END : getDataList");
	}
	
	private void setSingleColumnData(List<Object> inObject){
		this.singleFieldDataObject = new ArrayList<>();
		this.singleFieldDataObject = inObject;
	}
	public List<Object> getSingleColumnData(){
		return singleFieldDataObject;
	}
	private void setMultiColumnData(List<Map<String,Object>> inObject){
		this.multiFieldDataObject = new ArrayList<>();
		this.multiFieldDataObject = inObject;
	}
	public List<Map<String,Object>> getMultiColumnData(){
		return multiFieldDataObject;
	}
	
}
