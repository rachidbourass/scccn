package org.ascn.utils;

import org.apache.commons.lang3.StringUtils;

public class ASCNStringUtils {

	
	public ASCNStringUtils() {}
	/**
	 * 
	 * @param inStr: String to be substringed for example: 
	 * String inStr = "CN=Schettino\\, Samantha A.,OU=Disabled,OU=Managed,DC=fljac,DC=DS,DC=SJHS,DC=com"
	 * @param delimiter:  The Delimiter text we need after the occurrence of a string, 
	 * for example "DC="
	 * @param position: the position from which we need to extract the string
	 * for example 1
	 * @return: returns the string DC=fljac,DC=DS,DC=SJHS,DC=com 
	 * when invoked getDNSubString(inStr, "DC=", 1)
	 * or when invoked getDNSubString(inStr, "DC=", 2) returns DC=DS,DC=SJHS,DC=com
	 * Use: We use this to determine if we are dealing with a Access/Account request in Forrest or domain
	 */
	public static String getDNSubString(String inStr, String delimiter, int position){
		return inStr.substring(StringUtils.ordinalIndexOf(inStr,delimiter,position));
	}
}
