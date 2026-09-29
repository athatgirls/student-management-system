package org.example.config;

import org.example.aspect.CurrentUserAspect;
import org.example.interceptor.VisitStatisticsInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CurrentUserAspect currentUserAspect;
    
    @Autowired
    private VisitStatisticsInterceptor visitStatisticsInterceptor;


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserAspect);
    }

    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitStatisticsInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**");
    }
}
