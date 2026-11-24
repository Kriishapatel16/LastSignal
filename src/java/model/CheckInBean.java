package model;

import java.io.Serializable;
import java.sql.Timestamp;

public class CheckInBean implements Serializable {
    private int checkInId;
    private int userId;
    private Timestamp scheduledAt;
    private Timestamp graceDeadline;
    private Timestamp reminderSentAt;
    private Timestamp confirmedAt;
    private String status;

    public int getCheckInId() { return checkInId; }
    public void setCheckInId(int checkInId) { this.checkInId = checkInId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Timestamp getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Timestamp scheduledAt) { this.scheduledAt = scheduledAt; }
    public Timestamp getGraceDeadline() { return graceDeadline; }
    public void setGraceDeadline(Timestamp graceDeadline) { this.graceDeadline = graceDeadline; }
    public Timestamp getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(Timestamp reminderSentAt) { this.reminderSentAt = reminderSentAt; }
    public Timestamp getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Timestamp confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
