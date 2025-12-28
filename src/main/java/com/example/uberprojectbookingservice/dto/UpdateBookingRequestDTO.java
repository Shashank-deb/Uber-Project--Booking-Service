package com.example.uberprojectbookingservice.dto;


import lombok.*;

import java.util.Optional;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingRequestDTO {
    private String status;
    private Optional<Long> driverId;
}
