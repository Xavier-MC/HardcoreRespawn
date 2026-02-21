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
    private long totalOnlineTime; // 总在线时间（毫秒）
    private long lastOnlineReward; // 上次获得在线时间奖励的时间戳
    private long lastRespawnRecovery; // 上次恢复复活次数的时间戳
    private double maxHealth; // 生命值上限
    private boolean isNewPlayer;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.respawnCount = 3; // 临时默认值，将在加载时被覆盖
        this.deathTimestamp = 0;
        this.isWaiting = false;
        this.waitDuration = 24 * 60 * 60 * 1000; // 临时默认值，将在加载时被覆盖
        this.lastLogin = System.currentTimeMillis();
        this.totalOnlineTime = 0;
        this.lastOnlineReward = System.currentTimeMillis(); // 初始化上次在线奖励时间为当前时间
        this.lastRespawnRecovery = System.currentTimeMillis(); // 初始化上次恢复时间为当前时间
        this.maxHealth = 1.0; // 临时默认值，将在加载时被覆盖
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

    public long getTotalOnlineTime() { return totalOnlineTime; }
    public void setTotalOnlineTime(long totalOnlineTime) { this.totalOnlineTime = totalOnlineTime; }

    public long getLastOnlineReward() { return lastOnlineReward; }
    public void setLastOnlineReward(long lastOnlineReward) { this.lastOnlineReward = lastOnlineReward; }

    public long getLastRespawnRecovery() { return lastRespawnRecovery; }
    public void setLastRespawnRecovery(long lastRespawnRecovery) { this.lastRespawnRecovery = lastRespawnRecovery; }

    public boolean isNewPlayer() { return isNewPlayer; }
    public void setNewPlayer(boolean newPlayer) { isNewPlayer = newPlayer; }

    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }

    /**
     * 获取剩余等待时间（毫秒）
     */
    public long getTimeUntilRelease(long currentTime) {
        if (!isWaiting) return 0;
        long elapsed = currentTime - deathTimestamp;
        return Math.max(0, waitDuration - elapsed);
    }
}