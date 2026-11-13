package com.debangshu.dedupe.pipeline;

import com.debangshu.dedupe.Contact;

import java.util.List;

/** A group of rows that are the same person, and the one merged row that replaces them. */
public record Cluster(int id, List<Contact> members, Contact golden) {
}
