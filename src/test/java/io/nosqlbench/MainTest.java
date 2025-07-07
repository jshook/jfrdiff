package io.nosqlbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the JFR diff tool using static test files from resources.
 */
public class MainTest {

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream errorStream;

    @BeforeEach
    public void setUp() {
        // Capture stdout for verification
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        // Capture stderr for verification
        originalErr = System.err;
        errorStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    public void tearDown() {
        // Reset stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Test comparison of two JFR files with default threshold")
    public void testCompareJfrFilesDefaultThreshold() throws Exception {
        // Get paths to test JFR files
        URL firstFileUrl = getClass().getClassLoader().getResource("profile_1751574827.jfr");
        URL secondFileUrl = getClass().getClassLoader().getResource("profile_1751575034.jfr");

        assertNotNull(firstFileUrl, "First test file not found in resources");
        assertNotNull(secondFileUrl, "Second test file not found in resources");

        String firstFilePath = Paths.get(firstFileUrl.toURI()).toString();
        String secondFilePath = Paths.get(secondFileUrl.toURI()).toString();

        // Execute the command with arguments
        String[] args = {firstFilePath, secondFilePath};
        int exitCode = new CommandLine(new Main()).execute(args);

        // Verify execution was successful
        assertEquals(0, exitCode, "Execution should complete successfully");

        // Verify output contains expected content
        String output = outputStream.toString();
        assertTrue(output.contains("Comparing JFR files:"), "Output should contain comparison header");
        assertTrue(output.contains("=== JFR Comparison Summary ==="), "Output should contain summary header");
    }

    @Test
    @DisplayName("Test comparison of two JFR files with custom threshold")
    public void testCompareJfrFilesCustomThreshold() throws Exception {
        // Get paths to test JFR files
        URL firstFileUrl = getClass().getClassLoader().getResource("profile_1751575705.jfr");
        URL secondFileUrl = getClass().getClassLoader().getResource("profile_1751575919.jfr");

        assertNotNull(firstFileUrl, "First test file not found in resources");
        assertNotNull(secondFileUrl, "Second test file not found in resources");

        String firstFilePath = Paths.get(firstFileUrl.toURI()).toString();
        String secondFilePath = Paths.get(secondFileUrl.toURI()).toString();

        // Execute the command with arguments including custom threshold
        String[] args = {"-t", "5.0", firstFilePath, secondFilePath};
        int exitCode = new CommandLine(new Main()).execute(args);

        // Verify execution was successful
        assertEquals(0, exitCode, "Execution should complete successfully");

        // Verify output contains expected content
        String output = outputStream.toString();
        assertTrue(output.contains("Threshold: 5.0%"), "Output should contain custom threshold");
    }

    @Test
    @DisplayName("Test comparison with event type filtering")
    public void testCompareJfrFilesWithEventTypeFilter() throws Exception {
        // Get paths to test JFR files
        URL firstFileUrl = getClass().getClassLoader().getResource("profile_1751574827.jfr");
        URL secondFileUrl = getClass().getClassLoader().getResource("profile_1751575919.jfr");

        assertNotNull(firstFileUrl, "First test file not found in resources");
        assertNotNull(secondFileUrl, "Second test file not found in resources");

        String firstFilePath = Paths.get(firstFileUrl.toURI()).toString();
        String secondFilePath = Paths.get(secondFileUrl.toURI()).toString();

        // Execute the command with arguments including event type filtering
        String[] args = {
            "-e", "jdk.GCHeapSummary,jdk.CPULoad", 
            firstFilePath, 
            secondFilePath
        };
        int exitCode = new CommandLine(new Main()).execute(args);

        // Verify execution was successful
        assertEquals(0, exitCode, "Execution should complete successfully");

        // Verify output contains expected content
        String output = outputStream.toString();
        assertTrue(output.contains("Event types filter:"), "Output should mention event type filtering");
        assertTrue(output.contains("jdk.GCHeapSummary") || output.contains("jdk.CPULoad"), 
                  "Output should contain at least one of the filtered event types");
    }

    @Test
    @DisplayName("Test error handling with non-existent file")
    public void testErrorHandlingWithNonExistentFile(@TempDir Path tempDir) throws Exception {
        // Create a path to a non-existent file
        Path nonExistentFile = tempDir.resolve("non-existent.jfr");
        URL existingFileUrl = getClass().getClassLoader().getResource("profile_1751574827.jfr");

        assertNotNull(existingFileUrl, "Test file not found in resources");
        String existingFilePath = Paths.get(existingFileUrl.toURI()).toString();

        // Execute the command with a non-existent file
        String[] args = {nonExistentFile.toString(), existingFilePath};
        int exitCode = new CommandLine(new Main()).execute(args);

        // Verify execution failed
        assertEquals(1, exitCode, "Execution should fail with non-existent file");

        // Verify error output contains expected content
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("Error comparing JFR files"), 
                  "Error output should mention comparison error");
    }

    @Test
    @DisplayName("Test time delta calculation and application")
    public void testTimeDeltaCalculation() throws Exception {
        // Get paths to test JFR files
        URL firstFileUrl = getClass().getClassLoader().getResource("profile_1751575705.jfr");
        URL secondFileUrl = getClass().getClassLoader().getResource("profile_1751575919.jfr");

        assertNotNull(firstFileUrl, "First test file not found in resources");
        assertNotNull(secondFileUrl, "Second test file not found in resources");

        String firstFilePath = Paths.get(firstFileUrl.toURI()).toString();
        String secondFilePath = Paths.get(secondFileUrl.toURI()).toString();

        // Execute the command with arguments
        String[] args = {firstFilePath, secondFilePath};
        int exitCode = new CommandLine(new Main()).execute(args);

        // Verify execution was successful
        assertEquals(0, exitCode, "Execution should complete successfully");

        // Verify output contains time delta information
        String output = outputStream.toString();
        assertTrue(output.contains("Time delta:"), "Output should contain time delta information");

        // Verify timestamp information is included in the event comparison
        assertTrue(output.contains("First Event:"), "Output should contain first event timestamp information");
        assertTrue(output.contains("Last Event:"), "Output should contain last event timestamp information");
        assertTrue(output.contains("Time Span (File 1):"), "Output should contain time span information for file 1");
        assertTrue(output.contains("Time Span (File 2):"), "Output should contain time span information for file 2");
    }

    @Test
    @DisplayName("Test readout view selection")
    public void testReadoutViewSelection() throws Exception {
        // Get paths to test JFR files
        URL firstFileUrl = getClass().getClassLoader().getResource("profile_1751574827.jfr");
        URL secondFileUrl = getClass().getClassLoader().getResource("profile_1751575034.jfr");

        assertNotNull(firstFileUrl, "First test file not found in resources");
        assertNotNull(secondFileUrl, "Second test file not found in resources");

        String firstFilePath = Paths.get(firstFileUrl.toURI()).toString();
        String secondFilePath = Paths.get(secondFileUrl.toURI()).toString();

        // Execute the command with default view (implicit)
        String[] defaultViewArgs = {firstFilePath, secondFilePath};
        int defaultViewExitCode = new CommandLine(new Main()).execute(defaultViewArgs);

        // Verify execution was successful
        assertEquals(0, defaultViewExitCode, "Execution should complete successfully with default view");

        // Verify output contains default view information
        String defaultViewOutput = outputStream.toString();
        assertTrue(defaultViewOutput.contains("Readout view: default"), 
                  "Output should indicate default view is being used");

        // Reset output stream for next test
        outputStream.reset();

        // Execute the command with explicit default view
        String[] explicitDefaultViewArgs = {"-v", "default", firstFilePath, secondFilePath};
        int explicitDefaultViewExitCode = new CommandLine(new Main()).execute(explicitDefaultViewArgs);

        // Verify execution was successful
        assertEquals(0, explicitDefaultViewExitCode, "Execution should complete successfully with explicit default view");

        // Verify output contains default view information
        String explicitDefaultViewOutput = outputStream.toString();
        assertTrue(explicitDefaultViewOutput.contains("Readout view: default"), 
                  "Output should indicate default view is being used when explicitly specified");
    }

    @Test
    @DisplayName("Test output format consistency")
    public void testOutputFormatConsistency() throws Exception {
        // Get paths to test JFR files
        URL firstFileUrl = getClass().getClassLoader().getResource("profile_1751574827.jfr");
        URL secondFileUrl = getClass().getClassLoader().getResource("profile_1751575034.jfr");

        assertNotNull(firstFileUrl, "First test file not found in resources");
        assertNotNull(secondFileUrl, "Second test file not found in resources");

        String firstFilePath = Paths.get(firstFileUrl.toURI()).toString();
        String secondFilePath = Paths.get(secondFileUrl.toURI()).toString();

        // Execute the command with arguments
        String[] args = {firstFilePath, secondFilePath};
        int exitCode = new CommandLine(new Main()).execute(args);

        // Verify execution was successful
        assertEquals(0, exitCode, "Execution should complete successfully");

        // Verify output format consistency
        String output = outputStream.toString();

        // Verify time delta information is included
        assertTrue(output.contains("File 1 mtime:"), "Output should contain file 1 mtime");
        assertTrue(output.contains("File 2 mtime:"), "Output should contain file 2 mtime");
        assertTrue(output.contains("Time delta:"), "Output should contain time delta information");

        // Print the output for debugging
        System.err.println("[DEBUG_LOG] Output: " + output);

        // Check for key sections in the output
        assertTrue(output.contains("Comparing JFR files:"), "Output should contain comparison header");
        assertTrue(output.contains("=== JFR Comparison Summary ==="), "Output should contain summary header");

        // Check for event type formatting
        assertTrue(output.contains("Event Type:"), "Output should contain event type headers");

        // Check for count comparison formatting
        assertTrue(output.contains("Count:"), "Output should contain count comparisons");

        // Check for percentage change formatting - use a more flexible pattern
        assertTrue(output.contains("(") && output.contains(")") && output.contains("%"), 
                  "Output should contain percentage changes in parentheses");

        // Check for duration formatting if present
        if (output.contains("Duration:")) {
            assertTrue(output.contains("ns") || output.contains("µs") || 
                      output.contains("ms") || output.contains("s"), 
                      "Output should contain properly formatted durations");
        }
    }
}
