
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * @author Ryan Shaw
 * @created Jun 25, 2009
 * @version 1.0
 * @since JDK1.6
 */
public final class Strings
{
    public static final String EMPTY = "";
    public static final String[] EMPTY_STRINGS = new String[0];
    public static final String NEW_LINE = System.getProperty("line.separator");


    /**
     * Create a new String by repeating a specified char several times.
     */
    public static String newString(char ch, int repeat) {
        if (repeat == 0) {
            return EMPTY;
        }

        char[] tmp = new char[repeat];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = ch;
        }
        return new String(tmp);
    }


    public static boolean isNullOrEmpty(String value) {
        return value == null || value.length() == 0;
    }


    public static boolean isNullOrTrimmedEmpty(String value) {
        if (value == null || value.length() == 0) {
            return true;
        }

        int index = 0;
        while (index < value.length() && Character.isSpaceChar(value.charAt(index))) {
            index++;
        }
        if (index == value.length()) {
            return true;
        }

        return false;
    }


    /**
     * Returns value if it is not null, Strings.EMPTY otherwise.
     */
    public static String nullIf(String value) {
        return nullIf(value, Strings.EMPTY);
    }


    public static String nullIf(String value, String defaultValue) {
        if (value != null) {
            return value;
        }

        if (defaultValue != null) {
            return defaultValue;
        }

        return Strings.EMPTY;
    }


    public static String trim(String value) {
        return value == null ? null : value.trim();
    }


    public static String toLower(String value) {
        return value == null ? null : value.toLowerCase();
    }


    public static String toLowerAndTrim(String value) {
        return value == null ? null : value.toLowerCase().trim();
    }


    public static String toUpper(String value) {
        return value == null ? null : value.toUpperCase();
    }


    /** Restricts a string in a given length. */
    public static String restrict(String value, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }

        if (value == null) {
            return null;
        }

        if (length == 0) {
            return Strings.EMPTY;
        }

        if (value.length() <= length) {
            return value;
        }

        return value.substring(0, length);
    }


    public static boolean containsLetterOrDigit(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetterOrDigit(value.charAt(i))) {
                return true;
            }
        }

        return false;
    }


    public static boolean isNumber(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        int dotCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            switch (ch) {
                case '.':
                    if (++dotCount > 1) {
                        return false;
                    }
                    break;

                case ',':
                    break;

                default:
                    if (!Character.isDigit(ch)) {
                        return false;
                    }
                    break;
            }
        }

        return true;
    }


    public static String normalizeNumber(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        if (!Strings.isNumber(value)) {
            return value;
        }

        StringBuilder buff = new StringBuilder(value.length());

        int dotCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            switch (ch) {
                case '.':
                    if (++dotCount > 1) {
                        return value;
                    } else {
                        buff.append(ch);
                    }
                    break;

                case ',':
                    break;

                default:
                    if (!Character.isDigit(ch)) {
                        return value;
                    } else {
                        buff.append(ch);
                    }
                    break;
            }
        }

        return buff.toString();
    }


    public static boolean isInteger(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }


    public static int tryParseInteger(String value, int defaultValue) {
        if (Strings.isNullOrEmpty(value)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (Throwable e) {
            return defaultValue;
        }
    }


    /** Given a string and parse it as if it contains command line arguments.
     *  This function supports double quotes. */
    public static String[] parseArguments(String cmd) {

        if (Strings.isNullOrEmpty(cmd)) {
            return new String[]{};
        }

        List<String> rsBuff = new ArrayList<String>();
        StringBuilder strBuff = new StringBuilder();

        for (int i = 0; i < cmd.length(); i++) {

            /* Scan to next \" if we see a \" */
            if (cmd.charAt(i) == '\"') {
                for (i++; i < cmd.length() && cmd.charAt(i) != '\"'; i++) {
                    strBuff.append(cmd.charAt(i));
                }

                rsBuff.add(strBuff.toString());
                strBuff.delete(0, strBuff.length());

                i++;
            }

            for (; i < cmd.length() && cmd.charAt(i) != ' '; i++) {
                strBuff.append(cmd.charAt(i));
            }

            if (strBuff.length() > 0) {
                rsBuff.add(strBuff.toString());
                strBuff.delete(0, strBuff.length());
            }
        }

        if (strBuff.length() > 0) {
            rsBuff.add(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }

        String[] result = Arrays.fromList(rsBuff, String.class);

        return result;
    }


    public static byte[] getMD5Hash(String text) {
        Charset charset = Charset.forName("UTF-8");
        return getMD5Hash(text, charset);
    }


    public static byte[] getMD5Hash(String text, Charset charset) {
        if (text == null) {
            throw new NullPointerException("The argument 'text' is null.");
        }
        if (charset == null) {
            throw new NullPointerException("The argument 'charset' is null.");
        }

        MessageDigest md = getMD5();

        synchronized (md) {
            byte[] bytes = text.getBytes(charset);
            md.update(bytes, 0, bytes.length);
            byte[] md5hash = md.digest();

            return md5hash;
        }
    }


    public static String toHEXString(byte[] bytes) {
        String rs = new BigInteger(1, bytes).toString(16).toUpperCase();
        if (rs.length() < 32) {
            String affix = newString('0', 32 - rs.length());

            rs = affix + rs;
        }
        return rs;
    }


    private static MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean equals(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        }

        if (str2 == null) {
            return str1 == null;
        }

        return str1.equals(str2);
    }


    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        }

        if (str2 == null) {
            return str1 == null;
        }

        return str1.equalsIgnoreCase(str2);
    }


    public static int compare(String s1, String s2) {
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        }

        if (s2 == null) {
            return s1 == null ? 0 : 1;
        }

        return s1.compareTo(s2);
    }


    public static int compareIgnoreCase(String s1, String s2) {
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        }

        if (s2 == null) {
            return s1 == null ? 0 : 1;
        }

        return s1.compareToIgnoreCase(s2);
    }


    public static <T> String join(T[] array) {
        return Strings.join(array, ", ");
    }


    public static <T> String join(T[] array, String separater) {
        if (array == null) {
            throw new NullPointerException("The argument 'array' is null.");
        }

        StringBuilder buff = new StringBuilder(Strings.EMPTY);

        if (separater == null) {
            separater = Strings.EMPTY;
        }

        for (T t : array) {
            buff.append(t);
            buff.append(separater);
        }

        if (buff.length() > separater.length()) {
            buff.delete(buff.length() - separater.length(), buff.length());
        }

        return buff.toString();
    }


    public static <T> String join(Collection<T> collection) {
        return Strings.join(collection, ", ");
    }


    public static <T> String join(Collection<T> collection, String separater) {
        if (collection == null) {
            throw new NullPointerException("The argument 'collection' is null.");
        }

        if (separater == null) {
            separater = Strings.EMPTY;
        }

        StringBuilder buff = new StringBuilder(Strings.EMPTY);

        for (T t : collection) {
            buff.append(t);
            buff.append(separater);
        }

        if (buff.length() > separater.length()) {
            buff.delete(buff.length() - separater.length(), buff.length());
        }

        return buff.toString();
    }


    public static <T, K> String join(Map<T, K> dict) {
        return Strings.join(dict, ": ", ", ");
    }


    public static <T, K> String join(Map<T, K> dict, String divider, String separater) {
        if (dict == null) {
            throw new NullPointerException("The argument 'dict' is null.");
        }

        if (separater == null) {
            separater = Strings.EMPTY;
        }
        if (divider == null) {
            divider = Strings.EMPTY;
        }

        StringBuilder buff = new StringBuilder(Strings.EMPTY);

        for (T key : dict.keySet()) {
            K obj = dict.get(key);

            buff.append(key).append(divider).append(obj).append(separater);
        }

        if (buff.length() > separater.length()) {
            buff.delete(buff.length() - separater.length(), buff.length());
        }

        return buff.toString();
    }


    public static String toString(Throwable error) {
        if (error == null) {
            return Strings.EMPTY;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);

        error.printStackTrace(pw);

        pw.flush();
        sw.flush();

        String output = sw.toString();

        Objects.dispose(sw);
        Objects.dispose(pw);

        return output;
    }


    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }

        return obj.toString();
    }


    public static String trimNonLetterOrDigit(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }

        value = Strings.leftTrimNonLetterOrDigit(value);
        value = Strings.rightTrimNonLetterOrDigit(value);

        return value;
    }


    public static String leftTrimNonLetterOrDigit(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        int i = 0;
        while (i < value.length() && !Character.isLetterOrDigit(value.charAt(i))) {
            i++;
        }

        return value.substring(i);
    }


    public static String rightTrimNonLetterOrDigit(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        int i = value.length() - 1;
        while (i >= 0 && !Character.isLetterOrDigit(value.charAt(i))) {
            i--;
        }

        return value.substring(0, i + 1);
    }


    public static String trimChar(String value, char ch) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }

        value = Strings.leftTrimChar(value, ch);
        value = Strings.rightTrimChar(value, ch);

        return value;
    }


    public static String leftTrimChar(String value, char ch) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        int i = 0;
        while (i < value.length() && value.charAt(i) == ch) {
            i++;
        }

        return value.substring(i);
    }


    public static String rightTrimChar(String value, char ch) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        int i = value.length() - 1;
        while (i >= 0 && value.charAt(i) == ch) {
            i--;
        }

        return value.substring(0, i + 1);
    }


    public static boolean startWith(String value, String prefix) {
        if (value == null) {
            return false;
        }

        return value.startsWith(prefix);
    }


    public static boolean isFirstLetterUpperCase(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        return Character.isUpperCase(value.charAt(0));
    }


    public static boolean isFirstCharacterLetter(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        return Character.isLetter(value.charAt(0));
    }


    public static boolean isLastCharacterLetter(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        return Character.isLetter(value.charAt(value.length() - 1));
    }


    public static boolean isAllLetterUpperCase(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (!Character.isUpperCase(ch) || ch == '.') {
                return false;
            }
        }

        return true;
    }

    public static int numberOfUpperCaseLetter(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return 0;
        }

        int count = 0;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (Character.isUpperCase(ch)) {
                count++;
            }
        }
        return count;
    }


    public static String displayMemory(long memory) {
        if (memory == 0) {
            return "0 KB";
        }

        long mem = 0;

        if ((mem = memory / 1024) == 0) {
            return memory + " B";
        } else {
            memory = mem;
        }

        if ((mem = memory / 1024) == 0) {
            return memory + " KB";
        } else {
            memory = mem;
        }

        if ((mem = memory / 1024) == 0) {
            return memory + " MB";
        } else {
            memory = mem;
        }

        if ((mem = memory / 1024) == 0) {
            return memory + " GB";
        } else {
            memory = mem;
        }

        mem = memory / 1024;
        return memory + " TB";
    }

}
