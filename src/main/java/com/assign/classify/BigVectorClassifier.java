

package com.assign.classify;

import com.assign.matrix.SparseFloatVector;
import com.assign.matrix.Vector;

import com.assign.util.AbstractExternalizable;
import com.assign.util.BoundedPriorityQueue;
import com.assign.util.Scored;
import com.assign.util.ScoredObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

public class BigVectorClassifier
    implements ScoredClassifier<Vector>,
               Serializable {

    static final long serialVersionUID = 5149230080619243511L;

    private final int[] mTermIndexes;
    private final int[] mDocumentIds;
    private final float[] mScores;
    private final String[] mCategories;

    private int mMaxResults;

  
    public BigVectorClassifier(Vector[] termVectors,
                               int maxResults) {
        this(termVectors,categoriesFor(termVectors),maxResults);
    }
    
    public BigVectorClassifier(Vector[] termVectors,
                               String[] categories,
                               int maxResults) {
        mCategories = categories;
        
        mTermIndexes = new int[termVectors.length];
        int size = termVectors.length; 
        for (Vector termVector : termVectors)
            size += termVector.nonZeroDimensions().length;
        mDocumentIds = new int[size];
        mScores = new float[size];
        int pos = 0;
        for (int i = 0; i < termVectors.length; ++i) {
            mTermIndexes[i] = pos;
            Vector termVector = termVectors[i];
            int[] nzDims = termVector.nonZeroDimensions();
            for (int k = 0; k < nzDims.length; ++k) {
                int j = nzDims[k];
                mDocumentIds[pos] = j;
                mScores[pos] = (float) termVector.value(j);
                ++pos;
            }
            mDocumentIds[pos] = -1;
            ++pos;
        }
        setMaxResults(maxResults);

    }

    BigVectorClassifier(int[] termIndexes,
                        int[] documentIds,
                        float[] scores,
                        String[] categories,
                        int maxResults) {
        mTermIndexes = termIndexes;
        mDocumentIds = documentIds;
        mScores = scores;
        setMaxResults(maxResults);
        mCategories = categories;
    }

    static String[] categoriesFor(Vector[] termVectors) {
        int max = 0;
        for (Vector termVector : termVectors) {
            int[] nzDims = termVector.nonZeroDimensions();
            for (int k = 0; k < nzDims.length; ++k)
                max = Math.max(max,nzDims[k]);
        }
        String[] categories = new String[max];
        for (int i = 0; i < categories.length; ++i)
            categories[i] = Integer.toString(i);
        return categories;
    }
    
    public int maxResults() {
        return mMaxResults;
    }

    public void setMaxResults(int maxResults) {
        if (maxResults < 1) {
            String msg = "Max results must be positive."
                + " Found maxResults=" + maxResults;
            throw new IllegalArgumentException(msg);
        }
        mMaxResults = maxResults;
    }

    public ScoredClassification classify(Vector x) {
        int[] nzDims = x.nonZeroDimensions();
        int heapSize = 0; // number dims in range of terms
        for (int k = 0; k < nzDims.length; ++k)
            if (nzDims[k] < mTermIndexes.length)
                ++heapSize;
        int[] current = new int[heapSize];
        float[] vals = new float[heapSize];
        int j = 0;
        for (int k = 0; k < heapSize; ++k) {
            if (nzDims[k] >= mTermIndexes.length)
                continue;
            current[j] = mTermIndexes[nzDims[k]];
            vals[j] = (float) x.value(nzDims[k]);
            ++j;
        }
        for (int k = (heapSize+1)/2; --k >= 0; )
            heapify(k,heapSize,current,vals,mDocumentIds);

        BoundedPriorityQueue<ScoredDoc> queue
            = new BoundedPriorityQueue<ScoredDoc>(ScoredObject.comparator(),
                                                  mMaxResults);
        int[] documentIds = mDocumentIds;
        while (heapSize > 0) {
            // printHeap(heapSize,current,vals,documentIds);
            int doc = documentIds[current[0]];
            // System.out.println("doc=" + doc);
            double score = 0.0;
            while (heapSize > 0 && documentIds[current[0]] == doc) {
                score += vals[0] * mScores[current[0]];
                ++current[0];
                if (documentIds[current[0]] == -1) {
                    --heapSize;
                    if (heapSize > 0) {
                        current[0] = current[heapSize];
                        vals[0] = vals[heapSize];
                    }
                }
                heapify(0,heapSize,current,vals,documentIds);
            }
            queue.offer(new ScoredDoc(doc,score));
        }
        String[] categories = new String[queue.size()];
        double[] scores = new double[queue.size()];
        int pos = 0;
        for (ScoredDoc sd : queue) {
            categories[pos] = Integer.toString(sd.docId());
            scores[pos] = sd.score();
            ++pos;
        }
        return new ScoredClassification(categories,scores);

    }

    Object writeReplace() {
        return new Serializer(this);
    }


    static void heapify(int i, int heapSize,
                        int[] current, float[] vals,
                        int[] documentIds) {
        while (true) {
            int left = 2 * (i+1) - 1;
            if (left >= heapSize)
                return;
            if (documentIds[current[i]] > documentIds[current[left]]) {
                swap(left,i,current);
                swap(left,i,vals);
                i = left;
                continue;
            }
            int right = left+1;
            if (right >= heapSize)
                return;
            if (documentIds[current[i]] > documentIds[current[right]]) {
                swap(right,i,current);
                swap(right,i,vals);
                i = right;
                continue;
            }
            return;
        }
    }

    static void printHeap(int heapSize,
                          int[] current, float[] vals,
                          int[] documentIds) {
        System.out.println("\nHeapSize=" + heapSize);
        for (int i = 0; i < heapSize; ++i)
            System.out.println("i=" + i + " curent=" + current[i] + " vals=" + vals[i]
                               + " docId=" + documentIds[current[i]]);
    }

    static void swap(int i, int j, int[] xs) {
        int tempXsI = xs[i];
        xs[i] = xs[j];
        xs[j] = tempXsI;
    }


    static void swap(int i, int j, float[] xs) {
        float tempXsI = xs[i];
        xs[i] = xs[j];
        xs[j] = tempXsI;
    }


    static class ScoredDoc implements Scored {
        private final int mDocId;
        private final double mScore;
        public ScoredDoc(int docId, double score) {
            mDocId = docId;
            mScore = score;
        }
        public int docId() {
            return mDocId;
        }
        public double score() {
            return mScore;
        }
        public String toString() {
            return mDocId + ":" + mScore;
        }
    }



    static class Serializer extends AbstractExternalizable {
        static final long serialVersionUID = 3954262240692411543L;
        private final BigVectorClassifier mClassifier;
        public Serializer() {
            this(null);
        }
        public Serializer(BigVectorClassifier classifier) {
            mClassifier = classifier;
        }
        @Override
        public void writeExternal(ObjectOutput objOut) throws IOException {
            writeInts(mClassifier.mTermIndexes,objOut);
            writeInts(mClassifier.mDocumentIds,objOut);
            writeFloats(mClassifier.mScores,objOut);
            writeUTFs(mClassifier.mCategories,objOut);
            objOut.writeInt(mClassifier.mMaxResults);
        }
        @Override
        public Object read(ObjectInput objIn)
            throws ClassNotFoundException, IOException {
            int[] termIndexes = readInts(objIn);
            int[] documentIds = readInts(objIn);
            float[] scores = readFloats(objIn);
            String[] categories = readUTFs(objIn);
            int maxResults = objIn.readInt();
            return new BigVectorClassifier(termIndexes,
                                           documentIds,
                                           scores,
                                           categories,
                                           maxResults);
        }
    }





}

