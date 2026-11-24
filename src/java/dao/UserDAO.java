package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import model.UserBean;
import util.DBConnection;

/** Database access for the USERS table. All values are bound parameters. */
public final class UserDAO {

    private static final String USER_COLUMNS =
            "USER_ID, NAME, EMAIL, PASSWORD_HASH, PHONE, \"ROLE\", STATUS, CREATED_AT";

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT USER_ID FROM USERS WHERE LOWER(EMAIL) = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /** Inserts one account and returns its generated USER_ID. */
    public int registerUser(UserBean user) throws SQLException {
        final String insertSql = "INSERT INTO USERS "
                + "(NAME, EMAIL, PASSWORD_HASH, PHONE, \"ROLE\", STATUS, CREATED_AT) "
                + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        final String idSql = "SELECT USER_ID FROM USERS WHERE LOWER(EMAIL) = ?";

        try (Connection connection = DBConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    insert.setString(1, user.getName());
                    insert.setString(2, normalizeEmail(user.getEmail()));
                    insert.setString(3, user.getPasswordHash());
                    insert.setString(4, user.getPhone() == null ? "" : user.getPhone());
                    // Public registration can never assign an administrative role.
                    insert.setString(5, "USER");
                    insert.setString(6, "ACTIVE");
                    insert.executeUpdate();
                }

                int userId;
                try (PreparedStatement findId = connection.prepareStatement(idSql)) {
                    findId.setString(1, normalizeEmail(user.getEmail()));
                    try (ResultSet result = findId.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("The new account ID could not be retrieved.");
                        }
                        userId = result.getInt("USER_ID");
                    }
                }

                connection.commit();
                return userId;
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // The connection is closed by try-with-resources.
                }
            }
        }
    }

    public UserBean findByEmail(String email) throws SQLException {
        String sql = "SELECT " + USER_COLUMNS + " FROM USERS WHERE LOWER(EMAIL) = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapUser(result) : null;
            }
        }
    }

    public UserBean findById(int userId) throws SQLException {
        String sql = "SELECT " + USER_COLUMNS + " FROM USERS WHERE USER_ID = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapUser(result) : null;
            }
        }
    }


    public java.util.List<UserBean> findAllUsers() throws SQLException {
        String sql = "SELECT USER_ID, NAME, EMAIL, PHONE, \"ROLE\", STATUS, CREATED_AT "
                + "FROM USERS ORDER BY CREATED_AT DESC";
        java.util.List<UserBean> users = new java.util.ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                UserBean user = new UserBean();
                user.setUserId(result.getInt("USER_ID"));
                user.setName(result.getString("NAME"));
                user.setEmail(result.getString("EMAIL"));
                user.setPhone(result.getString("PHONE"));
                user.setRole(result.getString("ROLE"));
                user.setStatus(result.getString("STATUS"));
                user.setCreatedAt(result.getTimestamp("CREATED_AT"));
                users.add(user);
            }
        }
        return users;
    }

    public boolean updateAccountStatus(int userId, String status) throws SQLException {
        if (userId <= 0 || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))) {
            throw new IllegalArgumentException("Invalid account status.");
        }
        String sql = "UPDATE USERS SET STATUS = ? WHERE USER_ID = ? AND \"ROLE\" <> 'ADMIN'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) AS TOTAL FROM USERS WHERE \"ROLE\" <> 'ADMIN'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt("TOTAL");
        }
    }

    private UserBean mapUser(ResultSet result) throws SQLException {
        UserBean user = new UserBean();
        user.setUserId(result.getInt("USER_ID"));
        user.setName(result.getString("NAME"));
        user.setEmail(result.getString("EMAIL"));
        user.setPasswordHash(result.getString("PASSWORD_HASH"));
        user.setPhone(result.getString("PHONE"));
        user.setRole(result.getString("ROLE"));
        user.setStatus(result.getString("STATUS"));
        user.setCreatedAt(result.getTimestamp("CREATED_AT"));
        return user;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
