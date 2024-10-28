package com.example.ajouevent.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOriginPatterns("http://localhost:3000", "https://www.ajouevent.com", "https://ajouevent-simzards-projects.vercel.app", "https://ajou-event.vercel.app, https://ajouevent-dev.vercel.app", "https://ajouevent-dev.vercel.app/", "https://ajouevent-git-dev-simzards-projects.vercel.app")
			.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
			.allowedHeaders("Authorization", "Content-Type")
			.exposedHeaders("Custom-Header")
			.allowCredentials(true)
			.maxAge(3600);
	}
}
