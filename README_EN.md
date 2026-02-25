# HardcoreRespawn

![Minecraft 1.21](https://img.shields.io/badge/Minecraft-1.21-green?style=for-the-badge&logo=minecraft)  ![Paper 1.21](https://img.shields.io/badge/Paper-1.21-blue?style=for-the-badge&logo=paper) ![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java) ![GitHub Issues](https://img.shields.io/github/issues/Xavier-MC/HardcoreRespawn?style=for-the-badge) ![GPL](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)

[中文](README.md) | [English](README_EN.md)

**HardcoreRespawn** is a Bukkit plugin designed specifically for hardcore survival servers, providing core players with more challenging death penalty mechanics. The plugin adopts a triple mechanism of **One-Heart Mode** + **Respawn Count System** + **Death Waiting Period**, making every death meaningful. Multi-language support included.

---

### Plugin Features

- Player maximum health permanently limited to **1 heart** (configurable)
- After exhausting respawn counts, enter a **24-hour waiting period** (configurable)
- During waiting period:
    - Forced spectator mode (configurable)
    - Movement restricted within spawn point radius
    - Countdown BossBar displayed in screen center (configurable)
- New players automatically receive **3** immediate respawn opportunities upon first join (configurable)
- Death prioritizes consuming respawn counts; respawn normally if counts available
- Enter waiting period after exhausting all counts
- Data stored by UUID, player name changes do not affect data
- Chance to obtain respawn counts by killing specific mobs (configurable)
- Configurable drop probability and quantity
- **Online Time Accumulated Rewards**: Players can earn respawn counts by accumulating online time
    - Default: 1 respawn opportunity per 24 hours of online time (configurable)
    - Maximum stack of 3 times (configurable)
    - Automatically tracks online time and distributes rewards
    - Supports viewing remaining online time requirements

---

### Environment Requirements

| Component | Minimum Version | Recommended Version |
|-----------|-----------------|---------------------|
| Minecraft | 1.21 | 1.21+ |
| Server Core | Spigot 1.21 | Paper 1.21.3+ |
| Java | 21 | Java 21 LTS |

---

### config.yml

```yaml
# HardcoreRespawn Configuration File
version: 4.1

settings:
  # Default respawn count
  default_respawn_count: 3

  # Default maximum health
  default_max_health: 1.0

  # Death waiting time
  wait_time:
    hours: 24               # Hours
    minutes: 0              # Minutes

  # One-Heart Mode
  one_heart:
    enabled: true                    # Enable one-heart mode
    speed_reduction: 0.2             # Movement speed reduction ratio (20%)
    speed_effect_enabled: false      # Apply speed reduction effect
    particle_effect: false           # Display particle effects at low health
    sound_effect: false              # Play heartbeat sound at low health
    low_health_threshold: 1.0        # Threshold to trigger low health effects (health points)

  # BossBar Settings
  bossbar:
    enabled: true
    color: RED
    style: SOLID

  # Spawn point safety radius (blocks)
  spawn_radius: 5

  # Game mode during death waiting (0:survival/2:adventure/3:spectator)
  wait_time_mode: 3

  # Continue countdown while offline
  countdown_offline: true

  # Command whitelist during waiting period (players can only use these commands)
  # Note: If list is empty, no command restrictions (feature disabled)
  # OP players are unrestricted by default (requires hardcorerespawn.bypass.commandlimit permission)
  command_whitelist:
    - "msg"
    - "tell"
    - "r"
    - "reply"
    - "respawn"
    - "hr"
    - "hardcorerespawn"
    - "hr skip"
    - "hr info"
    - "respawn skip"
    - "respawn info"
    - "hardcorerespawn skip"
    - "hardcorerespawn info"
    - "l"
    - "login"
    - "reg"
    - "register"

  # Online time accumulated respawn count settings
  online_time_reward:
    enabled: true                    # Enable online time rewards
    offline: false                   # Count time while offline
    reward_counts: 1                 # Respawn counts per reward
    required_time:
      hours: 24                      # Hours
      minutes: 0                     # Minutes
    max_stacks: 3                    # Maximum stacked respawn counts

  # AuthMe Support
  authme_supported: false            # Enable AuthMe plugin support; unrestricted for unlogged players when enabled

# Respawn counts from killing mobs
rewards:
  enabled: false
  entities:
    WANDERING_TRADER:
      chance: 0.1  # 10% probability
      count: 1
    PHANTOM:
      chance: 0.2
```

---

### Command List

#### Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/respawn` | None | Display help information |
| `/respawn info` | `hardcorerespawn.info` | View respawn counts, cooldown status, and online time information |
| `/respawn skip` | `hardcorerespawn.skip` | Consume 1 opportunity to respawn immediately (available during waiting period) |

#### Administrator Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/respawn admin add <player> <count>` | `hardcorerespawn.admin` | Add respawn counts to player |
| `/respawn admin set <player> <count>` | `hardcorerespawn.admin` | Set player respawn counts |
| `/respawn admin reset <player>` | `hardcorerespawn.admin` | Reset player death waiting status |
| `/respawn admin sethealth <player> <amount>` | `hardcorerespawn.admin` | Set player health points |
| `/respawn admin addhealth <player> <amount>` | `hardcorerespawn.admin` | Add health points to player |
| `/respawn admin removehealth <player> <amount>` | `hardcorerespawn.admin` | Remove health points from player |
| `/respawn reload` | `hardcorerespawn.admin` | Reload configuration file |

`/hr` and `/hardcorerespawn` can be used as command aliases

---

### Database Structure

The plugin uses SQLite to store player data. Database file located at `plugins/HardcoreRespawn/players.db`

```sql
CREATE TABLE IF NOT EXISTS player_data (
    uuid VARCHAR(36) PRIMARY KEY,
    player_name VARCHAR(16) NOT NULL,
    respawn_count INT DEFAULT 3,
    death_timestamp LONG DEFAULT 0,
    is_waiting BOOLEAN DEFAULT FALSE,
    wait_duration LONG DEFAULT 86400000, -- 24 hours default
    last_login LONG DEFAULT 0,
    total_online_time LONG DEFAULT 0, -- Total online time (milliseconds)
    last_respawn_recovery LONG DEFAULT 0, -- Timestamp of last respawn count recovery
    max_health DOUBLE DEFAULT 1.0, -- Maximum health
    created_at LONG DEFAULT (strftime('%s', 'now')),
    is_new_player BOOLEAN DEFAULT TRUE
);
```

---

### Project Structure

```
HardcoreRespawn/
├── build.gradle
├── src/main/java/xaviermc/top/hardcoreRespawn/
│   ├── HardcoreRespawn.java          # Main class
│   ├── commands/
│   │   └── RespawnCommand.java       # Command handling
│   ├── listeners/
│   │   ├── CommandListener.java      # Command restrictions
│   │   ├── DeathListener.java        # Death events
│   │   ├── MoveListener.java         # Movement restrictions
│   │   ├── JoinListener.java         # Player join
│   │   ├── EntityKillListener.java   # Entity kill rewards
│   │   └── LowHealthListener.java    # Low health effects
│   ├── managers/
│   │   └── PlayerDataManager.java    # Player data management
│   ├── database/
│   │   └── DatabaseManager.java      # Database operations
│   ├── utils/
│   │   ├── MessageUtils.java         # Message utilities
│   │   └── TimeUtils.java            # Time utilities
│   └── models/
│       └── PlayerData.java           # Data model
└── src/main/resources/
    ├── plugin.yml
    ├── config.yml
    └── messages.yml
```

---

### 🐛 Issue Reporting

Encountering issues or have feature suggestions? Feel free to contact us through the following channels:

- 📧 **Issues**: [GitHub Issues](https://github.com/CerealAxis/HardcoreRespawn/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/CerealAxis/HardcoreRespawn/discussions)
- 📮 **Email**: CerealAxis@xaviermc.top

**When submitting an Issue, please provide:**
1. Server version and plugin version
2. Complete error logs
3. Steps to reproduce
4. Relevant configuration files (please sanitize sensitive information)

**Made with ❤️ by CerealAxis**