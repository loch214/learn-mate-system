package com.learnmate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeSpecificErrorMessageWhenAuthenticationExceptionPresent() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException("Bad credentials"));

        mockMvc.perform(MockMvcRequestBuilders.get("/login")
                        .param("error", "true")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loginError", "Bad credentials"));
    }

    @Test
    void shouldFallbackToGenericMessageWhenNoExceptionStored() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login")
                        .param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loginError", "Invalid username or password."));
    }

    @Test
    void shouldStillShowMessageWhenSessionContainsExceptionWithoutErrorParam() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException("Bad credentials"));

        mockMvc.perform(MockMvcRequestBuilders.get("/login")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loginError", "Bad credentials"));
    }
}
