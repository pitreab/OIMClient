package com.oracle.poc.impl;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.vo.Account;
import Thor.API.Operations.tcFormDefinitionOperationsIntf;
import Thor.API.Operations.tcOrganizationOperationsIntf;

import com.oracle.poc.util.OIMUtils;

public class EvaluateAPForUser {

	/**
	 * @param args
	 */
	private static tcOrganizationOperationsIntf orgOperationsService = null;
	private static tcFormDefinitionOperationsIntf formDefinitionOperationsIntf = null;
	private UserManager userManagerService = null;
	private static OIMUtils oimUtils = null;
	private static final int CHILD_TABLE_RECORDS=1;
	private static final int NUM_USER_RECORDS=2;
	private static Set<String> userIds = new HashSet<String>();
	
	public EvaluateAPForUser(){
	}
	public static void main(String[] args) throws Exception {
		System.out.println("EvaluateAPForUser.main(): Start..");
		
		System.setProperty("APPSERVER_TYPE", "wls");
		System.setProperty("java.security.auth.login.config", "D:\\designconsole\\config\\authwl.conf");
		System.setProperty("XL.HomeDir", "D:\\designconsole");

		System.out.println("EvaluateAPForUser.main(): "+ System.getProperty("APPSERVER_TYPE"));
		System.out.println("EvaluateAPForUser.main(): "+ System.getProperty("java.security.auth.login.config"));
		System.out.println("EvaluateAPForUser.main(): "+ System.getProperty("XL.HomeDir"));
		
		EvaluateAPForUser evaluateAPForUser = new EvaluateAPForUser();
		evaluateAPForUser.findAccessPolicy();
//		evaluateAPForUser.createAPSetUp();
//		oimUtils.runPolicyEvaluationScheduledJob();
//		Thread.sleep(5000);
//		evaluateAPForUser.searchAccount();
		System.out.println("EvaluateAPForUser.main(): End..");
	}		
	
	public void searchAccount(){
		for (int i = 1; i < 80; i++) {
			userIds.add(i+"");
		}
		
		List<String> allAppInstanceIds = new ArrayList<String>();
		allAppInstanceIds.add("1");
		allAppInstanceIds.add("2");
		
		System.out.println("EvaluateAPForUser.searchAccount(): userIds: "+userIds);
		SearchCriteria userSearchCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.USER_ID.getId(), 
				userIds, SearchCriteria.Operator.IN);
		
        SearchCriteria appInstSearchCriteria = null;
        for (String appInstId : allAppInstanceIds) {
            if (appInstSearchCriteria == null) {
                appInstSearchCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.APPINST_ID.getId(), 
                		appInstId, SearchCriteria.Operator.EQUAL);
                continue;
            }
            SearchCriteria tmpSC = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.APPINST_ID.getId(), 
            		appInstId, SearchCriteria.Operator.EQUAL);                
            appInstSearchCriteria = new SearchCriteria(appInstSearchCriteria, tmpSC, SearchCriteria.Operator.OR);
        }
        SearchCriteria sc = new SearchCriteria(userSearchCriteria, appInstSearchCriteria, SearchCriteria.Operator.AND);
        oimUtils = OIMUtils.getInstance();
        Map<String, List<Account>> userAccountListMap = oimUtils.searchAccount(sc);
        
        
	}
	
	public void findAccessPolicy() throws Exception{
		oimUtils = OIMUtils.getInstance();
		oimUtils.findAccessPolicy();
	}
	
	public void createAPSetUp() throws Exception{
		PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
		System.setOut(out);
		oimUtils = OIMUtils.getInstance();
		String prefix = oimUtils.generateUniqueID();
//		String prefix1 = oimUtils.generateUniqueID();
		
		oimUtils.createGroup(prefix);
//		oimUtils.createGroup(prefix1);
	    long grpKey = oimUtils.getGroupKey(prefix);
//	    
//	    long grpKey1 = oimUtils.getGroupKey(prefix1);

//		long grpKey =19;
	    System.out.println("EvaluateAPForUser.initialize():grpKey: "+grpKey);
//	    System.out.println("EvaluateAPForUser.initialize():grpKey1: "+grpKey1);
	    
	    long polKey = oimUtils.setAccessPolicyConfiguration(prefix,grpKey,CHILD_TABLE_RECORDS);
//	    long polKey1 = oimUtils.setAccessPolicyConfiguration(prefix1,grpKey1,CHILD_TABLE_RECORDS);
	    //long polKey = 6;

//	    System.out.println("EvaluateAPForUser.initialize():done creating policy with polKey: "+polKey);
//	    System.out.println("EvaluateAPForUser.initialize():done creating policy with polKey1: "+polKey1);


		long usrKey = -1;
		long start = System.currentTimeMillis();
		
//		for (int i = 0; i < NUM_USER_RECORDS; i++) {
//			 usrKey = oimUtils.createUser(prefix+"-"+i);
//			 userIds.add(usrKey+"");
//			 System.out.println("EvaluateAPForUser.initialize()..created "+i+ "th user: "+usrKey);
			 
//			 oimUtils.addUserToGroup(usrKey, grpKey);
//			 System.out.println("EvaluateAPForUser.initialize()..adding group: "+grpKey+" to user: "+usrKey);
			 
//			 oimUtils.addUserToGroup(usrKey, grpKey1);
//			 System.out.println("EvaluateAPForUser.initialize()..adding group: "+grpKey1+" to user: "+usrKey);
//		}
//	    oimUtils.runPolicyEvaluationScheduledJob();
	    
	    long end = System.currentTimeMillis( );
//        long diff = end - start;
//        
//        System.out.println("EvaluateAPForUser.initialize(): "+oimUtils.covertMilliSecToReadbleFormat(diff));
		
	}

}



