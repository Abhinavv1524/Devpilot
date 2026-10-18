package com.devpilot.aiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class AiAgentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiAgentServiceApplication.class, args);
	}

}
