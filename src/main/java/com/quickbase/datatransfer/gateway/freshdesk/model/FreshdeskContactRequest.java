package com.quickbase.datatransfer.gateway.freshdesk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FreshdeskContactRequest {
    public String name;
    public String address;
    public String email;
    public String twitterId;
    public String uniqueExternalId;
    public String description;
}
