package org.ascn.connector;

import java.io.File;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.ascn.utils.Common;
import org.ascn.utils.GeneralUtils;
import org.ascn.utils.QueryUtils;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Application;
import sailpoint.object.Bundle;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.IdentityRequest;
import sailpoint.object.IdentityRequestItem;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.object.ProvisioningRequest;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

/**
 * ServiceNowHelper is a class containing methods that are used in validating,
 * formatting , building content to create REQUEST Object, RITM and Tasks
 * ServiceNow using ServiceDesk integration
 */

public class ServiceNowHelper {

	private SailPointContext context;
	private String LOG_NAME = "org.ascn.connector.ServiceNowHelper";
	private Map<String, Object> entitlement;
	String errorLogName = "DAS-SNOWTest-SHelper";

	public ServiceNowHelper() throws GeneralException {
		this.context = SailPointFactory.getCurrentContext();
	}

	/**
	 * This methods writes back to the csv files of Delimited application
	 *
	 * @param appName,     application that is part of the AccountRequest
	 * @param writeParams, Map object that is passed from the caller providing the
	 *                     data to be written into the RITM
	 */
	public void writeToFile(String appName, Map writeParams) {
		String methodName = "writeToFile(String appName, Map writeParams)";
		Common.info(LOG_NAME, "Entering " + methodName);
		try {
			if (StringUtils.isBlank(appName)) {
				Common.info(LOG_NAME, methodName + " Application Name is required");
				throw new RuntimeException(LOG_NAME + ": " + methodName + " Application Name is required");
			}
			if (null == writeParams || writeParams.isEmpty()) {
				Common.info(LOG_NAME, methodName + " Parameters to Write are required");
				throw new RuntimeException(LOG_NAME + ": " + methodName + " Parameters to Write are required");
			}
			Application application = context.getObjectByName(Application.class, appName);
			if (null == application) {
				Common.info(LOG_NAME, methodName + " Application Object is required");
				throw new RuntimeException(LOG_NAME + ": " + methodName + " Application Object is required");
			}
			String fileName = application.getStringAttributeValue("file");
			if (StringUtils.isBlank(fileName)) {
				Common.info(LOG_NAME, methodName + " File Name is required");
				throw new RuntimeException(LOG_NAME + ": " + methodName + " File Name is required");
			}
			// open file in read-write mode
			RandomAccessFile randomAccessFile;
			randomAccessFile = new RandomAccessFile(new File(fileName), "rw");
			FileChannel channel = randomAccessFile.getChannel();
			// get lock
			FileLock tryLock = channel.lock();
			Object userName = writeParams.get("Username");
			String firstName = writeParams.get("Firstname") == null ? "" : writeParams.get("Firstname").toString();
			String lastName = writeParams.get("Lastname") == null ? "" : writeParams.get("Lastname").toString();
			String employeeNumber = writeParams.get("EmployeeNumber") == null ? ""
					: writeParams.get("EmployeeNumber").toString();
			String emailAddress = writeParams.get("EmailAddress") == null ? ""
					: writeParams.get("EmailAddress").toString();
			String department = writeParams.get("Department") == null ? "" : writeParams.get("Department").toString();
			String role = writeParams.get("Role") == null ? "" : writeParams.get("Role").toString();
			String dataStr = "\n" + userName + "," + firstName + "," + lastName + "," + employeeNumber + ","
					+ emailAddress + "," + department + "," + role;
			// write to end of file
			randomAccessFile.seek(randomAccessFile.length());
			randomAccessFile.write(dataStr.getBytes());
			// release lock
			tryLock.release();
			// close file
			randomAccessFile.close();

		} catch (Exception e) {
			Common.error(LOG_NAME, "Error in " + methodName + " " + e, e);
		}
		Common.info(LOG_NAME, "Exiting " + methodName);
	}

	/**
	 * This method builds Request object comments given a IdentityRequestId
	 *
	 * @param idReqId
	 * @return requester comments
	 */
	@SuppressWarnings("unchecked")
	public String getRequesterCommnets(String idReqId) {
		String methodName = "getRequesterCommnets(String identityRequestId)";
		Common.info(LOG_NAME, "Entering " + methodName + " IdentityRequest ID: " + idReqId);
		String comment = "";
		Map<String, String> map = new ConcurrentHashMap<>();
		try {
			IdentityRequest idReqObj = context.getObjectByName(IdentityRequest.class, idReqId);
			if (idReqObj == null) {
				Common.info(LOG_NAME, "IdentityRequest Object is Null:" + idReqId);
				throw new RuntimeException(LOG_NAME + " IdentityRequest Object is Null: " + idReqId);
			}
			if (idReqObj.getItems() == null) {
				Common.info(LOG_NAME, "No Items in IdentityRequest Object: " + idReqId);
				throw new RuntimeException(LOG_NAME + " No Items in IdentityRequest Object: " + idReqId);
			}
			List<IdentityRequestItem> itemsList = idReqObj.getItems();
			for (IdentityRequestItem item : itemsList) {
				if (Util.isNotNullOrEmpty(item.getRequesterComments()) && Util.nullSafeCaseInsensitiveEq(
						"IdentityIQ for ServiceNow Service Desk", item.getProvisioningEngine())) {
					map.put(item.getValue().toString(), item.getRequesterComments());
				}
			}
			if (!Util.isEmpty(map)) {
				Collection values = map.values();
				if (!Util.isEmpty(values)) {
					List valueList = new ArrayList(values);
					HashSet<Object> set = new HashSet(valueList);
					// Util.removeDuplicates(valueList);
					if (null != valueList) {
						// comment = valueList.toString().substring(1, valueList.toString().length() -
						// 1);
						comment = set.toString().substring(1, set.toString().length() - 1);
					}
				}
			}
		} catch (Exception ex) {
			Common.error(LOG_NAME, "Exception in getRequesterCommnets " + ex.getMessage());
		}
		Common.trace(LOG_NAME, "Exiting " + methodName);
		return comment;
	}

	/**
	 * Adds an SNOW Assignment group in Plan
	 *
	 * @param plan
	 * @param reqName
	 * @return ProvisioningPlan including assignment groups
	 */

	public ProvisioningPlan addAssignmentGroupInPlan(ProvisioningPlan plan) {
		String methodName = "addAssignmentGroupInPlan(..)";

		Common.info(LOG_NAME, "Entering ");
		List<AccountRequest> modifiedAccountRequest = new CopyOnWriteArrayList<>();
		//GeneralUtils.createCustomErrors(errorLogName, "Entering Try", "Entering Try");
		try {
			if (plan != null) {
				// Get Account Request... Account Request is always per application
				List<AccountRequest> accountRequests = new CopyOnWriteArrayList<>();
				accountRequests = plan.getAccountRequests();
				if (null != accountRequests) {
					// Process Account Request
					
					for (AccountRequest request : accountRequests) {
						String businessApplication = null;
						String assignmentGroup = null;
						List<AccountRequest> localARList = new CopyOnWriteArrayList<>();
						//GeneralUtils.createCustomErrors(errorLogName, "ADD-ASSG-GROUP-2", request.toXml());
						// AR Level Declarations
						String appName = request.getApplicationName();
						// List of IdentityIQ Apps
						List<String> appList = Arrays.asList("IIQ", "IdentityIQ");
						// Parse AttributeRequest from request
						List<AttributeRequest> attributeRequests = new CopyOnWriteArrayList<>();
						attributeRequests = request.getAttributeRequests();
						for (AttributeRequest attrReq : attributeRequests) {

							String attribName = attrReq.getName();
							Object attribVal = attrReq.getValue();
							if (!StringUtils.isBlank(GeneralUtils.OTOS(attribVal))) {
								// Get Assignment Groups for Entitlements
								if (StringUtils.isBlank(assignmentGroup) && appName != null
										&& !appList.contains(appName)) {
									List<Map<String, Object>> entlAttribs = new ArrayList<>();
									try {
										entlAttribs = QueryUtils.getManagedAttributes(appName, attribName, attribVal);
										if (!StringUtils.isBlank(entlAttribs.toString()) || !entlAttribs.isEmpty()  || null != entlAttribs) {
											//GeneralUtils.createCustomErrors(errorLogName, "ENT-ATTRIBS", entlAttribs);
											for (Map m : entlAttribs) {
												assignmentGroup = GeneralUtils.OTOS(m.get("snowAssignmentGroup"));
												businessApplication = GeneralUtils.OTOS(m.get("ascBusinessApplication"));
											}
										}
									} catch (Exception el) {
										//GeneralUtils.createCustomErrors(errorLogName, "ENTL-ATTRIBS-ERROR",entlAttribs);
									}
								} 	/* Get Assignment Group from Role*/ 
								    else if (StringUtils.isBlank(assignmentGroup) && appList.contains(appName)
										&& "assignedRoles".equals(attribName)) {
									List<Map<String, Object>> attrMap = new ArrayList<>();
									try {
										attrMap = QueryUtils.getBundleAttributes(attribVal);
										if (null != attrMap) {
											for (Map m : attrMap) {
												//GeneralUtils.createCustomErrors(errorLogName, "ROLE-ATTRIBS", attrMap);
												assignmentGroup = GeneralUtils.OTOS(m.get("snowAssignmentGroup"));
											}
										}
									} catch (Exception e) {
										//GeneralUtils.createCustomErrors(errorLogName, "BUNDLE-ATTRIB-ERROR", e);
									}
								} else if	/* Get Assignment Group From Application*/
								(StringUtils.isBlank(assignmentGroup) && appName != null
										&& !appList.contains(appName)) {
									Map<String, Object> arMap = new HashMap<>();
									try {
										arMap = QueryUtils.getAttributeValue(Application.class, appName);
										//GeneralUtils.createCustomErrors(errorLogName, "APP-ATTRIBS", arMap);
									} catch (Exception e) {
										//GeneralUtils.createCustomErrors(errorLogName, "APP-ATTRIB-ERROR", e);
									}
									assignmentGroup = GeneralUtils.OTOS(arMap.get("snowAssignmentGroup"));
									businessApplication = GeneralUtils.OTOS(arMap.get("ascBusinessApplication"));
								}
							}
						}
						// Set Default Assignment Group if assignmentGroup is blank
						if (StringUtils.isBlank(assignmentGroup)) {
							assignmentGroup = "cac4f0384722a2507a5f8edf016d436a";
						}
						request.add(new AttributeRequest("assignment_group", ProvisioningPlan.Operation.Set,
								assignmentGroup));
						if (Util.isNotNullOrEmpty(businessApplication)) {
							request.add(new AttributeRequest("businessApplication", ProvisioningPlan.Operation.Set,
									businessApplication));
						}

						localARList.add(request);
						modifiedAccountRequest.addAll(localARList);
					}
				}
			}

			plan.setAccountRequests(modifiedAccountRequest);

		} catch (Exception e) {
			//GeneralUtils.createCustomErrors(errorLogName, "ADD-ASSG-GROUP-ERROR", e.getMessage() + " " + e);
			Common.error(LOG_NAME, "ERROR: Failed to create Plan for ServiceNow Request: " + e, e);
			throw new RuntimeException(LOG_NAME + " Failed to create Plan for ServiceNow Request: " + e, e);
		}
		Common.debug(LOG_NAME, "Exiting ");
		return plan;
	}

	/**
	 * Parses Attribute request from Plan
	 *
	 * @param accountReq
	 * @param attrName
	 * @return String representation of AttributeRequest
	 */
	public String getAttributeRequest(AccountRequest accountReq, String attrName) {
		Common.error(LOG_NAME, "Entering ");
		String result = "N/A";
		AttributeRequest attrReq = accountReq.getAttributeRequest(attrName);
		if (attrReq != null) {
			if (attrReq.getValue() instanceof String) {
				result = GeneralUtils.OTOS(attrReq.getValue());
			}
		}
		Common.error(LOG_NAME, "Existing");
		return result;
	}

	/**
	 * Returns a list representation of Attribute Requests for Roles Request
	 *
	 * @param accountReq
	 * @param attrName
	 * @return List of Attribute Requests
	 */
	public List<String> getRoleAttributeRequests(AccountRequest accountReq, String attrName) {
		String methodName = "getRoleAttributeRequests(AccountRequest accountReq, String attrName)";
		Common.error(LOG_NAME, "Entering " + methodName);
		List<String> results = new ArrayList<>();
		List<AttributeRequest> attrReqList = accountReq.getAttributeRequests(attrName);
		if (attrReqList != null && attrReqList.size() > 0) {
			for (AttributeRequest attrReq : attrReqList) {
				results.add(GeneralUtils.OTOS(attrReq.getValue()));
			}
		}
		if (results.size() > 0) {
			results.add("N/A");
		}
		Common.error(LOG_NAME, "Exiting " + methodName);
		return results;
	}

	/**
	 * Returns list of Attribut Requests extracted from Plan, Used to but a final
	 * plan.
	 *
	 * @param accountReq
	 * @param attrName
	 * @return
	 */
	public List<String> getAttributeRequestList(AccountRequest accountReq, String attrName) {
		String methodName = "getAttributeRequestList(AccountRequest accountReq, String attrName)";
		Common.error(LOG_NAME, "Entering :" + methodName);
		List<String> result = new ArrayList<>();
		try {
			AttributeRequest attrReq = accountReq.getAttributeRequest(attrName);
			if (attrReq != null) {
				if (attrReq.getValue() instanceof List) {
					result.addAll((List<String>) attrReq.getValue());
				} else if (attrReq.getValue() instanceof String) {
					result.add((String) attrReq.getValue());
				}
			}
		} catch (Exception e) {
			Common.error(LOG_NAME, "ERROR: " + methodName + " : " + e, e);
		}
		Common.error(LOG_NAME, "Exiting :" + methodName);
		return result;
	}

	/**
	 * Returns Manager of the Identity
	 *
	 * @param identity
	 * @param attribute
	 * @return String representation of Manager on the Identity cube
	 */
	public String getIdentityManager(Identity identity, String attribute) {
		Identity maId = identity.getManager();
		if (null != maId && attribute != null) {
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
	 *
	 * @param plan
	 * @param comment
	 * @param identity
	 * @param arguments
	 * @return Formatted final Plan
	 */
	public ProvisioningPlan createRITMDisplay(ProvisioningPlan plan, String comment, Identity identity,
			Map<String, Object> arguments) {
		Common.error(LOG_NAME, "Entering ");

		StringBuffer buffer = null;
		Map<String, String> configMap = new ConcurrentHashMap<>();
		String shortDesc = null;
		Map<String, String> orderMap = null;
		String custObjectName = "ASCN-ServiceNow-RITM-Display";
		String appName = "";
		List<AccountRequest> modifiedAccountRequest = new CopyOnWriteArrayList<>();
		try {
			Common.info(LOG_NAME, "plan in try " + plan.toXml());
			Common.info(LOG_NAME, "identity in try " + identity.getName());
			if (plan != null && identity != null) {
				List<AccountRequest> accountRequests = plan.getAccountRequests();

				if (!Util.isEmpty(accountRequests)) {
					for (AccountRequest account : accountRequests) {

						buffer = new StringBuffer();
						appName = account.getApplicationName();
						shortDesc = "AAM (SailPoint) Request for " + identity.getLastname() + ", "
								+ identity.getFirstname() + " on Application: " + appName;
						if (appName == null) {
							Common.debug(LOG_NAME, " No Application Name, Application Name required!");
						}
						configMap = GeneralUtils.loadCustomMap(appName, custObjectName);
						if (configMap == null || configMap.isEmpty()) {
							configMap = GeneralUtils.loadAPIConfiguration("default", custObjectName);
						}
						// Formatting
						if (null != configMap) {
							Map<String, Map<String, String>> map2 = new ConcurrentHashMap<>();
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
									Map<String, String> innerMap = new ConcurrentHashMap<>();
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
									buffer.append(
											m.getKey() + identity.getAttribute(m.getValue().split("#")[1]) + "\n");
								} else if (m.getValue().startsWith("identityManager#")) {
									buffer.append(m.getKey() + getIdentityManager(identity, m.getValue().split("#")[1])
											+ "\n");
								} else if (m.getValue().startsWith("arguments#")) {
									buffer.append(
											m.getKey() + arguments.get(m.getValue().toString().split("#")[1]) + "\n");
								} else if (m.getValue().split("#")[0].equalsIgnoreCase("getAttributeRequest")) {
									buffer.append(m.getKey() + getAttributeRequest(account, m.getValue().split("#")[1])
											+ "\n\n");

								} else if (m.getValue().startsWith("accountReq#")) {
									if ((m.getValue().split("#")[1]).equalsIgnoreCase("applicationName")) {
										buffer.append(m.getKey() + account.getApplicationName() + "\n");
									} else if ((m.getValue().split("#")[1]).equalsIgnoreCase("nativeIdentity")) {
										buffer.append(m.getKey() + account.getNativeIdentity() + "\n");
									}
								} else if (m.getValue().toString().split("#")[0]
										.equalsIgnoreCase("getAttributeRequestList")) {
									List<String> faciltyAccList = getAttributeRequestList(account,
											m.getValue().split("#")[1]);
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
									List<String> roleRequestList = getRoleAttributeRequests(account,
											m.getValue().toString().split("#")[1]);

									if (roleRequestList.size() > 0) {
										for (String entValue : roleRequestList) {
											buffer.append(entValue + "\n\n");
										}
									} else {
										buffer.append("N/A" + "\n\n");
									}
								} else if (m.getValue().toString().startsWith("roleList#")) {
									List<AttributeRequest> getAttrRequests = account.getAttributeRequests();
									if (null != getAttrRequests) {
										for (AttributeRequest attrRequest : getAttrRequests) {
											String[] roles = (m.getValue().toString().split("#")[1]).split("\\|");
											List<String> roleList = Arrays.asList(roles);
											if (roleList.contains(attrRequest.getValue().toString())) {
												buffer.append(
														attrRequest.getOperation().toString() + " " + attrRequest.getName()
																+ " : " + attrRequest.getValue().toString() + "\n");
												buffer.append("add My User Experience- Nursing Group" + "\n");
											} else {
												buffer.append(
														attrRequest.getOperation().toString() + " " + attrRequest.getName()
																+ " : " + attrRequest.getValue().toString() + "\n");
											}
										}
									}
								} else {
									buffer.append(m.getKey().toString() + m.getValue() + "\n");
								}
							}
						}

						account.put("requestDescription", buffer.toString());
						account.put("shortDesc", shortDesc);
						modifiedAccountRequest.add(account);

					}
				}
			} else {
				Common.debug(LOG_NAME,
						"NULL method createRITMDisplay :: plan " + plan.toXml() + " :: identity: " + identity);
			}
		} catch (Exception ex) {
			Common.error(LOG_NAME, "Exception in createRITMDisplay " + ex, ex);
		}
		Common.info(LOG_NAME, "Exiting");
		// List<AccountRequest> newAccountRequests = new ArrayList<>(new
		// HashSet<>(modifiedAccountRequest));
		plan.setAccountRequests(modifiedAccountRequest);
		plan.setIdentity(identity);
		return plan;
	}

	public boolean hasPreviousPendingRequest(Identity identity, String appName, String attribName, Object attribValue,
			Object op) {
		String methodName = "hasPreviousPendingRequest(Identity identity, String appName,String attribName, String attribValue, String op)";
		Common.error(LOG_NAME, "Entering :" + methodName);
		boolean flag = false;
		List<IdentityRequest> identityRequests = null;
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			QueryOptions ops = new QueryOptions();
			ops.add(Filter.eq("IdentityRequest.type", "AccessRequest"));
			ops.add(Filter.eq("IdentityRequest.targetId", identity.getId()));
			ops.add(new Filter[] { Filter.or(Filter.eq("IdentityRequest.completionStatus", "Pending"),
					Filter.eq("IdentityRequest.completionStatus", "Incomplete")) });

			identityRequests = context.getObjects(IdentityRequest.class, ops);
			if (identityRequests != null) {
				for (Iterator i$ = identityRequests.iterator(); i$.hasNext();) {
					IdentityRequest request = (IdentityRequest) i$.next();
					List<IdentityRequestItem> reqItems = request.getItems();
					for (IdentityRequestItem reqItem : reqItems) {
						ProvisioningPlan provisionplan = reqItem.getProvisioningPlan();
						if (provisionplan != null) {
							for (AccountRequest acReq : provisionplan.getAccountRequests()) {

								String targetIntegration = acReq.getTargetIntegration();
								if (targetIntegration != null
										&& targetIntegration.equals("IdentityIQ for ServiceNow Service Desk")) {
									Common.info(LOG_NAME, targetIntegration);
									String accAppName = acReq.getApplication();
									/*
									 * String accRoleName = acReq.get(roleName) != null ?
									 * acReq.get(roleName).toString() : null; Common.info(LOG_NAME,accAppName);
									 * Common.info(LOG_NAME,accRoleName+" roleName : "+roleName);
									 * if(accRoleName!=null && roleName!=null &&
									 * accRoleName.equalsIgnoreCase(roleName)){ flag = true; break; }
									 */
									if (acReq.getAttributeRequests() != null) {
										for (AttributeRequest atReq : acReq.getAttributeRequests()) {
											Object entitlement = atReq.getValue();
											Object operation = atReq.getOp();
											String atrName = atReq.getName() != null ? atReq.getName().toString()
													: null;
											Common.info(LOG_NAME,
													acReq.getApplication() + " : entitlement : " + entitlement);
											if (accAppName != null && accAppName.equals(appName) && attribValue != null
													&& (attribValue).equals(entitlement) && null != operation
													&& operation.equals(op) && null != atrName
													&& atrName.equalsIgnoreCase(attribName)) {
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
			Common.error(LOG_NAME, "Error :" + methodName + " :" + e, e);
		}
		Common.error(LOG_NAME, "Exiting :" + methodName);
		return flag;
	}

	/**
	 * This method addresses the issue of Zmobie tickets. Evaluates if there is a
	 * pending Access Request and skips the transaction during Indetity Request
	 * opeartion
	 *
	 * @param identityId
	 * @return boolean
	 */
	public boolean hasPreviousPendingRequest(String identityId) {
		String methodName = "hasPreviousPendingRequest(String identityId)";
		Common.error(LOG_NAME, "Entering :" + methodName);
		boolean flag = false;
		List<IdentityRequest> identityRequests = null;
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			QueryOptions ops = new QueryOptions();
			ops.add(Filter.eq("IdentityRequest.type", "AccessRequest"));
			ops.add(Filter.eq("IdentityRequest.targetId", identityId));
			ops.add(new Filter[] { Filter.or(Filter.eq("IdentityRequest.completionStatus", "Pending"),
					Filter.eq("IdentityRequest.completionStatus", "Incomplete")) });

			identityRequests = context.getObjects(IdentityRequest.class, ops);

			if (identityRequests != null) {
				flag = true;
			}
		} catch (Exception e) {
			Common.error(LOG_NAME, "Error :" + methodName + " :" + e, e);
		}
		Common.error(LOG_NAME, "Exiting :" + methodName);
		return flag;
	}

	private String fetchSnowAssignedGroup(String appName, String entValue) throws GeneralException {
		Filter filter = Filter
				.ignoreCase(Filter.and(Filter.eq("application.name", appName), Filter.eq("value", entValue)));
		ManagedAttribute mgdAttribute = context.getUniqueObject(ManagedAttribute.class, filter);
		String snowGroupMgdAttrName = mgdAttribute.getAttribute("snowAssignmentGroup") != null
				? mgdAttribute.getAttribute("snowAssignmentGroup").toString()
				: null;
		return snowGroupMgdAttrName;
	}

	public String checkProvisioningRequestExist(String identityName, String accountName, String appName,
			List<ProvisioningPlan.AttributeRequest> attributeRequest) {
		String methodName = "getProvRequest(String identityName)";
		String targetIntegration = "IdentityIQ for ServiceNow Service Desk";
		Identity identity = null;
		Filter f = null;
		String returnStr = null;
		try {
			SailPointContext context = SailPointFactory.getCurrentContext();
			identity = context.getObjectByName(Identity.class, identityName);
			if (identity != null) {
				f = Filter.and(Filter.eq("target", targetIntegration), Filter.eq("identity.id", identity.getId()));
				QueryOptions qo = new QueryOptions();
				qo.addFilter(f);
				qo.setCloneResults(true);
				List<ProvisioningRequest> pr = context.getObjects(ProvisioningRequest.class, qo);
				ProvisioningPlan plan = pr.get(0).getPlan();
				if (plan != null) {
					for (AccountRequest acr : plan.getAccountRequests()) {

						String actTargetIntegration = acr.getTargetIntegration();
						String provStatus = acr.getResult().getStatus();
						String nativeIdentity = acr.getNativeIdentity();
						if (actTargetIntegration != null && targetIntegration.equals(actTargetIntegration)) {
							if (acr.getAttributeRequests() != null) {
								if (attributeRequest.equals(acr.getAttributeRequests())) {
									returnStr = acr.getResult().getRequestID();
									break;
								}

								/*for (AttributeRequest atr : acr.getAttributeRequests()) {

									if (attribName.equals(atr.getName()) && attribVal.equals(atr.getValue())
											&& appName.equals(acr.getApplication())
											&& accountName.equals(nativeIdentity) && "queued".equals(provStatus)) {
										returnStr = acr.getResult().getRequestID();
										break;
									}

								}*/
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Common.error(LOG_NAME, " Error in :" + methodName + " " + e, e);
		}
		return returnStr;

	}

	/**
	 * This methods validates and modifies the original plan to filter out all account requests with un-met
	 * conditions
	 * @param plan ProvisioningPlan
	 * @return modified plan
	 */
	public ProvisioningPlan validatedPlan(ProvisioningPlan plan, Identity identity) {
		String errorLogName = "DAS-SNOWTest-SHelper";
		//GeneralUtils.createCustomErrors(errorLogName,"Entering","validatedPlan");
		List<AccountRequest> listOfAR = new ArrayList<>();
		String idName = null;
		boolean process = true;
		String source = "IdentityIQ for ServiceNow Service Desk";
		ProvisioningPlan newPlan = new ProvisioningPlan();
		if(plan == null) {
			throw new RuntimeException("Plan is null, Can not create ServiceNow Request without valid ProvisioningPlan");
		}
		if(identity == null) {
			throw new RuntimeException("Identity is required, Can not create ServiceNow Request without valid Identity");
		}
		idName = identity.getName();
		boolean inactive = identity.isInactive();
		Object niObj = identity.getAttribute("samAccountName");
		String networkId = niObj != null ? niObj.toString() : null;

		for (AccountRequest acr : plan.getAccountRequests()) {

			Object operation = acr.getOperation();
			String appName = acr.getApplicationName();
			//Do not process Entitlement add if NetworkId is blank or null or
    		//Identity is inactive or network Id is blank on entitlementAdd operation
			if(StringUtils.isBlank(networkId) || inactive || checkRoleAssigned(identity)) {
		   		 process = false;
		   		 break;
		    }
			if(acr.getAttributeRequests()!=null){
				//if(null != atr.getName() && null != atr.getValue()) {
          		  //Check Provisioning Request already Exists
          		  String reqNumber = checkProvisioningRequestExist(idName,acr.getNativeIdentity(),acr.getApplicationName(),acr.getAttributeRequests());
          		  //GeneralUtils.createCustomErrors(errorLogName,"checkProvisioningRequestExist","validatedPlan");
          		  if (!StringUtils.isBlank(reqNumber)) {
          			  //GeneralUtils.createCustomErrors(errorLogName,"checkProvisioningRequestExist",reqNumber);
          			  Map<String,String> m = new HashMap<>();
          			  m.put("string1", "MATCH = "+GeneralUtils.OTOS(acr.getAttributeRequests()));
          			  GeneralUtils.logAuditEvent(source, idName, appName, acr.getNativeIdentity(), m);
          			  plan.remove(acr);
          			  process = false;
          			  break;
          		  }
          		  //Check if Request Item Exists

               // }
            	  for(AttributeRequest atr : acr.getAttributeRequests()){

                	 if(hasPreviousPendingRequest(identity, appName,atr.getName(), atr.getValue(), atr.getOp())) {
           			 // GeneralUtils.createCustomErrors(errorLogName,"hasPreviousPendingRequest",atr.getName()+" "+atr.getValue()+" "+atr.getOp());
           			  Map<String,String> m = new HashMap<>();
           			  m.put("string1", GeneralUtils.OTOS(atr.getValue()));
           			  GeneralUtils.logAuditEvent(source, idName, appName, acr.getNativeIdentity(), m);
           			  plan.remove(acr);
           			  process = false;
           			  break;
           		  }

                  }

              }

			//Add Audit event to log removed account requests.
    		if (process) {
    			listOfAR.add(acr);
    		}
        }
		 plan.setAccountRequests(listOfAR);
		 return plan;
	}

	public boolean checkRoleAssigned(Identity identity){
		String customObjName = "ASCN-NoDuplicate-RoleREQCreation";
		Map<?, ?> customMap = GeneralUtils.loadCustomMap("ExcludeDuplicates",customObjName);
		boolean outVale = false;

		List<Bundle> roles = identity.getAssignedRoles();
		for(Bundle b : roles) {

			String roleName = b.getName();
			Map<?, ?> outMap = QueryUtils.getEntitlementsFromBusinessBundle(roleName);
			if(customMap.containsKey(roleName) || outMap.containsKey(customMap.get(roleName))) {
			 outVale = true;
			}
		}
		return outVale;
	}

}