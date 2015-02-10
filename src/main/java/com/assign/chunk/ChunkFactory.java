
package com.assign.chunk;

public class ChunkFactory {

   
    private ChunkFactory() { 
        
    }

   
    public static final String DEFAULT_CHUNK_TYPE = "CHUNK";

   
    public static final double DEFAULT_CHUNK_SCORE 
        = Double.NEGATIVE_INFINITY;
    
    
    public static Chunk createChunk(int start, int end) {
        validateSpan(start,end);
        return new StartEndChunk(start,end);
    }

    public static Chunk createChunk(int start, int end, String type) {
        validateSpan(start,end);
        return new StartEndTypeChunk(start,end,type);
    }

    public static Chunk createChunk(int start, int end, double score) {
        validateSpan(start,end);
        return new StartEndScoreChunk(start,end,score);
    }

    public static Chunk createChunk(int start, int end, 
                                    String type, double score) {
        validateSpan(start,end);
        return new StartEndTypeScoreChunk(start,end,type,score);
    }

    private static void validateSpan(int start, int end) {
        if (start < 0) {
            String msg = "Start must be >= 0."
                + " Found start=" + start;
            throw new IllegalArgumentException(msg);
        }
        if (start > end) {
            String msg = "Start must be > end."
                + " Found start=" + start
                + " end=" + end;
            throw new IllegalArgumentException(msg);
        }
    }

    private static abstract class AbstractChunk implements Chunk {
        private final int mStart;
        private final int mEnd;
        AbstractChunk(int start, int end) {
            mStart = start;
            mEnd = end;
        }
        public final int start() {
            return mStart;
        }
        public final int end() {
            return mEnd;
        }
        public String type() {
            return DEFAULT_CHUNK_TYPE;
        }
        public double score() {
            return DEFAULT_CHUNK_SCORE;
        }
        @Override
        public boolean equals(Object that) {
            if (!(that instanceof Chunk)) return false;
            Chunk thatChunk = (Chunk) that;
            return start() == thatChunk.start()
                && end() == thatChunk.end()
                && score() == thatChunk.score()
                && type().equals(thatChunk.type());
        }
        @Override
        public int hashCode() {
            int h1 = start();
            int h2 = end();
            int h3 = type().hashCode();
            return h1 + 31*(h2 + 31*h3); // ignores score
        }
        @Override
        public String toString() {
            return start() + "-" + end() + ":" + type() + "@" + score();
        }
    }

    private static final class StartEndChunk extends AbstractChunk {
        StartEndChunk(int start, int end) {
            super(start,end);
        }
    }

    private static final class StartEndTypeChunk extends AbstractChunk {
        private final String mType;
        StartEndTypeChunk(int start, int end, String type) {
            super(start,end);
            mType = type;
        }
        @Override
        public String type() {
            return mType;
        }
    }

    private static final class StartEndScoreChunk extends AbstractChunk {
        private final double mScore;
        StartEndScoreChunk(int start, int end, double score) {
            super(start,end);
            mScore = score;
        }
        @Override
        public double score() {
            return mScore;
        }
    }

    private static final class StartEndTypeScoreChunk extends AbstractChunk {
        private final String mType;
        private final double mScore;
        StartEndTypeScoreChunk(int start, int end, String type,
                               double score) {
            super(start,end);
            mType = type;
            mScore = score;
        }
        @Override
        public String type() {
            return mType;
        }
        @Override
        public double score() {
            return mScore;
        }
    }

}
