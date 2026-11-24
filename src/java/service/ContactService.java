package service;

import dao.ContactDAO;
import dao.CheckInSettingsDAO;
import model.ContactBean;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ContactService {

    private final ContactDAO contactDAO;
    private final CheckInSettingsDAO settingsDAO = new CheckInSettingsDAO();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+"
                    + "@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$"
            );

    public ContactService() {
        this.contactDAO = new ContactDAO();
    }

    public List<ContactBean> getContacts(int userId)
            throws SQLException {

        validateUserId(userId);

        return contactDAO.getContactsByUserId(userId);
    }

    public boolean addContact(ContactBean contact)
            throws SQLException {

        validateContact(contact);

        return contactDAO.addContact(contact);
    }

    public boolean updateContact(ContactBean contact)
            throws SQLException {

        validateContact(contact);

        if (contact.getContactId() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid contact ID."
            );
        }

        return contactDAO.updateContact(contact);
    }

    public boolean deleteContact(int contactId, int userId)
            throws SQLException {

        validateUserId(userId);

        if (contactId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid contact ID."
            );
        }

        ensureAnotherActiveContactRemains(contactId, userId);
        return contactDAO.deleteContact(contactId, userId);
    }

    public boolean changeContactStatus(int contactId,
                                       int userId,
                                       boolean active)
            throws SQLException {

        validateUserId(userId);

        if (contactId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid contact ID."
            );
        }

        return contactDAO.updateContactStatus(
                contactId, userId, active
        );
    }

    private void ensureAnotherActiveContactRemains(int contactId, int userId)
            throws SQLException {
        var settings = settingsDAO.findByUserId(userId);
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        List<ContactBean> contacts = contactDAO.getContactsByUserId(userId);
        ContactBean target = contacts.stream()
                .filter(contact -> contact.getContactId() == contactId)
                .findFirst().orElse(null);

        if (target != null && target.isActive()) {
            long activeCount = contacts.stream().filter(ContactBean::isActive).count();
            if (activeCount <= 1) {
                throw new IllegalArgumentException(
                        "Monitoring is enabled. Keep at least one active trusted contact, or disable monitoring first.");
            }
        }
    }

    private void validateContact(ContactBean contact) {

        if (contact == null) {
            throw new IllegalArgumentException(
                    "Contact information is required."
            );
        }

        validateUserId(contact.getUserId());

        String name = contact.getContactName();

        if (name == null
                || name.trim().length() < 2
                || name.trim().length() > 100) {

            throw new IllegalArgumentException(
                    "Contact name must contain 2 to 100 characters."
            );
        }

        String email = contact.getEmail();

        if (email == null
                || email.length() > 254
                || !EMAIL_PATTERN.matcher(email.trim()).matches()) {

            throw new IllegalArgumentException(
                    "Please enter a valid email address."
            );
        }

        int priority = contact.getPriorityLevel();

        if (priority < 1 || priority > 3) {
            throw new IllegalArgumentException(
                    "Priority must be between 1 and 3."
            );
        }

        contact.setContactName(name.trim());
        contact.setEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    private void validateUserId(int userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid user session."
            );
        }
    }
}