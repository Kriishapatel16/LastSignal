package model;

import java.io.Serializable;
import java.sql.Timestamp;

public class ActionTokenBean implements Serializable {
    private int tokenId;
    private int userId;
    private int checkInId;
    private Timestamp expiresAt;
    private Timestamp usedAt;

    public int getTokenId() { return tokenId; }
    public void setTokenId(int tokenId) { this.tokenId = tokenId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getCheckInId() { return checkInId; }
    public void setCheckInId(int checkInId) { this.checkInId = checkInId; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
    public Timestamp getUsedAt() { return usedAt; }
    public void setUsedAt(Timestamp usedAt) { this.usedAt = usedAt; }
}
