
package service;

import dao.CheckInSettingsDAO;
import dao.ContactDAO;
import java.sql.SQLException;
import java.util.List;
import model.CheckInSettingsBean;
import model.ContactBean;

public class CheckInSettingsService {
    private final CheckInSettingsDAO settingsDAO = new CheckInSettingsDAO();
    private final ContactDAO contactDAO = new ContactDAO();

    public CheckInSettingsBean getSettings(int userId) throws SQLException {
        requireUser(userId);
        CheckInSettingsBean settings = settingsDAO.findByUserId(userId);

        if (settings == null) {
            settings = new CheckInSettingsBean();
            settings.setUserId(userId);
            settings.setIntervalHours(12);
            settings.setGraceMinutes(30);
            settings.setEscalationDelay(30);
            settings.setEnabled(false);
        }

        return settings;
    }

    public void saveSettings(CheckInSettingsBean settings) throws SQLException {
        validate(settings);

        if (settings.isEnabled()) {
            List<ContactBean> contacts =
                    contactDAO.getContactsByUserId(settings.getUserId());

            boolean hasActiveContact =
                    contacts.stream().anyMatch(ContactBean::isActive);

            if (!hasActiveContact) {
                throw new IllegalArgumentException(
                        "Add at least one active trusted contact before enabling monitoring.");
            }
        }

        settingsDAO.saveAndSynchronize(settings);
    }

    private void validate(CheckInSettingsBean settings) {
        if (settings == null || settings.getUserId() <= 0) {
            throw new IllegalArgumentException("Invalid account.");
        }

        if (settings.getIntervalHours() != 1
                && settings.getIntervalHours() != 4
                && settings.getIntervalHours() != 8
                && settings.getIntervalHours() != 12
                && settings.getIntervalHours() != 24
                && settings.getIntervalHours() != 48) {
            throw new IllegalArgumentException("Select a valid check-in interval.");
        }

        if (settings.getGraceMinutes() != 15
                && settings.getGraceMinutes() != 30
                && settings.getGraceMinutes() != 60
                && settings.getGraceMinutes() != 120
                && settings.getGraceMinutes() != 240) {
            throw new IllegalArgumentException("Select a valid grace period.");
        }

        if (settings.getEscalationDelay() != 15
                && settings.getEscalationDelay() != 30
                && settings.getEscalationDelay() != 60
                && settings.getEscalationDelay() != 120) {
            throw new IllegalArgumentException("Select a valid escalation delay.");
        }
    }

    private void requireUser(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid account.");
        }
    }
}
