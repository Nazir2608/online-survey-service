package com.nazir.onlinesurveyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class OnlineSurveyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineSurveyServiceApplication.class, args);
    }
}
