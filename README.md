# HardcoreRespawn

![Minecraft 1.21](https://img.shields.io/badge/Minecraft-1.21-green?style=for-the-badge&logo=minecraft)  ![Paper 1.21](https://img.shields.io/badge/Paper-1.21-blue?style=for-the-badge&logo=paper) ![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java) ![GitHub Issues](https://img.shields.io/github/issues/Xavier-MC/HardcoreRespawn?style=for-the-badge) ![GPL](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)

**HardcoreRespawn** 是一款专为硬核生存服务器设计的 Bukkit 插件，为核心玩家提供更具挑战性的死亡惩罚机制。插件采用 **一滴血模式** + **复活次数系统** + **死亡等待期** 三重机制，让每一次死亡都充满意义。


### 插件功能

- 玩家最大生命值永久限制为 **1 颗心（2 点生命值）**
- 复活次数用尽后进入 **24 小时等待期**（可配置）
- 等待期间：
    - 强制冒险模式（无法破坏/放置方块）
    - 限制在出生点范围内活动
    - 屏幕中央显示倒计时 BossBar
- 离线期间倒计时继续计算
- 新玩家首次加入自动获得 **3 次** 立即复活机会
- 死亡时优先消耗次数，有次数则正常复活
- 次数用尽后进入等待期
- 数据基于 UUID 存储，玩家改名不影响
- 击杀特定生物有概率获得复活次数
- 支持配置掉落概率和数量

---


### 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Minecraft | 1.21 | 1.21+ |
| 服务端核心 | Paper 1.21 | Paper 1.21.3+ |
| Java | 21 | Java 21 LTS |

---

### config.yml 

```yaml
# HardcoreRespawn 配置文件
version: 1.0

settings:
  # 死亡等待时间（小时）
  wait_time_hours: 24

  # 一滴血模式 - 将玩家最大生命值永久限制为1点生命值
  one_heart:
    enabled: true                    # 是否启用一滴血模式
    speed_reduction: 0.2             # 移动速度降低比例（20%）
    speed_effect_enabled: false      # 是否应用速度降低效果
    particle_effect: true            # 低血量时显示粒子效果
    sound_effect: true               # 低血量时播放心跳音效
    low_health_threshold: 1.0        # 触发低血量效果的阈值（生命值）

  # BossBar 设置
  bossbar:
    enabled: true
    color: RED
    style: SOLID

  # 出生点安全半径（格）
  spawn_radius: 5

  # 离线时是否继续倒计时
  countdown_offline: true

# 未来扩展：击杀生物获得次数
rewards:
  enabled: false
  entities:
    WANDERING_TRADER:
      chance: 0.1  # 10% 概率
      count: 1
    PHANTOM:
      chance: 0.2
      count: 1

# 消息配置
messages:
  respawn_used: "§a你使用了一次复活机会！剩余次数: {count}"
  death_penalty_started: "§c你已经用完了所有复活机会，将在出生点等待24小时才能复活！"
  movement_restricted: "§c你正在等待期，不能离开出生点区域！"
  not_in_waiting_period: "§c你不在等待期！"
  no_respawn_count: "§c你没有足够的复活次数来跳过等待！"
  skip_success: "§a成功跳过等待！剩余次数: {count}"
  waiting_period_ended: "§a你的等待期已结束，现在可以自由行动！"
  info_respawn_count: "§a复活次数: {count}"
  info_waiting_time_left: "§c剩余等待时间: {time}"
  info_not_waiting: "§a你不在等待期"
  bossbar_title: "§c等待复活 §f{time}"
  reward_received: "§a你击杀{entity}获得了{count}次复活机会！"
  data_not_loaded: "§c玩家数据尚未加载完成，请稍后再试！"
  admin_respawn_count_added: "§a管理员给你增加了{amount}次复活机会！当前总数: {total}"
  admin_respawn_count_added_console: "§a已给玩家{player}增加{amount}次复活机会，当前总数: {total}"
  admin_respawn_count_set: "§a管理员将你的复活次数设置为{amount}！"
  admin_respawn_count_set_console: "§a已将玩家{player}的复活次数设置为{amount}"
  admin_reset_player: "§a管理员重置了你的等待状态！"
  admin_reset_player_console: "§a已重置玩家{player}的等待状态"
  player_not_found: "§c找不到该玩家！"
  one_heart_applied: "§c一滴血模式已启用！你的最大生命值为1颗心"
```

---

### 指令列表

#### 玩家指令

| 指令 | 权限 | 说明 |
|------|------|------|
| `/respawn` | 无 | 显示帮助信息 |
| `/respawn info` | `hardcorerespawn.info` | 查看复活次数和冷却状态 |
| `/respawn skip` | `hardcorerespawn.skip` | 消耗 1 次机会立即复活（等待期间可用） |

#### 管理员指令

| 指令 | 权限 | 说明 |
|------|------|------|
| `/respawn admin add <玩家> <数量>` | `hardcorerespawn.admin` | 给玩家添加复活次数 |
| `/respawn admin set <玩家> <数量>` | `hardcorerespawn.admin` | 设置玩家复活次数 |
| `/respawn admin reset <玩家>` | `hardcorerespawn.admin` | 重置玩家死亡等待状态 |
| `/respawn reload` | `hardcorerespawn.admin` | 重载配置文件 |

可使用/hr /hardcorerespawn 作为指令别名

---

### 数据库结构

插件使用 SQLite 存储玩家数据，数据库文件位于 `plugins/HardcoreRespawn/players.db`


```sql
CREATE TABLE player_data (
    uuid VARCHAR(36) PRIMARY KEY,
    player_name VARCHAR(16) NOT NULL,
    respawn_count INT DEFAULT 3,
    death_timestamp LONG DEFAULT 0,
    is_waiting BOOLEAN DEFAULT FALSE,
    wait_duration LONG DEFAULT 86400000,
    last_login LONG DEFAULT 0,
    created_at LONG DEFAULT (strftime('%s', 'now')),
    is_new_player BOOLEAN DEFAULT TRUE
);
```

---

### 项目结构

```
HardcoreRespawn/
├── pom.xml
├── src/main/java/xaviermc/top/hardcoreRespawn/
│   ├── HardcoreRespawn.java          # 主类
│   ├── commands/
│   │   └── RespawnCommand.java       # 指令处理
│   ├── listeners/
│   │   ├── DeathListener.java        # 死亡事件
│   │   ├── MoveListener.java         # 移动限制
│   │   ├── JoinListener.java         # 玩家加入
│   │   ├── EntityKillListener.java   # 实体击杀奖励
│   │   └── LowHealthListener.java    # 低血量效果
│   ├── managers/
│   │   ├── PlayerDataManager.java    # 玩家数据管理
│   │   └── CooldownManager.java      # 冷却时间管理
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