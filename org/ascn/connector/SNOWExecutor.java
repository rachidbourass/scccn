package org.ascn.connector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ascn.utils.Common;
import org.ascn.utils.GeneralUtils;
import org.ascn.utils.QueryUtils;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Attributes;
import sailpoint.object.Identity;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class SNOWExecutor {
	
private String LOG_NAME = this.getClass().getName();	

@SuppressWarnings("unchecked")
public ProvisioningPlan sdExecute(ProvisioningPlan plan) {
	String methodName = "Method sdExecute ";
    Common.info(LOG_NAME, "Entering "+methodName);
	String appName = "";
	String bundleSnowAssignmentGroup = "";
	String requestedFor = "";
	String networkID = "";
	String email = "";
	String requesterComment = "";
	String managerSamAccountName = "";
	String firstName = "";
	String lastName = "";
	String requestedByFirstName = "";
	String requestedByLastName = "";
	String requestDescription = "";
	String requestAppDesc = "";
	String requester = "";
	String strRequesterName="RequesterNotNull";
	try {
		SailPointContext context = SailPointFactory.createContext();
		ServiceNowHelper snh = new ServiceNowHelper();
		if (plan == null) {
			Common.debug(LOG_NAME, methodName+" Plan is null, Plan is required to process transaction");
			throw new RuntimeException(LOG_NAME+" PLAN is Null or Empty. Can not process transaction!");
		}
		if (plan.getArguments() == null) {
			Common.debug(LOG_NAME, methodName+" Plan ARGUMENTS is null, is required to process transaction");
			throw new RuntimeException(LOG_NAME+" Plan ARGUMENTS are missing or Empty. Can not process transaction! ");
		}
		Attributes<String, Object> arguments = plan.getArguments();
		Identity identity = plan.getIdentity();
		if (Util.isNotNullOrEmpty(plan.getString("identityRequestId"))) {
			requesterComment = snh.getRequesterCommnets(plan.getString("identityRequestId"));
			if (Util.isNotNullOrEmpty(requesterComment)) {
				arguments.put("requesterComment", requesterComment);
			}
		} else {
			if (null != identity) {
				List<AccountRequest> accountRequests = plan.getAccountRequests();
				if (!Util.isEmpty(accountRequests)) {
					Map<String, Object> fileWriteArgs = new HashMap<>();
					String roleApps = null;
					for (AccountRequest account : accountRequests) {
						if (account.getOperation().equals(AccountRequest.Operation.Create)) {
							appName = account.getApplicationName();
							if (Util.isNotNullOrEmpty(appName)
									&& (appName.equalsIgnoreCase("INEVA/ININD - Pyxis ES")
											|| appName.equalsIgnoreCase("ININD - Pyxis ES - Anderson"))) { // TO DO - Move this method into ServiceNowHelper
								String nativeIdentity = account.getNativeIdentity();
	
								if (Util.isNotNullOrEmpty(nativeIdentity)) {
									fileWriteArgs.put("Username", nativeIdentity);
									
									List<AttributeRequest> attrRequests = account.getAttributeRequests();
									if (!attrRequests.isEmpty() || null != attrRequests) {
										for (AttributeRequest attrReq : attrRequests) {
											String attrName = attrReq.getName();
											String attrValue = GeneralUtils.OTOS(attrReq.getValue());
											fileWriteArgs.put(attrName, attrValue);
										}
									}
								}
							}
						}
					}
					
					if (Util.isNotNullOrEmpty(appName) && null != fileWriteArgs && !fileWriteArgs.isEmpty()) {
						snh.writeToFile(appName, fileWriteArgs);
						boolean aggregated = exeTargetAggregation(appName,fileWriteArgs);
						Common.info(LOG_NAME, methodName+" Target Aggregation is "+aggregated);
					}
				}
			}
		}
	
		if (identity != null) {
			requestedFor = identity.getName();
			networkID = identity.getStringAttribute("samAccountName");
			email = identity.getEmail();
			firstName = identity.getFirstname();
			lastName = identity.getLastname();
			if (identity.getManager() != null) {
				managerSamAccountName = identity.getManager().getStringAttribute("samAccountName");
			}
		}
	
		Map<String, Object> requesterIdentity = null;
		if (arguments.get("requester") != null) {
			requester = GeneralUtils.OTOS(arguments.get("requester"));
			//added by shilpa
			if (requester == null) {
				strRequesterName = null;
			}
			if (requester.equalsIgnoreCase("RequestHandler")) {
				requestedByFirstName = "RequestHandler_sailpoint";
				requestedByLastName = "RequestHandler_sailpoint";
			} else {
				requesterIdentity = QueryUtils.getBasicIdentity(requester);
				if (requesterIdentity != null) {
					requestedByFirstName = GeneralUtils.OTOS(requesterIdentity.get("Firstname"));
					requestedByLastName = GeneralUtils.OTOS(requesterIdentity.get("Lastname"));
				}
			}
		}
	
		//added by shilpa
		if (requester.equalsIgnoreCase("RequestHandler")) {
			arguments.put("opened_by", "RequestHandler_sailpoint");
		}
		if (requesterIdentity != null) {
			arguments.put("opened_by", requesterIdentity.get("name"));
		} // added by shilpa
		else if (requesterIdentity == null && requester.equalsIgnoreCase("RequestHandler")) {
			arguments.put("opened_by", "RequestHandler_sailpoint");
		} else {
			Common.debug(LOG_NAME, "Setting Opened by argument is default");
			strRequesterName = null;
			arguments.put("opened_by", "980df3d31b8ce0943f951025bd4bcbd7"); 
			// temp solution - defaults to user
			// sail_point
		}
	
		if (Util.isNotNullOrEmpty(requestedFor)) {
			Common.debug(LOG_NAME, "Setting requested_for argument in plan");
			arguments.put("requested_for", requestedFor);
		}
		if (Util.isNotNullOrEmpty(networkID)) {
			Common.debug(LOG_NAME, "Setting networkID argument in plan");
			arguments.put("networkID", networkID);
		}
		if (Util.isNotNullOrEmpty(email)) {
			Common.debug(LOG_NAME, "Setting email argument in plan");
			arguments.put("email", email);
		}
	
		if (Util.isNotNullOrEmpty(firstName)) {
			Common.debug(LOG_NAME, "Setting firstName argument in plan");
			arguments.put("requestedForFirstName", firstName);
		}
	
		if (Util.isNotNullOrEmpty(lastName)) {
			Common.debug(LOG_NAME, "Setting lastName argument in plan");
			arguments.put("requestedForLastName", lastName);
		}
	
		if (Util.isNotNullOrEmpty(requestedByFirstName)) {
			Common.debug(LOG_NAME, "Setting requestedByFirstName argument in plan");
			arguments.put("requestedByFirstName", requestedByFirstName);
		}
	
		if (Util.isNotNullOrEmpty(requestedByLastName)) {
			Common.debug(LOG_NAME, "Setting requestedByLastName argument in plan");
			arguments.put("requestedByLastName", requestedByLastName);
		}
	
		String shortDesc = "AAM (SailPoint) Request for "+lastName+", "+firstName+" on Application: " +appName;
		if (shortDesc.length() > 256) {
			shortDesc = StringUtils.abbreviate(shortDesc, 256);
		}
		arguments.put("shortDesc", shortDesc);
		ProvisioningPlan newPlan = snh.createRITMDisplay(plan, requestAppDesc, identity, arguments);
		plan = snh.addAssignmentGroupInPlan(newPlan, strRequesterName);
	} catch (Exception e) {
		Common.error(LOG_NAME, "Error in: "+methodName+" "+e,e);
	}
	Common.info(LOG_NAME, "Exiting "+methodName);
	return plan;
}

private boolean exeTargetAggregation(String appName, Map<String, Object> fileWriteArgs) {
	String methodName = "Method exeTargetAggregation ";
    Common.info(LOG_NAME, "Entering "+methodName);
	boolean targetAggResult = false;
	try {
		Attributes<String, Object> aggregationArgs = new Attributes<>();
		aggregationArgs.put("noOptimizeReaggregation", "true");
		aggregationArgs.put("correlateEntitlements", "true");
		aggregationArgs.put("promoteManagedAttributes", "true");
		aggregationArgs.put("checkDeleted", "false");
		String userName = GeneralUtils.OTOS(fileWriteArgs.get("Username"));
		targetAggResult = Common.doTargetedAggregation(userName,appName, aggregationArgs);
		Common.info(LOG_NAME, "Exiting "+methodName+" Username: "+userName+" AppName: "+appName);
	} catch (Exception e) {
		Common.error(LOG_NAME, "Error in: "+this.getClass().getSimpleName()+e,e);
	}
	return targetAggResult;
}
	
}
