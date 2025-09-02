package com.example.projectplanner.model;

public enum Status {
    UNVERIFIED(0),
    ACTIVE(1),
    DELETED(2);

    private final int value;

    Status(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}