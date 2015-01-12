package com.oracle.poc.impl;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import oracle.iam.platform.OIMClient;
import oracle.iam.provisioning.api.ApplicationObjectclassService;
import oracle.iam.provisioning.api.ApplicationService;
import oracle.iam.provisioning.api.ConnectorInfoService;
import oracle.iam.provisioning.api.ConnectorServerService;
import oracle.iam.provisioning.api.ManagedObjectService;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.Application;
import oracle.iam.provisioning.vo.ApplicationConfigurationManagerResult;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ApplicationObjectclass;
import oracle.iam.provisioning.vo.ApplicationObjectclass.RECON_RESPONSES;
import oracle.iam.provisioning.vo.ApplicationObjectclass.RECON_SITUATIONS;
import oracle.iam.provisioning.vo.ApplicationObjectclass.SCRIPT_TRIGGER;
import oracle.iam.provisioning.vo.ApplicationObjectclassCapability;
import oracle.iam.provisioning.vo.Attribute;
import oracle.iam.provisioning.vo.AttributeBuilder;
import oracle.iam.provisioning.vo.BasicAttribute;
import oracle.iam.provisioning.vo.ComplexAttribute;
import oracle.iam.provisioning.vo.ConnectorConfiguration;
import oracle.iam.provisioning.vo.ConnectorConfigurationProperty;
import oracle.iam.provisioning.vo.ConnectorInfo;
import oracle.iam.provisioning.vo.ConnectorKey;
import oracle.iam.provisioning.vo.ConnectorServer;
import oracle.iam.provisioning.vo.EmbeddedObjectBuilder;
import oracle.iam.provisioning.vo.Grant;
import oracle.iam.provisioning.vo.ScriptConfig;
import oracle.iam.provisioning.vo.Grant.GrantMechanism;
import oracle.iam.provisioning.vo.ProvConfig;
import oracle.iam.provisioning.vo.ProvisioningOperationResult.STATUS;
import oracle.iam.provisioning.vo.ManagedObject;
import oracle.iam.provisioning.vo.ProvisioningOperationResult;
import oracle.iam.provisioning.vo.Application.STATUS_TYPE;
import oracle.iam.provisioning.vo.ReconConfig;


public class Demo {

	static final String ALPHABETS = "abcdefghijklmnopqrstuvwxyz";
	static OIMClient oimClient;
	static ConnectorServer cs;
	static String port = "389";
	static String host = "140.84.132.147";
	static String adminLogin = "adlrgadmin";
	static String adminPassword = "Welcome1";
	static String container = "DC=adlrg,DC=us,DC=oracle,DC=com";
	static String domainName = "adlrg.us.oracle.com";
	static ConnectorInfoService connectorInfoService = null;
	static ApplicationService applicationService = null;
	static ApplicationObjectclassService applicationObjectclassService = null;
	static ManagedObjectService managedObjectService = null;
	static ProvisioningService provisioningService = null;


	public static void init() throws Exception {
		Hashtable env = new Hashtable();
		//env.put("java.naming.provider.url", "t3://adc2120649:14001/oim");
		env.put("java.naming.provider.url", "t3://slc01nba:14001/oim");
		env.put("java.naming.factory.initial",
				"weblogic.jndi.WLInitialContextFactory");

		oimClient = new OIMClient(env);
		oimClient.login("xelsysadm", "Welcome1".toCharArray());
		
		connectorInfoService = oimClient.getService(ConnectorInfoService.class);
		applicationService = oimClient.getService(ApplicationService.class);
		managedObjectService = oimClient.getService(ManagedObjectService.class);
		applicationObjectclassService = oimClient.getService(ApplicationObjectclassService.class);
		provisioningService = oimClient.getService(ProvisioningService.class);
	}

	public static void main(String[] args) throws Exception {
		init();

		String op = args[0];
		
		switch (op) {
		case "discoverInstalledBundles":
			discoverInstalledBundles();
			break;
			
		case "introspectObjectclass":
			if (args.length != 2){
				System.out.println("usage: introspectObjectclass connectorName objectclassName");
			}
			introspectObjectclass(args[1], args[2]);
			break;
			
		case "discoverTargets":
			if (args.length != 2){
				System.out.println("usage: discoverConnectorServer connectorServerName");
			}
			discoverTargets(args[1]);
			break;
		case "createApplication":
			if (args.length != 4){
				System.out.println("usage: createApplication bundleName applicationName flatfileLocation");
			}
			createApplication(args[1],args[2],args[3]);
			break;
		case "createApplicationObjectclass":
			if (args.length != 3){
				System.out.println("usage: createApplicationObjectclass targetObjectclassName applicationName");
			}
			createApplicationObjectclass(args[1], args[2]);
			break;
		case "createApplicationWithObjectclasses":
			if (args.length != 3){
				System.out.println("usage: createApplicationWithObjectclasses bundleName prefix");
			}
			createApplicationWithObjectclasses(args[1], args[2]);
			break;
		case "onboardMultipleApplications":
			if (args.length != 3){
				System.out.println("usage: onboardMultipleApplications prefix directoryName");
			}
			onboardMultipleApplications(args[1], args[2]);
			break;

		case "createManagedObject":
			if (args.length != 3){
				System.out.println("usage: createManagedObject aocName objName");
			}
			createObject(args[1], args[2]);
			break;
		case "deleteManagedObject":
			if (args.length != 3){
				System.out.println("usage: deleteManagedObject uid aocName");
			}
			deleteObject(args[1], args[2]);
			break;
		case "provisionAccount":
			if (args.length != 3){
				System.out.println("usage: provisionAccount aocName name");
			}
			createAccount(args[1], args[2]);
			break;
		case "provisionMultipleAccount":
			if (args.length != 4){
				System.out.println("usage: provisionMultipleAccount aocName prefix numAccounts");
			}
			createMultipleAccount(args[1], args[2], Integer.parseInt(args[3]));
			break;			
		case "enableAccount":
			if (args.length != 2){
				System.out.println("usage: enableAccount id");
			}			
			enableAccount(args[1]);
			break;
		case "disableAccount":
			if (args.length != 2){
				System.out.println("usage: disableAccount id");
			}	
			disableAccount(args[1]);
			break;
		case "revokeAccount":
			if (args.length != 2){
				System.out.println("usage: revokeAccount id");
			}	
			revokeAccount(args[1]);
			break;
		default:
			System.out.println("Operation " + op + " not suppoprted!");
			break;
		}


		shutdown();

	}
	private static void createMultipleAccount(String aocName, String prefix,
			int numAccounts)  throws Exception{
		
		for (int i=0 ; i<numAccounts ; i++) {
			String accountName = prefix + i;
			createAccount(aocName, accountName);
		}
		
	}

	private static void introspectObjectclass(String connectorName, String objectclassName) throws Exception {
		ConnectorInfo connectorInfo = connectorInfoService
				.getAllConnectorsForType(connectorName).get(0);
	
		ConnectorConfiguration connectorConfig = connectorInfo.getConfigProperties();
		LinkedHashMap<String, ConnectorConfigurationProperty> connectorConfigProperties = connectorConfig
				.getConfigProperties();
		List<ConnectorConfigurationProperty> bundleConfig = new LinkedList<ConnectorConfigurationProperty>();
		
		// Set ConfigurationProperty values for the connector
		HashMap<String, String> hmValues = getConnectorConfigPropValues(connectorName,null);

	
		for (String propName : connectorConfigProperties.keySet()) {
			ConnectorConfigurationProperty connectorConfigProperty = connectorConfigProperties
					.get(propName);
			// System.out.println("Setting property value for " + propName
			// + " with type " + connectorConfigProperty.getType());
			if (hmValues.containsKey(propName)) {
				connectorConfigProperty.setValue((String) hmValues
						.get(propName));
				bundleConfig.add(connectorConfigProperty);
			}
		}
		
		Application application = new Application();
		application.setName("test");
		application.setDescription("Testing");
		application.setOwnerType("User");
		application.setOwnerId(UUID.randomUUID().toString());
		application.setBundleName(connectorInfo.getConnectorKey().getBundleName());
		application.setBundleConfig(bundleConfig);
		application.setBundleVersion(connectorInfo.getConnectorKey().getBundleVersion());
		application.setConnectorDisplayName(connectorInfo.getConnectorKey().getConnectorName());
		application.setConnectorName(connectorInfo.getConnectorKey().getConnectorName());
		application
				.setConnectorBundleURI(connectorInfo.getConnectorBundleURI());
		application.setConnectorServer(connectorInfo.getConnectorServer());
		application.setStatus(STATUS_TYPE.Active);
		
		List<ApplicationObjectclass> aocList = applicationService
				.getSupportedApplicationObjectclasses(application);
		
		for (ApplicationObjectclass appOC : aocList) {
			if (!appOC.getTargetObjectclass().equals(objectclassName)) 
				continue;
			
			System.out.println("Discovered objectclass schema: ");
			Set<BasicAttribute> schema = appOC.getSchemaAttrs();
			for (BasicAttribute attr : schema) {
				System.out.println("\t" + attr.getName() + " " + attr.getType());
			}

			System.out.println("----------------------------------------------");
			System.out.println("Discovered objectclass properties: ");
			Map<String, String> appOCProperties = appOC.getProvConfig().getAppOCProperties();
			for (String propName : appOCProperties.keySet()) {
				System.out.println("\t" + propName + " " + appOCProperties.get(propName));
			}
			
			System.out.println("----------------------------------------------");
			System.out.println("Discovered objectclass capabilities: ");
			List<ApplicationObjectclassCapability> capabilities = appOC.getProvConfig().getCapabilities();
			for (ApplicationObjectclassCapability cap : capabilities) {
				System.out.println("\t" + cap.getProvisioningOperation() + " " + cap.getOperationPolicy().toString());
			}			
			
		}

		
	}

	private static void onboardMultipleApplications(String applicationPrefix,
			String directoryName) throws Exception {
		
		File dir = new File(directoryName);
		File[] configFiles = dir.listFiles();
		
		for (File configFile : configFiles) {
			String appOCPrefix = configFile.getName().substring(0, configFile.getName().indexOf("."));
			String applicationName = applicationPrefix + "-" + appOCPrefix;
			System.out.println("Creating Application " + applicationName + " ... ");
			createApplication("Flat File Connector", applicationName, configFile.getAbsolutePath());
			
			Application application = applicationService.findApplicationByName(applicationName);

			String appOCName = applicationPrefix + "-" + appOCPrefix + "-Account";
			System.out.println("Creating Application Objectclass " + appOCName +  " ... ");
			createApplicationObjectclass(appOCName, "__ACCOUNT__", application);
			System.out.println("-----------------------------------------------");
		}
	}

	private static void createApplicationWithObjectclasses(String bundleName, String applicationPrefix) throws Exception {
		String applicationName = applicationPrefix + "-Application";
		System.out.println("Creating Application " + applicationName + " using Bundle " + bundleName + " ... ");
		createApplication(bundleName, applicationName, null);
		
		String[] objectClasses = new String[] {
			"__ACCOUNT__",
			"__GROUP__"
		};
		
		Application application = applicationService.findApplicationByName(applicationName);

		for (String ocName : objectClasses) {
			String tmp = ocName.replaceAll("_", "").toLowerCase();
			String appOCName = applicationPrefix + "-" + new StringBuffer(Character.toUpperCase(tmp.charAt(0)) 
					+ tmp.substring(1)).toString();
			System.out.println("Creating Application Objectclass " + appOCName +  " ... ");
			createApplicationObjectclass(appOCName, ocName, application);
		}
		
	}

	private static void discoverInstalledBundles() throws Exception {
		System.out.println();
		 List<String> connectorTypes = connectorInfoService
				.getAllInstalledConnectorTypes();
	
		 for (String connectorType : connectorTypes) {
			 ConnectorInfo connectorInfo = connectorInfoService
						.getAllConnectorsForType(connectorType).get(0);
			 
			 ConnectorKey key = connectorInfo.getConnectorKey();
			 System.out.println("Discovered Bundle Display name: " + connectorType);
			 System.out.println("Bundle name: " + key.getBundleName());
			 System.out.println("Bundle Version: " + key.getBundleVersion());
			 System.out.println("Connector Name: " + key.getConnectorName());
			 System.out.println("Bundle URI: " + connectorInfo.getConnectorBundleURI());
			 System.out.println("Connection Pooling supported? " + connectorInfo.isConnectionPoolingSupported());
			 
			 ConnectorConfiguration connectorConfig = connectorInfo.getConfigProperties();
			 LinkedHashMap<String, ConnectorConfigurationProperty> connectorConfigProperties = connectorConfig
					 .getConfigProperties();
			 
			 Iterator<String> bundlePropsIt = connectorConfigProperties.keySet().iterator();
			 while (bundlePropsIt.hasNext()) {
				 ConnectorConfigurationProperty prop = connectorConfigProperties.get(bundlePropsIt.next());
				 System.out.println("\t " + prop.getName() + " " + prop.getType());
			 }
			 
			 System.out.println("-----------------------------------------------");

		 }
		
	}

	private static void enableAccount(String id) throws Exception{
		ProvisioningOperationResult result = provisioningService.enable(id);
		if (result.getFailedEntity() != null) {
			printFailures(result);
			return;
		}
		System.out.println("Account Enabled !");
		
	}	
	
	private static void revokeAccount(String id) throws Exception{
		ProvisioningOperationResult result = provisioningService.revoke(id);
		if (result.getFailedEntity() != null) {
			printFailures(result);
			return;
		}
		System.out.println("Account Revoked !");
		
	}

	private static void disableAccount(String id) throws Exception{
		ProvisioningOperationResult result = provisioningService.disable(id);
		if (result.getFailedEntity() != null) {
			printFailures(result);
			return;
		}
		System.out.println("Account Disabled !");
		
	}

	public static void createApplication(String connectorName, String applicationName,String flatFileLocation) throws Exception {
		if (connectorInfoService.getAllConnectorsForType(connectorName).isEmpty()) {
			System.out.println("The connector server of " + connectorName + " is not detected");
			return;
		}
	
		System.out.println();
		ConnectorInfo connectorInfo = connectorInfoService
				.getAllConnectorsForType(connectorName).get(0);
	
		ConnectorConfiguration connectorConfig = connectorInfo.getConfigProperties();
		LinkedHashMap<String, ConnectorConfigurationProperty> connectorConfigProperties = connectorConfig
				.getConfigProperties();
		List<ConnectorConfigurationProperty> bundleConfig = new LinkedList<ConnectorConfigurationProperty>();

		// Set ConfigurationProperty values for the connector
		HashMap<String, String> hmValues = getConnectorConfigPropValues(connectorName,flatFileLocation);

	
		for (String propName : connectorConfigProperties.keySet()) {
			ConnectorConfigurationProperty connectorConfigProperty = connectorConfigProperties
					.get(propName);
			// System.out.println("Setting property value for " + propName
			// + " with type " + connectorConfigProperty.getType());
			if (hmValues.containsKey(propName)) {
				connectorConfigProperty.setValue((String) hmValues
						.get(propName));
				bundleConfig.add(connectorConfigProperty);
			}
		}
	
		Application application = new Application();
		application.setName(applicationName);
		application.setDescription("Testing " + applicationName);
		application.setOwnerType("User");
		application.setOwnerId(UUID.randomUUID().toString());
		application.setBundleName(connectorInfo.getConnectorKey().getBundleName());
		application.setBundleConfig(bundleConfig);
		application.setBundleVersion(connectorInfo.getConnectorKey().getBundleVersion());
		application.setConnectorDisplayName(connectorInfo.getConnectorKey().getConnectorName());
		application.setConnectorName(connectorInfo.getConnectorKey().getConnectorName());
		application
				.setConnectorBundleURI(connectorInfo.getConnectorBundleURI());
		application.setConnectorServer(connectorInfo.getConnectorServer());
		application.setStatus(STATUS_TYPE.Active);
		ApplicationConfigurationManagerResult appConfigResult = applicationService
				.create(application);

		application = (Application) appConfigResult.getEntity();
		System.out.println("Application created.  id = " + application.getId()
				+ " status = " + appConfigResult.getStatus());
	}

	public static HashMap<String, String> getConnectorConfigPropValues(
			String connectorName,String flatFileLocation) throws Exception {
		HashMap<String, String> hmValues = new HashMap<String, String>();
		switch(connectorName) {
		case "Windows Active Directory Connector":
			hmValues = new HashMap<String, String>();
			hmValues.put("DirectoryAdminName", adminLogin);
			hmValues.put("DirectoryAdminPassword", adminPassword);
			hmValues.put("Container", container);
			hmValues.put("DomainName", domainName);
			break;
		case "Generic Unix Connector":
			hmValues.put("host","slc01fpj.us.oracle.com");
			hmValues.put("port", "22");
			hmValues.put("loginUser", "aime");
			hmValues.put("loginUserpassword", "2cool");				
			hmValues.put("sudoAuthorization", "1");
			break;
		case "Flat File Connector" :
			hmValues.put("schemaFile", flatFileLocation);
			break;
		}
				
		return hmValues;
	}

	
	public static void createApplicationObjectclass(String targetObjectclass, String applicationName) throws Exception {
		Application application = applicationService.findApplicationByName(applicationName);
		
		createApplicationObjectclass(null, targetObjectclass, application);

	}

	public static void deleteObject(String uid, String aocName) throws Exception {
		ApplicationObjectclass applicationObjectclass = applicationObjectclassService.findApplicationObjectclassByName(aocName);
		ManagedObject managedObject = new ManagedObject(applicationObjectclass);
		managedObject.setUID(uid);
		
		ProvisioningOperationResult result = managedObjectService
				.delete(managedObject);
		System.out.println("ManagedObject deleted with status = " + result.getStatus());
	}

	public static void createObject(String aocName, String name) throws Exception {
		ApplicationObjectclass applicationObjectclass = applicationObjectclassService.findApplicationObjectclassByName(aocName); 
		System.out.println("Found " + aocName);
		
		//String randomString = getRandomString(6);
		String randomString = name;
		
		System.out.println("Creating object " + randomString);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("__NAME__", "cn=" + randomString + ",ou=" + "helium" + ","
				+ container);
		data.put("description", randomString + " description");
		data.put("displayName", randomString + " display");
		data.put("sAMAccountName", randomString);
		data.put("mail", randomString + "@oracle.com");

		Set<Attribute> attributes = convertMapToAttributeSet(
				applicationObjectclass, data);
		
		
		ManagedObject managedObject = new ManagedObject(applicationObjectclass, attributes);
		managedObject.setOwnerID(java.util.UUID.randomUUID().toString()
				.replaceAll("-", "").toUpperCase());
		
		ProvisioningOperationResult result = managedObjectService.create(managedObject);
		if (result.getStatus().equals(STATUS.FAILED)) {
			printFailures(result);
		}
			
		managedObject = (ManagedObject) result.getEntity();
		
		System.out.println("ManagedObject created. id = "
				+ managedObject.getID());

	}
	
	public static void createAccount(String aocName, String name) throws Exception {
		ApplicationObjectclass applicationObjectclass = applicationObjectclassService.findApplicationObjectclassByName(aocName); 
		
		//String uid = getRandomString(6);
		String uid = name;
		
		System.out.println("Creating object " + uid);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("__NAME__", "cn=" + uid + ",ou=" + "helium" + ","
				+ container);
		data.put("sAMAccountName",uid);
		data.put("givenName",uid + "_First");
		data.put("sn", uid + "_Last");
		data.put("__PASSWORD__","Welcome1");
		data.put("department","IDM");
		data.put("company","Oracle");
		data.put("division","Fusion Middleware");
		data.put("displayName", uid + "First " + uid + "Last");
		data.put("initials", "ABC");
		data.put("description","Created from new provisioning engine");
		data.put("title", "Mr");
		
		ArrayList<String> mvaPhoneValues = new ArrayList<String>();
		mvaPhoneValues.add("1-1111");
		mvaPhoneValues.add("2-2222");
		mvaPhoneValues.add("3-3333");
		data.put("otherHomePhone", mvaPhoneValues);
		
		ArrayList<String> mvaEntGrpMem = new ArrayList<String>();
		mvaEntGrpMem.add("cn=HeliumGroup1,ou=helium,DC=adlrg,DC=us,DC=oracle,DC=com");
		mvaEntGrpMem.add("cn=HeliumGroup4,ou=helium,DC=adlrg,DC=us,DC=oracle,DC=com");
		data.put("__GROUPS__", mvaEntGrpMem);
		
		Set<Attribute> attributes = convertMapToAttributeSet(
				applicationObjectclass, data);
		
		ApplicationInstance appInstance = new ApplicationInstance();
		appInstance.setType(ApplicationInstance.TYPE.ICFBased);
		appInstance.setApplicationObjectclass(applicationObjectclass);
		Grant grantInfo = new Grant(UUID.randomUUID().toString(), GrantMechanism.ADMIN);
		grantInfo.setGrantStartTime(new Date(System.currentTimeMillis()));
		Account account = new Account(appInstance,attributes,grantInfo);
		
		ProvisioningOperationResult result = provisioningService.provision(account);
		if (result.getFailedEntity() != null) {
			printFailures(result);
			return;
		}
		account = (Account) result.getEntity();
		
		System.out.println("Account created. id = "	+ account.getID());

	}

	private static ApplicationObjectclass createApplicationObjectclass(String aocName,
			String targetObjectclass, Application application) throws Exception {
		List<ApplicationObjectclass> aocList = applicationService
				.getSupportedApplicationObjectclasses(application);
		ApplicationObjectclass appObjClass = null;
		for (ApplicationObjectclass aoc : aocList) {
		if (aoc.getTargetObjectclass().equalsIgnoreCase(targetObjectclass)) {
				appObjClass = aoc;
			}
		}

		if (appObjClass == null) {
			System.out.println("The application object class " + targetObjectclass
					+ " is not supported");
		}

		if (aocName == null) {
			aocName = getRandomString(6) + "_" + targetObjectclass;
		}
		System.out.println("Creating an applicationObjectclass with name "
				+ aocName);
		appObjClass.setApplication(application);
		appObjClass.setAttributeValuePolicyID("2");
		appObjClass.setDescription("Testing " + appObjClass.getName());
		appObjClass.setName(aocName);
		appObjClass.setOwnerId("1");
		// Set the prov Config
		ProvConfig provConfig = appObjClass.getProvConfig();
		Map<String, String> appOCProperties = provConfig.getAppOCProperties();
		appOCProperties.put(ApplicationObjectclass.APP_OC_PROPERTIES.ALLOW_MULTIPLE.getId(), "true");
		appOCProperties.put(ApplicationObjectclass.APP_OC_PROPERTIES.CASE_SENSITIVE.getId(), "false");
		
		List<ApplicationObjectclassCapability> capabilities = provConfig.getCapabilities();
		for (ApplicationObjectclassCapability cap : capabilities) {
			Map<SCRIPT_TRIGGER, ScriptConfig> scripts = new HashMap<ApplicationObjectclass.SCRIPT_TRIGGER, ScriptConfig>();
			
			// BEFORE script on connector
			ScriptConfig scriptConfig = new ScriptConfig();
			scriptConfig.setScriptLanguage("Groovy");
			StringBuffer scriptBody = new StringBuffer(1024);
			scriptBody.append("println 'Test script for pre-action of " + cap.getProvisioningOperation() + " operation'\n");
			//scriptBody.append("println 'Hello World'\n");
			//scriptBody.append("println 'Printing another random string'\n");
			//scriptBody.append("def s = 1 + 2\n");
			scriptConfig.setScriptBody(scriptBody.toString());
			scripts.put(SCRIPT_TRIGGER.BEFORE_OPERATION_ON_CONNECTOR, scriptConfig);
			
			// AFTER script on connector
			ScriptConfig scriptConfig1 = new ScriptConfig();
			scriptConfig1.setScriptLanguage("Groovy");
			StringBuffer scriptBody1 = new StringBuffer(1024);
			scriptBody1.append("println 'Test script for post-action of " + cap.getProvisioningOperation() + " operation'\n");
			//scriptBody1.append("println 'Hello World'\n");
			//scriptBody1.append("def client = new OIMClient()\n");
			//scriptBody1.append("UserManager usrMgr = client.getService(UserManager.class)\n");			
			//scriptBody1.append("println 'This handle can be used to perform user operations'\n");
			scriptConfig1.setScriptBody(scriptBody1.toString());
			scripts.put(SCRIPT_TRIGGER.AFTER_OPERATION_ON_CONNECTOR, scriptConfig1);			

			cap.setProvScriptHandlers(scripts);
		}
		

		ReconConfig reconConfig = appObjClass.getReconConfig();
		reconConfig.setFullReconJobName("Full Recon Job for " + aocName);
		reconConfig.setIncrementalReconJobName("Incremntal Recon Job for " + aocName);

		Map<RECON_SITUATIONS, RECON_RESPONSES> reconSituationResponses = reconConfig.getReconSituationResponses();
		if (reconSituationResponses == null) {
			reconSituationResponses = new HashMap<RECON_SITUATIONS, RECON_RESPONSES>();
		}
		reconSituationResponses.put(RECON_SITUATIONS.SITUATION_MATCHED, RECON_RESPONSES.ADD_USER);
		reconSituationResponses.put(RECON_SITUATIONS.SITUATION_UNMATCHED, RECON_RESPONSES.ASSIGN);
		reconConfig.setReconSituationResponses(reconSituationResponses);

		ApplicationObjectclassService appObjectclassService = oimClient.getService(ApplicationObjectclassService.class);
		ApplicationConfigurationManagerResult result = appObjectclassService
				.create(appObjClass);
		System.out.println("Created ApplicationObjectclass with entity id "
				+ ((ApplicationObjectclass) result.getEntity()).getId()
				+ " and status " + result.getStatus());

		ApplicationObjectclass applicationObjectclass = (ApplicationObjectclass) result.getEntity();

		return applicationObjectclass;
	}

	public static void discoverTargets(String connectorServerName) throws Exception {
		ConnectorServerService connectorServerService = oimClient
				.getService(ConnectorServerService.class);

		List<ConnectorServer> connectorServers = connectorServerService
				.searchByName(connectorServerName);

		if (!connectorServers.isEmpty()) {
			cs = connectorServers.get(0);
			System.out.println(connectorServerName + " is running at " + cs.getHost() + ":" + cs.getPort());
		} else {
			System.out.println("Connector server " + connectorServerName + " not found!");
		}
		
	}

	public static void shutdown() throws Exception {
		if (oimClient != null) {
			oimClient.logout();
		}
		System.exit(0);
	}
	
    public static String getRandomString(int length) {
        Random r = new Random();
        StringBuffer buf = new StringBuffer();
        
        for (int i=0 ; i<length ; i++) {
        	buf.append(ALPHABETS.charAt(r.nextInt(ALPHABETS.length())));
        }
        
        return buf.toString().toUpperCase();
    }
    
	public static Set<Attribute> convertMapToAttributeSet(
			ApplicationObjectclass appObjclass, Map<String, Object> data) {
		Set<Attribute> attributes = new HashSet<Attribute>();
	
		for (String attrName : data.keySet()) {
			AttributeBuilder attributeBuilder = appObjclass.getAttributeBuilder(attrName);
			BasicAttribute attrDefn = attributeBuilder.getAttributeDefinition();

			if (attrDefn != null && attrDefn.isComplex()) {
				EmbeddedObjectBuilder embeddedObjectBuilder = appObjclass.getEmbeddedObjectBuilder(attrName);
				ComplexAttribute complexAttrDefn = (ComplexAttribute) attrDefn;

				List<Map<String, Object>> childDataList = (List<Map<String, Object>>) data.get(attrName);
				
				for (Map<String, Object> childData : childDataList) {
					embeddedObjectBuilder.addAttributes(childData);
					attributeBuilder.addValue(embeddedObjectBuilder.build());
				}

				attributes.add(attributeBuilder.build());
			} else {
				if (data.get(attrName) instanceof Collection<?>) {
					attributes.add(attributeBuilder.buildAttribute((Collection<?>) data
							.get(attrName)));
				} else {
					attributes.add(attributeBuilder.buildAttribute(data
							.get(attrName)));
				}
			}
		}
		return attributes;
	}
	
	public static void printFailures(ProvisioningOperationResult result) {
		Throwable t = result.getFailure();
		if (t != null) {
			System.out.println("Throwable message:: " + t.getMessage());
			System.out.println("Throwable localized message:: "
					+ t.getLocalizedMessage());
			System.out.println("Throwable cause:: " + t.getCause());
			System.out.println("Throwable stacktrace:: ");
			t.printStackTrace();
			System.out.println("------------------------------");
		}
	
	}
}

