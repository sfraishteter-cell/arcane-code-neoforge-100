# Arcane Code 1.0.0 — Release Candidate

Geometric programmable magic for Minecraft 1.21.1 and NeoForge 21.1.238+.

## Main systems
- Rotation-invariant rune drawing on a hexagonal lattice.
- 142 core runes plus dynamic runes for registered effects, particles, blocks, items, sounds and entity types.
- Typed stack machine with numbers, booleans, strings, vectors, entities, registry values, lists and quoted programs.
- Quotes, conditional evaluation, Repeat, For Each, Map, Filter, Fold and While.
- Effects, world interaction, particle geometry and temporary physical barriers.
- Arcane Dust as the central media resource.
- Resonance Mill: an actual durable item that grinds iron ingots and amethyst shards into dust.
- Four named staff tiers: Spark, Resonance, Architect and Sovereign.
- Interactive chapter-based Arcane Codex with search and a live rune catalog.
- Optional Create 6.x recipes for Milling, Crushing and Mechanical Mixing.

## Controls
- Right-click a staff: open the Rune Chamber.
- Draw by holding left mouse over adjacent hex-grid nodes.
- Save writes the program into the held or available staff.
- Shift + right-click a staff: cast its saved program.
- Right-click the Arcane Codex: open the guide.
- Hold the Resonance Mill in one hand and an iron ingot or amethyst shard in the other; right-click to grind it.

## Build
The included GitHub Actions workflow builds with Java 21, Gradle 8.10.2 and NeoForge ModDevGradle. The production JAR appears as an Actions artifact only after a successful build.

See `UPLOAD_AND_BUILD_RU.md`, `TEST_CHECKLIST_RU.md` and `RELEASE_STATUS_RU.md`.
