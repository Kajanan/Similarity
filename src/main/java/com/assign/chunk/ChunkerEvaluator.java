

package com.assign.chunk;

import com.assign.classify.ScoredPrecisionRecallEvaluation;

import com.assign.corpus.ObjectHandler;

import com.assign.util.ObjectToCounterMap;
import com.assign.util.ScoredObject;
import com.assign.util.Strings;

import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class ChunkerEvaluator 
    implements ObjectHandler<Chunking> { 
               
    private Chunker mChunker;

    private boolean mVerbose = false;

    // 1st-best
    private final ChunkingEvaluation mChunkingEvaluation;

    
    private final ObjectToCounterMap<Integer> mCorrectRanks
        = new ObjectToCounterMap<Integer>();

    
    private final ScoredPrecisionRecallEvaluation mConfEval
        = new ScoredPrecisionRecallEvaluation();

  
    int mMaxNBest = 64;
    int mMaxNBestPrint = 8;
    String mLastNBestCase = null;

   
    int mConfMaxChunks = 128;
    String mLastConfidenceCase = null;


   
    public ChunkerEvaluator(Chunker chunker) {
        mChunker = chunker;
        mChunkingEvaluation = new ChunkingEvaluation();
    }

    public Chunker chunker() {
        return mChunker;
    }

    public void setChunker(Chunker chunker) {
        mChunker = chunker;
    }

    public void setVerbose(boolean isVerbose) {
        mVerbose = isVerbose;
    }

    public String lastFirstBestCaseReport() {
        return mChunkingEvaluation.mLastCase;
    }

    public void setMaxConfidenceChunks(int n) {
        mConfMaxChunks = n;
    }

    public String lastConfidenceCaseReport() {
        return mLastConfidenceCase;
    }

    public void setMaxNBest(int n) {
        mMaxNBest = n;
    }

    public void setMaxNBestReport(int n) {
        mMaxNBestPrint = n;
    }

    public String lastNBestCaseReport() {
        return mLastNBestCase;
    }

    void handle(String[] tokens, String[] whitespaces, String[] tags) {
        ChunkTagHandlerAdapter2 adapter = new ChunkTagHandlerAdapter2(this);
        adapter.handle(tokens,whitespaces,tags);
    }

    public void handle(Chunking referenceChunking) {
        CharSequence cSeq = referenceChunking.charSequence();


        // first-best
        Chunking firstBestChunking  = mChunker.chunk(cSeq);
        if (firstBestChunking == null)
            firstBestChunking = new ChunkingImpl(cSeq);
        mChunkingEvaluation.addCase(referenceChunking,firstBestChunking);

        if (mChunker instanceof NBestChunker) {
            NBestChunker nBestChunker = (NBestChunker) mChunker;
            char[] cs = Strings.toCharArray(cSeq);
            StringBuilder sb = new StringBuilder();

            sb.append(ChunkingEvaluation.formatHeader(13,referenceChunking));
            sb.append(" REF                 " + ChunkingEvaluation.formatChunks(referenceChunking));


            double score = Double.NEGATIVE_INFINITY;
            int foundRank = -1;
            int i = 0;
            Iterator<ScoredObject<Chunking>> nBestIt
                = nBestChunker.nBest(cs,0,cs.length,mMaxNBest);
            Formatter formatter = new Formatter(sb,Locale.US);
            for (i = 0; i < mMaxNBest && nBestIt.hasNext(); ++i) {
                ScoredObject<Chunking> so = nBestIt.next();
                score = so.score();
                Chunking responseChunking = so.getObject();
                if (i < mMaxNBestPrint) {
                    formatter.format("%9d",i);
                    sb.append(" ");
                    formatter.format("%10.3f",score);
                    sb.append(" ");
                    sb.append(ChunkingEvaluation.formatChunks(responseChunking));
                }
                if (responseChunking.equals(referenceChunking)) {
                    sb.append("  -----------\n");
                    foundRank = i;
                }
            }
            if (foundRank < 0)
                sb.append("Correct Rank >=" + mMaxNBest + "\n\n");
            else
                sb.append("Correct Rank=" + foundRank + "\n\n");
            mCorrectRanks.increment(Integer.valueOf(foundRank));

            mLastNBestCase = sb.toString();
        }

        if (mChunker instanceof ConfidenceChunker) {
            ConfidenceChunker confChunker = (ConfidenceChunker) mChunker;
            char[] cs = Strings.toCharArray(cSeq);
            StringBuilder sb = new StringBuilder();
            Set<Chunk> refChunks = new HashSet<Chunk>();
            for (Chunk nextChunk : referenceChunking.chunkSet()) {
                Chunk zeroChunk = toUnscoredChunk(nextChunk);
                refChunks.add(zeroChunk);
            }
            sb.append(ChunkingEvaluation.formatHeader(5,referenceChunking));

            Iterator<Chunk> nBestChunkIt
                = confChunker.nBestChunks(cs,0,cs.length,mConfMaxChunks);

            int count = 0;
            int missCount = refChunks.size();
            while (nBestChunkIt.hasNext()) {
                Chunk nextChunk = nBestChunkIt.next();
                double score = nextChunk.score();
                Chunk zeroedChunk = toUnscoredChunk(nextChunk);
                boolean correct = refChunks.contains(zeroedChunk);
                if (correct) --missCount;
                sb.append((correct ? "TRUE " : "false")
                          + " (" + nextChunk.start() + ", " + nextChunk.end() + ")"
                          + ": " + nextChunk.type()
                          + "  " + nextChunk.score() + "\n");

                mConfEval.addCase(correct,score);
            }
            mConfEval.addMisses(missCount);
            mLastConfidenceCase = sb.toString();
        }
        report();
    }

    void report() {
        if (!mVerbose) return;
        System.out.println(mChunkingEvaluation.mLastCase);
        if (mChunker instanceof NBestChunker)
            System.out.println(mLastNBestCase);
        if (mChunker instanceof ConfidenceChunker)
            System.out.println(mLastConfidenceCase);
    }

    public ScoredPrecisionRecallEvaluation confidenceEvaluation() {
        return mConfEval;
    }

    public ChunkingEvaluation evaluation() {
        return mChunkingEvaluation;
    }

    public ObjectToCounterMap<Integer> nBestEvaluation() {
        return mCorrectRanks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FIRST-BEST EVAL\n");
        sb.append(evaluation().toString());
        if (mChunker instanceof NBestChunker) {
            sb.append("\n\nN-BEST EVAL (rank=count)\n");
            sb.append(nBestEvaluation().toString());
        }
        if (mChunker instanceof ConfidenceChunker) {
            sb.append("\n\nCONFIDENCE EVALUATION");
            sb.append(confidenceEvaluation().toString());
        }
        return sb.toString();
    }

    static Chunk toUnscoredChunk(Chunk c) {
        return ChunkFactory.createChunk(c.start(),
                                        c.end(),
                                        c.type());
    }


}
