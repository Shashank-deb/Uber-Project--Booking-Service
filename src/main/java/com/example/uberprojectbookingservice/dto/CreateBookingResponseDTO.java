package com.example.uberprojectbookingservice.dto;


import com.example.uberprojectentityservice.models.Driver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingResponseDTO {

    private Long bookingId;
    private String bookingStatus;
    private Optional<Driver> driver;

}
