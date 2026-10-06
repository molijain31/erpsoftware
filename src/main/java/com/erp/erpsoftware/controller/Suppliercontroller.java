package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.service.SupplierService;
import com.erp.erpsoftware.entity.SupplierItemMapping;
import com.erp.erpsoftware.service.ItemService;
import com.erp.erpsoftware.service.TypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/suppliers")
public class Suppliercontroller{

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private TypeService typeService;

    // Display Supplier List
    @GetMapping
    public String viewSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "Supplier-list";
    }

    // Show Add Supplier Form
    @GetMapping("/add")
    public String showAddSupplierForm(Model model) {
        Supplier supplier = new Supplier();
        supplier.getMappings().add(new SupplierItemMapping());
        model.addAttribute("supplier", supplier);
        model.addAttribute("items", itemService.getAllItems());
        model.addAttribute("types", typeService.getAllTypes());
        return "Supplier-form";
    }

    @PostMapping("/save")
    public String saveSupplier(@ModelAttribute Supplier supplier) {
        if (supplier.getMappings() != null) {
            for (SupplierItemMapping mapping : supplier.getMappings()) {
                if (mapping != null) {
                    mapping.setSupplier(supplier);
                }
            }
        }
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }
    // Show Edit Supplier Form
    @GetMapping("/edit/{id}")
    public String editSupplier(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.getSupplierById(id);
        if (supplier.getMappings().isEmpty()) {
            supplier.getMappings().add(new SupplierItemMapping());
        }
        model.addAttribute("supplier", supplier);
        model.addAttribute("items", itemService.getAllItems());
        model.addAttribute("types", typeService.getAllTypes());
        return "Supplier-form";
    }

    // Delete Supplier
    @GetMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return "redirect:/suppliers";
    }
}




