# Java Flight Recorder (JFR) Glyphs

This document provides a set of unicode glyphs that symbolize key concepts in Java Flight Recorder (JFR) semantics.

## File Structure Components

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| JFR Recording File | 📊 | 📁 📄 📃 📑 📈 | Complete JFR recording file |
| Metadata | 📝 | 📋 🏷️ 📌 ℹ️ 🔍 | Contains event type definitions and recording information |
| Constant Pool | 🔄 | 🔁 ♻️ 🔃 🔀 💱 | Stores constant values for deduplication |
| Event Data | 📈 | 📊 📉 📇 📋 📅 | The actual recorded events with timestamps and values |

## Temporal Types

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| Timestamp | ⏱️ | ⏰ 🕰️ 🕓 ⌚ 📅 | Absolute time when an event occurred |
| Duration | ⏳ | ⌛ ⏲️ 🕙 ⏰ 📏 | Time span between two points |
| Timespan | 🕒 | 🕓 🕔 🕕 🕖 🕗 | Similar to Duration but with different semantic meaning |
| Ticks | ⚡ | ⏲️ ⚙️ 🔄 ⏱️ 🔂 | High-resolution timing unit for precise measurements |
| Start Timestamp | 🏁 | 🚩 🚀 ▶️ 🎬 🎯 | Beginning of a timed event |
| End Timestamp | 🏆 | 🎯 🛑 ⏹️ 🔚 🏁 | Completion of a timed event |

## Event Categories

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| JVM Events | 🔧 | ☕ 🖥️ 🔨 🛠️ 🔩 | Events related to the Java Virtual Machine |
| Java App Events | 📱 | 💻 📲 📴 📳 🖥️ | Events from the Java application |
| OS Events | 💻 | 🖥️ 🖱️ ⌨️ 🖲️ 🔌 | Events from the operating system |
| GC Events | ♻️ | 🗑️ 🧹 🚮 🔄 🧽 | Garbage collection events |
| JIT Events | 🚀 | 🔥 ⚡ 🏎️ 🛩️ 🔆 | Just-In-Time compilation events |
| Method Events | 📞 | 📲 📳 📴 🔊 📢 | Method invocation events |
| Thread Events | 🧵 | 🧶 🔄 🧠 🧩 🔀 | Thread lifecycle events |
| CPU Events | ⚙️ | 🔧 🔨 🛠️ 🔩 🧰 | CPU utilization events |
| Memory Events | 🧠 | 💾 💿 🧮 📊 📈 | Memory usage events |

## Event Structure

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| JFR Event | 📋 | 📝 📄 📃 📜 📊 | Generic JFR event |
| Event Metadata | 🏷️ | 📌 ℹ️ 📎 🔖 📝 | Metadata about an event |
| Event Fields | 📑 | 📋 📁 📂 📇 📊 | Data fields within an event |
| Stack Trace | 📚 | 📜 📃 📄 📋 📊 | Call stack at the time of the event |
| Event Type | 📌 | 🏷️ 📎 🔖 📝 📋 | Defines the type of event |
| Thread | 🧶 | 🧵 🔄 🧠 🧩 🔀 | Thread that generated the event |

## Event Relationships

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| Causal Relationships | 🔗 | ⛓️ 🔄 🔁 📎 🧩 | One event causing another |
| Temporal Relationships | ⏰ | ⏱️ ⌚ 🕰️ 📅 ⌛ | Time-based relationships between events |
| Contextual Relationships | 🌐 | 🔍 🧩 🌍 🌎 🌏 | Context-based relationships |
| Parent-Child | 👨‍👦 | 🌳 🌲 📊 📑 🧩 | Hierarchical relationship between events |
| Before-After | ⏪⏩ | ◀️▶️ ⬅️➡️ 🔙🔜 ⏮️⏭️ 🔄 | Sequential relationship between events |
| Thread-based | 🧵 | 🧶 🔄 🧠 🧩 🔀 | Events from the same thread |

## Common Event Types

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| GCHeapSummary | 🗑️ | ♻️ 🧹 🚮 🔄 🧽 | Heap statistics before and after GC |
| CPULoad | 📈 | 📊 📉 ⚙️ 🔧 🖥️ | CPU utilization for JVM process |
| ThreadStart/End | 🧵 | 🧶 🔄 🧠 🧩 🔀 | Thread creation and termination events |
| JavaMonitor | 🔒 | 🔐 🔓 🔑 🔏 🔍 | Monitor entry and wait events |
| Socket Operations | 🔌 | 🌐 📡 📶 📱 💻 | Socket read/write operations |
| File Operations | 📂 | 📁 📄 📃 📝 💾 | File read/write operations |
| Compilation | 🏭 | 🔨 🛠️ 🔧 ⚙️ 🚀 | JIT compilation events |

## Data Flow

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| Event Producers | 🏭 | 🔨 🛠️ 🔧 ⚙️ 🚀 | Sources that generate JFR events |
| JFR Buffer | 💾 | 🧮 📋 📊 🔄 💿 | In-memory circular buffer for events |
| JFR Recorder Thread | 📼 | 🎥 🎬 📹 📽️ 🎞️ | Writes events from buffer to disk |
| JFR File | 📁 | 📂 📄 📃 📝 💾 | Persistent storage of events |
| JFR Consumers | 🔍 | 🔎 👁️ 👀 📊 📈 | Tools that analyze JFR data |

## Configuration

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| Recording Level | 🎚️ | 🎛️ 📊 📈 📉 🔢 | Profile, normal, minimal settings |
| Duration | ⏲️ | ⏱️ ⏳ ⌛ 🕰️ ⌚ | Time-bound or continuous recording |
| Disk Persistence | 💽 | 💾 💿 📀 🖴 📁 | Whether to write to disk or memory-only |
| Event Settings | ⚙️ | 🔧 🛠️ 🔨 🔩 🧰 | Which events to record and their thresholds |
| Stack Depth | 📏 | 📐 📊 📈 📉 📚 | How many frames to capture in stack traces |

## API Components

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| Event Class | 📜 | 📄 📃 📝 📋 📑 | Custom event definition |
| Event Fields | 📋 | 📝 📄 📃 📑 📊 | Data fields with annotations |
| Recording | ⏺️ | 🎬 🎥 📹 📼 🎞️ | API to start/stop recordings |
| Consumer API | 📥 | 📤 📨 📩 📫 📬 | For reading JFR data |

## Event Lifecycle

| Concept | Glyph | Alternative Glyphs | Description |
|---------|-------|-------------------|-------------|
| Event Creation | 🌱 | 🌿 🌲 🌳 🌴 🌵 | Instantiate event object and set fields |
| Event Begin | 🏁 | 🚩 🚀 ▶️ 🎬 🎯 | Record start time and initial state |
| Event Commit | ✅ | ✓ ☑️ 📝 💾 📤 | Record end time and write to buffer |
| Event Storage | 📦 | 📂 💾 💿 🗄️ 🗃️ | Store in memory and write to disk |
