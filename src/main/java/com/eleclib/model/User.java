package com.eleclib.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

    private Long userId;
    private String login;
    private String password;
    private String firstName;
    private String lastName;
    @Builder.Default
    private String role = "app_user";

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
