package com.quickbase.datatransfer.gateway.model;

public class FreshdeskContact {
    public Long id;
    public String name;

    @Override
    public String toString() {
        return "FreshdeskSearchContactsResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

