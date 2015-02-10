package com.assign.test;

import com.assign.classify.ConditionalClassification;

import com.assign.hmm.HiddenMarkovModel;
import com.assign.hmm.HmmDecoder;


import com.assign.tag.TagLattice;
import com.assign.tag.ScoredTagging;
import com.assign.tag.Tagging;

import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;
import com.assign.tokenizer.RegExTokenizerFactory;

import com.assign.util.Streams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class run_tagger {

    static TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

    public static void main(String[] args)
            throws ClassNotFoundException, IOException,Exception {
        run_tagger obj = new run_tagger();
        String dir = "F://Semester1AY2010-11//CS4248//Assign-02//a2_data//Hidden-markov";

        System.out.println("Reading model from file=" + dir);
        FileInputStream fileIn = new FileInputStream(dir);
        ObjectInputStream objIn = new ObjectInputStream(fileIn);
        HiddenMarkovModel hmm = (HiddenMarkovModel) objIn.readObject();
        Streams.closeInputStream(objIn);
        HmmDecoder decoder = new HmmDecoder(hmm);

        InputStreamReader isReader = new InputStreamReader(System.in);
        BufferedReader bufReader = new BufferedReader(isReader);

        String dirTest = "F://Semester1AY2010-11//CS4248//Assign-02//a2_data//sents.test";
        ArrayList<String> sents = obj.readFile(dirTest);
        for (int i = 0; i < sents.size(); i++) {
            String line = sents.get(i);
            System.out.print("\n\nINPUT> ");
            System.out.flush();
            char[] cs = line.toCharArray();

            Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs, 0, cs.length);
            String[] tokens = tokenizer.tokenize();
            List<String> tokenList = Arrays.asList(tokens);

            //firstBest(tokenList, decoder);
            writeFile(tokenList, decoder);
            nBest(tokenList, decoder);
            confidence(tokenList, decoder);

        }
        Streams.closeReader(bufReader);
    }

    public ArrayList readFile(String dir) {


        ArrayList<String> sents = new ArrayList<String>();
        try {
            FileReader input = new FileReader(dir);
            BufferedReader bufRead = new BufferedReader(input);
            String line;
            int count = 0;
            line = bufRead.readLine();
            count++;
            while (line != null) {
                sents.add(line);
                System.out.println(count + ": " + line);

                line = bufRead.readLine();

                count++;

            }
            bufRead.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sents;

    }

    static void firstBest(List<String> tokenList, HmmDecoder decoder) {
        Tagging<String> tagging = decoder.tag(tokenList);
        System.out.println("\nFIRST BEST");
        for (int i = 0; i < tagging.size(); ++i) {
            System.out.print(tagging.token(i) + "/" + tagging.tag(i) + " ");
        }
        System.out.println();

    }

    static void writeFile(List<String> tokenList, HmmDecoder decoder) throws Exception {
        Tagging<String> tagging = decoder.tag(tokenList);
        FileWriter outFile = new FileWriter("F://Semester1AY2010-11//CS4248//Assign-02//a2_data//sents.out",true);
        PrintWriter out = new PrintWriter(outFile);
        for (int i = 0; i < tagging.size(); ++i) {
            // Create file
            out.append(tagging.token(i) + "/" + tagging.tag(i) + " ");
            //Close the output stream
            
        }
        out.println();
        out.close();
        
    }
    static final int MAX_N_BEST = 5;

    static void nBest(List<String> tokenList, HmmDecoder decoder) {
        System.out.println("\nN BEST");
        System.out.println("#   JointLogProb         Analysis");
        Iterator<ScoredTagging<String>> nBestIt = decoder.tagNBest(tokenList, MAX_N_BEST);
        for (int n = 0; n < MAX_N_BEST && nBestIt.hasNext(); ++n) {
            ScoredTagging<String> scoredTagging = nBestIt.next();
            double score = scoredTagging.score();
            System.out.print(n + "   " + format(score) + "  ");
            for (int i = 0; i < tokenList.size(); ++i) {
                System.out.print(scoredTagging.token(i) + "_" + pad(scoredTagging.tag(i), 5));
            }
            System.out.println();
        }
    }

    static void confidence(List<String> tokenList, HmmDecoder decoder) {
        System.out.println("\nCONFIDENCE");
        System.out.println("#   Token          (Prob:Tag)*");
        TagLattice<String> lattice = decoder.tagMarginal(tokenList);
        for (int tokenIndex = 0; tokenIndex < tokenList.size(); ++tokenIndex) {
            ConditionalClassification tagScores = lattice.tokenClassification(tokenIndex);
            System.out.print(pad(Integer.toString(tokenIndex), 4));
            System.out.print(pad(tokenList.get(tokenIndex), 15));
            for (int i = 0; i < 5; ++i) {
                double conditionalProb = tagScores.score(i);
                String tag = tagScores.category(i);
                System.out.print(" " + format(conditionalProb)
                        + ":" + pad(tag, 4));
            }
            System.out.println();
        }
    }

    static String format(double x) {
        return String.format("%9.3f", x);
    }

    static String pad(String in, int length) {
        if (in.length() > length) {
            return in.substring(0, length - 3) + "...";
        }
        if (in.length() == length) {
            return in;
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(in);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();

    }
}
