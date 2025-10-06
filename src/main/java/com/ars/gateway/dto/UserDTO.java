package com.ars.gateway.dto;

@SuppressWarnings("unused")
public class UserDTO {
    private Integer id;
    private String username;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UserDTO user = new UserDTO();

        public Builder withId(Integer id) {
            user.id = id;
            return this;
        }

        public Builder withUsername(String username) {
            user.username = username;
            return this;
        }

        public UserDTO build() {
            return user;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
