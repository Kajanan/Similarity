

package com.assign.classify;


public class Classification {

    private final String mBestCategory;

    public Classification(String bestCategory) {
        if (bestCategory == null) {
            String msg = "Category cannot be null for classifiers.";
            throw new IllegalArgumentException(msg);
        }
        mBestCategory = bestCategory;
    }

    
    public String bestCategory() {
        return mBestCategory;
    }

 
    @Override
    public String toString() {
        return "Rank    Category\n1=" + bestCategory();
    }

}
