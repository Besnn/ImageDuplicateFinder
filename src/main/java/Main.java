import app.CLI;

import picocli.CommandLine;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String... args) {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }
}