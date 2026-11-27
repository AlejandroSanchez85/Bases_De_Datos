package com.bogotickets;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "BogoTickets API", version = "1.0",
        description = "Venta de boletas con control de aforo para escenarios de Bogotá"))
@SpringBootApplication
public class BogoTicketsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BogoTicketsApplication.class, args);
    }
}
