package xaviermc.top.hardcoreRespawn.models;

import java.util.UUID;

public class PlayerData {
    private UUID uuid;
    private String playerName;
    private int respawnCount;
    private long deathTimestamp;
    private boolean isWaiting;
    private long waitDuration;
    private long lastLogin;
    private boolean isNewPlayer;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.respawnCount = 3; // 默认3次复活机会
        this.deathTimestamp = 0;
        this.isWaiting = false;
        this.waitDuration = 24 * 60 * 60 * 1000; // 24小时默认
        this.lastLogin = System.currentTimeMillis();
        this.isNewPlayer = true;
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getRespawnCount() { return respawnCount; }
    public void setRespawnCount(int respawnCount) { this.respawnCount = respawnCount; }

    public long getDeathTimestamp() { return deathTimestamp; }
    public void setDeathTimestamp(long deathTimestamp) { this.deathTimestamp = deathTimestamp; }

    public boolean isWaiting() { return isWaiting; }
    public void setWaiting(boolean waiting) { isWaiting = waiting; }

    public long getWaitDuration() { return waitDuration; }
    public void setWaitDuration(long waitDuration) { this.waitDuration = waitDuration; }

    public long getLastLogin() { return lastLogin; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }

    public boolean isNewPlayer() { return isNewPlayer; }
    public void setNewPlayer(boolean newPlayer) { isNewPlayer = newPlayer; }

    /**
     * 获取剩余等待时间（毫秒）
     */
    public long getTimeUntilRelease(long currentTime) {
        if (!isWaiting) return 0;
        long elapsed = currentTime - deathTimestamp;
        return Math.max(0, waitDuration - elapsed);
    }
}
