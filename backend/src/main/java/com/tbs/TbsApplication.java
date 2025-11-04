package com.tbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TbsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TbsApplication.class, args);
	}

}
