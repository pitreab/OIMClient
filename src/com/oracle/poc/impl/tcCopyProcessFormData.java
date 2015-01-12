package com.oracle.poc.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thortech.util.logging.Logger;
import com.thortech.xl.client.events.ScheduleItemEvents.tcScheduleItemEvent;
import com.thortech.xl.dataobj.PreparedStatementUtil;
import com.thortech.xl.dataobj.tcDataSet;
import com.thortech.xl.dataobj.tcScheduleItem;
import com.thortech.xl.dataobj.tcUDProcess;
import com.thortech.xl.util.logging.LoggerMessages;
import com.thortech.xl.util.logging.LoggerModules;


public class tcCopyProcessFormData extends tcScheduleItemEvent {
	private static Logger logger = Logger.getLogger(LoggerModules.XL_JAVA_CLIENT);
	private static final String EVENT_NAME="CopyProcessFormData";
	private static final String GET_OBJ_FROM_OBI = "select obj_name from obj where obj_key = (select obj_key from obi where obi_key = ?";
	private static final String GET_PARENT_OIU_FROM_OUD = "select oud.oud_parent_oiu_key, oud.obj_key " +
														 "from oud oud " +
														 "where oud.oiu_key = (select oiu.oiu_key from oiu oiu where oiu.orc_key =?)" ;
	private static final String GET_OBJ_FROM_OBJ_KEY = "select obj_name from obj where obj_key=?";
	private static final String GET_ORC_FROM_OIU = "select orc_key from oiu where oiu_key=?";
	private static final String GET_UD_METADATA= "select orc.tos_key, sdk.sdk_name, sdk.sdk_key, " +
												 "sdk.sdk_active_version from orc orc, tos tos left " +
												 "outer join sdk sdk on tos.sdk_key=sdk.sdk_key where " +
												 "orc.tos_key=tos.tos_key and orc.orc_key=?" ;
	private static final String GET_SDC_LABEL="select sdc_label from sdc where sdk_key=? and sdc_version=? and sdc_label is not null";
	private static final String GET_SDC_NAME="select sdc_label from sdc where sdk_key=? and sdc_version=? and sdc_label = ?";	
	
	private static final String GET_LOOKUP_ENTRY = "select lkv_encoded, lkv_decoded from lkv lkv, lku lku " +
												   " where lkv.lku_key=lku.lku_key and lku.lku_type_string_key=?";
	
	public tcCopyProcessFormData() {
        setEventName(EVENT_NAME);
    }

    protected void implementation() throws Exception {
    	tcScheduleItem schItem = (tcScheduleItem) getDataObject();
    	PreparedStatementUtil psUtil = new PreparedStatementUtil();
    	
    	// this is orc_key of child resource (FA)
    	String childORCKey = schItem.getOrcKey();
    	String obiKey = schItem.getObiKey();

    	// Get form information
    	tcDataSet ds = new tcDataSet();;

    	//get object details of child / dependent resource
    	ds = executeQuery(psUtil, GET_OBJ_FROM_OBI, obiKey);
    	ds.goToRow(0);
    	String childObjectName = ds.getString("obj_name");;
        
    	//find corresponding oud entry for oiu_key of child to get parent details.
    	ds = executeQuery(psUtil, GET_PARENT_OIU_FROM_OUD, childORCKey);
        ds.goToRow(0);
        String parentOIUKey = ds.getString("oud_parent_oiu_key");
        String parentOBJKey = ds.getString("obj_key");
        
        // find object name of parent resource object
        ds = executeQuery(psUtil, GET_OBJ_FROM_OBJ_KEY, parentOBJKey);
        ds.goToRow(0);
        String parentObjectName = ds.getString("obj_name");
        
    	// for oud_parent_oiu_key -> get corresponding orc_key
        ds = executeQuery(psUtil, GET_ORC_FROM_OIU, parentOIUKey);
        ds.goToRow(0);
        String parentORCKey = ds.getString("orc_key");
        
        	
    	// find UD_TABLE meta-data for parent orc_key
        ds = executeQuery(psUtil, GET_UD_METADATA, parentORCKey);
        ds.goToRow(0);
        String udParentObjectTableKey=ds.getString("sdk_key");
        String udParentObjectTableName = ds.getString("sdk_name");

        // create query to check if user-defined table has an entry for the
        // specified child ORC key
        String query2 = "select  * from " + udParentObjectTableName + " where orc_key=?";
        ds = executeQuery(psUtil, query2, parentORCKey);
        
        // TODO if no entry in UD, then nothing to update ?
        if(ds==null || ds.getRowCount()==0){
        	String errorMessage = "No entry found in "+udParentObjectTableName+ "for parent object orc_key:  "+parentORCKey;
			logger.error(LoggerMessages.getMessage("ErrorMethodDebug", "tcCopyProcessFormData/implementation", errorMessage));
			throw new Exception(errorMessage);        	
        }
        ds.goToRow(0);
        String activeVersion = ds.getString("udParentObjectVersionCol");
        
        // convert this dataset into map, this is the source data from where we need to copy data from
        Map<String, String> parentObjectDataMap = convertDataSetToMap(psUtil, ds, udParentObjectTableName, 
        		udParentObjectTableKey, activeVersion);
        

    	// find UD_TABLE meta-data for child RO orc_key
        ds = executeQuery(psUtil, GET_UD_METADATA, childORCKey);
        ds.goToRow(0);
        String udChildROTableKey=ds.getString("sdk_key");
        String udChildROTableName = ds.getString("sdk_name");
        String udChildROPKName = udChildROTableName + "_key";
        String udChildRORowVer = udChildROTableName + "_rowver";
        String udChildROVersionCol = udChildROTableName + "_version";        

        // create query to check if user-defined table has an entry for the
        // specified parent ORC key
        query2 = "select  * from " + udChildROTableName + " where orc_key=?";
        ds = executeQuery(psUtil, query2, childORCKey);
        ds.goToRow(0);        

        // Now update the process form
        tcUDProcess udProcess = new tcUDProcess(getDataObject(), "" + udChildROTableKey,
                childORCKey, udChildROPKName, ds.getString(udChildROPKName), null, null,
                ds.getByteArray(udChildRORowVer));        
        
       // The format of the lookup definition name is
       // Lookup.Objects.parentObjectName.ChildObjectName.CopyAttributesMap
        String lookupName = "Lookup.Objects." + parentObjectName + "."+ childObjectName+ ".CopyAttributesMap";
        tcDataSet lookupDs = psUtil.getDataSet();
        
        lookupDs = executeQuery(psUtil, GET_LOOKUP_ENTRY, lookupName);
      
        // if entry not found, throw error
        if(lookupDs==null || lookupDs.getRowCount()==0){
        	String errorMessage = "No lookup entry found for "+lookupName;
			logger.error(LoggerMessages.getMessage("ErrorMethodDebug", "tcCopyProcessFormData/implementation", errorMessage));
			throw new Exception(errorMessage);

        }
        
        for (int i=0 ; i<lookupDs.getRowCount() ; i++) {
        	lookupDs.goToRow(i);
        	String parentObjectLabelCol = lookupDs.getString("lkv_encoded");
        	String childObjectLabelCol = lookupDs.getString("lkv_decoded");
        	// Get field name for childObjectLabelCol
        	String childObjectROColName = getColumnName(psUtil, udChildROTableKey, ds.getString("udChildROVersionCol"), childObjectLabelCol);
        	if (parentObjectDataMap.containsKey(parentObjectLabelCol)) {
        		//get the column name for column label for form of the child object
        		udProcess.setString(childObjectROColName, (String)parentObjectDataMap.get(parentObjectLabelCol));
        	}
        }
        udProcess.save();
        updateMilestoneStatus("C");
        return;
    }
    
    private Set<String> getExcludeColumnSet(String tableName){
    	Set<String> excludeColumnSet = new HashSet<String>();
		excludeColumnSet.add(tableName+"_KEY");
		excludeColumnSet.add(tableName+"_UPDATE");
		excludeColumnSet.add(tableName+"_UPDATEBY");
		excludeColumnSet.add(tableName+"_CREATE");
		excludeColumnSet.add(tableName+"_CREATEBY");
		excludeColumnSet.add(tableName+"_VERSION");
		excludeColumnSet.add(tableName+"_NOTE");
		excludeColumnSet.add(tableName+"_ROWVER");
		excludeColumnSet.add(tableName+"_DATA_LEVEL");
		excludeColumnSet.add(tableName+"_REVOKE");    
		return excludeColumnSet;
    }
    
    private tcDataSet executeQuery(PreparedStatementUtil psUtil, String sqlQuery, String input) throws Exception{
        psUtil.setStatement(getDataBase(), sqlQuery);
        psUtil.setString(1, input);
        try {
			psUtil.execute();
		} catch (Exception e) {
			logger.error(LoggerMessages.getMessage("ErrorMethodDebug", "tcCopyProcessFormData/executeQuery",  e.getMessage()),  e);
			throw e;
		} 
        tcDataSet ds = psUtil.getDataSet(); 
        return ds;
    }
    
    private Map<String, String> convertDataSetToMap(PreparedStatementUtil psUtil, tcDataSet ds, String udTableName, String sdkKey, String activeVersion) throws Exception{
    	Set<String> excludeColumnSet = getExcludeColumnSet(udTableName);
		Map<String, String> parentOrcData = new HashMap<String, String>();
		for (int j = 0; j < ds.getRowCount(); j++) {
			ds.goToRow(j);
			int mnColCount = ds.getColumnCount();
			for (int k = 0; k < mnColCount; k++) {
				String udColumnName = ds.getColumnName(k) ;
				if(excludeColumnSet.contains(udColumnName)){
					continue;
				}
				String udColumnValue = ds.getDisplayText(k);
				//check label for the columnName
				String udColumnLabel = getColumnLabel(psUtil, sdkKey, activeVersion, udColumnName);
				parentOrcData.put(udColumnLabel,udColumnValue);	
			}
		}     
		return parentOrcData;
    	
    }
    
    private String getColumnLabel(PreparedStatementUtil psUtil, String sdkKey, String activeVersion, String columnName) 
    throws Exception{
        psUtil.setStatement(getDataBase(), GET_SDC_LABEL);
        psUtil.setString(1, sdkKey);
        psUtil.setString(2, activeVersion);
        psUtil.setString(3, columnName);
        try {
			psUtil.execute();
		} catch (Exception e) {
			logger.error(LoggerMessages.getMessage("ErrorMethodDebug", "tcCopyProcessFormData/executeQuery",  e.getMessage()),  e);
			throw e;
		} 
        tcDataSet ds = psUtil.getDataSet(); 
        ds.goToRow(0);
        String columnLabel = ds.getString("sdc_label");
        return columnLabel;
    }   
    
    private String getColumnName(PreparedStatementUtil psUtil, String sdkKey, String activeVersion, String columnLabel) 
    throws Exception{
        psUtil.setStatement(getDataBase(), GET_SDC_NAME);
        psUtil.setString(1, sdkKey);
        psUtil.setString(2, activeVersion);
        psUtil.setString(3, columnLabel);
        try {
			psUtil.execute();
		} catch (Exception e) {
			logger.error(LoggerMessages.getMessage("ErrorMethodDebug", "tcCopyProcessFormData/getColumnName",  e.getMessage()),  e);
			throw e;
		} 
        tcDataSet ds = psUtil.getDataSet(); 
        ds.goToRow(0);
        String columnName = ds.getString("sdc_name");
        return columnName;
    }     
}
