

package com.assign.chunk;

import com.assign.tokenizer.TokenCategorizer;
import com.assign.util.Strings;

import java.util.Arrays;

final class TokenShapeDecoder {

    private double mLog2Beam;

    private final CompiledEstimator mEstimator;

    private final TokenCategorizer mTokenCategorizer;

    public TokenShapeDecoder(CompiledEstimator estimator,
			     TokenCategorizer categorizer,
			     double pruningThreshold) {
        mEstimator = estimator;
        mTokenCategorizer = categorizer;
        mLog2Beam = pruningThreshold;
    }

    void setLog2Beam(double beam) {
	mLog2Beam = beam;
    }


    public String[] decodeTags(String[] tokens) {
        if (tokens == null) return null;
        if (tokens.length == 0) return Strings.EMPTY_STRING_ARRAY;
        TagHistory th = decode(tokens);
        String[] result = new String[tokens.length];
	if (th == null) {
	    // last resort recover to all OUT tags
	    Arrays.fill(result,Tags.OUT_TAG);
	    return result;
	}
        th.toTagArray(mEstimator, result);
        return result;
    }

    private TagHistory decode(String[] tokens) {
        int numTags = mEstimator.numTags();
        TagHistory[] history = new TagHistory[numTags];
        double[] historyScore = new double[numTags];
        TagHistory[] nextHistory = new TagHistory[numTags];
        double[] nextHistoryScore = new double[numTags];

        int startTagID = mEstimator.tagToID(Tags.START_TAG);
        int startTokenID = mEstimator.tokenToID(Tags.START_TOKEN);

        int tokenID;
        int tokenMinus1ID = startTokenID;
        int tokenMinus2ID = startTokenID;

	int outTagID = mEstimator.tagToID(Tags.OUT_TAG);

       
        String token = tokens[0];
        tokenID = mEstimator.tokenToID(token);

       
        if (tokenID < 0) {
            String tokenCategory = mTokenCategorizer.categorize(token);
            tokenID = mEstimator.tokenToID(tokenCategory);
	  
        }

      
        for (int resultTagID = 0; resultTagID < numTags; ++resultTagID) {
            if (mEstimator.cannotFollow(resultTagID,startTagID)) {
                historyScore[resultTagID] = Double.NaN;
                continue;
            }
            historyScore[resultTagID]
                = mEstimator.estimate(resultTagID, tokenID,
                                      startTagID,
                                      tokenMinus1ID,
                                      tokenMinus2ID);
        

            history[resultTagID] = ( Double.isNaN(historyScore[resultTagID])
                                     ? (TagHistory)null
                                     : new TagHistory(resultTagID,null)  );
	   
        }

        
        for (int i = 1; i < tokens.length; ++i) {

            token = tokens[i];
            tokenID = mEstimator.tokenToID(token);

            if (tokenID < 0) {
                String tokenCategory = mTokenCategorizer.categorize(token);
                tokenID
                    = mEstimator.tokenToID(tokenCategory);
		
            }

            for (int resultTagID = 0; resultTagID < numTags; ++resultTagID) {
                int bestPreviousTagID = -1;
                double bestScore = Double.NaN;

                
                for (int previousTagID = 0; previousTagID < numTags; ++previousTagID) {
                    if (history[previousTagID] == null) continue;

                    if (mEstimator.cannotFollow(resultTagID,previousTagID))
                        continue;

                    double estimate = mEstimator.estimate(resultTagID, tokenID,
                                                          previousTagID,
                                                          tokenMinus1ID,
                                                          tokenMinus2ID);
                    if (!Double.isNaN(estimate)
                        && (bestPreviousTagID == -1
                            || ( estimate + historyScore[previousTagID]
                                 > bestScore))) {
                        bestPreviousTagID = previousTagID;
                        bestScore = estimate + historyScore[previousTagID];
                    }
                }

                if (bestPreviousTagID == -1) {
                    nextHistory[resultTagID] = null;
                } else {
                    nextHistory[resultTagID]
                        = new TagHistory(resultTagID,
                                         history[bestPreviousTagID]);
                    nextHistoryScore[resultTagID] = bestScore;
                }
            }

            int[] startIds = mEstimator.startTagIDs();
            int[] interiorIds = mEstimator.interiorTagIDs();
            for (int m = 0; m < startIds.length; ++m) {
                if (nextHistory[startIds[m]] == null
                    || nextHistory[interiorIds[m]] == null) continue;
                if (nextHistoryScore[startIds[m]] >
                    nextHistoryScore[interiorIds[m]]) {

                    nextHistoryScore[interiorIds[m]] = Double.NaN;
                    nextHistory[interiorIds[m]] = null;
                } else {
                    nextHistoryScore[startIds[m]] = Double.NaN;
                    nextHistory[startIds[m]] = null;
                }

            }

            double bestScore = Double.NaN;
	    TagHistory bestPreviousHistory = null;
            for (int resultTagID = 0; resultTagID < numTags; ++resultTagID) {
                if (nextHistory[resultTagID] == null) continue;
                if (Double.isNaN(bestScore)
                    || nextHistoryScore[resultTagID] > bestScore) {
                    bestScore = nextHistoryScore[resultTagID];
		    bestPreviousHistory = nextHistory[resultTagID];
                }
            }

	    double worstScoreToKeep = bestScore - mLog2Beam;
            for (int resultTagID = 0; resultTagID < numTags; ++resultTagID) {
		
		if (resultTagID == outTagID) {
		    if (nextHistory[outTagID] == null) {
			nextHistory[outTagID]  // fill if necessary
			    = new TagHistory(outTagID,bestPreviousHistory);
			if (Double.isNaN(nextHistoryScore[outTagID])
			    || Double.isInfinite(nextHistoryScore[outTagID])) {
			    nextHistoryScore[outTagID] = bestScore;
			}
		    }
		    continue; // no OUT pruning
		} 
                if (nextHistory[resultTagID] == null) continue;
                if (nextHistoryScore[resultTagID] < worstScoreToKeep)
                    nextHistory[resultTagID] = null;
            }

            if (allNull(nextHistory)) {               
		return null;
            }

            tokenMinus2ID = tokenMinus1ID;
            tokenMinus1ID = tokenID;
            TagHistory[] tempHistory = history;
            double[] tempHistoryScore = historyScore;
            history = nextHistory;
            historyScore = nextHistoryScore;
            nextHistory = tempHistory;
            nextHistoryScore = tempHistoryScore;
        }
        return extractBest(history,historyScore);
    }

 
    private TagHistory extractBest(TagHistory[] history,
                                   double[] historyScore) {
        int bestIndex = -1;
        for (int i = 0; i < history.length; ++i) {
            if (history[i] == null) continue;
            else if (bestIndex == -1) bestIndex = i;
            else if (historyScore[i] > historyScore[bestIndex]) bestIndex = i;
        }
        return bestIndex == -1 ? null : history[bestIndex];
    }

   
    private static boolean allNull(Object[] xs) {
        for (int i = 0; i < xs.length; ++i)
            if (xs[i] != null)
                return false;
        return true;
    }


    private static final class TagHistory {

        private final int mTag;

      
        private final TagHistory mPreviousHistory;

     
        public TagHistory(int tag, TagHistory previousHistory) {
            mTag = tag;
            mPreviousHistory = previousHistory;
        }

        public void toTagArray(CompiledEstimator estimator,
                               String[] result) {
            TagHistory history = this;
            for (int i = result.length;
                 --i >= 0;
                 history = history.mPreviousHistory)

                result[i] = estimator.idToTag(history.mTag);
        }
    }


}
