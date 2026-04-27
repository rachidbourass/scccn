package org.ascn.ldap;

import javax.naming.ldap.LdapContext;

public class TestLDAPMethods {

	public static void main(String[] args) throws Exception {
		System.out.println("Inside ExternalParallelThreads.run");
		try {
		LDAPHelper lh = new LDAPHelper();
		LdapContext ldapContext = lh.getLdapContext();
		System.out.println("Inside ExternalParallelThreads.run = LdapContext: "+ldapContext);
		lh.setSearchAttribName("sAMAccountName");
		lh.setSearchAttribValue("abro0067");
		System.out.println("RESULT: "+lh.checkADAccountExists());
		}catch(Exception e) {
			System.out.println("ERROR :"+e);
		}
		// TODO Auto-generated method stub
		//ExternalParallelThreads.runExternalParallelTests(30);
		/*String userDN = "CN=abro0067,OU=Users,OU=Managed,DC=dcwas,DC=DS,DC=SJHS,DC=com";
		LdapName dn = new LdapName(userDN);
		String cnRegex = "(CN=)(.*?)(?<!\\\\),.*";
		Pattern pattern = Pattern.compile(cnRegex);
		Matcher matcher = pattern.matcher(userDN);
		System.out.println("Domain DN: " + userDN.substring(userDN.indexOf("DC=")));
		System.out.println("FOREST DN: " + userDN.substring(userDN.indexOf("DC=")));
		System.out.println("FOREST DN: " + userDN.substring(StringUtils.ordinalIndexOf(userDN,"DC=",1)));
		String generatedString = RandomStringUtils.random(12, true, true);
		System.out.println("GENERATED Password: "+generatedString);

		if(matcher.find()) {
			System.out.println("Pattern: " + matcher.group(2));
		}
		System.out.println("RDN: "+dn.getSuffix(1));
		System.out.println("Sufix: "+dn.getPrefix(dn.size()));
		System.out.println("DOMAIN SUFFIX: "+dn.toString().split("DC="));
		for(Rdn rdn : dn.getRdns()) {
			System.out.println(rdn.getType()+"="+ rdn.getValue());
		   /* if(rdn.getType().equalsIgnoreCase("CN")) {
		    	System.out.println(rdn.getType()+" is: " + rdn.getValue());
		     }
		    if(rdn.getType().equalsIgnoreCase("OU")) {
		        System.out.println(rdn.getType()+" is: " + rdn.getValue());

		     }
		    if(rdn.getType().equalsIgnoreCase("DC")) {
		        System.out.println(rdn.getType()+" is: " + rdn.getValue());

		     }
		}
		Map<String,Object> myMap = new HashMap<>();
		myMap.put("ADD#name","Rajesh Saka");
		myMap.put("ADD#Color","Blue");
		myMap.put("REMOVE#Size","Mid");
		myMap.put("MODIFY#Height", "5.10");
		int i =0;
		for (Map.Entry<String,Object> m : myMap.entrySet()) {
			String[] actionKey = m.getKey().split("#");
			String action = actionKey[0];
			String keyName = actionKey[1];
			if(action.equals("ADD")) {
				System.out.println("DirContext.ADD_ATTRIBUTE :ACTION: "+action+" = "+keyName+"= "+m.getValue());
			}else if(action.equals("REMOVE")) {
				System.out.println("DirContext.REMOVE_ATTRIBUTE :ACTION: "+action+" = "+keyName+"= "+m.getValue());
			}if(action.equals("MODIFY")) {
				System.out.println("DirContext.MODIFY_ATTRIBUTE :ACTION: "+action+" = "+keyName+"= "+m.getValue());
			}
				i++;

		}
		*/
	}
	public static String[] splittedString(String inStr, String splitter) {
	String[] arrOfStr = inStr.split(splitter);

	    for (String a : arrOfStr) {
	        System.out.println(a);
	    }
	    return arrOfStr;
	}

}
