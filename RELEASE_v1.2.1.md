# Naoya Mod v1.2.1 Release Notes

## 🚀 Overview
Naoya Mod v1.2.1 brings significant improvements to the Divine Speed abilities from Jujutsu Kaisen. This update focuses on gameplay balance, visual enhancements, and new mechanics for a more authentic Naoya Zenin experience.

## 📦 Download
- **File**: `naoya-mod-1.2.1.jar`
- **Size**: 142 KB
- **Minecraft Version**: 1.20.1
- **Fabric API Required**: Yes

## ✨ New Features & Changes

### 🎯 Level-Based Shadow Frames
- **Level 1**: 6 shadow frames (3 seconds of history)
- **Level 2**: 12 shadow frames (6 seconds of history)  
- **Level 3**: 24 shadow frames (12 seconds of history)
- Creates more impressive afterimage trails at higher levels

### 💥 Contact Damage System
- **Explosion Effects**: Particle explosions on entity contact
- **Massive Damage**: 20+ damage based on ability level
- **Ability Reset**: Deactivates abilities on contact (15-second effective cooldown)
- **Self Immunity**: Player takes no damage from contact explosions

### 🌊 Improved Water Running
- **Maintained Speed**: Full running speed preserved on water surfaces
- **Natural Movement**: Horizontal momentum conserved while preventing sinking
- **Surface Detection**: Automatic water surface positioning

### 🎨 Visual Improvements
- **Removed Screen Text**: "SOUND BARRIER BROKEN" text removed from screen center
- **GUI Only**: Sound barrier status now shown only in HUD panel
- **Cleaner Interface**: Reduced visual clutter during high-speed gameplay

### ⚙️ Gameplay Balance
- **Default Whitelist Off**: Players start without abilities (must be added via command)
- **Contact Consequences**: Abilities deactivate on entity contact for risk/reward balance
- **Level Progression**: Clear distinction between ability tiers

## 🛠️ Installation

### For Players
1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.20.1
2. Install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Download `naoya-mod-1.2.1.jar`
4. Place in your `mods` folder

### For Server Admins
1. Add `naoya-mod-1.2.1.jar` to server `mods` folder
2. Use `/naoya whitelist add <player>` to grant abilities
3. Use `/naoya level <player> <1|2|3>` to set ability levels

## 🎮 Commands

### Whitelist Management
```
/naoya whitelist add <player>    # Grant abilities to player
/naoya whitelist remove <player> # Remove abilities from player  
/naoya whitelist list            # Show all whitelisted players
```

### Ability Control
```
/naoya level <player> <1|2|3>    # Set ability level (1-3)
/naoya burst <player> <on|off>   # Toggle burst mode (Level 3 only)
```

## 🐛 Bug Fixes
- Fixed water running speed maintenance
- Improved shadow frame rendering performance
- Enhanced particle effect stability
- Fixed sound barrier visual timing

## 📋 Requirements
- **Minecraft**: 1.20.1
- **Fabric Loader**: 0.15.0+
- **Fabric API**: Latest for 1.20.1
- **Java**: 17+

## 🔄 Changelog from v1.2.0

### Added
- Level-based shadow frame system (6/12/24 frames)
- Contact damage system with explosion effects
- Ability reset on entity contact
- Water running speed preservation

### Changed
- Removed "SOUND BARRIER BROKEN" screen text
- Default whitelist now off for all players
- Improved water walking mechanics

### Fixed
- Water speed maintenance issues
- Shadow frame count consistency
- Visual effect timing

## 🤝 Credits
- **Developer**: Fi3w0
- **Inspiration**: Naoya Zenin from Jujutsu Kaisen
- **Testers**: Community contributors
- **Mod Loader**: FabricMC

## 📄 License
MIT License - See LICENSE file for details

## ❓ Support
For issues or questions:
1. Check the [GitHub Issues](https://github.com/Fi3w0/Naoya-Mod/issues)
2. Include Minecraft version, mod version, and error logs
3. Provide detailed reproduction steps

---

**Enjoy the Divine Speed! ⚡**