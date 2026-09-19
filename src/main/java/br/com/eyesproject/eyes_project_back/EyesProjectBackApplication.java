package br.com.eyesproject.eyes_project_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EyesProjectBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(EyesProjectBackApplication.class, args);
	}

}
