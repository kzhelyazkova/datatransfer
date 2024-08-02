package com.quickbase.datatransfer.dto;

public class UserDataDTO extends DataDTO {
    public String name;
    public String company;
    public String email;
    public String address;

    public UserDataDTO() {
    }

    public static boolean isUserDataType(String dataType) {
        return "user".equals(dataType);
    }

    @Override
    public String toString() {
        return "UserDataDTO{" +
                "name='" + name + '\'' +
                ", company='" + company + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
