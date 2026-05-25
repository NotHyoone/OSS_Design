package com.github.insight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 허용할 CORS 오리진 목록 (쉼표 구분).
     * 운영: CORS_ALLOWED_ORIGINS 환경변수로 배포 도메인을 명시한다.
     * 예) CORS_ALLOWED_ORIGINS=https://insight.example.com
     */
    @Value("${cors.allowed-origins:http://localhost:8080}")
    private String allowedOriginsValue;

    /**
     * 프론트엔드 정적 파일(web/)을 루트에서 서빙
     * mvn -f backend/pom.xml spring-boot:run 으로 실행 시 작업 디렉터리가 repo 루트이므로
     * file:web/ 경로가 올바르게 동작함
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("file:web/", "classpath:/static/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOriginsValue.split(",");
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "DELETE")
                .allowedHeaders("*");
    }
}
