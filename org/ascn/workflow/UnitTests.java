package org.ascn.workflow;

import org.ascn.utils.ASCNStringUtils;

public class UnitTests {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String entDn = "CN=Exchange Install Domain Servers,CN=Microsoft Exchange System Objects,DC=AHCHM,DC=DS,DC=SJHS,DC=com";
		String userDn = "CN=TGROUPSC,OU=Managed,OU=SailPointDev,DC=wimil,DC=DS,DC=SJHS,DC=com";
		System.out.println(ASCNStringUtils.getDNSubString(entDn, "DC=", 1));
		System.out.println(ASCNStringUtils.getDNSubString(userDn, "DC=", 1));
	}

}
