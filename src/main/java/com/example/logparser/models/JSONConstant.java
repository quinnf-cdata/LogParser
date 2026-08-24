package com.example.logparser.models;

public class JSONConstant {
    private final String displayName;
    private final String pattern;

    public JSONConstant(String displayName, String pattern) {
        this.displayName = displayName;
        this.pattern = pattern;
    }

    public String getDisplayName() {
        return displayName;
    }


    public String getPattern() {
        return pattern;
    }
}
