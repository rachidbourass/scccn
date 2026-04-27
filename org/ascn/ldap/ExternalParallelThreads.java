package org.ascn.ldap;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import sailpoint.tools.Internationalizer;

import org.apache.log4j.Logger;

public class ExternalParallelThreads {
	static Logger log = Logger.getLogger("sp.rules");
    
	public static void main(String[] args) throws Exception {
		
		System.out.println("Main thread is- "+ Thread.currentThread().getName());
		Thread t1 = new Thread(new ExternalParallelThreads.MyRunnable(30, LDAPTest.class));
		t1.run();
	}
	public static void runExternalParallelTests(String rootPath, String userId, String password, String acctPrefix,
			int numberOfThreads, int cubeStart, int cubeEnd, Map entitlementsMap) throws Exception {
	
		log.debug("Start ExternalParallelThreads::runExternalProvisioningTests");
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		for (int iter = cubeStart + 0; iter <= cubeStart + numberOfThreads; iter++) {
			// Example: Runnable worker = new MyRunnable("http://localhost:8080/identityiq",
			// "spadmin", "admin", "TESTAPI", iter);
			//Runnable worker = new MyRunnable(rootPath, userId, password, acctPrefix, iter, entitlementsMap);
			//executor.execute(worker);
		}
		executor.shutdown();
// Wait until all threads are finish
		while (!executor.isTerminated()) {

		}
		log.debug("End ExternalParallelThreads::runExternalProvisioningTests Finished all threads");
	}
	public static void runExternalParallelTests(int numberOfThreads, Class T) throws Exception {
	
		log.debug("Start ExternalParallelThreads::runExternalProvisioningTests");
		System.out.println("Start ExternalParallelThreads::runExternalProvisioningTests");
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		for (int iter = 0; iter <= numberOfThreads; iter++) {
			// Example: Runnable worker = new MyRunnable("http://localhost:8080/identityiq",
			// "spadmin", "admin", "TESTAPI", iter);
			Runnable worker = new MyRunnable(iter,T);
			executor.execute(worker);
		}
		executor.shutdown();
// Wait until all threads are finish
		while (!executor.isTerminated()) {

		}
		log.debug("End ExternalParallelThreads::runExternalProvisioningTests Finished all threads");
	}

	public static class MyRunnable implements Runnable {

		private String rootPath = null;
		private String userId = null;
		private String Password = null;
		private String acctPrefix = null;
		private Class T;
		private int iter;
		private Map<String, String> entitlementsMap = null;

		/*MyRunnable(String rootPath, String userId, String Password, String acctPrefix, int iter, Map entitlementsMap) 		{
			this.rootPath = rootPath;
			this.userId = userId;
			this.Password = Password;
			this.acctPrefix = acctPrefix;
			this.iter = iter;
			this.entitlementsMap = entitlementsMap;

		}*/
		MyRunnable(int iter, Class T) 		{
			this.iter = iter;
			this.T = T;
			System.out.println("ITR: "+iter);
			
		}
		
		

		@Override
		public void run() {
			try {
				/*System.out.println("Inside ExternalParallelThreads.run");
				LDAPHelper lh = new LDAPHelper();
				LdapContext ldapContext = lh.getLdapContext();
				System.out.println("Inside ExternalParallelThreads.run = LdapContext: "+ldapContext);
				lh.setSearchAttribName("sAMAccountName");
				lh.setSearchAttribValue("abro0067");
				System.out.println("RESULT: "+lh.checkADAccountExists());
				*/
				
				
			} catch (Exception e) {
				log.debug("ERROR: Failed to communicate with IIQ system!");
				String exceptionMessage = e.getLocalizedMessage();
				if (exceptionMessage.startsWith("401:")) {
					log.debug("ERROR: Invalid user name or password given.");
				}
				log.debug(exceptionMessage);
			}
			return;
		}
	}

		
}
