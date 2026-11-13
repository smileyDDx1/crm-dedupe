package com.debangshu.dedupe.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(name = "crm-dedupe",
        mixinStandardHelpOptions = true,
        version = "crm-dedupe 0.1.0",
        description = "Finds duplicate contacts in a CRM export (CSV) with fuzzy matching.",
        subcommands = {FindCommand.class, GenerateCommand.class, EvaluateCommand.class})
public class DedupeCli implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        throw new ParameterException(spec.commandLine(), "Missing command: use find, generate or evaluate (see --help)");
    }
}
