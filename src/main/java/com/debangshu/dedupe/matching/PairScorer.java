package com.debangshu.dedupe.matching;

/**
 * Gives a pair of contacts a score from 0 to 1: how likely is it that they are the same person?
 *
 * It's a weighted average of field similarities, using only the fields that BOTH rows have filled in.
 * An empty phone says nothing about whether two rows match, so it shouldn't pull the score down.
 *
 *   name     0.45  Jaro-Winkler on "first last" (also tried as "last first", for swapped columns)
 *   company  0.25  Jaro-Winkler on the normalized company name
 *   email    0.20  1.0 if equal, otherwise half the similarity of the part before the @
 *   phone    0.15  1.0 if equal, otherwise 0
 *   city     0.05  1.0 if equal, otherwise 0
 *
 * On top of that, an identical email or phone number is very strong evidence: if the names are close
 * too, the score is raised to at least 0.97.
 */
public final class PairScorer {

    static final double NAME_WEIGHT = 0.45;
    static final double COMPANY_WEIGHT = 0.25;
    static final double EMAIL_WEIGHT = 0.20;
    static final double PHONE_WEIGHT = 0.15;
    static final double CITY_WEIGHT = 0.05;

    public double score(NormalizedContact a, NormalizedContact b) {
        double nameSimilarity = Math.max(JaroWinkler.similarity(a.fullName(), b.fullName()),
                JaroWinkler.similarity(a.reversedName(), b.fullName()));
        double total = NAME_WEIGHT * nameSimilarity;
        double weights = NAME_WEIGHT;

        if (!a.company().isEmpty() && !b.company().isEmpty()) {
            total += COMPANY_WEIGHT * JaroWinkler.similarity(a.company(), b.company());
            weights += COMPANY_WEIGHT;
        }

        boolean sameEmail = false;
        if (!a.email().isEmpty() && !b.email().isEmpty()) {
            sameEmail = a.email().equals(b.email());
            // different addresses can still be the same person (work vs personal), so a similar user name
            // ("rahul.sharma" vs "rahulsharma87") still counts a little
            double emailSimilarity = sameEmail ? 1.0 : 0.5 * JaroWinkler.similarity(a.emailUser(), b.emailUser());
            total += EMAIL_WEIGHT * emailSimilarity;
            weights += EMAIL_WEIGHT;
        }

        boolean samePhone = false;
        if (!a.phone().isEmpty() && !b.phone().isEmpty()) {
            samePhone = a.phone().equals(b.phone());
            total += PHONE_WEIGHT * (samePhone ? 1.0 : 0.0);
            weights += PHONE_WEIGHT;
        }

        if (!a.city().isEmpty() && !b.city().isEmpty()) {
            total += CITY_WEIGHT * (a.city().equals(b.city()) ? 1.0 : 0.0);
            weights += CITY_WEIGHT;
        }

        double score = total / weights;
        if ((sameEmail || samePhone) && nameSimilarity >= 0.8) {
            score = Math.max(score, 0.97);
        }
        return score;
    }
}
