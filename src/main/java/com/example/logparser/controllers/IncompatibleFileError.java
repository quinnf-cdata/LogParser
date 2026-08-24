package com.example.logparser.controllers;

public class IncompatibleFileError extends Exception {
    public IncompatibleFileError(String file1, String file2, String error) {
        super(String.format("File %1$s is not compatible with %2$s. Error: %3$s.", file1, file2, error));
    }

    public IncompatibleFileError(String error) {
        super(error);
    }
}
