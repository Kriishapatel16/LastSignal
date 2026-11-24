package dao;

import model.ContactBean;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {

    public List<ContactBean> getContactsByUserId(int userId)
            throws SQLException {

        String sql = "SELECT * FROM TRUSTED_CONTACTS "
                + "WHERE USER_ID = ? "
                + "ORDER BY PRIORITY_LEVEL";

        List<ContactBean> contacts = new ArrayList<>();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    contacts.add(mapContact(rs));
                }
            }
        }

        return contacts;
    }

    public boolean addContact(ContactBean contact)
            throws SQLException {

        String sql = "INSERT INTO TRUSTED_CONTACTS "
                + "(USER_ID, CONTACT_NAME, EMAIL, "
                + "PRIORITY_LEVEL, IS_ACTIVE) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, contact.getUserId());
            ps.setString(2, contact.getContactName());
            ps.setString(3, contact.getEmail());
            ps.setInt(4, contact.getPriorityLevel());
            ps.setInt(5, contact.isActive() ? 1 : 0);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateContact(ContactBean contact)
            throws SQLException {

        String sql = "UPDATE TRUSTED_CONTACTS "
                + "SET CONTACT_NAME = ?, EMAIL = ?, "
                + "PRIORITY_LEVEL = ? "
                + "WHERE CONTACT_ID = ? AND USER_ID = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, contact.getContactName());
            ps.setString(2, contact.getEmail());
            ps.setInt(3, contact.getPriorityLevel());
            ps.setInt(4, contact.getContactId());
            ps.setInt(5, contact.getUserId());

            return ps.executeUpdate() == 1;
        }
    } 

public boolean deleteContact(int contactId, int userId)
        throws SQLException {

    String ownershipSql =
            "SELECT CONTACT_ID FROM TRUSTED_CONTACTS "
            + "WHERE CONTACT_ID = ? AND USER_ID = ?";

    String detachSql =
            "UPDATE NOTIFICATION_LOGS "
            + "SET CONTACT_ID = NULL WHERE CONTACT_ID = ?";

    String deleteSql =
            "DELETE FROM TRUSTED_CONTACTS "
            + "WHERE CONTACT_ID = ? AND USER_ID = ?";

    try (Connection con = getConnection()) {

        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            // Verify ownership before modifying notification history.
            boolean owned;

            try (PreparedStatement ps =
                    con.prepareStatement(ownershipSql)) {

                ps.setInt(1, contactId);
                ps.setInt(2, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    owned = rs.next();
                }
            }

            if (!owned) {
                con.rollback();
                return false;
            }

            // Preserve notification records, but remove their contact FK.
            try (PreparedStatement ps =
                    con.prepareStatement(detachSql)) {

                ps.setInt(1, contactId);
                ps.executeUpdate();
            }

            // Delete only the contact belonging to this user.
            int deleted;

            try (PreparedStatement ps =
                    con.prepareStatement(deleteSql)) {

                ps.setInt(1, contactId);
                ps.setInt(2, userId);

                deleted = ps.executeUpdate();
            }

            if (deleted != 1) {
                con.rollback();
                return false;
            }

            con.commit();
            return true;

        } catch (SQLException | RuntimeException ex) {

            con.rollback();
            throw ex;

        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }
}

    public boolean updateContactStatus(int contactId,
                                       int userId,
                                       boolean active)
            throws SQLException {

        String sql = "UPDATE TRUSTED_CONTACTS "
                + "SET IS_ACTIVE = ? "
                + "WHERE CONTACT_ID = ? AND USER_ID = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, contactId);
            ps.setInt(3, userId);

            return ps.executeUpdate() == 1;
        }
    }

    private ContactBean mapContact(ResultSet rs)
            throws SQLException {

        ContactBean contact = new ContactBean();

        contact.setContactId(rs.getInt("CONTACT_ID"));
        contact.setUserId(rs.getInt("USER_ID"));
        contact.setContactName(rs.getString("CONTACT_NAME"));
        contact.setEmail(rs.getString("EMAIL"));
        contact.setPriorityLevel(rs.getInt("PRIORITY_LEVEL"));
        contact.setActive(rs.getInt("IS_ACTIVE") == 1);

        return contact;
    }

    private Connection getConnection() throws SQLException {

        Connection con = DBConnection.getConnection();

        if (con == null) {
            throw new SQLException(
                    "Database connection is unavailable."
            );
        }

        return con;
    }
}