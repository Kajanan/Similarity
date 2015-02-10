package com.assign.classify;

public interface ConditionalClassifier<E> extends ScoredClassifier<E> {

    public ConditionalClassification classify(E input);

}