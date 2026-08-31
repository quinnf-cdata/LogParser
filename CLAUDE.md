# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Run the application (GUI mode)
mvn clean javafx:run

# Build a fat JAR (jar-with-dependencies in target/)
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=LogFileTest

# Build a Windows EXE installer (requires jpackage on PATH)
mvn verify
```

The app also supports a headless CLI mode:
```bash
java -jar LogParser-1.0-SNAPSHOT-jar-with-dependencies.jar -f <path-to-log> -s <Q-Id|HTTP>
```
`-s` triggers a file split without launching the GUI and writes split files next to the source.

## Architecture

### Layer overview

| Layer | Key classes | Role |
|---|---|---|
| Entry point | `LogParserApplication` | JavaFX `Application`; loads `main-view.fxml`; handles CLI args |
| UI controller | `LogParserController` | `@FXML`-wired to FXML; owns `LogFileController` and `TabMetadata` |
| File controller | `LogFileController` | All file operations: load, split, sort, combine, write |
| Model | `LogFile` / `LogEntry` | File reading with pagination; in-memory Lucene index; line parsing |
| Index UI | `IndexTree` (extends `LogParserController`) | Builds the hierarchical Q-Id/HTTP tree; handles double-click navigation |
| Config | `JSONUtils` | Reads `%APPDATA%/CData Internal/Log Analyzer/custom_filters.json` at startup |

### LogFile & indexing

`LogFile` holds an Apache Lucene `ByteBuffersDirectory` index — entirely in RAM, not on disk. Indexing runs on a background thread the moment a file is loaded; use `indexNotReady()` / `isLoading()` to guard operations that require a ready index.

Each line is parsed by `LogEntry.parseLine()` into fields indexed by Lucene: `LINE`, `TIMESTAMP`, `LEVEL`, `QID`, `HTTP`, `CONNECTION`, `META`, `CONTENT`. Splits and searches key on these fields.

### Pagination

`LogFile.readLines(start, end)` reads only the requested range. Default page size is `Constants.MAX_LINES_DEFAULT = 100`. `TabMetadata` tracks current page, page count, and page boundaries per tab. `maxLines` on `LogFile` also limits Lucene search result sets.

### Split / Sort flow

1. `LogFileController.splitLogsByType()` queries Lucene for every occurrence of the split key, then reads the file between consecutive hits.
2. Results are accumulated in `memoryFiles` (`Map<String, StringBuilder>`), keyed by output path.
3. If `memoryOnly=false`, results are also flushed to disk; if `true` (sort path), they stay in memory.
4. Sort (`combineAndDelete()`) calls split in memory-only mode, then merges the per-key files back into a single sorted file and deletes the temporaries.
5. `isMaster` flag distinguishes user-loaded files from derived split/sort files; only master files are processed during split/sort.

### Custom filters & action types

At startup, `JSONUtils.readExternalJSON()` reads `%APPDATA%/CData Internal/Log Analyzer/custom_filters.json`. If the file doesn't exist, it is created from the embedded template (`src/main/resources/.../custom_filters_template.json`). Filters control which log lines are hidden in the UI; action types populate the Split/Sort dropdown. Both lists are `JSONConstant` objects with `displayname` and `pattern` fields.

### Threading

Heavy operations (indexing, split, sort) run on non-JavaFX threads. UI updates are dispatched via `Platform.runLater(...)`. The progress dialog polls `logFileController.isProcessing()` on a separate thread and closes itself when processing finishes.
