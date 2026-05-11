package com.github.insight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

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

    /** 개발 편의를 위해 모든 오리진에서 /api/** 호출 허용 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "DELETE")
                .allowedHeaders("*");
    }
}
