package com.nutrigraph;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir les fichiers statiques
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Rediriger les routes principales vers le fichier HTML
        registry.addViewController("/").setViewName("forward:/static/nutrigraph-frontend.html");
        registry.addViewController("/app").setViewName("forward:/static/nutrigraph-frontend.html");
        registry.addViewController("/frontend").setViewName("forward:/static/nutrigraph-frontend.html");
        registry.addViewController("/search").setViewName("forward:/static/nutrigraph-frontend.html");
        registry.addViewController("/admin").setViewName("forward:/static/nutrigraph-frontend.html");
        registry.addViewController("/ui").setViewName("forward:/static/nutrigraph-frontend.html");
    }
}