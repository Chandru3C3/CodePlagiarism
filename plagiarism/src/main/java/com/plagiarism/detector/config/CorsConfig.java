//package com.plagiarism.detector.config;
//
//import java.util.Arrays;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.PropertySource;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//@Configuration
//@PropertySource("classpath:database.properties")
//public class CorsConfig {
//
//	@Bean
//	public CorsFilter corsFilter() {
//		CorsConfiguration config = new CorsConfiguration();
//
//		config.setAllowCredentials(true);
//		config.setAllowedOriginPatterns(Arrays.asList("http://localhost:4200", "https://codeplagiarism.netlify.app",
//				"http://localhost:3000", "http://127.0.0.1:4200"));
//
//		config.setAllowedHeaders(Arrays.asList("*"));
//		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
//		config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Content-Disposition"));
//		config.setMaxAge(3600L);
//
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", config);
//
//		return new CorsFilter(source);
//	}
//}