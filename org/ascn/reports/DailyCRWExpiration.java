package org.ascn.reports;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ascn.objectutils.IdentityUtils;
import org.ascn.utils.DateUtils;
import org.ascn.utils.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

public class DailyCRWExpiration {
	private String accountExpire = "accountExpires";
	private SailPointContext context;
	private Logger log;
	public DailyCRWExpiration(String logLevel) {
    	this.context = SailPointFactory.peekCurrentContext();
    	log = new Logger(this.getClass());
		if(null == logLevel) {
			log.setLogLevel("info");
		}
    }
	public boolean checkADExpDateMatch(Object expDate, int rDays) throws GeneralException {
		boolean readyForExpiration = false;
		Object adExpDateValueObj = expDate;
		try {
			if (null != adExpDateValueObj) {
				if (adExpDateValueObj instanceof String) {
					if (!adExpDateValueObj.toString().equalsIgnoreCase("never")) {
						long diff = DateUtils.daysBetween(adExpDateValueObj.toString());
						if (diff > 0 && diff <= rDays) {
							readyForExpiration = true;
						}
					}
				}
				if (adExpDateValueObj instanceof Date) {
					long diff = DateUtils.daysBetween((Date) adExpDateValueObj);
					if (diff > 0 && diff <= rDays) {
						readyForExpiration = true;
					}
				}
			}
		} catch (Exception e) {
			 log.logMessage("ERROR IN checkADExpDateMatch: " + adExpDateValueObj + "---" + e, e);
			// throw new RuntimeException("ERROR IN checkADExpDateMatch: " + adExpDateValueObj + "---" + e, e);
		}
		return readyForExpiration;
	}
	private boolean isWithinRange(int diff, int rDays) {
		boolean out = false;
		if(diff > 0 && diff < rDays) {
			out = true;
		}
		return out;
	}

	public List<String> getFilteredADLinksForManager(String managerName, int expiringInDays, String idnTypes)
			throws GeneralException, ParseException {
		List<String> idList = new ArrayList<>();
		IdentityUtils iu = new IdentityUtils();
		List<String> inIdnType = null;
		try {
			if(null != idnTypes) {
				inIdnType = new ArrayList<>();
				String[] types = idnTypes.split(",");
				 for (String type : types) { 
					 inIdnType.add(type);
			     } 
			}
			QueryOptions qo = new QueryOptions();
			Filter f = Filter.ignoreCase(Filter.and(Filter.eq("manager.name", managerName),
					Filter.eq("inactive", false), Filter.in("type", inIdnType)));
			qo.addFilter(f);
			Iterator<Identity> iterator = context.search(Identity.class, qo);
			// ***Variable Declarations
			String displayName = null;
			String manager = null;
			String idName = null;
			String idEmail = null;
			String firstName = null;
			String lastName = null;
			String idnType = null;
			String contractorManager = null;
			String cmDisplayName = "Not Available";
			String cmExternalEmail = "Not Available";
			while (iterator.hasNext()) {
				Identity idn = (Identity) iterator.next();
				if (null != idn && !idn.isInactive()) {
					try {
						displayName = idn.getDisplayableName().replaceAll(",", "");
						manager = idn.getManager() != null
								? idn.getManager().getDisplayableName().replaceAll(",", "").toString()
								: null;
						idName = idn.getName();
						idEmail = idn.getEmail() != null ? idn.getEmail().toString() : "Not Available";
						firstName = idn.getFirstname() != null ? idn.getFirstname().toString() : "Not Available";
						lastName = idn.getLastname() != null ? idn.getLastname().toString() : "Not Available";
						idnType = idn.getType() != null ? StringUtils.capitalize(idn.getType().toString())
								: "Not Available";
						contractorManager = idn.getAttribute("contractorManager") != null
								? idn.getAttribute("contractorManager").toString()
								: null;
						cmDisplayName = "Not Available";
						cmExternalEmail = "Not Available";
						if (null != contractorManager) {
							Identity cmIdn = context.getObjectByName(Identity.class, contractorManager);
							cmDisplayName = cmIdn.getDisplayableName() != null
									? cmIdn.getDisplayableName().toString().replaceAll(",", "")
									: "Not Available";
							cmExternalEmail = cmIdn.getAttribute("externalEmail") != null
									? cmIdn.getAttribute("externalEmail").toString()
									: "Not Available";
						}
					} catch (Exception e) {
						log.logMessage("ERROR in Getting Identity Attributes: Manager Name: "+managerName+"  -- " + e, e);
						//throw new RuntimeException("ERROR in Getting Identity Attributes: Manager Name: "+managerName+"  -- " + e, e);
					}
					List<Link> links = iu.getIdFilteredActiveADLinks(idn);
					for (Link link : links) {

						Date today = new Date();
						String expirationDate = null;
						boolean containsRec = false;

						Object expDateObj = link.getAttribute(accountExpire);
						if (expDateObj != null) {
							List<String> expDateOptions = new ArrayList<>();
							expDateOptions.add("never");
							expDateOptions.add("");
							expDateOptions.add(" ");
							expDateOptions.add(null);
							if (!Util.nullSafeContains(expDateOptions, expDateObj)) {
								Date futureAccExpiration = null;
								try {
									futureAccExpiration = Util.stringToDate(expDateObj.toString());
									int diff =  Util.getDaysDifference(futureAccExpiration, today);
									containsRec = isWithinRange(diff,expiringInDays);
									expirationDate = Util.dateToString(futureAccExpiration, "MM/dd/yyyy");

								} catch (java.text.ParseException pe) {
									log.logMessage("Date Conversion Error: "+pe, pe);
								}
							}
							try {
								//containsRec = checkADExpDateMatch(expirationDate, expiringInDays);
								
							} catch (Exception e) {
								log.logMessage("ERROR: checkADExpDateMatch: "+managerName+"  -- " + e, e);
								//throw new RuntimeException("ERROR: checkADExpDateMatch: " + e, e);
							}
						
						String accountNameString = link.getDisplayName() != null ? link.getDisplayName().toString()
								: null;
						String domainName = null;
						String accountName = null;
						if (null != accountNameString) {
							domainName = accountNameString.split("\\\\")[0];
							accountName = link.getAttribute("sAMAccountName") != null ? link.getAttribute("sAMAccountName").toString() : null;
						}
						if (containsRec) {
							String reportData = idnType + "," + displayName + "," + idName + "," + firstName + ","
									+ lastName + "," + idEmail + "," + manager + "," + cmDisplayName + ","
									+ cmExternalEmail + "," + domainName + "," + accountName + "," + expirationDate;
							idList.add(reportData);
							
						}
						}
					}
				}
			}
		} catch (Exception e) {
			log.logMessage("ERROR: In getFilteredADLinksForManager: " + e, e);
			//throw new RuntimeException("ERROR: In getFilteredADLinksForManager: " + e, e);
		}
		return idList; 
	}

}
