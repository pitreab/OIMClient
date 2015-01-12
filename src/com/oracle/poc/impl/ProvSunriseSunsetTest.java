package com.oracle.poc.impl;

import oracle.iam.conf.api.SystemConfigurationService;
import oracle.iam.conf.vo.SystemProperty;
import java.util.Calendar;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.Random;
import java.text.SimpleDateFormat;
//import oracle.iam.reconciliation.*;
import oracle.iam.provisioning.api.*;
import oracle.iam.provisioning.vo.*;

import oracle.iam.platform.entitymgr.EntityManager;
import oracle.iam.platform.entitymgr.vo.Entity;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.platform.context.ContextManager;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Platform;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.utils.*;
import oracle.iam.identity.utils.Constants.*;
import oracle.iam.identity.usermgmt.vo.*;
import oracle.iam.identity.vo.Identity;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.ValidationFailedException;
import static oracle.iam.platform.entitymgr.spi.entity.Searchable.SortOrder;
import oracle.iam.identity.exception.UserManagerException;
import static oracle.iam.identity.utils.Constants.*;
import oracle.iam.identity.usermgmt.api.UserManagerConstants.AttributeName;
import oracle.iam.identity.usermgmt.api.UserManagerConstants.AttributeValues;
import oracle.iam.configservice.api.LocaleUtil;
import oracle.iam.platform.entitymgr.spi.entity.Searchable;
import java.util.Hashtable;
import oracle.iam.platform.*;

public class ProvSunriseSunsetTest {

	// args - usrLogin, appInstance, future_start_date, future_end_date
	public static void main(String[] args) {
		try {

			if (args.length < 2) {
				System.out.println(" usage:");
				System.out
						.println(" account <user_login> <app instance name> <Start Date (yyyyMMdd)> <End Date> (yyyyMMdd) \n");
				System.out
						.println(" ent <ent_list_key> <oiu_key> <Start Date (yyyyMMdd)> <End Date> (yyyyMMdd) \n");

				return;
			}
			String entity = args[0];
			System.out.println("entity:" + entity);

			System.out.println("Startup...");
			System.out.println("Getting configuration...");
			String ctxFactory = "weblogic.jndi.WLInitialContextFactory";
			String hostName = "slc05ksx.us.oracle.com";
			String port = "8003";
			String serverURL = "t3://" + hostName + ":" + port;
			Hashtable env = new Hashtable();
			env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, serverURL);
			env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, ctxFactory);
			OIMClient platform = new OIMClient(env);
			platform.login("xelsysadm", "Welcome1");

			ApplicationInstanceService appInstService = platform
					.getService(ApplicationInstanceService.class);
			ProvisioningService provService = platform
					.getService(ProvisioningService.class);
			EntitlementService entService = platform
					.getService(EntitlementService.class);
			UserManager usrMgr = platform.getService(UserManager.class);

			if (entity.equals("account")) {
				String usrLogin = args[1];
				String appInstance = args[2];
				SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
				Date startDate = (Date) formatter.parse(args[3]);
				Date endDate = (Date) formatter.parse(args[4]);
				long usrKey = ProvSunriseSunsetTest.createUser(usrMgr,
						usrLogin, 1);
				long oiuKey = ProvSunriseSunsetTest.provisionAccount(
						appInstService, provService, appInstance, usrKey,
						usrLogin, startDate, endDate);
				System.out.println("Account provisioned with oiukey:" + oiuKey);
			} else if (entity.equals("ent")) {
				System.out.println(args[1]);
				long entlistKey = Long.valueOf(args[1]);
				long oiuKey = Long.valueOf(args[2]);
				SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
				Date startDate = (Date) formatter.parse(args[3]);
				Date endDate = (Date) formatter.parse(args[4]);
				Entitlement ent = entService.findEntitlement(entlistKey);
				EntitlementInstance entInstance = new EntitlementInstance(ent);
				entInstance.setAccountKey(oiuKey);
				entInstance.setValidFromDate(startDate);
				entInstance.setValidToDate(endDate);
				provService.grantEntitlement(entInstance);
			}

			System.out.println("done");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static long provisionAccount(
			ApplicationInstanceService appInstService,
			ProvisioningService provService, String appInstanceName,
			long userKey, String userLogin, Date startDate, Date endDate) {
		long acctKey = 0;
		try {
			ApplicationInstance appInstance = appInstService
					.findApplicationInstanceByName(appInstanceName);
			long parentFormKey = appInstance.getAccountForm().getFormKey();
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("UD_ADUSER_ORGNAME",
					"31~OU=sdey,DC=adlrg,DC=us,DC=oracle,DC=com");
			data.put("UD_ADUSER_EMAIL", userLogin + "@oim.com");
			data.put("UD_ADUSER_USERPRINCIPALNAME", userLogin);
			data.put("UD_ADUSER_UID", userLogin);
			data.put("UD_ADUSER_COMMONNAME", userLogin);
			AccountData objAccountData = new AccountData(parentFormKey + "",
					null, data);
			Account objAccount = new Account(appInstance, objAccountData);
			objAccount.setAccountType(Account.ACCOUNT_TYPE.Primary);
			objAccount.setValidFromDate(startDate);
			objAccount.setValidToDate(endDate);
			acctKey = provService.provision(userKey + "", objAccount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return acctKey;
	}

	private static long createUser(UserManager usrMgr, String userLogin,
			long organizationKey) {
		UserManagerResult userResult = null;
		HashMap<String, Object> createAttributes = new HashMap<String, Object>();
		String userKey = null;
		// to make it unique
		createAttributes.put(USERID, userLogin);
		createAttributes.put(FIRSTNAME, userLogin + "_First");
		createAttributes.put(LASTNAME, userLogin + "_Last");
		// to make it unique
		String email = createAttributes.get(USERID) + "@oracle.com";
		createAttributes.put(EMAIL, email);
		createAttributes.put(ORGKEY, organizationKey);
		createAttributes.put(USERTYPE, "End-User");
		createAttributes.put(EMPTYPE, "Full-Time");
		createAttributes.put(PASSWORD, "Welcome1");
		try {
			userResult = usrMgr.create(new User(null, createAttributes));
			userKey = userResult.getEntityId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Long.parseLong(userKey);
	}

}
