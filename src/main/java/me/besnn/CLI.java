package me.besnn;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "idf", mixinStandardHelpOptions = true,
        version = "0.0.1",
        description = "for now asdf")
public class CLI implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        return 0;
    }
}

//TODO: implement
