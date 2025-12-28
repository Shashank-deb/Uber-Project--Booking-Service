package com.example.uberprojectbookingservice.services;


import com.example.uberprojectbookingservice.dto.CreateBookingDTO;
import com.example.uberprojectbookingservice.dto.CreateBookingResponseDTO;
import com.example.uberprojectbookingservice.dto.UpdateBookingRequestDTO;
import com.example.uberprojectbookingservice.dto.UpdateBookingResponseDTO;


public interface BookingService {
     CreateBookingResponseDTO createBooking(CreateBookingDTO booking);
     UpdateBookingResponseDTO updateBooking(UpdateBookingRequestDTO requestDTO,Long bookingId);
}
