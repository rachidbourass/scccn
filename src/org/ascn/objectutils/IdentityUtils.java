package org.ascn.objectutils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import sailpoint.api.Aggregator;
import sailpoint.api.ObjectUtil;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.Connector;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.Custom;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;
import sailpoint.object.ResourceObject;
import sailpoint.object.TaskResult;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class IdentityUtils {
    private SailPointContext context;
	public IdentityUtils() throws GeneralException {

		this.context = SailPointFactory.getCurrentContext();
	}
	/**
	 *
	 * @return
	 * @throws GeneralException
	 */
	public List<String> getExcludedAppsList() throws GeneralException{
		Custom cust = context.getObjectByName(Custom.class,"Acension-Custom-ExcludedApplications");
		List<String> appsList = (List<String>) cust.get("appNames");
		return appsList;
	}

	/**
	 *
	 * @param attribName
	 * @return
	 * @throws GeneralException
	 */
	 public List<Identity>  getNullAttributeIdentities(String attribName) throws GeneralException{
		QueryOptions qo = new QueryOptions();
	    Filter filter = Filter.ignoreCase(Filter.and(Filter.isnull(attribName), 		Filter.eq("inactive",false), Filter.eq("protected",false)));
	    qo.addFilter(filter);
	    qo.setResultLimit(200);
	    List<Identity> idn = context.getObjects(Identity.class, qo);
	    return idn;
	  }
	 /**
	  *
	  * @param attribName
	  * @param attribValue
	  * @return
	  * @throws GeneralException
	  */
	public List<Identity>  getNullAttributeIdentities(String attribName, String attribValue) {
	    QueryOptions qo = new QueryOptions();
	    List<Identity> idn = new ArrayList<>();
	    Filter filter = Filter.ignoreCase(Filter.and(Filter.isnull(attribName), Filter.eq(attribName,attribValue), Filter.eq("inactive",false), Filter.eq("protected",false)));
	    qo.addFilter(filter);
	    try {
	    qo.setResultLimit(10);
	     idn = context.getObjects(Identity.class, qo);
	    } catch(Exception e) {

	    }
	    return idn;
	  }

	/**
	 *
	 * @param identity
	 * @return
	 * @throws GeneralException
	 */
	public List<Link>  getIdADLinks(Identity identity) throws GeneralException{
	    QueryOptions qo = new QueryOptions();
	    Filter filter = Filter.ignoreCase(Filter.and(Filter.eq("application.type", "Active Directory - Direct"), Filter.eq("identity.name",identity.getName())));
	    qo.addFilter(filter);
	    List<Link> links = context.getObjects(Link.class, qo);
	    return links;
	  }

	/**
	 *
	 * @param identity
	 * @param appName
	 * @return
	 * @throws GeneralException
	 */
	public List<Link>  getIdentityAppLinks(Identity identity, String appName) throws GeneralException{
	    QueryOptions qo = new QueryOptions();
	    Filter filter = Filter.ignoreCase(Filter.and(Filter.eq("application.name", appName), 		Filter.eq("identity.name",identity.getName())));
	    qo.addFilter(filter);
	    List<Link> links = context.getObjects(Link.class, qo);
	    return links;
	  }

	/**
	 *
	 * @param identity
	 * @param filteredApp
	 * @return
	 * @throws GeneralException
	 */
	public List<Link>  getIdActiveADLinks(Identity identity, String filteredApp) throws GeneralException{
		    QueryOptions qo = new QueryOptions();

		    Filter filter = null;
		    filter = Filter.ignoreCase(Filter.and(Filter.eq("iiqDisabled", Boolean.FALSE), Filter.eq("application.type", "Active Directory - Direct"), Filter.eq("identity.name",identity.getName())));
		    if(null != filteredApp) {
		    	filter = Filter.ignoreCase(Filter.and(Filter.ne("application.name", filteredApp),Filter.eq("iiqDisabled", Boolean.FALSE), Filter.eq("application.type", "Active Directory - Direct"), Filter.eq("identity.name",identity.getName())));
		    }
		    qo.addFilter(filter);
		    List<Link> links = context.getObjects(Link.class, qo);
		    return links;
		  }

	public List<Link>  getIdFilteredActiveADLinks(Identity identity) throws GeneralException{
		    QueryOptions qo = new QueryOptions();
		    Filter cf = Filter.in("application.name", getExcludedAppsList());
		    Filter filter = null;
		    filter = Filter.ignoreCase(Filter.and( Filter.not(cf),Filter.eq("iiqDisabled", Boolean.FALSE), Filter.eq("application.type", "Active Directory - Direct"), Filter.eq("identity.name",identity.getName())));
		    qo.addFilter(filter);
		    List<Link> links = context.getObjects(Link.class, qo);
		    return links;
		  }
	public List<Link>  getIdActiveADLinks(Identity identity) throws GeneralException{
		    QueryOptions qo = new QueryOptions();
		    Filter filter = null;
		    filter = Filter.ignoreCase(Filter.and(Filter.eq("iiqDisabled", Boolean.FALSE), Filter.eq("application.type", "Active Directory - Direct"), Filter.eq("identity.name",identity.getName())));
		    qo.addFilter(filter);
		    List<Link> links = context.getObjects(Link.class, qo);
		    return links;
		  }

	public List<Identity>  getIDNWithoutManager(Filter f) throws GeneralException{
			List<Identity> outIdn = new ArrayList<>();
			QueryOptions qo = new QueryOptions();
			qo.addFilter(f);
			Iterator<Identity> iterator = context.search(Identity.class, qo);
			while (iterator.hasNext()) {
				Identity idn = iterator.next();
				outIdn.add(idn);
			}
			return outIdn;
		}

	public List<Object>  getGenericObject(Object inObj, QueryOptions qo) throws GeneralException{
		    List<Object> outObj = new ArrayList<>();
		    List<Application> appObj;
		    List<Link> linkObj;
		    List<Identity> idnObj;
		    if("Link".equalsIgnoreCase((String) inObj)) {
		    	linkObj = context.getObjects(Link.class, qo);
		    	outObj.add(linkObj);
		    }
		    if("Application".equalsIgnoreCase((String) inObj)) {
		    	appObj = context.getObjects(Application.class, qo);
		    	outObj.add(appObj);
		    }
		    if("Identity".equalsIgnoreCase((String) inObj)) {
		    	idnObj = context.getObjects(Identity.class, qo);
		    	outObj.add(idnObj);

		    }
		    return outObj;
		  }

	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public boolean runSingleAggregation(String nativeIdentity, String appName) throws GeneralException {
		  String accountName= nativeIdentity;
		  String applicationName = appName;
		  boolean result = false;
		  Application appObject = context.getObjectByName(Application.class, applicationName);
		      String appConnName = appObject.getConnector();
		      String errorMessage="";
		      Object appConnector = sailpoint.connector.ConnectorFactory.getConnector(appObject, null);

		      if(null == appConnector)
		      {
		          errorMessage = "Single Aggregation Rule: Failed to construct an instance of connector [" + appConnName + "]";
		          return result;
		      }

		      ResourceObject rObj = null;
		      boolean foundAccount =false;
		      try{
		        rObj = ((Connector) appConnector).getObject("account", accountName, null);
		        if(rObj != null){result = true; }
		        else{ result = false;}
		      }
		      catch(Throwable nsme){
		      }
		      // arguments to the account aggregation tasks.  Some suggested defaults are:
		      Attributes argMap = new Attributes();
		      argMap.put("applications", applicationName);
		      argMap.put("checkDeleted", "false");
		      argMap.put("checkHistory", "false");
		      argMap.put("checkPolicies", "false");
		      argMap.put("correlateEntitlements", "false");
		      argMap.put("correlateOnly", "false"); //Only set to this false if your intent is to create a net new Identity Cube containing only the AD account you're aggregating. This argument, when set to false, is the equivalent of deselecting the "Only create links if they can be correlated to an existing identity" checkbox on an Account Aggregation task.
		      argMap.put("correlateScope", "false");
		      argMap.put("deltaAggregation", "false");
		      argMap.put("enablePartitioning", "false");
		      argMap.put("haltOnMaxError", "false");
		      argMap.put("noAutoCreateApplications", "true");
		      argMap.put("noAutoCreateScopes", "true");
		      argMap.put("noNeedsRefresh", "false");
		      argMap.put("noOptimizeReaggregation", "true");
		      argMap.put("promoteManagedAttributes", "true");
		      argMap.put("refreshCertifications", "false");
		      argMap.put("refreshScorecard", "false");
		      argMap.put("sequential", "false");
		      argMap.put("taskCompletionEmailNotify", "Disabled");

		      // Construct an aggregator instance.
		      Aggregator agg = new Aggregator(context, argMap);

		      if(null == agg)
		      {
		          errorMessage = "Single Aggregation Rule: Null Aggregator returned from constructor. Unable to Aggregate!";
		          return result;
		      }

		      // Invoke the aggregation task by calling the aggregate() method.
		      // Note: the aggregate() call may take serveral seconds to complete.
		      TaskResult taskResult = agg.aggregate(appObject, rObj);
		      if (null == taskResult)
		      {
		          errorMessage = "Single Aggregation Rule: ERROR: Null taskResult returned from aggregate() call.";
		          return result;
		      }
		      else
		      {

		      }

		      return result;
	  }
	public boolean iiqUserExists(String searchAttrib, String searchValue) throws GeneralException {
		boolean iiqUserExists = false;
		Filter f = Filter.ignoreCase(Filter.and(Filter.eq(searchAttrib, searchValue)));
		QueryOptions qo = new QueryOptions();
		Identity identity = (Identity) context.getObjects(Identity.class, qo);
	    if(null != identity) {
	    	iiqUserExists = true;
	    }
	    return iiqUserExists;
	}

	public List<String> addAllWorkGroupMembers(String wgName) {
		List<String> outList = new ArrayList<>();
		try {
		Identity wg = context.getObjectByName(Identity.class, wgName);
		List<String> attribs = new ArrayList<>();
		attribs.add("id");
		Iterator<Object[]> members = ObjectUtil.getWorkgroupMembers(context, wg, attribs);
		if (members.hasNext()){
		     Object[] results = members.next();
		     Identity id= context.getObjectById(Identity.class,results[0].toString());
		     id.add(wg);
		     outList.add(id.toString());
		     context.saveObject(wg);
		     context.commitTransaction();
		     context.decache(id);
		}
		Util.flushIterator(members);
		} catch (Exception e) {

		}
	    return outList;
	}

	public List<String> addWorkGroupMembers(String wgName, List<String> idNames) {
		List<String> outList = new ArrayList<>();
		try {
			Identity wg = context.getObjectByName(Identity.class, wgName);
			List<String> attribs = new ArrayList<>();
			attribs.add("id");
			attribs.add("name");
			Iterator<Object[]> members = ObjectUtil.getWorkgroupMembers(context, wg, attribs);
			if (members.hasNext()){
			     Object[] results = members.next();
			     Object memberId = results[0] != null ? results[0].toString() : null;
			     String memberName = results[1] != null ? results[1].toString() : null;
			     if(!StringUtils.isBlank(memberName) && idNames.contains(memberName.toLowerCase())) {
			    	 Identity id= context.getObjectByName(Identity.class,memberName);
			    	 if (null != id) {
				     	 id.add(wg);
					     outList.add(id.toString());
					     context.saveObject(wg);
					     context.commitTransaction();
					     context.decache(id);
			    	 }
				}
			}
			Util.flushIterator(members);

		} catch (Exception e) {

		}
	    return outList;
	}

	public List<String> removeWorkGroupMembers(String wgName, List<String> idNames) {
		List<String> outList = new ArrayList<>();
		try {
			Identity wg = context.getObjectByName(Identity.class, wgName);
			List<String> attribs = new ArrayList<>();
			attribs.add("id");
			attribs.add("name");
			Iterator<Object[]> members = ObjectUtil.getWorkgroupMembers(context, wg, attribs);
			if (members.hasNext()){
			     Object[] results = members.next();
			     Object memberId = results[0] != null ? results[0].toString() : null;
			     String memberName = results[1] != null ? results[1].toString() : null;
			     if(!StringUtils.isBlank(memberName) && idNames.contains(memberName.toLowerCase())) {
			    	 Identity id= context.getObjectByName(Identity.class,memberName);
			    	 if (null != id) {
				     	 id.remove(wg);
					     outList.add(id.toString());
					     context.saveObject(wg);
					     context.commitTransaction();
					     context.decache(id);
			    	 }
				}
			}
			Util.flushIterator(members);

		} catch (Exception e) {

		}
	    return outList;
	}


}
