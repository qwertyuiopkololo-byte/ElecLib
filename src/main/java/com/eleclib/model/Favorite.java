package com.eleclib.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {

    private Long userId;
    private Long bookId;
}
