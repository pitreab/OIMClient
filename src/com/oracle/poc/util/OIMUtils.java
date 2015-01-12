package com.oracle.poc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.usermgmt.vo.UserManagerResult;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.api.EntitlementService;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.api.ProvisioningServiceInternal;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ChildTableRecord;
import oracle.iam.provisioning.vo.Entitlement;
import oracle.iam.provisioning.vo.FormField;
import oracle.iam.provisioning.vo.FormInfo;
import oracle.iam.scheduler.api.SchedulerService;
import Thor.API.tcResultSet;
import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcAccessPolicyOperationsIntf;
import Thor.API.Operations.tcFormDefinitionOperationsIntf;
import Thor.API.Operations.tcGroupOperationsIntf;
import Thor.API.Operations.tcITResourceDefinitionOperationsIntf;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;
import Thor.API.Operations.tcImportOperationsIntf;
import Thor.API.Operations.tcLookupOperationsIntf;
import Thor.API.Operations.tcObjectOperationsIntf;

import com.thortech.xl.vo.AccessPolicyResourceData;

public class OIMUtils {
	public static OIMUtils oimUtils= new OIMUtils();
	public OIMClient oimClient =null;
	public static final int uniqueIDLength = 4;
	public static final String ORGANIZATION_PREFIX = "ORG_";
	public static final String SUFFIX = "_AP";
	public static final String ORGANIZATION_NAME = "Organizations.Organization Name";
	public static final String ORGANIZATION_KEY = "Organizations.Key";
	public static final String USER_PREFIX = "USR_";
	public static final String USER_PASSWORD="Welcome1";
	public static final String hostName = "slc03qyd.us.oracle.com";


	public static final String port = "14001";
	
	// I need to make it singleton: later
	private OIMUtils() {
		try {
			oimClient = loginAsUser("xelsysadm", "Welcome1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static OIMUtils getInstance(){
		if(oimUtils==null){
			oimUtils = new OIMUtils(); 
		}
		return oimUtils;
	}
	
	public ApplicationInstance createApplicationInstance(String appInstanceName, String appInstanceDesc) throws Exception{
		 tcITResourceDefinitionOperationsIntf itResourceDefinitionOperationsService = null;
		 tcITResourceInstanceOperationsIntf itResourceInstanceOperationsService = null;
		 ApplicationInstanceService applicationInstanceService = null;
		 
		applicationInstanceService = (ApplicationInstanceService)oimClient.getService(ApplicationInstanceService.class);
		itResourceInstanceOperationsService = (tcITResourceInstanceOperationsIntf)oimClient.getService(tcITResourceInstanceOperationsIntf.class);
		itResourceDefinitionOperationsService = (tcITResourceDefinitionOperationsIntf)oimClient.getService(tcITResourceDefinitionOperationsIntf.class);
		
		// Find Existing App Instance
		ApplicationInstance appInstance = applicationInstanceService.findApplicationInstanceByName("DemoAppInstance");
		System.out.println("OIMUtils.createApplicationInstance()-->"+appInstance.getItResourceKey());

		// Get ITResource Key
		long itResourceKey = appInstance.getItResourceKey();
		
		// Find ITResource Details
		Map<String, String> iTResMap = new HashMap<String, String>(); 
		iTResMap.put("IT Resource.Key", itResourceKey+"");
		tcResultSet rs = itResourceInstanceOperationsService.findITResourceInstances(iTResMap);
		String itResName = rs.getStringValue("IT Resource.Name");
		String itResTypeDef = rs.getStringValue("IT Resource Type Definition.Server Type");
		System.out.println("OIMUtils.createApplicationInstance()itResTypeDef: "+itResTypeDef);

		// Find ITResource Parameters
		tcResultSet rs1 = itResourceInstanceOperationsService.getITResourceInstanceParameters(itResourceKey);
		// Populate Original ITResource Parameters
		Map<String, String> iTResAttributeMap = new HashMap<String, String>(); 
		String itResParamName=null;
		String itResParamValue=null;
		for (int i = 0; i < rs1.getRowCount(); ++i) {
			rs1.goToRow(i);
			itResParamName = rs1.getStringValue("IT Resources Type Parameter.Name");
			itResParamValue= rs1.getStringValue("IT Resource.Parameter.Value");
			iTResAttributeMap.put(itResParamName, itResParamValue);
		}
		
		// Find ITResource Definition Key
		iTResMap = new HashMap<String, String>();
		iTResMap.put("IT Resource Type Definition.Server Type",itResTypeDef);
		rs = itResourceDefinitionOperationsService.getITResourceDefinition(iTResMap);
		rs.goToRow(0);
		String itResTypeDefKey = rs.getStringValue("IT Resources Type Definition.Key");

		// Create new ITResource Instance using ITResource Name and ITResource Definition Key
		String uniqID = generateUniqueID();		
		iTResMap.put("IT Resources.Name", itResName+"_"+uniqID);
		iTResMap.put("IT Resources Type Definition.Key", itResTypeDefKey);
		System.out.println("OIMUtils.createApplicationInstance():uniqID: "+uniqID+ " itResTypeDefKey: "+itResTypeDefKey);

		long newItResourceKey = itResourceInstanceOperationsService.createITResourceInstance(iTResMap);
       System.out.println("OIMUtils.createApplicationInstance():newItResourceKey: "+newItResourceKey);
       
       // Update ITResource Parameters Using existing parameters
       itResourceInstanceOperationsService.updateITResourceInstanceParameters(newItResourceKey,iTResAttributeMap);

       // Using this ITResource Key create a new Application Instance
       ApplicationInstance appInstanceToAdd = new ApplicationInstance(appInstanceName, appInstanceName,appInstanceDesc, appInstance.getObjectKey(), newItResourceKey,appInstance.getDataSetName(), "" );
       ApplicationInstance newAppInstance = applicationInstanceService.addApplicationInstance(appInstanceToAdd);
       return newAppInstance;
       
	}
	
//	public static ApplicationInstance addApplicationInstance(String appInstanceName, String appInstanceDesc) throws Exception{
//		ApplicationInstanceService applicationInstanceService = null;
//		OIMClient oimClient = loginAsUser("xelsysadm", "Welcome1");
//		applicationInstanceService = (ApplicationInstanceService)oimClient.getService(ApplicationInstanceService.class);
//		ApplicationInstance newAppInstance = applicationInstanceService.addApplicationInstance(appInstanceName, appInstanceDesc, "DemoAppInstance");
//		
//		 return newAppInstance;
//	}

	public List<Entitlement> findEntitlement() throws Exception{
		EntitlementService entitlementService = null;
		SearchCriteria crit = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.OBJ_KEY.getId(), "84", SearchCriteria.Operator.EQUAL);
		entitlementService = (EntitlementService)oimClient.getService(EntitlementService.class);
		List<Entitlement> entitlementList = entitlementService.findEntitlements(crit, null);
		System.out.println("OIMUtils.findEntitlement():entitlementList size: "+entitlementList.size());
		 return entitlementList;
	}
		
	public void getAccountDetails(long accountID) throws Exception{
		ProvisioningService provisioningService = null;
		provisioningService = (ProvisioningService)oimClient.getService(ProvisioningService.class);
		Account account = provisioningService.getAccountDetails(11);
		Map<String, ArrayList<ChildTableRecord>>  childDataMap = account.getAccountData().getChildData();
		List<FormInfo> childForms = account.getAppInstance().getChildForms();
		String childFormName="";
		for (FormInfo childForm : childForms) {
			List<ChildTableRecord> retainedChildRecords = new ArrayList<ChildTableRecord>();
			childFormName = childForm.getName();
			List<FormField> formFieldList = childForm.getFormFields();
			for (FormField formField : formFieldList) {
				String formFieldName ="";
				if(isEntitlementField(formField)){
					formFieldName = formField.getName();
					ArrayList<ChildTableRecord> childRecords = childDataMap.get(childFormName);
					
					for (ChildTableRecord childTableRecord : childRecords) {
						Map<String, Object> childData = childTableRecord.getChildData();
						if (childData!=null & childData.containsKey(formFieldName)){
							//populate child records in List
							retainedChildRecords.add(childTableRecord);
						}
					}					
				}
			}

			
		}
	}
	
	private boolean isEntitlementField(FormField formField){
		if(formField.getProperties()!=null 
				&& formField.getProperty("Entitlement")!=null 
				&& ((String)formField.getProperty("Entitlement")).equalsIgnoreCase("true")
				){
			return true;
		}
		return false;
	
	}
	
	public String generateUniqueID() {
		Random r = new Random();
		String uniqueID = (Long.toString(Math.abs(r.nextLong()), 36)).substring(0, uniqueIDLength);
		return uniqueID.toUpperCase();
	}
	
	public static OIMClient loginAsUser(String userName, String password)
	throws Exception {
		OIMClient oimClient = new OIMClient(getEnvironment());
		oimClient.login(userName, password.toCharArray());
		return oimClient;
	}
	
//	public static Hashtable<String, String> getEnvironment(){
//		String ctxFactory;
//		String hostName;
//		String port;
//		String serverURL;
//		Hashtable<String, String> env = new Hashtable<String, String>();		
//		ctxFactory = "weblogic.jndi.WLInitialContextFactory";
//		hostName = "adc4120297.us.oracle.com";
//		port = "8003";
//		serverURL = "t3://" + hostName + ":" + port;		
//		env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, serverURL);
//		env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, ctxFactory);
//		env.put(OIMClient.APPSERVER_TYPE_WEBLOGIC, "wls");
//		
//		return env;		
//	}
	
	public static Hashtable<String, String> getEnvironment(){
		String ctxFactory;
		String serverURL;
		Hashtable<String, String> env = new Hashtable<String, String>();		
		ctxFactory = "weblogic.jndi.WLInitialContextFactory";
		serverURL = "t3://" + hostName + ":" + port;		
		env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, serverURL);
		env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, ctxFactory);
		env.put(OIMClient.APPSERVER_TYPE_WEBLOGIC, "wls");
		System.out.println("OIMUtils.getEnvironment(): host: "+hostName+" port: "+port);
		return env;		
	}	
	public <T> T getService(Class<T> serviceClass, OIMClient oimClient) throws Exception{
		return oimClient.getService(serviceClass);
	}	

	public static void printResultSet(tcResultSet rs) throws Exception {

		System.out.println("COUNT = " + rs.getRowCount() + "\n\n");
		String[] cols = rs.getColumnNames();

		for (int i = 0; i < rs.getRowCount(); ++i) {
			rs.goToRow(i);

			for (int j = 0; j < cols.length; j++) {
				if (cols[j].indexOf("Row Version") == -1) {
					System.out.println(cols[j] + "\t\t:"
							+ rs.getStringValue(cols[j]));
				}
			}
			System.out.println();
		}
	}
	
	
	public  Account getAccount(long oiuKey) throws Exception {
		ProvisioningService provisioningService = null;
		provisioningService = (ProvisioningService)oimClient.getService(ProvisioningService.class);
		Account account = provisioningService.getAccountDetails(1);
		System.out.println("OIMUtils.getProcessFormData():account: "+account);
		return account;
		
	}

	
    public void createGroup(String groupName) 
            throws Exception {
    		tcGroupOperationsIntf grpIntf =  (tcGroupOperationsIntf)oimClient.getService(tcGroupOperationsIntf.class);
        	HashMap hm = new HashMap();
        	hm.put("Groups.Group Name", groupName);
        	
        	grpIntf.createGroup(hm);
        }	
    
	public long  createUser(String uniqueID) throws Exception {
		UserManager userManagerService =  (UserManager)oimClient.getService(UserManager.class);
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
		attrs.put("usr_password", USER_PASSWORD);
		attrs.put("User Login", userLogin);
		attrs.put("Xellerate Type", "End-User");
		attrs.put("Role", "Full-Time");
		result = userManagerService.create(new User(null,attrs)); 
		
		userKey = result.getEntityId();
		return Long.parseLong(userKey);
	}	
	
	
	public File findResource(String fileName) throws MalformedURLException {
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
	
	
	public void importXML(File xmlFile) throws Exception {
		
		tcImportOperationsIntf tcImportIntf =  (tcImportOperationsIntf)oimClient.getService(tcImportOperationsIntf.class);

		String xmlFileName = xmlFile.getAbsolutePath();
		System.out.println("importXML()..Start Import of file: "+ xmlFileName);

		StringBuffer dmXml = new StringBuffer();
		FileReader fr = new FileReader(xmlFileName);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
			dmXml.append(line);
		}
		tcImportIntf.acquireLock(true);
		Collection items = tcImportIntf.addXMLFile(xmlFileName, dmXml
				.toString());
		tcImportIntf.performImport(items);
		System.out.println("importXML(): Completed import of file without error");

	}	
	
	public void importXMLWithSubstitution(File xmlFile, String existingPrefix, String newPrefix) throws Exception {
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
	
	public ApplicationInstance createAppInstance(String prefix, long objKey, long itResKey) throws Exception{
		ApplicationInstanceService applicationInstanceService =  (ApplicationInstanceService)oimClient.getService(ApplicationInstanceService.class);
		//Create App Instance
        ApplicationInstance appInstanceToAdd = new ApplicationInstance(prefix, prefix,"Description for appinstance: "+prefix,objKey, itResKey,"", "" );
        ApplicationInstance newAppInstance = applicationInstanceService.addApplicationInstance(appInstanceToAdd);
		return newAppInstance;
	}
	
	public long setAccessPolicyConfiguration(String prefix, long grpKey, int childTableRecords) throws Exception{
		
		File dmFile = findResource("accesspolicy_single_itres_accntdiscr_harvesting_template.xml");
		String formName = "UD_" + prefix;
        String childFormName = "UD_CHI_"+prefix ;
        
        importXMLWithSubstitution(dmFile, "XXXX", prefix);
        
        long objKey = getObjectKey(prefix);
        System.out.println("OIMUtils.setAccessPolicyConfiguration():objKey: "+objKey);
        
        long formKey = getFormKey(formName);
        System.out.println("OIMUtils.setAccessPolicyConfiguration():formKey: "+formKey);
        
        long childFormKey = getFormKey(childFormName);
        System.out.println("OIMUtils.setAccessPolicyConfiguration():childFormKey: "+childFormKey);
        
        long itResKey = createITResource(prefix);
        System.out.println("OIMUtils.setAccessPolicyConfiguration():itResKey: "+itResKey);
        
        //Create App Instance
        ApplicationInstance newAppInstance = createAppInstance(prefix,objKey, itResKey);
        System.out.println("OIMUtils.setAccessPolicyConfiguration(): created AppInstance: "+newAppInstance.getApplicationInstanceName());
        
        AccessPolicyResourceData aprd = new AccessPolicyResourceData(objKey, prefix, formKey, formName, "P");
        HashMap formData = new HashMap();
        formData.put(formName + "_SERVER", itResKey + "");
        formData.put(formName + "_FIRST", itResKey + "_FIRST_"+prefix);
        formData.put(formName + "_LAST", itResKey + "_LAST_"+prefix);
        
        aprd.setFormData(formData);
        
        HashMap childFormData =null;
        for (int i = 0; i < childTableRecords; i++) {
        	System.out.println("OIMUtils.setAccessPolicyConfiguration(): created "+i+"th childrecord..");
        	childFormData = new HashMap();
            childFormData.put(childFormName+"_GROUP","group_run_"+prefix+"_"+i);
            childFormData.put(childFormName+"_LOCATION","loc_run_"+prefix+"_"+i);
            aprd.addChildTableRecord(String.valueOf(childFormKey), childFormName, "Add", childFormData);        
		}
        
        System.out.println("OIMUtils.setAccessPolicyConfiguration(): calling API to crete policy.."+prefix);
        long polKey = createAccessPolicy(prefix, new long[]{objKey}, new boolean[]{true},new long[0], new long[]{grpKey}, new AccessPolicyResourceData[]{aprd});
        System.out.println("Created policy name=" + prefix + " key=" + polKey);
        return polKey;
        		
	}
	
	public void findAccessPolicy() throws Exception{
		tcAccessPolicyOperationsIntf apIntf =  (tcAccessPolicyOperationsIntf)oimClient.getService(tcAccessPolicyOperationsIntf.class);
    	HashMap hm = new HashMap();
    	hm.put("Access Policies.Key", "3");
    	tcResultSet rs = apIntf.findAccessPolicies(hm);
    	rs.goToRow(0);
    	String name = rs.getStringValue("Access Policies.Name");
    	System.out.println("OIMUtils.findAccessPolicy():name: "+name);
//    	printResultSet(rs);
		
	}
    public long createAccessPolicy(String apName, long[] provObjKeys, boolean[] revokeIfNotApply, long[] denyObjKeys, 
            long[] grpKeys, AccessPolicyResourceData[] aprd) 
        	throws Exception {
    		
    		tcAccessPolicyOperationsIntf apIntf =  (tcAccessPolicyOperationsIntf)oimClient.getService(tcAccessPolicyOperationsIntf.class);
        	
        	HashMap hm = new HashMap();
        	hm.put("Access Policies.Name", apName);
        	hm.put("Access Policies.Note", apName + " NOTE");
        	hm.put("Access Policies.Description", apName + " DESCRIPTION");
        	hm.put("Access Policies.By Request", "0");
        	hm.put("Access Policies.Retrofit Flag", "1");

        	long apKey = apIntf.createAccessPolicy(hm, provObjKeys, revokeIfNotApply, denyObjKeys, grpKeys, aprd);
        	
        	return apKey;
        	
        }
    
    public  tcResultSet getLookupValues(String lookupName) throws Exception {
		tcLookupOperationsIntf  lookupOperationsIntf= (tcLookupOperationsIntf)oimClient.getService(tcLookupOperationsIntf.class);
    	tcResultSet rs = lookupOperationsIntf.getLookupValues(lookupName,1,10000);
    	return rs;
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
	
    public  long createITResource(String itResName) 
            throws Exception {
    		tcITResourceInstanceOperationsIntf itinstIntf =  (tcITResourceInstanceOperationsIntf)oimClient.getService(tcITResourceInstanceOperationsIntf.class);

        	long itResKey = 0;
        	long itDefKey = getITResDefinitionKey(itResName);
        		
        	HashMap hm = new HashMap();
        	hm.put("IT Resources Type Definition.Key", itDefKey + "");
        	hm.put("IT Resources.Name", itResName);
        	hm.put("p", "ppp_" + itResName);
        	hm.put("q", "qqq_" + itResName);
        	hm.put("r", "rrr_" + itResName);
        	
        	itResKey = itinstIntf.createITResourceInstance(hm);
        	return itResKey;
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
    
    	public void addUserToGroup(long usrKey, long grpKey) throws Exception {
    		tcGroupOperationsIntf grpIntf =  (tcGroupOperationsIntf)oimClient.getService(tcGroupOperationsIntf.class);
        	addUserToGroup(usrKey, grpKey, true);
        }

        public void addUserToGroup(long usrKey, 
            long grpKey, boolean evalPolicies) throws Exception {
        	tcGroupOperationsIntf grpIntf =  (tcGroupOperationsIntf)oimClient.getService(tcGroupOperationsIntf.class);
        	grpIntf.addMemberUser(grpKey, usrKey, evalPolicies);
        }
        
    	public void runPolicyEvaluationScheduledJob() throws Exception {
    		SchedulerService schedulerService =  (SchedulerService)oimClient.getService(SchedulerService.class);
    		Thread.sleep(2000);
    		schedulerService.triggerNow("Evaluate User Policies");
    		//schedulerService.triggerNow("Entitlement List");
    		//schedulerService.triggerNow("Catalog Synchronization Job");
    		Thread.sleep(8000);
    		
    		
    	}	
        
        public String covertMilliSecToReadbleFormat(long durationInMillis) {
            String res = "";
            long days  = TimeUnit.MILLISECONDS.toDays(durationInMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(durationInMillis)
                           - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(durationInMillis));
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis)
                             - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(durationInMillis));
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis)
                           - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationInMillis));
            if (days == 0) {
              res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            }
            else {
              res = String.format("%dd%02d:%02d:%02d", days, hours, minutes, seconds);
            }
            return res;
          }  	
        
        
        public Map<String, List<Account>> searchAccount(SearchCriteria sc){
    		ProvisioningServiceInternal provServiceInternal =  (ProvisioningServiceInternal)oimClient.getService(ProvisioningServiceInternal.class);
        	Map<String, List<Account>> userAccountListMap = null;
        	Map<String, String> controls = new HashMap<String, String>();
        	userAccountListMap = provServiceInternal.searchAccounts(sc, controls);
        	printMap(userAccountListMap);
        	return userAccountListMap;
        }
        

        
        public void printMap(Map map) {
            Set keys = map.keySet(); 
            Iterator keyIter = keys.iterator();
            while (keyIter.hasNext()) {
                Object key = keyIter.next(); 
                Object value = map.get(key); 
                System.out.println(key+ "\t\t:"+ value);
                if(value instanceof List){
                	for (Object object : (List)value) {
						if(object instanceof Account){
							System.out.println("Account is: "+((Account)object).toString());
						}else{
							System.out.println("object is: "+object);
						}
					}
                	
                }else {
                	System.out.println(key+ "\t\t:"+ value);
                }
                
            }
        }


    	
    
}

