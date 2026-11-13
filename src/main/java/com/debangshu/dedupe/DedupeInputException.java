package com.debangshu.dedupe;

/** A problem with the input file that the user can fix (missing column, duplicate id...). */
public class DedupeInputException extends RuntimeException {

    public DedupeInputException(String message) {
        super(message);
    }
}
