package com.eleclib.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    private Long authorId;
    private String firstName;
    private String lastName;
    private String biography;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
