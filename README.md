# BeeAuction

A global auction plugin for Minecraft servers that allows players to auction items or commands.

## Features

- Start auctions for items or commands
- Scheduled auctions that run automatically
- Customizable messages and commands
- Integration with Vault for economy support
- Boss bar notifications for active auctions
- SQLite database for storing auction data

## Requirements

- Minecraft 1.21 or higher
- [Vault](https://www.spigotmc.org/resources/vault.34315/) plugin
- An economy plugin that supports Vault (e.g., EssentialsX, iConomy, etc.)

## Installation

1. Download the latest version of BeeAuction from [here](https://github.com/yourusername/BeeAuction/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin by editing the `config.yml` file in the `plugins/BeeAuction` folder

## Commands

### Admin Commands

- `/auction start item <time> <start_price>` - Start an item auction with the item in hand
- `/auction start command <time> <start_price> <command>` - Start a command auction with specified command
- `/auction cancel` - Cancel the active auction
- `/auction reload` - Reload the configuration

### Player Commands

- `/bid <amount>` - Place a bid on the active auction

## Permissions

- `auction.admin` - Allows access to admin commands
- `auction.bid` - Allows bidding on auctions (default: true)

## Configuration

The plugin is highly configurable. You can customize:

- Command names and aliases
- Auction settings (bid time extension, minimum bid amount, etc.)
- Scheduled auctions
- Messages and notifications

See the `config.yml` and `messages.yml` files for all available options.

## Scheduled Auctions

You can set up scheduled auctions that run automatically at specific times. Configure them in the `config.yml` file:

```yaml
schedule:
  enabled: true
  auctions:
    - day: MONDAY
      time: "20:00"
      name: "&bSpecial Diamonds Auction"
      reward:
        type: ITEM
        item:
          material: DIAMOND
          amount: 3
          name: "&bSpecial Diamonds"
          lore:
            - "&7These diamonds were won in an auction"
            - "&7They are very special!"
        start_price: 500
```

## Support

If you encounter any issues or have questions, please open an issue on the [GitHub repository](https://github.com/yourusername/BeeAuction/issues).

## License

This project is licensed under the MIT License - see the LICENSE file for details.