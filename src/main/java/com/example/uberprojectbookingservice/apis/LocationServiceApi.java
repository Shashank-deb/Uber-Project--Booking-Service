package com.example.uberprojectbookingservice.apis;

import com.example.uberprojectbookingservice.dto.DriverLocationDTO;
import com.example.uberprojectbookingservice.dto.NearbyDriversRequestDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LocationServiceApi {

    @POST("/api/v1/location/nearby/drivers")
    Call<DriverLocationDTO[]> getNearbyDrivers(@Body NearbyDriversRequestDTO requestDTO);
}
