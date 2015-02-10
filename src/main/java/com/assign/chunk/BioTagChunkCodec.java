package com.assign.chunk;

import com.assign.tag.StringTagging;
import com.assign.tag.Tagging;
import com.assign.tag.TagLattice;

import com.assign.symbol.SymbolTable;

import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;

import com.assign.util.AbstractExternalizable;
import com.assign.util.BoundedPriorityQueue;
import com.assign.util.Iterators;
import com.assign.util.Scored;
import com.assign.util.ScoredObject;
import com.assign.util.Strings;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class BioTagChunkCodec
    extends AbstractTagChunkCodec
    implements Serializable {

    static final long serialVersionUID = -4597052413756614276L;

    private final String mBeginTagPrefix;
    private final String mInTagPrefix;
    private final String mOutTag;

   
    public BioTagChunkCodec() {
        this(null,false);
    }


    public BioTagChunkCodec(TokenizerFactory tokenizerFactory,
                            boolean enforceConsistency) {
        this(tokenizerFactory,enforceConsistency,
             BEGIN_TAG_PREFIX,IN_TAG_PREFIX,OUT_TAG);
    }

   
    public BioTagChunkCodec(TokenizerFactory tokenizerFactory,
                            boolean enforceConsistency,
                            String beginTagPrefix,
                            String inTagPrefix,
                            String outTag) {
        super(tokenizerFactory,enforceConsistency);
        mOutTag = outTag;
        mBeginTagPrefix = beginTagPrefix;
        mInTagPrefix = inTagPrefix;
    }


   
    public boolean enforceConsistency() {
        return mEnforceConsistency;
    }

    
    public Set<String> tagSet(Set<String> chunkTypes) {
        Set<String> tagSet = new HashSet<String>();
        tagSet.add(mOutTag);
        for (String chunkType : chunkTypes) {
            tagSet.add(mBeginTagPrefix + chunkType);
            tagSet.add(mInTagPrefix + chunkType);
        }
        return tagSet;
    }

    public boolean legalTagSubSequence(String... tags) {
        if (tags.length == 0)
            return true;
        if (tags.length == 1)
            return legalTagSingle(tags[0]);
        for (int i = 1; i < tags.length; ++i)
            if (!legalTagPair(tags[i-1],tags[i]))
                return false;
        return true;
    }

    public boolean legalTags(String... tags) {
        return legalTagSubSequence(tags)
            && (tags.length == 0 || !tags[0].startsWith(mInTagPrefix));
    }

    public Chunking toChunking(StringTagging tagging) {
        enforceConsistency(tagging);
        ChunkingImpl chunking = new ChunkingImpl(tagging.characters());
        for (int n = 0; n < tagging.size(); ++n) {
            String tag = tagging.tag(n);
            if (mOutTag.equals(tag)) continue;
            if (!tag.startsWith(mBeginTagPrefix)) {
                if (n == 0) {
                    String msg = "First tag must be out or begin."
                        + " Found tagging.tag(0)=" + tagging.tag(0);
                    throw new IllegalArgumentException(msg);
                }
                String msg = "Illegal tag sequence."
                    + " tagging.tag(" + (n-1) + ")=" + tagging.tag(n-1)
                    + " tagging.tag(" + n + ")=" + tagging.tag(n);
                throw new IllegalArgumentException(msg);
            }
            String type = tag.substring(2);
            int start = tagging.tokenStart(n);
            String inTag = mInTagPrefix + type;
            while ((n+1) < tagging.size() && inTag.equals(tagging.tag(n+1)))
                ++n;
            int end = tagging.tokenEnd(n);
            Chunk chunk = ChunkFactory.createChunk(start,end,type);
            chunking.add(chunk);
        }
        return chunking;
    }

    
    public StringTagging toStringTagging(Chunking chunking) {
        if (mTokenizerFactory == null) {
            String msg = "Tokenizer factory must be non-null to convert chunking to tagging.";
            throw new UnsupportedOperationException(msg);
        }
        enforceConsistency(chunking);
        List<String> tokenList = new ArrayList<String>();
        List<String> tagList = new ArrayList<String>();
        List<Integer> tokenStartList = new ArrayList<Integer>();
        List<Integer> tokenEndList = new ArrayList<Integer>();
        toTagging(chunking,tokenList,tagList,
                  tokenStartList,tokenEndList);
        StringTagging tagging = new StringTagging(tokenList,
                                                  tagList,
                                                  chunking.charSequence(),
                                                  tokenStartList,
                                                  tokenEndList);
        return tagging;
    }

   
    public Tagging<String> toTagging(Chunking chunking) {
        if (mTokenizerFactory == null) {
            String msg = "Tokenizer factory must be non-null to convert chunking to tagging.";
            throw new UnsupportedOperationException(msg);
        }
        enforceConsistency(chunking);
        List<String> tokens = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        toTagging(chunking,tokens,tags,null,null);
        return new Tagging<String>(tokens,tags);
    }

    public Iterator<Chunk> nBestChunks(TagLattice<String> lattice,
                                       int[] tokenStarts,
                                       int[] tokenEnds,
                                       int maxResults) {
        if (maxResults < 0) {
            String msg = "Require non-negative number of results.";
            throw new IllegalArgumentException(msg);
        }
        if (tokenStarts.length != lattice.numTokens()) {
            String msg = "Token starts must line up with num tokens."
                + " Found tokenStarts.length=" + tokenStarts.length
                + " lattice.numTokens()=" + lattice.numTokens();
            throw new IllegalArgumentException(msg);
        }
        if (tokenEnds.length != lattice.numTokens()) {
            String msg = "Token ends must line up with num tokens."
                + " Found tokenEnds.length=" + tokenEnds.length
                + " lattice.numTokens()=" + lattice.numTokens();
            throw new IllegalArgumentException(msg);
        }
        for (int i = 1; i < tokenStarts.length; ++i) {
            if (tokenStarts[i-1] > tokenStarts[i]) {
                String msg = "Token starts must be in order."
                    + " Found tokenStarts[" + (i-1) + "]=" + tokenStarts[i-1]
                    + " tokenStarts[" + i + "]=" + tokenStarts[i];
                throw new IllegalArgumentException(msg);
            }
            if (tokenEnds[i-1] > tokenEnds[i]) {
                String msg = "Token ends must be in order."
                    + " Found tokenEnds[" + (i-1) + "]=" + tokenEnds[i-1]
                    + " tokenEnds[" + i + "]=" + tokenEnds[i];
                throw new IllegalArgumentException(msg);
            }
        }
        if (lattice.numTags() == 0) {
            return Iterators.<Chunk>empty();
        }
        for (int i = 0; i < tokenStarts.length; ++i) {
            if (tokenStarts[i] > tokenEnds[i]) {
                String msg = "Token ends must not precede starts."
                    + " Found tokenStarts[" + i + "]=" + tokenStarts[i]
                    + " tokenEnds[" + i + "]=" + tokenEnds[i];
                throw new IllegalArgumentException(msg);
            }
        }
        return new NBestIterator(lattice,tokenStarts,tokenEnds,maxResults,
                                 mBeginTagPrefix,mInTagPrefix,mOutTag);
    }

    /**
     * Return a string-based representation of this codec.
     *
     * @return A string-based representation of this codec.
     */
    public String toString() {
        return "BioTagChunkCodec";
    }

    static class NBestIterator extends Iterators.Buffered<Chunk> {
        private final String mBeginTagPrefix;
        private final String mInTagPrefix;
        private final String mOutTag;
        private final TagLattice<String> mLattice;
        private final int[] mTokenStarts;
        private final int[] mTokenEnds;
        private final BoundedPriorityQueue<Chunk> mChunkQueue;
        private final BoundedPriorityQueue<NBestState> mStateQueue;
        private final int mMaxResults;
        private final String[] mChunkTypes;
        private final int[] mBeginTagIds;
        private final int[] mInTagIds;
        private final int mOutTagId;
        private int mNumResults = 0;
        public NBestIterator(TagLattice<String> lattice,
                             int[] tokenStarts,
                             int[] tokenEnds,
                             int maxResults,
                             String beginTagPrefix,
                             String inTagPrefix,
                             String outTag) {
            mBeginTagPrefix = beginTagPrefix;
            mInTagPrefix = inTagPrefix;
            mOutTag = outTag;

            mLattice = lattice;
            mTokenStarts = tokenStarts;
            mTokenEnds = tokenEnds;
            mMaxResults = maxResults;
            Set<String> chunkTypeSet = new HashSet<String>();
            SymbolTable tagSymbolTable = lattice.tagSymbolTable();
            for (int k = 0; k < lattice.numTags(); ++k)
                if (lattice.tag(k).startsWith(mInTagPrefix))
                    chunkTypeSet.add(lattice.tag(k).substring(mInTagPrefix.length()));
            mChunkTypes = chunkTypeSet.toArray(Strings.EMPTY_STRING_ARRAY);
            mBeginTagIds = new int[mChunkTypes.length];
            mInTagIds = new int[mChunkTypes.length];
            for (int j = 0; j < mChunkTypes.length; ++j) {
                mBeginTagIds[j] = tagSymbolTable.symbolToID(mBeginTagPrefix + mChunkTypes[j]);
                mInTagIds[j] = tagSymbolTable.symbolToID(mInTagPrefix + mChunkTypes[j]);
            }
            mOutTagId = tagSymbolTable.symbolToID(mOutTag);

            mStateQueue = new BoundedPriorityQueue<NBestState>(ScoredObject.comparator(),
                                                               maxResults);
            mChunkQueue = new BoundedPriorityQueue<Chunk>(ScoredObject.comparator(),
                                                          maxResults);

            double[] nonContBuf = new double[lattice.numTags()-1];
            for (int j = 0; j < mChunkTypes.length; ++j) {
                int lastN = lattice.numTokens() - 1;
                if (lastN < 0) continue;
                String chunkType = mChunkTypes[j];
                int inTagId = mInTagIds[j];
                int beginTagId = mBeginTagIds[j];
                // System.out.println("beginTagId=" + beginTagId + " lastN=" + lastN);
                mChunkQueue.offer(ChunkFactory.createChunk(mTokenStarts[lastN],
                                                           mTokenEnds[lastN],
                                                           chunkType,
                                                           lattice.logProbability(lastN,beginTagId)));
                if (lastN > 0) {
                    mStateQueue.offer(new NBestState(lattice.logBackward(lastN,inTagId),
                                                     lastN,lastN,j));
                }
                for (int n = 0; n < lastN; ++n) {
                    double nonCont = nonContLogSumExp(j,n,beginTagId,lattice,nonContBuf);
                    mChunkQueue.offer(ChunkFactory.createChunk(mTokenStarts[n],
                                                               mTokenEnds[n],
                                                               chunkType,
                                                               nonCont + lattice.logForward(n,beginTagId)
                                                               - lattice.logZ()));
                }
                for (int n = 1; n < lastN; ++n) {
                    double nonCont = nonContLogSumExp(j,n,inTagId,lattice,nonContBuf);
                    mStateQueue.offer(new NBestState(nonCont,n,n,j));
                }
            }
        }
        double nonContLogSumExp(int j, int n, int fromTagId, TagLattice<String> lattice, double[] nonContBuf) {
            nonContBuf[0] = lattice.logBackward(n+1,mOutTagId)
                + lattice.logTransition(n,fromTagId,mOutTagId);
            int bufPos = 1;
            for (int j2 = 0; j2 < mBeginTagIds.length; ++j2) {
                nonContBuf[bufPos++] = lattice.logBackward(n+1,mBeginTagIds[j2])
                    + lattice.logTransition(n,fromTagId,mBeginTagIds[j2]);
                if (j == j2) continue;
                nonContBuf[bufPos++] = lattice.logBackward(n+1,mInTagIds[j2])
                    + lattice.logTransition(n,fromTagId,mInTagIds[j2]);
            }
            double result = com.assign.util.Math.logSumOfExponentials(nonContBuf);
            // System.out.print("nonCont j=" + j + " n=" + n + " fromId=" + fromTagId + " result=" + result + "  ");
            // for (int i = 0; i < nonContBuf.length; ++i) System.out.print(nonContBuf[i] + ", ");
            // System.out.println();
            return result;
        }

        public Chunk bufferNext() {
            if (mNumResults >= mMaxResults)
                return null;
            search();
            Chunk chunk = mChunkQueue.poll();
            if (chunk == null)
                return null;
            ++mNumResults;
            return chunk;
        }
        void search() {
            while ((!mStateQueue.isEmpty())
                   && (mChunkQueue.isEmpty()
                       || mChunkQueue.peek().score() < mStateQueue.peek().score())) {
                NBestState state = mStateQueue.poll();
                extend(state);
            }
        }
        void extend(NBestState state) {
            // System.out.println("\nextend " + state + "\n");
            int beginTagId = mBeginTagIds[state.mChunkId];
            int inTagId = mInTagIds[state.mChunkId];
            mChunkQueue.offer(ChunkFactory.createChunk(mTokenStarts[state.mPos-1],
                                                       mTokenEnds[state.mEndPos],
                                                       mChunkTypes[state.mChunkId],
                                                       state.score()
                                                       + mLattice.logForward(state.mPos-1,beginTagId)
                                                       + mLattice.logTransition(state.mPos-1,beginTagId,inTagId)
                                                       - mLattice.logZ()));
            if (state.mPos > 1)
                mStateQueue.offer(new NBestState(state.score()
                                                 + mLattice.logTransition(state.mPos-1,inTagId,inTagId),
                                                 state.mPos - 1,
                                                 state.mEndPos,
                                                 state.mChunkId));
        }
        static class NBestState implements Scored {
            private final double mScore;
            private final int mPos;
            private final int mEndPos;
            private int mChunkId;
            public NBestState(double score, int pos, int endPos,
                              int chunkId) {
                mScore = score;
                mPos = pos;
                mEndPos = endPos;
                mChunkId = chunkId;
            }
            public double score() {
                return mScore;
            }
            @Override
            public String toString() {
                return "score=" + mScore + " pos=" + mPos + " end=" + mEndPos + " id=" + mChunkId;
            }
        }
    }



    void enforceConsistency(StringTagging tagging) {
        if (!mEnforceConsistency) return;
        StringBuilder sb = new StringBuilder();
        if (isDecodable(tagging,sb)) return;
        throw new IllegalArgumentException(sb.toString());
    }

    void enforceConsistency(Chunking chunking) {
        if (!mEnforceConsistency) return;
        StringBuilder sb = new StringBuilder();
        if (isEncodable(chunking,sb)) return;
        throw new IllegalArgumentException(sb.toString());
    }

    boolean legalTagSingle(String tag) {
        return mOutTag.equals(tag)
            || tag.startsWith(mBeginTagPrefix)
            || tag.startsWith(mInTagPrefix);
    }
    boolean legalTagPair(String tag1, String tag2) {
        // B_X, I_X -> I_X, B_Y, O
        // O -> B_Y, O
        if (!legalTagSingle(tag1)) 
            return false;
        if (!legalTagSingle(tag2)) 
            return false;
        if (tag2.startsWith(mInTagPrefix))
            return tag1.endsWith(tag2.substring(mInTagPrefix.length()));
        return true;
    }

    void toTagging(Chunking chunking,
                   List<String> tokenList,
                   List<String> tagList,
                   List<Integer> tokenStartList,
                   List<Integer> tokenEndList) {
        char[] cs = Strings.toCharArray(chunking.charSequence());
        Set<Chunk> chunkSet = chunking.chunkSet();
        Chunk[] chunks = chunkSet.toArray(new Chunk[chunkSet.size()]);
        Arrays.sort(chunks,Chunk.TEXT_ORDER_COMPARATOR);
        int pos = 0;
        for (Chunk chunk : chunks) {
            String type = chunk.type();
            int start = chunk.start();
            int end = chunk.end();
            outBioTag(cs,pos,start,tokenList,tagList,tokenStartList,tokenEndList);
            chunkBioTag(cs,type,start,end,tokenList,tagList,tokenStartList,tokenEndList);
            pos = end;
        }
        outBioTag(cs,pos,cs.length,tokenList,tagList,tokenStartList,tokenEndList);
    }

    void outBioTag(char[] cs, int start, int end,
                   List<String> tokenList, List<String> tagList,
                   List<Integer> tokenStartList, List<Integer> tokenEndList) {
        int length = end - start;
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,start,length);
        String token;
        while ((token = tokenizer.nextToken()) != null) {
            tokenList.add(token);
            addOffsets(tokenizer,start,tokenStartList,tokenEndList);
            tagList.add(mOutTag);
        }
    }

    void chunkBioTag(char[] cs, String type, int start, int end,
                     List<String> tokenList, List<String> tagList,
                     List<Integer> tokenStartList, List<Integer> tokenEndList) {
        int length = end - start;
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,start,length);
        String firstToken = tokenizer.nextToken();
        if (firstToken == null) {
            String msg = "Chunks must contain at least one token."
                + " Found chunk with yield=|" + new String(cs,start,length) + "|";
            throw new IllegalArgumentException(msg);
        }
        tokenList.add(firstToken);
        addOffsets(tokenizer,start,tokenStartList,tokenEndList);
        String beginTag = mBeginTagPrefix + type;
        tagList.add(beginTag);
        String inTag = mInTagPrefix + type;
        String token;
        while ((token = tokenizer.nextToken()) != null) {
            tokenList.add(token);
            addOffsets(tokenizer,start,tokenStartList,tokenEndList);
            tagList.add(inTag);
        }
    }

    void addOffsets(Tokenizer tokenizer,
                    int offset,
                    List<Integer> tokenStartList, List<Integer> tokenEndList) {
        if (tokenStartList == null) return;
        int start = tokenizer.lastTokenStartPosition() + offset;
        int end = tokenizer.lastTokenEndPosition() + offset;
        tokenStartList.add(start);
        tokenEndList.add(end);
    }

    Object writeReplace() {
        return new Serializer(this);
    }



    static class Serializer extends AbstractExternalizable {
        static final long serialVersionUID = -2473387657606045149L;
        private final BioTagChunkCodec mCodec;
        public Serializer() {
            this(null);
        }
        public Serializer(BioTagChunkCodec codec) {
            mCodec = codec;
        }
        public void writeExternal(ObjectOutput out)
            throws IOException {

            out.writeBoolean(mCodec.mEnforceConsistency);
            out.writeObject(Boolean.TRUE); // signals 3.9.2 encoding for bw compatibility
            out.writeObject(mCodec.mTokenizerFactory != null
                            ? mCodec.mTokenizerFactory
                            : Boolean.FALSE); // false just a dummy object
            out.writeUTF(mCodec.mBeginTagPrefix);
            out.writeUTF(mCodec.mInTagPrefix);
            out.writeUTF(mCodec.mOutTag);
        }
        public Object read(ObjectInput in)
            throws IOException, ClassNotFoundException {

            boolean enforceConsistency = in.readBoolean();
            Object obj = in.readObject();
            if (Boolean.TRUE.equals(obj)) {
                
                Object obj2 = in.readObject();
                @SuppressWarnings("unchecked")
                TokenizerFactory tf
                    = Boolean.FALSE.equals(obj2)
                    ? null 
                    : (TokenizerFactory) obj2;
                String beginTagPrefix = in.readUTF();
                String inTagPrefix = in.readUTF();
                String outTag = in.readUTF();
                return new BioTagChunkCodec(tf,enforceConsistency,
                                            beginTagPrefix,inTagPrefix,outTag);
                
            }
           
            @SuppressWarnings("unchecked")
            TokenizerFactory tf
                = Boolean.FALSE.equals(obj)
                ? null 
                : (TokenizerFactory) obj;
            return new BioTagChunkCodec(tf,enforceConsistency);
        }
    }

    
    public static final String OUT_TAG = "O";

    
    public static final String BEGIN_TAG_PREFIX = "B_";
    
    public static final String IN_TAG_PREFIX = "I_";

    static final int PREFIX_LENGTH = 2;

}