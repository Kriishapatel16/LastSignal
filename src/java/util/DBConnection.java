
package util;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public final class DBConnection {

    private static final String JNDI_NAME = "jdbc/LastSignalDB";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            InitialContext context = new InitialContext();

            DataSource dataSource =
                    (DataSource) context.lookup(JNDI_NAME);

            return dataSource.getConnection();

        } catch (NamingException e) {
            throw new SQLException(
                    "Unable to obtain LastSignal database connection.", e
            );
        }
    }
}
