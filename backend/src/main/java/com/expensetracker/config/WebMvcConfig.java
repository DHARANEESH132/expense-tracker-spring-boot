package com.expensetracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve frontend directory path
        String frontendDir = Paths.get("..", "frontend").toAbsolutePath().normalize().toUri().toString();
        String localFrontendDir = Paths.get("frontend").toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler("/**")
                .addResourceLocations(frontendDir + "/", localFrontendDir + "/", "classpath:/static/", "classpath:/public/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
