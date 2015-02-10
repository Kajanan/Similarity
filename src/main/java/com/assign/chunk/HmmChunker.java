
package com.assign.chunk;

import com.assign.hmm.HmmDecoder;

import com.assign.symbol.SymbolTable;

import com.assign.tag.ScoredTagging;
import com.assign.tag.TagLattice;

import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;

import com.assign.util.BoundedPriorityQueue;
import com.assign.util.Iterators;
import com.assign.util.Scored;
import com.assign.util.ScoredObject;
import com.assign.util.Strings;
import static com.assign.util.Math.naturalLogToBase2Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class HmmChunker implements NBestChunker, ConfidenceChunker {

    private final TokenizerFactory mTokenizerFactory;
    private final HmmDecoder mDecoder;

    public HmmChunker(TokenizerFactory tokenizerFactory,
                      HmmDecoder decoder) {
        mTokenizerFactory = tokenizerFactory;
        mDecoder = decoder;
    }

    public HmmDecoder getDecoder() {
        return mDecoder;
    }

    public TokenizerFactory getTokenizerFactory() {
        return mTokenizerFactory;
    }

    public Chunking chunk(char[] cs, int start, int end) {
        Strings.checkArgsStartEnd(cs,start,end);
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,start,end-start);
        List<String> tokList = new ArrayList<String>();
        List<String> whiteList = new ArrayList<String>();
        tokenizer.tokenize(tokList,whiteList);
        String[] toks = toStringArray(tokList);
        String[] whites = toStringArray(whiteList);
        String[] tags = mDecoder.tag(tokList).tags().toArray(Strings.EMPTY_STRING_ARRAY);
        decodeNormalize(tags);
        return ChunkTagHandlerAdapter2.toChunkingBIO(toks,whites,tags);
    }
    public Chunking chunk(CharSequence cSeq) {
        char[] cs = Strings.toCharArray(cSeq);
        return chunk(cs,0,cs.length);
    }

    public Iterator<ScoredObject<Chunking>> nBest(char[] cs, int start, int end, int maxNBest) {
        Strings.checkArgsStartEnd(cs,start,end);
        if (maxNBest < 1) {
            String msg = "Maximum n-best value must be greater than zero."
                + " Found maxNBest=" + maxNBest;
            throw new IllegalArgumentException(msg);
        }
        String[][] toksWhites = getToksWhites(cs,start,end);
        Iterator<ScoredTagging<String>> it = mDecoder.tagNBest(Arrays.asList(toksWhites[0]),maxNBest);
        return new NBestIt(it,toksWhites);
    }

    public Iterator<ScoredObject<Chunking>> nBestConditional(char[] cs, int start, int end, int maxNBest) {
        Strings.checkArgsStartEnd(cs,start,end);
        if (maxNBest < 1) {
            String msg = "Maximum n-best value must be greater than zero."
                + " Found maxNBest=" + maxNBest;
            throw new IllegalArgumentException(msg);
        }
        String[][] toksWhites = getToksWhites(cs,start,end);
        Iterator<ScoredTagging<String>> it = mDecoder.tagNBestConditional(Arrays.asList(toksWhites[0]),maxNBest);
        return new NBestIt(it,toksWhites);
    }
    public Iterator<Chunk> nBestChunks(char[] cs, int start, int end, int maxNBest) {
        String[][] toksWhites = getToksWhites(cs,start,end);
        @SuppressWarnings("deprecation")
        TagLattice<String> lattice = mDecoder.tagMarginal(Arrays.asList(toksWhites[0]));
        return new NBestChunkIt(lattice,toksWhites[1],maxNBest);
    }

    String[][] getToksWhites(char[] cs, int start, int end) {
        Strings.checkArgsStartEnd(cs,start,end);
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,start,end-start);
        List<String> tokList = new ArrayList<String>();
        List<String> whiteList = new ArrayList<String>();
        tokenizer.tokenize(tokList,whiteList);
        String[] toks = toStringArray(tokList);
        String[] whites = toStringArray(whiteList);
        return new String[][] { toks, whites };
    }


    private static class NBestChunkIt extends Iterators.Buffered<Chunk> {
        final TagLattice<String> mLattice;
        final String[] mWhites;
        final int mMaxNBest;
        final int[] mTokenStartIndexes;
        final int[] mTokenEndIndexes;

        String[] mBeginTags;
        int[] mBeginTagIds;
        int[] mMidTagIds;
        int[] mEndTagIds;

        String[] mWholeTags;
        int[] mWholeTagIds;

        final BoundedPriorityQueue<Scored> mQueue;

        final int mNumToks;

        final double mTotal;

        int mCount = 0;

        NBestChunkIt(TagLattice<String> lattice, String[] whites, int maxNBest) {
            mTotal = com.assign.util.Math.naturalLogToBase2Log(lattice.logZ());
            mLattice = lattice;
            mWhites = whites;
            String[] toks = lattice.tokenList().toArray(Strings.EMPTY_STRING_ARRAY);
            mNumToks = toks.length;
            mTokenStartIndexes = new int[mNumToks];
            mTokenEndIndexes = new int[mNumToks];
            int pos = 0;
            for (int i = 0; i < mNumToks; ++i) {
                pos += whites[i].length();
                mTokenStartIndexes[i] = pos;
                pos += toks[i].length();
                mTokenEndIndexes[i] = pos;
            }

            mMaxNBest = maxNBest;
            mQueue = new BoundedPriorityQueue<Scored>(ScoredObject.comparator(),
                                                      maxNBest);
            initializeTags();
            initializeQueue();
        }

        void initializeTags() {
            SymbolTable tagTable = mLattice.tagSymbolTable();
            List<String> beginTagList = new ArrayList<String>();
            List<Integer> beginTagIdList = new ArrayList<Integer>();
            List<Integer> midTagIdList = new ArrayList<Integer>();
            List<Integer> endTagIdList = new ArrayList<Integer>();
            List<String> wholeTagList = new ArrayList<String>();
            List<Integer> wholeTagIdList = new ArrayList<Integer>();
            int numTags = tagTable.numSymbols();
            for (int i = 0; i < numTags; ++i) {
                String tag = tagTable.idToSymbol(i);
                if (tag.startsWith("B_")) {
                    String baseTag = tag.substring(2);
                    beginTagList.add(baseTag);
                    beginTagIdList.add(Integer.valueOf(i));

                    String midTag = "M_" + baseTag;
                    int midTagId = tagTable.symbolToID(midTag);
                    midTagIdList.add(Integer.valueOf(midTagId));

                    String endTag = "E_" + baseTag;
                    int endTagId = tagTable.symbolToID(endTag);
                    endTagIdList.add(Integer.valueOf(endTagId));
                }
                else if (tag.startsWith("W_")) {
                    String baseTag = tag.substring(2);
                    wholeTagList.add(baseTag);
                    wholeTagIdList.add(Integer.valueOf(i));
                }
            }
            mBeginTags = toStringArray(beginTagList);
            mBeginTagIds = toIntArray(beginTagIdList);
            mMidTagIds = toIntArray(midTagIdList);
            mEndTagIds = toIntArray(endTagIdList);

            mWholeTags = toStringArray(wholeTagList);
            mWholeTagIds = toIntArray(wholeTagIdList);
        }
        void initializeQueue() {
            int len = mWhites.length-1;
            for (int i = 0; i < len; ++i) {
                for (int j = 0; j < mBeginTagIds.length; ++j)
                    initializeBeginTag(i,j);
                for (int j = 0; j < mWholeTagIds.length; ++j)
                    initializeWholeTag(i,j);
            }
        }
        void initializeBeginTag(int tokPos, int j) {
            int startCharPos = mTokenStartIndexes[tokPos];

            String tag = mBeginTags[j];
            int beginTagId = mBeginTagIds[j];
            int midTagId = mMidTagIds[j];
            int endTagId = mEndTagIds[j];

            double forward = naturalLogToBase2Log(mLattice.logForward(tokPos,beginTagId));
            double backward = naturalLogToBase2Log(mLattice.logBackward(tokPos,beginTagId));

            ChunkItState state
                = new ChunkItState(startCharPos,tokPos,
                                   tag,beginTagId,midTagId,endTagId,
                                   forward,backward);
            mQueue.offer(state);
        }
        void initializeWholeTag(int tokPos, int j) {
            int start = mTokenStartIndexes[tokPos];
            int end = mTokenEndIndexes[tokPos];
            String tag = mWholeTags[j];
            double log2Score = naturalLogToBase2Log(mLattice.logProbability(tokPos,mWholeTagIds[j]));
            Chunk chunk = ChunkFactory.createChunk(start,end,tag,log2Score);
            mQueue.offer(chunk);
        }
        @Override
        public Chunk bufferNext() {
            if (mCount > mMaxNBest) return null;
            while (!mQueue.isEmpty()) {
                Object next = mQueue.poll();
                if (next instanceof Chunk) {
                    ++mCount;
                    Chunk result = (Chunk) next;
                    return ChunkFactory.createChunk(result.start(),
                                                    result.end(),
                                                    result.type(),
                                                    result.score()-mTotal);
                }
                ChunkItState state = (ChunkItState) next;
                addNextMidState(state);
                addNextEndState(state);
            }
            return null;
        }
        void addNextMidState(ChunkItState state) {
            int nextTokPos = state.mTokPos + 1;
            if (nextTokPos + 1 >= mNumToks)
                return; // don't add if can't extend
            int midTagId = state.mMidTagId;
            double transition
                = naturalLogToBase2Log(mLattice.logTransition(nextTokPos-1,
                                                              state.mCurrentTagId,
                                                              midTagId));
            double forward = state.mForward + transition;
            double backward = naturalLogToBase2Log(mLattice.logBackward(nextTokPos,midTagId));
            ChunkItState nextState
                = new ChunkItState(state.mStartCharPos,nextTokPos,
                                   state.mTag, midTagId,
                                   state.mMidTagId, state.mEndTagId,
                                   forward,backward);
            mQueue.offer(nextState);
        }
        void addNextEndState(ChunkItState state) {
            int nextTokPos = state.mTokPos + 1;
            if (nextTokPos >= mNumToks) return;
            int endTagId = state.mEndTagId;
            double transition
                = naturalLogToBase2Log(mLattice.logTransition(nextTokPos-1,
                                                              state.mCurrentTagId,
                                                              endTagId));
            double forward = state.mForward + transition;
            double backward = naturalLogToBase2Log(mLattice.logBackward(nextTokPos,endTagId));
            double log2Prob = forward + backward; //  - mTotal;
            Chunk chunk
                = ChunkFactory.createChunk(state.mStartCharPos,
                                           mTokenEndIndexes[nextTokPos],
                                           state.mTag,
                                           log2Prob);
            mQueue.offer(chunk);
        }
    }

    private static class ChunkItState implements Scored {
        final int mStartCharPos;
        final int mTokPos;

        final String mTag;
        final double mForward;
        final double mBack;
        final double mScore;

        final int mCurrentTagId;
        final int mMidTagId;
        final int mEndTagId;
        ChunkItState(int startCharPos, int tokPos,
                     String tag, int currentTagId, int midTagId, int endTagId,
                     double forward, double back) {
            mStartCharPos = startCharPos;
            mTokPos = tokPos;

            mTag = tag;
            mCurrentTagId = currentTagId;
            mMidTagId = midTagId;
            mEndTagId = endTagId;

            mForward = forward;
            mBack = back;
            mScore = forward + back;
        }
        public double score() {
            return mScore;
        }
    }

    private static class NBestIt
        implements Iterator<ScoredObject<Chunking>> {

        final Iterator<ScoredTagging<String>> mIt;
        final String[] mWhites;
        final String[] mToks;
        NBestIt(Iterator<ScoredTagging<String>> it, String[][] toksWhites) {
            mIt = it;
            mToks = toksWhites[0];
            mWhites = toksWhites[1];
        }
        public boolean hasNext() {
            return mIt.hasNext();
        }
        public ScoredObject<Chunking> next() {
            ScoredTagging<String> so = mIt.next();
            double score = so.score();
            String[] tags = so.tags().toArray(Strings.EMPTY_STRING_ARRAY);
            decodeNormalize(tags);
            Chunking chunking
                = ChunkTagHandlerAdapter2.toChunkingBIO(mToks,mWhites,tags);
            return new ScoredObject<Chunking>(chunking,score);
        }
        public void remove() {
            mIt.remove();
        }
    }


    private static String[] toStringArray(Collection<String> c) {
        return c.<String>toArray(Strings.EMPTY_STRING_ARRAY);
    }

    private static int[] toIntArray(Collection<Integer> c) {
        int[] result = new int[c.size()];
        Iterator<Integer> it = c.iterator();
        for (int i = 0; it.hasNext(); ++i) {
            Integer nextVal = it.next();
            result[i] = nextVal.intValue();
        }
        return result;
    }


    static String baseTag(String tag) {
        if (ChunkTagHandlerAdapter2.isOutTag(tag)) return tag;
        return tag.substring(2);
    }



    static String[] trainNormalize(String[] tags) {
        if (tags.length == 0) return tags;
        String[] normalTags = new String[tags.length];
        for (int i = 0; i < normalTags.length; ++i) {
            String prevTag = (i-1 >= 0) ? tags[i-1] : "W_BOS"; // "W_BOS";
            String nextTag = (i+1 < tags.length) ? tags[i+1] : "W_BOS"; // "W_EOS";
            normalTags[i] = trainNormalize(prevTag,tags[i],nextTag);
        }
        return normalTags;
    }


    private static void decodeNormalize(String[] tags) {
        for (int i = 0; i < tags.length; ++i)
            tags[i] = decodeNormalize(tags[i]);
    }


    static String trainNormalize(String prevTag,
                                 String tag,
                                 String nextTag) {
        if (ChunkTagHandlerAdapter2.isOutTag(tag)) {
            if (ChunkTagHandlerAdapter2.isOutTag(prevTag)) {
                if (ChunkTagHandlerAdapter2.isOutTag(nextTag)) {
                    return "MM_O";
                } else {
                    return "EE_O_" + baseTag(nextTag);
                }
            } else if (ChunkTagHandlerAdapter2.isOutTag(nextTag)) {
                return "BB_O_" + baseTag(prevTag);
            } else {
                return "WW_O_" + baseTag(nextTag); // WW_O
            }
        }
        if (ChunkTagHandlerAdapter2.isBeginTag(tag)) {
            if (ChunkTagHandlerAdapter2.isInTag(nextTag))
                return "B_" + baseTag(tag);
            else
                return "W_" + baseTag(tag);
        }
        if (ChunkTagHandlerAdapter2.isInTag(tag)) {
            if (ChunkTagHandlerAdapter2.isInTag(nextTag))
                return "M_" + baseTag(tag);
            else
                return "E_" + baseTag(tag);
        }
        String msg = "Unknown tag triple."
            + " prevTag=" + prevTag
            + " tag=" + tag
            + " nextTag=" + nextTag;
        throw new IllegalArgumentException(msg);
    }

    private static String decodeNormalize(String tag) {
        if (tag.startsWith("B_") || tag.startsWith("W_")) {
            String baseTag = tag.substring(2);
            return ChunkTagHandlerAdapter2.toBeginTag(baseTag);
        }
        if (tag.startsWith("M_") || tag.startsWith("E_")) {
            String baseTag = tag.substring(2);
            return ChunkTagHandlerAdapter2.toInTag(baseTag);
        }
        return ChunkTagHandlerAdapter2.OUT_TAG;
    }

}
