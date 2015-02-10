
package com.assign.chunk;

import com.assign.corpus.ObjectHandler;

import com.assign.tag.StringTagging;
import com.assign.tag.Tagging;

public class TagChunkCodecAdapters {

    private TagChunkCodecAdapters() { 
        
    }

    public static ObjectHandler<Chunking> stringTaggingToChunking(TagChunkCodec codec,
                                                                  ObjectHandler<StringTagging> handler) {
        return new StringTaggingHandlerAdapter(codec,handler);
    }

    public static ObjectHandler<Chunking>
        taggingToChunking(TagChunkCodec codec,
                          ObjectHandler<Tagging<String>> handler) {
        return new TaggingHandlerAdapter(codec,handler);
    }


    public static ObjectHandler<StringTagging>
        chunkingToStringTagging(TagChunkCodec codec,
                                ObjectHandler<Chunking> handler) {
        return new ChunkingHandlerAdapter(codec,handler);
    }


    public static ObjectHandler<Tagging<String>>
        chunkingToTagging(TagChunkCodec codec,
                          ObjectHandler<Chunking> handler) {
        return new ChunkingHandlerAdapterPad(codec,handler);
    }


    static StringTagging pad(Tagging<String> tagging) {
        StringBuilder sb = new StringBuilder();
        int[] tokenStarts = new int[tagging.size()];
        int[] tokenEnds = new int[tagging.size()];
        for (int n = 0; n < tagging.size(); ++n) {
            if (n > 0) sb.append(' ');
            tokenStarts[n] = sb.length();
            sb.append(tagging.token(n));
            tokenEnds[n] = sb.length();
        }
        return new StringTagging(tagging.tokens(),
                                 tagging.tags(),
                                 sb,
                                 tokenStarts,
                                 tokenEnds);
    }
                                                                      
    static class StringTaggingHandlerAdapter implements ObjectHandler<Chunking> {
        private final TagChunkCodec mCodec;
        private final ObjectHandler<StringTagging> mHandler;
        public StringTaggingHandlerAdapter(TagChunkCodec codec,
                                           ObjectHandler<StringTagging> handler) {
            mCodec = codec;
            mHandler = handler;
        }
        public void handle(Chunking chunking) {
            StringTagging tagging = mCodec.toStringTagging(chunking);
            mHandler.handle(tagging);
        }
    }

    static class TaggingHandlerAdapter implements ObjectHandler<Chunking> {
        private final TagChunkCodec mCodec;
        private final ObjectHandler<Tagging<String>> mHandler;
        public TaggingHandlerAdapter(TagChunkCodec codec,
                                     ObjectHandler<Tagging<String>> handler) {
            mCodec = codec;
            mHandler = handler;
        }
        public void handle(Chunking chunking) {
            Tagging<String> tagging = mCodec.toTagging(chunking);
            mHandler.handle(tagging);
        }
    }

    static class ChunkingHandlerAdapter implements ObjectHandler<StringTagging> {
        private final TagChunkCodec mCodec;
        private final ObjectHandler<Chunking> mHandler;
        public ChunkingHandlerAdapter(TagChunkCodec codec,
                                      ObjectHandler<Chunking> handler) {
            mCodec = codec;
            mHandler = handler;
        }
        public void handle(StringTagging tagging) {
            Chunking chunking = mCodec.toChunking(tagging);
            mHandler.handle(chunking);
        }
    }

    static class ChunkingHandlerAdapterPad implements ObjectHandler<Tagging<String>> {
        private final TagChunkCodec mCodec;
        private final ObjectHandler<Chunking> mHandler;
        public ChunkingHandlerAdapterPad(TagChunkCodec codec,
                                         ObjectHandler<Chunking> handler) {
            mCodec = codec;
            mHandler = handler;
        }
        public void handle(Tagging<String> tagging) {
            StringTagging stringTagging = pad(tagging);
            Chunking chunking = mCodec.toChunking(stringTagging);
            mHandler.handle(chunking);
        }
    }

}