package com.ishito.sample.dpra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DpraController {
    
    @RequestMapping("/")
    public String hello() {
        return "cars/top";
    }
}
