package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.service.ItemService;
import com.erp.erpsoftware.service.TypeService;
import com.erp.erpsoftware.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemService service;

    @Autowired
    private UnitService unitService;

    @Autowired
    private TypeService typeService;

    @GetMapping
    public String list(Model model) {
        try {
            model.addAttribute("items", service.getAllItems());
            return "item-list";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/new")
    public String newItem(Model model) {
        System.err.println("item >>> " + new Item());
        model.addAttribute("item", new Item());
        model.addAttribute("units", unitService.getAllUnits());
        model.addAttribute("types", typeService.getAllTypes());
        return "item-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Item item) {
        service.saveItem(item);
        return "redirect:/items";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("item", service.getItemById(id));
        model.addAttribute("units", unitService.getAllUnits());
        model.addAttribute("types", typeService.getAllTypes());
        return "item-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteItem(id);
        return "redirect:/items";
    }
}
