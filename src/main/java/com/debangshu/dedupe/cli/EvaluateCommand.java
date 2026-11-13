package com.debangshu.dedupe.cli;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.evaluation.Evaluator;
import com.debangshu.dedupe.evaluation.Evaluator.BlockingQuality;
import com.debangshu.dedupe.evaluation.Evaluator.Metrics;
import com.debangshu.dedupe.io.ContactCsvReader;
import com.debangshu.dedupe.pipeline.DedupeResult;
import com.debangshu.dedupe.pipeline.DedupeStats;
import com.debangshu.dedupe.pipeline.Deduplicator;
import com.debangshu.dedupe.pipeline.MatchSettings;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import static com.debangshu.dedupe.cli.Format.number;
import static com.debangshu.dedupe.cli.Format.percent;
import static com.debangshu.dedupe.cli.Format.seconds;

/**
 * Runs the matching on a file with a true_entity_id column and prints precision/recall for a range of
 * thresholds. This is how the default threshold was chosen.
 */
@Command(name = "evaluate", mixinStandardHelpOptions = true,
        description = "Measure precision and recall on a generated file (needs the true_entity_id column).")
class EvaluateCommand implements Callable<Integer> {

    static final double[] THRESHOLDS = {0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95};

    @Spec
    CommandSpec spec;

    @Option(names = {"-i", "--input"}, required = true, description = "CSV with a true_entity_id column")
    Path input;

    @Option(names = "--max-block-size", defaultValue = "" + MatchSettings.DEFAULT_MAX_BLOCK_SIZE,
            description = "Skip blocking keys shared by more rows than this (default: ${DEFAULT-VALUE})")
    int maxBlockSize;

    @Option(names = "--exhaustive", description = "Compare every pair instead of blocking (slow, for small files)")
    boolean exhaustive;

    @Option(names = "--sequential", description = "Score on one thread (to compare with the parallel version)")
    boolean sequential;

    @Override
    public Integer call() throws IOException {
        List<Contact> contacts = ContactCsvReader.read(input);
        Evaluator.requireTruth(contacts);
        // keep every pair from the lowest threshold up, then group at each threshold separately
        MatchSettings settings = new MatchSettings(MatchSettings.DEFAULT_MATCH_THRESHOLD, THRESHOLDS[0],
                maxBlockSize, exhaustive, !sequential);
        DedupeResult result = new Deduplicator(settings).run(contacts);
        DedupeStats stats = result.stats();

        PrintWriter out = spec.commandLine().getOut();
        out.printf("%s rows (%s real people)%n", number(stats.rows()),
                number(contacts.stream().map(Contact::trueEntityId).distinct().count()));
        if (exhaustive) {
            out.printf("Compared all %s pairs (no blocking)%n", number(stats.allPairs()));
        } else {
            BlockingQuality blocking = Evaluator.blockingQuality(contacts, result.candidatePairs());
            out.printf("Compared %s pairs instead of %s (%s fewer)%n", number(stats.comparedPairs()),
                    number(stats.allPairs()), percent(stats.comparisonsSavedPercent()));
            out.printf("Blocking let through %s of %s real duplicate pairs (%s)%n", number(blocking.truePairsCompared()),
                    number(blocking.truePairs()), percent(100 * blocking.recall()));
        }
        out.printf("Scoring took %s on %s%n", seconds(stats.scoringMs()), sequential ? "1 thread" : "all CPU cores");
        out.println();
        out.println("threshold  precision  recall   F1       merged pairs  correct");
        for (double threshold : THRESHOLDS) {
            Metrics m = Evaluator.atThreshold(contacts, result.scoredPairs(), threshold);
            out.printf(Locale.ROOT, "%-9.2f  %-9s  %-7s  %-7s  %-12s  %s%s%n", threshold, percent(100 * m.precision()),
                    percent(100 * m.recall()), percent(100 * m.f1()), number(m.predictedPairs()), number(m.correctPairs()),
                    threshold == MatchSettings.DEFAULT_MATCH_THRESHOLD ? "   <- default" : "");
        }
        out.flush();
        return 0;
    }
}
