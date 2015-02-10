/* SqlConnection.java */
package com.sql;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.util.Objects;
import com.util.Threads;

/**
 * Provides SQL server access in single threaded fashion.
 * 
 * @author  Ryan Shaw
 * @created Feb 12, 2010
 */
public final class SqlConnection implements Closeable {

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Throwable x) {
            throw new RuntimeException("Bang!@", x);
        }
    }
    /* 
     * The connection string.
     */
    private final String connectionString;
    /*
     * The singleton sql connection instance.
     */
    private Connection connection;
    /* 
     * The singleton sql statement instance.
     */
    private Statement statement;
    /**
     * The synchronized object for multi-thread access
     */
    private Object syncRoot;

    /**
     * The copy constructor.
     */
    public SqlConnection(SqlConnection instance) {
        this(instance.connectionString);
    }

    /**
     * Construct an SqlConnection instance, and set the keep-alive timer in milliseconds.
     * If keepAliveInterval is zero, the keep-alive timer is disabled.
     */
    public SqlConnection(String connectionString) {
        if (connectionString == null) {
            throw new NullPointerException("The argument 'connectionString' is null.");
        }

        this.connectionString = connectionString;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.close();
        } finally {
            super.finalize();
        }
    }

    public synchronized Object getSyncRoot() {
        if (syncRoot == null) {
            syncRoot = new Object();
        }
        return syncRoot;
    }

    /**
     * Gets the Connection object held by current thread. If no such an object
     * exists, it will be created then.
     */
    public synchronized Connection currentConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.closeConnection();
            this.connection = DriverManager.getConnection(connectionString);
        }

        return this.connection;
    }

    /**
     * Gets the Statement object held by current thread. If no such an object
     * exists, it will be created first.
     */
    public synchronized Statement currentStatement() throws SQLException {
        if (this.statement == null || this.statement.isClosed()) {
            this.statement = this.currentConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            this.statement.setFetchSize(Integer.MIN_VALUE);
        }

        return this.statement;
    }

    /**
     * Releases the resources held by the instance.
     */
    public void close() {
        this.closeConnection();
    }

    /**
     * Disposes the Connection object held by the current thread.
     */
    protected void closeConnection() {
        this.closeStatement();

        if (this.connection != null) {
            Objects.dispose(this.connection);
            this.connection = null;
        }
    }

    /**
     * Disposes the Statement object held by the current thread.
     */
    private void closeStatement() {
        if (this.statement != null) {
            Objects.dispose(this.statement);
            this.statement = null;
        }
    }
    public static final int SQL_RETRY_MAX = 5;
    public static final int SQL_RETRY_INTERNAL = 3000;

    /**
     * Executes a light-weight SQL query statement.
     * If the SQL query is to be used frequently, consider using
     * executeQuery(PreparedStatement pst).
     */
    public synchronized ResultSet executeQuery(String sql) throws SQLException {
        int retry = SQL_RETRY_MAX;
        Throwable ex = null;

        while (retry-- > 0) {
            try {

                ResultSet rs = this.currentStatement().executeQuery(sql);
                return rs;
            } catch (Throwable e) {
                this.closeConnection();
                ex = e;

                Threads.sleep(SQL_RETRY_INTERNAL);
                continue;
            }
        }

        if (ex instanceof SQLException) {
            throw (SQLException) ex;
        } else {
            throw new SQLException(ex.getMessage(), ex);
        }
    }

    /**
     * Executes a light-weight and not-frequently-used SQL update statement.
     *
     * If the SQL query is to be used frequently, consider using executeUpdate(PreparedStatement pst).
     */
    public synchronized int executeUpdate(String sql) throws SQLException {
        int retry = SQL_RETRY_MAX;
        Throwable ex = null;

        while (retry-- > 0) {
            try {
                int rs = this.currentStatement().executeUpdate(sql);
                return rs;
            } catch (Throwable e) {
                this.closeConnection();
                ex = e;

                Threads.sleep(SQL_RETRY_INTERNAL);
                continue;
            }
        }

        if (ex instanceof SQLException) {
            throw (SQLException) ex;
        } else {
            throw new SQLException(ex);
        }
    }

    @Override
    public String toString() {
        int index = connectionString.indexOf('?');
        if (index > 0) {
            return connectionString.substring(0, index);
        } else {
            return connectionString;
        }
    }
}


