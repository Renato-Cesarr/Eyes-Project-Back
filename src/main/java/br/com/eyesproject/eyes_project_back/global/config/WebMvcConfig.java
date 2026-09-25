package br.com.eyesproject.eyes_project_back.global.config;

import br.com.eyesproject.eyes_project_back.global.security.ratelimit.PublicEndpointRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PublicEndpointRateLimitInterceptor publicEndpointRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(publicEndpointRateLimitInterceptor)
                .addPathPatterns("/api/v1/auth/**", "/api/v1/access-requests");
    }
}
