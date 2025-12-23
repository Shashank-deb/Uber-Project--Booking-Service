package com.example.uberprojectbookingservice.services;


import com.example.uberprojectbookingservice.dto.CreateBookingDTO;
import com.example.uberprojectbookingservice.dto.CreateBookingResponseDTO;


public interface BookingService {
    public CreateBookingResponseDTO createBooking(CreateBookingDTO booking);
}
