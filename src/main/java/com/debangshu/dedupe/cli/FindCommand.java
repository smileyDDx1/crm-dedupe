package com.debangshu.dedupe.cli;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.io.ContactCsvReader;
import com.debangshu.dedupe.io.ResultWriter;
import com.debangshu.dedupe.pipeline.DedupeResult;
import com.debangshu.dedupe.pipeline.DedupeStats;
import com.debangshu.dedupe.pipeline.Deduplicator;
import com.debangshu.dedupe.pipeline.MatchSettings;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static com.debangshu.dedupe.cli.Format.number;
import static com.debangshu.dedupe.cli.Format.percent;
import static com.debangshu.dedupe.cli.Format.seconds;

@Command(name = "find", mixinStandardHelpOptions = true,
        description = "Find duplicates in a CSV and write merged.csv, clusters.csv and review.csv.")
class FindCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"-i", "--input"}, required = true, description = "CSV export with a header row and an id column")
    Path input;

    @Option(names = {"-o", "--output-dir"}, defaultValue = "dedupe-output", description = "Where to write the results (default: ${DEFAULT-VALUE})")
    Path outputDir;

    @Option(names = "--threshold", defaultValue = "" + MatchSettings.DEFAULT_MATCH_THRESHOLD,
            description = "Score needed to merge two rows, 0-1 (default: ${DEFAULT-VALUE})")
    double threshold;

    @Option(names = "--review-threshold", defaultValue = "" + MatchSettings.DEFAULT_REVIEW_THRESHOLD,
            description = "Pairs between this and --threshold go to review.csv (default: ${DEFAULT-VALUE})")
    double reviewThreshold;

    @Option(names = "--max-block-size", defaultValue = "" + MatchSettings.DEFAULT_MAX_BLOCK_SIZE,
            description = "Skip blocking keys shared by more rows than this (default: ${DEFAULT-VALUE})")
    int maxBlockSize;

    @Override
    public Integer call() throws IOException {
        MatchSettings settings = settings();
        List<Contact> contacts = ContactCsvReader.read(input);
        DedupeResult result = new Deduplicator(settings).run(contacts);
        ResultWriter.write(result, outputDir);

        DedupeStats stats = result.stats();
        PrintWriter out = spec.commandLine().getOut();
        out.printf("Read %s rows from %s%n", number(stats.rows()), input.getFileName());
        out.printf("Compared %s pairs instead of %s (%s fewer%s)%n", number(stats.comparedPairs()), number(stats.allPairs()),
                percent(stats.comparisonsSavedPercent()),
                stats.skippedBlocks() > 0 ? ", " + stats.skippedBlocks() + " oversized blocks skipped" : "");
        out.printf("Found %s duplicate rows in %s groups -> %s rows left%n", number(stats.duplicateRows()),
                number(stats.clusters()), number(stats.rows() - stats.duplicateRows()));
        out.printf("%s pairs need a human look (%s)%n", number(stats.reviewPairs()), ResultWriter.REVIEW);
        out.printf("Wrote %s%n", outputDir.resolve(ResultWriter.MERGED) + ", " + outputDir.resolve(ResultWriter.CLUSTERS)
                + ", " + outputDir.resolve(ResultWriter.REVIEW));
        out.printf("Time: %s (normalize %s, blocking %s, scoring %s, grouping %s)%n", seconds(stats.totalMs()),
                seconds(stats.normalizeMs()), seconds(stats.blockingMs()), seconds(stats.scoringMs()), seconds(stats.clusteringMs()));
        out.flush();
        return 0;
    }

    private MatchSettings settings() {
        try {
            return new MatchSettings(threshold, reviewThreshold, maxBlockSize, false, true);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage());
        }
    }
}
