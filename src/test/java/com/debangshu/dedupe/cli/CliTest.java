package com.debangshu.dedupe.cli;

import com.debangshu.dedupe.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the real commands like a user would, but in-process, and checks the files they write. */
class CliTest {

    @TempDir
    Path dir;

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    private int run(String... args) {
        CommandLine cli = Main.commandLine();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));
        return cli.execute(args);
    }

    private Path generateSample(int people) {
        Path file = dir.resolve("sample.csv");
        assertThat(run("generate", "--people", String.valueOf(people), "--seed", "11", "-o", file.toString())).isZero();
        return file;
    }

    @Test
    void generateWritesACsvWithATruthColumn() throws IOException {
        Path file = generateSample(300);

        List<String> lines = Files.readAllLines(file);
        assertThat(lines.getFirst()).isEqualTo("id,first_name,last_name,email,phone,company,city,updated_at,true_entity_id");
        assertThat(lines.size() - 1).isGreaterThan(300);
        assertThat(out.toString()).contains("for 300 people");
    }

    @Test
    void findWritesTheThreeOutputFiles() throws IOException {
        Path input = generateSample(500);
        Path output = dir.resolve("result");

        int exit = run("find", "-i", input.toString(), "-o", output.toString());

        assertThat(exit).isZero();
        assertThat(output.resolve("merged.csv")).exists();
        assertThat(output.resolve("clusters.csv")).exists();
        assertThat(output.resolve("review.csv")).exists();
        assertThat(out.toString()).contains("Found").contains("duplicate rows").contains("Compared");

        // merged.csv has one row per person that is left, clusters.csv lists the rows that were merged
        long inputRows = Files.readAllLines(input).size() - 1;
        long mergedRows = Files.readAllLines(output.resolve("merged.csv")).size() - 1;
        assertThat(mergedRows).isLessThan(inputRows).isGreaterThanOrEqualTo(500 * 9 / 10);
    }

    @Test
    void evaluatePrintsPrecisionAndRecallPerThreshold() {
        Path input = generateSample(500);

        int exit = run("evaluate", "-i", input.toString());

        assertThat(exit).isZero();
        assertThat(out.toString())
                .contains("Blocking let through")
                .contains("threshold  precision  recall")
                .contains("<- default");
    }

    @Test
    void evaluateCanCompareEveryPair() {
        Path input = generateSample(200);

        assertThat(run("evaluate", "-i", input.toString(), "--exhaustive", "--sequential")).isZero();
        assertThat(out.toString()).contains("Compared all").contains("1 thread");
    }

    @Test
    void badInputFileExitsWithCode2AndAMessage() throws IOException {
        Path input = dir.resolve("no-id.csv");
        Files.writeString(input, "name,email\nRahul,rahul@x.com\n");

        int exit = run("find", "-i", input.toString(), "-o", dir.resolve("out").toString());

        assertThat(exit).isEqualTo(2);
        assertThat(err.toString()).contains("error:").contains("needs an 'id' column");
    }

    @Test
    void evaluateWithoutTruthColumnExitsWithCode2() throws IOException {
        Path input = dir.resolve("plain.csv");
        Files.writeString(input, "id,first_name,last_name\nC1,Rahul,Sharma\n");

        assertThat(run("evaluate", "-i", input.toString())).isEqualTo(2);
        assertThat(err.toString()).contains("true_entity_id");
    }

    @Test
    void invalidOptionsAreRejected() {
        assertThat(run("find", "-i", "x.csv", "--threshold", "1.5")).isEqualTo(2);
        assertThat(err.toString()).contains("between 0 and 1");
    }

    @Test
    void noCommandShowsAnError() {
        assertThat(run()).isEqualTo(2);
        assertThat(err.toString()).contains("Missing command");
    }

    @Test
    void versionAndHelp() {
        assertThat(run("--version")).isZero();
        assertThat(out.toString()).contains("crm-dedupe 0.1.0");

        assertThat(run("find", "--help")).isZero();
        assertThat(out.toString()).contains("--threshold").contains("--review-threshold");
    }
}
