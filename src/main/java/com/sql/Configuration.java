/* ConnectionConfiguration.java */

package com.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import com.util.Objects;
import com.util.Strings;


/**
 * Represents a configuration file that contains SQL database connectivity
 * information.
 *
 * This object will be consumed by a ConnectionFactory object.
 *
 * @author  Ryan Shaw
 * @created Aug 12, 2009
 */
public final class Configuration
{
    private String provider;
    private String className;
    private String server;
    private String database;
    private String userID;
    private String password;
    private Map<String, String> otherProperties;
    private String connectionString;


    /**
     * The copy constructor #1.
     */
    public Configuration(Configuration other) {
        if (other == null) {
            throw new NullPointerException("The argument 'other' is null.");
        }
        this.provider = other.provider;
        this.className = other.className;
        this.server = other.server;
        this.database = other.database;
        this.userID = other.userID;
        this.password = other.password;
        this.otherProperties = new HashMap<String, String>(other.otherProperties);   // Shallow copy
        this.connectionString = this.fieldsToString();
    }


    /**
     * Loads the specified configuration file.
     */
    public Configuration(String file) throws IOException {
        this(new File(file));
    }


    /**
     * Loads the specified configuration file.
     */
    public Configuration(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("The argument 'file' is null.");
        }

        FileInputStream fsin = new FileInputStream(file);
        Properties properties = new Properties();

        properties.load(fsin);

        this.otherProperties = new HashMap<String, String>();

        for (String key : properties.stringPropertyNames()) {
            if ("provider".equalsIgnoreCase(key)) {
                this.provider = properties.getProperty(key);
            } else if ("className".equalsIgnoreCase(key)) {
                this.className = properties.getProperty(key);
            } else if ("server".equalsIgnoreCase(key)) {
                this.server = properties.getProperty(key);
            } else if ("database".equalsIgnoreCase(key)) {
                this.database = properties.getProperty(key);
            } else if ("userID".equalsIgnoreCase(key)) {
                this.userID = properties.getProperty(key);
            } else if ("password".equalsIgnoreCase(key)) {
                this.password = properties.getProperty(key);
            } else {
                this.otherProperties.put(key, properties.getProperty(key));
            }
        }

        this.validateFields();
        this.connectionString = this.fieldsToString();

        Objects.dispose(fsin);
    }


    /**
     * Loads the specified configuration file.
     */
    public Configuration(ResourceBundle resource) throws IOException {
        if (resource == null) {
            throw new NullPointerException("The argument 'resource' is null.");
        }

        this.otherProperties = new HashMap<String, String>();

        for (String key : resource.keySet()) {
            if ("provider".equalsIgnoreCase(key)) {
                this.provider = resource.getString(key);
            } else if ("className".equalsIgnoreCase(key)) {
                this.className = resource.getString(key);
            } else if ("server".equalsIgnoreCase(key)) {
                this.server = resource.getString(key);
            } else if ("database".equalsIgnoreCase(key)) {
                this.database = resource.getString(key);
            } else if ("userID".equalsIgnoreCase(key)) {
                this.userID = resource.getString(key);
            } else if ("password".equalsIgnoreCase(key)) {
                this.password = resource.getString(key);
            } else {
                this.otherProperties.put(key, resource.getString(key));
            }
        }

        this.validateFields();
        this.connectionString = this.fieldsToString();
    }


    /**
     * Gets the JDBC representation of the SQL connectivity.
     */
    public String getConnectionString() {
        return this.connectionString;
    }


    public String getProvider() {
        return provider;
    }


    public String getClassName() {
        return className;
    }


    public String getServer() {
        return server;
    }


    public String getDatabase() {
        return database;
    }


    public void setDatabase(String database) {
        if (!Strings.equalsIgnoreCase(this.database, database)) {
            this.database = database;
            this.connectionString = this.fieldsToString();
        }
    }


    public String getUserID() {
        return userID;
    }


    public String getPassword() {
        return password;
    }


    private void validateFields() {
        if (Strings.isNullOrEmpty(className)) {
            throw new IllegalArgumentException("No 'className' is provided.");
        }
        if (Strings.isNullOrEmpty(provider)) {
            throw new IllegalArgumentException("No 'provider' is provided.");
        }
        if (Strings.isNullOrEmpty(server)) {
            throw new IllegalArgumentException("No 'server' is provided.");
        }
        if (Strings.isNullOrEmpty(userID)) {
            throw new IllegalArgumentException("No 'userID' is provided.");
        }
    }


    private String fieldsToString() {
        StringBuilder buff = new StringBuilder();

        buff.append("jdbc").append(':');
        buff.append(provider).append(':');
        buff.append('/').append('/');
        buff.append(server).append('/');
        buff.append(database).append('?');
        buff.append("user=").append(userID).append('&');
        buff.append("password=").append(password);

        for (String key : otherProperties.keySet()) {
            buff.append('&').append(key).append('=').append(otherProperties.get(key));
        }

        return buff.toString();
    }


    @Override
    public String toString() {
        int tmp = connectionString.indexOf('?');
        if (tmp > 0) {
            return connectionString.substring(0, tmp);
        } else {
            return connectionString;
        }
    }

}
