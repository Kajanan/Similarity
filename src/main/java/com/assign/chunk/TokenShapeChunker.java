package com.assign.chunk;

import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;

import com.assign.util.Strings;

import java.util.ArrayList;
import java.util.List;


public class TokenShapeChunker implements Chunker {

    private final TokenizerFactory mTokenizerFactory;
    private final TokenShapeDecoder mDecoder;

    TokenShapeChunker(TokenizerFactory tf, TokenShapeDecoder decoder) {
	mTokenizerFactory = tf;
	mDecoder = decoder;
    }

    public Chunking chunk(CharSequence cSeq) {
	char[] cs = Strings.toCharArray(cSeq);
	return chunk(cs,0,cs.length);
    }


    public Chunking chunk(char[] cs, int start, int end) {
	List<String> tokenList = new ArrayList<String>();
	List<String> whiteList = new ArrayList<String>();

	Tokenizer tokenizer 
	    = mTokenizerFactory.tokenizer(cs,start,end-start);
	tokenizer.tokenize(tokenList,whiteList);

	ChunkingImpl chunking = new ChunkingImpl(cs,start,end);

	if (tokenList.size() == 0) return chunking;

	String[] tokens = tokenList.toArray(Strings.EMPTY_STRING_ARRAY);
	String[] whites = whiteList.toArray(Strings.EMPTY_STRING_ARRAY);

	int[] tokenStarts = new int[tokens.length];
	int[] tokenEnds = new int[tokens.length];
    
	int pos = whites[0].length();
	for (int i = 0; i < tokens.length; ++i) {
	    tokenStarts[i] = pos;
	    pos += tokens[i].length();
	    tokenEnds[i] = pos;
	    pos += whites[i+1].length();
	}

        String[] tags = mDecoder.decodeTags(tokens);
	if (tags.length < 1) return chunking;

	int neStartIdx = -1;
	int neEndIdx = -1;
	String neTag = Tags.OUT_TAG;

	for (int i = 0; i < tags.length; ++i) {
	    if (!tags[i].equals(neTag)) {
		if (!Tags.isOutTag(neTag)) {
		    Chunk chunk
			= ChunkFactory.createChunk(neStartIdx,
						   neEndIdx,
						   Tags.baseTag(neTag));
		    chunking.add(chunk);
		}
		neTag = Tags.toInnerTag(tags[i]);
		neStartIdx = tokenStarts[i];
	    }
	    neEndIdx = tokenEnds[i];
	}
	// check final tag
	if (!Tags.isOutTag(neTag)) {
	    Chunk chunk
		= ChunkFactory.createChunk(neStartIdx,neEndIdx,
					   Tags.baseTag(neTag));
	    chunking.add(chunk);
	}
	return chunking;
    }


    public void setLog2Beam(double beamWidth) {
	if (beamWidth <= 0.0 || Double.isNaN(beamWidth)) {
	    String msg = "Require beam width to be positive."
		+ " Found beamWidth=" + beamWidth;
	    throw new IllegalArgumentException(msg);
	}
	mDecoder.setLog2Beam(beamWidth);
    }
}


