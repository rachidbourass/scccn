package org.ascn.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Attributes;
import sailpoint.object.Bundle;
import sailpoint.object.Custom;
import sailpoint.object.Filter;
import sailpoint.object.Filter.LeafFilter;
import sailpoint.object.Identity;
import sailpoint.object.IdentityRequest;
import sailpoint.object.Link;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.Profile;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

/**
 * Utility class for performing various query operations related to identity
 * management in SailPoint.
 */
public class QueryUtils {
    /**
     * Logger instance for logging information and errors related to query
     * operations.
     */
    private static final Log log = LogFactory.getLog(QueryUtils.class);
    private static final String LOG_NAME = "org.ascn.utils.QueryUtils";

    /**
     * Retrieves the current SailPointContext.
     * <p>
     * This method attempts to get the current SailPoint context from the
     * SailPointFactory. If the context cannot be obtained, a RuntimeException is
     * thrown.
     * </p>
     *
     * @return the current SailPointContext
     * @throws RuntimeException if unable to obtain the SailPointContext
     */
    private static SailPointContext getSPContext() {
        SailPointContext context = null;
        try {
            context = SailPointFactory.getCurrentContext();
        } catch (Exception e) {
            throw new RuntimeException("Unable to get SailPointContext: ", e);
        }
        return context;
    }

    /**
     * Retrieves the Identity Object by Name or ID.
     *
     * <p>
     * This method attempts to get the Identity Object based on the provided Name or
     * ID. This method checks if the provided name or ID is blank, the method
     * returns null. Otherwise, it attempts to retrieve the Identity object. If the
     * Identity cannot be obtained, a RuntimeException is thrown.
     * </p>
     *
     * @param nameOrId the name or ID of the identity to be retrieved; must not be
     *                 blank
     * @return the Identity Object
     * @throws RuntimeException if unable to obtain the Identity Object
     */
    public static Identity getIdentityByNameOrId(String nameOrId) {
        if (StringUtils.isBlank(nameOrId)) {
            return null;
        }
        try {
            return getSPContext().getObject(Identity.class, nameOrId);
        } catch (Exception e) {
            throw new RuntimeException("Could not get Identity for: " + nameOrId, e);
        }
    }

    /**
     * Retrieves an object of the specified class based on a given attribute name
     * and its value.
     * <p>
     * This method searches for an object of type clazz where the attribute
     * specified by attribName matches the provided attribValue. The search is
     * limited to returning a single result. If found, the method returns the
     * object.
     * </p>
     *
     * @param clazz       the class type of the object to be retrieved
     * @param attribName  the name of the attribute to search by
     * @param attribValue the value of the attribute to match
     * @return the object found matching the specified attribute, or null if no
     *         matching object is found
     * @throws IllegalArgumentException if clazz, attribName, or attribValue is null
     *                                  or empty.
     * @throws RuntimeException         if an error occurs during the search process
     */
    public static Object getObjectByAttribute(Class clazz, String attribName, Object attribValue) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class can not be null");
        }
        if (StringUtils.isBlank(attribName)) {
            throw new IllegalArgumentException("Attribute Name can not be null");
        }
        if (attribValue == null || attribValue == "") {
            throw new IllegalArgumentException("Attribute Value can not be null");
        }
        QueryOptions qo = new QueryOptions();
        qo.add(Filter.ignoreCase(Filter.eq(attribName, attribValue)));
        qo.setResultLimit(1);
        String outVal = null;
        try {
            @SuppressWarnings("unchecked")
            Iterator<Object> it = getSPContext().search(clazz, qo, "name");
            while (it.hasNext()) {
                Object[] row = (Object[]) it.next();
                outVal = (String) row[0];
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while getting Object By Attribute: " + attribName, e);
        }
        return outVal;
    }

    /**
     * Retrieves an Identity object based on a given attribute name and its value.
     * <p>
     * This method searches for an Identity object where the attribute specified by
     * attribName matches the provided attribValue. If found, the method returns the
     * object.
     * </p>
     *
     * @param attribName  the name of the attribute to search by
     * @param attribValue the value of the attribute to match
     * @return the object found matching the specified attribute, or null if no
     *         matching object is found
     * @throws RuntimeException if an error occurs during the search process
     */
    public static Object getIdentityByAttributeName(String attribName, Object attribValue) {
        if (StringUtils.isBlank(attribName)) {
            return null;
        }
        try {
            return getObjectByAttribute(Identity.class, attribName, attribValue);
        } catch (Exception e) {
            throw new RuntimeException("Could not get Identity for: " + attribName, e);
        }
    }

    /**
     * Retrieves a List of Link objects of an Identity based on a given identity
     * name and application name.
     * <p>
     * This method searches for Link objects based on specified attribute
     * identityName and appName. If found, the method returns the List of Link
     * objects.
     * </p>
     *
     * @param identityName the name of the Identity to search by
     * @param appName      the name of the application to search by
     * @return the list of Link objects found based on the specified identity name
     *         and application name
     * @throws RuntimeException if identityName, or appName is blank.
     * @throws RuntimeException if an error occurs during the search process
     */
    public static List<Link> getIdentityAppLinks(String identityName, String appName) {
        List<Link> outList = new ArrayList<>();
        if (StringUtils.isBlank(appName)) {
            throw new RuntimeException("Application Name can not be null to get Identity Links");
        }
        if (StringUtils.isBlank(identityName)) {
            throw new RuntimeException("Identity Name or ID can not be null to get Identity Links");
        }
        QueryOptions qo = new QueryOptions();
        try {
            Filter filter = Filter.ignoreCase(
                    Filter.and(Filter.eq("application.name", appName), Filter.eq("identity.name", identityName)));
            qo.addFilter(filter);

            outList = getSPContext().getObjects(Link.class, qo);
        } catch (Exception e) {
            throw new RuntimeException("Exception Occured when querying Links for Identity Name: " + identityName
                    + " on application: " + appName);
        }
        return outList;
    }

    /**
     * Retrieves all the link objects of an Identity based on a given identity name.
     * <p>
     * This method searches for all the Link objects of an identity based on
     * specified attribute identityName. If found, the method returns the List of
     * Link objects.
     * </p>
     *
     * @param identityName the name of the Identity to search by
     * @return the list of Link objects found based on the specified identity name
     * @throws RuntimeException if identityName is blank.
     * @throws RuntimeException if an error occurs during the search process
     */
    public static List<Link> getAllIdentityLinks(String identityName) {
        List<Link> outList = new ArrayList<>();
        if (StringUtils.isBlank(identityName)) {
            throw new RuntimeException("Identity Name or ID can not be null to get Identity Links");
        }
        QueryOptions qo = new QueryOptions();
        try {
            Filter filter = Filter
                    .ignoreCase(Filter.and(Filter.or(Filter.eq("name", identityName), Filter.eq("id", qo))));
            qo.addFilter(filter);
            List<Identity> idnList = getSPContext().getObjects(Identity.class, qo);
            if (idnList.size() > 0) {
                for (Identity idn : idnList) {
                    outList = idn.getLinks();
                }
            } else {
                throw new RuntimeException("Identity not found for :" + identityName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception Occured when querying Links for Identity Name: " + identityName);
        }
        return outList;
    }

    /**
     * Retrieves the Native Id's of an identity from all the application it has
     * accounts on.
     * <p>
     * This method searches for all the Link objects of an identity based on
     * specified attribute identityName. It then iterates over all the links and
     * fetch the Native Id's and return the list containing a map of application
     * name and native Id's.
     * </p>
     *
     * @param identityName the name of the Identity to search by
     * @return the list containing a map of application name and native Id's of the
     *         specified identity
     * @throws RuntimeException if identityName is blank.
     */
    public static List<Map<String, String>> getNativeIds(String identityName) {
        List<Map<String, String>> outList = new ArrayList<>();
        if (StringUtils.isBlank(identityName)) {
            throw new RuntimeException("Identity Name or ID can not be null to get Identity Links");
        }
        List<Link> idnLinks = getAllIdentityLinks(identityName);
        if (idnLinks.size() > 0) {
            for (Link l : idnLinks) {
                Map<String, String> linkMap = new HashMap<>();
                linkMap.put(l.getApplicationName(), l.getNativeIdentity());
                outList.add(linkMap);
            }
        }
        return outList;
    }

    /**
     * Checks if there are any links associated with the specified identity and
     * application.
     * <p>
     * This method retrieves the list of links for the given identity name and
     * application name. It returns true if the list contains one or more links, and
     * false otherwise.
     * </p>
     *
     * @param identityName the name of the identity to check for links
     * @param appName      the name of the application to check for links
     * @return true if there are links between the identity and the application,
     *         false otherwise
     */
    public static boolean hasLinks(String identityName, String appName) {
        boolean exists = false;
        List<Link> links = getIdentityAppLinks(identityName, appName);
        if (links.size() > 0) {
            exists = true;
        }
        return exists;
    }

    /**
     * Checks if there are any links associated with the specified identity and
     * List of application names.
     * <p>
     * This method retrieves the list of links for the given identity name and
     * application name. It returns true if the list contains one or more links, and
     * false otherwise.
     * </p>
     *
     * @param identityName the name of the identity to check for links
     * @param appNames List    the name of the application to check for links
     * @return true if there are links between the identity and the application,
     *         false otherwise
     */
    public static boolean hasLinks(String identityName, List<String> appNames) {
    	String methodName = LOG_NAME+" hasLinks(String identityName, List<String> appNames): ";
        boolean exists = false;
        try {
	        SailPointContext context = SailPointFactory.getCurrentContext();
	        Filter f = Filter.ignoreCase(Filter.and(Filter.in("application.name", appNames),Filter.eq("identity.name",identityName)));
	        QueryOptions qo = new QueryOptions();
	        qo.add(f);
	        List<Link> links = context.getObjects(Link.class,qo);
	        if(links.size() > 0) {
	        	exists = true;
	        }

        } catch (Exception e) {
        	throw new RuntimeException(methodName+" ERROR: "+e,e);
        }
        return exists;
    }


    /**
     * Retrieves the email address for notifying based on skip levels in a
     * hierarchical structure. If the provided skip level number exceeds a
     * configurable threshold, the default email address is returned.
     * <p>
     * The method traverses the management hierarchy starting from the given
     * currentIdentity up to the specified skipLevelNumber times to find the
     * appropriate manager's email. If a manager is inactive or lacks an email
     * address, the defaultEmail is returned.
     * </p>
     *
     * @param currentIdentity The starting identity from which the manager hierarchy
     *                        is traversed.
     * @param skipLevelNumber The number of levels to skip in the hierarchy to find
     *                        the appropriate manager.
     * @param defaultEmail    The default email address to be used if conditions for
     *                        obtaining manager's email are not met.
     * @param customObj       An optional custom object containing configuration
     *                        settings, like skip level thresholds and default Email
     *                        address.
     * @return The email address of the manager at the specified skip level, or the
     *         default email if conditions are not met.
     * @throws RuntimeException if customObj is empty.
     */
    public static String getSkipLevelNotificationEmail(Identity currentIdentity, int skipLevelNumber,
            String defaultEmail, Custom customObj) {
        int skipLevelThresold = 0;
        Identity nthManager = null;
        if (customObj != null) {
            skipLevelThresold = Integer.parseInt((String) customObj.get("skipLevelThresholdforJoinerNotification"));
            // the default skip level threshold value used to determine the toEmail
            // attribute value
        } else {
            throw new RuntimeException("Custom Object cannot be null for fetching skipLevel threshold");
        }
        if (skipLevelNumber > skipLevelThresold) {
            return defaultEmail;
        } else {
            for (int i = 0; i < skipLevelNumber; i++) {
                nthManager = currentIdentity.getManager();
                if (nthManager == null) {
                    return defaultEmail;
                }
                currentIdentity = nthManager;
            }
            if (nthManager != null && !nthManager.isInactive() && nthManager.getEmail() != null) {
                return nthManager.getEmail();
            } else {
                return defaultEmail;
            }
        }
    }

    /**
     * Gets the managed attribute ID for a given application, entitlement and value.
     *
     * @param appName     required argument application name
     * @param attribName  required attribute name
     * @param attribValue attribute value, this should be a string value
     * @return ID of the ManagedAttribute ID
     */
    public static String getManagedAttributeId(String appName, String attribName, Object attribValue) {
        String outValue = null;
        if (appName == null || attribName == null || attribValue == null) {
            throw new RuntimeException();
        }
        QueryOptions qo = new QueryOptions();
        qo.addFilter(Filter.eq("application.name", appName));
        qo.addFilter(Filter.eq("attribute", attribName));
        qo.addFilter(Filter.eq("value", attribValue));
        try {
            Iterator<Object[]> it = SailPointFactory.getCurrentContext().search(ManagedAttribute.class, qo, "id");
            while (it.hasNext()) {
                Object[] row = it.next();
                outValue = (String) row[0];
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting ManagedAttribute ID in QueryUtils.getManagedAttributeId(...)");
        }
        return outValue;
    }

    /**
     * Gets the managed attribute Name for a given application, entitlement and
     * value.
     *
     * @param appName     required argument application name
     * @param attribName  required attribute name
     * @param attribValue attribute value, this should be a string value
     * @return ID of the ManagedAttribute ID
     */
    public static ManagedAttribute getManagedAttribute(String appName, String attribName, Object attribValue) {
        if (appName == null || attribName == null || attribValue == null) {
            throw new RuntimeException();
        }
        QueryOptions qo = new QueryOptions();
        qo.addFilter(Filter.eq("application.name", appName));
        qo.addFilter(Filter.eq("attribute", attribName));
        qo.addFilter(Filter.eq("value", attribValue));
        ManagedAttribute attribute = null;
        try {
            List<ManagedAttribute> attributes = SailPointFactory.getCurrentContext().getObjects(ManagedAttribute.class,
                    qo);
            if (!attributes.isEmpty()) {
                attribute = attributes.get(0);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting ManagedAttribute ID in QueryUtils.getManagedAttributeId(...)");
        }
        return attribute;
    }

    /**
     * Gets the managed attribute Name for a given application, entitlement and
     * value.
     *
     * @param appName     required argument application name
     * @param attribName  required attribute name
     * @param attribValue attribute value, this should be a string value
     * @return ID of the ManagedAttribute ID
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getEntitlementAttributes(String appName, String attribName, Object attribValue) {
        if (appName == null || attribName == null || attribValue == null) {
            throw new RuntimeException();
        }
        QueryOptions qo = new QueryOptions();
        qo.addFilter(Filter.eq("application.name", appName));
        qo.addFilter(Filter.eq("attribute", attribName));
        if (attribValue instanceof List) {
        	List attrValList = (List<String>) attribValue;
        	qo.addFilter(Filter.in("value", attrValList));
        } else {
        	qo.addFilter(Filter.eq("value", attribValue));
        }
        Map<String, Object> attributes = new HashMap<>();
        try {
            Iterator it = SailPointFactory.getCurrentContext().search(ManagedAttribute.class, qo, "attributes");
            if (it != null && it.hasNext()) {
                attributes = (Map<String, Object>) it.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting Entitlement Attributes in QueryUtils.getEntitlementAttributes(...)");
        }
        return attributes;
    }

    /**
     * This method will return all Links of a given Application Type
     */
    public List<Link> getIdnLinks(String identityName, String appType, boolean inactive) {
        QueryOptions qo = new QueryOptions();
        List<Link> links = new ArrayList<>();
        try {
            Filter filter = Filter.ignoreCase(Filter.and(Filter.eq("application.type", appType),Filter.eq("identity.name", identityName), Filter.eq("identity.inactive", inactive)));
            qo.addFilter(filter);
            links = SailPointFactory.getCurrentContext().getObjects(Link.class, qo);
        } catch (Exception e) {
            throw new RuntimeException("Error getting links for identity: " + identityName + " " + e, e);
        }
        return links;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void generateProvisioningPlanIIQ(String idName, String operation) {
    	String WORKFLOW_NAME="LCM Provisioning";
		ProvisioningPlan plan = new ProvisioningPlan();
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			plan.setNativeIdentity(idName);
			plan.setIdentity(context.getObjectByName(Identity.class,idName));
			String caseName=WORKFLOW_NAME+" - "+operation+" - "+idName;
			Attributes wfArgs = new Attributes();
			wfArgs.put("identityName",idName);
			wfArgs.put("plan",plan);
			wfArgs.put("flow","Add / Remove Workgroup Member");
			wfArgs.put("approvalScheme","none");
			wfArgs.put("notificationScheme","none");
			wfArgs.put("policyScheme","none");
			long systemtime = System.currentTimeMillis();
			ScheduledWorkflowRule.scheduleWorkflow(WORKFLOW_NAME, caseName, systemtime, wfArgs, "spadmin");
		} catch(Exception e) {
			throw new RuntimeException("ERROR Executing Scheduled Workflow Rule");
		}
    }
    /**
     * Check if an entitlement esists using the attribut name and displayName
     * @param appName
     * @param attribName
     * @param attribVal
     * @return
     */
    public boolean checkEntitlementByDisplayName(String appName, String attribName, String attribVal) {
        boolean exists = false;
        QueryOptions qo = new QueryOptions();
        qo.addFilter(Filter.eq("application.name", appName));
        qo.addFilter(Filter.eq("attribute", attribName));
        qo.addFilter(Filter.eq("displayName", attribVal));

        Iterator < ManagedAttribute > it;
        try {
            it = getSPContext().search(ManagedAttribute.class, qo);
        } catch (GeneralException e) {
            throw new RuntimeException("ERROR Searching for Entitlement By DisplayName");
        }
        if (it.hasNext()) {
            ManagedAttribute row = it.next();
            exists = true;
        }
        return exists;
    }

    /**
     * Check if a list of entitlements exists given an attribute and list of attribute values
     * that corresponds to displayName
     * @param appName
     * @param attribName
     * @param attribVal
     * @return
     */
    public boolean checkEntitlementByDisplayName(String appName, String attribName, List<String> attribVal) {
        List<Boolean> outList = new ArrayList<>();
        boolean exists = false;
        QueryOptions qo = new QueryOptions();
        qo.addFilter(Filter.eq("application.name", appName));
        qo.addFilter(Filter.eq("attribute", attribName));
        for(String s : attribVal) {
	        qo.addFilter(Filter.eq("displayName", s));
	        Iterator<ManagedAttribute> it;
	        try {
	            it = getSPContext().search(ManagedAttribute.class, qo);
	        } catch (GeneralException e) {
	            throw new RuntimeException("ERROR Searching for Entitlement By DisplayName");
	        }
	        if (it.hasNext()) {
	            ManagedAttribute row = it.next();
	            outList.add(true);
	        }
        }
        if (outList.contains(false) || outList.isEmpty()) {
        	exists = false;
        } else if (!outList.isEmpty() && !outList.contains(false)) {
        	exists = true;
        }
        return exists;
    }
    /**
     * getBasicIDAttributes(String searchStr) is a generic method to return an Identity's basic
     * attributes used in Ascension
     * @param searchStr, this can be a Identity ID, Identity Name or Identity Email
     * used for searching of an identity
     * @return Map of basic following identity attributes id,name,inactive,
     * employeeId, firstname, lastname,
     * middleName, type, email,manager,samAccountName
     */
    @SuppressWarnings("null")
	public Map<String, Object> getBasicIDAttributes(String searchStr){
      	List<String> propertyNames =Arrays.asList("id","name","inactive", "employeeId", "firstname", "lastname", "middleName", "type", "email","manager","samAccountName","displayName","ministry","domain");
      	Map<String, Object> resultMap = new LinkedHashMap<>();
      	try {
      		QueryOptions qo = new QueryOptions();
      		Filter f = Filter.ignoreCase(Filter.or(Filter.eq("id", searchStr),Filter.eq("name", searchStr), Filter.eq("email",searchStr)));
      		qo.addFilter(f);
      		Iterator<Object[]> it = getSPContext().search(Identity.class,qo,propertyNames);
      		if(it != null) {
	      		while(it.hasNext()) {
	      			Object[] row = it.next();
	      		    for( int i =0; i < row.length; i++) {
	      		    	resultMap.put(propertyNames.get(i), row[i]);
	      		    }
		        }
      		}
      	}catch(Exception e) {
        	// Add Log statements here
        }
        return resultMap;
      }

    @SuppressWarnings("null")
   	public static Map<String, Object> getBasicIdentity(String searchStr){
         	List<String> propertyNames =Arrays.asList("id","name","inactive", "employeeId", "firstname", "lastname", "middleName", "type", "email","manager","samAccountName","displayName","ministry","domain");
         	Map<String, Object> resultMap = new LinkedHashMap<>();
         	try {
         		QueryOptions qo = new QueryOptions();
         		Filter f = Filter.ignoreCase(Filter.or(Filter.eq("id", searchStr),Filter.eq("name", searchStr), Filter.eq("email",searchStr)));
         		qo.addFilter(f);
         		Iterator<Object[]> it = getSPContext().search(Identity.class,qo,propertyNames);
         		if(it != null) {
   	      		while(it.hasNext()) {
   	      			Object[] row = it.next();
   	      		    for( int i =0; i < row.length; i++) {
   	      		    	if ("manager".equals(propertyNames.get(i))) {
	   	      		    	if(row[i] instanceof Identity) {
	   	      		    		Identity idn = (Identity) row[i];
		   	      		    	resultMap.put("managerName",idn.getName());
		   	      		    	resultMap.put("managerEmail",idn.getEmail());
		   	      		    	resultMap.put("managerDisplayName",idn.getDisplayName());
		   	      		    	resultMap.put("managerDepartment",idn.getAttribute("department"));
		   	      		    	resultMap.put("managerMobile",idn.getAttribute("mobile"));
		   	      		    	resultMap.put("managerNetworkIId",idn.getAttribute("samAccountName"));
		   	      		    	resultMap.put("managerFirstName",idn.getFirstname());
		   	      		    	resultMap.put("managerLastName",idn.getLastname());
		   	      		    	resultMap.put("managerId",idn.getId());
		   	      		    	resultMap.put("managerInactive",idn.isInactive());
		   	      		    }
	   	      		   }

	   	      		   if (!"manager".equals(propertyNames.get(i))) {
	     		    		resultMap.put(propertyNames.get(i), row[i]);
	     		    	}
   	      		    }
   		        }
         		}
         	}catch(Exception e) {
           	// Add Log statements here
           }
           return resultMap;
         }

    /**
     * getCustomIDAttributes(String searchStr, List<String> propertyNames) is a
     * generic method to
     * return an Identity's user specified identity attributes used in Ascension
     * @param searchStr, this can be a Identity ID, Identity Name or Identity Email
     * used for searching of an identity
     * @return Map of basic following identity attributes id,name,inactive, employeeId,
     * firstname, lastname,
     * middleName, type, email,manager,samAccountName
     */
    @SuppressWarnings("null")
	public static Map<String, Object> getCustomIDAttributes(String searchStr, List<String> propertyNames){
      	Map<String, Object> resultMap = new LinkedHashMap<>();
      	try {
      		QueryOptions qo = new QueryOptions();
      		Filter f = Filter.ignoreCase(Filter.or(Filter.eq("id", searchStr),Filter.eq("name", searchStr), Filter.eq("email",searchStr)));
      		qo.addFilter(f);
      		Iterator<Object[]> it = getSPContext().search(Identity.class,qo,propertyNames);
      		if(it != null) {
	      		while(it.hasNext()) {
	      			Object[] row = it.next();
	      			for( int i =0; i < row.length; i++) {
	      		    	if ("manager".equals(propertyNames.get(i))) {
	   	      		    	if(row[i] instanceof Identity) {
	   	      		    		Identity idn = (Identity) row[i];
		   	      		    	resultMap.put("managerName",idn.getName());
		   	      		    	resultMap.put("managerEmail",idn.getEmail());
		   	      		    	resultMap.put("managerDisplayName",idn.getDisplayName());
		   	      		    	resultMap.put("managerDepartment",idn.getAttribute("department"));
		   	      		    	resultMap.put("managerMobile",idn.getAttribute("mobile"));
		   	      		    	resultMap.put("managerNetworkIId",idn.getAttribute("samAccountName"));
		   	      		    	resultMap.put("managerFirstName",idn.getFirstname());
		   	      		    	resultMap.put("managerLastName",idn.getLastname());
		   	      		    	resultMap.put("managerId",idn.getId());
		   	      		    	resultMap.put("managerInactive",idn.isInactive());

	   	      		    	}

   	      		    	}
	      		    	if (!"manager".equals(propertyNames.get(i))) {
	      		    		resultMap.put(propertyNames.get(i), row[i]);
	      		    	}


	      		    }
		        }
      		}
      	}catch(Exception e) {
        	// Add Log statements here
        }
        return resultMap;
      }

    /**
     * Returns Attributes as a Map of Key Value pairs from given SailPoint Object
     * @param clz, this is the object class
     * @param searchStr
     * @return Map of Attribute Key and Names
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> getAttributeValue(Class clz, String searchStr) {
    	Map<String, Object> outMap = new ConcurrentHashMap<>();
    	try {
      		QueryOptions qo = new QueryOptions();
      		Filter f = Filter.ignoreCase(Filter.or(Filter.eq("id", searchStr),Filter.eq("name", searchStr)));
      		qo.addFilter(f);
      		Iterator<Object[]> it = getSPContext().search(clz,qo,"attributes");
      		if(it != null) {
	      		while(it.hasNext()) {
	      			Object[] row = it.next();
	      			outMap = (Map<String, Object>)row[0];
	      		}
      		}
      	}catch(Exception e) {
        	// TO-DO
        }
    	return outMap;
    }

    /**
     * Returns Attributes as a Map of Key Value pairs from given SailPoint Object
     * @param clz
     * @param searchStr
     * @return Map of Attribute key and values
     */
    @SuppressWarnings("unchecked")
	public Map<String, Object> getAttributeValue(Class clz, String searchStr, String attribName) {
    	Map<String, Object> outMap = new LinkedHashMap<>();
    	try {
      		QueryOptions qo = new QueryOptions();
      		Filter f = Filter.ignoreCase(Filter.or(Filter.eq("id", searchStr),Filter.eq("name", searchStr)));
      		qo.addFilter(f);
      		Iterator<Object[]> it = getSPContext().search(clz,qo,"attributes");
      		if(it != null) {
	      		while(it.hasNext()) {
	      			Object[] row = it.next();
	      			outMap = (Map<String, Object>)row[0];
		        }
      		}
      	}catch(Exception e) {
        	// TO-DO
        }
    	return outMap;
    }

    /**
     * @param appName:    name of the application
     * @param attribName: name of the attribute name of ManagedAttribute
     * @param object:     Attribute Value essentially the name of the Managed
     * Attribute to search the managed attribute
     * @return List of Map of Attribute Key/Value
     * @throws GeneralException Usage: This method should be invoked to return Map
     * of ManagedAttribute attributes.
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getManagedAttributes(String appName, String attribName, Object object)
            throws GeneralException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> attributes = new CopyOnWriteArrayList<>();
        SailPointContext context = SailPointFactory.getCurrentContext();
        QueryOptions qo = new QueryOptions();
        try {
            qo.addFilter(Filter.eq("application.name", appName));

            qo.addFilter(Filter.eq("attribute", attribName));
            if (object instanceof List) {
            	List<String> objectList = (List<String>) object;
            	qo.addFilter(Filter.in("value", objectList));
            } else {
            	qo.addFilter(Filter.eq("value", object));
            }

            Iterator<?> it = context.search(ManagedAttribute.class, qo, "attributes");

            if (null != it && it.hasNext()) {
                attributes = objectMapper.convertValue(it.next(), List.class);
            }
        } catch (Exception e) {

        }
        return attributes;
    }

    /**
     * Returns Link(s) of an Identity for a given application
     * @param identity
     * @param appName, name of the application for which links are required.
     * @return Identity Link
     * @throws GeneralException
     */
    public static Link getIdentityAppLink(Identity identity, String appName) throws GeneralException {
        SailPointContext context = SailPointFactory.getCurrentContext();
        QueryOptions qo = new QueryOptions();
        Filter filter = Filter.ignoreCase(Filter.and(Filter.eq("application.name", appName), Filter.eq("identity.name", identity.getName())));
        qo.addFilter(filter);
        Link l = (Link) context.getObjects(Link.class, qo);

        return l;
    }

    /**
     * Returns Link(s) of an Identity for a given application type
     * @param String identityName
     * @param appType, Type of the application for which links are required.
     * @return List of Identity Links
     * @throws GeneralException
     */
    public static List<Link> getLinksByAppType(String identityName, String appType) {
    	String methodName = "getLinksByAppType(String identityName, String appType)";
    	List<Link> l = null;

    	if (StringUtils.isBlank(appType)) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+": application type is required.");
    	}
    	if (StringUtils.isBlank(identityName)) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+": identity name is required.");
    	}
       	try {
	    	l = new ArrayList<>();
	        SailPointContext context = SailPointFactory.getCurrentContext();
	        QueryOptions qo = new QueryOptions();
	        Filter appTypeFilter = Filter.eq("application.type", appType);
	        Filter idnNameFilter = Filter.eq("identity.name", identityName);
	        Filter filter = Filter.ignoreCase(Filter.and(appTypeFilter, idnNameFilter));
	        qo.addFilter(filter);
	        l = context.getObjects(Link.class, qo);
    	} catch (Exception e) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+" ERROR: "+e,e);
    	}

        return l;
    }

    /**
     * Returns Link(s) of an Identity for a given application name
     * @param String identityName
     * @param appName, name of the application for which links are required.
     * @return Identity Link
     * @throws GeneralException
     */
    public static List<Link> getLinksByAppName(String identityName, String appName) throws GeneralException {
    	String methodName = "getLinksByAppType(String identityName, String appType)";
    	List<Link> l = null;

    	if (StringUtils.isBlank(appName)) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+": application name is required.");
    	}
    	if (StringUtils.isBlank(identityName)) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+": identity name is required.");
    	}
       	try {
	    	l = new ArrayList<>();
	        SailPointContext context = SailPointFactory.getCurrentContext();
	        QueryOptions qo = new QueryOptions();
	        Filter appNameFilter = Filter.eq("application.name", appName);
	        Filter idnNameFilter = Filter.eq("identity.name", identityName);
	        Filter filter = Filter.ignoreCase(Filter.and(appNameFilter, idnNameFilter));
	        qo.addFilter(filter);
	        l = context.getObjects(Link.class, qo);
    	} catch (Exception e) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+" ERROR: "+e,e);
    	}

        return l;
    }

    //This is incomplete method
    public void parseIdentityRequest(String irId) throws GeneralException {
    	SailPointContext context = SailPointFactory.getCurrentContext();
    	IdentityRequest irObj = context.getObjectByName(IdentityRequest.class, irId);
    }

    /**
     * Returns a boolean value of Identity's inactive value if false
     * @param searchStr, this can be ID, IdentityName or email
     * @return boolean
     */
    @SuppressWarnings("null")
    public static boolean isIdentityActive(String searchStr){
    	String methodName = "isIdentityActive(String searchStr)";
    	boolean active = false;
    	try {
    		QueryOptions qo = new QueryOptions();
    		Filter f = Filter.ignoreCase(Filter.and(Filter.or(Filter.eq("id", searchStr),Filter.eq("name", searchStr), Filter.eq("email",searchStr)),Filter.eq("inactive",Boolean.FALSE)));
    		qo.addFilter(f);
    		Iterator<Object[]> it = getSPContext().search(Identity.class,qo,"inactive");
    		List<Object> resultList = IteratorUtils.toList(it);

    		if(it != null && resultList.size() == 1) {

    			active = true;
    		} else {
    			active = false;
    		}
    	}catch(Exception e) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+" ERROR: "+e,e);
       }
       return active;
    }

    /**
     * Return link attribute for a given application account
     * @param appName String
     * @param nativeId String
     * @param attrName String
     * @return Object attribute Value as object representation
     */
    public static Object getLinkAttribute(String appName, String nativeId, String attrName) {
    	String methodName = "getLinkAttribute(String appName, String nativeId, String attrName)";
    	Object outVal = null;
    	QueryOptions qo = new QueryOptions();
    	qo.addFilter(Filter.eq("nativeIdentity",nativeId));
    	qo.addFilter(Filter.eq("application.name",appName));

    	try {
    		Iterator it = getSPContext().search(Link.class,qo,"attributes");
    		while(it.hasNext()) {
    			Object[] row = (Object[]) it.next();
    			Map<String,Object> attrs = (Map<String,Object>) row[0];
    			if(attrs != null && attrs.containsKey(attrName)) {
    				outVal = attrs.get(attrName);
    			}
    		}
    	} catch(Exception e) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+" ERROR: "+e,e);
    	}
    	return outVal;
    }

    /**
     * Return link attribute for a given application account
     * @param appName String
     * @param nativeId String
     * @param attrName String
     * @return Object attribute Value as object representation
     */
    @SuppressWarnings("rawtypes")
	public static Link getLinkFromAccount(String appName, String nativeId) {
    	String methodName = "getLinkFromAccount(String appName, String nativeId)";
    	Link outLink = null;
    	QueryOptions qo = new QueryOptions();
    	qo.addFilter(Filter.eq("nativeIdentity",nativeId));
    	qo.addFilter(Filter.eq("application.name",appName));

    	try {
    		Iterator it = getSPContext().search(Link.class,qo);
    		while(it.hasNext()) {
    			Link[] l = (Link[])it.next();
    			outLink = l[0];

    		}
    	} catch(Exception e) {
    		throw new RuntimeException(LOG_NAME+": "+methodName+" ERROR: "+e,e);
    	}
    	return outLink;
    }

    /**
     * getBundleAttributes is a convinence method to grab all bundle attributes
     * given a bundle value/name
     * @param attrVal
     * @return List, returns list of Map of attributes of a Bundle
     */
    public static List<Map<String, Object>>  getBundleAttributes(Object attrVal) {
    	String methodName = "getBundleAttributes(Object attrVal)";
    	ObjectMapper objectMapper = new ObjectMapper();
		QueryOptions qo = new QueryOptions();
		List<Map<String, Object>> attributes = new CopyOnWriteArrayList();
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
	        qo.addFilter(Filter.eq("name", attrVal.toString()));
	        Iterator it = context.search(Bundle.class, qo, "attributes");
			if (null != it && it.hasNext()) {
                attributes = objectMapper.convertValue(it.next(), List.class);
        }

		} catch (GeneralException e) {
			throw new RuntimeException(" ERROR: "+e,e);
		}
		return attributes;
	}

    /**
     * Checks to see if Specific Application exists in a plan
     * @param plan
     * @param appName
     * @return
     */
    public static boolean isAppExistsInPlan(ProvisioningPlan plan, String appName) {
    	boolean exists = false;
    	List<AccountRequest> accountRequests = plan.getAccountRequests();
    	 for (AccountRequest account: accountRequests) {
             if(appName.equalsIgnoreCase(account.getApplicationName())) {
            	 exists =  true;
             }
    	 }
    	 return exists;
    }

    /**
     * getITBundleMap, return a Map of IT Bundle attributes and profile
     * @param itBundleName
     * @return Map
     */
    public static Map<String,Object> getITBundleMap(Bundle itBundleName){
    	String methodName = "getITBundleMap(Bundle itBundleName)";
        Map<String,Object> roleDataMap = new HashMap<>();
        List<String> profileStrings = new ArrayList<>();
        try{

          List<Profile> profiles = itBundleName.getProfiles();
          for (Profile p: profiles){
            String applicationName = p.getApplication().getName();
            List<Filter> filters = p.getConstraints();
            for (Filter f: filters){
              String attrName = null;
              Object entValue = null;
              if(f instanceof Filter.LeafFilter){
                if(((LeafFilter) f).getValue() instanceof List){
                  List<String> entitlementsData = (List) ((LeafFilter) f).getValue();
                  attrName = ((LeafFilter) f).getProperty();
                  for(String entValueD:entitlementsData){
                    roleDataMap.put(entValueD,getManagedAttributes(applicationName,attrName,entValueD));
                  }
                } else {
                  attrName = ((LeafFilter) f).getProperty();
                  entValue = ((LeafFilter) f).getValue();
                  if (!StringUtils.isBlank(entValue.toString())) {
                	  roleDataMap.put(entValue.toString(),getManagedAttributes(applicationName,attrName,entValue));
                  }

                }
              }

            }
          }
        }
        catch (Exception e){
          Common.error(log.getClass().getName(), "Error in: "+methodName+" "+e,e);
        }
        return roleDataMap;
      }

    /**
     * getEntitlementsFromBusinessBundle returns attributes of an entitlement on an IT Bundle that is inhereted by a business bundle
     * @param businessRoleName
     * @return
     */
    public static Map getEntitlementsFromBusinessBundle(String businessRoleName) {
    	String methodName = "getEntitlementsFromBusinessBundle";
        Map<String,Object> roleMap = new HashMap<>();
        try {
	        Bundle businessRoleObj = getSPContext().getObjectByName(Bundle.class, businessRoleName);
	        List<Bundle> requirements = businessRoleObj.getRequirements();
	        if(requirements.size()>0){
	          for(Bundle b:requirements){
	            Map<String,Object> itBundleMap = getITBundleMap(b);
	            roleMap.putAll(itBundleMap);
	          }
	        }
        } catch (Exception e) {
        	Common.error(log.getClass().getName(), "Error in : "+methodName+" "+e,e);
        }
        return roleMap;
      }

    /**
     * Returns List Assigned Bundles on an Identity.
     * @param searchStr, can be Identity ID, name
     * @return list of bundles / roles
     */
    public static List<Bundle>  getAssignedBundles(String searchStr) {
    	String methodName = "getAssignedBundles(String searchStr)";
    	List<Bundle>  assignedBundles = new ArrayList<>();
    	QueryOptions qo = new QueryOptions();
  		Filter f = Filter.ignoreCase(Filter.or(Filter.eq("id", searchStr),Filter.eq("name", searchStr)));
  		qo.addFilter(f);
  		Identity identity = null;
  		try {
  			identity = getSPContext().getUniqueObject(Identity.class,f);
  			if (identity != null) {
  				 assignedBundles.addAll(identity.getAssignedRoles());
  			 }
  		} catch (Exception e) {
  			Common.error(log.getClass().getName(),"Error in :"+methodName+" "+e,e);
  			throw new RuntimeException(log.getClass().getName()+": Error in :"+methodName+" "+e,e);
  		}

  		return assignedBundles;
    }

    /**
     * Checks if a Bundle exists on the Identity, return true if exists.
     * @param searchStr: Identity name, or Id
     * @param bundleName
     * @return boolean exists
     */
    public static boolean checkBundleExists(String searchStr, String bundleName) {
    	String methodName = "checkBundleExists(String searchStr, String bundleName)";
    	boolean exists = false;
    	try {
	    	List<Bundle> assignedBundles = getAssignedBundles(searchStr);
	    	if (assignedBundles.isEmpty() || assignedBundles == null) {
	    		throw new RuntimeException("Assigned Bundles not found for Identity: "+searchStr);
	    	}
	    	for (Bundle b : assignedBundles) {
	    		if (bundleName.equalsIgnoreCase(b.getName())) {
	    			exists = true;
	    			break;
	    		}
	    	}
    	} catch (Exception e) {
    		Common.error(log.getClass().getName()," Error in :"+methodName+" "+e,e);
    	}
    	return exists;
    }
}