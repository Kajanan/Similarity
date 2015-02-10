/* TestSentenceSimilarity.java */
package com.app.similarity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import com.assign.spell.EditDistance;
import com.assign.spell.JaccardDistance;
import com.assign.spell.TfIdfDistance;
import com.assign.tokenizer.IndoEuropeanTokenizerFactory;
import com.assign.util.Proximity;

import com.assign.tokenizer.TokenizerFactory;
import com.assign.tokenizer.RegExTokenizerFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author  Ryan Shaw
 * @created Aug 19, 2009
 */
public class AppSimilarity_Apple_Android {

    private static Logger logger;
    private java.sql.Connection conn;
    private Map<Integer, ArrayList<String>> App_DescMap = new HashMap<Integer, ArrayList<String>>(370000);
    private Map<Integer, AppData> App_DescMapClass = new HashMap<Integer, AppData>(370000);
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
        //logger.info(score);
        return score;

        //System.out.println(appName1 + " ---- "+ appName2 +" = " +tfIdf.proximity(appName1, appName2));
    }

    public double computeSimilarity(String appName1, String appName2) {
//        JaroWinklerDistance jaroWinkler = JaroWinklerDistance.JARO_WINKLER_DISTANCE;
//         String line = "Free Cell.+";
//        char[] cs = line.toCharArray();
//        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs, 0, cs.length);
//        String[] tokens = tokenizer.tokenize();
//        List<String> tokenList = Arrays.asList(tokens);
//        System.out.println(tokenList.toString());

        JaccardDistance jaccard = new JaccardDistance(TOKENIZER_FACTORY);
        appName1 = "Ghost Stories 1000+";
        appName2 = "Free Ghost Stories";
        double score = jaccard.proximity(appName1, appName2);
        logger.info("Similarity" + score);
        return score;

    }
    PreparedStatement pst_insert = null;

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
    ArrayList<String> appNamelist = new ArrayList<String>();
    TreeMap<String, String> appIDName = new TreeMap<String, String>();

    public void processAppIDfile(AppSimilarity_Apple_Android demog, String filename) throws SQLException {
        ArrayList<String> lineset = demog.read(filename);

        for (int i = 0; i < lineset.size(); i++) {
            StringBuilder build = new StringBuilder();
            String strLine = lineset.get(i);
            //logger.info(strLine);
            String[] tokens = strLine.split("\t");
            String appID = null;
            String appName = null;
            appID = tokens[0];
            if (tokens.length == 2) {
                appName = tokens[1];
            } else {
                for (int j = 1; j < tokens.length; j++) {
                    build.append(tokens[i]).append(" ");
                }
                appName = build.toString().trim();
            }
            appIDName.put(appID, appName);
            appNamelist.add(appName);
        }
        tfIDFCompute(appNamelist);
    }
    double score = 0;
     double lastscore = 0;
    public void findsimilarapps(String fileName) {
        ArrayList<String> lineset = read(fileName);

        for (int i = 0; i < lineset.size(); i++) {
            StringBuilder build = new StringBuilder();
            String strLine = lineset.get(i);
            String[] tokens = strLine.split("\t");
            String similarApp = "";
            String similarAppID = "";
            String appid = "";
            String appname = "";
            
            for (Iterator it = appIDName.keySet().iterator(); it.hasNext();) {
                appid = (String) it.next();
                appname = appIDName.get(appid).toString();
              
                score = TFIDFSimilarity(appname, tokens[0]);
                if (score >= 0.6 && score > lastscore) {
                    similarApp = appname;
                    similarAppID = appid;
                    lastscore = score;
                }

            }
            if(lastscore > 0.6){
                //logger.info(tokens[0] + "\t" + tokens[1] + "\t" + tokens[2] + "\t" + similarApp + "\t" + similarAppID + "\t" + lastscore);
                 logger.info(tokens[0] + "\t" +  "\t" + similarApp + "\t" + similarAppID + "\t" + lastscore);
                lastscore = 0; 
            }else{
                 logger.info(tokens[0] + "\t"  + "\t" + "No Similar App" + "\t" + "No Similar App"+ "\t" + lastscore);
            }

        }
    }

    public ArrayList<String> read(String file) {
        ArrayList<String> list = new ArrayList<String>();
        try {
            BufferedReader b = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String content = "";
            while ((content = b.readLine()) != null) {
                list.add(content);
            }
        } catch (Exception e) {
            logger.error("Error in reading the file");

        } finally {
            return list;
        }
    }
    static TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

    public static void main(String[] args) throws Throwable {

        AppSimilarity_Apple_Android obj = new AppSimilarity_Apple_Android();
        PropertyConfigurator.configure("./etc/log4j.properties");
        logger = Logger.getLogger(AppSimilarity_Apple_Android.class);
        logger.info("Similarity ");
        //obj.computeSimilarity(null, null);        
        String fileName = "F:/MW/Similarity/etc/appnames.csv";
        obj.processAppIDfile(obj, fileName);
        logger.info("Finished creating corpus");
        ///obj.TFIDFSimilarity("Ghost Stories 1000+", "Free Ghost Stories");
        obj.findsimilarapps("etc/test.csv");

    }
}
