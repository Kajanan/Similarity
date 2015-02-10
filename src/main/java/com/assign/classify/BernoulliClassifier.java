package com.assign.classify;

import com.assign.corpus.ObjectHandler;


import com.assign.stats.MultivariateEstimator;

import com.assign.util.AbstractExternalizable;
import com.assign.util.Counter;
import com.assign.util.FeatureExtractor;
import com.assign.util.ObjectToCounterMap;
import com.assign.util.ObjectToDoubleMap;
import com.assign.util.ScoredObject;
import com.assign.util.Strings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.Serializable;


public class BernoulliClassifier<E>
    implements JointClassifier<E>,
               ObjectHandler<Classified<E>>,
               Serializable {

    static final long serialVersionUID = -7761909693358968780L;

    private final MultivariateEstimator mCategoryDistribution;
    private final FeatureExtractor<E> mFeatureExtractor;
    private final double mActivationThreshold;
    private final Set<String> mFeatureSet;
    private final Map<String,ObjectToCounterMap<String>> mFeatureDistributionMap;

  
    public BernoulliClassifier(FeatureExtractor<E> featureExtractor) {
        this(featureExtractor,0.0);
    }

    public BernoulliClassifier(FeatureExtractor<E> featureExtractor,
                               double featureActivationThreshold) {
        this(new MultivariateEstimator(),
             featureExtractor,
             featureActivationThreshold,
             new HashSet<String>(),
             new HashMap<String,ObjectToCounterMap<String>>());
    }

    BernoulliClassifier(MultivariateEstimator catDistro,
                        FeatureExtractor<E> featureExtractor,
                        double activationThreshold,
                        Set<String> featureSet,
                        Map<String,ObjectToCounterMap<String>> featureDistributionMap) {
        mCategoryDistribution = catDistro;
        mFeatureExtractor = featureExtractor;
        mActivationThreshold = activationThreshold;
        mFeatureSet = featureSet;
        mFeatureDistributionMap = featureDistributionMap;
    }

    public double featureActivationThreshold() {
        return mActivationThreshold;
    }


    public FeatureExtractor<E> featureExtractor() {
        return mFeatureExtractor;
    }

    public String[] categories() {
        String[] categories = new String[mCategoryDistribution.numDimensions()];
        for (int i = 0; i < mCategoryDistribution.numDimensions(); ++i)
            categories[i] = mCategoryDistribution.label(i);
        return categories;
    }


    public void handle(Classified<E> classified) {
        handle(classified.getObject(),
               classified.getClassification());
    }


    void handle(E input, Classification classification) {
        String category = classification.bestCategory();
        mCategoryDistribution.train(category,1L);
        ObjectToCounterMap<String> categoryCounter
            = mFeatureDistributionMap.get(category);
        if (categoryCounter == null) {
            categoryCounter = new ObjectToCounterMap<String>();
            mFeatureDistributionMap.put(category,categoryCounter);
        }

        for (String feature : activeFeatureSet(input)) {
            categoryCounter.increment(feature);
            mFeatureSet.add(feature);
        }
    }

    public JointClassification classify(E input) {
        Set<String> activeFeatureSet = activeFeatureSet(input);
        Set<String> inactiveFeatureSet = new HashSet<String>(mFeatureSet);
        inactiveFeatureSet.removeAll(activeFeatureSet);

        String[] activeFeatures
            = activeFeatureSet.<String>toArray(Strings.EMPTY_STRING_ARRAY);
        String[] inactiveFeatures
            = inactiveFeatureSet.<String>toArray(Strings.EMPTY_STRING_ARRAY);

        ObjectToDoubleMap<String> categoryToLog2P
            = new ObjectToDoubleMap<String>();
        int numCategories = mCategoryDistribution.numDimensions();
        for (long i = 0; i < numCategories; ++i) {
            String category = mCategoryDistribution.label(i);
            double log2P = com.assign.util.Math.log2(mCategoryDistribution.probability(i));

            double categoryCount
                = mCategoryDistribution.getCount(i);

            ObjectToCounterMap<String> categoryFeatureCounts
                = mFeatureDistributionMap.get(category);

            for (String activeFeature : activeFeatures) {
                double featureCount = categoryFeatureCounts.getCount(activeFeature);
                if (featureCount == 0.0) continue; // ignore unknown features
                log2P += com.assign.util.Math.log2((featureCount+1.0) / (categoryCount+2.0));
            }

            for (String inactiveFeature : inactiveFeatures) {
                double notFeatureCount
                    = categoryCount
                    - categoryFeatureCounts.getCount(inactiveFeature);
                log2P += com.assign.util.Math.log2((notFeatureCount + 1.0) / (categoryCount + 2.0));
            }
            categoryToLog2P.set(category,log2P);
        }
        String[] categories = new String[numCategories];
        double[] log2Ps = new double[numCategories];
        List<ScoredObject<String>> scoredObjectList
            = categoryToLog2P.scoredObjectsOrderedByValueList();
        for (int i = 0; i < numCategories; ++i) {
            ScoredObject<String> so = scoredObjectList.get(i);
            categories[i] = so.getObject();
            log2Ps[i] = so.score();
        }
        return new JointClassification(categories,log2Ps);
    }

    Object writeReplace() {
        return new Serializer<E>(this);
    }

    private Set<String> activeFeatureSet(E input) {
        Set<String> activeFeatureSet = new HashSet<String>();
        Map<String,? extends Number> featureMap
            = mFeatureExtractor.features(input);
        for (Map.Entry<String,? extends Number> entry : featureMap.entrySet()) {
            String feature = entry.getKey();
            Number val = entry.getValue();
            if (val.doubleValue() > mActivationThreshold)
                activeFeatureSet.add(feature);
        }
        return activeFeatureSet;
    }

    static class Serializer<F> extends AbstractExternalizable {
        static final long serialVersionUID = 4803666611627400222L;
        final BernoulliClassifier<F> mClassifier;
        public Serializer(BernoulliClassifier<F> classifier) {
            mClassifier = classifier;
        }
        public Serializer() {
            this(null);
        }
        @Override
        public void writeExternal(ObjectOutput objOut) throws IOException {
            objOut.writeObject(mClassifier.mCategoryDistribution);
            objOut.writeObject(mClassifier.mFeatureExtractor);
            objOut.writeDouble(mClassifier.mActivationThreshold);
            objOut.writeInt(mClassifier.mFeatureSet.size());
            for (String feature : mClassifier.mFeatureSet)
                objOut.writeUTF(feature);
            objOut.writeInt(mClassifier.mFeatureDistributionMap.size());
            for (Map.Entry<String,ObjectToCounterMap<String>> entry : mClassifier.mFeatureDistributionMap.entrySet()) {
                objOut.writeUTF(entry.getKey());
                ObjectToCounterMap<String> map = entry.getValue();
                objOut.writeInt(map.size());
                for (Map.Entry<String,Counter> entry2 : map.entrySet()) {
                    objOut.writeUTF(entry2.getKey());
                    objOut.writeInt(entry2.getValue().intValue());
                }
            }
        }
        @Override
        public Object read(ObjectInput objIn)
            throws ClassNotFoundException, IOException {

            MultivariateEstimator estimator 
                = (MultivariateEstimator) objIn.readObject();
            @SuppressWarnings("unchecked")
            FeatureExtractor<F> featureExtractor
                = (FeatureExtractor<F>) objIn.readObject();
            double activationThreshold = objIn.readDouble();
            int featureSetSize = objIn.readInt();
            Set<String> featureSet = new HashSet<String>(2 * featureSetSize);
            for (int i = 0; i < featureSetSize; ++i)
                featureSet.add(objIn.readUTF());
            int featureDistributionMapSize = objIn.readInt();
            Map<String,ObjectToCounterMap<String>> featureDistributionMap
                = new HashMap<String,ObjectToCounterMap<String>>(2*featureDistributionMapSize);
            for (int i = 0; i < featureDistributionMapSize; ++i) {
                String key = objIn.readUTF();
                int mapSize = objIn.readInt();
                ObjectToCounterMap<String> otc = new ObjectToCounterMap<String>();
                featureDistributionMap.put(key,otc);
                for (int j = 0; j < mapSize; ++j) {
                    String key2 = objIn.readUTF();
                    int count = objIn.readInt();
                    otc.set(key2,count);
                }
            }
            return new BernoulliClassifier<F>(estimator,
                                              featureExtractor,
                                              activationThreshold,
                                              featureSet,
                                              featureDistributionMap);
        }
    }


}
