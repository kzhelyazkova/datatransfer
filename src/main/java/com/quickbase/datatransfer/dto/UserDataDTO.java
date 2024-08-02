package com.quickbase.datatransfer.dto;

public class UserDataDTO extends DataDTO {
    public String username;

    public UserDataDTO(String username) {
        this.username = username;
    }

    public static boolean isUserDataType(String dataType) {
        return "user".equals(dataType);
    }
}
