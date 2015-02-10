
package com.assign.chunk;

import com.assign.util.BoundedPriorityQueue;
import com.assign.util.ScoredObject;
import com.assign.util.Strings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public abstract class RescoringChunker<B extends NBestChunker>
    implements NBestChunker, ConfidenceChunker {

    final B mChunker;
    int mNumChunkingsRescored;

    public RescoringChunker(B chunker, int numChunkingsRescored) {
        mChunker = chunker;
        mNumChunkingsRescored = numChunkingsRescored;
    }

    public abstract double rescore(Chunking chunking);

    public B baseChunker() {
        return mChunker;
    }
    
    public int numChunkingsRescored() {
        return mNumChunkingsRescored;
    }

    public void setNumChunkingsRescored(int numChunkingsRescored) {
        mNumChunkingsRescored = numChunkingsRescored;
    }

    public Chunking chunk(CharSequence cSeq) {
        char[] cs = Strings.toCharArray(cSeq);
        return chunk(cs,0,cs.length);
    }

    public Chunking chunk(char[] cs, int start, int end) {
        return firstBest(mChunker.nBest(cs,start,end,mNumChunkingsRescored));
    }

    public Iterator<ScoredObject<Chunking>> nBest(char[] cs, int start, int end,
                                    int maxNBest) {
        return nBest(mChunker.nBest(cs,start,end,
                                    mNumChunkingsRescored),
                     maxNBest);
    }


 
    public Iterator<Chunk> nBestChunks(char[] cs, int start, int end,
                                       int maxNBest) {
        double totalScore = 0.0;
        Map<Chunk,Double> chunkToScore = new HashMap<Chunk,Double>();

        Iterator<ScoredObject<Chunking>> it = nBest(cs,start,end,mNumChunkingsRescored);
        while (it.hasNext()) {
            ScoredObject<Chunking> so = it.next();
            double score = java.lang.Math.pow(2.0,so.score());
            totalScore += score;
            Chunking chunking = so.getObject();
            for (Chunk chunk : chunking.chunkSet()) {
                Chunk unscoredChunk
                    = ChunkFactory.createChunk(chunk.start(), chunk.end(),
                                               chunk.type());
                Double currentScoreD = chunkToScore.get(chunk);
                double currentScore = currentScoreD == null
                    ? 0.0
                    : currentScoreD.doubleValue();
                double nextScore = currentScore + score;
                chunkToScore.put(unscoredChunk,Double.valueOf(nextScore));
            }
        }
        BoundedPriorityQueue<Chunk> bpq
            = new BoundedPriorityQueue<Chunk>(ScoredObject.comparator(),
                                              maxNBest);
        for (Map.Entry<Chunk,Double> entry : chunkToScore.entrySet()) {
            Chunk chunk = entry.getKey();
            double conditionalEstimate = entry.getValue().doubleValue()
                / totalScore;
            Chunk scored = ChunkFactory.createChunk(chunk.start(),
                                                    chunk.end(),
                                                    chunk.type(),
                                                    conditionalEstimate);
            bpq.offer(scored);
        }
        return bpq.iterator();
    }

    private Chunking firstBest(Iterator<ScoredObject<Chunking>> nBestChunkingIt) {
        Chunking bestChunking = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        while (nBestChunkingIt.hasNext()) {
            ScoredObject<Chunking> scoredChunking = nBestChunkingIt.next();
            Chunking chunking = scoredChunking.getObject();
            double score = rescore(chunking);
            if (score > bestScore) {
                bestScore = score;
                bestChunking = chunking;
            }
        }
        return bestChunking;
    }

    private Iterator<ScoredObject<Chunking>>
        nBest(Iterator<ScoredObject<Chunking>> nBestChunkingIt,
              int maxNBest) {

        BoundedPriorityQueue<ScoredObject<Chunking>> queue
            = new BoundedPriorityQueue<ScoredObject<Chunking>>(ScoredObject.comparator(),
                                                               maxNBest);
        while (nBestChunkingIt.hasNext()) {
            ScoredObject<Chunking> scoredChunking
                = nBestChunkingIt.next();
            Chunking chunking = scoredChunking.getObject();
            double score = rescore(chunking);
            queue.offer(new ScoredObject<Chunking>(chunking,score));
        }
        return queue.iterator();
    }


}
