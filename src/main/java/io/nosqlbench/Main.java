package io.nosqlbench;

import jdk.jfr.consumer.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * A command-line tool to compare two JFR files and summarize the key differences.
 */
@Command(
    name = "jfrdiff",
    mixinStandardHelpOptions = true,
    version = "jfrdiff 1.0",
    description = "Compares two JFR files and summarizes the key differences."
)
public class Main implements Callable<Integer> {

    /**
     * Enum for different readout view types for the comparison.
     */
    public enum ReadoutView {
        DEFAULT("default"),
        TIMELINE("timeline"),
        HEAP_GRAPH("heapgraph");

        private final String name;

        ReadoutView(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static ReadoutView fromString(String value) {
            for (ReadoutView view : ReadoutView.values()) {
                if (view.name.equalsIgnoreCase(value)) {
                    return view;
                }
            }
            return DEFAULT;
        }
    }

    @Parameters(index = "0", description = "First JFR file")
    private Path firstFile;

    @Parameters(index = "1", description = "Second JFR file")
    private Path secondFile;

    @Option(names = {"-t", "--threshold"}, description = "Threshold percentage for reporting differences (default: ${DEFAULT-VALUE})")
    private double thresholdPercent = 10.0;

    @Option(names = {"-e", "--event-types"}, description = "Specific event types to compare (comma-separated)", split = ",")
    private List<String> eventTypes;

    @Option(names = {"-v", "--view"}, description = "Readout view type for comparison (default: ${DEFAULT-VALUE}, candidates: ${COMPLETION-CANDIDATES})")
    private ReadoutView viewType = ReadoutView.DEFAULT;

    // Fields to store file modification times and time delta
    private Instant firstFileMtime;
    private Instant secondFileMtime;
    private Duration timeDelta;

    // Field to store the selected readout view
    private ReadoutView selectedView;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Reads the modification time of a file and returns it as an Instant.
     */
    private Instant getFileMtime(Path file) throws IOException {
        FileTime fileTime = Files.getLastModifiedTime(file);
        return fileTime.toInstant();
    }

    /**
     * Calculates the time delta between two JFR files based on their modification times.
     */
    private void calculateTimeDelta() throws IOException {
        firstFileMtime = getFileMtime(firstFile);
        secondFileMtime = getFileMtime(secondFile);
        timeDelta = Duration.between(firstFileMtime, secondFileMtime);

        System.out.println("  File 1 mtime: " + firstFileMtime);
        System.out.println("  File 2 mtime: " + secondFileMtime);
        System.out.println("  Time delta: " + formatDuration(timeDelta) + 
                          (timeDelta.isNegative() ? " (file 1 is newer)" : " (file 2 is newer)"));
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Comparing JFR files:");
        System.out.println("  File 1: " + firstFile);
        System.out.println("  File 2: " + secondFile);
        System.out.println("  Threshold: " + thresholdPercent + "%");

        // Initialize the selected view
        selectedView = viewType;
        System.out.println("  Readout view: " + selectedView);

        if (eventTypes != null && !eventTypes.isEmpty()) {
            System.out.println("  Event types filter: " + String.join(", ", eventTypes));
        }

        try {
            // Calculate time delta between files based on mtime
            calculateTimeDelta();

            // Load and parse the JFR files
            Map<String, EventSummary> firstFileSummary = summarizeJfrFile(firstFile, true);
            Map<String, EventSummary> secondFileSummary = summarizeJfrFile(secondFile, false);

            // Compare the summaries and print differences
            compareAndPrintDifferences(firstFileSummary, secondFileSummary);

            return 0;
        } catch (Exception e) {
            System.err.println("Error comparing JFR files: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Summarizes a JFR file by event type.
     * 
     * @param jfrFile The JFR file to summarize
     * @param isFirstFile Whether this is the first file (true) or second file (false)
     * @return A map of event summaries by event type
     */
    private Map<String, EventSummary> summarizeJfrFile(Path jfrFile, boolean isFirstFile) throws IOException {
        Map<String, EventSummary> eventSummaries = new HashMap<>();

        try (RecordingFile recordingFile = new RecordingFile(jfrFile)) {
            System.out.println("\nAnalyzing file: " + jfrFile);

            // Track recording time range while processing events
            Instant recordingStart = null;
            Instant recordingEnd = null;

            // Process all events
            while (recordingFile.hasMoreEvents()) {
                RecordedEvent event = recordingFile.readEvent();

                // Get the event timestamp
                Instant eventTimestamp = event.getStartTime();

                // If this is the second file, adjust the event timestamp based on the time delta
                // This normalizes timestamps across files based on their modification times
                if (!isFirstFile && timeDelta != null) {
                    eventTimestamp = eventTimestamp.minus(timeDelta);
                    // For debugging
                    // System.out.println("Adjusted timestamp: " + event.getStartTime() + " -> " + eventTimestamp);
                }

                // Skip if we're only interested in specific event types and this isn't one of them
                String currentEventType = event.getEventType().getName();

                if (eventTypes != null && !eventTypes.isEmpty()) {
                    boolean matchFound = false;

                    for (String eventType : eventTypes) {
                        if (currentEventType.equals(eventType.trim())) {
                            matchFound = true;
                            break;
                        }
                    }

                    if (!matchFound) {
                        continue;
                    }
                }

                String eventType = event.getEventType().getName();
                EventSummary summary = eventSummaries.computeIfAbsent(
                    eventType, 
                    k -> new EventSummary(eventType)
                );

                summary.count++;

                // Update timestamp information with the (potentially adjusted) timestamp
                summary.updateTimestamps(eventTimestamp);

                // Process event duration if available
                if (event.hasField("duration")) {
                    Duration duration = event.getDuration("duration");
                    summary.totalDuration = summary.totalDuration.plus(duration);

                    if (summary.minDuration == null || duration.compareTo(summary.minDuration) < 0) {
                        summary.minDuration = duration;
                    }

                    if (summary.maxDuration == null || duration.compareTo(summary.maxDuration) > 0) {
                        summary.maxDuration = duration;
                    }
                }

                // Process event stack trace if available
                if (event.getStackTrace() != null) {
                    summary.stackTraceCount++;
                }

                // Process heap object information if available
                if (eventType.equals("jdk.ObjectAllocationInNewTLAB") || 
                    eventType.equals("jdk.ObjectAllocationOutsideTLAB") ||
                    eventType.equals("jdk.OldObjectSample")) {

                    // Extract class name and size from the event
                    String className = null;
                    long objectSize = 0;

                    if (event.hasField("objectClass")) {
                        className = event.getClass("objectClass").getName();
                    }

                    if (event.hasField("allocationSize")) {
                        objectSize = event.getLong("allocationSize");
                    } else if (event.hasField("objectSize")) {
                        objectSize = event.getLong("objectSize");
                    }

                    // Update heap object information if we have a class name
                    if (className != null) {
                        final String finalClassName = className;
                        final long finalObjectSize = objectSize;
                        HeapObjectInfo objectInfo = summary.heapObjects.computeIfAbsent(
                            finalClassName, 
                            k -> new HeapObjectInfo(finalClassName)
                        );
                        objectInfo.update(finalObjectSize);
                    }
                }
            }
        }


        return eventSummaries;
    }

    /**
     * Compares two JFR file summaries and prints the key differences.
     */
    private void compareAndPrintDifferences(
            Map<String, EventSummary> firstSummary, 
            Map<String, EventSummary> secondSummary) {

        System.out.println("\n=== JFR Comparison Summary ===");

        // Use the selected view to determine how to display the comparison
        switch (selectedView) {
            case DEFAULT:
                // The default view is the current implementation
                compareAndPrintDifferencesDefaultView(firstSummary, secondSummary);
                break;
            case TIMELINE:
                // The timeline view shows a graphical representation of differences
                compareAndPrintDifferencesTimelineView(firstSummary, secondSummary);
                break;
            case HEAP_GRAPH:
                // The heap graph view shows a reachability graph of heap objects
                compareAndPrintDifferencesHeapGraphView(firstSummary, secondSummary);
                break;
            default:
                // Fallback to default view if the selected view is not implemented
                compareAndPrintDifferencesDefaultView(firstSummary, secondSummary);
                break;
        }
    }

    /**
     * Default view implementation for comparing and printing differences.
     */
    private void compareAndPrintDifferencesDefaultView(
            Map<String, EventSummary> firstSummary, 
            Map<String, EventSummary> secondSummary) {

        // Get all unique event types from both files
        Set<String> allEventTypes = new HashSet<>();

        // If event types filter is specified, only include those event types
        if (eventTypes != null && !eventTypes.isEmpty()) {
            for (String eventType : eventTypes) {
                String trimmedEventType = eventType.trim();
                if (firstSummary.containsKey(trimmedEventType) || secondSummary.containsKey(trimmedEventType)) {
                    allEventTypes.add(trimmedEventType);
                }
            }
            System.out.println("  Filtering to event types: " + String.join(", ", eventTypes));
        } else {
            // Otherwise include all event types
            allEventTypes.addAll(firstSummary.keySet());
            allEventTypes.addAll(secondSummary.keySet());
        }

        // Sort event types for consistent output
        List<String> sortedEventTypes = new ArrayList<>(allEventTypes);
        Collections.sort(sortedEventTypes);

        for (String eventType : sortedEventTypes) {
            EventSummary summary1 = firstSummary.getOrDefault(eventType, new EventSummary(eventType));
            EventSummary summary2 = secondSummary.getOrDefault(eventType, new EventSummary(eventType));

            // Skip if both have zero count
            if (summary1.count == 0 && summary2.count == 0) {
                continue;
            }

            // Calculate differences
            double countDiffPercent = calculatePercentDifference(summary1.count, summary2.count);

            // Only show significant differences based on threshold
            if (Math.abs(countDiffPercent) >= thresholdPercent || 
                (summary1.count == 0 && summary2.count > 0) || 
                (summary1.count > 0 && summary2.count == 0)) {

                System.out.println("\nEvent Type: " + eventType);
                System.out.println("  Count: " + summary1.count + " -> " + summary2.count + 
                                  " (" + formatPercentChange(countDiffPercent) + ")");

                // Print timestamp information if available
                if (summary1.firstEventTimestamp != null && summary2.firstEventTimestamp != null) {
                    System.out.println("  First Event: " + summary1.firstEventTimestamp + " -> " + 
                                      summary2.firstEventTimestamp);
                    System.out.println("  Last Event: " + summary1.lastEventTimestamp + " -> " + 
                                      summary2.lastEventTimestamp);

                    // Calculate and print the time span of events in each file
                    if (summary1.lastEventTimestamp != null && summary1.firstEventTimestamp != null) {
                        Duration timeSpan1 = Duration.between(summary1.firstEventTimestamp, summary1.lastEventTimestamp);
                        System.out.println("  Time Span (File 1): " + formatDuration(timeSpan1));
                    }

                    if (summary2.lastEventTimestamp != null && summary2.firstEventTimestamp != null) {
                        Duration timeSpan2 = Duration.between(summary2.firstEventTimestamp, summary2.lastEventTimestamp);
                        System.out.println("  Time Span (File 2): " + formatDuration(timeSpan2));
                    }
                }

                // Compare durations if available
                if ((summary1.totalDuration != null && !summary1.totalDuration.isZero()) || 
                    (summary2.totalDuration != null && !summary2.totalDuration.isZero())) {

                    System.out.println("  Avg Duration: " + 
                                      formatDuration(summary1.getAverageDuration()) + " -> " + 
                                      formatDuration(summary2.getAverageDuration()));

                    if (summary1.minDuration != null && summary2.minDuration != null) {
                        System.out.println("  Min Duration: " + 
                                          formatDuration(summary1.minDuration) + " -> " + 
                                          formatDuration(summary2.minDuration));
                    }

                    if (summary1.maxDuration != null && summary2.maxDuration != null) {
                        System.out.println("  Max Duration: " + 
                                          formatDuration(summary1.maxDuration) + " -> " + 
                                          formatDuration(summary2.maxDuration));
                    }
                }

                // Compare stack trace counts if available
                if (summary1.stackTraceCount > 0 || summary2.stackTraceCount > 0) {
                    double stackDiffPercent = calculatePercentDifference(
                        summary1.stackTraceCount, summary2.stackTraceCount);

                    System.out.println("  Stack Traces: " + summary1.stackTraceCount + " -> " + 
                                      summary2.stackTraceCount + 
                                      " (" + formatPercentChange(stackDiffPercent) + ")");
                }
            }
        }
    }

    /**
     * Calculates the percentage difference between two values.
     */
    private double calculatePercentDifference(long value1, long value2) {
        if (value1 == 0 && value2 == 0) {
            return 0;
        }

        if (value1 == 0) {
            return 100.0; // Represents infinity, but capped at 100%
        }

        return ((double) (value2 - value1) / value1) * 100.0;
    }

    /**
     * Formats a duration in a human-readable format.
     */
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "N/A";
        }

        long nanos = duration.toNanos();

        if (nanos < 1_000) {
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.2f µs", nanos / 1_000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanos / 1_000_000_000.0);
        }
    }

    /**
     * Formats a percentage change with a sign.
     */
    private String formatPercentChange(double percentChange) {
        if (percentChange > 0) {
            return "+" + String.format("%.1f%%", percentChange);
        } else {
            return String.format("%.1f%%", percentChange);
        }
    }

    /**
     * Timeline view implementation for comparing and printing differences.
     * This view shows a pseudo graphical representation of the differences between two JFR files,
     * with a timeline moving from left to right and using Unicode glyphs.
     */
    private void compareAndPrintDifferencesTimelineView(
            Map<String, EventSummary> firstSummary, 
            Map<String, EventSummary> secondSummary) {

        System.out.println("\n=== Timeline View of Significant Differences ===");

        // Get all unique event types from both files
        Set<String> allEventTypes = new HashSet<>();

        // If event types filter is specified, only include those event types
        if (eventTypes != null && !eventTypes.isEmpty()) {
            for (String eventType : eventTypes) {
                String trimmedEventType = eventType.trim();
                if (firstSummary.containsKey(trimmedEventType) || secondSummary.containsKey(trimmedEventType)) {
                    allEventTypes.add(trimmedEventType);
                }
            }
            System.out.println("  Filtering to event types: " + String.join(", ", eventTypes));
        } else {
            // Otherwise include all event types
            allEventTypes.addAll(firstSummary.keySet());
            allEventTypes.addAll(secondSummary.keySet());
        }

        // Sort event types for consistent output
        List<String> sortedEventTypes = new ArrayList<>(allEventTypes);
        Collections.sort(sortedEventTypes);

        // Filter for significant differences based on threshold
        List<String> significantEventTypes = new ArrayList<>();
        for (String eventType : sortedEventTypes) {
            EventSummary summary1 = firstSummary.getOrDefault(eventType, new EventSummary(eventType));
            EventSummary summary2 = secondSummary.getOrDefault(eventType, new EventSummary(eventType));

            // Skip if both have zero count
            if (summary1.count == 0 && summary2.count == 0) {
                continue;
            }

            // Calculate differences
            double countDiffPercent = calculatePercentDifference(summary1.count, summary2.count);

            // Only include significant differences based on threshold
            if (Math.abs(countDiffPercent) >= thresholdPercent || 
                (summary1.count == 0 && summary2.count > 0) || 
                (summary1.count > 0 && summary2.count == 0)) {
                significantEventTypes.add(eventType);
            }
        }

        if (significantEventTypes.isEmpty()) {
            System.out.println("\nNo significant differences found with threshold " + thresholdPercent + "%");
            return;
        }

        // Print timeline header
        System.out.println("\n  Timeline Legend:");
        System.out.println("  📊 JFR Recording   ⏱️ Timestamp   ⏳ Duration   📈 Event Count   🔄 Change");
        System.out.println("  🔼 Increase   🔽 Decrease   ⚠️ Significant Change   ✨ New Event   ❌ Missing Event");

        // Print timeline scale
        System.out.println("\n  Time Scale:");
        System.out.println("  ◀───────────────────────────────────────────────────────────────▶");
        System.out.println("  Earlier                                                      Later");

        // Print the timeline for each significant event type
        for (String eventType : significantEventTypes) {
            EventSummary summary1 = firstSummary.getOrDefault(eventType, new EventSummary(eventType));
            EventSummary summary2 = secondSummary.getOrDefault(eventType, new EventSummary(eventType));

            // Skip if both have zero count
            if (summary1.count == 0 && summary2.count == 0) {
                continue;
            }

            // Calculate differences
            double countDiffPercent = calculatePercentDifference(summary1.count, summary2.count);

            // Get event type glyph based on category
            String eventGlyph = getEventGlyph(eventType);

            // Print event type header
            System.out.println("\n  " + eventGlyph + " " + getShortEventName(eventType) + ":");

            // Print count comparison with sparkline
            String countSparkline = generateSparkline(summary1.count, summary2.count);
            System.out.println("    📈 Count: " + summary1.count + " → " + summary2.count + 
                              " (" + formatPercentChange(countDiffPercent) + ") " + countSparkline);

            // Print timeline visualization
            if (summary1.firstEventTimestamp != null && summary2.firstEventTimestamp != null) {
                // Calculate time delta between first events
                Duration firstEventDelta = Duration.between(summary1.firstEventTimestamp, summary2.firstEventTimestamp);
                String firstEventDeltaStr = formatDuration(firstEventDelta);

                // Calculate time delta between last events
                Duration lastEventDelta = Duration.between(summary1.lastEventTimestamp, summary2.lastEventTimestamp);
                String lastEventDeltaStr = formatDuration(lastEventDelta);

                // Print first event timeline
                System.out.println("    🏁 First Event:");
                System.out.print("      File 1: ");
                System.out.println(summary1.firstEventTimestamp);
                System.out.print("      File 2: ");
                System.out.println(summary2.firstEventTimestamp);
                System.out.println("      Delta: " + firstEventDeltaStr + 
                                  (firstEventDelta.isNegative() ? " (earlier in file 2)" : " (later in file 2)"));

                // Print last event timeline
                System.out.println("    🏆 Last Event:");
                System.out.print("      File 1: ");
                System.out.println(summary1.lastEventTimestamp);
                System.out.print("      File 2: ");
                System.out.println(summary2.lastEventTimestamp);
                System.out.println("      Delta: " + lastEventDeltaStr + 
                                  (lastEventDelta.isNegative() ? " (earlier in file 2)" : " (later in file 2)"));

                // Print time span comparison
                if (summary1.lastEventTimestamp != null && summary1.firstEventTimestamp != null &&
                    summary2.lastEventTimestamp != null && summary2.firstEventTimestamp != null) {

                    Duration timeSpan1 = Duration.between(summary1.firstEventTimestamp, summary1.lastEventTimestamp);
                    Duration timeSpan2 = Duration.between(summary2.firstEventTimestamp, summary2.lastEventTimestamp);
                    double timeSpanDiffPercent = calculatePercentDifference(timeSpan1.toMillis(), timeSpan2.toMillis());

                    System.out.println("    ⏳ Time Span:");
                    System.out.println("      File 1: " + formatDuration(timeSpan1));
                    System.out.println("      File 2: " + formatDuration(timeSpan2));
                    System.out.println("      Change: " + formatPercentChange(timeSpanDiffPercent));

                    // Generate a visual timeline
                    System.out.println("    ⏱️ Timeline:");
                    System.out.println("      " + generateTimeline(summary1, summary2));
                }
            }

            // Print duration comparison if available
            if ((summary1.totalDuration != null && !summary1.totalDuration.isZero()) || 
                (summary2.totalDuration != null && !summary2.totalDuration.isZero())) {

                Duration avgDuration1 = summary1.getAverageDuration();
                Duration avgDuration2 = summary2.getAverageDuration();

                // Calculate percentage difference for average duration
                double avgDurationDiffPercent = 0;
                if (avgDuration1.toNanos() > 0) {
                    avgDurationDiffPercent = ((double) (avgDuration2.toNanos() - avgDuration1.toNanos()) / avgDuration1.toNanos()) * 100.0;
                }

                String durationSparkline = generateDurationSparkline(avgDuration1, avgDuration2);

                System.out.println("    ⏳ Avg Duration: " + formatDuration(avgDuration1) + " → " + 
                                  formatDuration(avgDuration2) + " (" + formatPercentChange(avgDurationDiffPercent) + ") " + 
                                  durationSparkline);
            }

            // Print stack trace comparison if available
            if (summary1.stackTraceCount > 0 || summary2.stackTraceCount > 0) {
                double stackDiffPercent = calculatePercentDifference(summary1.stackTraceCount, summary2.stackTraceCount);

                String stackSparkline = generateSparkline(summary1.stackTraceCount, summary2.stackTraceCount);

                System.out.println("    📚 Stack Traces: " + summary1.stackTraceCount + " → " + 
                                  summary2.stackTraceCount + " (" + formatPercentChange(stackDiffPercent) + ") " + 
                                  stackSparkline);
            }
        }
    }

    /**
     * Generates a sparkline representation of the change between two values.
     */
    private String generateSparkline(long value1, long value2) {
        if (value1 == 0 && value2 == 0) {
            return "▁▁▁▁▁";
        }

        if (value1 == 0) {
            return "▁▁▁▁▃▅▇ ✨"; // New event
        }

        if (value2 == 0) {
            return "▇▅▃▁▁▁▁ ❌"; // Missing event
        }

        double percentChange = ((double) (value2 - value1) / value1) * 100.0;

        if (Math.abs(percentChange) < 5) {
            return "▃▃▃▃▃"; // No significant change
        } else if (percentChange > 0) {
            if (percentChange > 100) {
                return "▁▃▅▇█ 🔼⚠️"; // Very large increase
            } else if (percentChange > 50) {
                return "▁▃▅▇█ 🔼"; // Large increase
            } else {
                return "▁▃▅▆▇ 🔼"; // Moderate increase
            }
        } else {
            if (percentChange < -50) {
                return "█▇▅▃▁ 🔽⚠️"; // Very large decrease
            } else if (percentChange < -25) {
                return "█▇▅▃▁ 🔽"; // Large decrease
            } else {
                return "█▆▅▃▁ 🔽"; // Moderate decrease
            }
        }
    }

    /**
     * Generates a sparkline representation of the change between two durations.
     */
    private String generateDurationSparkline(Duration duration1, Duration duration2) {
        if (duration1.isZero() && duration2.isZero()) {
            return "▁▁▁▁▁";
        }

        if (duration1.isZero()) {
            return "▁▁▁▁▃▅▇ ✨"; // New duration
        }

        if (duration2.isZero()) {
            return "▇▅▃▁▁▁▁ ❌"; // Missing duration
        }

        double percentChange = ((double) (duration2.toNanos() - duration1.toNanos()) / duration1.toNanos()) * 100.0;

        if (Math.abs(percentChange) < 5) {
            return "▃▃▃▃▃"; // No significant change
        } else if (percentChange > 0) {
            if (percentChange > 100) {
                return "▁▃▅▇█ 🔼⚠️"; // Very large increase
            } else if (percentChange > 50) {
                return "▁▃▅▇█ 🔼"; // Large increase
            } else {
                return "▁▃▅▆▇ 🔼"; // Moderate increase
            }
        } else {
            if (percentChange < -50) {
                return "█▇▅▃▁ 🔽⚠️"; // Very large decrease
            } else if (percentChange < -25) {
                return "█▇▅▃▁ 🔽"; // Large decrease
            } else {
                return "█▆▅▃▁ 🔽"; // Moderate decrease
            }
        }
    }

    /**
     * Generates a visual timeline representation of two event summaries.
     */
    private String generateTimeline(EventSummary summary1, EventSummary summary2) {
        StringBuilder timeline = new StringBuilder();

        // Calculate the total time span across both files
        Instant earliestStart = summary1.firstEventTimestamp.isBefore(summary2.firstEventTimestamp) ? 
                               summary1.firstEventTimestamp : summary2.firstEventTimestamp;

        Instant latestEnd = summary1.lastEventTimestamp.isAfter(summary2.lastEventTimestamp) ? 
                           summary1.lastEventTimestamp : summary2.lastEventTimestamp;

        Duration totalTimeSpan = Duration.between(earliestStart, latestEnd);

        // Calculate positions for file 1 events
        Duration file1StartOffset = Duration.between(earliestStart, summary1.firstEventTimestamp);
        Duration file1Duration = Duration.between(summary1.firstEventTimestamp, summary1.lastEventTimestamp);

        // Calculate positions for file 2 events
        Duration file2StartOffset = Duration.between(earliestStart, summary2.firstEventTimestamp);
        Duration file2Duration = Duration.between(summary2.firstEventTimestamp, summary2.lastEventTimestamp);

        // Generate timeline for file 1
        timeline.append("File 1: ");
        int timelineWidth = 50; // Width of the timeline in characters

        double file1StartPos = (double) file1StartOffset.toMillis() / totalTimeSpan.toMillis() * timelineWidth;
        double file1EndPos = (double) (file1StartOffset.toMillis() + file1Duration.toMillis()) / totalTimeSpan.toMillis() * timelineWidth;

        for (int i = 0; i < timelineWidth; i++) {
            if (i < file1StartPos) {
                timeline.append("·");
            } else if (i <= file1EndPos) {
                timeline.append("━");
            } else {
                timeline.append("·");
            }
        }

        // Add event count indicator
        timeline.append(" ").append(summary1.count).append(" events");
        timeline.append("\n      ");

        // Generate timeline for file 2
        timeline.append("File 2: ");

        double file2StartPos = (double) file2StartOffset.toMillis() / totalTimeSpan.toMillis() * timelineWidth;
        double file2EndPos = (double) (file2StartOffset.toMillis() + file2Duration.toMillis()) / totalTimeSpan.toMillis() * timelineWidth;

        for (int i = 0; i < timelineWidth; i++) {
            if (i < file2StartPos) {
                timeline.append("·");
            } else if (i <= file2EndPos) {
                timeline.append("━");
            } else {
                timeline.append("·");
            }
        }

        // Add event count indicator
        timeline.append(" ").append(summary2.count).append(" events");

        // Add delta markers
        timeline.append("\n      ");

        // Calculate delta positions
        double deltaStartPos = Math.abs(file1StartPos - file2StartPos);
        double deltaEndPos = Math.abs(file1EndPos - file2EndPos);

        // Add delta visualization
        if (deltaStartPos > 0 || deltaEndPos > 0) {
            timeline.append("Delta:  ");

            for (int i = 0; i < timelineWidth; i++) {
                if ((i >= Math.min(file1StartPos, file2StartPos) && i <= Math.max(file1StartPos, file2StartPos)) ||
                    (i >= Math.min(file1EndPos, file2EndPos) && i <= Math.max(file1EndPos, file2EndPos))) {
                    timeline.append("▲");
                } else {
                    timeline.append(" ");
                }
            }

            // Add delta information
            Duration startDelta = Duration.between(summary1.firstEventTimestamp, summary2.firstEventTimestamp);
            Duration endDelta = Duration.between(summary1.lastEventTimestamp, summary2.lastEventTimestamp);

            timeline.append(" Start: ").append(formatDuration(startDelta.abs()));
            timeline.append(", End: ").append(formatDuration(endDelta.abs()));
        }

        return timeline.toString();
    }

    /**
     * Returns a glyph representing the event type category.
     */
    private String getEventGlyph(String eventType) {
        if (eventType.contains("GC")) {
            return "♻️";
        } else if (eventType.contains("CPU")) {
            return "⚙️";
        } else if (eventType.contains("Thread")) {
            return "🧵";
        } else if (eventType.contains("Memory")) {
            return "🧠";
        } else if (eventType.contains("Method")) {
            return "📞";
        } else if (eventType.contains("Compilation")) {
            return "🚀";
        } else if (eventType.contains("Monitor")) {
            return "🔒";
        } else if (eventType.contains("Socket")) {
            return "🔌";
        } else if (eventType.contains("File")) {
            return "📂";
        } else {
            return "📋";
        }
    }

    /**
     * Returns a shortened version of the event name for display.
     */
    private String getShortEventName(String eventType) {
        // Remove package prefix if present
        int lastDot = eventType.lastIndexOf('.');
        if (lastDot > 0) {
            return eventType.substring(lastDot + 1);
        }
        return eventType;
    }

    /**
     * Heap graph view implementation for comparing and printing differences.
     * Shows a reachability graph of heap objects, including only the top 10 by count
     * and top 10 by total residency objects.
     */
    private void compareAndPrintDifferencesHeapGraphView(
            Map<String, EventSummary> firstSummary, 
            Map<String, EventSummary> secondSummary) {

        System.out.println("\n=== Heap Reachability Graph View ===");

        // Process first file
        System.out.println("\nFile 1 Heap Objects:");
        processHeapObjectsForView(firstSummary);

        // Process second file
        System.out.println("\nFile 2 Heap Objects:");
        processHeapObjectsForView(secondSummary);
    }

    /**
     * Processes heap objects for the heap graph view.
     * Shows only the top 10 by count and top 10 by total residency objects.
     */
    private void processHeapObjectsForView(Map<String, EventSummary> summary) {
        // Collect all heap objects from all event types
        Map<String, HeapObjectInfo> allHeapObjects = new HashMap<>();

        for (EventSummary eventSummary : summary.values()) {
            for (Map.Entry<String, HeapObjectInfo> entry : eventSummary.heapObjects.entrySet()) {
                String className = entry.getKey();
                HeapObjectInfo objectInfo = entry.getValue();

                HeapObjectInfo existingInfo = allHeapObjects.get(className);
                if (existingInfo == null) {
                    allHeapObjects.put(className, objectInfo);
                } else {
                    // Merge the information
                    existingInfo.count += objectInfo.count;
                    existingInfo.totalSize += objectInfo.totalSize;
                }
            }
        }

        if (allHeapObjects.isEmpty()) {
            System.out.println("  No heap object information available.");
            return;
        }

        // Sort by count
        List<HeapObjectInfo> sortedByCount = new ArrayList<>(allHeapObjects.values());
        sortedByCount.sort((o1, o2) -> Long.compare(o2.count, o1.count)); // Descending order

        // Sort by total size
        List<HeapObjectInfo> sortedBySize = new ArrayList<>(allHeapObjects.values());
        sortedBySize.sort((o1, o2) -> Long.compare(o2.totalSize, o1.totalSize)); // Descending order

        // Display top 10 by count
        System.out.println("\n  Top 10 Objects by Count:");
        System.out.println("  ----------------------");
        System.out.println("  | Class Name                      | Count       | Total Size  |");
        System.out.println("  |----------------------------------|-------------|-------------|");

        int count = 0;
        for (HeapObjectInfo info : sortedByCount) {
            if (count >= 10) break;

            // Format the class name to fit in the column
            String className = info.className;
            if (className.length() > 32) {
                className = "..." + className.substring(className.length() - 29);
            }

            System.out.printf("  | %-32s | %-11d | %-11d |\n", 
                              className, info.count, info.totalSize);
            count++;
        }

        // Display top 10 by total size
        System.out.println("\n  Top 10 Objects by Total Residency:");
        System.out.println("  ------------------------------");
        System.out.println("  | Class Name                      | Total Size  | Count       |");
        System.out.println("  |----------------------------------|-------------|-------------|");

        count = 0;
        for (HeapObjectInfo info : sortedBySize) {
            if (count >= 10) break;

            // Format the class name to fit in the column
            String className = info.className;
            if (className.length() > 32) {
                className = "..." + className.substring(className.length() - 29);
            }

            System.out.printf("  | %-32s | %-11d | %-11d |\n", 
                              className, info.totalSize, info.count);
            count++;
        }
    }

    /**
     * Class to hold summary information for a specific event type.
     */
    private static class EventSummary {
        final String eventType;
        long count = 0;
        Duration totalDuration = Duration.ZERO;
        Duration minDuration = null;
        Duration maxDuration = null;
        long stackTraceCount = 0;

        // Store timestamp information for time-based analysis
        Instant firstEventTimestamp = null;
        Instant lastEventTimestamp = null;

        // Store heap object information
        Map<String, HeapObjectInfo> heapObjects = new HashMap<>();

        EventSummary(String eventType) {
            this.eventType = eventType;
        }

        Duration getAverageDuration() {
            if (count == 0 || totalDuration.isZero()) {
                return Duration.ZERO;
            }
            return totalDuration.dividedBy(count);
        }

        /**
         * Updates the timestamp information for this event summary.
         */
        void updateTimestamps(Instant timestamp) {
            if (firstEventTimestamp == null || timestamp.isBefore(firstEventTimestamp)) {
                firstEventTimestamp = timestamp;
            }

            if (lastEventTimestamp == null || timestamp.isAfter(lastEventTimestamp)) {
                lastEventTimestamp = timestamp;
            }
        }
    }

    /**
     * Class to hold information about a heap object.
     */
    private static class HeapObjectInfo {
        final String className;
        long count = 0;
        long totalSize = 0;

        HeapObjectInfo(String className) {
            this.className = className;
        }

        void update(long size) {
            count++;
            totalSize += size;
        }
    }
}
