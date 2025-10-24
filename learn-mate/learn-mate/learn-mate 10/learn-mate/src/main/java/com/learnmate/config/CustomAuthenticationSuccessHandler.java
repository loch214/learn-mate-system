package com.learnmate.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Custom authentication success handler for role-based dashboard redirects
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        String redirectUrl = "/dashboard"; // Default fallback
        
        // Determine redirect URL based on user's role
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            
            switch (role) {
                case "ROLE_ADMIN":
                    redirectUrl = "/dashboard/admin";
                    break;
                case "ROLE_TEACHER":
                    redirectUrl = "/dashboard/teacher";
                    break;
                case "ROLE_STUDENT":
                    redirectUrl = "/dashboard/student";
                    break;
                case "ROLE_PARENT":
                    redirectUrl = "/dashboard/parent";
                    break;
                default:
                    redirectUrl = "/dashboard"; // Will redirect based on role using DashboardController
                    break;
            }
            
            // Break after finding the first (and typically only) role
            break;
        }
        
        // Clear any previous authentication errors from session
        request.getSession().removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        
        // Redirect to the appropriate dashboard
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
