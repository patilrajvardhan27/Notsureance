package com.notsureance.claims.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    // Any single lowercase segment is an app page, so a new route could not 404 on refresh again.
    @GetMapping({"/", "/{page:[a-z-]+}", "/claims/**"})
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
