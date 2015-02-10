/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.app.similarity;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Kajanan
 */
public class SisterAppList {

    public static final String DBDRIVER = "com.mysql.jdbc.Driver";
    public static final String DBURL = "jdbc:mysql://8sts12s.ddns.comp.nus.edu.sg:3306/Kajanan";
    public static final String DBUSER = "crawler";
    public static final String DBPASS = "crawler";
    private Connection conn = null;

    public void ConnectToDatabase() throws ClassNotFoundException, SQLException {
        Class.forName(DBDRIVER);
        conn = (Connection) DriverManager.getConnection(DBURL, DBUSER, DBPASS);
    }

    public SisterAppList() throws ClassNotFoundException, SQLException {
        ConnectToDatabase();
    }
    private PreparedStatement pstmt;

    public ArrayList<Integer> GetAppIDs(String param) throws SQLException {
        int gAppID = 1;

        HashSet<Integer> AppIDlist = new HashSet<Integer>();
        String tabName = "Kajanan";
        if (param.equals("Apple")) {
            tabName = "distinct(A.AppID1) as GAppID From KajananTO.Apple_App_Similarity A ";
        } else if (param.equals("Android")) {
            tabName = "distinct(A.AppID1) as GAppID From KajananTO.Android_App_Similarity A ";
        }
        String sql = "Select " + tabName;

        if (pstmt == null) {
            pstmt = (PreparedStatement) conn.prepareStatement(sql);
        }

        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            gAppID = rs.getInt("GAppID");
            AppIDlist.add(gAppID);
        }

        Object id[] = AppIDlist.toArray();
        ArrayList<Integer> listId = new ArrayList<Integer>();
        for (int i = 0; i < id.length; i++) {
            int t = Integer.parseInt(id[i].toString());
            listId.add(t);
        }
        rs.close();
        pstmt.close();
        return listId;

    }
    private PreparedStatement pstmt1;

    public void findAllSisterAppIDs(ArrayList<Integer> appIDList) {
        try {
            for (int i = 0; i < appIDList.size(); i++) {
                int appID = appIDList.get(i);
                String sql = "SELECT A.`AppID2` FROM KajananTO.Apple_App_Similarity A Where A.AppID1 = ? and A.SisterApp = 1 ; ";

                if (pstmt1 == null) {
                    pstmt1 = (PreparedStatement) conn.prepareStatement(sql);
                }
                int appID2 = 0;
                ArrayList<String> sisterList = new ArrayList<String>();
                pstmt1.setInt(1, appID);
                ResultSet rs = pstmt1.executeQuery();
                while (rs.next()) {
                    appID2 = rs.getInt(1);
                    sisterList.add(Integer.toString(appID2));

                }
                if (sisterList.size() >= 1) {
                    addtoBatchSisterList(appID, sisterList);
                }
                if (pstmt2 != null) {
                    pstmt2.executeBatch();
                    pstmt2.clearBatch();
                }
                sisterList.clear();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    PreparedStatement pstmt2 = null;

    public void addtoBatchSisterList(int appID1, ArrayList<String> sisterList) {

        try {
            String sql = "Insert into KajananTO.Apple_App_SisterList Values(?,?) ;";
            if (pstmt2 == null) {
                pstmt2 = (PreparedStatement) conn.prepareStatement(sql);
            }
            String list = sisterList.toString();     
            int strtindex= list.indexOf('[');
            int endindex = list.indexOf(']');
            list = list.substring(1,endindex);
            pstmt2.setInt(1, appID1);
            pstmt2.setString(2, list);
            pstmt2.addBatch();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void main(String args[]) {
        try {
            SisterAppList obj = new SisterAppList();
            ArrayList<Integer> appIDs = obj.GetAppIDs("Apple");
            obj.findAllSisterAppIDs(appIDs);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
