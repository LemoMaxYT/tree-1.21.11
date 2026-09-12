# Tree Physics — Minecraft 1.21.11 Fabric

A small standalone Fabric implementation that makes connected tree logs fall as vanilla `FallingBlockEntity` physics when a log is broken.

## Target
- Minecraft 1.21.11
- Fabric
- Java 21
- Fabric API 0.141.6+1.21.11

## Behavior
- Breaking a log scans the connected log structure.
- If at least two connected logs are found, every remaining log is converted into a falling block.
- The tree receives a small push in the player's direction.
- The original broken log still behaves normally.
- Scan is capped at 256 logs and 24 blocks from the starting position to avoid runaway scans.

## Build
Run `gradlew.bat build` on Windows with Java 21 installed.

The resulting mod JAR is in `build/libs/`.
