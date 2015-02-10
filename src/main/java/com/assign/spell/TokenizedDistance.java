/*
 * LingPipe v. 3.9
 * Copyright (C) 2003-2010 Alias-i
 *
 * This program is licensed under the Alias-i Royalty Free License
 * Version 1 WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the Alias-i
 * Royalty Free License Version 1 for more details.
 * 
 * You should have received a copy of the Alias-i Royalty Free License
 * Version 1 along with this program; if not, visit
 * http://alias-i.com/lingpipe/licenses/lingpipe-license-1.txt or contact
 * Alias-i, Inc. at 181 North 11th Street, Suite 401, Brooklyn, NY 11211,
 * +1 (718) 290-9170.
 */
package com.assign.spell;

import java.util.Arrays;
import com.assign.tokenizer.Tokenizer;
import com.assign.tokenizer.TokenizerFactory;

import com.assign.util.Distance;
import com.assign.util.ObjectToCounterMap;
import com.assign.util.Proximity;
import com.assign.util.Strings;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.assign.tokenizer.RegExTokenizerFactory;

/**
 * The <code>TokenizedDistance</code> class provides an underlying
 * implementation of string distance based on comparing sets of
 * tokens.  It holds a tokenizer factory and provides convenience
 * methods for extracting tokens from the input.
 *
 * <p>The method {@link #tokenSet(CharSequence)} provides the set of
 * tokens derived by tokenizing the specified character sequence.  The
 * method {@link #termFrequencyVector(CharSequence)} provides a
 * mapping from tokens extracted by a tokenizer to integer counts.
 *
 * @author  Bob Carpenter
 * @version 4.0.0
 * @since   LingPipe2.4.0
 */
public abstract class TokenizedDistance
        implements Distance<CharSequence>,
        Proximity<CharSequence> {

    /**
     * The underlying tokenizer factory, which is fixed at
     * construction time.
     */
    final TokenizerFactory mTokenizerFactory;

    /**
     * Construct a tokenized distance from the specified tokenizer
     * factory.
     *
     * @param tokenizerFactory Tokenizer for this distance.
     */
    public TokenizedDistance(TokenizerFactory tokenizerFactory) {
        mTokenizerFactory = tokenizerFactory;
    }

    /**
     * Return the tokenizer factory for this tokenized distance.
     *
     * @return This distance's tokenizer factory.
     */
    public TokenizerFactory tokenizerFactory() {
        return mTokenizerFactory;
    }

    /**
     * Return the set of tokens produced by the specified character
     * sequence using the tokenizer for this distance measure.
     *
     * @param cSeq Character sequence to tokenize.
     * @return The token set for the character sequence.
     */
    public Set<String> tokenSet(CharSequence cSeq) {
        char[] cs = Strings.toCharArray(cSeq);
        return tokenSet(cs, 0, cs.length);
    }
    /**
     * Return the set of tokens produced by the specified character
     * slice using the tokenizer for this distance measure.
     *
     * @param cs Underlying array of characters.
     * @param start Index of first character in slice.
     * @param length Length of slice.
     * @return The token set for the character sequence.
     * @throws IndexOutOfBoundsException If the start index is
     * not within the underlying array, or if the start index
     * plus the length minus one is not within the underlying
     * array.
     */
    TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

    public Set<String> tokenSet(char[] cs, int start, int length) {

        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs, 0, cs.length);
        //Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,start,length);
        Set<String> tokenSet = new HashSet<String>();
        String[] tokens = tokenizer.tokenize();
        List<String> tokenList = Arrays.asList(tokens);
        //while ((token = tokenizer.nextToken()) != null)
        for (int i = 0; i < tokenList.size(); i++) {
            tokenSet.add(tokenList.get(i));
        }
        return tokenSet;
    }

    /**
     * Return the mapping from terms to their counts derived from
     * the specified character sequence using the tokenizer factory
     * in th is class.
     *
     * @param cSeq Character sequence to tokenize.
     * @return Counts of tokens in character sequence.
     */
    public ObjectToCounterMap<String> termFrequencyVector(CharSequence cSeq) {
        ObjectToCounterMap<String> termFrequency = new ObjectToCounterMap<String>();
        char[] cs = Strings.toCharArray(cSeq);
        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs, 0, cs.length);
        //String token;
        String[] tokens = tokenizer.tokenize();
        List<String> tokenList = Arrays.asList(tokens);
//        while ((token = tokenizer.nextToken()) != null) {
//            termFrequency.increment(token);
//        }
        for (int i = 0; i < tokenList.size(); i++) {
            termFrequency.increment(tokenList.get(i));
        }
        return termFrequency;
    }
}
