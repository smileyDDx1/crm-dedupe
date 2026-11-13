package com.debangshu.dedupe.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocking: decides which pairs of rows are worth comparing at all.
 *
 * Comparing every row with every other row is n*(n-1)/2 comparisons: 1.25 billion for 50,000 rows.
 * Instead every row gets a few "blocking keys", and only rows that share a key are compared:
 *
 *   e:  normalized email           (same inbox)
 *   p:  normalized phone           (same number, any format)
 *   n1: soundex(last name) + first letter of first name   ("Sharma, Rahul" / "Sarma, Rahul")
 *   n2: soundex(first name) + first letter of last name   (catches a typo at the start of the last name)
 *
 * The trade-off: a duplicate that shares no key with its twin is never compared, so it's missed.
 * The evaluate command measures how often that happens ("blocking recall").
 */
public final class Blocker {

    private final int maxBlockSize;

    /**
     * @param maxBlockSize blocks bigger than this are skipped. A key like "n1:K500R" (Kumar, R...) can be
     *                     shared by thousands of rows in a big CRM, and one such block alone would be
     *                     millions of comparisons with almost no real duplicates in it.
     */
    public Blocker(int maxBlockSize) {
        this.maxBlockSize = maxBlockSize;
    }

    public record Result(long[] pairs, int blocks, int skippedBlocks) {
    }

    static List<String> keysFor(NormalizedContact contact) {
        List<String> keys = new ArrayList<>(4);
        if (!contact.email().isEmpty()) {
            keys.add("e:" + contact.email());
        }
        if (!contact.phone().isEmpty()) {
            keys.add("p:" + contact.phone());
        }
        if (!contact.firstName().isEmpty() && !contact.lastName().isEmpty()) {
            keys.add("n1:" + Soundex.encode(contact.lastName()) + contact.firstName().charAt(0));
            keys.add("n2:" + Soundex.encode(contact.firstName()) + contact.lastName().charAt(0));
        }
        return keys;
    }

    public Result candidatePairs(List<NormalizedContact> contacts) {
        Map<String, List<Integer>> blocks = new HashMap<>();
        for (int i = 0; i < contacts.size(); i++) {
            for (String key : keysFor(contacts.get(i))) {
                blocks.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }

        CandidatePairs pairs = new CandidatePairs();
        int skipped = 0;
        for (List<Integer> block : blocks.values()) {
            if (block.size() < 2) {
                continue;
            }
            if (block.size() > maxBlockSize) {
                skipped++;
                continue;
            }
            for (int a = 0; a < block.size(); a++) {
                for (int b = a + 1; b < block.size(); b++) {
                    pairs.add(block.get(a), block.get(b));
                }
            }
        }
        return new Result(pairs.toSortedUniqueArray(), blocks.size(), skipped);
    }
}
