package com.psoft.clinic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/bootstrap")
    public String showBootstrapTest() {
        return "bootstrap-test";
    }
}
