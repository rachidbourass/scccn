package org.ascn.connector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Attributes;
import sailpoint.object.Identity;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class ServiceDesk {
////////////////////////////////////////////////////////////////
// Main
////////////////////////////////////////////////////////////////
	private Log log = LogFactory.getLog(getClass());
	private SailPointContext context;
	private ServiceDeskHelper sdh;
	private String applicationName = "IIQ";
	private Identity identity;

	

//  Log logger = LogFactory.getLog("Rule.Application.ServiceNowIntegration");
	String RULE_NAME = "ServiceNow ServiceDesk Plan Initializer Script";
	String bundleSnowAssignmentGroup = "";
	Map arguments = new HashMap();
	String requestedFor = "";
	String networkID = "";
	String email = "";
	String requesterComment = "";
	String managerSamAccountName = "";
	String requestedForFirstName = "";
	String requestedForLastName = "";
	String requestedByFirstName = "";
	String requestedByLastName = "";
	String requestDescription = "";
	String requestAppDesc = "";
	String requester = "";
	String strRequesterName = "RequesterNotNull";

	/*
	 * if ((context == void) || (context == null)) { context =
	 * SailPointFactory.getCurrentContext(); }
	 */
//Added by Rajesh
	public void sdExecute(ProvisioningPlan plan) throws GeneralException {
		context = SailPointFactory.getCurrentContext();
		sdh = new ServiceDeskHelper();
		log.debug("Servicenow plan is ::::" + plan.toXml());
        identity = plan.getIdentity();
		if (plan != null && plan.getArguments() != null) {
			arguments = (Map) plan.getArguments();

		}
		if (plan != null && Util.isNotNullOrEmpty(plan.getString("identityRequestId"))) {
			requesterComment = sdh.getRequesterCommnets(plan.getString("identityRequestId"));
			if (Util.isNotNullOrEmpty(requesterComment)) {
				arguments.put("requesterComment", requesterComment);
			}

		} else {
			if (null != plan && null != identity) {
				List<AccountRequest> accountRequests = plan.getAccountRequests();
				if (!Util.isEmpty(accountRequests)) {
					HashMap fileWriteArgs = new HashMap();
					String appName = "";
					String roleApps = null;

					for (AccountRequest account : accountRequests) {
						/*
						 * if(roleApps==null){ roleApps=account.getApplicationName(); }else{
						 * if(!roleApps.contains(account.getApplicationName())){
						 * roleApps=roleapps+" , "+account.getApplicationName(); }
						 */
						if (account.getOperation().equals(AccountRequest.Operation.Create)) {
							String applicationName = account.getApplicationName();
							if (Util.isNotNullOrEmpty(applicationName)
									&& (applicationName.equalsIgnoreCase("INEVA/ININD - Pyxis ES")
											|| applicationName.equalsIgnoreCase("ININD - Pyxis ES - Anderson"))) {
								System.out.println("Application name matched");
								appName = applicationName;
								String nativeIdentity = account.getNativeIdentity();

								if (Util.isNotNullOrEmpty(nativeIdentity)) {
									fileWriteArgs.put("Username", nativeIdentity);

									List<AttributeRequest> attrRequests = account.getAttributeRequests();
									if (!Util.isEmpty(attrRequests)) {
										for (AttributeRequest attrReq : attrRequests) {
											String attrName = attrReq.getName();
											String attrValue = attrReq.getValue().toString();
											fileWriteArgs.put(attrName, attrValue);
										}
									}
								}
							}
						}
					}
					if (Util.isNotNullOrEmpty(appName) && null != fileWriteArgs && !fileWriteArgs.isEmpty()) {
						sdh.writeToFile(appName, fileWriteArgs);
						Attributes aggregationArgs = new Attributes();
						aggregationArgs.put("noOptimizeReaggregation", "true");
						aggregationArgs.put("correlateEntitlements", "true");
						aggregationArgs.put("promoteManagedAttributes", "true");
						aggregationArgs.put("checkDeleted", "false");
						boolean targetAggResult = sdh.doTargetedAggregation(fileWriteArgs.get("Username").toString(),
								appName, aggregationArgs);
					}
				}
			}
		}

		if (identity != null) {
			requestedFor = identity.getName();
			networkID = identity.getStringAttribute("samAccountName");
			email = identity.getEmail();
			requestedForFirstName = identity.getFirstname();
			requestedForLastName = identity.getLastname();
			if (identity.getManager() != null) {
				managerSamAccountName = identity.getManager().getStringAttribute("samAccountName");
			}
		}

		Identity requesterIdentity = null;
		if (arguments.get("requester") != null) {
			requester = arguments.get("requester").toString();
//added by shilpa
			if (requester == null) {
				log.debug("Requester is null");
				strRequesterName = null;
				
			}
			if (requester.equalsIgnoreCase("RequestHandler")) {
				requestedByFirstName = "RequestHandler_sailpoint";
				requestedByLastName = "RequestHandler_sailpoint";
			} else {
				requesterIdentity = context.getObjectByName(Identity.class, requester);
				if (requesterIdentity != null) {
					requestedByFirstName = requesterIdentity.getFirstname();
					requestedByLastName = requesterIdentity.getLastname();
				}
			}
		}
		log.error("strRequesterName is::" + strRequesterName);

//  if (Util.isNotNullOrEmpty(managerSamAccountName)) {
		log.debug("Setting Opened by argument in plan");
// arguments.put("opened_by", managerSamAccountName);}
//added by shilpa
		if (requester.equalsIgnoreCase("RequestHandler")) {
			arguments.put("opened_by", "RequestHandler_sailpoint");
		}
		if (requesterIdentity != null) {
			arguments.put("opened_by", requesterIdentity.getName());
		} // added by shilpa
		else if (requesterIdentity == null && requester.equalsIgnoreCase("RequestHandler")) {
			arguments.put("opened_by", "RequestHandler_sailpoint");
		} else {
			log.debug("Setting Opened by argument is default");
			strRequesterName = null;
			arguments.put("opened_by", "980df3d31b8ce0943f951025bd4bcbd7"); // temp solution - defaults to user
																			// sail_point
		}

		if (Util.isNotNullOrEmpty(requestedFor)) {
			log.debug("Setting requested_for argument in plan");
			arguments.put("requested_for", requestedFor);
		}
		if (Util.isNotNullOrEmpty(networkID)) {
			log.debug("Setting networkID argument in plan");
			arguments.put("networkID", networkID);
		}
		if (Util.isNotNullOrEmpty(email)) {
			log.debug("Setting email argument in plan");
			arguments.put("email", email);
		}

		if (Util.isNotNullOrEmpty(requestedForFirstName)) {
			log.debug("Setting requestedForFirstName argument in plan");
			arguments.put("requestedForFirstName", requestedForFirstName);
		}

		if (Util.isNotNullOrEmpty(requestedForLastName)) {
			log.debug("Setting requestedForLastName argument in plan");
			arguments.put("requestedForLastName", requestedForLastName);
		}

		if (Util.isNotNullOrEmpty(requestedByFirstName)) {
			log.debug("Setting requestedByFirstName argument in plan");
			arguments.put("requestedByFirstName", requestedByFirstName);
		}

		if (Util.isNotNullOrEmpty(requestedByLastName)) {
			log.debug("Setting requestedByLastName argument in plan");
			arguments.put("requestedByLastName", requestedByLastName);
		}

		
				String shortDesc = "AAM (SailPoint) Request for " + identity.getLastname() + ", " + identity.getFirstname()
				+ " on Application: " + applicationName;
		if (shortDesc.length() > 256) {
			shortDesc = StringUtils.abbreviate(shortDesc, 256);
		}
		arguments.put("shortDesc", shortDesc);

//added by shilpa
		Map varMap = new HashMap();
		varMap.put("business_justification", "shortDescription test");
		varMap.put("comments", "Test comments");
		varMap.put("short_description", "shortDescription test");
		varMap.put("req_description", "shortDescription req desc");

		arguments.put("variables", varMap);

//Rajjesh updated on apr22
//logs("INITIAL REQUESTED COMMENTS RAJESH plan:- "+plan.toXml(),"snowdesk");
		//plan = sdh.newUpdateRequirementDesc(plan, requestAppDesc, identity, arguments);
		plan = sdh.createRITMDisplay(plan, requestAppDesc, identity, arguments);
//logs("FINAL REQUESTED COMMENTS RAJESH plan:- "+plan.toXml(),"snowdesk");
//Rajjesh updated on apr22 Ended

		log.debug("Updating assignment_group in AccountRequests");
		sdh.addAssignmentGroupInPlan(plan, strRequesterName);
		log.debug("snow plann is::::" + plan.toXml());
		log.debug("Exiting " + RULE_NAME);
	}
}
