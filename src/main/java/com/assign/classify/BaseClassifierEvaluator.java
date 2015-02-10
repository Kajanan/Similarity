package com.assign.classify;

import com.assign.corpus.ObjectHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseClassifierEvaluator<E> 
    implements ObjectHandler<Classified<E>> {

    private final ConfusionMatrix mConfusionMatrix;
    private int mNumCases = 0;
    final String[] mCategories;
    final Set<String> mCategorySet;
    final boolean mStoreInputs;
    BaseClassifier<E> mClassifier;
    final List<Classification> mClassifications = new ArrayList<Classification>();
    final List<E> mCases = new ArrayList<E>();
    final List<String> mReferenceCategories = new ArrayList<String>();

    public BaseClassifierEvaluator(BaseClassifier<E> classifier,
                                   String[] categories,
                                   boolean storeInputs) {
        mClassifier = classifier;
        mStoreInputs = storeInputs;
        mCategories = categories;
        mCategorySet = new HashSet<String>();
        Collections.addAll(mCategorySet,categories);
        mConfusionMatrix = new ConfusionMatrix(categories);
    }


    public int numCategories() {
        return mCategories.length;
    }

    public String[] categories() {
        return mCategories.clone();
    }

    public BaseClassifier<E> classifier() {
        return mClassifier;
    }

    public void setClassifier(BaseClassifier<E> classifier) {
        setClassifier(classifier,BaseClassifierEvaluator.class);
    }

    public List<Classified<E>> truePositives(String category) {
        return caseTypes(category,true,true);
    }

    public List<Classified<E>> falsePositives(String category) {
        return caseTypes(category,false,true);
    }

    public List<Classified<E>> falseNegatives(String category) {
        return caseTypes(category,true,false);
    }

    public List<Classified<E>> trueNegatives(String category) {
        return caseTypes(category,false,false);
    }

    public void handle(Classified<E> classified) {
        E input = classified.getObject();
        Classification refClassification = classified.getClassification();
        String refCategory = refClassification.bestCategory();
        validateCategory(refCategory);
        Classification classification = mClassifier.classify(input);
        addClassification(refCategory,classification,input);
    }

    public void addClassification(String referenceCategory,
                                  Classification classification,
                                  E input) {
        addClassificationOld(referenceCategory,classification);
        if (mStoreInputs)
            mCases.add(input);
    }

    public int numCases() {
        return mNumCases;
    }

    public ConfusionMatrix confusionMatrix() {
        return mConfusionMatrix;
    }

    public PrecisionRecallEvaluation oneVersusAll(String refCategory) {
        validateCategory(refCategory);
        PrecisionRecallEvaluation prEval = new PrecisionRecallEvaluation();
        int numCases = mReferenceCategories.size();
        for (int i = 0; i < numCases; ++i) {
            Object caseRefCategory = mReferenceCategories.get(i);
            Classification response = mClassifications.get(i);
            Object caseResponseCategory = response.bestCategory();
            boolean inRef = caseRefCategory.equals(refCategory);
            boolean inResp = caseResponseCategory.equals(refCategory);
            prEval.addCase(inRef,inResp);
        }
        return prEval;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        baseToString(sb);

        sb.append("\nONE VERSUS ALL EVALUATIONS BY CATEGORY\n");
        String[] cats = categories();
        for (int i = 0; i < cats.length; ++i) {
            sb.append("\n\nCATEGORY[" + i + "]=" + cats[i] + " VERSUS ALL\n");
            oneVsAllToString(sb,cats[i],i);
        }       
        return sb.toString();
    }

    void baseToString(StringBuilder sb) {
        sb.append("BASE CLASSIFIER EVALUATION\n");
        mConfusionMatrix.toStringGlobal(sb);
    }

    void oneVsAllToString(StringBuilder sb, String category, int i) {
        sb.append("\nFirst-Best Precision/Recall Evaluation\n");
        sb.append(oneVersusAll(category));
        sb.append('\n');
    }


    void setClassifier(BaseClassifier<E> classifier, Class<?> clazz) {
        if (!this.getClass().equals(clazz)) {
            String msg = "Require appropriate classifier type."
                + " Evaluator class=" + this.getClass()
                + " Found classifier.class=" + classifier.getClass();
            throw new IllegalArgumentException(msg);
        }
        mClassifier = classifier;
    }

    private List<Classified<E>> caseTypes(String category, boolean refMatch, boolean respMatch) {
        if (!mStoreInputs) {
            String msg = "Class must store items to return true positives."
                + " Use appropriate constructor flag to store.";
            throw new UnsupportedOperationException(msg);
        }
        List<Classified<E>> result = new ArrayList<Classified<E>>();
        for (int i = 0; i < mReferenceCategories.size(); ++i) {
            String refCat = mReferenceCategories.get(i);
            Classification c = mClassifications.get(i);
            String respCat = c.bestCategory();
            if (category.equals(refCat) != refMatch) continue;
            if (category.equals(respCat) != respMatch) continue;
            Classified<E> classified = new Classified<E>(mCases.get(i),c);
            result.add(classified);
        }
        return result;
    }

    private void addClassificationOld(String referenceCategory,
                                      Classification classification) {

        mConfusionMatrix.increment(referenceCategory,
                                   classification.bestCategory());
        mReferenceCategories.add(referenceCategory);
        mClassifications.add(classification);
        ++mNumCases;
    }

    void validateCategory(String category) {
        if (mCategorySet.contains(category))
            return;
        String msg = "Unknown category=" + category;
        throw new IllegalArgumentException(msg);
    }


}
