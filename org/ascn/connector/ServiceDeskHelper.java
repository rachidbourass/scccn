package org.ascn.connector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ascn.utils.QueryUtils;
import org.ascn.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;

import sailpoint.object.Identity;
import sailpoint.object.QueryOptions;
import sailpoint.object.Filter;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;
import sailpoint.object.IntegrationConfig;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.Bundle;
import sailpoint.object.Custom;
import sailpoint.object.IdentityRequest;
import sailpoint.object.Profile;
import sailpoint.object.IdentityRequestItem;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.api.Provisioner;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.FileNotFoundException;

import sailpoint.api.Aggregator;
import sailpoint.connector.Connector;
import sailpoint.connector.ObjectNotFoundException;
import sailpoint.object.Attributes;
import sailpoint.object.ResourceObject;
import sailpoint.object.TaskResult;

public class ServiceDeskHelper {

	private SailPointContext context;
	private Log log = LogFactory.getLog(getClass());
	private String applicationName = "IIQ";
	private String ministry = "";
	private static final String LOG_NAME = "org.ascn.connector.ServiceDeskHelper";
	private String bundleSnowAssignmentGroup = "";

	public ServiceDeskHelper() {
		try {
			this.context = SailPointFactory.getCurrentContext();

		} catch (Exception e) {
			log.error(LOG_NAME +"Unable to create SailPointContext: "+ e, e);
			throw new RuntimeException(LOG_NAME +"Unable to create SailPointContext: "+ e, e);
		}
	}

	public boolean doTargetedAggregation(String nativeId, String appName, Attributes aggArgs) {
		String methodName = "doTargetedAggregation(String nativeId, String appName, Attributes aggArgs)";
		log.info(LOG_NAME+" "+methodName+" Entering");
		boolean isAggOK = true;
		try {
			Application app = context.getObjectByName(Application.class, appName);
			Connector connector = sailpoint.connector.ConnectorFactory.getConnector(app, null);

			if (aggArgs == null || aggArgs.isEmpty()) {
				aggArgs = new Attributes();
				aggArgs.put("noOptimizeReaggregation", "true");
				aggArgs.put("checkDeleted", "false");
			}

			ResourceObject rObj = connector.getObject("account", nativeId, null);
			if (null == rObj) {
				return false;
			}

			Aggregator agg = new Aggregator(context, aggArgs);
			TaskResult taskResult = agg.aggregate(app, rObj);

			if (taskResult == null) {
				isAggOK = false;
			} else {
				isAggOK = true;
			}
		} catch (Exception e) {
			isAggOK = false;
		}
		log.info(LOG_NAME+" "+methodName+" Exiting");
		return isAggOK;
	}

	public void writeToFile(String appName, Map writeParams) {
		if (Util.isNotNullOrEmpty(appName) && null != writeParams && !writeParams.isEmpty()) {
			Application application;
			try {
				application = context.getObjectByName(Application.class, appName);
			} catch (Exception e) {
				log.trace(LOG_NAME + e, e);
				throw new RuntimeException(LOG_NAME + e, e);
			}
			if (null != application) {
				String fileName = application.getStringAttributeValue("file");
				if (Util.isNotNullOrEmpty(fileName)) {
					// open file in read-write mode
					RandomAccessFile randomAccessFile;
					try {
						randomAccessFile = new RandomAccessFile(new File(fileName), "rw");
					} catch (FileNotFoundException e) {
						log.trace(LOG_NAME + e, e);
						throw new RuntimeException(LOG_NAME + e, e);
					}
					try {
						// get channel
						FileChannel channel = randomAccessFile.getChannel();
						// get lock
						FileLock tryLock = channel.lock();
						Object userName = writeParams.get("Username");
						String firstName = writeParams.get("Firstname") == null ? "": writeParams.get("Firstname").toString();
						String lastName = writeParams.get("Lastname") == null ? "": writeParams.get("Lastname").toString();
						String employeeNumber = writeParams.get("EmployeeNumber") == null ? "": writeParams.get("EmployeeNumber").toString();
						String emailAddress = writeParams.get("EmailAddress") == null ? "": writeParams.get("EmailAddress").toString();
						String department = writeParams.get("Department") == null ? "": writeParams.get("Department").toString();
						String role = writeParams.get("Role") == null ? "" : writeParams.get("Role").toString();
						String dataStr = "\n" + userName + "," + firstName + "," + lastName + "," + employeeNumber + ","+ emailAddress + "," + department + "," + role;
						// write to end of file
						randomAccessFile.seek(randomAccessFile.length());
						randomAccessFile.write(dataStr.getBytes());
						// release lock
						tryLock.release();
						// close file
						randomAccessFile.close();
					} catch (IOException e) {
						log.trace(LOG_NAME + e, e);
						throw new RuntimeException(LOG_NAME + e, e);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public String getRequesterCommnets(String identityRequestId) {
		log.trace(LOG_NAME + "Entering getRequesterCommnets");
		String comment = "";
		Map map = new HashMap();
		try {
			IdentityRequest identityRequestObj = context.getObjectByName(IdentityRequest.class, identityRequestId);
			if (identityRequestObj != null) {
				if (identityRequestObj.getItems() != null && !identityRequestObj.getItems().isEmpty()) {
					List<IdentityRequestItem> itemsList = identityRequestObj.getItems();
					for (IdentityRequestItem item : itemsList) {
						if (Util.isNotNullOrEmpty(item.getRequesterComments()) && Util.nullSafeCaseInsensitiveEq(
								"IdentityIQ for ServiceNow Service Desk", item.getProvisioningEngine())) {

							map.put(item.getValue(), item.getRequesterComments());
						}
					}
				}
				// context.decache(identityRequestObj);
			}
		} catch (Exception ex) {
			log.error("Exception in getRequesterCommnets " + ex.getMessage());
		}
		if (!Util.isEmpty(map)) {
			Collection values = map.values();
			if (!Util.isEmpty(values)) {
				List valueList = (new ArrayList(values));
				Util.removeDuplicates(valueList);
				comment = valueList.toString().substring(1, valueList.toString().length() - 1);
			}

		}
		log.trace("Exiting getRequesterCommnets");
		return comment;
	}

	public String findAssignmentFromBundle(ManagedAttribute eachEntitlement, String identityRequestId) {
		log.debug("Entering findAssignmentFromBundle");
		String result = "";
		String snowAssignmentGroup = "snowAssignmentGroup";

		String roleName = "";
		List<Application> inputAppList = new ArrayList();
		List<Profile> profileList = new ArrayList();
		boolean breakFlag = false;
		try {
			if (Util.isNotNullOrEmpty(identityRequestId) && eachEntitlement != null
					&& Util.isNotNullOrEmpty(eachEntitlement.getAttribute())
					&& Util.isNotNullOrEmpty(eachEntitlement.getValue()) && eachEntitlement.getApplication() != null) {
				inputAppList.add(eachEntitlement.getApplication());
				// get identity requestid obj
				IdentityRequest identityRequestObj = context.getObjectByName(IdentityRequest.class, identityRequestId);
				if (identityRequestObj != null) {
					if (identityRequestObj.getItems() != null && !identityRequestObj.getItems().isEmpty()) {
						List<IdentityRequestItem> itemsList = identityRequestObj.getItems();
						for (IdentityRequestItem item : itemsList) {
							if (breakFlag) {
								break;
							}
							if ("IIQ".equalsIgnoreCase(item.getApplication())) {
								roleName = "";
								if (item.getValue() instanceof String) {
									roleName = item.getValue().toString();
								} else if (item.getValue() instanceof List) {
									if (!((List) item.getValue()).isEmpty()) {
										roleName = ((List) item.getValue()).get(0).toString();
									}
								}
								// get role obj
								if (Util.isNotNullOrEmpty(roleName)) {
									Bundle bundle = context.getObjectByName(Bundle.class, roleName);
									if (bundle != null && bundle.getAttribute(snowAssignmentGroup) != null) {
										profileList = null;
										profileList = bundle.getProfilesForApplications(inputAppList);
										if (profileList != null && !profileList.isEmpty()) {
											for (Profile profile : profileList) {
												if (breakFlag) {
													break;
												}
												for (Filter filter : profile.getConstraints()) {
													if (Util.isNotNullOrEmpty(filter.getExpression())) {
														if (filter.getExpression()
																.contains(eachEntitlement.getAttribute())
																&& filter.getExpression()
																		.contains(eachEntitlement.getValue())) {
															// get the assignment details and break
															result = (String) bundle.getAttribute(snowAssignmentGroup);
															breakFlag = true;
														}
													}
												}
											}
										}
										// context.decache(bundle);
									}
								}
							}
						}
					}
					// context.decache(identityRequestObj);
				}
			}
		} catch (Exception ex) {
			log.error("Exception in findAssignmentFromBundle " + ex.getMessage());
		}
		log.debug("Exiting findAssignmentFromBundle");
		return result;
	}

	public Map addPropertyInPlan(ManagedAttribute managedAttr) {
		Set setValue = new HashSet();
		setValue.add("approvalType");
		setValue.add("ascBusinessApplication");
		setValue.add("snowAssignmentGroup");
		setValue.add("snowAssignmentGroupName");
		setValue.add("sysDescriptions");

		Map attributeMap = new HashMap();
		if (null != managedAttr && null != managedAttr.getAttributes()
				&& null != managedAttr.getAttributes().getMap()) {
			attributeMap = managedAttr.getAttributes().getMap();
			if (null != attributeMap && null != attributeMap.keySet()) {
				attributeMap.keySet().removeAll(setValue);
			}
		}
		return attributeMap;
	}

	public void addAssignmentGroupInPlan(ProvisioningPlan plan, String reqName) throws GeneralException {
		log.debug("Starting addAssignmentGroupInPlan");
		String assignmentGroup = null;
		String businessApplication = null;
		boolean isAccReqAdded = false;

		if (plan != null) {
			List<AccountRequest> accountRequests = plan.getAccountRequests();
			List splitAccountRequests = new ArrayList<>();
			if (!Util.isEmpty(accountRequests)) {
				for (AccountRequest request : accountRequests) {
					applicationName = request.getApplicationName();
					List<AttributeRequest> attributeRequests = request.getAttributeRequests();
					isAccReqAdded = false;

					if (!Util.isEmpty(attributeRequests)) {
						for (AttributeRequest attrRequest : attributeRequests) {
							assignmentGroup = null;
							businessApplication = null;
							Map propertyMap = new HashMap();
							Map accReqAttrMap = new HashMap();
							Attributes propertyAttr = new Attributes();
							String attrName = attrRequest.getName();
							Object attrValue = attrRequest.getValue();
							Filter appFilter = Filter.eq("application.name", applicationName);
							Filter attributeFilter = Filter.eq("attribute", attrName);
							if (attrValue instanceof String && Util.isNotNullOrEmpty(attrName)
									&& Util.isNotNullOrEmpty(attrValue.toString())) {

								Filter valueFilter = Filter.eq("value", attrValue);
								Filter finalFilter = Filter.and(appFilter, attributeFilter, valueFilter);
								ManagedAttribute entitlement = context.getUniqueObject(ManagedAttribute.class,
										finalFilter);

								if (entitlement != null) {
									bundleSnowAssignmentGroup = findAssignmentFromBundle(entitlement,
											plan.getString("identityRequestId"));
									if (Util.isNotNullOrEmpty(bundleSnowAssignmentGroup)) {
										assignmentGroup = bundleSnowAssignmentGroup;
									} else {
										assignmentGroup = entitlement.getAttribute("snowAssignmentGroup").toString();
									}
									if (Util.isNotNullOrEmpty(assignmentGroup)) {
										if (reqName == null) {
											assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5";
										}
										businessApplication = entitlement.getAttribute("ascBusinessApplication")
												.toString();
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
									if (entitlement != null) {
										// context.decache(entitlement);
									}
								} else if (attrValue instanceof List && Util.isNotNullOrEmpty(attrName)
										&& null != attrValue) {
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
												assignmentGroup = eachEntitlement.getAttribute("snowAssignmentGroup")
														.toString();
											}
											if (Util.isNotNullOrEmpty(assignmentGroup)) {
												if (reqName == null) {
													assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5"; // Identity
																											// governance
												}
												log.error("assignment group is::" + assignmentGroup);
												businessApplication = eachEntitlement
														.getAttribute("ascBusinessApplication").toString();
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
							if (Util.isEmpty(attributeRequests) || !isAccReqAdded) {

								AccountRequest eachAcctReq = new AccountRequest(request);

								String appName = request.getApplication();
								Application appObj = context.getObjectByName(Application.class, appName);
								if (appObj != null) {
									assignmentGroup = (String) ((appObj
											.getAttributeValue("snowAssignmentGroup") != null)
													? appObj.getAttributeValue("snowAssignmentGroup")
													: "");

									businessApplication = (String) ((appObj
											.getAttributeValue("ascBusinessApplication") != null)
													? appObj.getAttributeValue("ascBusinessApplication")
													: appName);
									if (Util.isNotNullOrEmpty(assignmentGroup)) {
										if (reqName == null) {
											assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5"; // Identity governance
										}
										log.error("assignment group is::" + assignmentGroup);
										if (Util.isNotNullOrEmpty(assignmentGroup)) {
											if (reqName == null) {
												assignmentGroup = "fe159ef11b935094768fea4fad4bcbe5"; // Identity
																										// governance
											}
											eachAcctReq.add(new AttributeRequest("assignment_group",
													ProvisioningPlan.Operation.Set, assignmentGroup));
										}
										if (Util.isNotNullOrEmpty(businessApplication)) {
											eachAcctReq.add(new AttributeRequest("businessApplication",
													ProvisioningPlan.Operation.Set, businessApplication));
										}
										// context.decache(appObj);
									}
									isAccReqAdded = true;
									splitAccountRequests.add(eachAcctReq);
								}

							}
						}
						plan.setAccountRequests(splitAccountRequests);
					}
					log.debug("Exiting addAssignmentGroupInPlan");
				}
			}
		}
	}

	public String buildFullName(Identity identity) {

		String firstName = identity.getFirstname();
		String lastName = identity.getLastname();
		String middleName = identity.getStringAttribute("middleName");
		String fullName = firstName;
		if (middleName != null) {
			fullName = fullName + " " + middleName;
		}
		fullName = fullName + " " + lastName;

		return fullName;
	}

	public String getAttributeRequest(AccountRequest accountReq, String attrName) {
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
		return result;
	}

	public List getRoleAttributeRequests(AccountRequest accountReq, String attrName) {
		List results = new ArrayList();

		List<AttributeRequest> attrReqList = accountReq.getAttributeRequests(attrName);
		if (attrReqList != null && attrReqList.size() > 0) {
			for (AttributeRequest attrReq : attrReqList) {
				results.add(attrReq.getValue());
			}
		}
		if (results.size() > 0) {
			results.add("N/A");
		}
		return results;
	}

	public List getAttributeRequestList(AccountRequest accountReq, String attrName) {
		List result = new ArrayList();
		AttributeRequest attrReq = accountReq.getAttributeRequest(attrName);
		if (attrReq != null) {

			if (attrReq.getValue() instanceof List) {

				result.addAll((List) attrReq.getValue());
			} else if (attrReq.getValue() instanceof String) {

				result.add((String) attrReq.getValue());
			}
		}
		return result;
	}

	// Created by Abhijith
	public String getAttributesFromID(String attribute, Map<String,Object> attMap, AccountRequest account){

        switch(attribute){
        
        case "Manager":
            return (attMap.get("manager") != null ? ((Identity) attMap.get("manager")).getName() : "N/A") + "-" + (attMap.get("manager") != null ? ((Identity) attMap.get("manager")).getDisplayableName() : "N/A");
                    
        case "Manager Email":
            return (attMap.get("manager") != null && ((Identity) attMap.get("manager")).getEmail() != null) ? ((Identity)attMap.get("manager")).getEmail() : "N/A" ;
            
        case "Manager Phone Number":
            return (attMap.get("manager") != null && ((Identity) attMap.get("manager")).getAttribute("telephoneNumber") != null) ? ((Identity) attMap.get("manager")).getAttribute("telephoneNumber").toString() : "N/A" ;
            
        case "Application Name":
            return account.getApplicationName();
            
        }
        return "N/A";

    }
    
	// Implemented # to update Provisioning Plan with "Epic-Wiggle" app request
    // details - SreeRadhika
    public void updatePlanForEpicApplications(AccountRequest account) {
        log.error("ISD-Entering updatePlanForEpicApplications");
        log.debug("inside updatePlanForEpicApplications ***********");
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
            log.debug("Inside the Catch Block of updatePlanForEpicApplications method" + e);
        }
        log.error("ISD-Exiting updatePlanForEpicApplications");
    }
    
	public ProvisioningPlan createRITMDisplay(ProvisioningPlan plan, String comment, Identity identity, Map arguments) {
        log.error("ISD-Entering createRITMDisplay");
        StringBuffer buffer = null;
        Map<String, Object> appCheck = null;
        Map<String, String> configMap = null;
        QueryUtils qUtils = null;
        Map<String, Object> attMap = null;
        String shortDesc = null;
        try {
            log.error("plan in try "+plan);
            log.error("identity in try "+identity);
                    
            if (plan != null && identity != null) {
                qUtils = new QueryUtils();
                attMap = qUtils.getBasicIDAttributes(identity.getName());
                
                List<AccountRequest> accountRequests = plan.getAccountRequests();
                if (!Util.isEmpty(accountRequests)) {
                    for (AccountRequest account : accountRequests) {
                        applicationName = account.getApplicationName();
                        shortDesc = "AAM Sailpoint Access Request for Application::" + applicationName;
                        // Modified by Abhijith
                        Custom custom = context.getObjectByName(Custom.class, "ASCN-ServiceNow-RITM-Display");                      
                        
                        if (custom !=null){
                            buffer = new StringBuffer();
                            appCheck = custom.getAttributes();                         	
                           
                            if (null != configMap && appCheck.containsKey(applicationName)){
                                buffer.append("\n\n"+"Request  Information: " + "\n" + "---------------------\n");
                             //   configMap = (Map<String, String>) custom.get(applicationName);
                                configMap = GeneralUtils.loadAPIConfiguration(applicationName,"ASCN-ServiceNow-RITM-Display");
                                for(Map.Entry<String, String> m : configMap.entrySet()){
                                    if (m.getValue().toString().startsWith("identity#")){
                                        buffer.append(m.getKey()+ identity.getAttribute(m.getValue().toString().split("#")[1]) +"\n");
                                    }else if (m.getValue().toString().startsWith("fetch#")){
                                        buffer.append(m.getKey()+ getAttributesFromID(m.getValue().toString().split("#")[1],attMap,account) +"\n");
                                    }else if (m.getValue().toString().startsWith("arguments#")){
                                        buffer.append(m.getKey()+ arguments.get(m.getValue().split("#")[1]) +"\n");
                                    }else if (m.getValue().toString().split("#")[0].equalsIgnoreCase("getAttributeRequest")){
                                        buffer.append("\n" +"Access Needed: " + "\n" + "---------------------\n");
                                        buffer.append(m.getKey()+getAttributeRequest(account, m.getValue().split("#")[1]) + "\n\n");
                                    }else if (m.getValue().toString().split("#")[0].equalsIgnoreCase("getAttributeRequestList")){
                                        List<String> faciltyAccList = getAttributeRequestList(account, m.getValue().toString().split("#")[1]);
                                        buffer.append(m.getKey()+ "\n");
                                        if (faciltyAccList.size() > 0) {
                                            for (String fAccVal : faciltyAccList) {
                                                buffer.append(fAccVal + "\n\n");
                                            }
                                        } else {
                                            buffer.append("N/A" + "\n\n");
                                        }
                                    }else if (m.getValue().toString().startsWith("getRoleAttributeRequests")){
                                        List<String> roleRequestList = getRoleAttributeRequests(account, m.getValue().toString().split("#")[1]);
                                        buffer.append("\n" +"Application Entitlements: " + "\n---------------------\n");
                                        if (roleRequestList.size() > 0) {
                                            for (String entValue : roleRequestList) {
                                                buffer.append(entValue + "\n\n");
                                            }
                                        } else {
                                            buffer.append("N/A" + "\n\n");
                                        }
                                    }else {
                                        buffer.append(m.getKey()+ m.getValue()+"\n");
                                    }
                                }     
                                if (applicationName.contains("Epic")){
                                    updatePlanForEpicApplications(account);                                   
                                }
                            }else {
                                buffer.append("\n\n"+"Request  Information: " + "\n" + "---------------------\n");
                                configMap = GeneralUtils.loadAPIConfiguration("default","ASCN-ServiceNow-RITM-Display");
                                for(Map.Entry<String, String> m : configMap.entrySet()){
                                    if (m.getValue().startsWith("identity#")){
                                        buffer.append(m.getKey()+ identity.getAttribute(m.getValue().toString().split("#")[1]) +"\n");
                                    }else if (m.getValue().startsWith("fetch#")){
                                        buffer.append(m.getKey()+ getAttributesFromID(m.getValue().toString().split("#")[1],attMap,account) +"\n");
                                    }else if (m.getValue().startsWith("arguments#")){
                                        buffer.append(m.getKey()+ arguments.get(m.getValue().toString().split("#")[1]) +"\n");
                                    }
                                }               
                                buffer.append("\n Application details:\n-----------------------------\n");
                                List<AttributeRequest> getAttrRequests = account.getAttributeRequests();
                                if (!account.getOperation().equals(AccountRequest.Operation.Create)) {
                                    // Application Account Name / Native Identity
                                    buffer.append("Account Username: " + account.getNativeIdentity() + "\n");
                                    // User Email Address
                                    buffer.append("User Email: " + attMap.get("email") + "\n");
                                }
                                for (AttributeRequest attrRequest : getAttrRequests) {
                                    List roleList = getCernerRoleForComments();

                                    if (roleList.contains(attrRequest.getValue().toString())) {
                                        buffer.append(attrRequest.getOperation().toString() + "  " + attrRequest.getName()+ " : " + attrRequest.getValue().toString() + "\n");
                                        buffer.append("add My User Experience- Nursing Group" + "\n");
                                    } else {
                                        buffer.append(attrRequest.getOperation().toString() + "  " + attrRequest.getName()+ " : " + attrRequest.getValue().toString() + "\n");
                                    }
                                }
                            }        
                        }
                        account.put("requestDescription", buffer.toString());
                        account.put("shortDescription", shortDesc);
                    }
                }
            } else {
                log.error("Null values received in the method createRITMDisplay :: plan "+plan+" :: identity: "+identity);
            }
        } catch (Exception ex) {
             log.error("Exception in createRITMDisplay " + ex.getMessage());
        }
        log.error("comment in identityiq servicenow service desk 1 *****" + buffer.toString());
        log.error("ISD-Exiting createRITMDisplay");
        return plan;
        }
	// This method is added to get the list of roles for displaying a specific
	// comment - Added for SI-1825

	public List getCernerRoleForComments() throws GeneralException {
		log.error("Inside getCernerRoleForComments() method ");
		Custom cust = context.getObjectByName(Custom.class, "Acension-Custom-CernerRoles");
		log.error("Inside getCernerRoleForComments() method cust " + cust);
		List roleList = (List) cust.get("roleNames");
		log.error("roleList************" + roleList);
		return roleList;
	}

	
	}
