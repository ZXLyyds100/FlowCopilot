package com.kama.jchatmind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置类。
 * <p>
 * 允许本地开发环境中的前端页面跨域访问后端接口与 SSE 通道。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    /**
     * 通过 Spring MVC 配置全局跨域规则。
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许跨域的域名
                // 注意：如果 allowCredentials 为 true，不能使用 "*"，需要指定具体域名
                // 开发环境可以使用 "http://localhost:5173", "http://localhost:3000" 等
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                // 允许任何方法（post、get等）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // 允许任何请求头
                .allowedHeaders("*")
                // 允许携带凭证（如果需要的话）
                .allowCredentials(true)
                // 预检请求的有效期，单位为秒
                .maxAge(3600);
    }

    /**
     * 构造基于 {@link CorsFilter} 的跨域过滤器。
     * <p>
     * 这是对 {@link #addCorsMappings(CorsRegistry)} 的补充实现，
     * 在部分请求链路或过滤器优先级场景下能提供更稳定的跨域处理。
     *
     * @return CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许本地开发环境的跨域调用
        // 如果需要允许所有域名，可以注释掉 allowCredentials(true) 并使用 "*"
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 允许携带凭证
        config.setAllowCredentials(true);
        // 预检请求的有效期，单位为秒
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
