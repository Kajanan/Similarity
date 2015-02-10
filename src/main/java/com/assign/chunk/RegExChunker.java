

package com.assign.chunk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.assign.util.AbstractExternalizable;
import com.assign.util.Compilable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;


public class RegExChunker implements Chunker, Compilable, Serializable {

    static final long serialVersionUID = -8997320544817071938L;

    private final Pattern mPattern;
    private final String mChunkType;
    private final double mChunkScore;

    public RegExChunker(String regex, String chunkType, double chunkScore) {
	this(Pattern.compile(regex),chunkType,chunkScore);
    }

    public RegExChunker(Pattern pattern, String chunkType, double chunkScore) {
	mPattern = pattern;
	mChunkType = chunkType;
	mChunkScore = chunkScore;
    }

    public Chunking chunk(CharSequence cSeq) {
	ChunkingImpl result = new ChunkingImpl(cSeq);
	Matcher matcher = mPattern.matcher(cSeq);
	while (matcher.find()) {
	    int start = matcher.start();
	    int end = matcher.end();
	    Chunk chunk 
		= ChunkFactory.createChunk(start,end,mChunkType,mChunkScore);
	    result.add(chunk);
	}
	return result;
    }

    public void compileTo(ObjectOutput out) throws IOException {
        out.writeObject(new Externalizer(this));
    }

    private Object writeReplace() {
        return new Externalizer(this);
    }
    
    public Chunking chunk(char[] cs, int start, int end) {
	return chunk(new String(cs,start,end-start));
    }

    static class Externalizer extends AbstractExternalizable {
        static final long serialVersionUID = -3419191413174871277L;
        private final RegExChunker mChunker;
        public Externalizer() {
            this(null);
        }
        public Externalizer(RegExChunker chunker) {
            mChunker = chunker;
        }
        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(mChunker.mPattern.pattern());
            out.writeUTF(mChunker.mChunkType);
            out.writeDouble(mChunker.mChunkScore);
        }
        @Override
        public Object read(ObjectInput in) throws IOException, ClassNotFoundException {
            String pattern = in.readUTF();
            String chunkType = in.readUTF();
            double score = in.readDouble();
            return new RegExChunker(pattern,chunkType,score);
        }
    }

}
