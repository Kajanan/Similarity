
/**
 * This source code is part of wordster project.
 *
 * This library is free software; you can redistribute it and/or modify it under 
 * the terms of the GNU Lesser General Public License (LGPL) as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more 
 * details.
 */
package com.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Ryan Shaw
 * @created Jul 6, 2009
 * @version 1.0
 * @since JDK1.6
 */
public class Arrays
{
    public static <T> T[] fromList(List<T> list, Class<T> type) {
        if (list == null) {
            throw new NullPointerException("The argument 'list' is null.");
        }
        if (type == null) {
            throw new NullPointerException("The argument 'type' is null.");
        }
        @SuppressWarnings("unchecked")
        T[] arr = (T[]) Array.newInstance(type, list.size());

        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }

        return arr;
    }


    public static <T> T[] fromCollection(Collection<T> list, Class<T> type) {
        if (list == null) {
            throw new NullPointerException("The argument 'list' is null.");
        }
        if (type == null) {
            throw new NullPointerException("The argument 'type' is null.");
        }
        @SuppressWarnings("unchecked")
        T[] arr = (T[]) Array.newInstance(type, list.size());

        int i = 0;
        for (T t : list) {
            arr[i++] = t;
        }

        return arr;
    }


    public static <T> List<T> asList(T[] array) {
        return asList(array, 0, array.length);
    }


    public static <T> List<T> asList(T[] array, int startIndex, int count) {
        if (array == null) {
            throw new NullPointerException("The argument 'array' is null.");
        }
        if (startIndex < 0 || startIndex > array.length) {
            throw new IndexOutOfBoundsException("The argument 'startIndex' is out of bound.");
        }
        if (count < 0) {
            throw new IllegalArgumentException("The argument 'count' is less than zero.");
        }

        int length = Math.min(array.length - startIndex, count);
        List<T> list = new ArrayList<T>(length);

        for (int i = 0; i < length; i++) {
            list.add(array[startIndex + i]);
        }

        return list;
    }


    public static <T> List<List<T>> arrange(List<T> list, int index, int window) {
        return arrange(list, index, window, window);
    }


    public static <T> List<List<T>> arrange(List<T> list, int index, int minWindow, int maxWindow) {
        List<List<T>> result = new ArrayList<List<T>>();

        for (int i = index - minWindow + 1; i <= index; i++) {
            if (i < 0) {
                continue;
            }

            int end = Math.min(i + maxWindow, list.size());
            if (end < i + minWindow) {
                continue;
            }
            
            List<T> lst = new ArrayList<T>(maxWindow);
            for (int j = i; j < end; j++) {
                lst.add(list.get(j));
            }

            result.add(lst);
        }

        return result;
    }

}
