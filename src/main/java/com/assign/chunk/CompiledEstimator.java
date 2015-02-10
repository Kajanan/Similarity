
package com.assign.chunk;

import com.assign.symbol.SymbolTable;

import com.assign.tokenizer.TokenCategorizer;

import java.io.IOException;
import java.io.ObjectInput;

import java.util.ArrayList;
import java.util.List;

final class CompiledEstimator {

    private final EstimatorTrie mTagTrie;

    private final EstimatorTrie mTokenTrie;

    
    private final SymbolTable mTagSymbolTable;

    
    private final SymbolTable mTokenSymbolTable;

    
    private final boolean[][] mCannotFollow;

   
    private final int[] mConvertToInterior;

    
    private final int[] mStart;

    
    private final int[] mInterior;

    
    private final double mLogUniformVocabEstimate;

    private final TokenCategorizer mTokenCategorizer;


    public CompiledEstimator(ObjectInput in)
        throws ClassNotFoundException, IOException {

        mTokenCategorizer = (TokenCategorizer) in.readObject();

        mTagSymbolTable = (SymbolTable) in.readObject();
        mTokenSymbolTable = (SymbolTable) in.readObject();

        // read from model & put in training
        mTagTrie = new EstimatorTrie(in);
        mTokenTrie = new EstimatorTrie(in);
        mLogUniformVocabEstimate = in.readDouble();

        int numSymbols = mTagSymbolTable.numSymbols();
        mConvertToInterior = new int[numSymbols];
        mCannotFollow = new boolean[numSymbols][numSymbols];
        int numTags = mTagSymbolTable.numSymbols();
        List<Integer> starts = new ArrayList<Integer>();
        List<Integer> interiors = new ArrayList<Integer>();
        for (int tagID = 0; tagID < numTags; ++tagID) {
            String tag = idToTag(tagID);
            mConvertToInterior[tagID] = tagToInteriorID(tag);
            if (tagID != mConvertToInterior[tagID]) {
                interiors.add(Integer.valueOf(mConvertToInterior[tagID]));
                starts.add(Integer.valueOf(tagID));
            }
            for (int tagMinus1ID = 0; tagMinus1ID < numTags; ++tagMinus1ID)
                mCannotFollow[tagID][tagMinus1ID]
                    = Tags.illegalSequence(idToTag(tagMinus1ID),tag);
        }
        mStart = convertToIntArray(starts);
        mInterior = convertToIntArray(interiors);
    }

  
    public int[] startTagIDs() {
        return mStart;
    }

    public int[] interiorTagIDs() {
        return mInterior;
    }

   
    public int numTags() {
        return mTagSymbolTable.numSymbols();
    }

  
    public int tagToID(String tag) {
        return mTagSymbolTable.symbolToID(tag);
    }

    public String idToTag(int id) {
        return mTagSymbolTable.idToSymbol(id);
    }

    public int tokenToID(String token) {
        return mTokenSymbolTable.symbolToID(token);
    }

  
    public int tokenOrCategoryToID(String token) {
        int id = tokenToID(token);
        if (id < 0) {
            id = tokenToID(mTokenCategorizer.categorize(token));
            if (id < 0) {
                System.err.println("No id for token category: " + token);
            }
        }
        return id;
    }

    public String idToToken(int id) {
        return mTokenSymbolTable.idToSymbol(id);

    }

    
    public boolean cannotFollow(int tagID, int tagMinus1ID) {
        return mCannotFollow[tagID][tagMinus1ID];
    }

   
    private int idToInteriorID(int tagID) {
        return mConvertToInterior[tagID];
    }


  
    public double estimate(int tagID, int tokenID,
                           int tagMinus1ID,
                           int tokenMinus1ID,
                           int tokenMinus2ID) {
        if (cannotFollow(tagID,tagMinus1ID)) return Double.NaN;
        int tagMinus1IDInterior = idToInteriorID(tagMinus1ID);
        return estimateTag(tagID,tagMinus1IDInterior,
                           tokenMinus1ID,tokenMinus2ID)
            + estimateToken(tokenID,tagID,tagMinus1IDInterior,tokenMinus1ID);
    }

    private double estimateTag(int tagID,
                               int tagMinus1ID,
                               int tokenMinus1ID,
                               int tokenMinus2ID) {
        
        int nodeTag1Index = mTagTrie.lookupChild(tagMinus1ID,0);
        if (nodeTag1Index == -1) {
            // no outcomes for simple tag -- really an error
            return Double.NaN;
        }
        int nodeTag1W1Index
            = mTagTrie.lookupChild(tokenMinus1ID,nodeTag1Index);
        if (nodeTag1W1Index == -1) {
            return mTagTrie.estimateFromNode(tagID,nodeTag1Index);
        }
        int nodeTag1W1W2Index
            = mTagTrie.lookupChild(tokenMinus2ID,nodeTag1W1Index);
        if (nodeTag1W1W2Index == -1) {
            return mTagTrie.estimateFromNode(tagID,nodeTag1W1Index);
        }
        return mTagTrie.estimateFromNode(tagID,nodeTag1W1W2Index);
    }

   
    private double estimateToken(int tokenID,
                                 int tagID, int tagMinus1ID,
                                 int tokenMinus1ID) {
        int nodeTagIndex = mTokenTrie.lookupChild(tagID,0);
        if (nodeTagIndex == -1)
            return Double.NaN;
        int nodeTagTag1Index = mTokenTrie.lookupChild(tagMinus1ID,nodeTagIndex);
        if (nodeTagTag1Index == -1) {
            return
                mTokenTrie.estimateFromNodeUniform(tokenID,
                                                   nodeTagIndex,
                                                   mLogUniformVocabEstimate);
        }
        int nodeTagTag1W1Index
            = mTokenTrie.lookupChild(tokenMinus1ID,nodeTagTag1Index);
        if (nodeTagTag1W1Index != -1) {
            return
                mTokenTrie.estimateFromNodeUniform(tokenID,
                                                   nodeTagTag1W1Index,
                                                   mLogUniformVocabEstimate);
        }
        return mTokenTrie.estimateFromNodeUniform(tokenID,
                                                  nodeTagTag1Index,
                                                  mLogUniformVocabEstimate);
    }

    /**
     * Return the identifier for the base tag corresponding
     * to the specified tag.
     *
     * @param tag Tag whose base tag ID is returned.
     * @return Identifier for base tag of specified tag.
     */
    private int tagToInteriorID(String tag) {
        return tagToID(Tags.toInnerTag(tag));
    }

    /**
     * Convert the array list of <code>Integer</code> objects to an
     * array of their integer values.
     *
     * @param xs Arraylist of Integer objects.
     * @return Array of integer values for the specified array of
     * objects.
     */
    private static int[] convertToIntArray(List<Integer> xs) {
        int[] result = new int[xs.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = xs.get(i).intValue();
        return result;
    }

}
