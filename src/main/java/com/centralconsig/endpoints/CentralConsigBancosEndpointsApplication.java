package com.centralconsig.endpoints;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"com.centralconsig.endpoints",
		"com.centralconsig.core"
})
@EnableScheduling
public class CentralConsigBancosEndpointsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralConsigBancosEndpointsApplication.class, args);
	}

}
