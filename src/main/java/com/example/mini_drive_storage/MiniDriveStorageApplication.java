package com.example.mini_drive_storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MiniDriveStorageApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniDriveStorageApplication.class, args);
	}

}
