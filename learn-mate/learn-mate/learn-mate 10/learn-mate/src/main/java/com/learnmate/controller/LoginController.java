package com.learnmate.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        HttpServletRequest request,
                        Model model) {
        AuthenticationException exception = extractAuthenticationException(request);

        if (error != null || exception != null) {
            model.addAttribute("loginError", buildErrorMessage(exception));
        }
        return "login";
    }

    private AuthenticationException extractAuthenticationException(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (exception instanceof AuthenticationException authException) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            return authException;
        }

        return null;
    }

    private String buildErrorMessage(AuthenticationException exception) {
        if (exception == null) {
            return "Invalid username or password.";
        }
        return exception.getMessage() != null ? exception.getMessage() : "Invalid username or password.";
    }
}
