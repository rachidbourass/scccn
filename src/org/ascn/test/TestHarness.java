package org.ascn.test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class TestHarness {



	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//System.out.println(ServiceNowAssets.getTotalCount("IdentityIQ for ServiceNow Service Desk",getConfigMap()));
		test();
	}

	public static void test()  {
		String rawUrl = "https://tableau-dev.ascension.org/api/3.23/sites/272f6f29-2a96-4fd7-8b8f-59cfa24b6a05/users?pageSize=200%23%23Ministry+Service+Center%23%23MSC%23%23Active%23%23response.siteId";

		 String newRawUrl =  decodeUrl(rawUrl);
		 String[] splitRawUrl = newRawUrl.split("##");
		 String fullUrl = splitRawUrl[0];
		 String siteName  =  splitRawUrl[1];
		 String siteContentUrl  =  splitRawUrl[2];
		 String siteStatus  =  splitRawUrl[3];
		 String siteId   =  splitRawUrl[4];

		 System.out.println("FULL URL: "+fullUrl);
	}



	public static String decodeUrl(String encodedString) {
        try {
            return URLDecoder.decode(encodedString, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return encodedString; // Return the original string if decoding fails
        }
    }






}
