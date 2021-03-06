package com.oracle.poc.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import oracle.iam.conf.api.SystemConfigurationService;
import oracle.iam.conf.exception.SystemConfigurationServiceException;
import oracle.iam.conf.vo.SystemProperty;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.usermgmt.vo.UserManagerResult;
import oracle.iam.platform.OIMClient;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.scheduler.api.SchedulerService;
import Thor.API.tcResultSet;
import Thor.API.Operations.tcAccessPolicyOperationsIntf;
import Thor.API.Operations.tcAccessPolicyOperationsIntf.PolicyNLAObjectActionType;
import Thor.API.Operations.tcFormDefinitionOperationsIntf;
import Thor.API.Operations.tcGroupOperationsIntf;
import Thor.API.Operations.tcITResourceDefinitionOperationsIntf;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;
import Thor.API.Operations.tcImportOperationsIntf;
import Thor.API.Operations.tcObjectOperationsIntf;

import com.thortech.xl.vo.AccessPolicyResourceData;

public class RoleGrantMultiThreadTest {

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

	
	int NUM_OF_RESOURCE_OBJECTS=1; //5
	int NUM_OF_ITRESOURCE_OBJECTS=25; //4000
	int NUM_OF_USERS_TO_CREATE=1; //5
	int NUM_OF_LOOKUP_PER_ITRESOURCE=1; //1000
	int CORE_POOL_SIZE=NUM_OF_ITRESOURCE_OBJECTS;

	
	public OIMClient oimClient =null;	

	String FIND_LKU_QUERY = "select * from lku where lku_type_string_key=?";
	
	public static String BULK_INSERT_QUERY = "insert into lkv (lkv_key, " +
			"lku_key, lkv_encoded, lkv_decoded, " +
			"lkv_language, lkv_country, lkv_disabled,   " +
			"lkv_create, lkv_createby, lkv_update, lkv_updateby,  lkv_rowver)" +
			"values (lkv_seq.nextval, " +
			"?, ? ,?, " +
			"'en', 'US', '0'," +
			"sysdate, ?, sysdate, 1, hextoraw('0000000000000000'))" ;	
	public static final String ALLOW_AP_BASED_MULTIPLE_ACCOUNT_PROVISIONING = "XL.AllowAPBasedMultipleAccountProvisioning";
	public static final String ALLOW_AP_HARVESTING = "XL.AllowAPHarvesting";
	public static final String OIA_INTEGRATIONENABLED = "OIM.IsOIAIntegrationEnabled";
	
	
	private  BlockingQueue <Runnable> roleGrantQueue = new LinkedBlockingQueue<Runnable>();;
	private  ThreadPoolExecutor tpe = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE, 0, TimeUnit.SECONDS, roleGrantQueue, Executors.defaultThreadFactory());
	List<String> groupList = new ArrayList<String>();

	public RoleGrantMultiThreadTest() {
		String APPSERVER_TYPE = "wls";
		String AUTH_LOGIN_CONFIG="D:\\designconsole\\config\\authwl.conf";
		String XL_HOME_DIR = "D:\\designconsole";
		
		System.setProperty("APPSERVER_TYPE", APPSERVER_TYPE);
		System.setProperty("java.security.auth.login.config", AUTH_LOGIN_CONFIG);
		System.setProperty("XL.HomeDir", XL_HOME_DIR);
		
		try {
			
			oimClient = loginAsUser("xelsysadm", "Welcome1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws Exception {
		
		RoleGrantMultiThreadTest roleGrantMultiThreadTest = new RoleGrantMultiThreadTest();
		roleGrantMultiThreadTest.executeTask();
		System.out.println("RoleGrantMultiThreadTest.main(): COMPLETED*****");
		System.exit(0);
	}
	
	public void executeTask() throws InterruptedException{
		UserManager userManagerService =  (UserManager)oimClient.getService(UserManager.class);
		String userKey = "";
		// create user
		String prefix = "USER_"+generateUniqueID(6);
		try {
			userKey = createUser(prefix, userManagerService);
			createData();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Already created roles and policies and populated in groupList
		// role assignment to a single happens as a part of each thread execution
		
		Collection<Future<?>> futures = new LinkedList<Future<?>>();
//		String[] myStringArray = {"1100", "1101", "1104", "1105", "1106", "1109", "1110", "1112", "1113", "1114", "1115", "1122"};
//		groupList = Arrays.asList(myStringArray);
		
		for (int i = 0; i< groupList.size(); i++) {
			String roleKey = (String)groupList.get(i);
			System.out.println("RoleGrantMultiThreadTest.executeTask()userKey: "+userKey+" roleKey: "+roleKey);
			Runnable r = new RoleGrantWorker(userKey,roleKey);
			tpe.execute(r);
		}

		while (tpe.getTaskCount() != tpe.getCompletedTaskCount() && ! tpe.getQueue().isEmpty()){
			synchronized (tpe) {
				tpe.wait(1000);
			}
		}
		System.out.println("RoleGrantMultiThreadTest.executeTask(): COMPLETED");
		
	}	
	
	public void createData() throws Exception{
		
		System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): login done......");
		
		SystemConfigurationService sysConfigService = (SystemConfigurationService)oimClient.getService(SystemConfigurationService.class);
		SchedulerService schedulerService =  (SchedulerService)oimClient.getService(SchedulerService.class);
		
		setMultipleAccountProvisioningSupported(sysConfigService,Boolean.TRUE.toString());		
		setAllowAPHarvesting(sysConfigService,Boolean.TRUE.toString());
		setOIAIntegrationEnabled(sysConfigService,Boolean.FALSE.toString());
		
		Map<String, String> objectMap = new HashMap<String,String>();
		Map<String, List<String>> itResMap = new HashMap<String,List<String>>();
		Map<String, List<String>> itRes_LKVMap = null;
		
		
		Map<String, String> polKeyPolicyNameMap = new HashMap<String,String>();
		Map<String, String> roleAccessPolicyMap = new HashMap<String,String>();
		Map<String, List<ApplicationInstance>> appInstanceMap = new HashMap<String,List<ApplicationInstance>>();
//		String prefix = "";
		List<String> prefixList = new ArrayList<String>();
		
		// Generate 5 uniqueIDs/ ResourceObjects to import
		System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): Creating "+ NUM_OF_RESOURCE_OBJECTS+"  Resource Objects");
		for (int i = 0; i < NUM_OF_RESOURCE_OBJECTS; i++) {
			String prefix = generateUniqueID(6);
			prefixList.add(prefix);
			// import provisioning artifacts and get object key.
			String objKey = createResourceObject(prefix); 
			System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): Created ResourceObject: "+prefix+ " with objKey = "+objKey);
			objectMap.put(prefix, objKey);
			
			// this list contains the ITRes Keys ~ 20K Keys
			System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): Creating ITResource Object for ResourceObject "+prefix+ " with objKey = "+objKey);

			List<String> itResList = createITResources(prefix);

			itResMap.put(prefix, itResList);
		}
		
		//create lookup entries
		itRes_LKVMap = createLKVEntriesForITResource(prefixList,itResMap);
	
		System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): DONE creating LKV entries");
		// create application instances
		for (String uniqueID : prefixList) {
			String objKey = objectMap.get(uniqueID);
			List<ApplicationInstance> appInstanceList = createBulkAppInstances(uniqueID,objKey,itResMap);
			appInstanceMap.put(uniqueID, appInstanceList);
		}
		System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): DONE creating AppInstances");
		
		System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): START creating"+ NUM_OF_ITRESOURCE_OBJECTS+ " access policies");
		
		tcAccessPolicyOperationsIntf apIntf =  (tcAccessPolicyOperationsIntf)oimClient.getService(tcAccessPolicyOperationsIntf.class);
		
		
		// create access policies - one policy and 5 different resources. ie 5 distinct AppInstances
		for (int k = 0; k< NUM_OF_ITRESOURCE_OBJECTS; k++) {
			System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): creating Access policy: "+k);
			
			int resInEachPolicy = prefixList.size();
			String accessPolicyName = "Access_Policy_"+generateUniqueID(6);
			System.out.println(" Access Policy Name: "+accessPolicyName);
			
			Map<String, String> attributeListMap = getPolicyAttributeMap(accessPolicyName);; 
			long[] provObjKeys = new long[resInEachPolicy];
			PolicyNLAObjectActionType[] policyNLAObjectActionType = new PolicyNLAObjectActionType[resInEachPolicy];
			long[] denyObjKeys = null;
			
			AccessPolicyResourceData[] aprdArray = new AccessPolicyResourceData[resInEachPolicy];
			
			int i=0;
			for (String uniqueID : prefixList) {
				System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest():uniqueID: "+uniqueID+ " for i: "+i);
				
				policyNLAObjectActionType[i] = PolicyNLAObjectActionType.REVOKE;
				
				String objKey = objectMap.get(uniqueID);
				System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest():adding objKey: "+objKey);
				provObjKeys[i] = Long.parseLong(objKey);
					
				List<String> itResList = itResMap.get(uniqueID);
				String itResKey = itResList.get(k);
				
				List<String> lkvList = itRes_LKVMap.get(itResKey);
				
				String formName = "UD_" + uniqueID+"P";
		        String childFormName = "UD_" + uniqueID+"C1";
		        
		        long formKey = getFormKey(formName);
		        System.out.println("parent formName: " + formName+ " formKey: "+formKey);
		        
		        long childFormKey = getFormKey(childFormName);
		        System.out.println("child formName: " + childFormName+ " formKey: "+childFormKey);
		        
		        AccessPolicyResourceData aprd = new AccessPolicyResourceData(Long.parseLong(objKey), uniqueID, formKey, formName, "P");
		        aprd.setFormData(getAccessPolicyDefaultParentData(formName,itResKey,uniqueID));
		        setAccessPolicyDefaultChildData(childFormName,childFormKey,lkvList,aprd);
		        aprdArray[i] = aprd;
		        i++;
				
			}
			// create role - createGroup(prefix)
			createGroup(accessPolicyName);
			long grpKey = getGroupKey(accessPolicyName);
//			String[] myStringArray = {"1100", "1101", "1104", "1105", "1106", "1109", "1110", "1112", "1113", "1114", "1115", "1122"};
//			groupList = Arrays.asList(myStringArray);
			groupList.add(grpKey+"");
			
			long pol_key = apIntf.createAccessPolicy(attributeListMap, provObjKeys,policyNLAObjectActionType,denyObjKeys,new long[]{grpKey}, aprdArray);
			System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): policy: "+pol_key+" name: "+accessPolicyName +" created.");
			
			polKeyPolicyNameMap.put(accessPolicyName, pol_key+"");
			roleAccessPolicyMap.put(accessPolicyName, grpKey+"");
		}
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
		System.out.println("RoleGrantMultiThreadTest.createUser(): Created User: "+uniqueID+ " with userKey: "+userKey);
		return userKey;
	}
	
	public Map<String, List<String>> createLKVEntriesForITResource(
			List<String> prefixList, Map<String, List<String>> itResMap)
			throws Exception {
		
		System.out.println("RoleGrantMultiThreadTest.createLKVEntriesForITResource():START");
		PreparedStatement prepStatement = null;
		Map<String, List<String>> itRes_LKVMap = new HashMap<String, List<String>>();
		Map<String, String> lkuMap = new HashMap<String, String>();
		// create lookup entries for each ITResource -
		try {
			connection = getConnection();
			
			ResultSet rs = null;
			for (String uniqueID : prefixList) {
				List<String> itResList = itResMap.get(uniqueID);

				// check if LKU entry exists
				String typeStringKey = "Lookup." + uniqueID + ".C1.Group";
				System.out
						.println("RoleGrantMultiThreadTest.createLKVEntriesForITResource():typeStringKey: "+typeStringKey);
				prepStatement = connection.prepareStatement(FIND_LKU_QUERY);
				// check the lku entry
				prepStatement.setString(1, typeStringKey);

				rs = prepStatement.executeQuery();

				String lkuKey = null;
				if (rs != null && rs.next()) {
					lkuKey = rs.getString("lku_key");
					System.out.println("RoleGrantMultiThreadTest.createLKVEntriesForITResource(): LKU_KEY: "+ lkuKey);
					lkuMap.put(uniqueID, typeStringKey);
				}

				if (lkuKey != null && !lkuKey.equals("")) {
					prepStatement = connection.prepareStatement(BULK_INSERT_QUERY);
					int i = 0;
					List<String> lkvList = null;
					for (String itResourceKey : itResList) {
						System.out
								.println("RoleGrantMultiThreadTest.createLKVEntriesForITResource():  Creating "+NUM_OF_LOOKUP_PER_ITRESOURCE+ " LKV entries for ITResource: "+itResourceKey);
						for (int j = 0; j < NUM_OF_LOOKUP_PER_ITRESOURCE; j++) {
							String encoded = itResourceKey + "~" + uniqueID
									+ "~" + ++i;
							prepStatement.setString(1, lkuKey);
							prepStatement.setString(2, encoded);
							prepStatement.setString(3, encoded);
							prepStatement.setString(4, "1");
							prepStatement.addBatch();
							if (itRes_LKVMap.containsKey(itResourceKey)) {
								lkvList = itRes_LKVMap.get(itResourceKey);
							} else {
								lkvList = new ArrayList<String>();
							}
							lkvList.add(encoded);
							itRes_LKVMap.put(itResourceKey, lkvList);
						}
						prepStatement.executeBatch();
					}
				}
			}
		} catch (Exception e) {
			System.out.println("RoleGrantMultiThreadTest.RoleGrantMultiThreadTest(): Exception: "
					+ e.getMessage());
			throw e;
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
				if (prepStatement != null) {
					prepStatement.close();
				}
			} catch (SQLException sqle) {
				// D0 nothing
			}
		}
		System.out.println("RoleGrantMultiThreadTest.createLKVEntriesForITResource():END");
		return itRes_LKVMap;
	}
	
	public long getGroupKey(String grpName) throws Exception {

		tcGroupOperationsIntf grpIntf =  (tcGroupOperationsIntf)oimClient.getService(tcGroupOperationsIntf.class);
    	long grpKey = 0;
    	
    	HashMap hm = new HashMap();
    	hm.put("Groups.Group Name", grpName);
    	tcResultSet rs = grpIntf.findGroups(hm);
    	rs.goToRow(0);
    	grpKey = rs.getLongValue("Groups.Key");
    	
    	return grpKey;
    }	
	
	public static void executeQuery(String query) throws SQLException{
	}
	
	
	public static Connection getConnection() throws Exception {
		Class.forName(directDBdriver).newInstance();
		return DriverManager.getConnection(jdbcURL, userName, password);
	}	
	
    public void createGroup(String groupName) 
            throws Exception {
    		tcGroupOperationsIntf grpIntf =  (tcGroupOperationsIntf)oimClient.getService(tcGroupOperationsIntf.class);
        	HashMap hm = new HashMap();
        	hm.put("Groups.Group Name", groupName);
        	
        	grpIntf.createGroup(hm);
        }	
	
	public Map<String, String> getPolicyAttributeMap(String accessPolicyName){
       	Map<String,String> hm = new HashMap<String, String>();
    	hm.put("Access Policies.Name", accessPolicyName);
    	hm.put("Access Policies.Note", accessPolicyName + " NOTE");
    	hm.put("Access Policies.Description", accessPolicyName + " DESCRIPTION");
    	hm.put("Access Policies.By Request", "0");
    	hm.put("Access Policies.Retrofit Flag", "1");		
    	return hm;
	}
	public HashMap<String,String> getAccessPolicyDefaultParentData(String formName, String itResKey, String prefix ){
    	HashMap<String,String> formData = new HashMap<String,String>();
        formData.put(formName + "_ITRES", itResKey + "");
        formData.put(formName + "_LOGIN", itResKey + "_LOGIN_"+prefix);
        formData.put(formName + "_FIRST", itResKey + "_FIRST_"+prefix);
        formData.put(formName + "_LAST", itResKey + "_LAST_"+prefix);
    	return formData;
    }
    
    public HashMap<String,String> setAccessPolicyDefaultChildData(String childFormName, long childFormKey, List<String> lkvList, AccessPolicyResourceData aprd ){
        HashMap childFormData = null;
        for (String lkvEncoded : lkvList) {
        	System.out.println("Adding into child: "+childFormName+ " value:"+lkvEncoded );
        	childFormData = new HashMap();
        	childFormData.put(childFormName+"_GROUP",lkvEncoded);
        	aprd.addChildTableRecord(String.valueOf(childFormKey), childFormName, "Add", childFormData);
		}
        return childFormData;
    }

    
	public  long getFormKey(String formName) throws Exception {
		tcFormDefinitionOperationsIntf fdIntf =  (tcFormDefinitionOperationsIntf)oimClient.getService(tcFormDefinitionOperationsIntf.class);
    	long formKey = 0;
    	HashMap hm = new HashMap();
    	hm.put("Structure Utility.Table Name", formName);
    	tcResultSet rs = fdIntf.findForms(hm);
    	rs.goToRow(0);
    	formKey = rs.getLongValue("Structure Utility.Key");
    	return formKey;
    }    

	
	public List<ApplicationInstance> createBulkAppInstances(String prefix,String objKey,Map<String, List<String>> itResMap ) throws Exception{
		List<ApplicationInstance> appInstanceList = new ArrayList<ApplicationInstance>();
		 //Create App Instance
		
		List<String> itResList = itResMap.get(prefix);
		for (String itResKey : itResList) {
			ApplicationInstance newAppInstance = createAppInstance(prefix+"_"+itResKey,Long.parseLong(objKey), Long.parseLong(itResKey));
			System.out.println("RoleGrantMultiThreadTest.createBulkAppInstances():newAppInstance: "+newAppInstance);
			appInstanceList.add(newAppInstance);
		}
		return appInstanceList;
	}
	
	public ApplicationInstance createAppInstance(String prefix, long objKey, long itResKey) throws Exception{
		ApplicationInstanceService applicationInstanceService =  (ApplicationInstanceService)oimClient.getService(ApplicationInstanceService.class);
		//Create App Instance
        ApplicationInstance appInstanceToAdd = new ApplicationInstance(prefix, prefix,"Description for appinstance: "+prefix,objKey, itResKey,"", "" );
        ApplicationInstance newAppInstance = applicationInstanceService.addApplicationInstance(appInstanceToAdd);
		return newAppInstance;
	}

    public  List<String> createITResources(String prefix) 
            throws Exception {
    		
    		List<String> itResList = new ArrayList<String>();
    		tcITResourceInstanceOperationsIntf itinstIntf =  (tcITResourceInstanceOperationsIntf)oimClient.getService(tcITResourceInstanceOperationsIntf.class);
        	long itResKey = 0;
        	long itDefKey = getITResDefinitionKey(prefix);
        	System.out.println("RoleGrantMultiThreadTest.createITResources(): Creating "+NUM_OF_ITRESOURCE_OBJECTS+ " ITResource Objects");
        	for (int j = 0; j < NUM_OF_ITRESOURCE_OBJECTS; j++) {
        		HashMap<String, String> hm = new HashMap<String, String>();
            	hm.put("IT Resources Type Definition.Key", itDefKey + "");
            	hm.put("IT Resources.Name", prefix+"_"+j);
            	hm.put("login", "login_" + prefix);
            	hm.put("password", "pwd_" + prefix);
            	hm.put("port", "1000" + prefix);
            	hm.put("host", "host_" + prefix);
            	System.out.println("RoleGrantMultiThreadTest.createITResources(): creating ITResource with name: "+prefix+"_"+j);
            	itResKey = itinstIntf.createITResourceInstance(hm);
            	itResList.add(itResKey+"");
            	
			}
        	return itResList;
        }    

    public  long getITResDefinitionKey(String itResDefName) throws Exception {
		tcITResourceDefinitionOperationsIntf itdefIntf =  (tcITResourceDefinitionOperationsIntf)oimClient.getService(tcITResourceDefinitionOperationsIntf.class);
    	long itDefKey = 0;
    	
    	HashMap hm = new HashMap();
    	hm.put("IT Resources Type Definition.Server Type", itResDefName);
    	
    	tcResultSet rs = itdefIntf.getITResourceDefinition(hm);
    	rs.goToRow(0);
    	
    	itDefKey = rs.getLongValue("IT Resources Type Definition.Key");
    	return itDefKey;
    }

    
	public String createResourceObject(String prefix) throws Exception{
		File dmFile = findResource("SIMRES.xml");
		importXMLWithSubstitution(dmFile, "SIMRES", prefix);
		long objKey = getObjectKey(prefix);
		return objKey+"";
		
	}

	public  void importXMLWithSubstitution(File xmlFile, String existingPrefix, String newPrefix) throws Exception {
		System.out.println("RoleGrantMultiThreadTest.importXMLWithSubstitution(): client Object: "+oimClient+ " for thread: "+Thread.currentThread().getName());
		
		tcImportOperationsIntf tcImportIntf =  (tcImportOperationsIntf)oimClient.getService(tcImportOperationsIntf.class);
		String xmlFileName = xmlFile.getAbsolutePath();

		StringBuffer dmXml = new StringBuffer();
		FileReader fr = new FileReader(xmlFileName);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
			dmXml.append(line);
		}

		String newDmXml = dmXml.toString();

		newDmXml = newDmXml.replaceAll(existingPrefix, newPrefix);

		tcImportIntf.acquireLock(true);
		Collection items = tcImportIntf.addXMLFile(xmlFileName, newDmXml);
		tcImportIntf.performImport(items);
	}
	
	public  File findResource(String fileName) throws MalformedURLException {
		//first see if the resource is a plain file
		File file = null;
		URL url = null;
		File f = new File(fileName);
		if (f.exists()) {
			file = f;
			url = f.toURI().toURL();
			return file;
		}
		//search for the resource on the classpath
		//get the default class/resource loader 
		ClassLoader cl = getClass().getClassLoader();
		url = cl.getResource(fileName);
		if (url != null) {
			file = new File(url.getFile());
		}
		return file;
	}
	public  String generateUniqueID(int uniqueIDLength) {
		Random r = new Random();
		String uniqueID = (Long.toString(Math.abs(r.nextLong()), 36)).substring(0, uniqueIDLength);
		
		return uniqueID.toUpperCase();
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
	
    public  long getObjectKey(String objName) throws Exception {
		tcObjectOperationsIntf objIntf =  (tcObjectOperationsIntf)oimClient.getService(tcObjectOperationsIntf.class);
    	long objKey = 0;
    	
    	HashMap hm = new HashMap();
    	hm.put("Objects.Name", objName);
    	tcResultSet rs = objIntf.findObjects(hm);
    	rs.goToRow(0);
    	objKey = rs.getLongValue("Objects.Key");
    	
    	return objKey;
    }	
	
    // nameOfScheduleJob= "Evaluate User Policies"
    // nameOfScheduleJob = "Entitlement List"
    // nameOfScheduleJob = "Catalog Synchronization Job"
	public void runScheduledJob(SchedulerService schedulerService, String nameOfScheduleJob) throws Exception {
		System.out.println("RoleGrantMultiThreadTest.runScheduledJob(): running job: "+nameOfScheduleJob);
		Thread.sleep(2000);
		schedulerService.triggerNow(nameOfScheduleJob);
		Thread.sleep(8000);
		
		
	}	
	
    public static void setMultipleAccountProvisioningSupported(SystemConfigurationService sysConfigService, 
    		String multipleAccountProvisioningSupported) throws Exception {
		try {
			SystemProperty multipleAccountProvisioningAllowedSysProp = 
				sysConfigService.getSystemProperty(ALLOW_AP_BASED_MULTIPLE_ACCOUNT_PROVISIONING);
			multipleAccountProvisioningAllowedSysProp.setPtyValue(multipleAccountProvisioningSupported);
			sysConfigService.updateSystemProperty(multipleAccountProvisioningAllowedSysProp, new Date());
		} catch (SystemConfigurationServiceException e) {
			
		}
    }	
    
    public static void setAllowAPHarvesting(SystemConfigurationService sysConfigService, 
    		String allowAPHarvesting) throws Exception {
		try {
			SystemProperty allowHarvestingSysProp = sysConfigService.getSystemProperty(ALLOW_AP_HARVESTING);
			allowHarvestingSysProp.setPtyValue(allowAPHarvesting);
			sysConfigService.updateSystemProperty(allowHarvestingSysProp, new Date());
		} catch (SystemConfigurationServiceException e) {
			
		}
    }
    
    public static void setOIAIntegrationEnabled(SystemConfigurationService sysConfigService, 
    		String isOIAIntegrationEnabled) throws Exception {
		try {
			SystemProperty OIAIntegrationEnabled = sysConfigService.getSystemProperty(OIA_INTEGRATIONENABLED);
			OIAIntegrationEnabled.setPtyValue(isOIAIntegrationEnabled);
			sysConfigService.updateSystemProperty(OIAIntegrationEnabled, new Date());
		} catch (SystemConfigurationServiceException e) {
			
		}
    }	    

}
