package org.base.core.controller.web;

import org.base.core.anotation.Web;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Web
@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "home";  // will use home.html template
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";  // will use dashboard.html template
    }

}

