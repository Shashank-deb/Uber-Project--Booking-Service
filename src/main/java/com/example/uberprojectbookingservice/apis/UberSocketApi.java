package com.example.uberprojectbookingservice.apis;

import com.example.uberprojectbookingservice.dto.RideRequestDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UberSocketApi {
    @POST("/api/v1/socket/newRide")
    Call<Boolean> getNearbyDrivers(@Body RideRequestDTO requestDTO);
}
