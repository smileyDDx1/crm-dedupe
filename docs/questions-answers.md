# Questions & Answers (why I built it this way)

Notes I wrote while building the tool, mostly so I can explain my choices in interviews.

## What problem does it solve?

CRM data collects duplicates: the same person imported from a webinar list, typed in by a sales rep and synced from an email tool, each time a little differently. "Rahul Sharma, Bluepeak Software Pvt. Ltd., +91 98765 43210" and "RAHUL SHRAMA, BLUEPEAK SOFTWARE PRIVATE LIMITED, 098765-43210" are one person. Exact matching (Excel's "remove duplicates") misses both. Duplicates mean double emails to the same person, two reps calling the same lead, and wrong numbers in every report.

## Why plain Java, without Spring?

It's a command-line tool that reads a file and writes files. It doesn't need a web server, dependency injection or a database. Two small libraries do the parts I didn't want to write myself: **picocli** (options, sub-commands, `--help`) and **Apache Commons CSV** (quotes, commas inside fields). Everything else is the JDK: records, streams, parallel streams, `java.text.Normalizer`, `java.time`.

## How does the matching work, step by step?

1. **Normalize** every value: lower case, accents removed, company suffixes like "Pvt Ltd" removed, phone reduced to its last 10 digits, Gmail dots and `+tags` removed. Most duplicates aren't typos, they're the same value written differently.
2. **Block**: only compare rows that share a blocking key (same email, same phone, or names that sound alike).
3. **Score** each candidate pair from 0 to 1 by adding up evidence points.
4. **Group** pairs above the threshold with union-find, so A~B and B~C become one group.
5. **Merge** each group into one golden record.

## Why blocking? Isn't comparing everything more accurate?

Comparing every row with every other row is n(n-1)/2 comparisons: 1.45 billion for 53,847 rows, growing with the square of the file size. Blocking only compares rows that share a key (same email, same phone, or a similar-sounding name), which cut it to about 5 million pairs, **99.66% fewer**. The cost is that a duplicate sharing no key with its twin is never compared. The evaluate command measures that directly: blocking still let through 99.25% of the real duplicate pairs. [benchmark-results in the README](../README.md#results) compares it with comparing every pair on a smaller file.

## What is Soundex and why use it for blocking?

Soundex turns a name into a letter + 3 digits based on how it sounds: Sharma and Sarma are both S650, Robert and Rupert both R163. Using it as a blocking key means a typo in a name usually still lands in the same block. It's from 1918 and very simple (I wrote it in about 30 lines), which is fine for blocking because the real decision is made later by the scorer. It does keep the first letter, so a typo in the first letter breaks it. That's why there's a second name key with first and last name swapped.

## Why Jaro-Winkler and not Levenshtein?

Both measure how similar two strings are. Jaro-Winkler was designed for people's names (US census record linkage): it handles swapped letters well, gives extra weight to a matching start (typos are rarer in the first letters), and returns a 0-to-1 score directly, which is easy to combine with other fields. Levenshtein counts edits (an integer) and would need normalizing by length. Both are O(length1 x length2) at worst, which is tiny for names.

## Why "evidence points" instead of a weighted average?

My first scorer was a weighted average of field similarities (name 45%, company 25%, email 20%...). The evaluate command showed **16.7% precision** at the default threshold. Two different people called "Rahul Sharma" got a high score from the name alone, plus a similar email user name, which is just the name again. And because union-find is transitive, a few wrong links chained hundreds of rows into one giant group.

The final scorer adds points for evidence, a simplified version of the Fellegi-Sunter model used in record linkage: name up to 0.60, same email or phone +0.35, different email or phone -0.10, same company +0.15, clearly different company -0.10, same city +0.05. Common things like names can never be enough on their own (0.60 is below every threshold); rare, personal things like an email address or phone number are the strong evidence. Precision went to 100% on the same data, with 89.7% recall.

## Why does the email/phone bonus need a similar name?

Some inboxes and numbers are shared: info@company.com, an office switchboard. Without the rule, two different people at the same company with the same generic email would be merged. The identifiers only count when the names are at least 0.75 similar.

## What's union-find and why is it needed?

Scoring gives pairs, but the output needs groups. If A matches B and B matches C, all three are one person even if A and C were never compared (or scored a bit lower). Union-find (disjoint set union) keeps a parent pointer per row: `union(a, b)` joins two groups, `find(x)` returns a group's representative. With union by size and path halving, both are nearly O(1).

The downside is the same transitivity: one wrong pair can join two big groups. That is exactly what made the first scorer's precision so low, and why the threshold favours precision.

## How do you measure precision and recall without labelled real data?

The generate command makes a fake CRM where every row has a `true_entity_id`. It creates people, then adds messy copies of some of them: typos, upper case, personal instead of work email, Gmail dots, phone formats, company suffixes, missing fields. The evaluate command then counts **pairs**:

- precision = of the pairs we merged, how many are really the same person
- recall = of the real duplicate pairs, how many we merged

Pairs are counted from group sizes (a group of n rows has n(n-1)/2 pairs), not listed one by one, so this is fast even for big groups.

The honest catch: I wrote both the generator and the matcher, so the test data has exactly the kinds of mess I thought of. In particular, emails and phones are unique per person in the generated data. Real CRMs have shared numbers and inboxes, so real precision will be lower than the 100% measured here.

## Why is the default threshold 0.85 and not the best F1?

At 0.80 the F1 score is slightly higher (95.6% vs 94.6%), but precision drops from 100% to 99.92%. For a CRM, a wrong merge is worse than a missed duplicate: merging two customers loses one of them, and can send one person's emails to someone else. A missed duplicate is just a duplicate that stays. So the default is the highest-recall threshold with no wrong merges in the test, and the 0.70-0.85 "maybe" band goes to review.csv for a human.

## Why parallel streams for scoring?

Every pair is scored independently, and the only shared data is the read-only list of normalized rows, so there are no locks and no shared mutable state. `Arrays.stream(pairs).parallel()` splits the pairs across the CPU cores with the common ForkJoinPool. It's the right tool for CPU-bound work over an in-memory array; for I/O-bound work (like sending HTTP requests) it isn't.

## Why store pairs as `long` values?

There are millions of candidate pairs. A `HashSet<Pair>` would allocate an object and a hash table entry for each (40+ bytes). Two `int` row indexes fit in one `long` (8 bytes): the first in the upper 32 bits, the second in the lower 32. Duplicates are removed by sorting the array, and the evaluator checks whether a pair was compared with a binary search.

## What does the golden record take from each row?

Rows are ranked: most recently updated first, then the most complete, then the lowest id. The top row's id survives, and each field takes the first non-empty value in that order, so an old row can still fill in a phone number the newest row is missing. In data quality tools these are called "survivorship rules".
