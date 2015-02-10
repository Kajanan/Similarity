/* SqlStatement.java */

package com.sql;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.util.Strings;


/**
 * @author  Ryan Shaw
 * @created Feb 18, 2010
 */
public class SqlStatement
{
    private String template;
    private Object[] parameters;


    public SqlStatement(String template) {
        this.template = template;
        this.parameters = new Object[countParameters(template)];
    }


    private int countParameters(String template) {
        int index = -1;
        int count = 0;
        while ((index = template.indexOf('?', index + 1)) >= 0) {
            count++;
        }

        return count;
    }


    public void setString(String string, int index) {
        this.parameters[index] = string;
    }


    public void setInteger(Integer num, int index) {
        this.parameters[index] = num;
    }


    public void setLong(Long num, int index) {
        this.parameters[index] = num;
    }


    public void setDouble(Double num, int index) {
        this.parameters[index] = num;
    }


    public void setFloat(Float num, int index) {
        this.parameters[index] = num;
    }


    public void setDateTime(Date dateTime, int index) {
        this.parameters[index] = dateTime;
    }


    public void setBytes(byte[] bytes, int index) {
        this.parameters[index] = bytes;
    }


    public void setObject(Object obj, int index) {
        this.parameters[index] = obj;
    }

    private static final String NULL = "NULL";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");


    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(this.estimateSize());
        int paramIndex = 0;

        for (int i = 0; i < template.length(); i++) {
            char ch = template.charAt(i);
            if (ch != '?') {
                buff.append(ch);
                continue;
            }

            Object object = parameters[paramIndex++];
            if (object == null) {
                buff.append(NULL);
            } else if (object instanceof String) {
                String string = (String) object;
                this.appendString(string, buff);
            } else if (object instanceof byte[]) {
                byte[] bytes = (byte[]) object;
                this.appendBytes(bytes, buff);
            } else if (object instanceof Date) {
                Date dateTime = (Date) object;
                String string = DATE_FORMAT.format(dateTime);
                this.appendString(string, buff);
            } else {
                this.appendString(object.toString(), buff);
            }
        }

        return buff.toString();
    }


    private void appendBytes(byte[] bytes, StringBuilder buff) {
        String string = Strings.toHEXString(bytes);
        buff.append('x').append('\'').append(string).append('\'');
    }


    private void appendString(String string, StringBuilder buff) {
        buff.append('\'');
        for (int j = 0; j < string.length(); j++) {
            char jc = string.charAt(j);
            if (jc == '\'') {
                buff.append('\\').append('\'');
            } else if (jc == '\\') {
                buff.append('\\').append('\\');
            } else {
                buff.append(jc);
            }
        }
        buff.append('\'');
    }


    private int estimateSize() {
        int n = 0;
        n += template.length();
        n += parameters.length * 4;

        for (int i = 0; i < parameters.length; i++) {
            Object object = parameters[i];

            if (object == null) {
                n += 4;
            } else if (object instanceof String) {
                String string = (String) object;
                n += string.length() * 1.1;
            } else if (object instanceof byte[]) {
                n += 34;
            } else if (object instanceof Date) {
                n += 24;
            } else {
                n += 32;
            }
        }
        return n;
    }

}
