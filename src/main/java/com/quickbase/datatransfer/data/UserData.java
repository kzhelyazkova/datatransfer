package com.quickbase.datatransfer.data;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserData extends BaseData {
    public String name;
    public String email;
    public String twitterHandle;
    public String address;
    public String externalId;
    public String description;
}
