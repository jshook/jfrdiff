# JFRDiff - Java Flight Recorder Comparison Tool

JFRDiff is a command-line tool that compares two Java Flight Recorder (JFR) files and summarizes the key differences between them. This helps in analyzing performance changes between different runs or configurations of a Java application.

## Features

- Compare event counts, durations, and stack traces between two JFR files
- Filter comparisons by specific event types
- Set custom thresholds for reporting differences
- Multiple visualization options including a pseudo graphical timeline view
- Simple command-line interface

## Usage

```bash
java -jar jfrdiff-1.0-SNAPSHOT-jar-with-dependencies.jar [options] FIRST_JFR_FILE SECOND_JFR_FILE
```

### Options

- `-t, --threshold=THRESHOLD_PERCENT`: Threshold percentage for reporting differences (default: 10.0)
- `-e, --event-types=EVENT_TYPES`: Specific event types to compare (comma-separated)
- `-v, --view=VIEW_TYPE`: Readout view type for comparison (default: default, options: default, timeline)
- `-h, --help`: Show help message and exit
- `-V, --version`: Print version information and exit

### Examples

```bash
# Compare two JFR files with default settings
java -jar jfrdiff.jar profile1.jfr profile2.jfr

# Compare with a custom threshold of 5%
java -jar jfrdiff.jar -t 5.0 profile1.jfr profile2.jfr

# Compare only specific event types
java -jar jfrdiff.jar -e jdk.GCHeapSummary,jdk.CPULoad profile1.jfr profile2.jfr

# Use the timeline view to visualize differences
java -jar jfrdiff.jar -v timeline profile1.jfr profile2.jfr

# Combine options: timeline view with 5% threshold and specific event types
java -jar jfrdiff.jar -v timeline -t 5.0 -e jdk.GCHeapSummary,jdk.CPULoad profile1.jfr profile2.jfr
```

## Building from Source

```bash
mvn clean package
```

This will create two JAR files in the `target` directory:
- `jfrdiff-1.0-SNAPSHOT.jar`: The main JAR file
- `jfrdiff-1.0-SNAPSHOT-jar-with-dependencies.jar`: A standalone JAR with all dependencies included

## Documentation

Comprehensive documentation about JFR file semantics is available in the [docs](docs) directory:

- [JFR Semantics](docs/JFR_SEMANTICS.md): Detailed information about JFR file structure, temporal types, relationships, and event categories with diagrams
- [JFR Glyphs](docs/JFR_GLYPHS.md): Unicode glyphs used to represent JFR concepts in the timeline view

This documentation will help you understand the data being compared by JFRDiff and how to interpret the results.

### Timeline View

The timeline view provides a pseudo graphical representation of the differences between two JFR files, with a timeline moving from left to right and using Unicode glyphs. It shows:

- Significant departures between the two JFR files based on the threshold
- Time deltas between corresponding events in both files
- Sparkline-style visualizations of key metrics (counts, durations, etc.)
- Visual timeline representation showing when events occurred in each file

The timeline view uses the following elements:

- **Event Type Glyphs**: Different glyphs represent different categories of events (e.g., ♻️ for GC events, ⚙️ for CPU events)
- **Change Indicators**: 🔼 for increases, 🔽 for decreases, ⚠️ for significant changes
- **Sparklines**: Visual representation of changes (e.g., `▁▃▅▇█` for increasing values)
- **Timeline Visualization**: Shows when events occurred in each file and highlights the time deltas between them

To use the timeline view, add the `-v timeline` option to your command.

## Requirements

- Java 11 or higher

## License

This project is licensed under the MIT License - see the LICENSE file for details.
