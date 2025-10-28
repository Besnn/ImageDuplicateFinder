import app.CLI;

import picocli.CommandLine;

import java.util.Arrays;

public class Main {
    public static void main(String... args) throws Exception {
//        ExifDebugFixed.main(args);
        System.out.println(Arrays.toString(args));
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }
}
