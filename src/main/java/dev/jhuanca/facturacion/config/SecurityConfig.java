/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.jhuanca.facturacion.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import dev.jhuanca.facturacion.service.CustomUserDetailsService;

/**
 *
 * @author javier
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/css/**", "/js/**", "/image/**", "/webjars/**")
                                                .permitAll()

                                                .requestMatchers("/login", "/forgot-password", "/reset-password",
                                                                "/error-page")
                                                .permitAll()

                                                .requestMatchers("/auth/**", "/services", "/api/**").permitAll()

                                                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                                                .requestMatchers("/pedidos/**", "/clientes/**", "/boletas/**",
                                                                "/servicios/**")
                                                .hasAnyRole("ADMIN", "TRABAJADOR")

                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureHandler(authenticationFailureHandler())
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .key("uniqueAndSecret")
                                                .tokenValiditySeconds(86400));
                return http.build();
        }

        @Bean
        public AuthenticationFailureHandler authenticationFailureHandler() {
                return new SimpleUrlAuthenticationFailureHandler("/login?error=true");
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
