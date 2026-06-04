# Magic Storage

Magic-themed storage system for Minecraft Forge 1.21.11.

## Features
- Heart Storage block + Storage Cells connected by face-neighbor BFS
- Up to 128 cells per heart (~3456 slots)
- 4 visual states for heart (dormant / green / yellow / red) based on fill level
- GUI with search, sort (name/count/mod), scroll
- Items physically stored in cells — break the heart, cells stay; break a cell, items drop

## Requirements
- Minecraft 1.21.11
- Forge 61.1.0+
- Java 21

## Install
Drop `magic_storage-x.y.z.jar` into your mods folder.

## Crafting
- **Storage Cell**: 7 copper ingots + 1 chest + 1 redstone dust (chest in center, redstone on top-center)
- **Heart Storage**: 3 redstone (top) + 2 emeralds + diamond (middle) + 3 obsidian (bottom)

## Status
Early beta. Working as a prototype but missing polish (shift-click distribute, sound, JEI integration, config). Feedback welcome via Issues.

## License
MIT — see [LICENSE](LICENSE).