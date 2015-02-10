

package com.assign.chunk;

import com.assign.symbol.SymbolTableCompiler;

import com.assign.tokenizer.TokenCategorizer;

import com.assign.util.AbstractExternalizable;
import com.assign.util.Compilable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;



final class TrainableEstimator implements Compilable {

  
    private Node mRootTagNode;

    
    private Node mRootTokenNode;

   
    private final SymbolTableCompiler mTokenSymbolTable
        = new SymbolTableCompiler();

    private final SymbolTableCompiler mTagSymbolTable
        = new SymbolTableCompiler();

    private double mLambdaFactor;

    private double mLogUniformVocabEstimate;

    
    private final TokenCategorizer mTokenCategorizer;

    public TrainableEstimator(double lambdaFactor,
                              double logUniformVocabEstimate,
                              TokenCategorizer categorizer) {
        mLambdaFactor = lambdaFactor;
        mLogUniformVocabEstimate = logUniformVocabEstimate;
        mTokenCategorizer = categorizer;
        mRootTagNode = new Node(null,mTagSymbolTable,null);
        mRootTokenNode = new Node(null,mTokenSymbolTable,null);
        mTagSymbolTable.addSymbol(Tags.OUT_TAG);
    }

    public TrainableEstimator(TokenCategorizer categorizer) {
        this (4.0,
              java.lang.Math.log(1.0/1000000.0),
              categorizer);
    }


    public void setLambdaFactor(double lambdaFactor) {
        if (lambdaFactor < 0.0
            || Double.isNaN(lambdaFactor)
            || Double.isInfinite(lambdaFactor))
            throw new
                IllegalArgumentException("Lambda factor must be > 0."
                                         + " Was=" + lambdaFactor);
        mLambdaFactor = lambdaFactor;
    }

  
    public void setLogUniformVocabularyEstimate(double estimate) {
        if (estimate >= 0.0
            || Double.isNaN(estimate)
            || Double.isInfinite(estimate))
            throw new
                IllegalArgumentException("Log vocab estimate must be < 0."
                                         + " Was=" + estimate);
        mLogUniformVocabEstimate = estimate;
    }
    
    public void handle(String[] tokens, String[] tags) {

        if (tokens.length < 1) return;
        trainOutcome(tokens[0],tags[0],
                     Tags.START_TAG,
                     Tags.START_TOKEN,Tags.START_TOKEN);
        if (tokens.length < 2) {
           
            trainOutcome(Tags.START_TOKEN,Tags.START_TAG,
                         tags[0],
                         tokens[0], Tags.START_TOKEN);
            return;
        }

        
        trainOutcome(tokens[1],tags[1],
                     tags[0],
                     tokens[0],Tags.START_TOKEN);

        
        for (int i = 2; i < tokens.length; ++i)
            trainOutcome(tokens[i],tags[i],
                         tags[i-1],
                         tokens[i-1],tokens[i-2]);

       
        trainOutcome(Tags.START_TOKEN, Tags.START_TAG,
                     tags[tags.length-1],
                     tokens[tokens.length-1], tokens[tokens.length-2]);
    }

   
    public void compileTo(ObjectOutput out) throws IOException {
        out.writeObject(new Externalizer(this));
    }

    static class Externalizer extends AbstractExternalizable {
        private static final long serialVersionUID = 4179100933315980535L;
        final TrainableEstimator mEstimator;
        public Externalizer() {
            this(null);
        }
        public Externalizer(TrainableEstimator estimator) {
            mEstimator = estimator;
        }
        @Override
        public Object read(ObjectInput in)
            throws ClassNotFoundException, IOException {

            return new CompiledEstimator(in);
        }
        @Override
        public void writeExternal(ObjectOutput objOut) throws IOException {
            AbstractExternalizable.compileOrSerialize(mEstimator.mTokenCategorizer,objOut);
            mEstimator.generateSymbols();
            mEstimator.mTagSymbolTable.compileTo(objOut);
            mEstimator.mTokenSymbolTable.compileTo(objOut);
            mEstimator.writeEstimator(mEstimator.mRootTagNode,objOut);
            mEstimator.writeEstimator(mEstimator.mRootTokenNode,objOut);
            objOut.writeDouble(mEstimator.mLogUniformVocabEstimate);
        }
    }


    public void trainOutcome(String token, String tag,
                             String tagMinus1,
                             String tokenMinus1, String tokenMinus2) {
        mTagSymbolTable.addSymbol(tag);
        mTokenSymbolTable.addSymbol(token);
        String tagMinus1Interior
            = (tagMinus1 == null)
            ? null
            : Tags.toInnerTag(tagMinus1);
        trainTokenModel(token,tag,tagMinus1Interior,tokenMinus1);
        trainTagModel(tag,tagMinus1Interior,tokenMinus1,tokenMinus2);
    }

    private void generateSymbols() {
        mRootTagNode.generateSymbols();
        
        mRootTokenNode.generateSymbols();
        
        String[] tokenCategories = mTokenCategorizer.categories();
        for (int i = 0; i < tokenCategories.length; ++i)
            mTokenSymbolTable.addSymbol(tokenCategories[i]);
    }

  
    public void trainTokenModel(String token,
                                String tag, String tagMinus1,
                                String tokenMinus1) {

       
        if (tag == null || token == null) return;
        Node nodeTag
            = mRootTokenNode.getOrCreateChild(tag,null,mTagSymbolTable);
        nodeTag.incrementOutcome(token,mTokenSymbolTable);

        if (tagMinus1 == null) return;
        Node nodeTagTag1
            = nodeTag.getOrCreateChild(tagMinus1,nodeTag,mTagSymbolTable);
        nodeTagTag1.incrementOutcome(token,mTokenSymbolTable);

        if (tokenMinus1 == null) return;
        Node nodeTagTag1W1
            = nodeTagTag1.getOrCreateChild(tokenMinus1,
                                           nodeTagTag1,mTokenSymbolTable);
        nodeTagTag1W1.incrementOutcome(token,mTokenSymbolTable);
    }


  
    public void trainTagModel(String tag,
                              String tagMinus1,
                              String tokenMinus1, String tokenMinus2) {


        if (tag == null || tagMinus1 == null) return;
        Node nodeTag1
            = mRootTagNode.getOrCreateChild(tagMinus1,null,mTagSymbolTable);
        nodeTag1.incrementOutcome(tag,mTagSymbolTable);

        if (tokenMinus1 == null) return;
        Node nodeTag1W1
            = nodeTag1.getOrCreateChild(tokenMinus1,
                                        nodeTag1,mTokenSymbolTable);
        nodeTag1W1.incrementOutcome(tag,mTagSymbolTable);

        if (tokenMinus2 == null) return;
        Node nodeTag1W1W2
            = nodeTag1W1.getOrCreateChild(tokenMinus2,
                                          nodeTag1W1,mTokenSymbolTable);
        nodeTag1W1W2.incrementOutcome(tag,mTagSymbolTable);
    }

  
    public void trainTokenOutcome(String token, String tag) {
        trainTokenModel(token,tag,null,null);
    }


    public int numTagNodes() {
        return mRootTagNode.numNodes();
    }

    public int numTagOutcomes() {
        return mRootTagNode.numCounters();
    }

    
    public int numTokenNodes() {
        return mRootTokenNode.numNodes();
    }

    public int numTokenOutcomes() {
        return mRootTokenNode.numCounters();
    }


    public void prune(int thresholdTag, int thresholdToken) {
        mRootTagNode.prune(thresholdTag);
        mRootTokenNode.prune(thresholdToken);
    }

    public void smoothTags(int countToAdd) {
        // mTagSymbolTable.add(Tags.OUT);
        String[] tags = mTagSymbolTable.symbols();
        for (int i = 0; i < tags.length; ++i) {
            String tag1 = tags[i];
            for (int j = 0; j < tags.length; ++j) {
                String tag2 = tags[j];
                if (Tags.illegalSequence(tag1,tag2)) continue;
                for (int k = 0; k < countToAdd; ++k) {
                    trainTagModel(tag2,tag1,null,null);
                }
            }
        }
    }

   
    private void writeEstimator(Node rootNode,
                                ObjectOutput out)
        throws IOException {

        rootNode.compileEstimates(mLambdaFactor);
        indexNodes(rootNode);
        out.writeInt(rootNode.numNodes());
        writeNodes(rootNode,out);
        out.writeInt(rootNode.numCounters());
        writeOutcomes(rootNode,out);
    }

    
    private static void indexNodes(Node rootNode) {
        LinkedList<Node> nodeQueue = new LinkedList<Node>();
        nodeQueue.addLast(rootNode);
        int index = 0;
        while (nodeQueue.size() > 0) {
            Node node = nodeQueue.removeFirst();
            node.setIndex(index++);
            for (String childString : node.children())
                nodeQueue.addLast(node.getChild(childString));
        }
    }

   
    private static void writeNodes(Node rootNode, ObjectOutput out)
        throws IOException {

        LinkedList<Object[]> nodeQueue = new LinkedList<Object[]>();
        nodeQueue.addLast(new Object[] {rootNode,null} );
        int outcomesIndex = 0;
        int index = 0;
        while (nodeQueue.size() > 0) {

            Object[] pair = nodeQueue.removeFirst();
            Node node = (Node) pair[0];
            out.writeInt(node.getSymbolID());
            out.writeInt(outcomesIndex);
            outcomesIndex += node.outcomes().size();
            TreeSet<String> children = new TreeSet<String>(node.children());
            if (children.size() == 0) {
                out.writeInt(index);
            } else {
                Iterator<String> childIterator = children.iterator();
                Node firstChild
                    = node.getChild(childIterator.next());
                out.writeInt(firstChild.index());
                index = firstChild.index() + node.children().size();
                childIterator = children.iterator();
                while (childIterator.hasNext()) {
                    String childName =  childIterator.next();
                    Node childNode = node.getChild(childName);
                    nodeQueue.addLast(new Object[] {childNode,childName});
                }
            }
            out.writeFloat(node.oneMinusLambda());
            out.writeInt(node.backoffNode() == null
                         ? -1 : node.backoffNode().index());
        }
    }

  
    private static void writeOutcomes(Node rootNode, ObjectOutput out)
        throws IOException {

        LinkedList<Node> nodeQueue = new LinkedList<Node>();
        nodeQueue.addLast(rootNode);
        while (nodeQueue.size() > 0) {
            Node node = nodeQueue.removeFirst();
            for (String outcome : node.outcomes()) {
                OutcomeCounter outcomeCounter = node.getOutcome(outcome);
                out.writeInt(outcomeCounter.getSymbolID());
                out.writeFloat(outcomeCounter.estimate());
            }
            for (String child : node.children())
                nodeQueue.addLast(node.getChild(child));
        }
    }

}
