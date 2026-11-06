package com.notsureance.claims.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    @GetMapping({"/", "/login", "/quote", "/dashboard", "/claims/**", "/queue", "/leads", "/insights"})
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
