# Magic Storage — Changelog

All notable changes for this mod. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [1.0.0] — 2026-06-23 — first public release

First public release. Everything below is what ships to players today; versions
0.1–0.4 are private development builds kept for history.

### Storage
- **Heart Storage** altar links nearby **Storage Cells** into one searchable,
  sortable inventory. Awaken it by inserting a **Magic Heart**.
- **Crystal Expanders** snap to the altar's faces to grow capacity (+4 cells
  each, up to 6 face-adjacent).
- **Reigall's Tuning Fork** re-tunes the network, compacting contents into the
  fewest cells.

### Interface
- Search, sort (name / count / mod), category filters, search autocomplete,
  scrolling grid, used/total fill bar.
- **Quick Dump** hotkey + **per-slot protection** with a **hotbar-protection
  toggle**. Both hotkeys rebindable in-GUI and in vanilla Controls.
- **Live fill indicators** — cells and the altar glow green → yellow → red.

### Progression & lore
- **Liber Reigallus** books handed out at milestones (lore + recipes).
- Recipes also appear in the vanilla recipe book.
- **Villager trades** — Magic Heart and Magic Crystal sold by librarians.

### Changed since dev builds
- Removed the experimental Tier-2 Heart; progression now runs through Crystal
  Expanders and future tier crystals.

### Requires
- Minecraft **1.21.11**, Forge **61+**. Licensed **MIT**.

## [0.3.0] — 2026-06-08

### Added
- **Tier 2 Heart Storage II** — new multi-block 1×2 vertical artifact (DoublePlantBlock pattern). Connects up to **16 cells** (432 slots). Custom textures: lower half with a heart glyph + rune ring, upper half as a crown with a purple crystal and dedication. Lower drops the item, upper visual-only.
- **Tier 2 textures (Gemini-generated)** — copper-gold framed obsidian panels, seamless mid-seam between halves. Block model uses placeholder T1 textures for active fill levels (green/yellow/red) — those will follow in a later patch.
- **Global Quick Dump from world** — press the same hotkey (`X` default) **outside** the GUI within 5 blocks of any Heart to dump the player's main inventory directly. Silent no-op if no heart is within range. Built on a new `SimpleChannel` + `GlobalDumpPacket` round-trip.
- **26-neighbor cell connectivity** — cells now connect through faces, edges and corners (previously faces only). Networks can grow as 3D clouds.
- **Heart overflow message** — when more cells are reachable than the heart can hold (T1=4, T2=16), the nearest player gets a one-shot action-bar message: «Heart connects up to N cells — extras are ignored».

### Fixed
- **🔴 Cell content "disappearing" after network growth** — when more cells became reachable, BFS would replace the connected set instead of preserving it, so previously-connected cells with items appeared empty in the menu. Fixed via stateful `Set<BlockPos> connected` on the Heart BE (persisted in NBT). Connected cells are never evicted while they remain reachable.
- **🔴 Client disconnect on opening the Heart menu** — the client built `AggregatedItemHandler` from an empty connected set (server-only state) and crashed on the first slot-content packet (`IndexOutOfBoundsException`). Fixed by sending the connected positions through the `Consumer<FriendlyByteBuf>` passed to `openMenu`, then resolving them client-side via `AggregatedItemHandler.fromPositions(level, positions)`.
- **Search box Esc behavior** — pressing Esc now clears the query and unfocuses the box (Screen-level defocus too), matching vanilla creative inventory.
- **Quick Dump hotkey ignored inside open GUI** — the hotkey check now runs above the search-box input consumption.

### Changed
- **Tier 1 Heart Storage capacity 128 → 4 cells** (108 slots). Creates a meaningful reason to upgrade to Tier 2.
- **`HeartStorageBlock` refactored to an abstract base class** with `HeartStorageT1Block` (4 cells) and `HeartStorageT2Block` (16 cells). One `BlockEntityType` registered for both blocks. Tier capacity comes from the block via `getMaxCells()`.
- `CellNetwork.collect` → `CellNetwork.collectReachable` — returns all reachable cells without an artificial cap, the Heart applies its own per-tier limit.
- `MagicStorageMenu.distributeIntoStorage` and `quickDump` moved into `AggregatedItemHandler.distribute(stack)` and `dumpPlayerInventoryMain(player)` for reuse from the global Quick Dump path.

### Internal / known
- Built against Minecraft **1.21.11**, Forge **61.1.0**, Java **21**.
- T2 lower fill-level textures (green/yellow/red) still reuse T1 textures as placeholders — a Gemini regen run for the matching glow variations is queued.

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
