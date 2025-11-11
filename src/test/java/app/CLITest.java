package app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class CLITest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private CommandLine cmd;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        cmd = new CommandLine(new CLI());
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("CLI.call() should print usage and return OK")
    void cliCall_shouldPrintUsageAndReturnOk() {
        CLI cli = new CLI();
        Integer exitCode = cli.call();
        assertEquals(CLI.Exit.OK, exitCode, "Exit code should be OK");
        assertTrue(outContent.toString().contains("Usage: idf [-hV] [COMMAND]"), "Should print usage information");
    }

    @Test
    @DisplayName("Running with no arguments should print usage")
    void runWithoutArguments_shouldPrintUsage() {
        int exitCode = cmd.execute();
        assertEquals(CLI.Exit.OK, exitCode);
        String output = outContent.toString();
        assertTrue(output.contains("Image Duplicate Finder"));
        assertTrue(output.contains("Usage: idf"));
    }

    @Test
    @DisplayName("Running with --help option should print help and exit with OK")
    void runWithHelpOption_shouldPrintHelp() {
        int exitCode = cmd.execute("--help");
        assertEquals(CLI.Exit.OK, exitCode);
        String output = outContent.toString();
        assertTrue(output.contains("Usage: idf [-hV] [COMMAND]"));
//        assertTrue(output.contains("Options:"));
        assertTrue(output.contains("Commands:"));
    }

    @Test
    @DisplayName("Running with --version option should print version and exit with OK")
    void runWithVersionOption_shouldPrintVersion() {
        int exitCode = cmd.execute("--version");
        assertEquals(CLI.Exit.OK, exitCode);
        String output = outContent.toString().trim();
        assertTrue(output.contains("0.0.1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hash", "cluster", "plan", "apply"})
    @DisplayName("Should have required subcommands registered")
    void shouldHaveSubcommandRegistered(String subcommand) {
        assertNotNull(cmd.getSubcommands().get(subcommand), "The '" + subcommand + "' subcommand should be registered.");
    }

    @Test
    @DisplayName("Running with an unknown command should return usage error code")
    void runWithUnknownCommand_shouldReturnError() {
        int exitCode = cmd.execute("unknown-command");
        assertEquals(CLI.Exit.USAGE, exitCode);
    }
}