package com.debangshu.dedupe.io;

import com.debangshu.dedupe.Contact;
import com.debangshu.dedupe.DedupeInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactCsvReaderTest {

    private static List<Contact> read(String csv) throws IOException {
        return ContactCsvReader.read(new StringReader(csv), "test.csv");
    }

    @Test
    void readsTheStandardColumns() throws IOException {
        List<Contact> contacts = read("""
                id,first_name,last_name,email,phone,company,city,updated_at
                C1,Rahul,Sharma,rahul@bluepeak.com,+91 98765 43210,"Bluepeak Software, Pvt Ltd",Pune,2026-03-14
                """);

        assertThat(contacts).containsExactly(new Contact("C1", "Rahul", "Sharma", "rahul@bluepeak.com",
                "+91 98765 43210", "Bluepeak Software, Pvt Ltd", "Pune", LocalDate.of(2026, 3, 14), null));
    }

    @Test
    void understandsColumnNamesFromOtherCrms() throws IOException {
        // the way a Zoho or Salesforce export might name them
        List<Contact> contacts = read("""
                Record Id,First Name,Last Name,Email Address,Mobile,Account Name,Modified Time,Owner
                L-9,Priya,Patel,priya@nimbus.in,9123456780,Nimbus Analytics,2026-01-05T10:22:31,someone
                """);

        Contact priya = contacts.getFirst();
        assertThat(priya.id()).isEqualTo("L-9");
        assertThat(priya.email()).isEqualTo("priya@nimbus.in");
        assertThat(priya.phone()).isEqualTo("9123456780");
        assertThat(priya.company()).isEqualTo("Nimbus Analytics");
        assertThat(priya.updatedAt()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(priya.city()).isEmpty(); // no city column
    }

    @Test
    void unreadableDateCountsAsUnknown() throws IOException {
        List<Contact> contacts = read("""
                id,first_name,updated_at
                C1,Rahul,last tuesday
                """);

        assertThat(contacts.getFirst().updatedAt()).isNull();
    }

    @Test
    void readsTheTruthColumnWhenPresent() throws IOException {
        List<Contact> contacts = read("""
                id,first_name,true_entity_id
                C1,Rahul,P000001
                """);

        assertThat(contacts.getFirst().trueEntityId()).isEqualTo("P000001");
    }

    @Test
    void anIdColumnIsRequired() {
        assertThatThrownBy(() -> read("""
                name,email
                Rahul,rahul@x.com
                """))
                .isInstanceOf(DedupeInputException.class)
                .hasMessageContaining("needs an 'id' column");
    }

    @Test
    void emptyOrRepeatedIdsAreRejected() {
        assertThatThrownBy(() -> read("""
                id,first_name
                ,Rahul
                """))
                .isInstanceOf(DedupeInputException.class)
                .hasMessageContaining("the id is empty");

        assertThatThrownBy(() -> read("""
                id,first_name
                C1,Rahul
                C1,Priya
                """))
                .isInstanceOf(DedupeInputException.class)
                .hasMessageContaining("appears twice");
    }

    @Test
    void missingFileIsAClearError(@TempDir Path dir) {
        assertThatThrownBy(() -> ContactCsvReader.read(dir.resolve("nope.csv")))
                .isInstanceOf(DedupeInputException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void writerOutputCanBeReadBack(@TempDir Path dir) throws IOException {
        List<Contact> original = List.of(new Contact("C1", "Rahul", "Sharma", "rahul@x.com", "9876543210",
                "Bluepeak, \"The\" Software", "Pune", LocalDate.of(2026, 1, 2), "P1"));
        Path file = dir.resolve("out/contacts.csv");

        ContactCsvWriter.write(file, original);

        assertThat(ContactCsvReader.read(file)).isEqualTo(original);
    }
}
