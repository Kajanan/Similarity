

package com.assign.chunk;

import com.assign.corpus.ObjectHandler;

import com.assign.hmm.HmmCharLmEstimator;

import com.assign.lm.LanguageModel;
import com.assign.lm.NGramBoundaryLM;
import com.assign.lm.NGramProcessLM;

import com.assign.tokenizer.TokenizerFactory;

import com.assign.util.AbstractExternalizable;
import com.assign.util.Compilable;
import com.assign.util.Strings;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.HashMap;
import java.util.Map;


public class CharLmRescoringChunker
    extends AbstractCharLmRescoringChunker<CharLmHmmChunker, 
                                           NGramProcessLM,
                                           NGramBoundaryLM>
    implements ObjectHandler<Chunking>, 
               Compilable {

    final int mNGram;
    final int mNumChars;
    final double mInterpolationRatio;

    char mNextCodeChar = (char) (BOS_CHAR - 2);

   
    public CharLmRescoringChunker(TokenizerFactory tokenizerFactory,
                                  int numChunkingsRescored,
                                  int nGram,
                                  int numChars,
                                  double interpolationRatio) {
        super(new CharLmHmmChunker(tokenizerFactory,
                                   new HmmCharLmEstimator(nGram,
                                                          numChars,
                                                          interpolationRatio)),
              numChunkingsRescored,
              new NGramProcessLM(nGram,numChars,interpolationRatio),
              new HashMap<String,Character>(),
              new HashMap<String,NGramBoundaryLM>());
        mNGram = nGram;
        mNumChars = numChars;
        mInterpolationRatio = interpolationRatio;
    }
    
    public CharLmRescoringChunker(TokenizerFactory tokenizerFactory,
                                  int numChunkingsRescored,
                                  int nGram,
                                  int numChars,
                                  double interpolationRatio,
                                  boolean smoothTags) {
        super(new CharLmHmmChunker(tokenizerFactory,
                                   new HmmCharLmEstimator(nGram,
                                                          numChars,
                                                          interpolationRatio),
                                   smoothTags),
              numChunkingsRescored,
              new NGramProcessLM(nGram,numChars,interpolationRatio),
              new HashMap<String,Character>(),
              new HashMap<String,NGramBoundaryLM>());
        mNGram = nGram;
        mNumChars = numChars;
        mInterpolationRatio = interpolationRatio;
    }

  
    public void handle(Chunking chunking) {
        // train underlying
        ObjectHandler<Chunking> handler2 = baseChunker();
        handler2.handle(chunking);

        // train rescorer
        String text = chunking.charSequence().toString();
        char prevTagChar = BOS_CHAR;
        int pos = 0;
        for (Chunk chunk : orderedSet(chunking)) {
            int start = chunk.start();
            int end = chunk.end();
            String chunkType = chunk.type();
            createTypeIfNecessary(chunkType);
            char tagChar = typeToChar(chunkType);
            trainOutLM(text.substring(pos,start),
                       prevTagChar,tagChar);
            trainTypeLM(chunkType,text.substring(start,end));
            pos = end;
            prevTagChar = tagChar;
        }
        trainOutLM(text.substring(pos),
                   prevTagChar,EOS_CHAR);
    }


    public void compileTo(ObjectOutput objOut) throws IOException {
        objOut.writeObject(new Externalizer(this));
    }

    public void trainDictionary(CharSequence cSeq, String type) {
        baseChunker().trainDictionary(cSeq,type);
        trainTypeLM(type,cSeq);
    }

    
    public void trainOut(CharSequence cSeq) {
        outLM().train(cSeq);
    }


    void createTypeIfNecessary(String chunkType) {
        if (mTypeToChar.containsKey(chunkType)) return;
        Character c = Character.valueOf(mNextCodeChar--);
        mTypeToChar.put(chunkType,c);
        NGramBoundaryLM lm
            = new NGramBoundaryLM(mNGram,mNumChars,mInterpolationRatio,
                                  (char) 0xFFFF);
        mTypeToLM.put(chunkType,lm);
    }


    void trainTypeLM(String type, CharSequence text) {
        createTypeIfNecessary(type);
        NGramBoundaryLM lm = mTypeToLM.get(type);
        lm.train(text);
    }

    void trainOutLM(String text,
                    char prevTagChar, char nextTagChar) {
        String trainSeq = prevTagChar + text + nextTagChar;
        outLM().train(trainSeq);
        outLM().substringCounter().decrementUnigram(prevTagChar);
    }

    static class Externalizer extends AbstractExternalizable {
        private static final long serialVersionUID = 3555143657918695241L;
        final CharLmRescoringChunker mChunker;
        public Externalizer() {
            this(null);
        }
        public Externalizer(CharLmRescoringChunker chunker) {
            mChunker = chunker;
        }
       
        @Override
        public void writeExternal(ObjectOutput objOut) throws IOException {
            mChunker.baseChunker().compileTo(objOut);
            objOut.writeInt(mChunker.numChunkingsRescored());
            String[] types
                = mChunker.mTypeToLM.keySet().<String>toArray(Strings.EMPTY_STRING_ARRAY);
            objOut.writeInt(types.length);
            for (int i = 0; i < types.length; ++i) {
                objOut.writeUTF(types[i]);
                objOut.writeChar(mChunker.typeToChar(types[i]));
                NGramBoundaryLM lm
                    = mChunker.mTypeToLM.get(types[i]);
                lm.compileTo(objOut);
            }
            mChunker.outLM().compileTo(objOut);
        }
        @Override
        public Object read(ObjectInput in)
            throws ClassNotFoundException, IOException {

            NBestChunker baseChunker = (NBestChunker) in.readObject();
            int numChunkingsRescored = in.readInt();
            int numTypes = in.readInt();
            Map<String,Character> typeToChar = new HashMap<String,Character>();
            Map<String,LanguageModel.Sequence> typeToLM = new HashMap<String,LanguageModel.Sequence>();
            for (int i = 0; i < numTypes; ++i) {
                String type = in.readUTF();
                char c = in.readChar();
                LanguageModel.Sequence lm
                    = (LanguageModel.Sequence) in.readObject();
                typeToChar.put(type,Character.valueOf(c));
                typeToLM.put(type,lm);
            }
            LanguageModel.Process outLM
                = (LanguageModel.Process) in.readObject();
            return new AbstractCharLmRescoringChunker<NBestChunker,LanguageModel.Process,LanguageModel.Sequence>(baseChunker,
                                                      numChunkingsRescored,
                                                      outLM,
                                                      typeToChar,
                                                      typeToLM);
        }
    }



}
