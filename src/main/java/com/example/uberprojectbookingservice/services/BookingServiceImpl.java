package com.example.uberprojectbookingservice.services;

import com.example.uberprojectbookingservice.apis.LocationServiceApi;
import com.example.uberprojectbookingservice.apis.UberSocketApi;
import com.example.uberprojectbookingservice.dto.*;


import com.example.uberprojectbookingservice.repository.BookingRepository;
import com.example.uberprojectbookingservice.repository.DriverRepository;
import com.example.uberprojectbookingservice.repository.PassengerRepository;
import com.example.uberprojectentityservice.models.Booking;
import com.example.uberprojectentityservice.models.BookingStatus;
import com.example.uberprojectentityservice.models.Driver;
import com.example.uberprojectentityservice.models.Passenger;

import org.springframework.http.ResponseEntity;
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

    private final UberSocketApi uberSocketApi;

    private final RequestContextFilter requestContextFilter;

    private final DriverRepository driverRepository;

//    private static final String LOCATION_SERVICE = "http://localhost:2510";


    public BookingServiceImpl(PassengerRepository passengerRepository,
                              BookingRepository bookingRepository,
                              LocationServiceApi locationServiceApi,
                              RequestContextFilter requestContextFilter,
                              DriverRepository driverRepository,
                              UberSocketApi uberSocketApi) {


        this.passengerRepository = passengerRepository;
        this.bookingRepository = bookingRepository;
        this.restTemplate = new RestTemplate();
        this.locationServiceApi = locationServiceApi;
        this.requestContextFilter = requestContextFilter;
        this.driverRepository = driverRepository;
        this.uberSocketApi = uberSocketApi;
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


        processNearbyDriversAsync(request,bookingDetails.getPassengerId());
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


    //This function is helping on fetching the details of Nearby Drivers as Async methods
    private void processNearbyDriversAsync(NearbyDriversRequestDTO requestDTO,Long passengerId) {
        Call<DriverLocationDTO[]> call = locationServiceApi.getNearbyDrivers(requestDTO);
        call.enqueue(new Callback<DriverLocationDTO[]>() {
            @Override
            public void onResponse(Call<DriverLocationDTO[]> call, Response<DriverLocationDTO[]> response) {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
                if (response.isSuccessful() && response.body().length > 0) {
                    List<DriverLocationDTO> driverLocations = Arrays.asList(response.body());

                    driverLocations.forEach(driverLocationDTO -> {
                        System.out.printf("Driver ID: %-10s | Lat: %-10.6f | Lon: %-10.6f%n",
                                driverLocationDTO.getDriverId(),
                                driverLocationDTO.getLatitude(),
                                driverLocationDTO.getLongitude());
                    });

                   raiseRequestAsync(RideRequestDTO.builder().passengerId(2L).build());
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

    private void raiseRequestAsync(RideRequestDTO requestDTO) {
        Call<Boolean> call = uberSocketApi.getNearbyDrivers(requestDTO);
        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Boolean result = response.body();
                    System.out.println("Ride request successfully sent to socket service");
                    System.out.println("Response status: " + response.code());
                    System.out.println("Driver response: " + result);
                } else {
                    System.err.println("Request for ride failed. Status code: " + response.code());
                    System.err.println("Error message: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            System.err.println("Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable throwable) {
                System.err.println("Failed to send ride request to socket service:");
                throwable.printStackTrace();
            }
        });
    }


    @Override
    public UpdateBookingResponseDTO updateBooking(UpdateBookingRequestDTO bookingRequestDTO, Long bookingId) {
        Optional<Driver> driver = driverRepository.findById(bookingRequestDTO.getDriverId().get());
        bookingRepository.updateBookingStatusAndDriverById(bookingId, BookingStatus.SCHEDULED, driver.get());
        Optional<Booking> booking = bookingRepository.findById(bookingId);
        return new UpdateBookingResponseDTO().builder()
                .bookingId(bookingId)
                .status(booking.get().getBookingStatus())
                .driver(Optional.ofNullable(booking.get().getDriver()))
                .build();

    }


}
