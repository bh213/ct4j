package com.whiletrue.sample;

import com.whiletrue.clustertasks.JPA.EnableClusterTasks;
import com.whiletrue.sample.jpa.TestTaskTableRepository;
import org.springframework.boot.Banner;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableClusterTasks
@EntityScan
@EnableJpaRepositories


public class SampleApplication {

	public static void main(String[] args) {


		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SampleApplication.class)
				.web(false)
			.bannerMode(Banner.Mode.OFF)
			.run(args);

	}
}
