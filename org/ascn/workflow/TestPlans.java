package org.ascn.workflow;
import java.util.ArrayList;
import java.util.List;

import org.ascn.workflow.*;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.object.Identity;


public class TestPlans {

	public TestPlans() {}
	private Identity identity;
	private List<String> groupDN;
	private String accountId;
	private String adAppName;
	private String maAttribName;
	
	public void getPlans() {
		 ProvisioningPlan plan = new ProvisioningPlan();
		  plan.setIdentity(identity);
		  //Set AD Entitlement Request's Account Object
		  List<AccountRequest> acctReqs = new ArrayList<>();
		  AccountRequest acctReq = new AccountRequest();
		  acctReq.setOperation(ProvisioningPlan.AccountRequest.Operation.Modify);
		  acctReq.setApplication(adAppName);
		  acctReq.setNativeIdentity(accountId);
		  //Set Attribute Request
		  acctReq.add(new AttributeRequest(maAttribName,ProvisioningPlan.Operation.Add,groupDN));
		  acctReqs.add(acctReq);
		  plan.setAccountRequests(acctReqs); 
		    
	}
}
