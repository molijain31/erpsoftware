package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.User;
import com.erp.erpsoftware.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", service.getAllUsers());
        return "user-list";
    }

    @GetMapping("/new")
    public String newUser(Model model) {
        model.addAttribute("user", new User());
        return "user-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute User user) {
        service.saveUser(user);
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("user", service.getUserById(id));
        return "user-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.deleteUser(id);
        return "redirect:/users";
    }
}
