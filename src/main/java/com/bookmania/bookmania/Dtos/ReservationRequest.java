// ReservationRequest.java
package com.bookmania.bookmania.Dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationRequest {
    @NotNull
    private Long bookId;
}