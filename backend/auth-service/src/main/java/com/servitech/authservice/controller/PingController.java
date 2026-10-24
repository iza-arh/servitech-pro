package com.servitech.authservice.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public String ping(Authentication auth) {
        return "ok " + auth.getName() + " " + auth.getAuthorities();
    }

    @GetMapping("/admin/ping")
    public String adminPing() {
        return "solo admin";
    }
}