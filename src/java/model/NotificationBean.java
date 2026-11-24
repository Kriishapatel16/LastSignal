package model;

import java.io.Serializable;
import java.sql.Timestamp;

public class NotificationBean implements Serializable {
    private int notificationId;
    private int checkInId;
    private int contactId;
    private String recipientEmail;
    private String notificationType;
    private int escalationLevel;
    private String eventKey;
    private Timestamp sentAt;
    private String deliveryStatus;
    private String errorMessage;

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int value) { notificationId = value; }
    public int getCheckInId() { return checkInId; }
    public void setCheckInId(int value) { checkInId = value; }
    public int getContactId() { return contactId; }
    public void setContactId(int value) { contactId = value; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String value) { recipientEmail = value; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String value) { notificationType = value; }
    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int value) { escalationLevel = value; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String value) { eventKey = value; }
    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp value) { sentAt = value; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String value) { deliveryStatus = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
}
