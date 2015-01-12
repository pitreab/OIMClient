package com.oracle.poc.impl;


import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import oracle.iam.platform.context.ContextAwareNumber;
import oracle.iam.platform.context.ContextAwareString;
import oracle.iam.platform.kernel.vo.EntityOrchestration;
import oracle.iam.platform.kernel.vo.Orchestration;
import oracle.iam.platform.kernel.vo.PostProcessOnly;
import oracle.iam.platform.kernel.vo.PostProcessOnlyBulkOrchestration;

public class DeserializeOrchestration {

    public DeserializeOrchestration()
    {

    }

    public static void main(String[] args) throws Exception {
        String statement1 = "select * from orchprocess where id=? ";
        String statement2 = "select * from orchevents where processid =? order by orchorder";
        System.out.println("=======================Record ===========================================");
        // String statement =
        // "select id, orchestration from orchprocess where entitytype='User' and operation='CREATE' order by id";
        // String statement =
        // "select orchestration from orchprocess where id in (select processid from orchevents where name='UserModifyLDAPPreProcessHandler' and status='FAILED') order by id desc";
        Connection conn = getOracleConnection();


        // Read object from oracle
        PreparedStatement pstmt = conn.prepareStatement(statement1);
        pstmt.setLong(1, 1318781339);
//        pstmt.setLong(1, 57219);
        //pstmt.setLong(1, 2026);

        ResultSet rs = pstmt.executeQuery();
        int record =0;
        while (rs.next()) {
            System.out.println("=======================Record " + record++ + "===========================================");
            dumpRS(rs);
            String id = rs.getString("ID");
            PreparedStatement pstmt1 = conn.prepareStatement(statement2);
            pstmt1.setString(1, id);
            ResultSet rs1 = pstmt1.executeQuery();
            while (rs1.next()) {
                System.out.println();
                dumpRS(rs1);
            }
            rs1.close();
            pstmt1.close();
        }

        rs.close();
        pstmt.close();
        conn.close();

        System.exit(0);

    }

    public static void dumpRS(ResultSet rs) throws Exception {

        InputStream is = null;
        ObjectInputStream oip = null;
        int record=0;
        //while (rs.next()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i=0 ; i<rsmd.getColumnCount() ; i++) {
                String colName = rsmd.getColumnName(i+1);

                if (colName.equalsIgnoreCase("orchestration") || colName.equalsIgnoreCase("result")) {
                    //is = rs.getBlob(1).getBinaryStream();
                    is = rs.getBinaryStream(colName);
                    oip = new ObjectInputStream(is);
                    Object o;
                    try {
                        o = oip.readObject();
                    } catch (InvalidClassException e) {
                        System.out.println("Invalid BLOB Class");
                        continue;
                    }

                    if (o instanceof PostProcessOnlyBulkOrchestration) {

                        PostProcessOnlyBulkOrchestration object = (PostProcessOnlyBulkOrchestration) o;

                        System.out.println("Operation = " + object.getOperation());
                        System.out.println("Target = " + object.getTarget());
                        System.out.println("Action Result = " + object.getActionResult());
                        System.out.println("Context value = " + object.getContextVal());
                        printMap("Bulk Parameters: ", object.getBulkParameters());
                        printMap("Parameters: ", object.getParameters());
                        printMap("Inter event data&&&&&&&&&&&&: ", object.getInterEventData());
                    } else if (o instanceof EntityOrchestration) {
                        EntityOrchestration object = (EntityOrchestration) o;

                        System.out.println("EntityID = " + object.getEntityId());
                        System.out.println("EntityType = " + object.getEntityType());
                        System.out.println("Type = " + object.getType());

                        System.out.print("All Entity IDs: " );
                        for (int j=0 ; j<object.getAllEntityId().length ; j++) {
 System.out.print(object.getAllEntityId()[j]);
                        }
                        System.out.println();


                    } else if (o instanceof Orchestration) {
                        Orchestration object = (Orchestration) o;

                        System.out.println("Operation = " + object.getOperation());
                        System.out.println("Target = " + object.getTarget());
                        //System.out.println("Action Result = " + ((Orchestration) object).getActionResult());
                        System.out.println("Context value = " + object.getContextVal());
                        //printMap("Bulk Parameters: ", object.getBulkParameters());
                        printMap("%%%%Parameters: ", object.getParameters());
                        printMap("%%%%%%%%%%Inter event data: ", object.getInterEventData());
                    } else if (o instanceof Exception) {
                        Exception  object = (Exception) o;
 System.out.println("-----------------------");
                        object.printStackTrace();
                        System.out.println(object.getCause());
                        System.out.println(object.getMessage());
 System.out.println("-----------------------");
                    } else {
                        System.out.println("UNKNOWN ORCHESTRATION BLOB");
                    }


                } else {
                    System.out.println(colName + " ===> " + rs.getString(colName));
                }
            }
        //}


        oip.close();
        is.close();

    }

    public static void printMap(String name, HashMap<String, Serializable>[] marr) {

        System.out.println("Dumping maps name = " + name);

        for (int i=0 ; i<marr.length ; i++) {
            System.out.println("Map element # " + i);
            HashMap<String, Serializable> m = marr[i];

            if (m == null) {
                System.out.println("Null Map found");
                continue;
            }
            Set<Entry<String, Serializable>> set = m.entrySet();
            Iterator<Entry<String, Serializable>> it = set.iterator();

            while (it.hasNext()) {
                Entry<String, Serializable> e = it.next();

                Object val = (Object)e.getValue();
                String v = "";

                if (val instanceof ContextAwareString) {
                    ContextAwareString cas = (ContextAwareString)val;
                    v = (String)cas.getObjectValue();
                } else if (val instanceof ContextAwareNumber) {
                    ContextAwareNumber can = (ContextAwareNumber)val;
                    v = String.valueOf(((Long)can.getObjectValue()));
                } else {
                    v = val.toString();
                }

                System.out.println("\t" + e.getKey() + " ---> " + v);

            }
        }
    }

    public static void printMap(String name, HashMap<String, Serializable> m) {

        System.out.println("Dumping maps name = " + name);

        if (m == null) {
            System.out.println("Null Map found");
            return;
        }

        Set<Entry<String, Serializable>> set = m.entrySet();
        Iterator<Entry<String, Serializable>> it = set.iterator();

        while (it.hasNext()) {
            Entry<String, Serializable> e = it.next();

            Object val = (Object)e.getValue();
            String v = "";

            if (val instanceof ContextAwareString) {
                ContextAwareString cas = (ContextAwareString)val;
                v = (String)cas.getObjectValue();
            } else if (val instanceof ContextAwareNumber) {
                ContextAwareNumber can = (ContextAwareNumber)val;
                v = String.valueOf(((Long)can.getObjectValue()));
            } else {
                if(val!=null)v = val.toString();
            }

            System.out.println("\t" + e.getKey() + " ---> " + v);

        }
    }



    public static Connection getOracleConnection() throws Exception {
        String driver = "oracle.jdbc.driver.OracleDriver";
//        String url = "jdbc:oracle:thin:@acsmt430.oracle.com:1521:testoim";
//        String username = "dev_oim";
//        String password = " Test0im1";


//        String url = "jdbc:oracle:thin:@adc4120297.us.oracle.com:5521:oimdb";
//        String username = "viam95_OIM";
//        String password = "welcome1";

//      String url = "jdbc:oracle:thin:@slc05mxi.us.oracle.com:1521:oimdb";
//      String username = "OIMR2PS2_OIM";
//      String password = "OIMR2PS2_OIM";
        
        String url = "jdbc:oracle:thin:@slc06utq.us.oracle.com:5521:oimdb";
        String username = "vslc06_OIM";
        String password = "welcome1";

        
        Class.forName(driver); // load Oracle driver
        Connection conn = DriverManager.getConnection(url, username, password);
        return conn;
    }



}

