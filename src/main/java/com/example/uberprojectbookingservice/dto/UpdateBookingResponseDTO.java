package com.example.uberprojectbookingservice.dto;

import com.example.uberprojectentityservice.models.BookingStatus;
import com.example.uberprojectentityservice.models.Driver;
import lombok.*;

import java.util.Optional;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingResponseDTO {
    private Long bookingId;
    private BookingStatus status;
    private Optional<Driver> driver;
}
