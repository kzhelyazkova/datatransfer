package com.quickbase.datatransfer.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDTO extends BaseDTO {
    public String name;
    public String email;
    public String twitterHandle;
    public String address;
    public String externalId;
    public String description;
}
