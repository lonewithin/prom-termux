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

## Keep-alive and start after reboot (v0.119.0-prom.1)
Official Termux:Boot cannot be used: it must be signed with the same key as the terminal, and PROM Terminal uses its own key.
This build therefore does the job inside the app:

- `RECEIVE_BOOT_COMPLETED` starts the existing foreground `TermuxService`, which holds a notification and a wake/wifi lock and returns `START_STICKY`.
- After reboot, and if the service is later killed, a background watchdog re-sources `~/.bashrc` every 60s so idempotent hooks (pgrep-guarded sniper / Starlink starts, etc.) come back. Tap **Exit** on the notification to stop this.
- Files in `~/.termux/boot/` are also run once at boot, same contract as Termux:Boot (optional).
- Opening the app prompts the Android battery-optimization exemption if it is not already granted.

Package name stays `com.termux` because the bootstrap packages are compiled for that prefix.
It cannot be installed alongside official Termux, and it does not work with official Termux:API/plugins (different signature).
