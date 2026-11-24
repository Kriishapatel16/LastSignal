package model;

import java.io.Serializable;

public class CheckInSettingsBean implements Serializable {
    private int settingId;
    private int userId;
    private int intervalHours;
    private int graceMinutes;
    private int escalationDelay;
    private boolean enabled;

    public int getSettingId() { return settingId; }
    public void setSettingId(int settingId) { this.settingId = settingId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getIntervalHours() { return intervalHours; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }
    public int getGraceMinutes() { return graceMinutes; }
    public void setGraceMinutes(int graceMinutes) { this.graceMinutes = graceMinutes; }
    public int getEscalationDelay() { return escalationDelay; }
    public void setEscalationDelay(int escalationDelay) { this.escalationDelay = escalationDelay; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
