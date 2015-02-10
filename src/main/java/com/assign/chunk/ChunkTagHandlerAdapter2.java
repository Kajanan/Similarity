
package com.assign.chunk;

import com.assign.corpus.ObjectHandler;

import com.assign.chunk.ChunkFactory;
import com.assign.chunk.Chunking;
import com.assign.chunk.ChunkingImpl;

import java.util.Arrays;

/**
 * CUT AND PASTE from corpus.
 * 
 * @author  Bob Carpenter
 * @version 4.0.0
 * @since   LingPipe3.9.1
 */
class ChunkTagHandlerAdapter2 {

    private ObjectHandler<Chunking> mChunkHandler;

    public ChunkTagHandlerAdapter2() {
       
    }
    
    public ChunkTagHandlerAdapter2(ObjectHandler<Chunking> handler) {
        mChunkHandler = handler;
    }

    public void setChunkHandler(ObjectHandler<Chunking> handler) {
        mChunkHandler = handler;
    }

    public void handle(String[] tokens, String[] whitespaces, String[] tags) {
        if (tokens.length != tags.length) {
            String msg = "Tags and tokens must be same length."
                + " Found tokens.length=" + tokens.length
                + " tags.length=" + tags.length;
            throw new IllegalArgumentException(msg);
        }
        if ((whitespaces != null) 
            && (whitespaces.length != 1 + tokens.length) ) {
            String msg = "Whitespaces must be one longer than tokens."
                + " Found tokens.length=" + tokens.length
                + " whitespaces.length=" + whitespaces.length;
            throw new IllegalArgumentException(msg);
        }
        Chunking chunking = toChunkingBIO(tokens,whitespaces,tags);
        mChunkHandler.handle(chunking);
    }

   
    public static String OUT_TAG = "O";

    public static String BEGIN_TAG_PREFIX = "B-";

    public static String IN_TAG_PREFIX = "I-";

    public static String toBaseTag(String tag) {
        if (isBeginTag(tag) || isInTag(tag)) return tag.substring(2);
        String msg = "Tag is neither begin not continuation tag."
            + " Tag=" + tag;
        throw new IllegalArgumentException(msg);
    }

    public static boolean isBeginTag(String tag) {
        return tag.startsWith(BEGIN_TAG_PREFIX);
    }

    public static boolean isOutTag(String tag) {
        return tag.equals(OUT_TAG);
    }

    public static boolean isInTag(String tag) {
        return tag.startsWith(IN_TAG_PREFIX);
    }

    public static String toInTag(String type) {
        return IN_TAG_PREFIX + type;
    }

    public static String toBeginTag(String type) {
        return BEGIN_TAG_PREFIX + type;
    }

    
    public static Chunking toChunkingBIO(String[] tokens, 
                                         String[] whitespaces,
                                         String[] tags) {
        StringBuilder sb = new StringBuilder();
        if (whitespaces == null) {
            whitespaces = new String[tokens.length+1];
            Arrays.fill(whitespaces," ");
            whitespaces[0] = "";
            whitespaces[whitespaces.length-1] = "";
        }
        for (int i = 0; i < tokens.length; ++i) {
            sb.append(whitespaces[i]);
            sb.append(tokens[i]);
        }
        sb.append(whitespaces[whitespaces.length-1]);
        ChunkingImpl chunking = new ChunkingImpl(sb);
    
        int pos = 0;
        for (int i = 0; i < tokens.length; ) {
            pos += whitespaces[i].length();
            if (!isBeginTag(tags[i])) {
                pos += tokens[i].length();
                ++i;
                continue;
            }
            int start = pos;
            String type = toBaseTag(tags[i]);
            while (true) {
                pos += tokens[i].length();
                ++i;
                if (i >= tokens.length 
                    || !isInTag(tags[i])) {
                    chunking.add(ChunkFactory.createChunk(start,pos,type));
                    break;
                }
                pos += whitespaces[i].length();
            }
        }        
        return chunking;
    }




}
