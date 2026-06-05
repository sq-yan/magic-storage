# Magic Storage — Changelog

All notable changes for this mod. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [0.2.0] — 2026-06-05

### Added
- **Brand-new dark glass GUI (256×236)**: drawn entirely via `gfx.fill` with no texture atlas. Subtle frosted-glass alpha lets the world slightly bleed through behind the panel.
- **Vertical filter panel** with 6 vanilla-item-sprite buttons: Show all, Potions, Armor, Tools & weapons, Food, Blocks. Active filter highlighted with a purple border + inner glow.
- **Storage grid widened to 12×6** (was 9×6), 72 visible slots — +33% more items on screen without scrolling.
- **Quick Dump button** + rebindable hotkey (default `X`): drops the player's inventory (excluding hotbar) into the storage in one click. Merges into existing stacks first, then fills empty slots.
- **Sort cycle button** (`·` → `A` → `#` → `M`): no sort, by name, by count, by mod.
- **Search field** (top bar, 140 px wide): live filter by hover-name, case-insensitive.
- **Used / total fill bar** between the inventory label and counter: 3-tone (green ≤50%, yellow 50–67%, red >67%) matching the heart's `fill_level`.
- **Used / total counter** auto-aligned to the right, always readable no matter how many cells are connected.

### Fixed
- **🔴 Item loss on shift-click into empty cells** — Forge `ItemStackHandler.insertItem` aliases the caller's stack into an empty slot. The old `setCount(0)` for loop control was zeroing the already-stored items. Fixed by always passing a copy and shrinking via diff (`insertCopyAndShrink`).
- **Crash safety with open GUI** — when a Cell is broken (by another player or a piston), the menu now closes automatically on the next tick. The handler also skips removed cells defensively.

### Changed
- Player inventory and hotbar are now centred horizontally inside the wider GUI.
- Item-handler interaction uses our own `distributeIntoStorage(stack)` (merge → place) instead of vanilla `moveItemStackTo`, much faster at 128-cell scale.

### Internal / known
- Built against Minecraft **1.21.11**, Forge **61.1.0**, Java **21**.
- Jar name format: `magic_storage-<mcver>-<modver>.jar` (set via `base.archivesName`). Each MC version will ship its own jar.

## [0.1.0-beta.1] — 2026-06-04

### Initial private build
- Heart Storage + Storage Cell blocks, BFS network (up to 128 cells = 3 456 slots).
- 4 visual states of Heart via `fill_level` blockstate.
- Vanilla `generic_54.png` GUI with search, sort, scroll, used/total indicator.
- EN + RU locales, recipes, own creative tab.
- Textures (5 PNGs, RGBA).
