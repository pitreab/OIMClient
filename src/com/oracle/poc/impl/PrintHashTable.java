package com.oracle.poc.impl;

import java.util.Hashtable;

public class PrintHashTable {

	public static void main(String[] s) {
	    Hashtable table = new Hashtable();
	    table.put("key1", "value1");
	    table.put("key2", "value2");
	    table.put("key3", "value3");

	    System.out.println(table);
	  }

}
