package se.psaas.minaarenden;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MinaArendenApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinaArendenApplication.class, args);
    }
}
