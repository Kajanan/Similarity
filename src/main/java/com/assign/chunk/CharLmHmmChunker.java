

package com.assign.chunk;

import com.assign.corpus.ObjectHandler;

import com.assign.hmm.AbstractHmmEstimator;
import com.assign.hmm.HiddenMarkovModel;
import com.assign.hmm.HmmDecoder;

import com.assign.symbol.SymbolTable;

import com.assign.tag.Tagging;

import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;

import com.assign.util.AbstractExternalizable;
import com.assign.util.Compilable;
import com.assign.util.ObjectToCounterMap;
import com.assign.util.Strings;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class CharLmHmmChunker extends HmmChunker
    implements ObjectHandler<Chunking>, 
               Compilable  {

    private final boolean mValidateTokenizer = false;

    private final AbstractHmmEstimator mHmmEstimator;
    private final TokenizerFactory mTokenizerFactory;
    private final Set<String> mTagSet = new HashSet<String>();

    private final boolean mSmoothTags;

    
    public CharLmHmmChunker(TokenizerFactory tokenizerFactory,
                            AbstractHmmEstimator hmmEstimator) {
        this(tokenizerFactory,hmmEstimator,false);
    }

    
    public CharLmHmmChunker(TokenizerFactory tokenizerFactory,
                            AbstractHmmEstimator hmmEstimator,
                            boolean smoothTags) {
        super(tokenizerFactory,new HmmDecoder(hmmEstimator));
        mHmmEstimator = hmmEstimator;
        mTokenizerFactory = tokenizerFactory;
        mSmoothTags = smoothTags;
        smoothBoundaries();
    }

    
    public AbstractHmmEstimator getHmmEstimator() {
        return mHmmEstimator;
    }

    
    @Override
    public TokenizerFactory getTokenizerFactory() {
        return mTokenizerFactory;
    }

   
    public void trainDictionary(CharSequence cSeq, String type) {
        char[] cs = Strings.toCharArray(cSeq);
        Tokenizer tokenizer = getTokenizerFactory().tokenizer(cs,0,cs.length);
        String[] tokens = tokenizer.tokenize();
        if (tokens.length < 1) {
            String msg = "Did not find any tokens in entry."
                + "Char sequence=" + cSeq;
            throw new IllegalArgumentException(msg);
        }
        AbstractHmmEstimator estimator = getHmmEstimator();
        SymbolTable table = estimator.stateSymbolTable();
        smoothBaseTag(type,table,estimator);
        if (tokens.length == 1) {
            estimator.trainEmit("W_" + type, tokens[0]);
            return;
        }
        String initialTag = "B_" + type;
        estimator.trainEmit(initialTag, tokens[0]);
        String prevTag = initialTag;
        for (int i = 1; i+1 < tokens.length; ++i) {
            String tag = "M_" + type;
            estimator.trainEmit(tag, tokens[i]);
            estimator.trainTransit(prevTag,tag);
            prevTag = tag;
        }
        String finalTag = "E_" + type;
        estimator.trainEmit(finalTag, tokens[tokens.length-1]);
        estimator.trainTransit(prevTag,finalTag);
    }

   
    public void handle(Chunking chunking) {
        CharSequence cSeq = chunking.charSequence();
        char[] cs = Strings.toCharArray(cSeq);

        Set<Chunk> chunkSet = chunking.chunkSet();
        Chunk[] chunks = chunkSet.<Chunk>toArray(EMPTY_CHUNK_ARRAY);
        Arrays.sort(chunks,Chunk.TEXT_ORDER_COMPARATOR);

        List<String> tokenList = new ArrayList<String>();
        List<String> whiteList = new ArrayList<String>();
        List<String> tagList = new ArrayList<String>();
        int pos = 0;
        for (Chunk nextChunk : chunks) {
            String type = nextChunk.type();
            int start = nextChunk.start();
            int end = nextChunk.end();
            outTag(cs,pos,start,tokenList,whiteList,tagList,mTokenizerFactory);
            chunkTag(cs,start,end,type,tokenList,whiteList,tagList,mTokenizerFactory);
            pos = end;
        }
        outTag(cs,pos,cSeq.length(),tokenList,whiteList,tagList,mTokenizerFactory);
        String[] toks = tokenList.<String>toArray(Strings.EMPTY_STRING_ARRAY);
        String[] whites = whiteList.toArray(Strings.EMPTY_STRING_ARRAY);
        String[] tags = tagList.toArray(Strings.EMPTY_STRING_ARRAY);
        if (mValidateTokenizer
            && !consistentTokens(toks,whites,mTokenizerFactory)) {
            String msg = "Tokens not consistent with tokenizer factory."
                + " Tokens=" + Arrays.asList(toks)
                + " Tokenization=" + tokenization(toks,whites)
                + " Factory class=" + mTokenizerFactory.getClass();
            throw new IllegalArgumentException(msg);
        }
        handle(toks,whites,tags);
    }

    void handle(String[] tokens, String[] whitespaces, String[] tags) {
        Tagging<String> tagging
            = new Tagging<String>(Arrays.asList(tokens),
                                  Arrays.asList(trainNormalize(tags)));
        getHmmEstimator().handle(tagging);
        smoothTags(tags);
    }


    
    public void compileTo(ObjectOutput objOut) throws IOException {
        objOut.writeObject(new Externalizer(this));
    }

    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        java.util.Set<String> expandedTagSet = new java.util.TreeSet<String>();
        expandedTagSet.add("MM_O");
        expandedTagSet.add("WW_O_BOS");
        expandedTagSet.add("BB_O_BOS");
        expandedTagSet.add("EE_O_BOS");
        for (Object tag0 : mTagSet) {
            String x = tag0.toString();
            expandedTagSet.add("B_" + x);
            expandedTagSet.add("M_" + x);
            expandedTagSet.add("E_" + x);
            expandedTagSet.add("W_" + x);
            expandedTagSet.add("BB_O_" + x);
            expandedTagSet.add("EE_O_" + x);
            expandedTagSet.add("WW_O_" + x);
        }

        for (Object tag0Obj : expandedTagSet) {
            String tag0 = tag0Obj.toString();
            sb.append("\n");
            sb.append("start(" + tag0 + ")=" + mHmmEstimator.startLog2Prob(tag0));
            sb.append("\n");
            sb.append("  end(" + tag0 + ")=" + mHmmEstimator.endLog2Prob(tag0));
            sb.append("\n");
            for (Object tag1Obj : expandedTagSet) {
                String tag1 = tag1Obj.toString();
                sb.append("trans(" + tag0 + "," + tag1 + ")="
                                   + mHmmEstimator.transitLog2Prob(tag0,tag1));
                sb.append("\n");
            }
        }
        return sb.toString();
    }



    void smoothBoundaries() {
        
        AbstractHmmEstimator hmmEstimator = getHmmEstimator();
        SymbolTable table = hmmEstimator.stateSymbolTable();
        String bbO = "BB_O_BOS";
        String mmO = "MM_O";
        String eeO = "EE_O_BOS";
        String wwO = "WW_O_BOS";

        table.getOrAddSymbol(bbO);
        table.getOrAddSymbol(mmO);
        table.getOrAddSymbol(eeO);
        table.getOrAddSymbol(wwO);

        hmmEstimator.trainStart(bbO);
        hmmEstimator.trainStart(wwO);

        hmmEstimator.trainEnd(eeO);
        hmmEstimator.trainEnd(wwO);

        hmmEstimator.trainTransit(bbO,mmO);
        hmmEstimator.trainTransit(bbO,eeO);
        hmmEstimator.trainTransit(mmO,mmO);
        hmmEstimator.trainTransit(mmO,eeO);
    }

    void smoothTags(String[] tags) {
        if (!mSmoothTags) return;
        AbstractHmmEstimator hmmEstimator = getHmmEstimator();
        SymbolTable table = hmmEstimator.stateSymbolTable();
        for (int i = 0; i < tags.length; ++i)
            smoothTag(tags[i],table,hmmEstimator);
    }

    void smoothTag(String tag, SymbolTable table,
                   AbstractHmmEstimator hmmEstimator) {

        smoothBaseTag(HmmChunker.baseTag(tag), table, hmmEstimator);

    }

    void smoothBaseTag(String baseTag, SymbolTable table,
                       AbstractHmmEstimator hmmEstimator) {

        if (!mTagSet.add(baseTag)) return; // already added
        if ("O".equals(baseTag)) return;  // constructor + other tags smooth "O"

        String b_x = "B_" + baseTag;
        String m_x = "M_" + baseTag;
        String e_x = "E_" + baseTag;
        String w_x = "W_" + baseTag;

        String bb_o_x = "BB_O_" + baseTag;
        // String mm_o = "MM_O"; // no tag modifier, just constant
        String ee_o_x = "EE_O_" + baseTag;
        String ww_o_x = "WW_O_" + baseTag;

        table.getOrAddSymbol(b_x);
        table.getOrAddSymbol(m_x);
        table.getOrAddSymbol(e_x);
        table.getOrAddSymbol(w_x);

        table.getOrAddSymbol(bb_o_x);
        // table.getOrAddSymbol("MM_O");  // in constructor
        table.getOrAddSymbol(ee_o_x);
        table.getOrAddSymbol(ww_o_x);

        hmmEstimator.trainStart(b_x);
        hmmEstimator.trainTransit(b_x,m_x);
        hmmEstimator.trainTransit(b_x,e_x);

        hmmEstimator.trainTransit(m_x,m_x);
        hmmEstimator.trainTransit(m_x,e_x);

        hmmEstimator.trainEnd(e_x);
        hmmEstimator.trainTransit(e_x,bb_o_x);

        hmmEstimator.trainStart(w_x);
        hmmEstimator.trainEnd(w_x);
        hmmEstimator.trainTransit(w_x,bb_o_x);

        hmmEstimator.trainTransit(bb_o_x,"MM_O");

        hmmEstimator.trainTransit("MM_O",ee_o_x); // handles all MM_O to ends

        hmmEstimator.trainTransit(ee_o_x,b_x);
        hmmEstimator.trainTransit(ee_o_x,w_x);

        hmmEstimator.trainStart(ww_o_x);
        hmmEstimator.trainTransit(ww_o_x,b_x);
        hmmEstimator.trainTransit(ww_o_x,w_x);

        hmmEstimator.trainTransit(e_x,"WW_O_BOS");
        hmmEstimator.trainTransit(w_x,"WW_O_BOS");

        hmmEstimator.trainTransit(bb_o_x,"EE_O_BOS");
        hmmEstimator.trainTransit("BB_O_BOS",ee_o_x);

        for (String type : mTagSet) {
            if ("O".equals(type)) continue;
            if ("BOS".equals(type)) continue;
            String bb_o_y = "BB_O_" + type;
            String ww_o_y = "WW_O_" + type;
            String ee_o_y = "EE_O_" + type;
            String b_y = "B_" + type;
            String w_y = "W_" + type;
            String e_y = "E_" + type;
            hmmEstimator.trainTransit(e_x,ww_o_y);
            hmmEstimator.trainTransit(e_x,b_y);
            hmmEstimator.trainTransit(e_x,w_y);
            hmmEstimator.trainTransit(w_x,ww_o_y);
            hmmEstimator.trainTransit(w_x,b_y);
            hmmEstimator.trainTransit(w_x,w_y);
            hmmEstimator.trainTransit(e_y,b_x);
            hmmEstimator.trainTransit(e_y,w_x);
            hmmEstimator.trainTransit(e_y,ww_o_x);
            hmmEstimator.trainTransit(w_y,b_x);
            hmmEstimator.trainTransit(w_y,w_x);
            hmmEstimator.trainTransit(w_y,ww_o_x);
            hmmEstimator.trainTransit(bb_o_x,ee_o_y);
            hmmEstimator.trainTransit(bb_o_y,ee_o_x);
        }

    }

    static class Externalizer extends AbstractExternalizable {
        private static final long serialVersionUID = 4630707998932521821L;
        final CharLmHmmChunker mChunker;
        public Externalizer() {
            this(null);
        }
        public Externalizer(CharLmHmmChunker chunker) {
            mChunker = chunker;
        }
        @Override
        public Object read(ObjectInput in)
            throws ClassNotFoundException, IOException {

            TokenizerFactory tokenizerFactory
                = (TokenizerFactory) in.readObject();
            HiddenMarkovModel hmm
                = (HiddenMarkovModel) in.readObject();
            HmmDecoder decoder = new HmmDecoder(hmm);
            return new HmmChunker(tokenizerFactory,decoder);
        }
        @Override
        public void writeExternal(ObjectOutput objOut) throws IOException {
            AbstractExternalizable.compileOrSerialize(mChunker.getTokenizerFactory(),objOut);
            AbstractExternalizable.compileOrSerialize(mChunker.getHmmEstimator(),objOut);
        }
    }

   

    static final Chunk[] EMPTY_CHUNK_ARRAY = new Chunk[0];

    static void outTag(char[] cs, int start, int end,
                       List<String> tokenList, List<String> whiteList, List<String> tagList,
                       TokenizerFactory factory) {
        Tokenizer tokenizer = factory.tokenizer(cs,start,end-start);
        whiteList.add(tokenizer.nextWhitespace());
        String nextToken;
        while ((nextToken = tokenizer.nextToken()) != null) {
            tokenList.add(nextToken);
            tagList.add(ChunkTagHandlerAdapter2.OUT_TAG);
            whiteList.add(tokenizer.nextWhitespace());
        }

    }

    static void chunkTag(char[] cs, int start, int end, String type,
                         List<String> tokenList, List<String> whiteList, List<String> tagList,
                         TokenizerFactory factory) {
        Tokenizer tokenizer = factory.tokenizer(cs,start,end-start);
        String firstToken = tokenizer.nextToken();
        tokenList.add(firstToken);
        tagList.add(ChunkTagHandlerAdapter2.BEGIN_TAG_PREFIX + type);
        while (true) {
            String nextWhitespace = tokenizer.nextWhitespace();
            String nextToken = tokenizer.nextToken();
            if (nextToken == null) break;
            tokenList.add(nextToken);
            whiteList.add(nextWhitespace);
            tagList.add(ChunkTagHandlerAdapter2.IN_TAG_PREFIX + type);
        }
    }

    public static boolean consistentTokens(String[] toks,
                                           String[] whitespaces,
                                           TokenizerFactory tokenizerFactory) {
        if (toks.length+1 != whitespaces.length) return false;
        char[] cs = getChars(toks,whitespaces);
        Tokenizer tokenizer = tokenizerFactory.tokenizer(cs,0,cs.length);
        String nextWhitespace = tokenizer.nextWhitespace();
        if (!whitespaces[0].equals(nextWhitespace)) {
            return false;
        }
        for (int i = 0; i < toks.length; ++i) {
            String token = tokenizer.nextToken();
            if (token == null) {
                return false;
            }
            if (!toks[i].equals(token)) {
                return false;
            }
            nextWhitespace = tokenizer.nextWhitespace();
            if (!whitespaces[i+1].equals(nextWhitespace)) {
                return false;
            }
        }
        return true;
    }

    List<String> tokenization(String[] toks, String[] whitespaces) {
        List<String> tokList = new ArrayList<String>();
        List<String> whiteList = new ArrayList<String>();
        char[] cs = getChars(toks,whitespaces);
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,0,cs.length);
        tokenizer.tokenize(tokList,whiteList);
        return tokList;
    }

    static char[] getChars(String[] toks, String[] whitespaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.length; ++i) {
            sb.append(whitespaces[i]);
            sb.append(toks[i]);
        }
        sb.append(whitespaces[whitespaces.length-1]);
        return Strings.toCharArray(sb);
    }

}
