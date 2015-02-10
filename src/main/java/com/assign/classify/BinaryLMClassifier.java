
package com.assign.classify;

import com.assign.lm.LanguageModel;
import com.assign.lm.UniformBoundaryLM;
import com.assign.lm.UniformProcessLM;

public class BinaryLMClassifier extends DynamicLMClassifier<LanguageModel.Dynamic> {

    private final String mAcceptCategory;
    private final String mRejectCategory;

    public BinaryLMClassifier(LanguageModel.Dynamic acceptingLM,
                              double crossEntropyThreshold) {
        this(acceptingLM,crossEntropyThreshold,
             DEFAULT_ACCEPT_CATEGORY,
             DEFAULT_REJECT_CATEGORY);
    }

   
    public BinaryLMClassifier(LanguageModel.Dynamic acceptingLM,
                              double crossEntropyThreshold,
                              String acceptCategory,
                              String rejectCategory) {
        super(new String[] { rejectCategory,
                             acceptCategory },
              new LanguageModel.Dynamic[] { 
                  createRejectLM(crossEntropyThreshold,
                                 acceptingLM),
                  acceptingLM });
        mAcceptCategory = acceptCategory;
        mRejectCategory = rejectCategory;
        // set up to uniform distribution 
        categoryDistribution().train(acceptCategory,1);
        categoryDistribution().train(rejectCategory,1);
    }

  
    public String acceptCategory() {
        return mAcceptCategory;
    }

    public String rejectCategory() {
        return mRejectCategory;
    }

    @Override
    void train(String category, char[] cs, int start, int end) {
        super.train(category,cs,start,end);
    }

    @Override
    void train(String category, CharSequence cSeq) {
        languageModel(mAcceptCategory).train(cSeq);
    }

    @Override
    public void handle(Classified<CharSequence> classified) {
        CharSequence cSeq  = classified.getObject();
        Classification classification = classified.getClassification();
        String bestCategory = classification.bestCategory();
        if (mRejectCategory.equals(bestCategory))
            return; // silently ignore reject data
        if (!mAcceptCategory.equals(bestCategory)) {
            String msg = "Require accept or reject category."
                + " Accept category=" + mAcceptCategory
                + " Reject category=" + mRejectCategory
                + " Found classified best category=" + bestCategory;
            throw new IllegalArgumentException(msg);
        }
        languageModel(mAcceptCategory).train(cSeq);
    }

    @Override
    public void resetCategory(String category,
                              LanguageModel.Dynamic lm,
                              int newCount) {
        String msg = "Resets not allowed for Binary LM classifier.";
        throw new UnsupportedOperationException(msg);
    }

    static LanguageModel.Dynamic 
        createRejectLM(double crossEntropyThreshold,
                       LanguageModel acceptingLM) {

        if (acceptingLM instanceof LanguageModel.Sequence) 
            return new UniformBoundaryLM(crossEntropyThreshold);
        else 
            return new UniformProcessLM(crossEntropyThreshold);
    }
   
    public static final String DEFAULT_ACCEPT_CATEGORY
        = Boolean.TRUE.toString();

    public static final String DEFAULT_REJECT_CATEGORY
        = Boolean.FALSE.toString();
 

}
