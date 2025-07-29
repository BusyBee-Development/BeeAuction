# 🐝 BeeAuction

**BeeAuction** is a powerful and flexible auction plugin for Minecraft servers. Run global auctions for items or command rewards, grow your server’s economy, and engage your players with scheduled or on-demand events.

---

## ✨ Features

- **Two Auction Types** — Auction physical items or run auctions for command rewards
- **Scheduled Auctions** — Automatically run auctions on specific days and times
- **Manual Auctions** — Start auctions any time with simple commands
- **Smart Bidding System** — Easy bidding with automatic time extensions
- **Economy Integration** — Fully compatible with [Vault Unlocked](https://modrinth.com/plugin/vaultunlocked)
- **Persistent Storage** — Uses SQLite to store auction data
- **Fully Customizable** — Edit all messages and announcements
- **Admin Controls** — Powerful commands to manage auctions
- **Permissions System** — Control who can start auctions and place bids

---

## 📥 Installation

1. Place the `.jar` file in your server’s `plugins` folder.
2. Install [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked) (required).
3. Make sure you have an economy plugin compatible with Vault Unlocked
4. Restart your server.
5. Configure the plugin using the generated files.

---

## ⚙️ Commands

### Admin Commands

| Command | Description |
|-------------------------------|-------------------------------------------------|
| `/auction start item <time> <start_price>` | Start an item auction with the item in your hand |
| `/auction start command <time> <start_price> <command>` | Start a command auction |
| `/auction cancel` | Cancel the current auction |
| `/auction reload` | Reload the plugin configuration |

### Player Command

| Command | Description |
|-----------------|---------------------------------------|
| `/bid <amount>` | Place a bid on the active auction |

---

## 🗂️ Configuration

BeeAuction is highly configurable:

- Adjust bid time extensions, minimum bids, and more
- Schedule auctions for specific days and times
- Customize messages, command names, and aliases
- Control storage settings





