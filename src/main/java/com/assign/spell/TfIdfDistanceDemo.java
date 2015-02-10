package com.assign.spell;

import com.assign.tokenizer.IndoEuropeanTokenizerFactory;
import com.assign.tokenizer.TokenizerFactory;

public class TfIdfDistanceDemo {

    public static void main(String[] args) {
        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        TfIdfDistance tfIdf = new TfIdfDistance(tokenizerFactory);
        String[] str = new String[2];
        if (args.length == 0) {
            str[0] = "Microsoft Game Studios|MEDIGAMES STUDIOS";
            str[1] = "Sigurd Snørteland|Sigurd Sn??rteland";
        }
        for (String s : str)
            tfIdf.handle(s);

        System.out.printf("\n  %18s  %8s  %8s\n",
                          "Term", "Doc Freq", "IDF");
        for (String term : tfIdf.termSet())
            System.out.printf("  %18s  %8d  %8.2f\n",
                              term,
                              tfIdf.docFrequency(term),
                              tfIdf.idf(term));

        for (String s1 : str) {
            for (String s2 : str) {
                System.out.println("\nString1=" + s1);
                System.out.println("String2=" + s2);
                System.out.printf("distance=%4.2f  proximity=%4.2f\n",
                                  tfIdf.distance(s1,s2),
                                  tfIdf.proximity(s1,s2));
            }
        }
        JaroWinklerDistance jaroWinkler = JaroWinklerDistance.JARO_WINKLER_DISTANCE;
        System.out.printf("%18s  %18s %5s  %5s\n",
                    "String-1","String-2","Distance","Proximity");
        for (String s : str) {
            String[] pair = s.split("\\|");
            String s1 = pair[0];
            String s2 = pair[1];
            
            System.out.printf("%18s  %18s  %5.3f  %5.3f\n",
                    s1, s2,
                    jaroWinkler.distance(s1, s2),
                    jaroWinkler.proximity(s1, s2));
        }
    }
}
