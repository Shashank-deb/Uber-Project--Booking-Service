package com.example.uberprojectbookingservice.services;

import com.example.uberprojectbookingservice.apis.LocationServiceApi;
import com.example.uberprojectbookingservice.dto.*;


import com.example.uberprojectbookingservice.repository.BookingRepository;
import com.example.uberprojectbookingservice.repository.PassengerRepository;
import com.example.uberprojectentityservice.models.Booking;
import com.example.uberprojectentityservice.models.BookingStatus;
import com.example.uberprojectentityservice.models.Passenger;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.RequestContextFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {


    private final PassengerRepository passengerRepository;

    private final BookingRepository bookingRepository;

    private final RestTemplate restTemplate;

    private final LocationServiceApi locationServiceApi;

    private final RequestContextFilter requestContextFilter;

//    private static final String LOCATION_SERVICE = "http://localhost:2510";


    public BookingServiceImpl(PassengerRepository passengerRepository,
                              BookingRepository bookingRepository,
                              LocationServiceApi locationServiceApi, RequestContextFilter requestContextFilter) {


        this.passengerRepository = passengerRepository;
        this.bookingRepository = bookingRepository;
        this.restTemplate = new RestTemplate();
        this.locationServiceApi = locationServiceApi;
        this.requestContextFilter = requestContextFilter;
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

        //API call to location service to fetch nearby drivers this is sync call to another microservices

        NearbyDriversRequestDTO request = NearbyDriversRequestDTO
                .builder()
                .latitude(bookingDetails.getStartLocation().getLatitude())
                .longitude(bookingDetails.getStartLocation().getLongitude())
                .build();


        processNearbyDriversAsync(request);
//
//
//        ResponseEntity<DriverLocationDTO[]> result = restTemplate.postForEntity(LOCATION_SERVICE + "/api/v1/location/nearby/drivers", request, DriverLocationDTO[].class);
//
//        if (result.getStatusCode().is2xxSuccessful() && result.getBody().length > 0) {
//            List<DriverLocationDTO> driverLocations = Arrays.asList(result.getBody());
//
//            driverLocations.forEach(driverLocationDTO -> {
//                System.out.printf("Driver ID: %-10s | Lat: %-10.6f | Lon: %-10.6f%n",
//                        driverLocationDTO.getDriverId(),
//                        driverLocationDTO.getLatitude(),
//                        driverLocationDTO.getLongitude());
//            });
//        }

        return CreateBookingResponseDTO
                .builder()
                .bookingId(newBooking.getId())
                .bookingStatus(newBooking.getBookingStatus().toString())
                .build();
    }


    private void processNearbyDriversAsync(NearbyDriversRequestDTO requestDTO) {
        Call<DriverLocationDTO[]> call = locationServiceApi.getNearbyDrivers(requestDTO);
        call.enqueue(new Callback<DriverLocationDTO[]>() {
            @Override
            public void onResponse(Call<DriverLocationDTO[]> call, Response<DriverLocationDTO[]> response) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (response.isSuccessful() && response.body().length > 0) {
                    List<DriverLocationDTO> driverLocations = Arrays.asList(response.body());

                    driverLocations.forEach(driverLocationDTO -> {
                        System.out.printf("Driver ID: %-10s | Lat: %-10.6f | Lon: %-10.6f%n",
                                driverLocationDTO.getDriverId(),
                                driverLocationDTO.getLatitude(),
                                driverLocationDTO.getLongitude());
                    });
                } else {
                    System.out.println("Request failed" + response.message());
                }
            }

            @Override
            public void onFailure(Call<DriverLocationDTO[]> call, Throwable throwable) {
                throwable.printStackTrace();
            }

        });

    }


}
