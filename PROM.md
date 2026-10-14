# PROM Terminal

A branded build of [Termux](https://github.com/termux/termux-app) (GPLv3) by @lonewithin.
All credit for the terminal, bootstrap and package system goes to the Termux project and its contributors.
Licensed under GPLv3 like upstream (see LICENSE.md).

## Changes vs upstream (v0.119.0-beta.3)
- App label "PROM Terminal", neon purple/green adaptive icon + TV banner
- Branded foreground notification text
- First-run defaults copied from `app/src/main/assets/prom/` (never overwrite existing user config):
  `~/.termux/termux.properties` (extra keys: ESC / - | HOME UP END PGUP, TAB CTRL ALT arrows PGDN ENTER),
  `~/.termux/colors.properties` (PROM neon dark), `~/.termux/font.ttf` (JetBrains Mono, OFL),
  `$PREFIX/etc/motd` (PROM banner), `$PREFIX/bin/prom-setup` (optional dev-tool installer; never starts trading)
- Release signing reads `~/.secrets/prom-termux.properties` (not in repo)

Package name stays `com.termux` because the bootstrap packages are compiled for that prefix.
It cannot be installed alongside official Termux, and it does not work with official Termux:API/plugins (different signature).
