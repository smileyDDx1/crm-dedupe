# Future plans

1. **Learn the weights instead of choosing them.** The evidence points are hand-picked and tuned with the evaluate command. Fellegi-Sunter can estimate them from the data itself (with expectation maximisation), which is what tools like Splink do.
2. **Nicknames and initials.** "Bob" / "Robert", "R. Sharma" / "Rahul Sharma". A nickname table and an initial rule would help recall.
3. **Frequency-aware name scoring.** Two "Rahul Sharma"s are weaker evidence than two "Zoya Fernandes"es, because the first name is far more common. The name points could depend on how common the name is in the file.
4. **Real phone parsing.** Phones are reduced to their last 10 digits, which works for Indian mobile numbers but not internationally. libphonenumber would handle country codes properly.
5. **Guard against chaining.** Union-find joins A~B~C even if A and C look nothing alike. A check that every new member is close enough to the group (or to its golden record) would stop one bad pair from merging two big groups.
6. **Streaming for huge files.** Everything is in memory, fine for a few hundred thousand rows. For millions, the blocking keys could be sorted on disk instead.
7. **Read straight from the CRM.** A Zoho or HubSpot API connector instead of a CSV export, and writing merges back with an undo log.
8. **A review UI.** review.csv is fine for a few hundred pairs; a small web page with "same / different" buttons would be faster, and the answers could tune the threshold.
