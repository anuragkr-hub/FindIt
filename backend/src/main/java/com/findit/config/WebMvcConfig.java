package com.findit.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApplicationContext applicationContext;

    public WebMvcConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations(
                        "file:../frontend/static/css/",
                        "file:./frontend/static/css/",
                        "classpath:/static/css/"
                )
                .setCachePeriod(3600);

        registry.addResourceHandler("/js/**")
                .addResourceLocations(
                        "file:../frontend/static/js/",
                        "file:./frontend/static/js/",
                        "classpath:/static/js/"
                )
                .setCachePeriod(3600);

        registry.addResourceHandler("/img/**")
                .addResourceLocations(
                        "file:../frontend/static/img/",
                        "file:./frontend/static/img/",
                        "classpath:/static/img/"
                )
                .setCachePeriod(3600);
    }

    @Bean
    public SpringResourceTemplateResolver frontendTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(this.applicationContext);

        // Check if frontend directory exists relative to current working directory
        File localFrontend = new File("../frontend/templates");
        File rootFrontend = new File("./frontend/templates");

        if (localFrontend.exists() && localFrontend.isDirectory()) {
            resolver.setPrefix("file:../frontend/templates/");
            resolver.setCacheable(false);
        } else if (rootFrontend.exists() && rootFrontend.isDirectory()) {
            resolver.setPrefix("file:./frontend/templates/");
            resolver.setCacheable(false);
        } else {
            resolver.setPrefix("classpath:/templates/");
            resolver.setCacheable(true);
        }

        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
        resolver.setCheckExistence(true);
        return resolver;
    }
}
