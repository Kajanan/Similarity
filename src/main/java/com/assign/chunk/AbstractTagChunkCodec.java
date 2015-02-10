
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

abstract class AbstractTagChunkCodec implements TagChunkCodec {

    final TokenizerFactory mTokenizerFactory;
    final boolean mEnforceConsistency;
    
    public AbstractTagChunkCodec() {
        this(null,false);
    }

    public AbstractTagChunkCodec(TokenizerFactory tokenizerFactory,
                                 boolean enforceConsistency) {
        mTokenizerFactory = tokenizerFactory;
        mEnforceConsistency = enforceConsistency;
    }

    public boolean enforceConsistency() {
        return mEnforceConsistency;
    }

    
    public TokenizerFactory tokenizerFactory() {
        return mTokenizerFactory;
    }

    
    public boolean isEncodable(Chunking chunking) {
        return isEncodable(chunking,null);
    }

   
    public boolean isDecodable(StringTagging tagging) {
        return isDecodable(tagging,null);
    }

    boolean isEncodable(Chunking chunking, StringBuilder sb) {
        if (mTokenizerFactory == null) {
            String msg = "Tokenizer factory must be non-null to support encodability test.";
            throw new UnsupportedOperationException(msg);
        }
        Set<Chunk> chunkSet = chunking.chunkSet();
        if (chunkSet.size() == 0) return true;
        Chunk[] chunks = chunkSet.toArray(new Chunk[chunkSet.size()]);
        Arrays.sort(chunks,Chunk.TEXT_ORDER_COMPARATOR);
        int lastEnd = chunks[0].end();
        for (int i = 1; i < chunks.length; ++i) {
            if (chunks[i].start() < lastEnd) {
                if (sb != null) {
                    sb.append("Chunks must not overlap."
                              + " chunk=" + chunks[i-1]
                              + " chunk=" + chunks[i]);
                }
                return false;
            }
            lastEnd = chunks[i].end();
        }
        char[] cs = Strings.toCharArray(chunking.charSequence());
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,0,cs.length);
        int chunkPos = 0;
        boolean chunkStarted = false;
        String token;
        while (chunkPos < chunks.length && (token = tokenizer.nextToken()) != null) {
            int tokenStart = tokenizer.lastTokenStartPosition();
            if (tokenStart == chunks[chunkPos].start())
                chunkStarted = true;
            int tokenEnd = tokenizer.lastTokenEndPosition();
            if (tokenEnd == chunks[chunkPos].end()) {
                if (!chunkStarted) {
                    if (sb != null)
                        sb.append("Chunks must start on token boundaries."
                                  + " Illegal chunk=" + chunks[chunkPos]);
                    return false;
                }
                ++chunkPos;
                chunkStarted = false;
            }
        }
        if (chunkPos < chunks.length) {
            if (sb != null)
                sb.append("Chunk beyond last token."
                          + " chunk=" + chunks[chunkPos]);
            return false;
        }
        return true;
    }

    boolean isDecodable(StringTagging tagging, StringBuilder sb) {
        if (mTokenizerFactory == null) {
            String msg = "Tokenizer factory must be non-null to support decodability test.";
            throw new UnsupportedOperationException(msg);
        }
        if (!legalTags(tagging.tags().toArray(Strings.EMPTY_STRING_ARRAY))) {
            sb.append("Illegal tags=" + tagging.tags());
            return false;
        }
        char[] cs = Strings.toCharArray(tagging.characters());
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,0,cs.length);
        for (int n = 0; n < tagging.size(); ++n) {
            String nextToken = tokenizer.nextToken();
            if (nextToken == null) {
                if (sb != null)
                    sb.append("More tags than tokens.");
                return false;
            }
            if (tagging.tokenStart(n) != tokenizer.lastTokenStartPosition()) {
                if (sb != null)
                    sb.append("Tokens must start/end in tagging to match tokenizer."
                              + " Found token " + n +
                              " from tokenizer=" + nextToken
                              + " tokenizer.lastTokenStartPosition()="
                              + tokenizer.lastTokenStartPosition()
                              + " tagging.tokenStart(" + n + ")="
                              + tagging.tokenStart(n));
                return false;
            }
            if (tagging.tokenEnd(n) != tokenizer.lastTokenEndPosition()) {
                if (sb != null)
                    sb.append("Tokens must start/end in tagging to match tokenizer."
                              + " Found token " + n
                              + " from tokenizer=" + nextToken
                              + " tokenizer.lastTokenEndPosition()="
                              + tokenizer.lastTokenEndPosition()
                              + " tagging.tokenEnd(" + n + ")="
                              + tagging.tokenEnd(n));
                return false;
            }
        }
        String excessToken = tokenizer.nextToken();
        if (excessToken != null) {
            if (sb != null)
                sb.append("Extra token from tokenizer beyond tagging."
                          + " token=" + excessToken
                          + " startPosition=" + tokenizer.lastTokenStartPosition()
                          + " endPosition=" + tokenizer.lastTokenEndPosition());
        }
        return true;
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


}