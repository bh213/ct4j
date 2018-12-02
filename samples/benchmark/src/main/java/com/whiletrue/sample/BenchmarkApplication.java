package com.whiletrue.sample;

import com.whiletrue.ct4j.spring.EnableCt4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

@EnableCt4j
@SpringBootApplication
@EntityScan
@EnableJpaRepositories
@EnableConfigurationProperties(BenchmarkConfigurationProperties.class)
public class BenchmarkApplication {

    @Bean
    public RestTemplate defaultRestTemplate() {
        return new RestTemplate();
    }


    public static void main(String[] args) {
        SpringApplication.run(BenchmarkApplication.class, args);
    }

}
