package com.learnmate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Always route root to the login page to make it explicit.
        // Authenticated users can navigate directly to their dashboards.
        return "redirect:/login";
    }
}
