package com.example.uberprojectbookingservice.controllers;

import com.example.uberprojectbookingservice.dto.CreateBookingDTO;
import com.example.uberprojectbookingservice.dto.CreateBookingResponseDTO;
import com.example.uberprojectbookingservice.dto.UpdateBookingRequestDTO;
import com.example.uberprojectbookingservice.dto.UpdateBookingResponseDTO;
import com.example.uberprojectbookingservice.services.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<CreateBookingResponseDTO> createBooking(@RequestBody CreateBookingDTO booking) {
        return new ResponseEntity<>(bookingService.createBooking(booking),HttpStatus.CREATED);

    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<UpdateBookingResponseDTO> updateBooking(@RequestBody UpdateBookingRequestDTO requestDTO, @PathVariable Long bookingId) {
        return new ResponseEntity<>(bookingService.updateBooking(requestDTO,bookingId),HttpStatus.OK);
    }
}
