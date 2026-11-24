
package util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DBConnectionTest {

    public static void main(String[] args) {

        try (Connection connection = DBConnection.getConnection()) {

            DatabaseMetaData metadata = connection.getMetaData();

            System.out.println("DATABASE CONNECTION SUCCESSFUL");
            System.out.println("Database: " +
                    metadata.getDatabaseProductName());
            System.out.println("Version: " +
                    metadata.getDatabaseProductVersion());
            System.out.println("URL: " + metadata.getURL());

        } catch (SQLException e) {
            System.err.println("DATABASE CONNECTION FAILED");
            e.printStackTrace();
        }
    }
}
