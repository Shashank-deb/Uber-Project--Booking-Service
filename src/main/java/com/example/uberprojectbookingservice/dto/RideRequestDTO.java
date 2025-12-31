package com.example.uberprojectbookingservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestDTO {
    private Long passengerId;
    private List<Long> driverIds;
    private Long bookingId;
}

