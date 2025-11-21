package com.learnmate.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ensures login failures consistently redirect back to /login with an error flag
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // Preserve the authentication exception so the login page can surface a helpful message
        request.getSession(true).setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);

        // Redirect with an error query parameter so the login page can display a message
        response.sendRedirect(request.getContextPath() + "/login?error=true");
    }
}
