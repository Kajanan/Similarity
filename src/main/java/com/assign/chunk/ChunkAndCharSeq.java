
package com.assign.chunk;

import com.assign.util.Scored;
import com.assign.util.Strings;


public class ChunkAndCharSeq implements Scored {

    private final Chunk mChunk;
    private final CharSequence mSeq;
    private final int mHashCode;

    public ChunkAndCharSeq(Chunk chunk, CharSequence cSeq) {
	if (chunk.end() > cSeq.length()) {
	    String msg = "Character sequence not long enough for chunk."
		+ " Chunk end=" + chunk.end()
		+ " Character sequence length=" + cSeq.length();
	    throw new IllegalArgumentException(msg);
	}
	mChunk = chunk;
	mSeq = cSeq;
	mHashCode = chunk.hashCode() + 31*Strings.hashCode(cSeq);
    }

    @Override
    public int hashCode() {
	return mHashCode;
    }

    @Override
    public boolean equals(Object that) {
	if (!(that instanceof ChunkAndCharSeq)) return false;
	ChunkAndCharSeq thatChunk = (ChunkAndCharSeq) that;
	if (thatChunk.hashCode() != hashCode()) return false; // cached
	return mChunk.equals(thatChunk.mChunk)
	    && mSeq.equals(thatChunk.mSeq);
    }

    public String span() {
	return mSeq.subSequence(mChunk.start(),mChunk.end()).toString();
    }

    public CharSequence spanStartContext(int contextLength) {
	if (contextLength < 1) {
	    String msg = "Context length must be greater than 0.";
	    throw new IllegalArgumentException(msg);
	}
	int start = mChunk.start() - contextLength;
	if (start < 0) start = 0;
	int end = mChunk.start() + contextLength;
	if (end > mSeq.length()) end = mSeq.length();
	int len = end - start;
	if (len < contextLength*2) {
	    StringBuilder padded = new StringBuilder();
	    for (int i = contextLength*2; i > len; i--) {
		padded.append(" ");
	    }
	    padded.append(mSeq.subSequence(start,end).toString());
	    return padded.subSequence(0,padded.length());
	}
	return mSeq.subSequence(start,end).toString();
    }

   
    public CharSequence spanEndContext(int contextLength) {
	if (contextLength < 1) {
	    String msg = "Context length must be greater than 0.";
	    throw new IllegalArgumentException(msg);
	}
	int start = mChunk.end() - contextLength;
	if (start < 0) start = 0;
	int end = mChunk.end() + contextLength;
	if (end > mSeq.length()) end = mSeq.length();
	int len = end - start;
	if (len < contextLength*2) {
	    StringBuilder padded = new StringBuilder();
	    padded.append(mSeq.subSequence(start,end).toString());
	    for (int i = contextLength*2; i > len; i--) {
		padded.append(" ");
	    }
	    return padded.subSequence(0,padded.length());
	}
	return mSeq.subSequence(start,end).toString();
    }

    
    public String charSequence() {
	return mSeq.toString();
    }

   
    public Chunk chunk() {
	return mChunk;
    }

    
    public double score() {
	return chunk().score();
    }

    
    @Override
    public String toString() {
	return chunk().start() + "-" + chunk().end() + "/"
	    + span() + ":" + chunk().type();
    }

}


