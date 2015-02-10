package com.assign.test;

import com.assign.io.FileExtensionFilter;
import com.assign.corpus.Parser;
import com.assign.corpus.ObjectHandler;
import com.assign.hmm.HmmCharLmEstimator;
import com.assign.tag.Tagging;
import com.assign.util.AbstractExternalizable;

import java.io.File;
import java.io.IOException;

public class build_tagger {

    // language model parameters for HMM emissions
    static int N_GRAM = 8;
    static int NUM_CHARS = 256;
    static double LAMBDA_FACTOR = 8.0;
    static String path_train ;
    static String path_devt ;
    static String model_file;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            path_train = args[0];
            path_devt = args[1];
            model_file = args[2];
        }else{
            path_train = "F://Semester1AY2010-11//CS4248//Assign-02//a2_data//sents.train";
            path_devt = "F://Semester1AY2010-11//CS4248//Assign-02//a2_data//sents.devt";
            model_file = "F://Semester1AY2010-11//CS4248//Assign-02//a2_data//Hidden-markov";
        }
        // set up parser with estimator as handler
        HmmCharLmEstimator estimator = new HmmCharLmEstimator(N_GRAM, NUM_CHARS, LAMBDA_FACTOR);
        Parser<ObjectHandler<Tagging<String>>> parser = new PosParser();
        parser.setHandler(estimator);

        // train on files in data directory ending in "ioc"
        File dataDir = new File(path_train);
        File file_2 = new File(path_devt);
        String[] extension = {"train", "devt"};
        //File[] files = dataDir.listFiles(new FileExtensionFilter(extension));
        File[] files = {dataDir,file_2};

        for (File file : files) {
            System.out.println("Training file=" + file);
            parser.parse(file);
        }

        // write output to file
        File modelFile = new File(model_file);
        AbstractExternalizable.compileTo(estimator, modelFile);
    }
}
