# Naoya Mod

A Minecraft Fabric mod that grants Naoya Zenin's Divine Speed abilities from Jujutsu Kaisen.

## Features

### Three Speed Levels
- **Level 1**: Enhanced speed with 6 shadow frames
- **Level 2**: Supersonic speed with water walking and 12 shadow frames
- **Level 3**: Godspeed with burst ability and 24 shadow frames

### Core Abilities
- **Shadow Frames**: Creates afterimages of the player at previous positions
- **Water Walking**: Run across water surfaces at full speed
- **Sound Barrier**: Visual effects when breaking the sound barrier
- **Burst Mode**: Explosive acceleration with massive damage (Level 3 only)
- **Contact Damage**: Explosive damage on contact with entities

### Gameplay Mechanics
- **Whitelist System**: Players must be added via command to use abilities
- **Level Progression**: Use `/naoya level <player> <1|2|3>` to set ability level
- **Burst Activation**: Hold `R` key while at Level 3 to activate burst mode
- **Contact System**: Touching entities causes explosions and deactivates abilities

## Installation

### For Players
1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.20.1
2. Install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Download the latest `naoya-mod-1.2.1.jar` from [Releases](https://github.com/Fi3w0/Naoya-Mod/releases)
4. Place the JAR file in your `mods` folder

### For Developers
```bash
git clone https://github.com/Fi3w0/Naoya-Mod.git
cd Naoya-Mod
./gradlew build
```

## Commands

### Whitelist Management
- `/naoya whitelist add <player>` - Add player to whitelist
- `/naoya whitelist remove <player>` - Remove player from whitelist
- `/naoya whitelist list` - Show all whitelisted players

### Ability Control
- `/naoya level <player> <1|2|3>` - Set player's ability level
- `/naoya burst <player> <on|off>` - Toggle burst mode (Level 3 only)

## Key Features in Detail

### Shadow Frames (Afterimages)
- **Level 1**: 6 frames (3 seconds of history)
- **Level 2**: 12 frames (6 seconds of history)
- **Level 3**: 24 frames (12 seconds of history)
- Frames fade over time, creating a trail effect

### Water Walking
- Maintains full running speed on water surfaces
- Automatically lifts player to water surface
- Preserves horizontal momentum while preventing sinking

### Contact Damage System
- **Explosion**: Creates particle explosion on entity contact
- **Massive Damage**: Deals 20+ damage based on level
- **Ability Reset**: Deactivates abilities on contact (15-second effective cooldown)
- **Self Immunity**: Player takes no damage from contact explosions

### Burst Mode (Level 3 Only)
- **Activation**: Hold `R` key while moving
- **Effects**: Screen shake, red vignette, explosive trail
- **Damage Bonus**: +15 damage on contact
- **Area Effect**: Knocks back and blinds nearby entities

## Configuration

The mod uses a whitelist system stored in `config/naoya_whitelist.json`. Players must be explicitly added to use abilities.

## Technical Details

- **Minecraft Version**: 1.20.1
- **Mod Loader**: Fabric
- **Java Version**: 17+
- **Dependencies**: Fabric API

### File Structure
```
src/main/java/com/naoyamod/
├── ability/          # Core ability logic
├── command/          # Command implementations
├── effect/           # Visual effects and particles
├── hud/              # HUD rendering
├── mixin/            # Minecraft class modifications
├── network/          # Client-server communication
└── NaoyaMod.java     # Main mod class
```

## Version History

### v1.2.1 (Current)
- Removed "SOUND BARRIER BROKEN" screen text (kept in GUI only)
- Implemented level-based shadow frames (6/12/24)
- Fixed water running to maintain speed on water
- Added contact damage system with explosion and ability reset
- Default whitelist off for all players

### v1.2.0
- Initial release with core abilities
- Three speed levels with progression
- Water walking and sound barrier effects
- Burst mode for Level 3

## Credits

- **Developer**: Fi3w0
- **Inspiration**: Naoya Zenin from Jujutsu Kaisen
- **Mod Loader**: FabricMC
- **Testers**: Community contributors

## License

This mod is licensed under the MIT License. See the LICENSE file for details.

## Support

For issues, feature requests, or questions:
1. Check the [Issues](https://github.com/Fi3w0/Naoya-Mod/issues) page
2. Create a new issue with detailed information
3. Include Minecraft version, mod version, and error logs if applicable