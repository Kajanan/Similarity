package com.assign.chunk;

import com.assign.lm.LanguageModel;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AbstractCharLmRescoringChunker<B extends NBestChunker,
                                            O extends LanguageModel.Process,
                                            C extends LanguageModel.Sequence>
    extends RescoringChunker<B> {

    final Map<String,Character> mTypeToChar;
    final Map<String,C> mTypeToLM;

    final O mOutLM;

    final static char UNKNOWN_TYPE_CHAR = 0xFFFF;
    final static char BOS_CHAR = (char)(0xFFFE);
    final static char EOS_CHAR = (char)(BOS_CHAR-1);

    /**
     * Construct a rescoring chunker based on the specified underlying
     * chunker, with the specified number of underlying chunkings
     * rescored, based on the models and type encodings provided in
     * the last three arguments.  See the class documentation for more
     * information on the role of these parameters.
     *
     * @param baseNBestChunker Underlying chunker to rescore.
     * @param numChunkingsRescored Number of underlying chunkings
     * rescored by this chunker.
     * @param outLM The process language model for non-chunks.
     * @param typeToChar A mapping from chunk types to the characters that
     * encode them.
     * @param typeToLM A mapping from chunk types to the language
     * models used to model them.
     */
    public AbstractCharLmRescoringChunker(B baseNBestChunker,
                                          int numChunkingsRescored,
                                          O outLM,
                                          Map<String,Character> typeToChar,
                                          Map<String,C> typeToLM) {
        super(baseNBestChunker,numChunkingsRescored);
        mOutLM = outLM;
        mTypeToChar = typeToChar;
        mTypeToLM = typeToLM;
    }

    /**
     * Returns the character used to encode the specified type
     * in the model.  See the class documentation for more details
     * on the use of this character in the model.
     *
     * @param chunkType Type of chunk.
     * @return The character to code the type in the model.
     * @throws IllegalArgumentException If the specified chunk
     * type does not exist.
     */
    public char typeToChar(String chunkType) {
        Character result = mTypeToChar.get(chunkType);
        if (result == null)
            return UNKNOWN_TYPE_CHAR;
        return result.charValue();
    }

    /**
     * Returns the process language model for non-chunks.  This
     * is the actual language model used, so changes to it affect
     * this chunker.
     *
     * @return The process language model for non-chunks.
     */
    public O outLM() {
        return mOutLM;
    }

    /**
     * Returns the sequence language model for chunks of the
     * specified type.
     *
     * @param chunkType Type of chunk.
     * @return Language model for the specified chunk type.
     */
    public C chunkLM(String chunkType) {
        return mTypeToLM.get(chunkType);
    }

    /**
     * Performs rescoring of the base chunking output using
     * character language models.  See the class documentation
     * above for more information.
     *
     * @param chunking Chunking being rescored.
     * @return New score for chunker.
     */
    @Override
    public double rescore(Chunking chunking) {
        String text = chunking.charSequence().toString();
        double logProb = 0.0;
        int pos = 0;
        char prevTagChar = BOS_CHAR;
        for (Chunk chunk : orderedSet(chunking)) {
            int start = chunk.start();
            int end = chunk.end();
            String chunkType = chunk.type();
            char tagChar = typeToChar(chunkType);
            logProb += outLMEstimate(text.substring(pos,start),
                                     prevTagChar,tagChar);

            if (mTypeToLM.get(chunkType) == null) {
                System.out.println("\nFound null lm for type=" + chunkType
                                   + " Full type set =" + mTypeToLM.keySet());
                System.out.println("Chunking=" + chunking);
            }

            logProb += typeLMEstimate(chunkType,text.substring(start,end));
            pos = end;
            prevTagChar = tagChar;
        }
        logProb += outLMEstimate(text.substring(pos),
                                 prevTagChar,EOS_CHAR);
        return logProb;
    }


    double typeLMEstimate(String type, String text) {
        LanguageModel.Sequence lm = mTypeToLM.get(type);
        if (lm == null) {
            String msg = "Found null lm for type=" + type
                + " Full type set =" + mTypeToLM.keySet();
            System.out.println("TypeLM Estimate:\n" + msg);
            return -16.0 * text.length();
        }
        double estimate = lm.log2Estimate(text);
        return estimate;
    }


    double outLMEstimate(String text,
                         char prevTagChar, char nextTagChar) {
        String seq = prevTagChar + text + nextTagChar;
        String start = seq.substring(0,1);
        double estimate = mOutLM.log2Estimate(seq)
            - mOutLM.log2Estimate(start);
        return estimate;
    }

    static char[] wrapText(String text, char prevTagChar, char nextTagChar) {
        char[] cs = new char[text.length()+2];
        cs[0] = prevTagChar;
        cs[cs.length-1] = nextTagChar;
        for (int i = 0; i < text.length(); ++i)
            cs[i+1] = text.charAt(i);
        return cs;
    }

    static Set<Chunk> orderedSet(Chunking chunking) {
        Set<Chunk> orderedChunkSet = new TreeSet<Chunk>(Chunk.TEXT_ORDER_COMPARATOR);
        orderedChunkSet.addAll(chunking.chunkSet());
        return orderedChunkSet;
    }






}
