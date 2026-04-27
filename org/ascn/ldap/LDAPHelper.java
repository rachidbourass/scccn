package org.ascn.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.ascn.objectutils.ApplicationUtils;
import org.ascn.utils.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Application;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

/* LDAPHelper Utility provides various methods to help perform AD Operations 
 * straight forward and simple. This is specific to be deployed in SailPoint 
 * environment and hence soley depends on getting AD connection information
 * from Active Directory connector configuration to facilitate the various 
 * operations
 */
public class LDAPHelper {

	private Logger log;
	private SailPointContext context;
	private LdapContext ldapContext;
	private String domainDN;
	private String userDN;
	private String appName;
	private List<String> dnParts;
	private String searchAttribName;
	private String searchAttribValue;
	private ApplicationUtils appUtils;
	
	final String ldapUsername = "DS\\sailpoint-ds";
    final String ldapPassword = "fy/t3[q.CV.3})F73*jJq!djU.Wkft";
    final String ldapAdServer = "ldap://ANDC1WADC0174.DCWAS.DS.SJHS.COM:389";
    
	public LDAPHelper() throws GeneralException {
		this.log = new Logger(getClass().getName());
		this.context = SailPointFactory.getCurrentContext();
		try {
			
			this.appUtils = new ApplicationUtils();
		}catch(Exception e) {
			log.logMessage("SP Context Error", e);
		}
	}
	
	public String getDomainDN() {
		return domainDN;
	}
	public void setDomainDN(String domainDN) {
		this.domainDN = domainDN;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public void setLdapContext(LdapContext ldapContext) {
		this.ldapContext = ldapContext;
	}
	public String getSearchAttribName() {
		return searchAttribName;
	}
	public void setSearchAttribName(String searchAttribName) {
		this.searchAttribName = searchAttribName;
	}
	public String getSearchAttribValue() {
		return searchAttribValue;
	}
	public void setSearchAttribValue(String searchAttribValue) {
		this.searchAttribValue = searchAttribValue;
	}
	public LdapContext getLdapContext() throws GeneralException {
		if(null == ldapContext) {
			try {
				if(getLdapEnv() != null || !getLdapEnv().isEmpty()) {
					ldapContext = new InitialLdapContext(getLdapEnv(),null);
				}
				ldapContext = new InitialLdapContext(getLdapEnvManual(),null);
				log.logMessage("Successfully Connected to LDAP");
			} catch (GeneralException | NamingException e) {
				log.logMessage("ERROR Setting LdapContext", e);
			}
		}
		return ldapContext;
	}
	public LdapName getUserDN() throws InvalidNameException {
		return new LdapName(userDN);
	}
	public void setUserDN(String userDN) {
		this.userDN = userDN;
	}
	
	public Properties getLdapEnv() throws GeneralException {
		log.logMessage("BEGIN: Executing Method getLdapEnv()");
		Properties env = new Properties();
		try {
			appUtils.setDomainDN(domainDN);
			appUtils.setAppName(appName);
		    Map<String,Object> ldapConfig = appUtils.getADConfig();
			if(!ldapConfig.isEmpty()){
				env.put(Context.SECURITY_AUTHENTICATION,ldapConfig.get("authType"));
				env.put(Context.SECURITY_PRINCIPAL, ldapConfig.get("userName"));
				env.put(Context.SECURITY_CREDENTIALS, ldapConfig.get("userPassword"));
				env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
				env.put(Context.PROVIDER_URL,ldapConfig.get("ldapUrl"));
			}
		}catch(Exception e) {
			log.logMessage("Error Creating LDAP Properties", e);
		}
		log.logMessage("END: Executing Method getLdapEnv()");
		return env;
}
	public Properties getLdapEnvManual() throws GeneralException {
		System.out.println("BEGIN: Executing Method getLdapEnv()");
		Properties env = new Properties();
		try {
			
				env.put(Context.SECURITY_AUTHENTICATION,"simple");
				env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
				env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
				env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
				env.put(Context.PROVIDER_URL,ldapAdServer);
			
		}catch(Exception e) {
			System.out.println("Error Creating LDAP Properties");
		}
		System.out.println("END: Executing Method getLdapEnv()");
		return env;
    }
	public boolean checkADAccountExists() throws GeneralException {
		log.logMessage("BEGIN: Executing Method checkADAccountExists()");
		boolean exists = false;
		NamingEnumeration<SearchResult> answer = null;
		if(null != getSearchAttribName() && null != getSearchAttribValue()) {
			String filter = "("+getSearchAttribName()+"="+getSearchAttribValue()+")";
			try {
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			answer = getLdapContext().search(getDomainDN(),filter,sc);
				if(answer.hasMore()){
					SearchResult sr = (SearchResult) answer.next();
					exists = Objects.nonNull(sr);
				}
		    }catch(Exception e) {
				log.logMessage("Error in Method checkADAccountExists()", e);
			}finally {
				close(answer);
			}
		}else {
			log.logMessage("Search AttributeName and/or Value is NULL = name= "+getSearchAttribName()+" : value= "+getSearchAttribValue());
		}
	    log.logMessage("END: Executing Method checkADAccountExists()");
	    return exists;
	}
	public boolean checkADAccountExists(Map<String,Object> inMap, String condition) throws GeneralException {
		log.logMessage("BEGIN: Executing Method checkADAccountExists()");
		boolean exists = false;
		NamingEnumeration<SearchResult> answer = null;
		if(null != inMap && null != condition) {
			String filter = buildFilterString(inMap,condition);
			try {
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			answer = getLdapContext().search(getDomainDN(),filter,sc);
				if(answer.hasMore()){
					SearchResult sr = (SearchResult) answer.next();
					exists = Objects.nonNull(sr);
				}
		    }catch(Exception e) {
				log.logMessage("Error in Method checkADAccountExists()", e);
			}finally {
				close(answer);
			}
		}else {
			log.logMessage("Search AttributeName and/or Value is NULL = name= "+getSearchAttribName()+" : value= "+getSearchAttribValue());
		}
	    log.logMessage("END: Executing Method checkADAccountExists()");
	    return exists;
	}
	
	//This method should be used to perform exact search on a AD attribute
	public Attributes searchADAccount(String[] attrNames) throws GeneralException{
		log.logMessage("BEGIN: Executing Method searchADAccounts()");
		Attributes attrs = null;
		NamingEnumeration<SearchResult> answer = null;
		String[] attrIDs = { LdapAttributes.SN,LdapAttributes.CN,LdapAttributes.DN,LdapAttributes.EMAIL,LdapAttributes.GIVENNAME,LdapAttributes.DISPLAYNAME,LdapAttributes.USERNAME,LdapAttributes.PRIMARYACCOUNT,LdapAttributes.EMPLNUMBER,LdapAttributes.MSEXATTRIB40,LdapAttributes.UPN,LdapAttributes.ACCOUNTFLAGS,LdapAttributes.EMPLOYEENUMBER,LdapAttributes.MAILNICKNAME,LdapAttributes.MSDSPRINCIPALNAME,LdapAttributes.DEPARTMENT,LdapAttributes.DEPARTMENTNUMBER,LdapAttributes.EXTERNALEMAILADDRESS,LdapAttributes.COMPANY,LdapAttributes.ASCGSUITELASTNAME};
		if(null != attrNames ) {
			attrIDs = attrNames;
		}
		if(null != getSearchAttribName() && null != getSearchAttribValue()) {
			String filter = "("+getSearchAttribName()+"="+getSearchAttribValue()+")";
			try {
			SearchControls sc = new SearchControls();
			sc.setReturningAttributes(attrIDs);
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			answer = getLdapContext().search(getDomainDN(),filter,sc);
				while(answer.hasMore()){
					SearchResult sr = (SearchResult) answer.next();
					this.setUserDN(sr.getNameInNamespace());
					attrs = sr.getAttributes();
				}
		    }catch(Exception e) {
				log.logMessage("Error in Method searchADAccounts()", e);
			}finally {
				close(answer);
			}
		}else {
			log.logMessage("Search AttributeName and/or Value is NULL = name= "+getSearchAttribName()+" : value= "+getSearchAttribValue());
		}
	    log.logMessage("END: Executing Method searchADAccounts()");
	    return attrs;
	}
	//This method should be used to perform exact search on a AD attribute
	public Attributes searchADAccount() throws GeneralException{
			log.logMessage("BEGIN: Executing Method searchADAccounts()");
			Attributes attrs = null;
			NamingEnumeration<SearchResult> answer = null;
			String[] attrIDs = { LdapAttributes.SN,LdapAttributes.CN,LdapAttributes.DN,LdapAttributes.EMAIL,LdapAttributes.GIVENNAME,LdapAttributes.DISPLAYNAME,LdapAttributes.USERNAME,LdapAttributes.PRIMARYACCOUNT,LdapAttributes.EMPLNUMBER,LdapAttributes.MSEXATTRIB40,LdapAttributes.UPN,LdapAttributes.ACCOUNTFLAGS,LdapAttributes.EMPLOYEENUMBER,LdapAttributes.MAILNICKNAME,LdapAttributes.MSDSPRINCIPALNAME,LdapAttributes.DEPARTMENT,LdapAttributes.DEPARTMENTNUMBER,LdapAttributes.EXTERNALEMAILADDRESS,LdapAttributes.COMPANY,LdapAttributes.ASCGSUITELASTNAME};
			if(null != getSearchAttribName() && null != getSearchAttribValue()) {
				String filter = "("+getSearchAttribName()+"="+getSearchAttribValue()+")";
				try {
				SearchControls sc = new SearchControls();
				sc.setReturningAttributes(attrIDs);
				sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
				answer = getLdapContext().search(getDomainDN(),filter,sc);
					while(answer.hasMore()){
						SearchResult sr = (SearchResult) answer.next();
						this.setUserDN(sr.getNameInNamespace());
						attrs = sr.getAttributes();
						
					}
			    }catch(Exception e) {
					log.logMessage("Error in Method searchADAccounts()", e);
				}finally {
					close(answer);
				}
			}else {
				log.logMessage("Search AttributeName and/or Value is NULL = name= "+getSearchAttribName()+" : value= "+getSearchAttribValue());
			}
		    log.logMessage("END: Executing Method searchADAccounts()");
		    return attrs;
		}
	public Attributes searchADAccount(Map<String,Object> inMap, String condition) throws GeneralException{
		log.logMessage("BEGIN: Executing Method searchADAccount(....)");
		Attributes attrs = null;
		NamingEnumeration<SearchResult> answer = null;
		String[] attrIDs = {LdapAttributes.SN,LdapAttributes.CN,LdapAttributes.DN,LdapAttributes.EMAIL,LdapAttributes.GIVENNAME,LdapAttributes.DISPLAYNAME,LdapAttributes.USERNAME,LdapAttributes.PRIMARYACCOUNT,LdapAttributes.EMPLNUMBER,LdapAttributes.MSEXATTRIB40,LdapAttributes.UPN,LdapAttributes.ACCOUNTFLAGS,LdapAttributes.EMPLOYEENUMBER,LdapAttributes.MAILNICKNAME,LdapAttributes.MSDSPRINCIPALNAME,LdapAttributes.DEPARTMENT,LdapAttributes.DEPARTMENTNUMBER,LdapAttributes.EXTERNALEMAILADDRESS,LdapAttributes.COMPANY,LdapAttributes.ASCGSUITELASTNAME};
		if(null != inMap && null != condition) {
			String filter = buildFilterString(inMap,condition);
			try {
			SearchControls sc = new SearchControls();
			sc.setReturningAttributes(attrIDs);
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			answer = getLdapContext().search(getDomainDN(),filter,sc);
				while(answer.hasMore()){
					SearchResult sr = (SearchResult) answer.next();
					this.setUserDN(sr.getNameInNamespace());
					attrs = sr.getAttributes();
					
				}
		    }catch(Exception e) {
				log.logMessage("Error in Method searchSingleValueMultiAttribs()", e);
			}finally {
				close(answer);
			}
		}else {
			log.logMessage("Search AttributeName and/or Value is NULL = name= "+getSearchAttribName()+" : value= "+getSearchAttribValue());
		}
	    log.logMessage("END: Executing Method searchSingleValueMultiAttribs(....)");
	    return attrs;
}
		/*
		 * getAccountAttributes takes naming.attributes and create a Map Object
		 * this is more for convenience, so the implementation in BeanShell only needs to 
		 * deal with Map object
		 */
	@SuppressWarnings("unchecked")
	public Map<String,Object> getAccountAttributes(Attributes attrs) throws GeneralException{
			log.logMessage("BEGIN: Parsing Returned LDAP Attributes into MAP Object: getAccountAttributes(Attributes attrs)");
			Map<String,Object> attrMap = new HashMap<>();
			if(null != attrs) {
				log.logMessage("Data: getAccountAttributes(Attributes attrs): Attributes: "+attrs);
			try {
		        for (NamingEnumeration<?> ae = attrs.getAll(); ae.hasMore();) {
		          Attribute attr = (Attribute) ae.next();
		          String attribName = attr.getID();
		          for (NamingEnumeration<?> value = attr.getAll(); value.hasMore();){
		        	 Object attribValue = value.next();
		             if(attribValue instanceof String) {
		            	 attrMap.put(attribName, attribValue); 
		             }
		             if(attribValue instanceof List) {
		            	 attrMap.put(attribName, Util.listToCsv((List<Object>) attribValue, false)); 
		             }
		          }
		         }
		      } catch (NamingException e) {
		    	  log.logMessage("ERROR: Parsing Returned LDAP Attributes into MAP Object: getAccountAttributes(Attributes attrs)",e);
		      }
			}else {
				log.logMessage("Data: getAccountAttributes(Attributes attrs): Attributes are null: ");
			}
			log.logMessage("END: Parsing Returned LDAP Attributes into MAP Object: getAccountAttributes(Attributes attrs)");
			return attrMap;
		}
	
	public boolean moveLDAPAccount(String oldDN, String newDN) throws GeneralException {
		log.logMessage("START: Move User to a new DN: moveLDAPAccount(oldDN,newDN)");
		boolean moved = false;
		try {
			getLdapContext().rename(oldDN, newDN);
			moved = true;
		} catch (GeneralException | NamingException e) {
			log.logMessage("ERROR: Move User to a new DN: moveLDAPAccount(oldDN,newDN): ",e);
		}
		log.logMessage("START: Move User to a new DN: moveLDAPAccount(oldDN,newDN)");
		return moved;
	}
	public boolean activateLDAPAccount() {
		boolean active = false;
		return active;
	}
	
	/*
	 * getDisplayNameFromDN takes UserDN as input and returns just the
	 * CN portion of it. The returned value can be used for setting 
	 * display name.
	 */
	public String getDisplayNameFromDN(String userDN) throws InvalidNameException {
		String userCN = null;
		LdapName dn = new LdapName(userDN);
		for(Rdn rdn : dn.getRdns()) {
		    if(rdn.getType().equalsIgnoreCase("CN")) {
		        userCN = (String) rdn.getValue();
		        break;
		    }
		}
		return userCN;
	}
	/*
	 * getCNFromDN takes UserDN as input and returns just the
	 * CN portion of it. The returned value can be used for setting 
	 * display name.
	 */
	public String getCNFromDN(String userDN) throws InvalidNameException {
		String userCN = null;
		LdapName dn = new LdapName(userDN);
		String cnRegex = "(CN=)(.*?)(?<!\\\\),.*";
		Pattern pattern = Pattern.compile(cnRegex); 
		Matcher matcher = pattern.matcher(userDN); 
		if(matcher.find()) {
			userCN = matcher.group(2); 
		}
		return userCN;
	}
	/*
	 * getDNParts takes UserDN and breaks this down into individual parts, adds 
	 * the parts to the list and return the list. This list can be used to determine
	 * the root DN and / or the OU of the user for further processing
	 * 
	 */
	public List<String> getDNParts(String dn) throws InvalidNameException{
		LdapName name = new LdapName(dn);
		dnParts = new ArrayList<>();
		if(null != name) {
			for(Rdn rdn : name.getRdns()) {
				this.dnParts.add(rdn.getType()+"="+ rdn.getValue());
			}
		}
		return dnParts;
	}
	/*
	 * createUserAccount is used to create AD account. 
	 * INPUTS: 
	 * Map<String,Object> (Required):
	 * String: (Required): OU or Container where the user account needs to be created
	 * String: (Optional): baseDN where the account will be created. if not provided, default BaseDN will be used
	 */
	public boolean createAccount(Map<String,Object> inMap, List<String> objectClassList) throws GeneralException, InvalidNameException {
		boolean success = false;
		log.logMessage("START:  Create AD Account createAccount...for: "+getUserDN());
		if(null == getUserDN()) {
			log.logMessage("REQUIRED ATTRIBUTE VALUE MISSING:  User DN is null: "+getUserDN());
			return success;
		}
		BasicAttributes attrs = new BasicAttributes(true);
		log.logMessage("Build Object Class");
		Attribute objClass = new BasicAttribute("objectclass");
		if(objectClassList.isEmpty()) {
			objClass.add("organizationalPerson"); 
			objClass.add("person"); 
			objClass.add("top"); 
			objClass.add("user"); 
		}else {
			for(String s:objectClassList) {
				objClass.add(s); 
			}
		}
		attrs.put(objClass);
	    for (Map.Entry<String,Object> m : inMap.entrySet()) {
		     attrs.put(new BasicAttribute(m.getKey(), m.getValue()));
	     }
	     try {
			ldapContext.createSubcontext(getUserDN(), attrs);
		} catch (NamingException e) {
			log.logMessage("ERROR: Create AD Account for: "+getUserDN(), e);
			return success;
		}
	     log.logMessage("END:  Create AD Account createAccount...for: "+getUserDN());
	     return success;
	}
	/*
	 * createUserAccount is used to create AD account. 
	 * INPUTS: 
	 * Map<String,Object> (Required):
	 * String: (Required): OU or Container where the user account needs to be created
	 * String: (Optional): baseDN where the account will be created. if not provided, default BaseDN will be used
	 */
	public boolean createAccount(Map<String,Object> inMap) throws GeneralException, InvalidNameException {
		boolean success = false;
		log.logMessage("START:  Create AD Account createAccount...for: "+getUserDN());
		if(null == getUserDN()) {
			log.logMessage("REQUIRED ATTRIBUTE VALUE MISSING:  User DN is null: "+getUserDN());
			return success;
		}
		BasicAttributes attrs = new BasicAttributes(true);
		log.logMessage("Build Object Class");
		Attribute objClass = new BasicAttribute("objectclass");
		objClass.add("organizationalPerson"); 
		objClass.add("person"); 
		objClass.add("top"); 
		objClass.add("user"); 
		attrs.put(objClass);
	    for (Map.Entry<String,Object> m : inMap.entrySet()) {
		     attrs.put(new BasicAttribute(m.getKey(), m.getValue()));
	     }
	     try {
			ldapContext.createSubcontext(getUserDN(), attrs);
		} catch (NamingException e) {
			log.logMessage("ERROR: Create AD Account for: "+getUserDN(), e);
			return success;
		}
	     log.logMessage("END:  Create AD Account createAccount...for: "+getUserDN());
	     return success;
	}
	/*
	 * modifyAccount is used to create AD account. 
	 * INPUTS: 
	 * Map<String,Object> (Required):
	 * String: (Required): OU or Container where the user account needs to be created
	 * String: (Optional): baseDN where the account will be created. if not provided, default BaseDN will be used
	 */
	public boolean modifyAccount(Map<String,Object> inMap) throws GeneralException, InvalidNameException {
		boolean success = false;
		log.logMessage("START:  Modified AD Account modifyAccount...for: "+getUserDN());
		if(null == getUserDN()) {
			log.logMessage("REQUIRED ATTRIBUTE VALUE MISSING:  User DN is null: "+getUserDN());
			return success;
		}
		if(inMap.isEmpty()) {
			log.logMessage("REQUIRED ATTRIBUTE VALUE MISSING:  Modification Attributes not provided: "+inMap);
			return success;
		}
		int numitems = inMap.size();
		ModificationItem[] mods = new ModificationItem[numitems];
		int i = 0;
		for(Map.Entry<String,Object> m : inMap.entrySet()) {
			String[] actionKey = m.getKey().split("#");
			String action = actionKey[0];
			String keyName = actionKey[1];
			if(Util.nullSafeCaseInsensitiveEq("ADD", action)) {
				mods[i] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(m.getKey(), m.getValue()));
			}else if(Util.nullSafeCaseInsensitiveEq("REMOVE", action)) {
				mods[i] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(m.getKey(), m.getValue()));
			}else if(Util.nullSafeCaseInsensitiveEq("MODIFY", action)) {
				mods[i] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(m.getKey(), m.getValue()));
			}
			i++;
		}
		try {
			ldapContext.modifyAttributes(getUserDN(), mods);
			log.logMessage("SUCCESS:  Modified AD Account for: "+getUserDN());
			success = true;
		} catch (NamingException e) {
			log.logMessage("ERROR: Modifying AD Account for: "+getUserDN(), e);
			return success;
		}
		log.logMessage("END:  Modified AD Account modifyAccount...for: "+getUserDN()+"  Result: "+success);
		return success;
	}
	public boolean changeADPassword(String password) throws GeneralException, InvalidNameException { 
		boolean success = false;
		log.logMessage("START:  Change AD Account Password: changeADPassword...for: "+getUserDN());
		if(null == getUserDN()) {
			log.logMessage("REQUIRED ATTRIBUTE VALUE MISSING:  User DN is null: "+getUserDN());
			return success;
		}
		if(null == password) {
			log.logMessage("REQUIRED ATTRIBUTE VALUE MISSING:  Modification Attributes not provided: "+password);
			return success;
		}
		try { 
		String quotedPassword = "\"" + password + "\""; 
		char unicodePwd[] = quotedPassword.toCharArray(); 
		byte pwdArray[] = new byte[unicodePwd.length * 2]; 
			for (int i=0; i<unicodePwd.length; i++) { 
				pwdArray[i*2 + 1] = (byte) (unicodePwd[i] >>> 8); 
				pwdArray[i*2 + 0] = (byte) (unicodePwd[i] & 0xff); 
			} 
			ModificationItem[] mods = new ModificationItem[1]; 
			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("UnicodePwd", pwdArray)); 
			ldapContext.modifyAttributes(getUserDN(), mods); 
			log.logMessage("SUCCESS:  Change AD Account Password: changeADPassword...for: "+getUserDN());
		} 
		catch (Exception e) { 
			log.logMessage("ERROR: Change AD Account Password: changeADPassword...for: "+getUserDN(), e);
			return success;
		} 
		return success;
		} 
	/**
     * Close context.
     * 
     * @param ldapContext
	 * @throws GeneralException 
     */
    public void close(LdapContext ldapContext) throws GeneralException {
        if (ldapContext == null) {
            return;
        }
        try {
        	ldapContext.close();
        } catch (NamingException ignore) {
            log.logMessage("ERROR: Closing LdapContext: "+ignore.getMessage(), ignore);
        }
    }
    /**
     * Close naming enumeration.
     * 
     * @param ne
     * @throws GeneralException 
     */
    public void close(NamingEnumeration ne) throws GeneralException {
        if (ne == null) {
            return;
        }
        try {
            ne.close();
        } catch (NamingException ignore) {
        	log.logMessage("ERROR: Closing NamingEnumeration: "+ ignore.getMessage(), ignore);
		}
    }
    private String[] splittedString(String inStr, String splitter) {
    	String[] arrOfStr = inStr.split(splitter);
    	 
    	    for (String a : arrOfStr) {
    	        System.out.println(a);
    	    }
    	    return arrOfStr;
    	}
    public static String buildFilterString(String attribNames, String attribValue, String condition) {
		String [] attribs = attribNames.split(",");
		String filterString = null;
		if("OR".equalsIgnoreCase(condition)) {
			filterString = "(|";
		}
		if("AND".equalsIgnoreCase(condition)) {
			filterString = "(&";
		}
		for(int i=0; i<attribs.length;i++) {
			filterString = filterString+"("+attribs[i]+"="+attribValue+")";
	    }
		filterString = filterString+")";
		return filterString;
	}
    public static String buildFilterString(Map<String,Object> inMap, String condition) {
		String filterString = null;
		if("OR".equalsIgnoreCase(condition)) {
			filterString = "(|";
		}
		if("AND".equalsIgnoreCase(condition)) {
			filterString = "(&";
		}
		for (Map.Entry<String,Object> m : inMap.entrySet()) {
			filterString = filterString+"("+m.getKey()+"="+m.getValue()+")";
		} 
		filterString = filterString+")";
		return filterString;
	}
}
