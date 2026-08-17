package br.com.escala24;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import br.com.escala24.config.InitialAdminProperties;

@SpringBootApplication
@EnableConfigurationProperties(
        InitialAdminProperties.class
)
public class Escala24Application {

    public static void main(String[] args) {
        SpringApplication.run(
                Escala24Application.class,
                args
        );
    }
}