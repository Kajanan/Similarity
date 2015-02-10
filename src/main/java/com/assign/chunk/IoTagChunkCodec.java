

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

public class IoTagChunkCodec
    extends AbstractTagChunkCodec
    implements Serializable {

    static final long serialVersionUID = 3871326314223465927L;

    final BioTagChunkCodec mBioCodec;

 
    public IoTagChunkCodec() {
        this(null,false);
    }

    public IoTagChunkCodec(TokenizerFactory tokenizerFactory,
                            boolean enforceConsistency) {
        super(tokenizerFactory,enforceConsistency);
        mBioCodec = new BioTagChunkCodec(tokenizerFactory,enforceConsistency);
    }

    @Override
    boolean isEncodable(Chunking chunking, StringBuilder sb) {
        if (!mBioCodec.isEncodable(chunking,sb))
            return false;
        Tagging<String> tagging = mBioCodec.toTagging(chunking);
        String lastTag = BioTagChunkCodec.OUT_TAG;
        for (String tag : tagging.tags()) {
            if (startSameType(lastTag,tag)) {
                if (sb != null)
                    sb.append("Two consectuive chunks of type " 
                              + tag.substring(BioTagChunkCodec.PREFIX_LENGTH));
                return false;
            }
            lastTag = tag;
        }
        return true;
    }

    boolean startSameType(String lastTag, String tag) {
        return tag.startsWith(BioTagChunkCodec.BEGIN_TAG_PREFIX)
            && !BioTagChunkCodec.OUT_TAG.equals(lastTag)
            && lastTag.substring(BioTagChunkCodec.PREFIX_LENGTH).equals(tag.substring(BioTagChunkCodec.PREFIX_LENGTH));
    }

    public Set<String> tagSet(Set<String> chunkTypes) {
        Set<String> tagSet = new HashSet<String>();
        tagSet.addAll(chunkTypes);
        tagSet.add(BioTagChunkCodec.OUT_TAG);
        return tagSet;
    }

    public boolean legalTagSubSequence(String... tags) {
        return true;
    }

    public boolean legalTags(String... tags) {
        return true;
    }

    public Chunking toChunking(StringTagging tagging) {
        enforceConsistency(tagging);
        ChunkingImpl chunking = new ChunkingImpl(tagging.characters());
        for (int n = 0; n < tagging.size(); ++n) {
            String tag = tagging.tag(n);
            if (BioTagChunkCodec.OUT_TAG.equals(tag)) continue;
            String type = tag;
            int start = tagging.tokenStart(n);
            while ((n + 1) < tagging.size() && tagging.tag(n+1).equals(type))
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
        mBioCodec.toTagging(chunking,tokenList,tagList,
                            tokenStartList,tokenEndList);
        transformTags(tagList);
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
        mBioCodec.toTagging(chunking,tokens,tags,null,null);
        transformTags(tags);
        return new Tagging<String>(tokens,tags);
    }

    public Iterator<Chunk> nBestChunks(TagLattice<String> lattice,
                                       int[] tokenStarts,
                                       int[] tokenEnds,
                                       int maxResults) {
        throw new UnsupportedOperationException("no n-best chunks yet for IO encodings");
    }

    public String toString() {
        return "IoTagChunkCodec";
    }

    Object writeReplace() {
        return new Serializer(this);
    }


    static void transformTags(List<String> tagList) {
        for (int i = 0; i < tagList.size(); ++i) {
            String tag = tagList.get(i);
            if (BioTagChunkCodec.OUT_TAG.equals(tag))
                continue;
            String transformedTag = tag.substring(BioTagChunkCodec.PREFIX_LENGTH);
            tagList.set(i,transformedTag);
        }
    }

    static class Serializer extends AbstractExternalizable {
        static final long serialVersionUID = -3559983129637286794L;
        private final IoTagChunkCodec mCodec;
        public Serializer() {
            this(null);
        }
        public Serializer(IoTagChunkCodec codec) {
            mCodec = codec;
        }
        public void writeExternal(ObjectOutput out)
            throws IOException {

            out.writeBoolean(mCodec.mEnforceConsistency);
            out.writeObject(mCodec.mTokenizerFactory != null
                            ? mCodec.mTokenizerFactory
                            : Boolean.FALSE); // dummy object
        }
        public Object read(ObjectInput in)
            throws IOException, ClassNotFoundException {

            boolean enforceConsistency = in.readBoolean();
            Object tfObj = in.readObject();
            TokenizerFactory tf
                = (tfObj instanceof TokenizerFactory)
                ? (TokenizerFactory) tfObj
                : null;
            return new IoTagChunkCodec(tf,enforceConsistency);
        }
    }

}