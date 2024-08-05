package com.quickbase.datatransfer.gateway.github.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GitHubUserResponse {
    public String name;
    public String email;
    public String location;
    public String login;
    public String bio;
    public String twitterUsername;
}
