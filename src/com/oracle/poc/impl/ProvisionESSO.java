package com.oracle.poc.impl;

import java.util.Date;

import javax.xml.rpc.ServiceException;

//import com.passlogix.vgo.pm.cli.CLIOperationParser;
//import com.passlogix.vgo.pm.cli.Operation;
//import com.passlogix.vgo.pm.cli.Operation.CollectionsMap;
//import com.passlogix.vgo.pm.cli.ProvisioningConnection;
//import com.passlogix.vgo.pm.operations.OperationKeys;

public class ProvisionESSO {

//	public static void main(String[] args) {
//		String strURL = "http://lcrd221.us.oracle.com/v-GO PM Service/UP.asmx";
//		String strAgent = "apitreAgent";
//		String strUsername = "cn=OIMuser";
//		String strPassword = "Password1";
//		ProvisioningConnection conn;
//		Operation oper;
//		try {
//			conn = new ProvisioningConnection(strURL, strAgent, strUsername,strPassword);
//			System.out.println("ProvisionESSO.main(): Entered");
//			try {
//				CLIOperationParser opParser = CLIOperationParser.newInstance();
//				Operation.StringMap options = new Operation.StringMap();
//				options.put(OperationKeys.USERID, "OIMuser");
//				options.put(OperationKeys.APP_USERID, "nrazdan");
//				options.put(OperationKeys.PASSWORD, "Welcome1234");
//				options.put(OperationKeys.APPLICATION, "google");
//				String strOper = CLIOperationParser.ADD_CREDENTIAL;
//				try {
//					oper = opParser.parse(strOper, options);
//					System.out.println("ProvisionESSO.main(): after parsing: ");
////					oper.setExecTime(new Date());
//					String result = conn.sendInstruction(oper);
//					if (!oper.getSuccess()) {
//						String strMsg = String.format(
//								"The command failed: id=%s, msg=%s",
//								oper.getCommandID(), oper.getError());
//						System.out.println("ProvisionESSO.main():strMsg: "+strMsg);
//					}
//					String strID = oper.getCommandID();
//					System.out.println("ProvisionESSO.main():strID: "+strID);
//					
//					CollectionsMap map = oper.getResultAttributes();
//				} catch (InstantiationException Inex) {
//					System.out.println("It went blewy");
//				} catch (IllegalAccessException Ilex) {
//					System.out.println("this went blewy too!");
//				}
//
//			} catch (Exception ex) { // print exception
//				ex.printStackTrace();
//			}
//		} catch (ServiceException serex) {
//			System.out.println("ProvisionESSO.main():serex: "+serex.getMessage());
//			serex.printStackTrace();
//		}
//		System.out.println("ProvisionESSO.main(): Exit");
//	}

}
