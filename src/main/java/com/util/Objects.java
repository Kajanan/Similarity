
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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;



/**
 * Provides several utility methods on objects.
 * 
 * @author Ryan Shaw
 * @created Jul 6, 2009
 * @version 1.0
 * @since JDK1.6
 */
public final class Objects
{
    /**
     * Returns value if it is not null, defaultValue otherwise.
     */
    public static <T> T nullif(T value, T defaultValue) {
        if (value != null) {
            return value;
        }

        return defaultValue;
    }


    public static void dispose(Closeable obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (Throwable e) {
            }
        }
    }


    public static void dispose(Connection obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (Throwable e) {
            }
        }
    }


    public static void dispose(Statement obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (Throwable e) {
            }
        }
    }


    public static void dispose(ResultSet obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (Throwable e) {
            }
        }
    }


    public static BufferedReader getStdin() {
        return new BufferedReader(new InputStreamReader(System.in));
    }


    public static <T> boolean equals(T obj1, T obj2) {
        if (obj1 == obj2) {
            return true;
        }

        if (obj1 == null) {
            return obj2 == null;
        }

        return obj1.equals(obj2);
    }


    public static <T extends Comparable<? super T>> int compares(T obj1, T obj2) {
        if (obj1 != null) {
            return obj1.compareTo(obj2);
        }

        if (obj2 != null) {
            return -obj2.compareTo(obj1);
        }

        return 0;
    }


    public static int hashCode(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

}
