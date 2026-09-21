package br.com.eyesproject.eyes_project_back.global.config;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.ratelimit.AccessRequestRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AccessRequestRateLimitInterceptor accessRequestRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessRequestRateLimitInterceptor)
                .addPathPatterns("/api/v1/access-requests");
    }
}
