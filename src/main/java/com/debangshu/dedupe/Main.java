package com.debangshu.dedupe;

import com.debangshu.dedupe.cli.DedupeCli;
import picocli.CommandLine;

public final class Main {

    /** Exit codes: 0 ok, 1 unexpected error, 2 bad input or bad options. */
    static final int BAD_INPUT = 2;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    /** Separate from main() so tests can run commands and read the output without exiting the JVM. */
    public static CommandLine commandLine() {
        CommandLine commandLine = new CommandLine(new DedupeCli());
        commandLine.setExecutionExceptionHandler((exception, cmd, parseResult) -> {
            cmd.getErr().println("error: " + exception.getMessage());
            cmd.getErr().flush();
            return exception instanceof DedupeInputException ? BAD_INPUT : 1;
        });
        return commandLine;
    }
}
