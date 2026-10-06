package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.Unit;

import com.erp.erpsoftware.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/units")
public class UnitController {
    @Autowired
    private UnitService service;


    @GetMapping
    public String list(Model model) {
        try {
            model.addAttribute("units", service.getAllUnits());
            return "unit-list";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/new")
    public String newUnit(Model model) {
        model.addAttribute("unit", new Unit());
        return "unit-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Unit unit) {
        service.saveUnit(unit);
        return "redirect:/units";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("unit", service.getUnitById(id));
        return "unit-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.deleteUnit(id);
        return "redirect:/units";
    }
}
