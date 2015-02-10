/* SqlConnectionFactory.java */

package com.sql;

import java.io.File;
import java.io.IOException;


/**
 * @author  Ryan Shaw
 * @created Feb 12, 2010
 */
public class SqlConnectionFactory
{
    public static SqlConnection create(File file) throws IOException {
        Configuration configuration = new Configuration(file);
        return new SqlConnection(configuration.getConnectionString());
    }


    public static SqlConnection create(String file) throws IOException {
        Configuration configuration = new Configuration(new File(file));
        return new SqlConnection(configuration.getConnectionString());
    }

}
