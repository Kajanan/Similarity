package com.assign.chunk;

import com.assign.tokenizer.TokenCategorizer;
import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;

import com.assign.corpus.ObjectHandler;

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

public class TrainTokenShapeChunker
    implements ObjectHandler<Chunking>, 
               Compilable {

    private final boolean mValidateTokenizer = false;

    private final int mKnownMinTokenCount;
    private final int mMinTokenCount;
    private final int mMinTagCount;
    private final TokenCategorizer mTokenCategorizer;
    private final TokenizerFactory mTokenizerFactory;

    private final TrainableEstimator mTrainableEstimator;
    private final List<String> mTokenList = new ArrayList<String>();
    private final List<String> mTagList = new ArrayList<String>();

    public TrainTokenShapeChunker(TokenCategorizer categorizer,
                                  TokenizerFactory factory) {
        this(categorizer,factory,
             8, 1, 1);
    }

 
    public TrainTokenShapeChunker(TokenCategorizer categorizer,
                                  TokenizerFactory factory,
                                  int knownMinTokenCount,
                                  int minTokenCount,
                                  int minTagCount) {
        mTokenCategorizer = categorizer;
        mTokenizerFactory = factory;
        mKnownMinTokenCount = knownMinTokenCount;
        mMinTokenCount = minTokenCount;
        mMinTagCount = minTagCount;
        mTrainableEstimator = new TrainableEstimator(categorizer);
    }

    void handle(String[] tokens, String[] whitespaces, String[] tags) {

        if (tokens.length != tags.length) {
            String msg = "Tokens and tags must be same length."
                + " Found tokens.length=" + tokens.length
                + " tags.length=" + tags.length;
            throw new IllegalArgumentException(msg);
        }

        for (int i = 0; i < tokens.length; ++i) {
            if (tokens[i] == null || tags[i] == null) {
                String msg = "Tags and tokens must not be null."
                    + " Found tokens[" + i + "]=" + tokens[i]
                    + " tags[" + i + "]=" + tags[i];
                throw new NullPointerException(msg);
            }
            mTokenList.add(tokens[i]);
            mTagList.add(tags[i]);
        }
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

    public void compileTo(ObjectOutput objOut) throws IOException {
        objOut.writeObject(new Externalizer(this));
    }

    static class Externalizer extends AbstractExternalizable {
        private static final long serialVersionUID = 142720610674437597L;
        final TrainTokenShapeChunker mChunker;
        public Externalizer() {
            this(null);
        }
        public Externalizer(TrainTokenShapeChunker chunker) {
            mChunker = chunker;
        }
        @Override
        public Object read(ObjectInput in)
            throws ClassNotFoundException, IOException {

            TokenizerFactory factory = (TokenizerFactory) in.readObject();
            TokenCategorizer categorizer = (TokenCategorizer) in.readObject();

            

            CompiledEstimator estimator = (CompiledEstimator) in.readObject();
          
            TokenShapeDecoder decoder
                = new TokenShapeDecoder(estimator,categorizer,1000.0);

            return new TokenShapeChunker(factory,decoder);
        }
        @Override
        public void writeExternal(ObjectOutput objOut) throws IOException {
            int len = mChunker.mTagList.size();
            String[] tokens
                = mChunker.mTokenList.<String>toArray(Strings.EMPTY_STRING_ARRAY);
            String[] tags
                = mChunker.mTagList.<String>toArray(Strings.EMPTY_STRING_ARRAY);

       
            mChunker.mTrainableEstimator.handle(tokens,tags);

           
            mChunker.replaceUnknownsWithCategories(tokens);
            mChunker.mTrainableEstimator.handle(tokens,tags);
            mChunker.mTrainableEstimator.prune(mChunker.mMinTagCount,
                                               mChunker.mMinTokenCount);
         
            mChunker.mTrainableEstimator.smoothTags(1);

          
            AbstractExternalizable.compileOrSerialize(mChunker.mTokenizerFactory,objOut);
            AbstractExternalizable.compileOrSerialize(mChunker.mTokenCategorizer,objOut);
            mChunker.mTrainableEstimator.compileTo(objOut);
        }
    }

    

    void replaceUnknownsWithCategories(String[] tokens) {
        ObjectToCounterMap<String> counter = new ObjectToCounterMap<String>();
        for (int i = 0; i < tokens.length; ++i)
            counter.increment(tokens[i]);
        for (int i = 0; i < tokens.length; ++i)
            if (counter.getCount(tokens[i]) < mKnownMinTokenCount)
                tokens[i] = mTokenCategorizer.categorize(tokens[i]);
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

    static boolean consistentTokens(String[] toks,
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




