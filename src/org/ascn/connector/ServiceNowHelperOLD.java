package org.ascn.connector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.ascn.utils.GeneralUtils;
import org.ascn.utils.QueryUtils;
import org.springframework.lang.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.ascn.utils.Common;

import sailpoint.api.Aggregator;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.Connector;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.Bundle;
import sailpoint.object.Custom;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.IdentityRequest;
import sailpoint.object.IdentityRequestItem;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.Profile;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.object.QueryOptions;
import sailpoint.object.ResourceObject;
import sailpoint.object.TaskResult;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;
public class ServiceNowHelperOLD {
    
private SailPointContext context;
private String appName = "IIQ";
private String ministry = "";
private String LOG_NAME = "org.ascn.connector.ServiceNowHelper";
private String bundleSnowAssignmentGroup = "";
private Map<String,Object> entitlement;

public ServiceNowHelperOLD() throws GeneralException {
    this.context = SailPointFactory.getCurrentContext();
}
    
/**
 * This method writes the RITM object
 * @param appName, application that is part of the AccountRequest
 * @param writeParams, Map object that is passed from the caller
 * providing the data to be written into the RITM
 */
public void writeToFile(String appName, Map writeParams){
	String methodName = "writeToFile(String appName, Map writeParams)";
	Common.info(LOG_NAME, "Entering "+methodName);
	try {
		if (StringUtils.isBlank(appName)) {
			Common.info(LOG_NAME, methodName+" Application Name is required");
			throw new RuntimeException(LOG_NAME+": "+methodName+" Application Name is required");
		}
		if(null == writeParams || writeParams.isEmpty()) {
			Common.info(LOG_NAME, methodName+" Parameters to Write are required");
			throw new RuntimeException(LOG_NAME+": "+methodName+" Parameters to Write are required");
		}
		Application application = context.getObjectByName(Application.class, appName);
		if (null == application) {
			Common.info(LOG_NAME, methodName+" Application Object is required");
			throw new RuntimeException(LOG_NAME+": "+methodName+" Application Object is required");
		}
		String fileName = application.getStringAttributeValue("file");
		if (StringUtils.isBlank(fileName)) {
			Common.info(LOG_NAME, methodName+" File Name is required");
			throw new RuntimeException(LOG_NAME+": "+methodName+" File Name is required");
		}
		// open file in read-write mode
		RandomAccessFile randomAccessFile;
		randomAccessFile = new RandomAccessFile(new File(fileName), "rw");
		FileChannel channel = randomAccessFile.getChannel();
		// get lock
		FileLock tryLock = channel.lock();
		Object userName = writeParams.get("Username");
		String firstName = writeParams.get("Firstname") == null ? "":writeParams.get("Firstname").toString();
		String lastName = writeParams.get("Lastname") == null ? "": writeParams.get("Lastname").toString();
		String employeeNumber = writeParams.get("EmployeeNumber") == null ? "":writeParams.get("EmployeeNumber").toString();
		String emailAddress = writeParams.get("EmailAddress") == null ? "":writeParams.get("EmailAddress").toString();
		String department = writeParams.get("Department") == null ? "":writeParams.get("Department").toString();
		String role = writeParams.get("Role") == null ? "" : writeParams.get("Role").toString();
		String dataStr = "\n" + userName + "," + firstName + "," + lastName + "," + employeeNumber + ","+emailAddress + "," + department + "," + role;
		// write to end of file
		randomAccessFile.seek(randomAccessFile.length());
		randomAccessFile.write(dataStr.getBytes());
		// release lock
		tryLock.release();
		// close file
		randomAccessFile.close();
		
	} catch (Exception e) {
		Common.error(LOG_NAME,"Error in "+methodName+" " + e, e);
	}
	Common.info(LOG_NAME, "Exiting "+methodName);
}
/**
 * This method builds Request object comments given a IdentityRequestId
 * @param idReqId
 * @return requester comments
 */
public String getRequesterCommnets(String idReqId) {
	String methodName = "getRequesterCommnets(String identityRequestId)";
	Common.info(LOG_NAME, "Entering "+methodName+" IdentityRequest ID: "+idReqId);
	String comment = "";
	Map<String,String> map = new HashMap<>();
	try {
		IdentityRequest idReqObj = context.getObjectByName(IdentityRequest.class, idReqId);
		if (idReqObj == null) {
			Common.info(LOG_NAME,"IdentityRequest Object is Null:"+idReqId);
			throw new RuntimeException(LOG_NAME+" IdentityRequest Object is Null: "+idReqId);
		}
		if (idReqObj.getItems() == null) {
			Common.info(LOG_NAME,"No Items in IdentityRequest Object: "+idReqId);
			throw new RuntimeException(LOG_NAME+" No Items in IdentityRequest Object: "+idReqId);
		}
		List<IdentityRequestItem> itemsList = idReqObj.getItems();
		for (IdentityRequestItem item : itemsList) {
			if (Util.isNotNullOrEmpty(item.getRequesterComments()) && Util.nullSafeCaseInsensitiveEq("IdentityIQ for ServiceNow Service Desk", item.getProvisioningEngine())) {
				map.put(item.getValue().toString(), item.getRequesterComments());
			}
		}
		if (!Util.isEmpty(map)) {
			Collection values = map.values();
			if (!Util.isEmpty(values)) {
				List valueList = new ArrayList(values);
				Util.removeDuplicates(valueList);
				if(null != valueList){
					comment = valueList.toString().substring(1, valueList.toString().length() - 1);
				}
			}
		}
	} catch (Exception ex) {
		Common.error(LOG_NAME, "Exception in getRequesterCommnets " + ex.getMessage());
	}
	Common.trace(LOG_NAME,"Exiting "+methodName);
	return comment;
}


/**
 * Returns the Managed Attribute Map
 * @param managedAttr, Managed Attribute 
 * @return Map of Managed Attribute Map
 */
public Map<String, Object> addPropertyInPlan(ManagedAttribute managedAttr) {

	Common.error(LOG_NAME, "ISD-Entering addPropertyInPlan");
	Set setValue = new HashSet();
	setValue.add("approvalType");
	setValue.add("ascBusinessApplication");
	setValue.add("snowAssignmentGroup");
	setValue.add("snowAssignmentGroupName");
	setValue.add("sysDescriptions");
	Map attributeMap = new HashMap();
	if (null != managedAttr && null != managedAttr.getAttributes() && null != managedAttr.getAttributes().getMap()) {
		attributeMap = managedAttr.getAttributes().getMap();
		if (null != attributeMap && null != attributeMap.keySet()) {
		attributeMap.keySet().removeAll(setValue);
		}
	}
	Common.error(LOG_NAME, "ISD-Exiting addPropertyInPlan");
	return attributeMap;
}
/**
 * Returns the Managed Attribute Map
 * @param managedAttr, Managed Attribute 
 * @return Map of Managed Attribute Map
 */
public Map<String, Object> addPropertyInPlan(Map<String,Object> managedAttr) {
	Common.error(LOG_NAME, "ISD-Entering addPropertyInPlan");
	Set setValue = new HashSet();
	setValue.add("approvalType");
	setValue.add("ascBusinessApplication");
	setValue.add("snowAssignmentGroup");
	setValue.add("snowAssignmentGroupName");
	setValue.add("sysDescriptions");
	Map<String,Object> attributeMap = new HashMap<>();
	if (null != managedAttr) {
		attributeMap = managedAttr;
		if (null != attributeMap && null != attributeMap.keySet()) {
		attributeMap.keySet().removeAll(setValue);
		}
	}
	Common.error(LOG_NAME, "ISD-Exiting addPropertyInPlan");
	return attributeMap;
}
public String findAssignmentFromBundle(AccountRequest acctReq) {
	
	String methodName = "findAssignmentFromBundle";
	Common.info(LOG_NAME, "Entering "+methodName);
	String returnVal = null;
	String appName = acctReq.getApplication();
	List appList = Arrays.asList("IIQ","IdentityIQ");
	if(appList.contains(appName)) {
		List<AttributeRequest> attrReqs =  acctReq.getAttributeRequests();
		for(AttributeRequest ar : attrReqs) {
			String attribName = ar.getName();
			if("assignedRoles".equals(attribName)) {
				Object attribVal = ar.getValue();
				Map<String,Object> attrMap = QueryUtils.getBundleAttributes(attribVal);
				returnVal = GeneralUtils.OTOS(attrMap.get("snowAssignmentGroup"));
			}
		}
	}
	Common.info(LOG_NAME, "Exiting "+methodName);
	return returnVal;
}

/**DELETE THIS METHOD AFTER TESTING THE ABOVE ONE
 * This method gets the Assignment Group from a given bundle
 * @param eachEntitlement
 * @param idnReqId
 * @return name of the assignment group as representation of String
 */
public String findAssignmentFromBundle(ManagedAttribute eachEntitlement, String idnReqId) {
	String methodName = "findAssignmentFromBundle";
	Common.info(LOG_NAME, "Entering "+methodName);
	String result = "";
	String snowAssignmentGroup = "snowAssignmentGroup";
	List<Application> inputAppList = new ArrayList<>();
	List<Profile> profileList = new ArrayList<>();
	boolean breakFlag = false;
	try {
		if (StringUtils.isBlank(idnReqId)) {
			throw new RuntimeException(LOG_NAME+" IdentityRequest ID is null");
		}
		if (eachEntitlement == null) {
			throw new RuntimeException(LOG_NAME+" Entitlement is null");
		}
		if (Util.isNotNullOrEmpty(eachEntitlement.getAttribute())) {
			throw new RuntimeException(LOG_NAME+" Entitlement Attribute is null");
		}
		if (Util.isNotNullOrEmpty(eachEntitlement.getValue())) {
			throw new RuntimeException(LOG_NAME+" Entitlement Attribute value is null");
		}
		if (eachEntitlement.getApplication() != null) {
			throw new RuntimeException(LOG_NAME+" Entitlement application is null");
		}
		
		inputAppList.add(eachEntitlement.getApplication());
		// get identity requestid obj
		IdentityRequest idnReqObj = context.getObjectByName(IdentityRequest.class, idnReqId);
		if (idnReqObj == null) {
			throw new RuntimeException(LOG_NAME+" IdentityRequest Object is null");
		}
		if (idnReqObj.getItems() != null && !idnReqObj.getItems().isEmpty()) {
			throw new RuntimeException(LOG_NAME+" IdentityRequest Object Items are null");
		}
		List<IdentityRequestItem> itemsList = idnReqObj.getItems();
		for (IdentityRequestItem item : itemsList) {
			if (breakFlag) {break;}
			//Get Role Name from the Request Item
			String roleName = null;
			if ("IIQ".equalsIgnoreCase(item.getApplication())) {
				if (null !=item.getValue()  && item.getValue() instanceof String) {
					roleName = item.getValue().toString();
				} else if (null !=item.getValue()  && item.getValue() instanceof List) {
					if (!((List) item.getValue()).isEmpty()) {
						roleName = ((List) item.getValue()).get(0).toString();
					}
				}
			}
		// get role obj
		if (!StringUtils.isBlank(roleName)) {
			Bundle bundle = context.getObjectByName(Bundle.class, roleName);
			if (bundle != null && bundle.getAttribute(snowAssignmentGroup) != null) {
				profileList =  bundle.getProfilesForApplications(inputAppList);
				if (profileList != null && !profileList.isEmpty()) {
					for (Profile profile : profileList) {
						if (breakFlag) {break;}
							for (Filter filter : profile.getConstraints()) {
								if (Util.isNotNullOrEmpty(filter.getExpression())) {
									if (filter.getExpression().contains(eachEntitlement.getAttribute()) && filter.getExpression().contains(eachEntitlement.getValue())) {
									// get the assignment details and break
										result = (String) bundle.getAttribute(snowAssignmentGroup);
										breakFlag = true;
									}
								}
							}
					}
				}
			}
		}
	}
} catch (Exception e) {
		Common.error(LOG_NAME, "Exception in "+methodName+" "+e,e);
	}
	Common.info(LOG_NAME, "Exiting "+methodName);
	return result;
}

/**
 * Adds an SNOW group in Plan
 * @param plan
 * @param reqName
 * @return ProvisioningPlan including assignment groups
 */
public ProvisioningPlan addAssignmentGroupInPlan(ProvisioningPlan plan, String reqName) {
	String methodName = "addAssignmentGroupInPlan(..)";
	Common.info(LOG_NAME, "Entering "+methodName);
	String assignmentGroup = null;
	String businessApplication = null;
	boolean isAccReqAdded = false;
	List<AccountRequest> splitAccountRequests = new ArrayList<>();
	try {
		if (plan != null) {
			List<AccountRequest> accountRequests = plan.getAccountRequests();
			//  List splitAccountRequests = new ArrayList<>();
			if (Util.isEmpty(accountRequests)) {
				
			}
			for (AccountRequest request : accountRequests) {//259-432
				appName = request.getApplicationName();
				List<AttributeRequest> attributeRequests = request.getAttributeRequests();
				isAccReqAdded = false;
				if (!Util.isEmpty(attributeRequests)) {
				}
				for (AttributeRequest attrRequest : attributeRequests) {//265 - 431
				assignmentGroup = null;
				businessApplication = null;
				Map<String, Object> propertyMap = new HashMap<>();
				Map<String, Object> accReqAttrMap = new HashMap<>();
				Attributes<String, Object> propertyAttr = new Attributes<>();
				String attrName = attrRequest.getName();
				Object attrValue = attrRequest.getValue();
				Filter appFilter = Filter.eq("application.name", appName);
				Filter attributeFilter = Filter.eq("attribute", attrName);
				if (attrValue instanceof String && Util.isNotNullOrEmpty(attrName)
						&& null != attrValue && Util.isNotNullOrEmpty(attrValue.toString())) {//276-430
						Filter valueFilter = Filter.eq("value", attrValue);
						Filter finalFilter = Filter.and(appFilter, attributeFilter, valueFilter);
						ManagedAttribute entitlement = context.getUniqueObject(ManagedAttribute.class,
						finalFilter);
						if (entitlement != null) {
							bundleSnowAssignmentGroup = findAssignmentFromBundle(entitlement,plan.getString("identityRequestId"));
							if (Util.isNotNullOrEmpty(bundleSnowAssignmentGroup)) {
								assignmentGroup = bundleSnowAssignmentGroup;
							} else {
								assignmentGroup = entitlement.getAttribute("snowAssignmentGroup") != 					null ? entitlement.getAttribute("snowAssignmentGroup").toString() : "" ;
							}
							if (Util.isNotNullOrEmpty(assignmentGroup)) {
								if (reqName == null) {
									assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5";
								}
								businessApplication = entitlement.getAttribute("ascBusinessApplication") != null ?entitlement.getAttribute("ascBusinessApplication").toString() : "";
								propertyMap = addPropertyInPlan(entitlement);
								AccountRequest eachAccountReq = new AccountRequest(request);
								if (Util.isNotNullOrEmpty(assignmentGroup)) {
									if (reqName == null) {
										assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5";
									}
									eachAccountReq.add(new AttributeRequest("assignment_group",
											ProvisioningPlan.Operation.Set, assignmentGroup));
								}
								if (Util.isNotNullOrEmpty(businessApplication)) {
									eachAccountReq.add(new AttributeRequest("businessApplication",
													ProvisioningPlan.Operation.Set, businessApplication));
										}
										if (propertyMap != null && !propertyMap.isEmpty()) {
											propertyAttr = eachAccountReq.getArguments();
											if (null != propertyAttr && null != propertyAttr.getMap()) {
											accReqAttrMap = propertyAttr.getMap();
											}
											accReqAttrMap.put("ObjectProperties", propertyMap);
											if (propertyAttr == null) {
											    propertyAttr = new Attributes();
											}
											propertyAttr.setMap(accReqAttrMap);
											eachAccountReq.setArguments(propertyAttr);
										}
										isAccReqAdded = true;
										splitAccountRequests.add(eachAccountReq);
								}
			/*if (entitlement != null) {
			// context.decache(entitlement);
			
			} */
			if(!StringUtils.isBlank(attrName) && attrValue != null) {
				
				if (attrValue instanceof List) { //327 - 387
				List attrValueList = Arrays.asList(attrValue);
				Filter valueFilter1 = Filter.in("value", attrValueList);
				Filter finalFilter1 = Filter.and(appFilter, attributeFilter, valueFilter1);
				QueryOptions qo = new QueryOptions();
				qo.add(finalFilter1);
				List<ManagedAttribute> entitlementList = context.getObjects(ManagedAttribute.class,
				qo);
				if (entitlementList != null && !entitlementList.isEmpty()) {
				for (ManagedAttribute eachEntitlement : entitlementList) {
				AttributeRequest eachAttributeReq = new AttributeRequest(attrName,
				attrRequest.getOp(), eachEntitlement.getValue());
				AccountRequest eachAccountReq = new AccountRequest(request);
				eachAccountReq.remove(attrRequest);
				eachAccountReq.add(eachAttributeReq);
				bundleSnowAssignmentGroup = findAssignmentFromBundle(eachEntitlement,
				plan.getString("identityRequestId"));
				if (Util.isNotNullOrEmpty(bundleSnowAssignmentGroup)) {
				assignmentGroup = bundleSnowAssignmentGroup;
				} else {
				assignmentGroup = eachEntitlement.getAttribute("snowAssignmentGroup") != null ? eachEntitlement.getAttribute("snowAssignmentGroup")
				.toString() : "";
				}
				if (Util.isNotNullOrEmpty(assignmentGroup)) {
				if (reqName == null) {
				assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5"; // Identity
				// governance
				}
				Common.error(LOG_NAME, "assignment group is::" + assignmentGroup);
				businessApplication = eachEntitlement.getAttribute("ascBusinessApplication") != null ? eachEntitlement.getAttribute("ascBusinessApplication").toString() : "";
				propertyMap = addPropertyInPlan(eachEntitlement);
				if (Util.isNotNullOrEmpty(assignmentGroup)) {
				if (reqName == null) {
				assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5"; // Identity
				// Governance
				}
				eachAccountReq.add(new AttributeRequest("assignment_group",
				ProvisioningPlan.Operation.Set, assignmentGroup));
				}
				if (Util.isNotNullOrEmpty(businessApplication)) {
				eachAccountReq.add(new AttributeRequest("businessApplication",
				ProvisioningPlan.Operation.Set, businessApplication));
				}
				if (propertyMap != null && !propertyMap.isEmpty()) {
				propertyAttr = eachAccountReq.getArguments();
				if (null != propertyAttr && null != propertyAttr.getMap()) {
				accReqAttrMap = propertyAttr.getMap();
				}
				accReqAttrMap.put("ObjectProperties", propertyMap);
				if (propertyAttr == null) {
				propertyAttr = new Attributes();
				}
				propertyAttr.setMap(accReqAttrMap);
				eachAccountReq.setArguments(propertyAttr);
				}
				isAccReqAdded = true;
				splitAccountRequests.add(eachAccountReq);
				}
				}
			}
			}
			}

				/*
				* enable or disable case or account req doesnot have attreq or account req
				* doesnot have entitlement
				*/
				if (Util.isEmpty(attributeRequests) || !isAccReqAdded) { // 393-428
				AccountRequest eachAcctReq = new AccountRequest(request);
				String appName = request.getApplication();
				Application appObj = context.getObjectByName(Application.class, appName);
				if (appObj != null) {
				assignmentGroup = (String) ((appObj.getAttributeValue("snowAssignmentGroup") != null) ? appObj.getAttributeValue("snowAssignmentGroup")
				: "");
				businessApplication = (String) ((appObj
				.getAttributeValue("ascBusinessApplication") != null) ? appObj.getAttributeValue("ascBusinessApplication") : appName);
				if (Util.isNotNullOrEmpty(assignmentGroup)) {
				if (reqName == null) {
				assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5"; // Identity governance
				}
				Common.error(LOG_NAME, "assignment group is::" + assignmentGroup);
				if (Util.isNotNullOrEmpty(assignmentGroup)) {
				if (reqName == null) {
				assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5"; // Identity
				// governance
				}
				eachAcctReq.add(new AttributeRequest("assignment_group",ProvisioningPlan.Operation.Set, assignmentGroup));
				}
				if (Util.isNotNullOrEmpty(businessApplication)) {
				eachAcctReq.add(new AttributeRequest("businessApplication",ProvisioningPlan.Operation.Set, businessApplication));
				}
				// context.decache(appObj);
				}
				isAccReqAdded = true;
				splitAccountRequests.add(eachAcctReq);
				}
				}
				} 
				} 
				}
			}
		}
} catch (Exception e) {
					//TO-DO
	}
	plan.setAccountRequests(splitAccountRequests);
	Common.debug(LOG_NAME,"Exiting "+methodName);
	return plan;
}

/**
 * Adds an SNOW group in Plan
 * @param plan
 * @param reqName
 * @return ProvisioningPlan including assignment groups
 */
public ProvisioningPlan addAssignmentGroupInPlan(ProvisioningPlan plan) {
	String methodName = "addAssignmentGroupInPlan(..)";
	Common.info(LOG_NAME, "Entering ");
	List<AccountRequest> modifiedAccountRequest = new ArrayList<>();
try {
	if (plan != null) {
		List<AccountRequest> accountRequests = plan.getAccountRequests();
	if (!Util.isEmpty(accountRequests) || accountRequests != null) {
		for (AccountRequest request : accountRequests) {
			String businessApplication = null;
			String assignmentGroup = null;
			String appName = request.getApplicationName();
			List<String> appList = Arrays.asList("IIQ","IdentityIQ");
			List<AttributeRequest> attributeRequests = request.getAttributeRequests();
			if(attributeRequests == null || attributeRequests.isEmpty()) {
				Map<String,Object>  arMap = QueryUtils.getAttributeValue(Application.class,appName);
				assignmentGroup = GeneralUtils.OTOS(arMap.get("snowAssignmentGroup"));
				businessApplication = GeneralUtils.OTOS(arMap.get("ascBusinessApplication"));
			} else {
				for(AttributeRequest attrReq : attributeRequests) {
					String attribName = attrReq.getName();
					Object attribVal = attrReq.getValue();
					if (!StringUtils.isBlank(GeneralUtils.OTOS(attribVal))) {
							if (appList.contains(appName) && "assignedRoles".equals(attribName)) {
									Map<String,Object> attrMap = QueryUtils.getBundleAttributes(attribVal);
									assignmentGroup = GeneralUtils.OTOS(attrMap.get("snowAssignmentGroup"));
							} else if (appName != null && !appList.contains(appName)){
								entitlement = new HashMap<>();
								String accountOperation = GeneralUtils.OTOS(request.get("operation"));
								List<String> entitlementOp = Arrays.asList("EntitlementAdd","EntitlementRemove");
								if(entitlementOp.contains(accountOperation)) {
									entitlement = QueryUtils.getEntitlementAttributes(appName,attribName,attribVal);
									assignmentGroup = GeneralUtils.OTOS(entitlement.get("snowAssignmentGroup"));
									businessApplication = GeneralUtils.OTOS(entitlement.get("ascBusinessApplication"));
								} else {
									Map<String,Object>  arMap = QueryUtils.getAttributeValue(Application.class,appName);
									assignmentGroup = GeneralUtils.OTOS(arMap.get("snowAssignmentGroup"));
									businessApplication = GeneralUtils.OTOS(arMap.get("ascBusinessApplication"));
								}
								
							}
						
						}
						if(StringUtils.isBlank(assignmentGroup)) {
							assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5";
						}
						request.add(new AttributeRequest("assignment_group",ProvisioningPlan.Operation.Set, assignmentGroup));
						if (Util.isNotNullOrEmpty(businessApplication)) {
							request.add(new AttributeRequest("businessApplication",ProvisioningPlan.Operation.Set, businessApplication));
						}
						modifiedAccountRequest.add(request);
				}
			}
	}
}
}
	plan.setAccountRequests(modifiedAccountRequest);
	
} catch (Exception e) {
	Common.error(LOG_NAME,"ERROR: Failed to create Plan for ServiceNow Request: "+e,e);
	throw new RuntimeException(LOG_NAME+" Failed to create Plan for ServiceNow Request: "+e,e);
}
    Common.debug(LOG_NAME,"Exiting ");
	return plan;
}

/**
 * This method formats a full name for a given identity
 * @param identity
 * @return Full Name of a Identity cude 
 */
public String buildFullName(Identity identity) {
Common.error(LOG_NAME, "ISD-Entering buildFullName");
String firstName = identity.getFirstname();
String lastName = identity.getLastname();
String middleName = identity.getStringAttribute("middleName");
String fullName = firstName;
if (middleName != null) {
fullName = fullName + " " + middleName;
}
fullName = fullName + " " + lastName;
Common.error(LOG_NAME, "ISD-Exiting buildFullName");
return fullName;
}

/**
 * Parses Attribute request from Plan
 * @param accountReq
 * @param attrName
 * @return String representation of AttributeRequest
 */
public String getAttributeRequest(AccountRequest accountReq, String attrName) {
Common.error(LOG_NAME, "ISD-Entering getAttributeRequest");
String result = null;
AttributeRequest attrReq = accountReq.getAttributeRequest(attrName);
if (attrReq != null) {
if (attrReq.getValue() instanceof String) {
return (String) attrReq.getValue();
}
}
if (result == null) {
result = "N/A";
}
Common.error(LOG_NAME, "ISD-Exiting getAttributeRequest");
return result;
}

/**
 * Returns a list representation of Attribute Requests for Roles Request
 * @param accountReq
 * @param attrName
 * @return List of Attribute Requests
 */
public List getRoleAttributeRequests(AccountRequest accountReq, String attrName) {
Common.error(LOG_NAME, "ISD-Entering getRoleAttributeRequests");
List<Object> results = new ArrayList<>();
List<AttributeRequest> attrReqList = accountReq.getAttributeRequests(attrName);
if (attrReqList != null && attrReqList.size() > 0) {
for (AttributeRequest attrReq : attrReqList) {
results.add(attrReq.getValue());
}
}
if (results.size() > 0) {
results.add("N/A");
}
Common.error(LOG_NAME, "ISD-Exiting getRoleAttributeRequests");
return results;
}

/**
 * Returns list of Attribut Requests extracted from Plan, Used to but a 
 * final plan.
 * @param accountReq
 * @param attrName
 * @return
 */
public List<String> getAttributeRequestList(AccountRequest accountReq, String attrName) {
Common.error(LOG_NAME, "ISD-Entering getAttributeRequestList");
List result = new ArrayList();
AttributeRequest attrReq = accountReq.getAttributeRequest(attrName);
if (attrReq != null) {
if (attrReq.getValue() instanceof List) {
	result.addAll((List) attrReq.getValue());
} else if (attrReq.getValue() instanceof String) {
	result.add((String) attrReq.getValue());
}
}
Common.error(LOG_NAME, "ISD-Exiting getAttributeRequestList");
    return result;
    }
    
/**
 * Returns Manager of the Identity 
 * @param identity
 * @param attribute
 * @return String representation of Manager on the Identity cube
 */
public String getIdentityManager(Identity identity, String attribute){
Identity maId = identity.getManager();
if(null != maId && attribute != null) {
if ("displayName".equalsIgnoreCase(attribute)) {
return maId.getDisplayableName() != null ? maId.getDisplayableName() : "Not Available";
} else if ("email".equalsIgnoreCase(attribute)) {
return maId.getEmail() != null ? maId.getEmail() : "Not Available";
} else if ("name".equalsIgnoreCase(attribute)) {
return maId.getName() != null ? maId.getName() : "Not Available";
} else {
Object otherAttr = maId.getAttribute("telephoneNumber");
return otherAttr != null ? otherAttr.toString() : "Not Available";
}
}
return "Manager Attributes Not Available";
}

/**
 * This is the main method that formats and provides the data for creating RITM
 * @param plan
 * @param comment
 * @param identity
 * @param arguments
 * @return Formatted final Plan
 */
public ProvisioningPlan createRITMDisplay(@NonNull ProvisioningPlan plan, String comment, Identity identity, Map<String, Object> arguments) {
	Common.error(LOG_NAME, "ISD-Entering createRITMDisplay");
	StringBuffer buffer = null;
	Map<String, String> configMap = new HashMap<>();
	QueryUtils qUtils = null;
	Map<String, Object> attMap = new HashMap<>();
	String shortDesc = null;
	Map<String, String> orderMap = null;
	String custObjectName = "ASCN-ServiceNow-RITM-Display";
	
	try {
		Common.info(LOG_NAME, "plan in try " + plan.toXml());
		Common.info(LOG_NAME, "identity in try " + identity.getName());
		if (plan != null && identity != null) {
			qUtils = new QueryUtils();
			attMap = qUtils.getBasicIDAttributes(identity.getName());
			List<AccountRequest> accountRequests = plan.getAccountRequests();
						
			if (!Util.isEmpty(accountRequests)) {
				
				for (AccountRequest account : accountRequests) {
					buffer = new StringBuffer();
					appName = account.getApplicationName();
					shortDesc = "AAM Sailpoint Access Request for Application::" + appName;
					if (appName == null) {
						Common.debug(LOG_NAME,  " No Application Name, Application Name required!");
					}
					configMap = GeneralUtils.loadCustomMap(appName, custObjectName);
					if (configMap == null || configMap.isEmpty()) {
						configMap = GeneralUtils.loadAPIConfiguration("default", custObjectName);
					}
					// Formatting
					if (null != configMap) {
						Map<String, Map<String, String>> map2 = new HashMap<>();
						// Iterate over the entries of configMap
						for (Map.Entry<String, String> entry : configMap.entrySet()) {
							String fullKey = entry.getKey();
							String value = entry.getValue();
							// Find the index of the '#' character
							int hashIndex = fullKey.indexOf('#');
							if (hashIndex != -1) {
								// Extract prefix (part before '#') and internal key (part after '#')
								String prefix = fullKey.substring(0, hashIndex);
								String internalKey = fullKey.substring(hashIndex + 1);
								// Put the internal key and value into the inner map
								@SuppressWarnings("rawtypes")
								Map<String, String> innerMap = new HashMap<>();
								innerMap.put(internalKey, value);
								map2.put(prefix, innerMap);
							}
						}
						orderMap = new LinkedHashMap<>();
						for (int i = 1;; i++) {
							String key1 = "H" + i;
							if (configMap.get(key1) != null && !configMap.get(key1).isEmpty()) {
								orderMap.put(key1, configMap.get(key1));
							} else {
								break;
							}
							for (int j = 1;; j++) {
								String key2 = "H" + i + "S" + j;
								if (map2.get(key2) != null && !map2.get(key2).isEmpty()) {
									orderMap.putAll(map2.get(key2));
									
								} else {
									break;
								}
							}
						}
					}
					Common.error(LOG_NAME, "desk 2" + orderMap);
					String regex = "H(\\d+)";
					// Compile the regular expression
					Pattern pattern = Pattern.compile(regex);
					if (null != orderMap) {
						for (Map.Entry<String, String> m : orderMap.entrySet()) {
							if (pattern.matcher(m.getKey()).matches()) {
								buffer.append("\n" + m.getValue() + "\n-----------------------------\n");
							} else if (m.getValue().startsWith("identity#")) {
								buffer.append(m.getKey() + identity.getAttribute(m.getValue().split("#")[1]) + "\n");
							} else if (m.getValue().startsWith("identityManager#")) {
								buffer.append(m.getKey()+ getIdentityManager(identity, m.getValue().split("#")[1]) + "\n");
							} else if (m.getValue().startsWith("arguments#")) {
								buffer.append(m.getKey() + arguments.get(m.getValue().toString().split("#")[1]) + "\n");
							} else if (m.getValue().split("#")[0].equalsIgnoreCase("getAttributeRequest")) {
								buffer.append(m.getKey() + getAttributeRequest(account, m.getValue().split("#")[1]) + "\n\n");
								
							}
							else if (m.getValue().startsWith("accountReq#")){
							    if((m.getValue().split("#")[1]).equalsIgnoreCase("applicationName")){
							        buffer.append(m.getKey()+ account.getApplicationName()+ "\n");
							    }else if((m.getValue().split("#")[1]).equalsIgnoreCase("nativeIdentity")){
							        buffer.append(m.getKey()+ account.getNativeIdentity()+ "\n");
							    }
							}
							else if (m.getValue().toString().split("#")[0].equalsIgnoreCase("getAttributeRequestList")) {
								List<String> faciltyAccList = getAttributeRequestList(account,m.getValue().split("#")[1]);
								buffer.append(m.getKey() + "\n");
								if (faciltyAccList.size() > 0) {
									for (String fAccVal : faciltyAccList) {
										buffer.append(fAccVal + "\n\n");
									}
								} else {
									buffer.append("N/A" + "\n\n");
								}
							} else if (m.getValue().toString().startsWith("getRoleAttributeRequests")) {
								@SuppressWarnings("unchecked")
								List<String> roleRequestList = getRoleAttributeRequests(account, m.getValue().toString().split("#")[1]);
								
								if (roleRequestList.size() > 0) {
									for (String entValue : roleRequestList) {
										buffer.append(entValue + "\n\n");
									}
								} else {
									buffer.append("N/A" + "\n\n");
								}
							} else if (m.getValue().toString().startsWith("roleList#")) {
								List<AttributeRequest> getAttrRequests = account.getAttributeRequests();
								for (AttributeRequest attrRequest : getAttrRequests) {
									String[] roles = (m.getValue().toString().split("#")[1]).split("\\|");
									List<String> roleList = Arrays.asList(roles);
									if (roleList.contains(attrRequest.getValue().toString())) {
										buffer.append(attrRequest.getOperation().toString() + " "+ attrRequest.getName() + " : "+ attrRequest.getValue().toString() + "\n");
										buffer.append("add My User Experience- Nursing Group" + "\n");
									} else {
										buffer.append(attrRequest.getOperation().toString() + " "+ attrRequest.getName() + " : "+ attrRequest.getValue().toString() + "\n");
									}
								}
							} else {
								buffer.append(m.getKey().toString() + m.getValue() + "\n");
							}
						}
						if (appName.contains("Epic")) {
							updatePlanForEpicApplications(account);
						}
					}
					account.put("requestDescription", buffer.toString());
					account.put("shortDescription", shortDesc);
				}
				
			}
		} else {
			Common.debug(LOG_NAME, "Null values received in the method createRITMDisplay :: plan " + plan.toXml() + " :: identity: "+ identity);
		}
	} catch (Exception ex) {
		Common.error(LOG_NAME, "Exception in createRITMDisplay " + ex,ex);
	}
	Common.info(LOG_NAME, "comment in identityiq servicenow service desk 1 *****" + buffer.toString());
	Common.info(LOG_NAME, "ISD-Exiting createRITMDisplay");
		return plan;
	}
  
public void updatePlanForEpicApplications(AccountRequest account) {
	String methodName = "Method updatePlanForEpicApplications "+account.getApplicationName();
    Common.info(LOG_NAME, "Entering "+methodName);
    try {
        List<AttributeRequest> attrReqs = account.getAttributeRequests();
        AttributeRequest epicAttrReq = null;
        if ((attrReqs != null) && (attrReqs.size() > 0)) {
            // Iterate through AttributeRequests and find dummy entitlement
            for (AttributeRequest attrReq : attrReqs) {
                if (attrReq.getName().equals("AccountOnly")) {
                    epicAttrReq = attrReq;
                    break;
                }
            }
        }
        if (epicAttrReq != null) {
            // Remove dummy entitlement AttributeRequest from AccountRequest
            account.remove(epicAttrReq);
        }
    } catch (Exception e) {
        Common.error(LOG_NAME, "Error in "+methodName+" "+e,e);
    }
    Common.info(LOG_NAME, "Exiting "+methodName);
}

public boolean hasPreviousPendingRequest(Identity identity,String roleName, String appName,String entitlementString){
    boolean flag = false;
    List results = new ArrayList<>();
    List<IdentityRequest> identityRequests = null;
    try {
    SailPointContext context = SailPointFactory.getCurrentContext();
    QueryOptions ops = new QueryOptions();
    ops.add(Filter.eq("IdentityRequest.type", "AccessRequest"));
    ops.add(Filter.eq("IdentityRequest.targetId", identity.getId()));
    ops.add(new Filter[] {
    Filter.or(Filter.eq("IdentityRequest.completionStatus","Pending"),
    Filter.eq("IdentityRequest.completionStatus","Incomplete"))
    }); 

    identityRequests = context.getObjects(IdentityRequest.class, ops);
    int count =0;
    if(identityRequests != null){
        for(Iterator i$ = identityRequests.iterator(); i$.hasNext();){
            IdentityRequest request = (IdentityRequest)i$.next();
            List<IdentityRequestItem> reqItems = request.getItems();
            for (IdentityRequestItem reqItem : reqItems) {
                count++;
                ProvisioningPlan provisionplan = reqItem.getProvisioningPlan();
                if(provisionplan != null){
                      for (AccountRequest accReqst : provisionplan.getAccountRequests()){
                        String targetIntegration = accReqst.getTargetIntegration();
                        if(targetIntegration !=null && targetIntegration.equals("IdentityIQ for ServiceNow Service Desk")){
                            Common.info(LOG_NAME,targetIntegration);
                            String accAppName = accReqst.getApplication();
                            String accRoleName = accReqst.get("roleName") != null ?  accReqst.get("roleName").toString() : null;
                            Common.info(LOG_NAME,accAppName);
                            Common.info(LOG_NAME,accRoleName+" roleName : "+roleName);
                            if(accRoleName!=null && roleName!=null && accRoleName.equalsIgnoreCase(roleName)){
                                flag = true;
                                break;
                            }
                            if(accReqst.getAttributeRequests()!=null){
                                for(AttributeRequest attrRest : accReqst.getAttributeRequests()){
                                    String entitlement= attrRest.getValue() != null ? attrRest.getValue().toString() : null;
                                    Common.info(LOG_NAME, accReqst.getApplication()+" : entitlement : "+entitlement);
                                    if(accAppName!=null && accAppName.equals(appName) && entitlementString != null && entitlementString.contains(entitlement)){
                                        flag = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } catch (Exception e) {
    	
    }
    return flag;
}

/**
 * This method addresses the issue of Zmobie tickets. Evaluates if there is a pending
 * Access Request and skips the transaction during Indetity Request opeartion
 * @param identityId
 * @return boolean
 */
public boolean hasPreviousPendingRequest(String identityId){
    boolean flag = false;
    List results = new ArrayList<>();
    List<IdentityRequest> identityRequests = null;
    try {
		SailPointContext context = SailPointFactory.getCurrentContext();
		QueryOptions ops = new QueryOptions();
		ops.add(Filter.eq("IdentityRequest.type", "AccessRequest"));
		ops.add(Filter.eq("IdentityRequest.targetId", identityId));
		ops.add(new Filter[] {
		Filter.or(Filter.eq("IdentityRequest.completionStatus","Pending"),
		Filter.eq("IdentityRequest.completionStatus","Incomplete"))
		}); 

		identityRequests = context.getObjects(IdentityRequest.class, ops);
		int count =0;
		if(identityRequests != null){
			flag = true;
			
		}
		} catch(Exception e) {
			//To-DO
		}
		return flag;
		}
}