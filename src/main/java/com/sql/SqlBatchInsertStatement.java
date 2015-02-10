/* SqlStatementBuilder.java */

package com.sql;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.util.Strings;


/**
 * @author  Ryan Shaw
 * @created Feb 18, 2010
 */
public class SqlBatchInsertStatement
{
    private String tableName;
    private String[] columnNames;
    private boolean ignore;
    private int parameterCount;
    private List<Object[]> batches;
    private Object[] parameters;


    public SqlBatchInsertStatement(String tableName, String[] columnNames) {
        this(tableName, columnNames, columnNames.length, true);
    }


    public SqlBatchInsertStatement(String tableName, String[] columnNames, boolean ignore) {
        this(tableName, columnNames, columnNames.length, ignore);
    }


    public SqlBatchInsertStatement(String tableName, int parameterCount) {
        this(tableName, null, parameterCount, true);
    }


    private SqlBatchInsertStatement(String tableName, String[] columnNames, int parameterCount, boolean ignore) {
        if (parameterCount <= 0) {
            throw new IllegalArgumentException("parameterCount <= 0");
        }

        if (columnNames != null) {
            parameterCount = columnNames.length;
        }

        this.tableName = tableName;
        this.columnNames = columnNames;
        this.ignore = ignore;
        this.parameterCount = parameterCount;
        this.batches = new ArrayList<Object[]>();
    }


    public int getBatchCount() {
        return this.batches.size();
    }


    public void clearBatch() {
        this.batches.clear();
    }


    public void addBatch() {
        this.parameters = new Object[parameterCount];
        this.batches.add(parameters);
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
        StringBuilder buff = new StringBuilder(10240);

        this.constructSQLHeader(buff);
        this.constructSQLBody(buff);

        return buff.toString();
    }


    private void constructSQLHeader(StringBuilder buff) {
        if (ignore) {
            buff.append("INSERT IGNORE INTO ");
        } else {
            buff.append("INSERT INTO ");
        }

        buff.append(tableName);

        if (this.columnNames != null) {
            buff.append('(');
            for (int i = 0; i < columnNames.length; i++) {
                buff.append(columnNames[i]);
                if (i != columnNames.length - 1) {
                    buff.append(',');
                }
            }
            buff.append(')');
        }

        buff.append(" VALUES ");
    }


    private void constructSQLBody(StringBuilder buff) {
        for (Object[] parameters : batches) {
            buff.append('(');

            for (Object object : parameters) {
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

                buff.append(',');
            }

            if (parameters.length > 0) {
                buff.deleteCharAt(buff.length() - 1);
            }

            buff.append(')');
            buff.append(',');
        }

        if (batches.size() > 0) {
            buff.deleteCharAt(buff.length() - 1);
        }
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

}
