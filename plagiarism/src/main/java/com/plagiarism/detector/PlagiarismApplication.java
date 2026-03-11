package com.plagiarism.detector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableAsync
public class PlagiarismApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlagiarismApplication.class, args);
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
		e.setCorePoolSize(4);
		e.setMaxPoolSize(8);
		e.setQueueCapacity(100);
		e.initialize();
		return e;
	}

}
