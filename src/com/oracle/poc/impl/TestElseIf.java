package com.oracle.poc.impl;

import java.util.Date;
import java.util.GregorianCalendar;

public class TestElseIf {

	public static enum PolicyOwnerType {
		   USER,
		   ROLE
	   }	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("TestElseIf.main(): "+PolicyOwnerType.ROLE);
		System.out.println("TestElseIf.main(): "+PolicyOwnerType.ROLE.toString().equals("ROLE"));
		
		String entityAttributeValue = null;
		Object obj = getEntityAttributeValue(entityAttributeValue);
		System.out.println("TestElseIf.main():obj "+obj);
	}
	
	public static Date getValidSunrisetDate(Date sunriseDate ){
	       GregorianCalendar calendar = new GregorianCalendar();
	       Date currentDate = calendar.getTime();
	       if(sunriseDate == null || sunriseDate.before(currentDate)){
	         sunriseDate =currentDate;
	       }
	       return sunriseDate;
	   }
	
	public static Date getValidSunsetDate(Date sunSetDate ){
	       GregorianCalendar calendar = new GregorianCalendar();
	       Date currentDate = calendar.getTime();
	       if(sunSetDate.before(currentDate)){
	    	   sunSetDate =null;
	       }
	       return sunSetDate;
	   }	

	public static Object getEntityAttributeValue(Object entityAttributeValue) {

		if (entityAttributeValue instanceof Byte)
			return (Byte) entityAttributeValue;

		if (entityAttributeValue instanceof Double)
			return (Double) entityAttributeValue;

		if (entityAttributeValue instanceof Integer)
			return (Integer) entityAttributeValue;

		if (entityAttributeValue instanceof String)
			return (String) entityAttributeValue;

		if (entityAttributeValue instanceof Short)
			return (Short) entityAttributeValue;

		if (entityAttributeValue instanceof Long)
			return (Long) entityAttributeValue;

		if (entityAttributeValue instanceof java.util.Date) {
			long date = ((java.util.Date) entityAttributeValue).getTime();
			java.sql.Date sqlDate = new java.sql.Date(date);
			return sqlDate;
		}

		if (entityAttributeValue instanceof java.sql.Date) {

			return (java.sql.Date) entityAttributeValue;

		}

		if (entityAttributeValue instanceof Boolean)
			return (Boolean) entityAttributeValue;

		return null;
	}	

}

