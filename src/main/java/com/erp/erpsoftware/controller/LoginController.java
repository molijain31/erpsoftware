package com.erp.erpsoftware.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping({"/", "/index"})
    public String indexPage(){
        return "redirect:/sales";
    }

    @GetMapping("/login")
    public String login(Model model){
        model.addAttribute("msg","hello word testing");
        return "layout";
    }
}
