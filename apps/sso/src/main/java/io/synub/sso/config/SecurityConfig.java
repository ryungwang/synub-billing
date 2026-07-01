package io.synub.sso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SSO 보안 설정. 무상태(JWT 발급 서비스). 공개 엔드포인트(가입/로그인/JWKS/헬스)만 허용,
 * 그 외는 차단(scaffold 단계에선 구현 엔드포인트가 공개뿐이라 나머지는 기본 차단으로 안전하게).
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Bean
    org.springframework.security.web.SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/.well-known/**", "/actuator/health").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 로컬 프론트(빌링 3100)에서 로그인/가입 호출 허용. 운영은 실제 도메인으로 좁힐 것.
        registry.addMapping("/auth/**")
                .allowedOrigins("http://localhost:3100", "http://localhost:3000")
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
