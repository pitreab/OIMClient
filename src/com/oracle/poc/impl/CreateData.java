package com.oracle.poc.impl;

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

import oracle.iam.conf.api.SystemConfigurationService;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.usermgmt.vo.UserManagerResult;
import oracle.iam.platform.OIMClient;
import oracle.iam.scheduler.api.SchedulerService;
import Thor.API.Operations.tcFormDefinitionOperationsIntf;

import com.oracle.poc.util.OIMUtils;

public class CreateData {

	public static OIMClient oimClient =null;
	
	// Sudhakara. 
//	public static String hostName = "slc06utq.us.oracle.com";
//	public static String port = "14003";
//	public static String jdbcURL="jdbc:oracle:thin:@slc06utq:5521:oimdb";
//	public static String userName="VC1211_OIM";
	
	// Ashutosh
//	public static String hostName = "slc03qyd.us.oracle.com";
//	public static String port = "14001";
//	public static String jdbcURL = "jdbc:oracle:thin:@slc03qyd:5521:oimdb";
//	public static String userName = "vbpm_oim";

//	public static String hostName = "slc01mbq.us.oracle.com";
//	public static String port = "14003";
//	public static String jdbcURL = "jdbc:oracle:thin:@slc01mbq:5521:oimdb";
//	public static String userName = "vhView_OIM";

	//	public static String hostName = "slc08fha.us.oracle.com";
//	public static String port = "14001";
//	public static String jdbcURL="jdbc:oracle:thin:@slc08fha:5521:oimdb";
//	public static String userName="vtest1_oim";
	
	//saheli
//	public static String hostName = "slc01mwv.us.oracle.com";
//	public static String port = "14001";
//	public static String jdbcURL="jdbc:oracle:thin:@slc01mwv:5521:oimdb";
//	public static String userName="vdec15_OIM";
	
	// Manjunath A
//	public static String hostName = "slc01awr.us.oracle.com";
//	public static String port = "14001";
//	public static String jdbcURL="jdbc:oracle:thin:@slc01awr:5521:oimdb";
//	public static String userName="vr2ps3_OIM";
//	public static String password="welcome1";
	
	// Muthu = PSR
	public static String hostName = "slcac647.us.oracle.com";
	public static String port = "14000";
	public static String jdbcURL="jdbc:oracle:thin:@(DESCRIPTION =(ADDRESS=(PROTOCOL=TCP)(HOST=adcgen17.us.oracle.com)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=oimps3.us.oracle.com)))";
	public static String userName="DEC10_OIM";
	
	public static String APPSERVER_TYPE = "wls";
	public static String AUTH_LOGIN_CONFIG = "D:\\designconsole\\config\\authwl.conf";
	public static String XL_HOME_DIR = "D:\\designconsole";

	public static String ctxFactory = "weblogic.jndi.WLInitialContextFactory";
	public static String directDBdriver="oracle.jdbc.OracleDriver";
	public Connection connection = null;
	
	public static int NUM_OF_USERS_TO_CREATE=20; //5
	
	
	public static tcFormDefinitionOperationsIntf formDefinitionOperationsIntf = null;
	private static OIMUtils oimUtils = OIMUtils.getInstance();

	public static void main(String[] args) throws Exception {
		System.out.println("CreateAppInstance.main(): start");
		CreateData createData = new CreateData();
		
		createData.createData(createData);
		System.out.println("CreatePSRData.main(): completed");
	}

	public void createData(CreateData data) throws Exception {
		System.setProperty("APPSERVER_TYPE", APPSERVER_TYPE);
		System.setProperty("java.security.auth.login.config", AUTH_LOGIN_CONFIG);
		System.setProperty("XL.HomeDir", XL_HOME_DIR);

		oimClient = data.loginAsUser("xelsysadm", "Welcome1");
		System.out.println("CreatePSRData.createPSRData(): login done......");

		SystemConfigurationService sysConfigService = (SystemConfigurationService)oimClient.getService(SystemConfigurationService.class);
		SchedulerService schedulerService =  (SchedulerService)oimClient.getService(SchedulerService.class);
//		runScheduledJob(schedulerService, "Evaluate User Policies");
		
		
		// create user and add to group. 
		UserManager userManagerService =  (UserManager)oimClient.getService(UserManager.class);
		RoleManager roleManagerService =  (RoleManager)oimClient.getService(RoleManager.class);
		
		String roleKey = "2006";
		Set roleSet = new HashSet<String>();
		roleSet.add(roleKey);
		
		for (int i = 0; i < NUM_OF_USERS_TO_CREATE ; i++) {
			String prefix = "USER_"+generateUniqueID(6)+"_"+i;
			String userKey = createUser(prefix, userManagerService);
			
			roleManagerService.grantRoles(userKey, roleSet);
			System.out.println("CreatePSRData.createPSRData(): grantRoles to user: "+prefix);
		}
//		runScheduledJob(schedulerService, "Entitlement List");
//		runScheduledJob(schedulerService, "Catalog Synchronization Job");
		runScheduledJob(schedulerService, "Evaluate User Policies");
		
		
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
		System.out.println("CreatePSRData.runScheduledJob(): running job: "+nameOfScheduleJob);
		Thread.sleep(2000);
		schedulerService.triggerNow(nameOfScheduleJob);
		Thread.sleep(8000);
	}
	
	public  String generateUniqueID(int uniqueIDLength) {
		Random r = new Random();
		String uniqueID = (Long.toString(Math.abs(r.nextLong()), 36)).substring(0, uniqueIDLength);
		
		return uniqueID.toUpperCase();
	}
	
	public String  createUser(String uniqueID, UserManager userManagerService) throws Exception {
		
		String userKey = null;
		UserManagerResult result = null;
		HashMap<String, Object> attrs = null;


		attrs = new HashMap<String, Object>();
//		String userLogin = USER_PREFIX + uniqueID + SUFFIX;
		String userLogin = uniqueID;
		attrs.put("act_key", new Long(1));
		attrs.put("Last Name", "LS");
		attrs.put("Middle Name", uniqueID);
		attrs.put("First Name", "USR");
		attrs.put("usr_password", "Welcome1");
		attrs.put("User Login", userLogin);
		attrs.put("Xellerate Type", "End-User");
		attrs.put("Role", "Full-Time");
		result = userManagerService.create(new User(null,attrs)); 
		
		userKey = result.getEntityId();
		System.out.println("CreatePSRData.createUser(): Created User: "+uniqueID+ " with userKey: "+userKey);
		return userKey;
	}
	

}

