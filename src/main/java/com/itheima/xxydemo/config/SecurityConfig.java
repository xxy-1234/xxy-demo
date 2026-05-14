package com.itheima.xxydemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                auth ->
                        auth.requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico")
                                .permitAll()
                                .requestMatchers("/error")
                                .permitAll()
                                .requestMatchers("/admin/**")
                                .hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/**")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/**")
                                .authenticated()
                                .requestMatchers("/login", "/register", "/logout", "/help", "/admin/login")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/", "/messages/**")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/register")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/messages/**")
                                .authenticated()
                                .anyRequest()
                                .authenticated());
        http.formLogin(form -> form.loginPage("/login").permitAll().defaultSuccessUrl("/", true));
        http.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
}
