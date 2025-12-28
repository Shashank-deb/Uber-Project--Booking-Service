package com.example.uberprojectbookingservice.controllers;

import com.example.uberprojectbookingservice.apis.LocationServiceApi;
import com.example.uberprojectbookingservice.apis.UberSocketApi;
import com.netflix.discovery.EurekaClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


@Configuration
public class RetrofitConfig {

    @Autowired
    private EurekaClient eurekaClient;

    private String getServiceUrl(String serviceName) {
        return eurekaClient.getNextServerFromEureka(serviceName, false).getHomePageUrl();
    }

    @Bean
    public LocationServiceApi getLocationServiceApi() {
        return new Retrofit.Builder()
                .baseUrl(getServiceUrl("UBERPROJECT-LOCATIONSERVICE"))
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build()
                .create(LocationServiceApi.class);
    }

    @Bean
    public UberSocketApi uberSocketApi() {
        String serviceUrl = getServiceUrl("UBERSOCKETSERVICE");
        System.out.println(serviceUrl);
        return new Retrofit.Builder()
                .baseUrl(getServiceUrl("UBERSOCKETSERVICE"))
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build()
                .create(UberSocketApi.class);
    }

}
