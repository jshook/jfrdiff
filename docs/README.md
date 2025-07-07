# JFR Documentation

This directory contains documentation about Java Flight Recorder (JFR) file semantics and usage.

## Contents

- [JFR_SEMANTICS.md](JFR_SEMANTICS.md) - Comprehensive documentation of JFR file structure, temporal types, relationships, and event categories with diagrams

## Purpose

The documentation in this directory provides a detailed understanding of JFR files, which is essential for:

1. Understanding the data processed by the jfrdiff tool
2. Interpreting JFR comparison results correctly
3. Extending the tool with new features
4. Troubleshooting performance issues using JFR data

## How to Use This Documentation

- Start with the JFR_SEMANTICS.md file for a comprehensive overview of JFR file structure and semantics
- Refer to the diagrams to understand the relationships between different components
- Use the information about event types to better understand the comparison output from jfrdiff

## Additional Resources

For more information about Java Flight Recorder, refer to:

- [JDK Mission Control Documentation](https://docs.oracle.com/en/java/java-components/jdk-mission-control/)
- [JEP 328: Flight Recorder](https://openjdk.org/jeps/328)
- [Java Flight Recorder Runtime Guide](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm)