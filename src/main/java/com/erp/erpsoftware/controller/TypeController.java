package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.Type;
import com.erp.erpsoftware.service.TypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/types")
public class TypeController {
    @Autowired
    private TypeService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", service.getAllTypes());
        return "type-list";
    }

    @GetMapping("/new")
    public String newType(Model model) {
        model.addAttribute("type", new Type());
        return "type-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Type type) {
        service.saveType(type);
        return "redirect:/types";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("type", service.getTypeById(id));
        return "type-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            service.deleteType(id);
            redirectAttributes.addFlashAttribute("successMessage", "Type deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete this Type because it is referenced by other records (e.g. in Item Master).");
        }
        return "redirect:/types";
    }
}
