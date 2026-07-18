# Arcane Code 1.0.0-dev — The Grand Weave

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

Use Java 21 and the checked-in Gradle Wrapper:

```text
./gradlew clean build verifyReleaseJar
```

The current artifact is `Arcane-Code-NeoForge-1.21.1-1.0.0-dev.jar`. The project remains a development build until every release criterion is verified. GitHub Actions also performs a dedicated-server smoke test without Create.

The code contains 142 core rune handlers, but gameplay coverage is not complete. See `docs/CURRENT_STATE_AUDIT_RU.md` for verified behavior and known limitations.

See `UPLOAD_AND_BUILD_RU.md`, `TEST_CHECKLIST_RU.md` and `RELEASE_STATUS_RU.md`.
