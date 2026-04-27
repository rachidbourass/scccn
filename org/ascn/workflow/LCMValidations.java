package org.ascn.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ascn.utils.ASCNStringUtils;
import org.ascn.utils.IIQObjectUtils;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Application;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class LCMValidations {
	
	
	private SailPointContext context;
	private HashMap<String, Object>  messageMap;
	private List<String> messageList;
	public LCMValidations() throws GeneralException {
		messageMap = new HashMap<>();
		messageList = new ArrayList<>();
	}
	private List<AccountRequest> getAccountRequestsToRemove(List<AccountRequest> acctReqs, List<String> appNames, String identityName) throws Exception {
		this.context = SailPointFactory.getCurrentContext();
		List<AccountRequest> ars = new ArrayList<>();
		for(AccountRequest ar : acctReqs){   
			List<AttributeRequest>  atrReqs = ar.getAttributeRequests();
			String nativeIdentity = ar.getNativeIdentity();
			String appName = ar.getApplicationName();
			Application app = context.getObjectByName(Application.class,appName);
			if(null != app && "Active Directory".startsWith(app.getType()) && (Util.nullSafeCaseInsensitiveEq(ar.getOperation().toString(),"Modify") || Util.nullSafeCaseInsensitiveEq(ar.getOperation().toString(),"Create"))) {
				for(AttributeRequest atr : atrReqs) {
					
				  List<Map<String,Object>> attrMap = IIQObjectUtils.getManagedAttributes(appName, atr.getName(), atr.getValue());
				    String accountRootDN = ASCNStringUtils.getDNSubString(nativeIdentity, "DC=", 1);
				    String entitlementRootDN = ASCNStringUtils.getDNSubString(atr.getValue().toString(), "DC=", 1);
					String accountForestDN = ASCNStringUtils.getDNSubString(nativeIdentity, "DC=", 2);
					String entitlementForestDN = ASCNStringUtils.getDNSubString(atr.getValue().toString(), "DC=", 2);
					String groupScope = null;
					for(Map<String,Object> m : attrMap){
						groupScope = m.get("GroupScope") != null ? m.get("GroupScope").toString() : null;
					  }
					if(Util.nullSafeCaseInsensitiveEq("Global",groupScope) && !Util.nullSafeCaseInsensitiveEq(accountRootDN,entitlementRootDN)) {
						ars.add(ar);
						messageList.add("ERROR: Group Scope is: "+ groupScope +" and Identity Name: "+identityName+": User Account ROOT DN is not same as Requested Entitlement's Root DN: ");
					}
					if(Util.nullSafeCaseInsensitiveEq("Universal",groupScope) || Util.nullSafeCaseInsensitiveEq("Domain Local",groupScope)) {
						if(!checkForestDomainMatch(accountForestDN,entitlementForestDN)) {
							ars.add(ar);	
							messageList.add("ERROR: Group Scope is: "+ groupScope +" and Identity Name: "+identityName+": User Account Forest DN is not same as Requested Entitlement's Forest DN: ");
						}
					}
				}
				    
			}
			else if("GSuite".equalsIgnoreCase(appName) && appNames.contains("GSuite") && ar.getOperation().toString().equalsIgnoreCase("Create")) {
				 messageList.add("ERROR: "+identityName+" already has a GSuite account, Account Request removed from plan");
				 ars.add(ar);
				
			}
		}
		messageMap.put("Error", messageList);
		return ars;
		
	}
	
	public boolean checkForestDomainMatch(String accountForestDN,String entitlementForestDN) {
		boolean flag = true;
		if(!accountForestDN.equalsIgnoreCase(entitlementForestDN)){
			flag = false;
		}
			    
		return flag;
	}
	
	public Map<String,Object> getMessageMap() {
		return messageMap;
	}
	 
	public List<AttributeRequest> getAttributeRequestsToRemove(AccountRequest ar, String identityName) throws Exception {
		this.context = SailPointFactory.getCurrentContext();
		List<AttributeRequest>  atrReqs = ar.getAttributeRequests();
		List<AttributeRequest>  atrReqsToRemove = new ArrayList<>();
		String nativeIdentity = ar.getNativeIdentity();
		String appName = ar.getApplicationName();
		Application app = context.getObjectByName(Application.class,appName);
		//Remove AD Group Requests based on groupScope logic
		if(null != app && "Active Directory".startsWith(app.getType()) && Util.nullSafeCaseInsensitiveEq(ar.getOperation().toString(),"Modify")) {
				for(AttributeRequest atr : atrReqs) {
					List<Map<String,Object>> attrMap = IIQObjectUtils.getManagedAttributes(appName, atr.getName(), atr.getValue());
				    String accountRootDN = ASCNStringUtils.getDNSubString(nativeIdentity, "DC=", 1);
				    String entitlementRootDN = ASCNStringUtils.getDNSubString(atr.getValue().toString(), "DC=", 1);
					String accountForestDN = ASCNStringUtils.getDNSubString(nativeIdentity, "DC=", 2);
					String entitlementForestDN = ASCNStringUtils.getDNSubString(atr.getValue().toString(), "DC=", 2);
					String groupScope = null;
					for(Map<String,Object> m : attrMap){
						groupScope = m.get("GroupScope") != null ? m.get("GroupScope").toString() : null;
					  }
					if(Util.nullSafeCaseInsensitiveEq("Global",groupScope) && !Util.nullSafeCaseInsensitiveEq(accountRootDN,entitlementRootDN)) {
						atrReqsToRemove.add(atr);
						messageList.add("ERROR: Group Scope is: "+ groupScope +" and Identity Name: "+identityName+": User Account ROOT DN is not same as Requested Entitlement's Root DN, Removed AttributeRequest ");
					}
					if(Util.nullSafeCaseInsensitiveEq("Universal",groupScope) || Util.nullSafeCaseInsensitiveEq("Domain Local",groupScope)) {
						if(!checkForestDomainMatch(accountForestDN,entitlementForestDN)) {
							atrReqsToRemove.add(atr);
							messageList.add("ERROR: Group Scope is: "+ groupScope +" and Identity Name: "+identityName+": User Account Forest DN is not same as Requested Entitlement's Forest DN: ");
						}
					}
				    
		}
		}
		messageMap.put("Error", messageList);
        return atrReqsToRemove;
	}

}