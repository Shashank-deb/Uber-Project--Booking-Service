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

import org.springframework.stereotype.Service;
import org.springframework.web.filter.RequestContextFilter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;
    private final LocationServiceApi locationServiceApi;
    private final UberSocketApi uberSocketApi;
    private final DriverRepository driverRepository;

    public BookingServiceImpl(PassengerRepository passengerRepository,
                              BookingRepository bookingRepository,
                              LocationServiceApi locationServiceApi,
                              RequestContextFilter requestContextFilter,
                              DriverRepository driverRepository,
                              UberSocketApi uberSocketApi) {

        this.passengerRepository = passengerRepository;
        this.bookingRepository = bookingRepository;
        this.locationServiceApi = locationServiceApi;
        this.driverRepository = driverRepository;
        this.uberSocketApi = uberSocketApi;
    }

    @Override
    public CreateBookingResponseDTO createBooking(CreateBookingDTO bookingDetails) {
        System.out.println("=======================================================");
        System.out.println("CREATE BOOKING REQUEST RECEIVED");
        System.out.println("=======================================================");
        System.out.println("Passenger ID: " + bookingDetails.getPassengerId());
        System.out.println("Start Location: Lat=" + bookingDetails.getStartLocation().getLatitude()
                + ", Lon=" + bookingDetails.getStartLocation().getLongitude());

        // Step 1: Fetch Passenger
        Optional<Passenger> passengerOpt = passengerRepository.findById(bookingDetails.getPassengerId());
        if (passengerOpt.isEmpty()) {
            System.out.println("ERROR: Passenger not found with ID: " + bookingDetails.getPassengerId());
            return CreateBookingResponseDTO.builder()
                    .bookingId(null)
                    .bookingStatus("FAILED - Passenger not found")
                    .build();
        }

        // Step 2: Create Booking
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.ASSIGNING_DRIVER)
                .startLocation(bookingDetails.getStartLocation())
                .passenger(passengerOpt.get())
                .build();

        Booking newBooking = bookingRepository.save(booking);
        System.out.println("Booking created with ID: " + newBooking.getId());

        // Step 3: Find nearby drivers
        NearbyDriversRequestDTO request = NearbyDriversRequestDTO.builder()
                .latitude(bookingDetails.getStartLocation().getLatitude())
                .longitude(bookingDetails.getStartLocation().getLongitude())
                .build();

        processNearbyDriversAsync(request, bookingDetails.getPassengerId(), newBooking.getId());

        return CreateBookingResponseDTO.builder()
                .bookingId(newBooking.getId())
                .bookingStatus(newBooking.getBookingStatus().toString())
                .build();
    }

    private void processNearbyDriversAsync(NearbyDriversRequestDTO requestDTO, Long passengerId, Long bookingId) {
        System.out.println("-------------------------------------------------------");
        System.out.println("STEP 1: Searching for nearby drivers...");
        System.out.println("Search Location: Lat=" + requestDTO.getLatitude() + ", Lon=" + requestDTO.getLongitude());
        System.out.println("-------------------------------------------------------");

        Call<DriverLocationDTO[]> call = locationServiceApi.getNearbyDrivers(requestDTO);
        call.enqueue(new Callback<DriverLocationDTO[]>() {

            @Override
            public void onResponse(Call<DriverLocationDTO[]> call, Response<DriverLocationDTO[]> response) {
                System.out.println("Location Service Response - Status Code: " + response.code());

                if (!response.isSuccessful()) {
                    System.out.println("ERROR: Location Service returned error");
                    System.out.println("Error Code: " + response.code());
                    System.out.println("Error Message: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            System.out.println("Error Body: " + response.errorBody().string());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return;
                }

                if (response.body() == null) {
                    System.out.println("ERROR: Response body is null");
                    return;
                }

                if (response.body().length == 0) {
                    System.out.println("=======================================================");
                    System.out.println("WARNING: No nearby drivers found!");
                    System.out.println("Please register drivers first using:");
                    System.out.println("POST http://localhost:2510/api/v1/location/drivers");
                    System.out.println("Body: {\"driverId\": \"6\", \"latitude\": " + requestDTO.getLatitude()
                            + ", \"longitude\": " + requestDTO.getLongitude() + "}");
                    System.out.println("=======================================================");
                    return;
                }

                // Drivers found!
                List<DriverLocationDTO> driverLocations = Arrays.asList(response.body());
                System.out.println("SUCCESS: Found " + driverLocations.size() + " nearby driver(s)");
                System.out.println("-------------------------------------------------------");

                driverLocations.forEach(driver -> {
                    System.out.printf("  Driver ID: %-10s | Lat: %-12.6f | Lon: %-12.6f%n",
                            driver.getDriverId(),
                            driver.getLatitude(),
                            driver.getLongitude());
                });

                System.out.println("-------------------------------------------------------");

                // Extract driver IDs
                List<Long> driverIds = driverLocations.stream()
                        .map(d -> Long.parseLong(d.getDriverId()))
                        .collect(Collectors.toList());

                // Send ride request to Socket Service
                RideRequestDTO rideRequest = RideRequestDTO.builder()
                        .passengerId(passengerId)
                        .driverIds(driverIds)
                        .build();

                raiseRequestAsync(rideRequest, bookingId);
            }

            @Override
            public void onFailure(Call<DriverLocationDTO[]> call, Throwable throwable) {
                System.out.println("=======================================================");
                System.out.println("ERROR: Failed to connect to Location Service");
                System.out.println("Exception: " + throwable.getMessage());
                System.out.println("Make sure Location Service is running on port 2510");
                System.out.println("=======================================================");
                throwable.printStackTrace();
            }
        });
    }

    private void raiseRequestAsync(RideRequestDTO requestDTO, Long bookingId) {
        System.out.println("-------------------------------------------------------");
        System.out.println("STEP 2: Sending ride request to Socket Service...");
        System.out.println("Passenger ID: " + requestDTO.getPassengerId());
        System.out.println("Driver IDs: " + requestDTO.getDriverIds());
        System.out.println("-------------------------------------------------------");

        Call<Boolean> call = uberSocketApi.raiseRideRequest(requestDTO);
        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null && response.body()) {
                    System.out.println("=======================================================");
                    System.out.println("SUCCESS: Ride request sent to Socket Service!");
                    System.out.println("Booking ID: " + bookingId);
                    System.out.println("Waiting for driver to accept...");
                    System.out.println("Check your browser for the popup!");
                    System.out.println("=======================================================");
                } else {
                    System.out.println("ERROR: Socket Service returned unsuccessful response");
                    System.out.println("Status Code: " + response.code());
                    System.out.println("Message: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            System.out.println("Error Body: " + response.errorBody().string());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable throwable) {
                System.out.println("=======================================================");
                System.out.println("ERROR: Failed to connect to Socket Service");
                System.out.println("Exception: " + throwable.getMessage());
                System.out.println("Make sure Socket Service is running on port 2511");
                System.out.println("=======================================================");
                throwable.printStackTrace();
            }
        });
    }

    @Override
    public UpdateBookingResponseDTO updateBooking(UpdateBookingRequestDTO bookingRequestDTO, Long bookingId) {
        System.out.println("=======================================================");
        System.out.println("UPDATE BOOKING REQUEST");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("New Status: " + bookingRequestDTO.getStatus());
        System.out.println("Driver ID: " + bookingRequestDTO.getDriverId().orElse(null));
        System.out.println("=======================================================");

        Optional<Driver> driver = driverRepository.findById(bookingRequestDTO.getDriverId().get());
        if (driver.isEmpty()) {
            System.out.println("ERROR: Driver not found with ID: " + bookingRequestDTO.getDriverId().get());
            return UpdateBookingResponseDTO.builder()
                    .bookingId(bookingId)
                    .status(null)
                    .driver(Optional.empty())
                    .build();
        }

        bookingRepository.updateBookingStatusAndDriverById(bookingId, BookingStatus.SCHEDULED, driver.get());
        Optional<Booking> booking = bookingRepository.findById(bookingId);

        System.out.println("SUCCESS: Booking updated successfully!");

        return UpdateBookingResponseDTO.builder()
                .bookingId(bookingId)
                .status(booking.get().getBookingStatus())
                .driver(Optional.ofNullable(booking.get().getDriver()))
                .build();
    }
}