package com.example.uberprojectbookingservice.services;

import com.example.uberprojectbookingservice.dto.CreateBookingDTO;
import com.example.uberprojectbookingservice.dto.CreateBookingResponseDTO;


import com.example.uberprojectbookingservice.dto.DriverLocationDTO;
import com.example.uberprojectbookingservice.dto.NearbyDriversRequestDTO;
import com.example.uberprojectbookingservice.repository.BookingRepository;
import com.example.uberprojectbookingservice.repository.PassengerRepository;
import com.example.uberprojectentityservice.models.Booking;
import com.example.uberprojectentityservice.models.BookingStatus;
import com.example.uberprojectentityservice.models.Passenger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {


    private final PassengerRepository passengerRepository;

    private final BookingRepository bookingRepository;

    private final RestTemplate restTemplate;

    private static final String LOCATION_SERVICE = "http://localhost:2510";


    public BookingServiceImpl(PassengerRepository passengerRepository,
                              BookingRepository bookingRepository) {


        this.passengerRepository = passengerRepository;
        this.bookingRepository = bookingRepository;
        this.restTemplate = new RestTemplate();
    }


    @Override
    public CreateBookingResponseDTO createBooking(CreateBookingDTO bookingDetails) {
        Optional<Passenger> passenger = passengerRepository.findById(bookingDetails.getPassengerId());
        Booking booking = Booking
                .builder()
                .bookingStatus(BookingStatus.ASSIGNING_DRIVER)
                .startLocation(bookingDetails.getStartLocation())
//                .endLocation(bookingDetails.getEndLocation())
                .passenger(passenger.get())
                .build();

        Booking newBooking = bookingRepository.save(booking);

        //API call to location service to fetch nearby drivers

        NearbyDriversRequestDTO request = NearbyDriversRequestDTO
                .builder()
                .latitude(bookingDetails.getStartLocation().getLatitude())
                .longitude(bookingDetails.getStartLocation().getLongitude())
                .build();


        ResponseEntity<DriverLocationDTO[]> result = restTemplate.postForEntity(LOCATION_SERVICE + "/api/v1/location/nearby/drivers", request, DriverLocationDTO[].class);

        if (result.getStatusCode().is2xxSuccessful() && result.getBody().length > 0) {
            List<DriverLocationDTO> driverLocations = Arrays.asList(result.getBody());

            driverLocations.forEach(driverLocationDTO -> {
                System.out.printf("Driver ID: %-10s | Lat: %-10.6f | Lon: %-10.6f%n",
                        driverLocationDTO.getDriverId(),
                        driverLocationDTO.getLatitude(),
                        driverLocationDTO.getLongitude());
            });
        }

        return CreateBookingResponseDTO
                .builder()
                .bookingId(newBooking.getId())
                .bookingStatus(newBooking.getBookingStatus().toString())
                .build();
    }


}
