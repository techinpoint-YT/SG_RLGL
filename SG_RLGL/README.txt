SG_RLGL (Paper/Spigot 1.21) — by King

Build:
  - Install JDK 17+
  - In project root, run: Windows: gradlew build  |  macOS/Linux: ./gradlew build
  - Output JAR: build/libs/SG_RLGL-1.0.0.jar

Usage:
  - /sg set lobby
  - /sg set guestlobby
  - /sg set admin <player>
  - /sg set guest <player>
  - /sg set rules
  - /sg set finish  (gives SG Hoe; right-click a block to set finish)
  - /sg remove <admin|guest|winner> <player>
  - /sg game start   (gives dyes to admins; broadcasts Game Starts/Green/Red)
  - /sg game off
  - /sg tp <guest|admin|player> <Guestlobby|Gamelobby>
  - /sg help

Notes:
  - Admins, guests, and winners are immune during Red Light.
  - Moving during Red Light causes lightning + instant ban with reason "Moved during Red Light".
  - All locations/roles/state persist in config.yml.
