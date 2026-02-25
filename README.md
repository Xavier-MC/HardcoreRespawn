# HardcoreRespawn

![Minecraft 1.21](https://img.shields.io/badge/Minecraft-1.21-green?style=for-the-badge&logo=minecraft)  ![Paper 1.21](https://img.shields.io/badge/Paper-1.21-blue?style=for-the-badge&logo=paper) ![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java) ![GitHub Issues](https://img.shields.io/github/issues/Xavier-MC/HardcoreRespawn?style=for-the-badge) ![GPL](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)

[中文](README.md) | [English](README_EN.md)

**HardcoreRespawn** 是一款专为硬核生存服务器设计的 Bukkit 插件，为核心玩家提供更具挑战性的死亡惩罚机制。插件采用 **一滴血模式** + **复活次数系统** + **死亡等待期** 三重机制，让每一次死亡都充满意义。支持多语言。


### 插件功能

- 玩家最大生命值永久限制为 **1 点生命值**（可配置）
- 复活次数用尽后进入 **24 小时等待期**（可配置）
- 等待期间：
    - 强制观察者模式（可配置）
    - 限制在出生点范围内活动
    - 屏幕中央显示倒计时 BossBar（可配置）
- 新玩家首次加入自动获得 **3 次** 立即复活机会（可配置）
- 死亡时优先消耗次数，有次数则正常复活
- 次数用尽后进入等待期
- 数据基于 UUID 存储，玩家改名不影响
- 击杀特定生物有概率获得复活次数（可配置）
- 支持配置掉落概率和数量
- **在线时间累计奖励**：玩家每在线一定时间可获得复活次数
  - 默认每 24 小时在线时间可获得 1 次复活机会（可配置）
  - 最多可叠加 3 次（可配置）
  - 自动跟踪在线时间并发放奖励
  - 支持查看剩余在线时间需求

---


### 环境要求

| 组件 | 最低版本        | 推荐版本 |
|------|-------------|---------|
| Minecraft | 1.21        | 1.21+ |
| 服务端核心 | Spigot 1.21 | Paper 1.21.3+ |
| Java | 21          | Java 21 LTS |

---

### config.yml 

```yaml
# HardcoreRespawn 配置文件
version: 4.1

settings:
  # 默认复活次数
  default_respawn_count: 3

  # 默认生命值上限
  default_max_health: 1.0

  # 死亡等待时间
  wait_time:
    hours: 24               # 小时
    minutes: 0              # 分钟

  # 一滴血模式
  one_heart:
    enabled: true                    # 是否启用一滴血模式
    speed_reduction: 0.2             # 移动速度降低比例（20%）
    speed_effect_enabled: false      # 是否应用速度降低效果
    particle_effect: false            # 低血量时显示粒子效果
    sound_effect: false               # 低血量时播放心跳音效
    low_health_threshold: 1.0        # 触发低血量效果的阈值（生命值）

  # BossBar 设置
  bossbar:
    enabled: true
    color: RED
    style: SOLID

  # 出生点安全半径（格）
  spawn_radius: 5

  # 死亡等待时的游戏模式(0:survival/2:adventure/3:spectator)
  wait_time_mode: 3

  # 离线时是否继续倒计时
  countdown_offline: true

  # 等待期指令白名单（等待期玩家只能使用这些指令）
  # 注意：如果列表为空，则不限制任何指令（功能关闭）
  # OP 玩家默认不受限制（需要 hardcorerespawn.bypass.commandlimit 权限）
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

  # 在线时间累计复活次数设置
  online_time_reward:
    enabled: true                    # 是否启用在线时间奖励
    offline: false                   # 离线时是否也进行计时
    reward_counts: 1                  # 每次奖励的次数
    required_time:
      hours: 24               # 小时
      minutes: 0              # 分钟
    max_stacks: 3                    # 最多叠加的复活次数

  # AuthMe 支持
  authme_supported: false            # 是否启用 AuthMe 插件支持，启用后未登录玩家将不受限制

# 击杀生物获得次数
rewards:
  enabled: false
  entities:
    WANDERING_TRADER:
      chance: 0.1  # 10% 概率
      count: 1
    PHANTOM:
      chance: 0.2
```

---

### 指令列表

#### 玩家指令

| 指令 | 权限 | 说明 |
|------|------|------|
| `/respawn` | 无 | 显示帮助信息 |
| `/respawn info` | `hardcorerespawn.info` | 查看复活次数、冷却状态和在线时间信息 |
| `/respawn skip` | `hardcorerespawn.skip` | 消耗 1 次机会立即复活（等待期间可用） |

#### 管理员指令

| 指令 | 权限 | 说明 |
|------|------|------|
| `/respawn admin add <玩家> <数量>` | `hardcorerespawn.admin` | 给玩家添加复活次数 |
| `/respawn admin set <玩家> <数量>` | `hardcorerespawn.admin` | 设置玩家复活次数 |
| `/respawn admin reset <玩家>` | `hardcorerespawn.admin` | 重置玩家死亡等待状态 |
| `/respawn admin removehealth <玩家> <数量>` | `hardcorerespawn.admin` | 给玩家减少生命值上限 |
| `/respawn admin sethealth <玩家> <数量>` | `hardcorerespawn.admin` | 设置玩家生命值上限 |
| `/respawn admin addhealth <玩家> <数量>` | `hardcorerespawn.admin` | 给玩家添加生命值上限 |
| `/respawn reload` | `hardcorerespawn.admin` | 重载配置文件 |

可使用/hr /hardcorerespawn 作为指令别名

---

### 数据库结构

插件使用 SQLite 存储玩家数据，数据库文件位于 `plugins/HardcoreRespawn/players.db`


```sql
CREATE TABLE IF NOT EXISTS player_data (
    uuid VARCHAR(36) PRIMARY KEY,
    player_name VARCHAR(16) NOT NULL,
    respawn_count INT DEFAULT 3,
    death_timestamp LONG DEFAULT 0,
    is_waiting BOOLEAN DEFAULT FALSE,
    wait_duration LONG DEFAULT 86400000, -- 24小时默认
    last_login LONG DEFAULT 0,
    total_online_time LONG DEFAULT 0, -- 总在线时间（毫秒）
    last_respawn_recovery LONG DEFAULT 0, -- 上次恢复复活次数的时间戳
    max_health DOUBLE DEFAULT 1.0, -- 生命值上限
    created_at LONG DEFAULT (strftime('%s', 'now')),
    is_new_player BOOLEAN DEFAULT TRUE
);
```

---

### 项目结构

```
HardcoreRespawn/
├── build.gradle
├── src/main/java/xaviermc/top/hardcoreRespawn/
│   ├── HardcoreRespawn.java          # 主类
│   ├── commands/
│   │   └── RespawnCommand.java       # 指令处理
│   ├── listeners/
│   │   ├── CommandListener.java      # 指令限制
│   │   ├── DeathListener.java        # 死亡事件
│   │   ├── MoveListener.java         # 移动限制
│   │   ├── JoinListener.java         # 玩家加入
│   │   ├── EntityKillListener.java   # 实体击杀奖励
│   │   └── LowHealthListener.java    # 低血量效果
│   ├── managers/
│   │   └── PlayerDataManager.java    # 玩家数据管理
│   ├── database/
│   │   └── DatabaseManager.java      # 数据库操作
│   ├── utils/
│   │   ├── MessageUtils.java         # 消息工具
│   │   └── TimeUtils.java            # 时间工具
│   └── models/
│       └── PlayerData.java           # 数据模型
└── src/main/resources/
    ├── plugin.yml
    ├── config.yml
    └── messages.yml

```


### 🐛 问题反馈

遇到问题或有功能建议？欢迎通过以下方式联系：

- 📧 **Issues**: [GitHub Issues](https://github.com/CerealAxis/HardcoreRespawn/issues)
- 💬 **讨论区**: [GitHub Discussions](https://github.com/CerealAxis/HardcoreRespawn/discussions)
- 📮 **邮箱**: CerealAxis@xaviermc.top

**提交 Issue 时请提供：**
1. 服务端版本和插件版本
2. 完整的错误日志
3. 复现步骤
4. 相关配置文件（敏感信息请脱敏）

**Made with ❤️ by CerealAxis**