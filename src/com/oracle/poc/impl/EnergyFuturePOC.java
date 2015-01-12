package com.oracle.poc.impl;

import oracle.iam.platform.OIMClient;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.vo.ApplicationInstance;

import com.oracle.poc.util.OIMUtils;

public class EnergyFuturePOC {

	public static ApplicationInstanceService applicationInstanceService = null;
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		OIMClient oimClient = OIMUtils.loginAsUser("xelsysadm", "Welcome1");
		applicationInstanceService = (ApplicationInstanceService)oimClient.getService(ApplicationInstanceService.class);
		ApplicationInstance appList = applicationInstanceService.findApplicationInstanceByKey(1);
		System.out.println(appList.getApplicationInstanceName());
		


	}

}
