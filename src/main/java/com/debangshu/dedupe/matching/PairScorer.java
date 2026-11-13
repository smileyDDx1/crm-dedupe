package com.debangshu.dedupe.matching;

/**
 * Gives a pair of contacts a score from 0 to 1: how sure are we that they are the same person?
 *
 * The score adds up points for evidence, a simplified version of the Fellegi-Sunter model used for
 * record linkage. A field that is empty in either row gives no points either way: a missing phone says
 * nothing about whether two rows match.
 *
 *   name      up to +0.60  Jaro-Winkler of "first last" (also tried as "last first")
 *   email     +0.35 same address,  -0.10 both filled in but different
 *   phone     +0.35 same number,   -0.10 both filled in but different
 *   company   +0.15 same company,  -0.10 clearly different company
 *   city      +0.05 same city
 *
 * Why not a simple weighted average of similarities? That was my first version, and the evaluate
 * command showed 17% precision: two different people called "Rahul Sharma" got a high score from the
 * name alone. Names are common, so a name can never be enough on its own (at most 0.60, below every
 * threshold). Email and phone are unique to a person, so they are the strong evidence.
 *
 * With the default threshold 0.85, a merge needs a close name plus the same email or phone.
 * Name + company + city reaches 0.80: that goes to review.csv, not merged.
 */
public final class PairScorer {

    static final double NAME_POINTS = 0.60;
    static final double SAME_IDENTIFIER = 0.35;
    static final double DIFFERENT_IDENTIFIER = -0.10;
    static final double SAME_COMPANY = 0.15;
    static final double DIFFERENT_COMPANY = -0.10;
    static final double SAME_CITY = 0.05;

    /** Email/phone only count when the names are close too, so a shared inbox (info@...) can't merge different people. */
    static final double MIN_NAME_FOR_IDENTIFIERS = 0.75;

    public double score(NormalizedContact a, NormalizedContact b) {
        double nameSimilarity = Math.max(JaroWinkler.similarity(a.fullName(), b.fullName()),
                JaroWinkler.similarity(a.reversedName(), b.fullName()));
        double score = NAME_POINTS * nameSimilarity;

        if (nameSimilarity >= MIN_NAME_FOR_IDENTIFIERS) {
            score += identifierPoints(a.email(), b.email());
            score += identifierPoints(a.phone(), b.phone());
        }

        if (!a.company().isEmpty() && !b.company().isEmpty()) {
            double companySimilarity = JaroWinkler.similarity(a.company(), b.company());
            if (companySimilarity >= 0.9) {
                score += SAME_COMPANY;
            } else if (companySimilarity < 0.75) {
                score += DIFFERENT_COMPANY;
            }
        }

        if (!a.city().isEmpty() && a.city().equals(b.city())) {
            score += SAME_CITY;
        }
        // rounded because of floating point: 0.60 - 0.10 + 0.35 must come out as exactly 0.85 (a match at
        // the default threshold), not 0.8499999999999999
        double rounded = Math.round(score * 1_000_000) / 1_000_000.0;
        return Math.max(0.0, Math.min(1.0, rounded));
    }

    private static double identifierPoints(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        return a.equals(b) ? SAME_IDENTIFIER : DIFFERENT_IDENTIFIER;
    }
}
