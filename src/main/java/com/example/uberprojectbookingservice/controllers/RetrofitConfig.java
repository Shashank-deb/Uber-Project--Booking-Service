package com.example.uberprojectbookingservice.controllers;

import com.example.uberprojectbookingservice.apis.LocationServiceApi;
import com.example.uberprojectbookingservice.apis.UberSocketApi;
import com.netflix.discovery.EurekaClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Configuration
public class RetrofitConfig {

    @Autowired
    @Lazy
    private EurekaClient eurekaClient;

    private String getServiceUrl(String serviceName) {
        try {
            return eurekaClient.getNextServerFromEureka(serviceName, false).getHomePageUrl();
        } catch (Exception e) {
            System.out.println("WARNING: " + serviceName + " not found in Eureka, using fallback URL");
            // Fallback URLs
            if (serviceName.contains("LOCATION")) {
                return "http://localhost:2510/";
            } else if (serviceName.contains("SOCKET")) {
                return "http://localhost:2511/";
            }
            return "http://localhost:8080/";
        }
    }

    @Bean
    @Lazy  // ADD THIS
    public LocationServiceApi getLocationServiceApi() {
        String url = getServiceUrl("UBERPROJECT-LOCATIONSERVICE");
        System.out.println("Location Service URL: " + url);
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build()
                .create(LocationServiceApi.class);
    }

    @Bean
    @Lazy
    public UberSocketApi uberSocketApi() {
        String url = getServiceUrl("UBERSOCKETSERVICE");
        System.out.println("Socket Service URL: " + url);
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build()
                .create(UberSocketApi.class);
    }
}