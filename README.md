# SG_RLGL - Squid Game Red Light Green Light

A Minecraft plugin that implements the Red Light Green Light game from Squid Game for Paper/Spigot servers.

## Features

- **Red Light Green Light Game**: Players must stop moving when "Red Light" is called
- **Admin Controls**: Special dyes for admins to control the light state
- **Player Roles**: Admin, Guest, and Winner roles with different permissions
- **Location Management**: Set lobby, guest lobby, and finish line locations
- **Automatic Enforcement**: Lightning strikes and bans for rule violations
- **Configurable Settings**: Customize messages, penalties, and game behavior

## Requirements

- **Server**: Paper 1.21.1+ or Spigot 1.21+
- **Java**: JDK 17 or higher
- **Maven**: 3.6.0 or higher (for building)

## Installation

1. Download the latest release from the releases page
2. Place `SG_RLGL-1.1.0.jar` in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using `/sg` commands

## Building from Source

### Prerequisites

- JDK 17 or higher
- Maven 3.6.0 or higher

### Build Steps

```bash
# Clone the repository
git clone <repository-url>
cd SG_RLGL

# Build the plugin
mvn clean package

# The compiled JAR will be in target/SG_RLGL-1.1.0.jar
```

### Development Build

For development with automatic recompilation:

```bash
# Install and run with file watching
mvn clean compile exec:java -Dexec.mainClass="com.king.sgrlgl.SG_RLGL"
```

## Commands

All commands require OP permissions or the `sgrlgl.admin` permission.

### Setup Commands
- `/sg set lobby` - Set the main game lobby at your current location
- `/sg set guestlobby` - Set the guest lobby at your current location
- `/sg set finish` - Get a special hoe to set the finish line location
- `/sg set rules` - Apply game rules to the current world (disable day/night cycle, weather, etc.)

### Player Management
- `/sg set admin <player>` - Make a player an admin (can control lights)
- `/sg set guest <player>` - Make a player a guest (immune to red light)
- `/sg remove <admin|guest|winner> <player>` - Remove a role from a player

### Game Control
- `/sg game start` - Start the Red Light Green Light game
- `/sg game stop` - Stop the game and remove admin items

### Teleportation
- `/sg tp guest guestlobby` - Teleport all guests to guest lobby
- `/sg tp admin gamelobby` - Teleport all admins to main lobby
- `/sg tp player <guestlobby|gamelobby>` - Teleport yourself to a lobby

### Information
- `/sg info` - Show current game status and configuration
- `/sg help` - Show all available commands

## Game Mechanics

### Light States
- **Green Light**: Players can move freely
- **Red Light**: Players must stop moving completely (including looking around)

### Player Roles
- **Regular Players**: Must follow red light rules or face consequences
- **Admins**: Can control light states using special dyes, immune to red light
- **Guests**: Immune to red light rules (for spectators/staff)
- **Winners**: Players who reached the finish line, immune to red light

### Consequences
When a player moves during red light:
1. Lightning strikes at their location
2. Player is immediately banned with reason "Moved during Red Light"
3. Other players are notified of the elimination

### Admin Controls
Admins receive special dyes when the game starts:
- **Green Dye**: Right-click to set light to green
- **Red Dye**: Right-click to set light to red

## Configuration

The plugin creates a `config.yml` file with the following options:

```yaml
# Game settings
settings:
  lightning-on-violation: true      # Strike lightning when players violate red light
  ban-on-violation: true           # Ban players who move during red light
  broadcast-messages: true         # Broadcast light changes to all players
  title-display-duration: 40       # How long titles stay on screen (ticks)
  title-fade-duration: 5          # Title fade in/out duration (ticks)

# Customize all plugin messages
messages:
  prefix: "&6[SG]&r "
  start: "&aGame Starts! Red Light Green Light begins now!"
  green: "&aGreen Light - You can move!"
  red: "&cRed Light - STOP MOVING!"
  # ... more messages
```

## Permissions

- `sgrlgl.use` - Basic plugin usage (default: OP)
- `sgrlgl.admin` - Administrative commands (default: OP)

## API Usage

Other plugins can interact with SG_RLGL:

```java
// Get the plugin instance
SG_RLGL plugin = (SG_RLGL) Bukkit.getPluginManager().getPlugin("SG_RLGL");
GameManager gameManager = plugin.getGameManager();

// Check game state
boolean isActive = gameManager.isGameActive();
GameManager.LightState state = gameManager.getState();

// Check player roles
boolean isAdmin = gameManager.isAdmin(player.getUniqueId());
boolean isWinner = gameManager.isWinner(player.getUniqueId());

// Control the game
gameManager.startGame();
gameManager.setRed();
```

## Troubleshooting

### Common Issues

1. **Commands not working**: Ensure you have OP permissions or `sgrlgl.admin`
2. **Players not getting banned**: Check that `ban-on-violation` is true in config
3. **Finish line not working**: Make sure you've set the finish location with `/sg set finish`
4. **Admin dyes not working**: Ensure the game is started with `/sg game start`

### Debug Information

Use `/sg info` to check:
- Game active status
- Current light state
- Number of players in each role
- Whether locations are set

## Version History

### 1.1.0 (Current)
- Converted from Gradle to Maven
- Modernized code with Java 17 features
- Improved package structure
- Enhanced error handling and logging
- Added comprehensive JavaDoc documentation
- Updated for Paper 1.21.1

### 1.0.0
- Initial release
- Basic Red Light Green Light functionality
- Admin controls and player roles
- Location management

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, please:
1. Check the troubleshooting section above
2. Review the configuration options
3. Create an issue on the GitHub repository