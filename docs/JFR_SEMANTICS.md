# Java Flight Recorder (JFR) File Semantics

This document provides a comprehensive overview of the core semantics of Java Flight Recorder (JFR) files, including temporal types, relationships, and different categories of events and data.

## 1. JFR File Structure Overview

```
┌─────────────────────────────────────────────────────────┐
│                   JFR Recording File                     │
├─────────────────────────────────────────────────────────┤
│ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────┐ │
│ │  Metadata       │ │  Constant Pool  │ │  Event Data │ │
│ └─────────────────┘ └─────────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────┘
```

A JFR file consists of three main components:
- **Metadata**: Contains event type definitions, settings, and recording information
- **Constant Pool**: Stores constant values to reduce file size through deduplication
- **Event Data**: The actual recorded events with their timestamps and values

## 2. Temporal Types and Time Representation

JFR uses a sophisticated time model to represent different temporal aspects of events:

```
┌───────────────────────────────────────────────────────────────────────┐
│                       JFR Temporal Types                               │
├───────────────┬───────────────────────────────────────────────────────┤
│ Timestamp     │ Absolute time when an event occurred                   │
├───────────────┼───────────────────────────────────────────────────────┤
│ Duration      │ Time span between two points (e.g., method execution)  │
├───────────────┼───────────────────────────────────────────────────────┤
│ Timespan      │ Similar to Duration but with different semantic meaning│
├───────────────┼───────────────────────────────────────────────────────┤
│ Ticks         │ High-resolution timing unit for precise measurements   │
└───────────────┴───────────────────────────────────────────────────────┘
```

### Time Relationships Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  Start Timestamp                                 End Timestamp      │
│       │                                                │            │
│       ▼                                                ▼            │
│       ┌────────────────────────────────────────────────┐           │
│       │              Event Duration/Timespan           │           │
│       └────────────────────────────────────────────────┘           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 3. Event Categories and Hierarchy

JFR events are organized in a hierarchical structure by category:

```
┌───────────────────────────────────────────────────────────────────┐
│                       JFR Event Categories                         │
└───────────────────────────────────────────────────────────────────┘
                                  │
          ┌──────────────────────┼──────────────────────┐
          ▼                      ▼                      ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  JVM Events     │    │  Java App Events│    │  OS Events      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
          │                      │                      │
    ┌─────┴─────┐          ┌────┴────┐           ┌─────┴─────┐
    ▼           ▼          ▼         ▼           ▼           ▼
┌─────────┐ ┌─────────┐ ┌───────┐ ┌───────┐ ┌─────────┐ ┌─────────┐
│ GC      │ │ JIT     │ │ Method│ │ Thread│ │ CPU     │ │ Memory  │
│ Events  │ │ Events  │ │ Events│ │ Events│ │ Events  │ │ Events  │
└─────────┘ └─────────┘ └───────┘ └───────┘ └─────────┘ └─────────┘
```

## 4. Event Structure and Relationships

Each JFR event has a common structure with specific attributes:

```
┌───────────────────────────────────────────────────────────────────┐
│                         JFR Event                                  │
├───────────────────────────────────────────────────────────────────┤
│ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────┐   │
│ │ Event Metadata  │ │ Event Fields    │ │ Stack Trace (opt)   │   │
│ └─────────────────┘ └─────────────────┘ └─────────────────────┘   │
└───────────────────────────────────────────────────────────────────┘
```

### Event Metadata Components

```
┌───────────────────────────────────────────────────────────────────┐
│                      Event Metadata                                │
├───────────────────┬───────────────────────────────────────────────┤
│ Event Type        │ Defines the type of event (e.g., GCHeapSummary)│
├───────────────────┼───────────────────────────────────────────────┤
│ Start Time        │ When the event started                         │
├───────────────────┼───────────────────────────────────────────────┤
│ Thread            │ Thread that generated the event                │
├───────────────────┼───────────────────────────────────────────────┤
│ Duration          │ How long the event lasted (if applicable)      │
└───────────────────┴───────────────────────────────────────────────┘
```

## 5. Event Type Relationships

Events in JFR can have relationships with each other:

```
┌───────────────────────────────────────────────────────────────────┐
│                    Event Relationships                             │
└───────────────────────────────────────────────────────────────────┘
                              │
          ┌──────────────────┼──────────────────────┐
          ▼                   ▼                     ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐
│ Causal          │  │ Temporal        │  │ Contextual              │
│ Relationships   │  │ Relationships   │  │ Relationships           │
└─────────────────┘  └─────────────────┘  └─────────────────────────┘
        │                    │                        │
        ▼                    ▼                        ▼
┌──────────────────┐ ┌──────────────────┐  ┌───────────────────────┐
│ Parent-Child     │ │ Before-After     │  │ Thread-based          │
│ (e.g., GC caused │ │ (e.g., Method A  │  │ (e.g., Events from    │
│  by allocation)  │ │  called Method B) │  │  the same thread)     │
└──────────────────┘ └──────────────────┘  └───────────────────────┘
```

## 6. Common JFR Event Types

```
┌───────────────────────────────────────────────────────────────────┐
│                     Common JFR Event Types                         │
├───────────────────┬───────────────────────────────────────────────┤
│ jdk.GCHeapSummary │ Heap statistics before and after GC           │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.CPULoad       │ CPU utilization for JVM process               │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.ThreadStart   │ Thread creation events                         │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.ThreadEnd     │ Thread termination events                      │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.JavaMonitorEnter│ Monitor entry events (synchronization)       │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.JavaMonitorWait│ Monitor wait events                           │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.SocketRead    │ Socket read operations                         │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.SocketWrite   │ Socket write operations                        │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.FileRead      │ File read operations                           │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.FileWrite     │ File write operations                          │
├───────────────────┼───────────────────────────────────────────────┤
│ jdk.Compilation   │ JIT compilation events                         │
└───────────────────┴───────────────────────────────────────────────┘
```

## 7. Data Flow in JFR Recording

The following diagram illustrates how data flows through the JFR system:

```
┌───────────────────────────────────────────────────────────────────┐
│                     JFR Data Flow                                  │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                     Event Producers                                │
│  (JVM, Java Application, JDK Libraries, Custom Instrumentation)    │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                     JFR Buffer                                     │
│             (In-memory circular buffer for events)                 │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                     JFR Recorder Thread                            │
│             (Writes events from buffer to disk)                    │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                     JFR File                                       │
│             (Persistent storage of events)                         │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│                     JFR Consumers                                  │
│       (JMC, jfrdiff, Custom Analysis Tools, JFR API)              │
└───────────────────────────────────────────────────────────────────┘
```

## 8. JFR Configuration and Settings

JFR recordings can be configured with different settings:

```
┌───────────────────────────────────────────────────────────────────┐
│                     JFR Configuration                              │
├───────────────────┬───────────────────────────────────────────────┤
│ Recording Level   │ profile, normal, minimal                       │
├───────────────────┼───────────────────────────────────────────────┤
│ Duration          │ Time-bound or continuous recording             │
├───────────────────┼───────────────────────────────────────────────┤
│ Disk Persistence  │ Whether to write to disk or memory-only        │
├───────────────────┼───────────────────────────────────────────────┤
│ Event Settings    │ Which events to record and their thresholds    │
├───────────────────┼───────────────────────────────────────────────┤
│ Stack Depth       │ How many frames to capture in stack traces     │
└───────────────────┴───────────────────────────────────────────────┘
```

## 9. JFR API Integration

JFR provides APIs for custom event creation and consumption:

```
┌───────────────────────────────────────────────────────────────────┐
│                     JFR API Components                             │
├───────────────────┬───────────────────────────────────────────────┤
│ Event Class       │ Custom event definition with @Name annotation  │
├───────────────────┼───────────────────────────────────────────────┤
│ Event Fields      │ Data fields with @Label annotations            │
├───────────────────┼───────────────────────────────────────────────┤
│ Recording         │ API to start/stop recordings                   │
├───────────────────┼───────────────────────────────────────────────┤
│ Consumer API      │ RecordingFile and EventStream for reading      │
└───────────────────┴───────────────────────────────────────────────┘
```

## 10. JFR Event Lifecycle

```
┌───────────────────────────────────────────────────────────────────┐
│                     JFR Event Lifecycle                            │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│ 1. Event Creation                                                  │
│    - Instantiate event object                                      │
│    - Set event fields                                              │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│ 2. Event Begin (for duration events)                               │
│    - Record start time                                             │
│    - Capture initial state                                         │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│ 3. Event Commit                                                    │
│    - Record end time (for duration events)                         │
│    - Capture stack trace (if enabled)                              │
│    - Write to JFR buffer                                           │
└───────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────┐
│ 4. Event Storage                                                   │
│    - Event data stored in memory buffer                            │
│    - Eventually written to disk by JFR system                      │
└───────────────────────────────────────────────────────────────────┘
```

## Summary

Java Flight Recorder (JFR) provides a comprehensive event recording system for the JVM with a rich set of built-in events and the ability to define custom events. The file format efficiently stores temporal data with precise timestamps and durations, allowing for detailed performance analysis and troubleshooting.

The hierarchical organization of events by category, along with the relationships between events, enables sophisticated analysis of application behavior across different subsystems and time periods.