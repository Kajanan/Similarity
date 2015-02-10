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
import java.util.HashMap;

/**
 *
 * @author workshop
 */
public class SisterApps {

    public static final String DBDRIVER = "com.mysql.jdbc.Driver";
    public static final String DBURL =
            "jdbc:mysql://8sts12s.ddns.comp.nus.edu.sg:3306/Nargis";
    public static final String DBUSER = "crawler";
    public static final String DBPASS = "crawler";
    private Connection conn = null;
    private PreparedStatement pstmt;
    private PreparedStatement pstmt1;
    private PreparedStatement pstmt2;
    public ArrayList<String> GAppAllList = new ArrayList<String>();
    
    public SisterApps() throws ClassNotFoundException, SQLException {
        ConnectToDatabase();
    }

    public void ConnectToDatabase() throws ClassNotFoundException,
            SQLException {
        Class.forName(DBDRIVER);
        conn = (Connection) DriverManager.getConnection(DBURL, DBUSER, DBPASS);
    }

    public void FindSisterApps() throws SQLException {

        ArrayList<Integer> devIdList = DevList();
        System.out.println(devIdList.size());
        int devId = 0;
        String Name;
        int gAppID = 0;
        HashMap<Integer, ArrayList<AppPair>> list = new HashMap<Integer, ArrayList<AppPair>>();
        for (int i = 0; i < devIdList.size(); i++) {
            
            ArrayList<Integer> GAppIDList = new ArrayList<Integer>();
            ArrayList<String> GAppList = new ArrayList<String>();
            devId = devIdList.get(i);
            String sql = "Select C.Name,C.GAppID from KajananTO.Dev_Apple A, mobapp_2012_01.C_Global_Apps C where A.DevID = ? "
                    + " and A.GAppID = C.GAppID Order By A.GAppID DESC ";
            if (pstmt == null) {
                pstmt = (PreparedStatement) conn.prepareStatement(sql);
            }
            pstmt.setInt(1, devId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Name = rs.getString(1);
                gAppID = rs.getInt(2);
                GAppList.add(Name);
                GAppAllList.add(Name);
                GAppIDList.add(gAppID);
            }
            ArrayList<AppPair> pair = GetAppPair(GAppList, GAppIDList);

            list.put(devId, pair);
        }
        AppSimilarity obj = new AppSimilarity();
        obj.tfIDFCompute(GAppAllList);
        for (int i = 0; i < devIdList.size(); i++) {
            devId = devIdList.get(i);
            ArrayList<AppPair> pair = list.get(devId);
            for (int j = 0; j < pair.size(); j++) {
                AppPair apps = pair.get(j);
                String app1 = apps.getApp1();
                int appID1 = apps.getAppID1();
                String app2 = apps.getApp2();
                int appID2 = apps.getAppID2();
                //find the proximity here
                //double score = obj.computeSimilarity(app1, app2);
                //insertData(appID1, app1,appID2,app2, score);
                double score = obj.TFIDFSimilarity(app1, app2);
                //obj.EditDistanceSimilarity(app1, app2);
                insertData(appID1, app1,appID2,app2, score);
            }
            pstmt2.executeBatch();
            pstmt2.clearBatch();
           
        }
         System.out.println("Finished Insertion ");
    }

    public void insertData(int AppID1, String appName1,int AppID2,String appName2,double Score) {
        try {
            String sql = "Insert into KajananTO.Apple_App_Similarity Values(?,?,?,?,?,null,null) ;";
            if (pstmt2 == null) {
                pstmt2 = (PreparedStatement) conn.prepareStatement(sql);
            }
            pstmt2.setInt(1, AppID1);
            pstmt2.setString(2,appName1);
            pstmt2.setInt(3, AppID2);
            pstmt2.setString(4,appName2);
            pstmt2.setDouble(5, Score);
            pstmt2.addBatch();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<AppPair> GetAppPair(ArrayList<String> GAppList, ArrayList<Integer> GAppIDList) {
        ArrayList<AppPair> Pair = new ArrayList<AppPair>();
        String app1;
        int appID1;
        String app2;
        int appID2;
        for (int i = 0; i < GAppList.size() - 1; i++) {
            for (int j = i + 1; j < GAppList.size(); j++) {
                app1 = GAppList.get(i);
                appID1 = GAppIDList.get(i);
                app2 = GAppList.get(j);
                appID2 = GAppIDList.get(j);
                Pair.add(new AppPair(app1, appID1, app2, appID2));
            }
        }
        return Pair;

    }

    ArrayList<Integer> DevList() throws SQLException {
        ArrayList<Integer> devIdList = new ArrayList<Integer>();
        int devId = 0;
        String sql = "select distinct DevID from KajananTO.Dev_Apple ";
        if (pstmt1 == null) {
            pstmt1 = (PreparedStatement) conn.prepareStatement(sql);
        }
        ResultSet rs = pstmt1.executeQuery();
        while (rs.next()) {
            devId = rs.getInt(1);
            devIdList.add(devId);
        }
        return devIdList;

    }

    public static void main(String[] args) throws
            ClassNotFoundException, SQLException {
        // TODO code application logic here
        SisterApps s = new SisterApps();
        s.FindSisterApps();
    }
}
