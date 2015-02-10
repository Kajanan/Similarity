/* TestSentenceSimilarity.java */
package com.app.similarity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import com.sql.Configuration;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import com.assign.spell.EditDistance;
import com.assign.spell.JaccardDistance;
import com.assign.spell.JaroWinklerDistance;
import com.assign.spell.TfIdfDistance;
import com.assign.tokenizer.IndoEuropeanTokenizerFactory;
import com.assign.util.Proximity;

import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;
import com.assign.tokenizer.RegExTokenizerFactory;

/**
 * @author Kajanan Sangaralingam
 * @created Aug 19, 2009
 */
public class AppSimilarity {

    private java.sql.Connection conn;
    private Map<Integer, ArrayList<String>> App_DescMap = new HashMap<Integer, ArrayList<String>>(370000);
    private Map<Integer, AppData> App_DescMapClass = new HashMap<Integer, AppData>(370000);

    public void loadAppData() {
        try {
            //Make the Database Connection
            Configuration cf = new Configuration("etc/sql.corpus_mobapps.properties");
            String connectionString = cf.getConnectionString();
            conn = DriverManager.getConnection(connectionString);
            System.out.println("Start loading Apple Data...");
            fillApple_DescMap();
            System.out.println("Finished loading Apple Data...");
            System.out.println("Start loading Android Data...");
            fillAndroid_DescMap();
            System.out.println("Finished loading Android Data...");
            System.out.println("Start loading Windows Data...");
            fillWindows_DescMap();
            System.out.println("Finished loading Windows Data...");
            System.out.println("Start loading BB Data...");
            fillBB_DescMap();
            System.out.println("Finished loading BB Data");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findSimilarityScoreDev() {
        try {

            TfIdfDistance tfIdf = new TfIdfDistance(tokenizerFactory);

            double score = 0.0;
            for (Iterator it = App_DescMap.keySet().iterator(); it.hasNext();) {
                int GAppID1 = Integer.parseInt(it.next().toString());
                ArrayList<String> list = App_DescMap.get(GAppID1);
                String desctfidf = list.get(3);
                tfIdf.handle(desctfidf);
            }
            System.out.println("Finished TF-IDF Computation");

            for (Iterator it = App_DescMap.keySet().iterator(); it.hasNext();) {
                int GAppID1 = Integer.parseInt(it.next().toString());
                ArrayList<String> list = App_DescMap.get(GAppID1);
                int AppID1 = Integer.parseInt(list.get(0));
                int source1 = Integer.parseInt(list.get(1));
                String name1 = list.get(2);
                String desc1 = list.get(3);
                System.out.println(App_DescMap.size());
                System.out.println("Upper Loop");
                for (Iterator ite = App_DescMap.keySet().iterator(); ite.hasNext();) {
                    int GAppID2 = Integer.parseInt(ite.next().toString());
                    ArrayList<String> list2 = App_DescMap.get(GAppID2);
                    int AppID2 = Integer.parseInt(list2.get(0));
                    int source2 = Integer.parseInt(list2.get(1));
                    String name2 = list2.get(2);
                    String desc2 = list2.get(3);
                    if (source1 == source2) {
                        break;
                    }
                    if (GAppID1 != GAppID2 && source1 != source2 && name1.equalsIgnoreCase(name2)) {
                        insert_dev_similarity(GAppID1, AppID1, source1, name1, GAppID2, AppID2, source2, name2, 1);
                        System.out.println("Inserted exactly similar Name App");
                    } else if (GAppID1 != GAppID2 && source1 != source2) {
                        score = tfIdf.proximity(desc1, desc2);
                        if (score > 0.8) {
                            insert_dev_similarity(GAppID1, AppID1, source1, name1, GAppID2, AppID2, source2, name2, score);
                            System.out.println("Inserted almost similar Name App");
                        } else {
                            continue;
                        }
                    }

                }
            }
            System.out.println("Finished Insertion!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    static final Proximity<CharSequence> P1 = new EditDistance(false);
    static final Proximity<CharSequence> P2 = new EditDistance(true);

    public void EditDistanceSimilarity(String appName1, String appName2) {
        double p1Score = P1.proximity(appName1, appName2);
        double p2Score = P2.proximity(appName1, appName2);

        System.out.println(appName1 + " == " + appName2 + "1. P1 " + p1Score);
        System.out.println("2. P2 " + p1Score);
        System.out.println("-----------------");
    }

    TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    TfIdfDistance tfIdf = new TfIdfDistance(tokenizerFactory);

    public void tfIDFCompute(ArrayList<String> appNameList) {
        for (int i = 0; i < appNameList.size(); i++) {
            tfIdf.handle(appNameList.get(i));
        }

    }

    public double TFIDFSimilarity(String appName1, String appName2) {
        double score = tfIdf.proximity(appName1, appName2);
        return score;

        //System.out.println(appName1 + " ---- "+ appName2 +" = " +tfIdf.proximity(appName1, appName2));
    }

    public double computeSimilarity(String appName1, String appName2) {
        JaroWinklerDistance jaroWinkler = JaroWinklerDistance.JARO_WINKLER_DISTANCE;
        double jaro = jaroWinkler.distance(appName1, appName2);
//        System.out.println("JaroWinkler " + jaro);
//         String line = "Free Cell.+";
//        char[] cs = line.toCharArray();
//        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs, 0, cs.length);
//        String[] tokens = tokenizer.tokenize();
//        List<String> tokenList = Arrays.asList(tokens);
//        System.out.println(tokenList.toString());

        JaccardDistance jaccard = new JaccardDistance(TOKENIZER_FACTORY);
//        String appName1 = "2010 Awards Edition! - Film Bot's Movie I.Q. (FREE)";
//        String appName2 = "2010 Awards Edition! - Film Bot's Movie I.Q. ";
        double score = jaccard.proximity(appName1, appName2);
        return score;

    }

    private void findSimilarityScoreDevAppData() {
        try {

            JaccardDistance jaccard = new JaccardDistance(tokenizerFactory);

            JaroWinklerDistance jaroWinkler = JaroWinklerDistance.JARO_WINKLER_DISTANCE;

            double score = 0.0;
            for (Iterator it = App_DescMapClass.keySet().iterator(); it.hasNext();) {
                int GAppID1 = Integer.parseInt(it.next().toString());
                AppData list = App_DescMapClass.get(GAppID1);
                String desctfidf = list.getAppDesc();
                tfIdf.handle(desctfidf);
            }
            System.out.println("Finished TF-IDF Computation");

            for (Iterator it = App_DescMapClass.keySet().iterator(); it.hasNext();) {
                int GAppID1 = Integer.parseInt(it.next().toString());
                AppData list = App_DescMapClass.get(GAppID1);
                int AppID1 = list.getAppID();
                int source1 = list.getSource();
                String name1 = list.getAppName();
                String desc1 = list.getAppDesc();
                System.out.println(App_DescMapClass.size());
                System.out.println("Upper Loop");
                for (Iterator ite = App_DescMapClass.keySet().iterator(); ite.hasNext();) {
                    int GAppID2 = Integer.parseInt(ite.next().toString());
                    AppData list2 = App_DescMapClass.get(GAppID2);
                    int AppID2 = list2.getAppID();
                    int source2 = list2.getSource();
                    String name2 = list2.getAppName();
                    String desc2 = list2.getAppDesc();
                    if (source1 == source2) {
                        break;
                    }
                    if (GAppID1 != GAppID2 && source1 != source2 && name1.equalsIgnoreCase(name2)) {
                        insert_dev_similarity(GAppID1, AppID1, source1, name1, GAppID2, AppID2, source2, name2, 1);
                        System.out.println("Inserted exactly similar Name App");
                    } else if (GAppID1 != GAppID2 && source1 != source2) {
                        score = jaroWinkler.proximity(desc1, desc2);
                        if (score > 0.8) {
                            insert_dev_similarity(GAppID1, AppID1, source1, name1, GAppID2, AppID2, source2, name2, score);
                            System.out.println("Inserted almost similar Name App");
                        } else {
                            continue;
                        }
                    }

                }
            }
            System.gc();
            System.out.println("Finished Insertion!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    PreparedStatement pst_insert = null;

    private void insert_dev_similarity(int GAppID1, int appID1, int source1, String appName1, int GAppID2, int appID2, int source2, String appName2, double score) {
        try {
            if (pst_insert == null) {
                String insert_sql
                        = "INSERT INTO Kajanan.Global_App_Similarity(`GAppID1`, `AppID1`, `Source1`, `Name1`, `GAppID2`, `AppID2`, `Source2`, `Name2`, `Similarity` ) "
                        + "   VALUES (?, ?, ?,?,?,?,?,?,?) ";
                pst_insert = conn.prepareStatement(insert_sql);
                pst_insert.setInt(1, GAppID1);
                pst_insert.setInt(2, appID1);
                pst_insert.setInt(3, source1);
                pst_insert.setString(4, appName1);
                pst_insert.setInt(5, GAppID2);
                pst_insert.setInt(6, appID2);
                pst_insert.setInt(7, source2);
                pst_insert.setString(8, appName2);
                pst_insert.setDouble(9, score);
                pst_insert.executeUpdate();
            }
            pst_insert.close();
            pst_insert = null;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void fillApple_DescMap() throws Exception {

        String sql = "SELECT C.`GAppID`,C.`AppID`, C.`Source`,C.`Name`,A.`Description` "
                + " FROM mobapps.C_Global_Apps C, mobapps.Apple_App A Where A.AppID = C.AppID and C.Source= 1 "
                + " limit 10000; ";
        java.sql.Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            int GAppID = rs.getInt(1);
            int AppID = rs.getInt(2);
            int Source = rs.getInt(3);
            String appName = rs.getString(4);
            String desc = rs.getString(5);
//            ArrayList<String> list = new ArrayList<String>();
//            list.add(0, AppID);
//            list.add(1, Source);
//            list.add(2, appName);
//            list.add(3, desc);
//            App_DescMap.put(GAppID, list);
            AppData obj = new AppData();
            obj.setAppID(AppID);
            obj.setSource(Source);
            obj.setAppName(appName);
            obj.setAppDesc(desc);
            App_DescMapClass.put(GAppID, obj);
        }
        rs.close();
        st.close();

    }

    private void fillAndroid_DescMap() throws Exception {

        String sql = "SELECT C.`GAppID`,C.`AppID`, C.`Source`,C.`Name`,A.`Description` "
                + " FROM mobapps.C_Global_Apps C,mobapps.MAndroid_App A Where A.AppID = C.AppID and C.Source= 5"
                + "  limit 10000; ";
        java.sql.Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            int GAppID = rs.getInt(1);
            int AppID = rs.getInt(2);
            int Source = rs.getInt(3);
            String appName = rs.getString(4);
            String desc = rs.getString(5);
            AppData obj = new AppData();
            obj.setAppID(AppID);
            obj.setSource(Source);
            obj.setAppName(appName);
            obj.setAppDesc(desc);
            App_DescMapClass.put(GAppID, obj);
        }
        rs.close();
        st.close();

    }

    private void fillWindows_DescMap() throws Exception {

        String sql = "SELECT C.`GAppID`,C.`AppID`, C.`Source`,C.`Name`,A.`Description` "
                + " FROM mobapps.C_Global_Apps C,mobapps.Win7_App A Where A.AppID = C.AppID and C.Source= 4"
                + " ;";
        java.sql.Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            int GAppID = rs.getInt(1);
            int AppID = rs.getInt(2);
            int Source = rs.getInt(3);
            String appName = rs.getString(4);
            String desc = rs.getString(5);
            AppData obj = new AppData();
            obj.setAppID(AppID);
            obj.setSource(Source);
            obj.setAppName(appName);
            obj.setAppDesc(desc);
            App_DescMapClass.put(GAppID, obj);
        }
        rs.close();
        st.close();

    }

    private void fillBB_DescMap() throws Exception {

        String sql = "SELECT C.`GAppID`,C.`AppID`, C.`Source`,C.`Name`,A.`Description` "
                + " FROM mobapps.C_Global_Apps C, mobapps.BB_App A Where A.AppID = C.AppID and C.Source= 2"
                + " ;";
        java.sql.Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            int GAppID = rs.getInt(1);
            String AppID = Integer.toString(rs.getInt(2));
            String Source = Integer.toString(rs.getInt(3));
            String appName = rs.getString(4);
            String desc = rs.getString(5);
            ArrayList<String> list = new ArrayList<String>();
            list.add(0, AppID);
            list.add(1, Source);
            list.add(2, appName);
            list.add(3, desc);
            App_DescMap.put(GAppID, list);
        }
        rs.close();
        st.close();

    }

    public void close() {
        try {
            conn.close();
        } catch (Exception ex) {
        }
    }
    PreparedStatement pst_null_update = null;

    static String[] splitOnCapitals(String str) {
        ArrayList<String> array = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        int min = 0;
        int max = 0;
        for (int i = 0; i < str.length(); i++) {
            if (Character.isUpperCase(str.charAt(i))) {
                String line = builder.toString().trim();
                if (line.length() > 0) {
                    array.add(line);
                }
                builder = new StringBuilder();
            }
            builder.append(str.charAt(i));
        }
        array.add(builder.toString().trim()); // get the last little bit too
        return array.toArray(new String[0]);
    }
    static ArrayList<String> fileNames;

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                fileNames.add(fileEntry.getName());
            }
        }
    }

    private  void tfidfhandler() throws FileNotFoundException, IOException {
        for (String file : fileNames) {
            file = "topicsimilarity/short_long/" + file;
            Scanner sc = new Scanner(new FileReader(file));
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] tab = line.split("\t");
                line = line.replace(tab[0], "").trim();
                tfIdf.handle(line);
            }
        }
        System.out.println("Finished computing the TF-IDF");
    }

    private void readFile() throws FileNotFoundException, IOException {
        int size = fileNames.size();
        int i = 0;
        for (String file : fileNames) {
            if (i == 0) {
                file = "topicsimilarity/short_long/" + file;
                Scanner sc = new Scanner(new FileReader(file));
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    String[] tab = line.split("\t");
                    line = line.replace(tab[0], "").trim();
                    //System.out.println(i + line);
                    readotherFiles(line);
                    System.out.println("*****************************************************");
                }
            }
            i++;
        }
    }
    

    private void readotherFiles(String text) throws FileNotFoundException, IOException {
        for (int k = 1; k < fileNames.size(); k++) {
            String file = "topicsimilarity/short_long/" + fileNames.get(k);
            Scanner sc = new Scanner(new FileReader(file));
            double jacard_score = 0;
            double tfidf_score = 0;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] tab = line.split("\t");
                line = line.replace(tab[0], "").trim();
//                jacard_score = jacard_score + computeSimilarity(text, line);
//                tfidf_score  = tfidf_score + TFIDFSimilarity(text, line);
                jacard_score = computeSimilarity(text, line);
                tfidf_score  =  TFIDFSimilarity(text, line);
                System.out.println(text+">>"+line+">>"+jacard_score+">>"+tfidf_score+"\n");
            }
//            double score = jacard_score / countLines(file);
//            
//            System.out.println("Overall Similarity is>>" + score);
//            score = tfidf_score / countLines(file);
//            System.out.println("Overall TF-IDF Similarity is>>" + score);
        }
    }

    public static int countLines(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
    }
    static TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

    public static void main(String[] args) throws Throwable {

        AppSimilarity obj = new AppSimilarity();
        fileNames = new ArrayList<String>();
        String dir = "topicsimilarity/short_long/";
        final File folder = new File(dir);
        obj.listFilesForFolder(folder);
        obj.tfidfhandler();
        obj.readFile();
//        obj.loadAppData();
//        obj.findSimilarityScoreDevAppData();
//        obj.EditDistanceSimilarity("Apple Shooting Colors HD", "Apple Shooting Colors HD Lite");
        String text1 = "golf	open	woods	play	tiger	round	championship	pga	mcilroy	shot	british	rory	win	par";
        String text2 = "league	minutes	win	minute	goal	side	champions	home	united	half	victory	game	penalty	points	draw";
        System.out.println("Jaccard Disttance: " + obj.computeSimilarity(text1, text2));

        //score = obj.TFIDFSimilarity("Free Cell.+", "FreeCell.+ HD");
        //System.out.println(score);
//        String test = "3/4Ton truCk";
//        String[] arr = splitOnCapitals(test);
//        for (String s : arr) {
//            System.out.println(s);
//        }
//
//        test = "Start with Capital";
//        arr = splitOnCapitals(test);
//        for (String s : arr) {
//            System.out.println(s);
//        }
        System.out.println("");
        String line = "FreeCell.+";
        char[] cs = line.toCharArray();
        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs, 0, cs.length);
        String[] tokens = tokenizer.tokenize();
        List<String> tokenList = Arrays.asList(tokens);
        System.out.println(tokenList.toString());
    }
}
