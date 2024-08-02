package com.quickbase.datatransfer.gateway.model;

public class FreshdeskContactBody {
    public String name;
    public String address;
    public String email;

    @Override
    public String toString() {
        return "FreshdeskContactBody{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
