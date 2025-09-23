package com.psoft.clinic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthPageController {

    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // vai procurar login.html em templates
    }

    @GetMapping("/register/patient")
    public String showPatientRegisterPage() {
        return "register-patient"; // vai procurar register-patient.html
    }

}
