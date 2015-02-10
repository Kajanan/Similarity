

package com.assign.chunk;

import com.assign.symbol.SymbolTableCompiler;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

class Node {

   
    float mOneMinusLambda;

    private int mIndex = -1;

    
    private int mTotalCount = 0;

   
    private short mNumOutcomes = 0;

    
    private final Map<String,Node> mChildren = new TreeMap<String,Node>();

    
    private final Map<String,OutcomeCounter> mOutcomes
        = new TreeMap<String,OutcomeCounter>();
    
    private final Node mBackoffNode;

  
    private final SymbolTableCompiler mSymbolTable;

    private final String mSymbol;

    public Node(String symbol, SymbolTableCompiler symbolTable,
                Node backoffNode) {

        mSymbol = symbol;
        if (symbolTable == null)
            throw new IllegalArgumentException("Null table.");
        mSymbolTable = symbolTable;
        if (symbol != null) symbolTable.addSymbol(symbol);
        mBackoffNode = backoffNode;
    }

    public void printSymbols() {
        if (mSymbolTable == null) System.out.println("NULL Symbol TABLE");
        System.out.println(mSymbolTable.toString());
    }

    public int getSymbolID() {
        if (mSymbol == null) return -1;
        return mSymbolTable.symbolToID(mSymbol);
    }

  
    public void generateSymbols() {
        if (mSymbol != null) mSymbolTable.addSymbol(mSymbol);
        for (OutcomeCounter counter : mOutcomes.values())
            counter.addSymbolToTable();
        for (Node child : mChildren.values())
            child.generateSymbols();
    }

    
    public int index() {
        return mIndex;
    }

   
    public void setIndex(int index) {
        mIndex = index;
    }

 
    public void prune(int threshold) {
        Iterator<String> outcomes = outcomes().iterator();
        while (outcomes.hasNext()) {
            OutcomeCounter counter = getOutcome(outcomes.next());
            if (counter.count() < threshold) {
                mTotalCount -= counter.count();
                --mNumOutcomes;
                outcomes.remove();
            }
        }
        Iterator<String> childrenIt = children().iterator();
        while (childrenIt.hasNext()) {
            Node childNode = getChild(childrenIt.next());
            childNode.prune(threshold);
            if (childNode.totalCount() < threshold)
                childrenIt.remove();
        }
    }

    public int numNodes() {
        int count = 1;
        for (String childString : children())
            count += getChild(childString).numNodes();
        return count;
    }

    public int numCounters() {
        int count = mOutcomes.keySet().size();
        for (String childString : children())
            count += getChild(childString).numCounters();
        return count;
    }

    public boolean hasOutcome(String outcome) {
        return mOutcomes.containsKey(outcome);
    }

    public OutcomeCounter getOutcome(String outcome) {
        return mOutcomes.get(outcome);
    }

    public boolean hasChild(String child) {
        return mChildren.containsKey(child);
    }

    public Node getChild(String child) {
        return mChildren.get(child);
    }

    public Node getOrCreateChild(String child, Node backoffNode,
                                 SymbolTableCompiler symbolTable) {
        if (hasChild(child)) return getChild(child);
        Node node = new Node(child, symbolTable, backoffNode);
        mChildren.put(child,node);
        return node;
    }

    public Set<String> outcomes() {
        return mOutcomes.keySet();
    }

    public Set<String> children() {
        return mChildren.keySet();
    }

    public int outcomeCount(String outcome) {
        OutcomeCounter ctr = getOutcome(outcome);
        return ctr == null ? 0 : ctr.count();
    }

    public void incrementOutcome(String outcome,
                                 SymbolTableCompiler symbolTable) {
        ++mTotalCount;
        if (hasOutcome(outcome)) {
            getOutcome(outcome).increment();
        } else {
            ++mNumOutcomes;
            mOutcomes.put(outcome,new OutcomeCounter(outcome,symbolTable,1));
        }
    }

    public int totalCount() {
        return mTotalCount;
    }

    public float oneMinusLambda() {
        return mOneMinusLambda;
    }

   public void compileEstimates(double lambdaFactor) {
        mOneMinusLambda = (float) java.lang.Math.log(1.0 - lambda(lambdaFactor));
        for (String outcome : outcomes()) {
            getOutcome(outcome).setEstimate((float)logEstimate(outcome,
                                                               lambdaFactor));
        }
        for (String childString : children()) {
            Node child = getChild(childString);
            child.compileEstimates(lambdaFactor);
        }
    }

   public double logEstimate(String outcome, double lambdaFactor) {
        return java.lang.Math.log(estimate(outcome,lambdaFactor));
    }

   public Node backoffNode() {
        return mBackoffNode;
    }

   
    public double estimate(String outcome, double lambdaFactor) {
        if (mBackoffNode == null) return maxLikelihoodEstimate(outcome);
        double lambda = lambda(lambdaFactor);
        return lambda * maxLikelihoodEstimate(outcome)
            + (1-lambda) * mBackoffNode.estimate(outcome,lambdaFactor);
    }

    
    public double maxLikelihoodEstimate(String outcome) {
        return ((double)outcomeCount(outcome)) / (double)mTotalCount;
    }

    
    public double lambda(double lambdaFactor) {
        if (mTotalCount == 0) return 0.0;
        return ((double)mTotalCount)
            / (mTotalCount + lambdaFactor * mNumOutcomes);
    }

}
