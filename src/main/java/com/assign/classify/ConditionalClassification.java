

package com.assign.classify;

import com.assign.util.Math;
import com.assign.util.Pair;
import com.assign.util.ScoredObject;

import java.util.Arrays;


public class ConditionalClassification extends ScoredClassification {

    private final double[] mConditionalProbs;

    public ConditionalClassification(String[] categories,
                                     double[] conditionalProbs) {
        this(categories,conditionalProbs,conditionalProbs,TOLERANCE);
    }

    public ConditionalClassification(String[] categories,
                                     double[] scores,
                                     double[] conditionalProbs) {
        this(categories,scores,conditionalProbs,TOLERANCE);
    }

    public ConditionalClassification(String[] categories,
                                     double[] conditionalProbs,
                                     double tolerance) {
        this(categories,conditionalProbs,conditionalProbs,tolerance);
    }

    public ConditionalClassification(String[] categories,
                                     double[] scores,
                                     double[] conditionalProbs,
                                     double tolerance) {
        super(categories,scores);
        mConditionalProbs = conditionalProbs;
        if (tolerance < 0.0 || Double.isNaN(tolerance)) {
            String msg = "Tolerance must be a positive number."
                + " Found tolerance=" + tolerance;
            throw new IllegalArgumentException(msg);
        }
        for (int i = 0; i < conditionalProbs.length; ++i) {
            if (conditionalProbs[i] < 0.0 || conditionalProbs[i] > 1.0) {
                String msg = "Conditional probabilities must be "
                    + " between 0.0 and 1.0."
                    + " Found conditionalProbs[" + i + "]="
                    + conditionalProbs[i];
                throw new IllegalArgumentException(msg);
            }
        }
        double sum = Math.sum(conditionalProbs);
        if (sum < (1.0-tolerance)  || sum > (1.0+tolerance)) {
            String msg = "Conditional probabilities must sum to 1.0."
                + " Acceptable tolerance=" + tolerance
                + " Found sum=" + sum;
            throw new IllegalArgumentException(msg);
        }
    }

    public double conditionalProbability(int rank) {
        if (rank < 0 || rank > (mConditionalProbs.length - 1)) {
            String msg = "Require rank in range 0.."
                + (mConditionalProbs.length-1)
                + " Found rank=" + rank;
            throw new IllegalArgumentException(msg);
        }
        return mConditionalProbs[rank];
    }

    public double conditionalProbability(String category) {
        for(int rank=0;rank<this.size();rank++) {
            if (category(rank).equals(category)) {
                return conditionalProbability(rank);
            }
        }
        String msg = category + " is not a valid category for this classification.  Valid categories are:";
        for(int rank=0;rank<this.size();rank++) {
            msg+=" " + category(rank) + ",";
        }
        msg = msg.substring(0,msg.length()-1);
        throw new IllegalArgumentException(msg);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rank  Category  Score  P(Category|Input)\n");
        for (int i = 0; i < size(); ++i)
            sb.append(i + "=" + category(i) + " " + score(i)
                      + " " + conditionalProbability(i) + '\n');
        return sb.toString();
    }

    private static final double TOLERANCE = 0.01;

    public static ConditionalClassification
        createLogProbs(String[] categories,
                       double[] logProbabilities) {
        verifyLengths(categories,logProbabilities);
        verifyLogProbs(logProbabilities);
        double[] linearProbs = logJointToConditional(logProbabilities);
        Pair<String[],double[]> catsProbs = sort(categories,linearProbs);
        return new ConditionalClassification(catsProbs.a(),
                                             catsProbs.b());
    }
    
    public static ConditionalClassification
        createProbs(String[] categories,
                    double[] probabilityRatios) {
        
        for (int i = 0; i < probabilityRatios.length; ++i) {
            if (probabilityRatios[i] < 0.0 || Double.isInfinite(probabilityRatios[i]) || Double.isNaN(probabilityRatios[i])) {
                String msg = "Probability ratios must be non-negative and finite."
                    + " Found probabilityRatios[" + i + "]=" + probabilityRatios[i];
                throw new IllegalArgumentException(msg);
            }
        }
        if (com.assign.util.Math.sum(probabilityRatios) == 0.0) {
            double[] conditionalProbs = new double[probabilityRatios.length];
            Arrays.fill(conditionalProbs, 1.0/probabilityRatios.length);
            return new ConditionalClassification(categories,conditionalProbs);
        }
            
        double[] logProbs = new double[probabilityRatios.length];
        for (int i = 0; i < probabilityRatios.length; ++i)
            logProbs[i] = com.assign.util.Math.log2(probabilityRatios[i]);
        return createLogProbs(categories,logProbs);
    }

    static void verifyLogProbs(double[] logProbabilities) {
        for (double x : logProbabilities) {
            if (Double.isNaN(x) || (x > 0.0)) {
                String msg = "Log probs must be non-positive numbers."
                    + " Found x=" + x;
                throw new IllegalArgumentException(msg);
            }
        }
    }


    static void verifyLengths(String[] categories, double[] logProbabilities) {
        if (categories.length != logProbabilities.length) {
            String msg = "Arrays must be same length."
                + " Found categories.length=" + categories.length
                + " logProbabilities.length=" + logProbabilities.length;
            throw new IllegalArgumentException(msg);
        }
    }

    static Pair<String[],double[]> sort(String[] categories, double[] vals) {
        verifyLengths(categories,vals);

        @SuppressWarnings({"unchecked","rawtypes"})
        ScoredObject<String>[] scoredObjects
            = (ScoredObject<String>[]) new ScoredObject[categories.length];

        for (int i = 0; i < categories.length; ++i)
            scoredObjects[i] = new ScoredObject<String>(categories[i],vals[i]);

        String[] categoriesSorted = new String[scoredObjects.length];

        double[] valsSorted = new double[categories.length];

        Arrays.sort(scoredObjects,ScoredObject.reverseComparator());
        for (int i = 0; i < scoredObjects.length; ++i) {
            categoriesSorted[i] = scoredObjects[i].getObject();
            valsSorted[i] = scoredObjects[i].score();
        }

        return new Pair<String[],double[]>(categoriesSorted,valsSorted);
    }

    static double[] logJointToConditional(double[] logJointProbs) {
        for (int i = 0; i < logJointProbs.length; ++i) {
            if (logJointProbs[i] > 0.0 && logJointProbs[i] < 0.0000000001)
                logJointProbs[i] = 0.0;
            if (logJointProbs[i] > 0.0 || Double.isNaN(logJointProbs[i])) {
                StringBuilder sb = new StringBuilder();
                sb.append("Joint probs must be zero or negative."
                          + " Found log2JointProbs[" + i + "]=" + logJointProbs[i]);
                for (int k = 0; k < logJointProbs.length; ++k)
                    sb.append("\nlogJointProbs[" + k + "]=" + logJointProbs[k]);
                throw new IllegalArgumentException(sb.toString());
            }
        }
        double max = com.assign.util.Math.maximum(logJointProbs);
        double[] probRatios = new double[logJointProbs.length];
        for (int i = 0; i < logJointProbs.length; ++i) {
            probRatios[i] = java.lang.Math.pow(2.0,logJointProbs[i] - max);  // diff is <= 0.0
            if (probRatios[i] == Double.POSITIVE_INFINITY)
                probRatios[i] = Float.MAX_VALUE;
            else if (probRatios[i] == Double.NEGATIVE_INFINITY || Double.isNaN(probRatios[i]))
                probRatios[i] = 0.0;
        }
        return com.assign.stats.Statistics.normalize(probRatios);
    }





}
