package com.quickbase.datatransfer.gateway.model;

public class GitHubUserData {
    public String name;
    public String company;
    public String email;
    public String location;

    @Override
    public String toString() {
        return "UserData{" +
                "name='" + name + '\'' +
                ", company='" + company + '\'' +
                ", email='" + email + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
