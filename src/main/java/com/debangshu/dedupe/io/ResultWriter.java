package com.debangshu.dedupe.io;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.pipeline.Cluster;
import com.debangshu.dedupe.pipeline.DedupeResult;
import com.debangshu.dedupe.pipeline.ScoredPair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Writes the three output files:
 *
 *   merged.csv   the cleaned list: one row per real person (golden records + rows that had no duplicate)
 *   clusters.csv every duplicate group with all its original rows, to check what was merged
 *   review.csv   "maybe" pairs that were not merged, for a human to decide
 */
public final class ResultWriter {

    public static final String MERGED = "merged.csv";
    public static final String CLUSTERS = "clusters.csv";
    public static final String REVIEW = "review.csv";

    private ResultWriter() {
    }

    public static void write(DedupeResult result, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        writeMerged(result, outputDir.resolve(MERGED));
        writeClusters(result, outputDir.resolve(CLUSTERS));
        writeReview(result, outputDir.resolve(REVIEW));
    }

    private static void writeMerged(DedupeResult result, Path file) throws IOException {
        Set<String> mergedIds = new HashSet<>();
        try (CSVPrinter csv = printer(file, "id", "merged_ids", "first_name", "last_name", "email", "phone",
                "company", "city", "updated_at")) {
            for (Cluster cluster : result.clusters()) {
                String ids = cluster.members().stream().map(Contact::id).collect(Collectors.joining("|"));
                printContact(csv, cluster.golden(), ids);
                cluster.members().forEach(member -> mergedIds.add(member.id()));
            }
            for (Contact contact : result.contacts()) {
                if (!mergedIds.contains(contact.id())) {
                    printContact(csv, contact, contact.id());
                }
            }
        }
    }

    private static void writeClusters(DedupeResult result, Path file) throws IOException {
        try (CSVPrinter csv = printer(file, "cluster_id", "kept", "id", "first_name", "last_name", "email",
                "phone", "company", "city", "updated_at")) {
            for (Cluster cluster : result.clusters()) {
                for (Contact member : cluster.members()) {
                    csv.printRecord(cluster.id(), member.id().equals(cluster.golden().id()) ? "yes" : "", member.id(),
                            member.firstName(), member.lastName(), member.email(), member.phone(), member.company(),
                            member.city(), member.updatedAt() == null ? "" : member.updatedAt());
                }
            }
        }
    }

    private static void writeReview(DedupeResult result, Path file) throws IOException {
        List<Contact> contacts = result.contacts();
        try (CSVPrinter csv = printer(file, "score", "id_1", "name_1", "email_1", "company_1", "id_2", "name_2",
                "email_2", "company_2")) {
            for (ScoredPair pair : result.reviewPairs()) {
                Contact a = contacts.get(pair.first());
                Contact b = contacts.get(pair.second());
                csv.printRecord(String.format(Locale.ROOT, "%.3f", pair.score()), a.id(), a.fullName(), a.email(),
                        a.company(), b.id(), b.fullName(), b.email(), b.company());
            }
        }
    }

    private static void printContact(CSVPrinter csv, Contact contact, String mergedIds) throws IOException {
        csv.printRecord(contact.id(), mergedIds, contact.firstName(), contact.lastName(), contact.email(),
                contact.phone(), contact.company(), contact.city(), contact.updatedAt() == null ? "" : contact.updatedAt());
    }

    private static CSVPrinter printer(Path file, String... header) throws IOException {
        Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        return new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(header).get());
    }
}
