package com.quickbase.datatransfer.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDTO extends BaseDTO {
    public String name;
    public String company;
    public String email;
    public String address;
}
