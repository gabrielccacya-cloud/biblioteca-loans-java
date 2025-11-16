package com.tecsup.library.model;

import java.util.Objects;

public class Member {

    private final String id;
    private String name;
    private int activeLoans = 0;

    public Member(String id, String name) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getActiveLoans() { return activeLoans; }

    public void incrementLoans() { activeLoans++; }

    public void decrementLoans() {
        if (activeLoans > 0) activeLoans--;
    }
}
