package com.debangshu.dedupe.cli;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.generator.ContactGenerator;
import com.debangshu.dedupe.io.ContactCsvWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static com.debangshu.dedupe.cli.Format.number;

@Command(name = "generate", mixinStandardHelpOptions = true,
        description = "Generate a messy test CSV where the right answer is known (true_entity_id column).")
class GenerateCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"-o", "--output"}, required = true, description = "CSV file to write")
    Path output;

    @Option(names = "--people", defaultValue = "10000", description = "How many different real people (default: ${DEFAULT-VALUE})")
    int people;

    @Option(names = "--duplicate-rate", defaultValue = "0.25",
            description = "Share of people with 1-3 extra rows, 0-1 (default: ${DEFAULT-VALUE})")
    double duplicateRate;

    @Option(names = "--seed", defaultValue = "42", description = "Same seed = same file (default: ${DEFAULT-VALUE})")
    long seed;

    @Override
    public Integer call() throws IOException {
        if (people < 1 || duplicateRate < 0 || duplicateRate > 1) {
            throw new ParameterException(spec.commandLine(), "--people must be at least 1 and --duplicate-rate between 0 and 1");
        }
        List<Contact> contacts = new ContactGenerator(seed).generate(people, duplicateRate);
        ContactCsvWriter.write(output, contacts);
        spec.commandLine().getOut().printf("Wrote %s rows for %s people (%s duplicate rows) to %s%n",
                number(contacts.size()), number(people), number(contacts.size() - people), output);
        spec.commandLine().getOut().flush();
        return 0;
    }
}
