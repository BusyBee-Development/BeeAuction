# BeeAuction

A comprehensive global auction plugin for Minecraft servers that allows players to participate in exciting auctions for items and command rewards.

## Features

- **Global Auction System** - Server-wide auctions that all players can participate in
- **Two Auction Types** - Item-based auctions and command-based auctions
- **Scheduled Auctions** - Automatically run auctions at specific times and days
- **Smart Bidding System** - Anti-snipe protection with automatic time extensions
- **Claim System** - Players can claim items from auctions they won while offline
- **Economy Integration** - Works with any Vault-compatible economy plugin
- **Database Support** - SQLite (default) or MySQL/MariaDB
- **PlaceholderAPI Support** - Full integration for custom displays
- **Security Features** - Command validation, input sanitization, and bid limits
- **bStats Metrics** - Anonymous usage statistics to help improve the plugin

## Requirements

- **Minecraft Version:** 1.21+
- **Server Software:** Paper, Purpur, or any Paper-based fork
- **Dependencies:**
  - Vault (required)
  - Any Vault-compatible economy plugin (e.g., EssentialsX, CMI, etc.)
- **Optional:** PlaceholderAPI for placeholders

## Installation

1. Download the latest version of BeeAuction
2. Place the `.jar` file in your server's `plugins` folder
3. Ensure Vault and an economy plugin are installed
4. Restart your server
5. Configure the plugin in `plugins/BeeAuction/config.yml`
6. Customize messages in `plugins/BeeAuction/messages.yml`

## Commands

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/auction start <type>` | `auction.admin` | Start a manual auction (type: `item` or `command`) |
| `/auction cancel` | `auction.admin` | Cancel the current active auction |
| `/auction reload` | `auction.admin` | Reload configuration files |

**Aliases:** `/ba`

### Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/bid <amount>` | `auction.bid` | Place a bid on the current auction |
| `/claim` | `auction.bid` | Claim items from auctions won while offline |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `auction.admin` | op | Access to all admin commands |
| `auction.bid` | true | Ability to bid on auctions and use /claim |

## Configuration

### Database Configuration

```yaml
database:
  type: sqlite  # Can be "sqlite" or "mysql"
  host: "localhost"
  port: 3306
  database: "beeauction"
  username: "root"
  password: ""
```

**SQLite** is recommended for most servers and requires no additional setup. **MySQL/MariaDB** is recommended for networks or large servers that need centralized data.

### Auction Settings

```yaml
auction:
  bid-time-extension: 30        # Seconds added when bidding near the end
  bid-time-threshold: 60        # Time remaining that triggers extension
  min-bid-amount: 100           # Minimum starting bid
  min-bid-increment: 10         # Minimum increase between bids
  sales-tax-rate: 0.05          # 5% tax on final sale price (player auctions)
  max-bid-amount: 1000000000    # Maximum bid to prevent abuse
```

### Scheduled Auctions

Schedule automatic auctions for specific days and times:

```yaml
schedule:
  enabled: true
  command-validation:
    enabled: true               # Enable command security checks
    whitelist-mode: false       # false = blacklist mode, true = whitelist only
    whitelist:
      - "give"
      - "effect"
      - "eco give"
    blacklist:
      - "op"
      - "deop"
      - "stop"
      - "execute"
  auctions:
    # Item Auction Example
    - day: MONDAY
      time: "20:00"             # 24-hour format (HH:mm)
      duration: 300             # 5 minutes in seconds
      name: "&bSpecial Diamonds Auction"
      reward:
        type: ITEM
        start_price: 500
        item:
          material: DIAMOND
          amount: 3
          name: "&bSpecial Diamonds"
          lore:
            - "&7These diamonds were won in an auction."

    # Command Auction Example
    - day: THURSDAY
      time: "20:30"
      duration: 300
      name: "&cStrength Effect Auction"
      reward:
        type: COMMAND
        start_price: 250
        command: "effect give %player% strength 300 1"
        display-name: "&cStrength II Potion (5:00)"
```

**Supported Days:** MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY

**Time Format:** 24-hour format (HH:mm), e.g., "14:30" for 2:30 PM

### Message Customization

All messages can be customized in `messages.yml`. Messages support:
- Color codes with `&` (e.g., `&a` for green, `&c` for red)
- Placeholders like `{player}`, `{item}`, `{amount}`, `{price}`
- The `{prefix}` placeholder for consistent branding

## How Auctions Work

### For Players

1. **When an auction starts**, a message is broadcast to all players showing:
   - The item or reward being auctioned
   - The starting bid amount
   - How long the auction will run

2. **To bid**, use `/bid <amount>` where the amount must be:
   - At least the current bid + the minimum increment
   - Within your available balance

3. **Anti-Snipe Protection:** If someone bids when less than 60 seconds remain (configurable), the auction time is automatically extended by 30 seconds (configurable).

4. **When you're outbid**, you receive a notification and your previous bid is refunded immediately.

5. **Winning:**
   - **Online:** Items are given to you immediately
   - **Offline:** Items are saved and can be claimed with `/claim` when you log back in

### For Admins

#### Starting a Manual Item Auction

1. Hold the item you want to auction in your hand
2. Run `/auction start item`
3. Follow the prompts:
   - Enter a name for the auction
   - Enter the starting bid price
4. The auction will begin immediately

#### Starting a Manual Command Auction

1. Run `/auction start command`
2. Follow the prompts:
   - Enter a name for the auction
   - Enter the command to run (use `%player%` for the winner's name)
   - Enter a display name for the reward
   - Enter the starting bid price
3. The auction will begin immediately

**Example command:** `effect give %player% speed 600 2`
**Example display name:** `&bSpeed II Potion (10:00)`

#### Cancelling an Auction

Use `/auction cancel` to immediately cancel the active auction. All placed bids will be refunded.

## PlaceholderAPI Support

If PlaceholderAPI is installed, the following placeholders are available:

| Placeholder | Description |
|-------------|-------------|
| `%beeauction_active%` | true/false if auction is active |
| `%beeauction_item%` | Current auction item name |
| `%beeauction_current_bid%` | Current highest bid |
| `%beeauction_bidder%` | Current highest bidder |
| `%beeauction_time_left%` | Time remaining (formatted) |

## Security Features

BeeAuction includes several built-in security measures:

- **Command Validation** - Scheduled command auctions are validated against a whitelist/blacklist to prevent dangerous commands
- **Input Sanitization** - All user inputs are sanitized to prevent injection attacks
- **Bid Limits** - Maximum bid amount prevents economic overflow
- **SQL Injection Protection** - Prepared statements for all database queries
- **Concurrent Access Control** - Thread-safe auction management

## Database Information

### SQLite
- No setup required
- Database file: `plugins/BeeAuction/database.db`
- Recommended for most servers

### MySQL/MariaDB
- Recommended for large servers or networks
- Supports connection pooling with HikariCP
- Configure in `config.yml` under `database` section

## API for Developers

BeeAuction provides an API for developers:

```java
BeeAuctionAPI api = BeeAuctionAPI.getInstance();

// Check if an auction is active
boolean isActive = api.isAuctionActive();

// Get the current auction (if active)
Auction auction = api.getCurrentAuction();

// Access managers
AuctionManager auctionManager = api.getAuctionManager();
BidManager bidManager = api.getBidManager();
```

## Support & Links

- **Issues:** Report bugs and request features on GitHub
- **Version:** 2026.2.0
- **Author:** djtmk (BusyBee Development)

## License

This project is licensed under the ARR License. See the LICENSE file for details.

## Statistics

BeeAuction uses bStats to collect anonymous usage statistics. This helps the developer understand how the plugin is used and improve it. You can opt-out by disabling bStats in your server's `plugins/bStats/config.yml` file.

View statistics at: https://bstats.org/plugin/bukkit/beeauction/29513

---

**Made with 🐝 by BusyBee Development**
