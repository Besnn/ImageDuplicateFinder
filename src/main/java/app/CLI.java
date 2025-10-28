package app;

import core.Exif;
import core.Gray;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "idf", mixinStandardHelpOptions = true,
        version = "0.0.1",
        description = "for now asdf")
public class CLI implements Callable<Integer> {
    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return Exit.OK;
    }

    public static void main(String[] args) {
        int code = new CommandLine(new CLI()).execute(args);
        System.exit(code);
    }

    public static final class Exit {
        public static final int OK = 0;
        public static final int USAGE = 2;
        public static final int RUNTIME_ERROR = 1;
    }
}

//TODO: implement
