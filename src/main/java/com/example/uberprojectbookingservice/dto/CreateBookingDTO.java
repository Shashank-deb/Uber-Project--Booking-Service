package com.example.uberprojectbookingservice.dto;


import com.example.uberprojectentityservice.models.ExactLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingDTO  {
    private Long passengerId;
    private ExactLocation startLocation;
    private ExactLocation endLocation;
}
