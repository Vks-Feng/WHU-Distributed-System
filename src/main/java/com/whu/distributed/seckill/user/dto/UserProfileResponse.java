package com.whu.distributed.seckill.user.dto;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String phone;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String username, String phone) {
        this.id = id;
        this.username = username;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
