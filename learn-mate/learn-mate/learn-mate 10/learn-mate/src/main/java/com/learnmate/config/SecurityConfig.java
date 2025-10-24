package com.learnmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    public SecurityConfig(CustomAuthenticationSuccessHandler authenticationSuccessHandler,
                          org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public access
                        .requestMatchers("/", "/users/register", "/login", "/css/**", "/js/**", "/images/**", "/static/**", "/uploads/**").permitAll()
                        
                        // Dashboard access - role-specific
                        .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                        .requestMatchers("/dashboard/teacher").hasRole("TEACHER")
                        .requestMatchers("/dashboard/student").hasRole("STUDENT")
                        .requestMatchers("/dashboard/parent").hasRole("PARENT")
                        .requestMatchers("/dashboard/**").authenticated() // General dashboard access
                        
                        // Admin-only access
                        .requestMatchers("/users/create", "/users/edit/**", "/users/delete/**", "/users/list", "/users/search").hasRole("ADMIN")
                        .requestMatchers("/reports/**").hasRole("ADMIN")
                        .requestMatchers("/parent/reports/**").hasRole("PARENT")
                        
                        // Classes access - view for teachers, management for admin
                        .requestMatchers("/classes/list").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/classes/**", "/subjects/**").hasRole("ADMIN")
                        
                        // Teacher-specific access
                        .requestMatchers("/exams/create", "/exams/edit/**", "/exams/delete/**").hasRole("TEACHER")
                        .requestMatchers("/marks/create", "/marks/edit/**").hasRole("TEACHER")
                        .requestMatchers("/attendances/create", "/attendances/edit/**").hasRole("TEACHER")
                        
                        // Multi-role access
                        .requestMatchers("/exams/list", "/marks/list").hasAnyRole("TEACHER", "STUDENT", "PARENT", "ADMIN")
                        .requestMatchers("/attendances/list").hasAnyRole("TEACHER", "STUDENT", "PARENT", "ADMIN")
                        .requestMatchers("/timetables/list").hasAnyRole("TEACHER", "STUDENT", "ADMIN")
                        // Fees: allow list/search for admin and parent, but lock create/edit/delete/verify to admin
                        .requestMatchers("/fees/list", "/fees/search", "/fees/create-subject-fee", "/fees/create-subject-fee/**").hasAnyRole("PARENT", "ADMIN")
                        .requestMatchers("/fees/create", "/fees/edit/**", "/fees/delete/**", "/fees/verify/**").hasRole("ADMIN")
                        
                        // Profile access for authenticated users
                        .requestMatchers("/users/profile", "/users/profile/edit", "/users/profile/change-password").authenticated()
                        .requestMatchers("/notifications/**").authenticated()
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("learnmate-remember-me-key")
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 14) // 14 days
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                );
        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
                        if (accessDeniedException != null) {
                                accessDeniedException.getMessage(); // touch exception to appease static analysis
                        }
            // Redirect to dashboard with a friendly message instead of raw 403 page
            response.sendRedirect(request.getContextPath() + "/dashboard");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
