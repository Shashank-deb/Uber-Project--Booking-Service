package com.example.uberprojectbookingservice.consumers;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {


    @KafkaListener(topics = "sample-topic")
    public void listen(String message) {
        System.out.println("Kafka message from sample topic inside booking service: " + message);
    }


}
