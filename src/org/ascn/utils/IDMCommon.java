package org.ascn.utils;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sailpoint.api.ObjectUtil;
import sailpoint.api.SailPointContext;
import sailpoint.object.AuditEvent;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningPlan.AccountRequest;
import sailpoint.object.ProvisioningPlan.AttributeRequest;
import sailpoint.object.QueryOptions;
import sailpoint.server.Auditor;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class IDMCommon {

	private SailPointContext context;

	public IDMCommon(SailPointContext context) {
		this.context = context;
	}
	public List<Identity>  getNullAttributeIdentities(String attribName) throws GeneralException{
	    QueryOptions qo = new QueryOptions();
	    Filter filter = Filter.ignoreCase(Filter.and(Filter.isnull(attribName), Filter.eq("inactive",false), Filter.eq("protected",false)));
	    qo.addFilter(filter);
	    qo.setResultLimit(10);
	    List<Identity> idn = context.getObjects(Identity.class, qo);
	    return idn;
	}

	public List<Link>  getIdADLinks(Identity identity) throws GeneralException{
	    QueryOptions qo = new QueryOptions();
	    Filter filter = Filter.ignoreCase(Filter.and(Filter.eq("application.type", "Active Directory - Direct"), Filter.eq("identity.name",identity.getName())));
	    qo.addFilter(filter);
	    List links = context.getObjects(Link.class, qo);
	    return links;
	}
	public List<Link>  getIdentityAppLinks(Identity identity, String appName) throws GeneralException{
	    QueryOptions qo = new QueryOptions();
	    Filter filter = Filter.ignoreCase(Filter.and(Filter.eq("application.name", appName), Filter.eq("identity.name",identity.getName())));
	    qo.addFilter(filter);
	    List links = context.getObjects(Link.class, qo);
	    return links;
		}
	public List<String> getActiveIdentityNames(String matchStr) throws GeneralException{
	   List<String> idNameList = new ArrayList();
		 QueryOptions qo = new QueryOptions();
	   Filter filter = null;
	    if(null != matchStr){
	     	filter = Filter.ignoreCase(Filter.and(Filter.like("name", matchStr,Filter.MatchMode.START),Filter.eq("inactive", false)));
	    }else{
	      filter = Filter.ignoreCase(Filter.and(Filter.eq("inactive", false)));
	    }
		 qo.addFilter(filter);
		 Iterator<Identity> it = context.search(Identity.class, qo);
		 try {
				while (it.hasNext()) {
					Identity idn = it.next();
					if (null != idn) {
						idNameList.add(idn.getName());
					}
				}
		}catch (Exception ex) {
			//log.error("Exception while processing Link matches", ex);
		}finally {
			sailpoint.tools.Util.flushIterator(it);
		}
	  return idNameList;
	}
	public List<String> getActiveIdentityNamesByManager(String managerName) throws GeneralException{
	   List<String> idNameList = new ArrayList();
	  QueryOptions qo = new QueryOptions();
	   Filter filter = null;
	    if(null != managerName){
	     	filter = Filter.ignoreCase(Filter.and(Filter.eq("inactive", false),Filter.eq("manager.name",managerName)));
	    }
		qo.addFilter(filter);
		 Iterator<Identity> it = context.search(Identity.class, qo);
		 try {
				while (it.hasNext()) {
					Identity idn = it.next();
					if (null != idn) {
						idNameList.add(idn.getName());
					}
				}
		}catch (Exception ex) {
			//log.error("Exception while processing Link matches", ex);
		}finally {
			sailpoint.tools.Util.flushIterator(it);
		}
	  return idNameList;
	}

	  public List<String> getActiveIdentityNamesByIDAttribute(String attribName, String attribVal) throws GeneralException{
	   List<String> idNameList = new ArrayList();
		 QueryOptions qo = new QueryOptions();
	   Filter filter = null;
	    if(null != attribName && null != attribVal){
	     	filter = Filter.ignoreCase(Filter.and(Filter.eq("inactive", false),Filter.eq(attribName,attribVal)));
	    }
		qo.addFilter(filter);
		 Iterator<Identity> it = context.search(Identity.class, qo);
		 try {
				while (it.hasNext()) {
					Identity idn = it.next();
					if (null != idn) {
						idNameList.add(idn.getName());
					}
				}
		}catch (Exception ex) {
			//log.error("Exception while processing Link matches", ex);
		}finally {
			sailpoint.tools.Util.flushIterator(it);
		}
	  return idNameList;
	}

	public Map getLinkAttributes(Identity refIdentity, String samAccountName) throws GeneralException{
		List<Link> idnLinks = getIdADLinks(refIdentity);
		String idnEmail = refIdentity.getEmail();
		String mailInLink = null;
		Map<String,Object> outMap = new HashMap<>();
		outMap.put("targetConflict",false);
		if(null != idnLinks && !idnLinks.isEmpty()){
			for(Link l:idnLinks){

				String appName = l.getApplicationName();
				boolean isAIDApp = Util.nullSafeCaseInsensitiveEq("AD-AID",appName);
				if(isAIDApp && Util.nullSafeCaseInsensitiveEq(l.getAttribute("sAMAccountName").toString(),samAccountName)){
					outMap.put("displayName",l.getDisplayName());
					outMap.put("applicationName",l.getApplicationName());
					outMap.put("accountDN",l.getNativeIdentity());
					mailInLink = l.getAttribute("mail").toString();
					break;
				}else if(!isAIDApp && Util.nullSafeCaseInsensitiveEq(l.getAttribute("sAMAccountName").toString(),samAccountName)){
					outMap.put("displayName",l.getDisplayName());
					outMap.put("applicationName",l.getApplicationName());
					outMap.put("accountDN",l.getNativeIdentity());
					mailInLink = l.getAttribute("mail").toString();
				}
				if(Util.isNotNullOrEmpty(idnEmail) && Util.isNotNullOrEmpty(mailInLink) && !Util.nullSafeCaseInsensitiveEq(idnEmail, mailInLink)) {
					 //Email already set on Identity does not match mail currently set on target AD link
					 outMap.put("targetConflict",true);
				}else{
					outMap.put("targetConflict",false);
				}
			}
		}
		return outMap;
	  }

	   public boolean hasADLink(Identity refIdentity) throws GeneralException {
	    List<Link> idnLinks = getIdADLinks(refIdentity);
			boolean hasLink = true;
	    if(idnLinks.isEmpty()){
	      hasLink = false;
	    }
			return hasLink;
	  }

	  public boolean hasAIDLink(Identity refIdentity) throws GeneralException {
	    List<Link> idnLinks = getIdentityAppLinks(refIdentity, "AD-AID");
	   // return hasAppLink(identity, "AD-AID");
	    boolean hasLink = true;
	    if(idnLinks.isEmpty()){
	      hasLink = false;
	    }
	    return hasLink;
	  }
	  public Map getManagedAttributeMap(String appName,String entValue) throws GeneralException{
	    Map extEntMap = null;
	    Filter finalFilter=Filter.ignoreCase(Filter.and(Filter.eq("application.name",appName),Filter.eq("value",entValue)));
	    ManagedAttribute mgd=context.getUniqueObject(ManagedAttribute.class,finalFilter);
	    if(null != mgd){
	      extEntMap = new HashMap();
	      extEntMap = mgd.getAttributes();
	    }
	    return extEntMap;
	}


	  public String getAttributeValueFromPlan(ProvisioningPlan plan, String appName, String attributeName) {
			String attributeValue = null;
			if(null != plan) {

				List<AccountRequest> ar = plan.getAccountRequests(appName);
				if(!ar.isEmpty()) {
					for(AccountRequest a:ar) {
						AttributeRequest attribReq = a.getAttributeRequest(attributeName);
						attributeValue = attribReq == null ? null : attribReq.toString();

					}
				}

			}
			return attributeValue;
		}

	public long getDaysBetween(String inDate) throws ParseException {
	    Long ldate = Long.parseLong(inDate);
			LocalDateTime now = LocalDate.now().atStartOfDay();
			LocalDateTime createdDate =   Instant.ofEpochMilli(ldate).atZone(ZoneId.systemDefault()).toLocalDateTime();
			Duration duration = Duration.between(now, createdDate);
		  long diff = Math.abs(duration.toDays());
			return diff;
	}


		public int getDateBetween(int numDays, long l) throws ParseException {
			int diff = 0;
			LocalDate today = LocalDate.now();
			LocalDate pastDate = today.minusDays(numDays);
			LocalDate ld = Instant.ofEpochMilli(l).atZone(ZoneId.systemDefault()).toLocalDate();
	        if(ld.isAfter(pastDate)) {
	        	diff = ld.compareTo(today);

	        }
	        return diff;
		}

	public int getDaysBetween(Date date) {
	  /*
	  Usage:
	  if (getDaysBetween(date) >= 7) {//it has been 7 or more days since [Date]}
	  */
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime();
		calendar.setTime(date);
		return Util.getDaysDifference(currentDate, date);

	}

	public void logAuditEvent(String objName,String identityName,String appName,String acctNativeIdentity, Map inMap) throws GeneralException{
		AuditEvent auditEvent = new AuditEvent();
		auditEvent = new AuditEvent(objName, "Comment", identityName);
		auditEvent.setApplication(appName);
		auditEvent.setAccountName(acctNativeIdentity);
		auditEvent.setString1(inMap.get("string1")+identityName);
		auditEvent.setString2(inMap.get("string2").toString());
		auditEvent.setString3(inMap.get("string3").toString());
		auditEvent.setString4(inMap.get("string4").toString());
		//auditEvent.setAttributes(attrMap);
		Auditor.log(auditEvent);
		context.commitTransaction();
	}

	  public String approvalSchemeVal(Identity identity, Identity launcher) throws GeneralException {
		String approvalSchemeVal = "none";
		boolean trustedWorkgroupContainsLauncher = false;

		List<Identity> allWorkgroups = identity.getWorkgroups();
		List<Identity> trustedWorkgroups = new ArrayList<>();
		for(Identity workgroup : allWorkgroups) {
			//String stringValue = workgroup.getValue().toString();
			String stringValue = workgroup.getName().toString();
			if(stringValue.contains("Trusted")) {
				trustedWorkgroups.add(workgroup);
			}
		}

		List trustedWorkgroupMembers = new ArrayList();

		if(Util.isNotNullOrEmpty(launcher.toString())){
			for(Identity trustedWorkgroup : trustedWorkgroups) {
				if(trustedWorkgroup!=null){
					Iterator<Object[]> members = ObjectUtil.getWorkgroupMembers(context, trustedWorkgroup, null);
					while (members.hasNext()){
						Object[] object = members.next();
						Identity firstValue = (Identity) object[0];
						trustedWorkgroupMembers.add(firstValue.getName());
					}
				}
			}

			if(((!Util.isNullOrEmpty(launcher.getAttribute("Is Manager").toString())) && ((launcher.getAttribute("Is Manager")).toString().equalsIgnoreCase("Y"))) || trustedWorkgroupMembers.contains(launcher)){
				trustedWorkgroupContainsLauncher = true;
				approvalSchemeVal = "owner";
			}
		}
		if(!trustedWorkgroupContainsLauncher) {
			approvalSchemeVal = "manager,owner";
		}
		return approvalSchemeVal;
	}

	public ProvisioningPlan getEmptyPlan(ProvisioningPlan plan){
		List emptyAccounts = new ArrayList();
		List<AccountRequest> acctReqs = plan.getAccountRequests();
		for(AccountRequest ar : acctReqs){
		   String appName = ar.getApplicationName();
		   if("GSuite".equalsIgnoreCase(appName) && ar.getOperation().toString().equalsIgnoreCase("Create")){
				plan.remove(ar);
		   }
		}
		return plan;
	}
}
