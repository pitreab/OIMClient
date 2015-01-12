package com.oracle.poc.impl;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import oracle.iam.identity.exception.RoleGrantException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.rolemgmt.vo.RoleManagerResult;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.scheduler.api.SchedulerService;

public class RoleGrantWorker implements Runnable {

	public static String ctxFactory = "weblogic.jndi.WLInitialContextFactory";
	public static String directDBdriver="oracle.jdbc.OracleDriver";
	public Connection connection = null;
	
	// Stress
//	public static String hostName = "slcac647.us.oracle.com";
//	public static String port = "14000";
//	public static String jdbcURL="jdbc:oracle:thin:@(DESCRIPTION =(ADDRESS=(PROTOCOL=TCP)(HOST=adcgen17.us.oracle.com)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=oimps3.us.oracle.com)))";
//	public static String userName="DEC10_OIM";
//	public static String password="welcome1";
	
	public static String hostName = "slc03qyd.us.oracle.com";
	public static String port = "14001";
	public static String jdbcURL="jdbc:oracle:thin:@slc03qyd:5521:oimdb";
	public static String userName="vbpm_oim";
	public static String password="welcome1";
	
	public OIMClient oimClient =null;	
	public String userKey = "";
	public String roleKey = "";
	
	public RoleGrantWorker(String userKey, String roleKey) {
		try {
			oimClient = loginAsUser("xelsysadm", "Welcome1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.userKey=userKey;
		this.roleKey=roleKey;
	}

	@Override
	public void run() {
		System.out.println("RoleGrantWorker.run(): "+Thread.currentThread().getName());
		Set<String> roleSet = new HashSet<String>();
		roleSet.add(roleKey);
		RoleManager roleManagerService =  (RoleManager)oimClient.getService(RoleManager.class);
		SchedulerService schedulerService =  (SchedulerService)oimClient.getService(SchedulerService.class);
		try {
			RoleManagerResult roleMgrresult = roleManagerService.grantRoles(userKey, roleSet);
			System.out.println("RoleGrantWorker.run():"+roleMgrresult.getStatus()+ " "+roleMgrresult.getFailedResults());
		} catch (ValidationFailedException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.out.println("RoleGrantWorker.run(): ValidationFailedException: for role: "+roleKey);
		} catch (RoleGrantException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.out.println("RoleGrantWorker.run(): RoleGrantException: "+roleKey);
		} catch (AccessDeniedException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.out.println("RoleGrantWorker.run(): AccessDeniedException: "+roleKey);
		}

		
		try {
			System.out.println("RoleGrantWorker.run(): bfr eval policy task: "+Thread.currentThread().getName());
			runScheduledJob(schedulerService, "Evaluate User Policies");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public  OIMClient loginAsUser(String userName, String password)
	throws Exception {
		OIMClient oimClient = new OIMClient(getEnvironment());
		oimClient.login(userName, password.toCharArray());
		return oimClient;
	}
	
	public  Hashtable<String, String> getEnvironment(){
		String serverURL;
		Hashtable<String, String> env = new Hashtable<String, String>();		
		serverURL = "t3://" + hostName + ":" + port;		
		env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, serverURL);
		env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, ctxFactory);
		env.put(OIMClient.APPSERVER_TYPE_WEBLOGIC, "wls");
		return env;		
	}	
	
	public void runScheduledJob(SchedulerService schedulerService, String nameOfScheduleJob) throws Exception {
		System.out.println("RoleGrantWorker.runScheduledJob(): running job: "+nameOfScheduleJob);
		schedulerService.triggerNow(nameOfScheduleJob);
//		Thread.sleep(8000);
		
		
	}	
	
}
