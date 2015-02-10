

package com.assign.classify;


public class Classified<E> {

    private final E mObject;
    private final Classification mClassification;

    public Classified(E object, Classification c) {
	mObject = object;
	mClassification = c;
    }

  
    public E getObject() {
	return mObject;
    }

    public Classification getClassification() {
	return mClassification;
    }

  
    @Override
    public String toString() {
        return mObject + ":" + mClassification.bestCategory();
    }

}