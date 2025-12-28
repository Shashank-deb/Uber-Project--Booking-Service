package com.example.uberprojectbookingservice.apis;

import com.example.uberprojectbookingservice.dto.RideRequestDTO;
import org.springframework.http.ResponseEntity;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UberSocketApi {
    @POST("/api/v1/socket/newRide")
    Call<ResponseEntity<Boolean>> getNearbyDrivers(@Body RideRequestDTO requestDTO);
}
