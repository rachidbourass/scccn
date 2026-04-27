package org.ascn.apps;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.ascn.objectutils.IdentityUtils;
import org.ascn.utils.DateUtils;
import org.ascn.utils.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Filter;
import sailpoint.object.Form;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

public class ExtendCWRExpiration {
	private Logger log;
	private DateUtils adf;
	private SailPointContext context;
	private IdentityUtils iu;
	private List<String> idAttribList;
	public ExtendCWRExpiration() throws GeneralException {
		iu = new IdentityUtils();
	}
	public ExtendCWRExpiration(String logLevel) throws GeneralException {
		this.context = SailPointFactory.getCurrentContext();
		this.iu = new IdentityUtils();
		this.adf = new DateUtils();
		log = new Logger(this.getClass());
		if(null == logLevel) {
			log.setLogLevel("info");
		}
		log.setLogLevel(logLevel);
	}
	
	public  List<List<String>> getFilteredADLinksForManager(String managerName,int expiringInDays, String idnTypes) throws GeneralException, ParseException{
		log.logMessage("BEGIN: getFilteredADLinksForManager");
		this.context = SailPointFactory.getCurrentContext();
		List<List<String>> idList = new ArrayList<>();
		List<String> inIdnType = null;
		try {
		QueryOptions qo = new QueryOptions();
		Filter f = null;
		if(null != idnTypes) {
			inIdnType = new ArrayList<>();
			String[] types = idnTypes.split(",");
			 for (String type : types) { 
				 inIdnType.add(type);
		     } 
		}
		if(null == inIdnType) {
			f = Filter.ignoreCase(Filter.and(Filter.eq("manager.name", managerName),Filter.eq("inactive", false)));
		}else {
			f = Filter.ignoreCase(Filter.and(Filter.in("type", inIdnType), Filter.eq("manager.name", managerName),Filter.eq("inactive", false)));
		}
		qo.addFilter(f);
		Iterator<Identity> iterator = context.search(Identity.class, qo);
		while (iterator.hasNext()) {
			log.logMessage("IN WHILE LOOP");
			Identity idn = (Identity) iterator.next();
			log.logMessage("IN WHILE LOOP: GOT IDENTITY: "+idn);
			if (null != idn && !idn.isInactive()) {
				String idName = idn.getName();
				log.logMessage("IN WHILE LOOP: Getting Links for: "+idName);
				List<Link> links = iu.getIdFilteredActiveADLinks(idn);
	            for(Link link : links){
	            	log.logMessage("IN FOR LOOP of Links: "+link.toXml());
	                boolean containsRec = false;
	                try{
	                	containsRec = DateUtils.checkADExpDateMatch(link,expiringInDays);
	                	
	                }catch(Exception e) {
	                	log.logMessage("ERROR: getFilteredADLinksForManager: "+e,e);
	                	throw new RuntimeException("ERROR: getFilteredADLinksForManager: "+e,e);
	                }
	                idAttribList = new ArrayList<>();
	                if(containsRec && idAttribList.isEmpty()) {
	                  idAttribList.add(idName);
	                  String emailAddr = idn.getAttribute("email") != null ? idn.getAttribute("email").toString() : "No Email";
	                  String dispVal = idn.getName()+" :"+idn.getDisplayableName() + " ( Email: " + emailAddr + ")";
	                  idAttribList.add(dispVal);
	                  idList.add(idAttribList);
	                }
	           }
	          }			
			  
				}
		}catch(Exception e) {
			log.logMessage("ERROR: In getFilteredADLinksForManager: "+e,e );
			throw new RuntimeException("ERROR: In getFilteredADLinksForManager: "+e,e );
		}
		log.logMessage("END: getFilteredADLinksForManager");
		return idList.stream().distinct().collect(Collectors.toList());
	 }
	
}
