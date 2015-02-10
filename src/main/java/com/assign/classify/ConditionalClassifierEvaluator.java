
package com.assign.classify;

import com.assign.util.Scored;

import java.util.ArrayList;
import java.util.List;

public class ConditionalClassifierEvaluator<E> extends ScoredClassifierEvaluator<E> {

    private final List<ScoreOutcome>[] mConditionalOutcomeLists;
    boolean mDefectiveConditioning = false;

    public ConditionalClassifierEvaluator(ConditionalClassifier<E> classifier,
                                          String[] categories,
                                          boolean storeInputs) {
        super(classifier,categories,storeInputs);
        @SuppressWarnings({"unchecked","rawtypes"})
        List<ScoreOutcome>[] conditionalOutcomeLists = new ArrayList[numCategories()];
        mConditionalOutcomeLists = conditionalOutcomeLists;
        for (int i = 0; i < mConditionalOutcomeLists.length; ++i)
            mConditionalOutcomeLists[i] = new ArrayList<ScoreOutcome>();
    }

    public void setClassifier(ConditionalClassifier<E> classifier) {
        setClassifier(classifier,ConditionalClassifierEvaluator.class);
    }

   
    @Override
    public ConditionalClassifier<E> classifier() {
        @SuppressWarnings("unchecked")
        ConditionalClassifier<E> result
            = (ConditionalClassifier<E>) super.classifier();
        return result;
    }

    @Override
    public void handle(Classified<E> classified) {
       
        E input = classified.getObject();
        Classification refClassification = classified.getClassification();
        String refCategory = refClassification.bestCategory();
        validateCategory(refCategory);
        ConditionalClassification classification = classifier().classify(input);
        addClassification(refCategory,classification,input);
        addRanking(refCategory,classification);
        addScoring(refCategory,classification);

        
        addConditioning(refCategory,classification);
    }

    public double averageConditionalProbability(String refCategory,
                                                String responseCategory) {
        validateCategory(refCategory);
        validateCategory(responseCategory);
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < mReferenceCategories.size(); ++i) {
            if (mReferenceCategories.get(i).equals(refCategory)) {
                ConditionalClassification c
                    = (ConditionalClassification) mClassifications.get(i);
                for (int rank = 0; rank < c.size(); ++rank) {
                    if (c.category(rank).equals(responseCategory)) {
                        sum += c.conditionalProbability(rank);
                        ++count;
                        break;
                    }
                }
            }
        }
        return sum / (double) count;
    }

    /**
     * Returns the average over all test cases of the conditional
     * probability of the response that matches the reference
     * category.  Better classifiers return higher values for this
     * average.
     *
     * <P>As a normalized value, the average conditional probability
     * always has a sensible interpretation across training instances.
     *
     * @return The average conditional probability of the reference
     * category in the response.
     */
    public double averageConditionalProbabilityReference() {
        double sum = 0.0;
        for (int i = 0; i < mReferenceCategories.size(); ++i) {
            String refCategory = mReferenceCategories.get(i).toString();
            ConditionalClassification c
                = (ConditionalClassification) mClassifications.get(i);
            for (int rank = 0; rank < c.size(); ++rank) {
                if (c.category(rank).equals(refCategory)) {
                    sum += c.conditionalProbability(rank);
                    break;
                }
            }
        }
        return sum / (double) mReferenceCategories.size();
    }

    /**
     * Returns a scored precision-recall evaluation of the
     * classifcation of the specified reference category versus all
     * other categories using the conditional probability scores.
     * This method may only be called for evaluations that have
     * scores.
     *
     * @param refCategory Reference category.
     * @return The conditional one-versus-all precision-recall evaluatuion.
     * @throws IllegalArgumentException If the specified category
     * is unknown.
     */
    public ScoredPrecisionRecallEvaluation
        conditionalOneVersusAll(String refCategory) {

        validateCategory(refCategory);
        return scoredOneVersusAll(mConditionalOutcomeLists,
                                  categoryToIndex(refCategory));
    }

    void addConditioning(String refCategory,
                                 ConditionalClassification scoring) {
        if (scoring.size() < numCategories())
            mDefectiveConditioning = true;
        for (int rank = 0; rank < numCategories() && rank < scoring.size(); ++rank) {
            double score = scoring.conditionalProbability(rank);
            String category = scoring.category(rank);
            int categoryIndex = categoryToIndex(category);
            boolean match = category.equals(refCategory);
            ScoreOutcome outcome = new ScoreOutcome(score,match,rank==0);
            mConditionalOutcomeLists[categoryIndex].add(outcome);
        }
    }

    @Override
    void baseToString(StringBuilder sb) {
        super.baseToString(sb);
        sb.append("Average Conditional Probability Reference="
                  + averageConditionalProbabilityReference() + "\n");
    }

    @Override
    void oneVsAllToString(StringBuilder sb, String category, int i) {
        super.oneVsAllToString(sb,category,i);
        sb.append("Conditional One Versus All\n");
        sb.append(conditionalOneVersusAll(category).toString() + "\n");
        sb.append("Average Conditional Probability Histogram=\n");
        appendCategoryLine(sb);
        for (int j = 0; j < numCategories(); ++j) {
            if (j > 0) sb.append(',');
            sb.append(averageConditionalProbability(category,
                                                    categories()[j]));
        }
        sb.append("\n");
    }

}