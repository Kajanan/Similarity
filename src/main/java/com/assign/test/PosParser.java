package com.assign.test;
import com.assign.corpus.ObjectHandler;
import com.assign.corpus.StringParser;

import com.assign.tag.Tagging;

import com.assign.util.Strings;

import java.util.ArrayList;
import java.util.List;

public class PosParser
    extends StringParser<ObjectHandler<Tagging<String>>> {

    public void parseString(char[] cs, int start, int end) {
        String in = new String(cs,start,end-start);
        String[] sentences = in.split("\n");
        for (int i = 0; i < sentences.length; ++i) {
            if (Strings.allWhitespace(sentences[i])) continue;
            if (sentences[i].indexOf('/') < 0) continue;
            processSentence(sentences[i]);
        }
    }

    private void processSentence(String sentence) {
        String[] tagTokenPairs = sentence.split(" ");
        List<String> tokenList = new ArrayList<String>(tagTokenPairs.length);
        List<String> tagList = new ArrayList<String>(tagTokenPairs.length);
        
        for (String pair : tagTokenPairs) {
            int j = pair.lastIndexOf('/');
            String token = pair.substring(0,j).trim();
            String tag = pair.substring(j+1).trim();
            tokenList.add(token);
            tagList.add(tag);
        }
        Tagging<String> tagging
            = new Tagging<String>(tokenList,tagList);
        getHandler().handle(tagging);
    }




}



