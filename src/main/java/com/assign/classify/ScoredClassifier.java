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

package com.assign.classify;

/**
 * The {@code ScoredClassifier} interface specifies a single method for
 * n-best scored classification.
 * 
 * @author  Bob Carpenter
 * @version 3.9.1
 * @since   LingPipe3.9.1
 * @param <E> the type of objects being classified
 */
public interface ScoredClassifier<E> extends RankedClassifier<E> {

    /**
     * Returns the n-best scored classification for the specified input.
     *
     * @param input Object to classify.
     * @return Classification of object.
     */
    public ScoredClassification classify(E input);

}