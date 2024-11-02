package edu.neu.csye7255.healthcare.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    GoogleTokenAuthFilter googleTokenAuthFilter;


    private static final List<String> WHITELIST_URLS = List.of(
            "/register",
            "/login",
            "/api/v1/auth/grantcode",
            "/accountverification",
            "/generateresetpasswordlink",
            "/changepassword",
            "/refreshaccesstoken",
            "/",
            "/index.html",
            "/register.html",
            "/forgot_password.html",
            "/styles.css",
            "/sign-google.jpg",
            "/sing-git.jpg"
    );

    //    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.authorizeHttpRequests(request -> {
//                    request.requestMatchers(WHITELIST_URLS).permitAll();
//                    request.anyRequest().authenticated();
//                })
//                .csrf(AbstractHttpConfigurer::disable)
//                .addFilterAfter(googleTokenAuthFilter, BasicAuthenticationFilter.class);
//        return http.build();
//    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        googleTokenAuthFilter.setWhitelistUrls(WHITELIST_URLS); // Pass the whitelist to the filter

        http.authorizeHttpRequests(request -> {
                    request.requestMatchers(WHITELIST_URLS.toArray(new String[0])).permitAll();
                    request.anyRequest().authenticated();
                })
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(googleTokenAuthFilter, BasicAuthenticationFilter.class);
        return http.build();
    }


}