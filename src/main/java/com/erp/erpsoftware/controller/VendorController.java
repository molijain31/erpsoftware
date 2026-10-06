package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.bean.VendorItemMappingDTO;
import com.erp.erpsoftware.entity.Vendor;
import com.erp.erpsoftware.entity.VendorItemMapping;
import com.erp.erpsoftware.repository.VendorItemMappingRepository;
import com.erp.erpsoftware.service.VendorService;
import com.erp.erpsoftware.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/vendors")
public class VendorController {
    @Autowired
    private VendorService service;

    @Autowired
    private ItemService itemService;

    @Autowired
    private VendorItemMappingRepository vendorItemMappingRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("vendors", service.getAllVendors());
        return "vendor-list";
    }

    @InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(java.util.Date.class, new org.springframework.beans.propertyeditors.CustomDateEditor(dateFormat, true));
    }

    @GetMapping("/new")
    public String newVendor(Model model) {
        Vendor vendor = new Vendor();
        if (vendor.getMappings() == null) {
            vendor.setMappings(new java.util.ArrayList<>());
        }
        vendor.getMappings().add(new VendorItemMappingDTO());
        model.addAttribute("vendor", vendor);
        model.addAttribute("items", itemService.getAllItems());
        return "vendor-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Vendor vendor, org.springframework.validation.BindingResult result, Model model) {
        if (result.hasErrors()) {
            System.out.println(">>> BINDING ERRORS FOUND: " + result.getAllErrors());
            model.addAttribute("items", itemService.getAllItems());
            return "vendor-form";
        }
        service.saveVendor(vendor);
        return "redirect:/vendors";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Vendor vendor = service.getVendorById(id);
        List<VendorItemMapping> entities = vendorItemMappingRepository.findByVendorId(id);
        List<VendorItemMappingDTO> dtos = new java.util.ArrayList<>();
        if (entities != null) {
            for (VendorItemMapping entity : entities) {
                VendorItemMappingDTO vendorItemMappingDTO = new VendorItemMappingDTO();
                vendorItemMappingDTO.setVenItemId(entity.getVenItemId());
                vendorItemMappingDTO.setVendorId(entity.getVendorId());
                vendorItemMappingDTO.setItem(itemService.getItemById(entity.getItemId()));
                vendorItemMappingDTO.setRate(entity.getRate());
                vendorItemMappingDTO.setFromDate(entity.getFromDate());
                vendorItemMappingDTO.setUptoDate(entity.getUptoDate());
                dtos.add(vendorItemMappingDTO);
            }
        }
        vendor.setMappings(dtos);
        if (vendor.getMappings() != null && vendor.getMappings().isEmpty()) {
            vendor.setMappings(List.of(new VendorItemMappingDTO()));
        }
        model.addAttribute("vendor", vendor);
        model.addAttribute("items", itemService.getAllItems());
        return "vendor-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteVendor(id);
        return "redirect:/vendors";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex) {
        try (java.io.FileWriter fw = new java.io.FileWriter("e:/erpsoftware/erpsoftware/error.log", true);
             java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
            pw.println("--- ERROR OCCURRED AT: " + new java.util.Date() + " ---");
            ex.printStackTrace(pw);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }
}
