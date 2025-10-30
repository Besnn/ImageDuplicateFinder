package app;

import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.Arrays;

@CommandLine.Command(
        name = "idf",
        mixinStandardHelpOptions = true,
        version = "0.0.1",
        description = "Image Duplicate Finder",
        subcommands = {
                Commands.Hash.class,
                Commands.Cluster.class,
                Commands.Plan.class,
                Commands.Apply.class
        }
)
public class CLI implements Callable<Integer> {
    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return Exit.OK;
    }

    public static void main(String[] args) {
        System.out.println(java.util.Arrays.toString(args));
        CommandLine cmd = new CommandLine(new CLI());
        cmd.addSubcommand("hash", new Commands.Hash());
        cmd.addSubcommand("cluster", new Commands.Cluster());
        int code = cmd.execute(args);
        System.exit(code);
    }

    public static final class Exit {
        public static final int OK = 0;
        public static final int USAGE = 2;
        public static final int RUNTIME_ERROR = 1;
    }
}

//TODO: create dataset for testing
