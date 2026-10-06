package com.hkp.sync_space;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.hkp.sync_space.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SyncSpaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyncSpaceApplication.class, args);
	}

}
