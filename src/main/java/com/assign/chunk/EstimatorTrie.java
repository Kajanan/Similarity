
package com.assign.chunk;

import java.io.IOException;
import java.io.ObjectInput;

class EstimatorTrie {

    private final int _numNodes;

    private final int[] _nodeSymbol;

    private final int[] _nodeFirstOutcome;

    private final int[] _nodeFirstChild;

    private final float[] _nodeLogOneMinusLambda;

    private final int[] _nodeBackoff;

    private final int _numOutcomes;

    private final int[] _outcomeSymbol;

    private final float[] _outcomeLogEstimate;

   
    public EstimatorTrie(ObjectInput in) throws IOException {
        _numNodes = in.readInt();
        _nodeSymbol = new int[_numNodes];
        _nodeFirstOutcome = new int[_numNodes+1];
        _nodeFirstChild = new int[_numNodes+1];
        _nodeLogOneMinusLambda = new float[_numNodes];
        _nodeBackoff = new int[_numNodes];
        for (int i = 0; i < _numNodes; ++i) {
            _nodeSymbol[i] = in.readInt();
            _nodeFirstOutcome[i] = in.readInt();
            _nodeFirstChild[i] = in.readInt();
            _nodeLogOneMinusLambda[i] = in.readFloat();
            _nodeBackoff[i] = in.readInt();
        }
        _nodeFirstChild[_numNodes] = _numNodes; // boundary
        _numOutcomes = in.readInt();
        _nodeFirstOutcome[_numNodes] = _numOutcomes; // boundary
        _outcomeSymbol = new int[_numOutcomes];
        _outcomeLogEstimate = new float[_numOutcomes];
        for (int i = 0; i < _numOutcomes; ++i) {
            _outcomeSymbol[i] = in.readInt();
            _outcomeLogEstimate[i] = in.readFloat();
        }
    }

   
    public double estimateFromNode(int symbolID, int nodeIndex) {
        if (symbolID < 0) return Double.NaN;
        double backoffAccumulator = 0.0;
        for(int currentNodeIndex = nodeIndex;
            currentNodeIndex >= 0;
            currentNodeIndex = _nodeBackoff[currentNodeIndex]) {
            int low = _nodeFirstOutcome[currentNodeIndex];
            int high = _nodeFirstOutcome[currentNodeIndex+1]-1;
            while (low <= high) {
                int mid = (high + low)/2;
                if (_outcomeSymbol[mid] == symbolID)
                    return backoffAccumulator + _outcomeLogEstimate[mid];
                else if (_outcomeSymbol[mid] < symbolID)
                    low = (low == mid ? mid+1 : mid);
                else high = (high == mid ? mid-1 : mid);
            }
            backoffAccumulator += _nodeLogOneMinusLambda[currentNodeIndex];
        }
        return Double.NaN; // no more backoff nodes available
    }

    public double estimateFromNodeUniform(int symbolID, int nodeIndex,
                                          double uniformEstimate) {

        if (symbolID < 0) return Double.NaN;
        double backoffAccumulator = 0.0;
        for (int currentNodeIndex = nodeIndex;
             currentNodeIndex >= 0;
             currentNodeIndex = _nodeBackoff[currentNodeIndex]) {
            int low = _nodeFirstOutcome[currentNodeIndex];
            int high = _nodeFirstOutcome[currentNodeIndex+1]-1;
            while (low <= high) {
                int mid = (high + low)/2;
                if (_outcomeSymbol[mid] == symbolID)
                    return backoffAccumulator + _outcomeLogEstimate[mid];
                else if (_outcomeSymbol[mid] < symbolID)
                    low = (low == mid ? mid+1 : mid);
                else high = (high == mid ? mid-1 : mid);
            }
            backoffAccumulator += _nodeLogOneMinusLambda[currentNodeIndex];
        }
        return (backoffAccumulator + uniformEstimate);
    }

   
    public int lookupChild(int symbolID, int parentNodeIndex) {
        int low = _nodeFirstChild[parentNodeIndex];
        int high = _nodeFirstChild[parentNodeIndex+1]-1;
        if (symbolID < 0) return -1;
        while (low <= high) {
            int mid = (high + low)/2;
            if (_nodeSymbol[mid] == symbolID) return mid;
            else if (_nodeSymbol[mid] < symbolID)
                low = (low == mid ? mid+1 : mid);
            else high = (high == mid ? mid-1 : mid);
        }
        return -1;
    }


}

