package com.oracle.poc.impl;

import java.util.Random;

import oracle.iam.platform.OIMClient;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ApplicationInstance.TYPE;
import oracle.iam.provisioning.vo.FormInfo;
import Thor.API.tcResultSet;
import Thor.API.Operations.tcFormDefinitionOperationsIntf;
import Thor.API.Operations.tcITResourceDefinitionOperationsIntf;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;

import com.oracle.poc.util.OIMUtils;
import com.thortech.xl.ejb.beansimpl.tcFormDefinitionOperationsBean;

public class CreateDiscAppInstance {

	 public static tcITResourceDefinitionOperationsIntf itResourceDefinitionOperationsService = null;
	 public static tcITResourceInstanceOperationsIntf itResourceInstanceOperationsService = null;
	 public static ApplicationInstanceService applicationInstanceService = null;
	 public static tcFormDefinitionOperationsIntf formDefinitionOperationsService = null;

	 private static final int uniqueIDLength = 4;
	 
	private  static String generateUniqueID() {
	        Random r = new Random();
	        String uniqueID = (Long.toString(Math.abs(r.nextLong()), 36)).substring(0, uniqueIDLength);
	        return uniqueID.toUpperCase();
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

	
	
	public static void main(String[] args) throws Exception {
		OIMClient oimClient = OIMUtils.loginAsUser("xelsysadm", "Welcome1");
		applicationInstanceService = (ApplicationInstanceService)oimClient.getService(ApplicationInstanceService.class);
		formDefinitionOperationsService = (tcFormDefinitionOperationsIntf)oimClient.getService(tcFormDefinitionOperationsIntf.class);
		
		String uniqueId =generateUniqueID();
		long formKey=19;
		Random r = new Random();
		formDefinitionOperationsService.createNewVersion(formKey, 1, uniqueId);
		
		int latestVersion=2;
		
		tcResultSet formFieldRS = formDefinitionOperationsService.getFormFields(19,0);
//		printResultSet(formFieldRS);
		
		
      	int maxOrder=0;

       	for (int i = 0; i < formFieldRS.getRowCount(); ++i) {
       		formFieldRS.goToRow(i);
       		int order =  formFieldRS.getIntValue("Structure Utility.Additional Columns.Order");
       		if(maxOrder<order){
       			maxOrder=order;
       		}
       	}
       	
       	System.out.println("CreateDiscAppInstance.main():maxOrder : "+maxOrder);
       	
       	
       	// Iterate through fields and add field one at a time. 
       	String psFieldName = uniqueId;
       	String psFieldType = "TextField";
       	String psVariantType = "String";
       	int pnFieldLength = 80;
       	int pnOrder=maxOrder+1;
       	String psDefaultValue="";
       	String pbProfileEnabled="";
       	boolean pbSecure=false;
       	
       	formDefinitionOperationsService.addFormField(19, 2, 
       			psFieldName, psFieldType, psVariantType, pnFieldLength,
       			pnOrder, psDefaultValue,pbProfileEnabled, pbSecure );         
         

       	// After end of iteration, 

	}

}
