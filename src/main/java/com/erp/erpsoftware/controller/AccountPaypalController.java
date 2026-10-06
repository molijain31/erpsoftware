package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.AccountPaypal;
import com.erp.erpsoftware.service.AccountPaypalService;
import com.erp.erpsoftware.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/accounts")
public class AccountPaypalController {

    @Autowired
    private AccountPaypalService service;

    @Autowired
    private VendorService vendorService;

    @GetMapping
    public String list(Model model) {
        try {
            model.addAttribute("accounts", service.getAllAccounts());
        } catch (Exception e) {
            System.err.println("Error rendering accounts list: " + e.getMessage());
            model.addAttribute("accounts", new java.util.ArrayList<>());
            model.addAttribute("errorMessage", "Error loading accounts: " + e.getMessage());
        }
        return "account-list";
    }

    @GetMapping("/new")
    public String newAccount(@RequestParam(value = "vendorId", required = false) Long vendorId, Model model) {
        AccountPaypal account = new AccountPaypal();
        if (vendorId != null) {
            account.setVendorId(vendorId);
            try {
                com.erp.erpsoftware.entity.Vendor v = vendorService.getVendorById(vendorId);
                if (v != null) {
                    account.setAccountName(v.getVendorName());
                    account.setVendorName(v.getVendorName());
                    account.setPaypalEmail(v.getTransactionAccount() != null ? v.getTransactionAccount() : "");
                }
            } catch (Exception ignored) {}
        }
        model.addAttribute("account", account);
        model.addAttribute("vendors", vendorService.getAllVendors());
        populateVendorSummaries(model);
        return "account-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("account") AccountPaypal account,
                       @RequestParam(value = "poOrderIds", required = false) java.util.List<Long> poOrderIds,
                       @RequestParam(value = "poPaymentAmounts", required = false) java.util.List<Double> poPaymentAmounts,
                       RedirectAttributes redirectAttributes) {
        try {
            service.saveAccount(account, poOrderIds, poPaymentAmounts);
            redirectAttributes.addFlashAttribute("successMessage", "PayPal Account saved successfully!");
        } catch (Exception e) {
            System.err.println("Error saving account: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("account", service.getAccountById(id));
        model.addAttribute("vendors", vendorService.getAllVendors());
        populateVendorSummaries(model);
        return "account-form";
    }

    @GetMapping("/view/vendor/{vendorId}")
    public String viewVendorAccount(@PathVariable Long vendorId, Model model) {
        com.erp.erpsoftware.entity.Vendor vendor = vendorService.getVendorById(vendorId);
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor not found with id: " + vendorId);
        }

        AccountPaypal account = service.getAllVendorAccounts().stream()
                .filter(acc -> vendorId.equals(acc.getVendorId()))
                .findFirst()
                .orElse(null);

        Map<String, Object> summary = service.getVendorFullyReceivedSummary(vendorId);

        model.addAttribute("vendor", vendor);
        model.addAttribute("account", account);
        model.addAttribute("summary", summary);
        return "account-view";
    }

    @GetMapping("/view/{id}")
    public String viewAccountById(@PathVariable Long id, Model model) {
        AccountPaypal account = service.getAccountById(id);
        if (account != null && account.getVendorId() != null) {
            return viewVendorAccount(account.getVendorId(), model);
        }
        throw new IllegalArgumentException("Account not found or no vendor attached for id: " + id);
    }

    @GetMapping("/vendor/{vendorId}/received-summary")
    @ResponseBody
    public java.util.Map<String, Object> getVendorReceivedSummary(@PathVariable Long vendorId) {
        return service.getVendorFullyReceivedSummary(vendorId);
    }

    @GetMapping("/vendor/{vendorId}/history")
    @ResponseBody
    public java.util.Map<String, Object> getVendorPaymentHistory(@PathVariable Long vendorId) {
        return service.getVendorPaymentHistory(vendorId);
    }

    private void populateVendorSummaries(Model model) {
        java.util.Map<Long, Integer> qtyMap = new java.util.HashMap<>();
        java.util.Map<Long, Double> costMap = new java.util.HashMap<>();
        try {
            java.util.List<com.erp.erpsoftware.entity.Vendor> vendors = vendorService.getAllVendors();
            if (vendors != null) {
                for (com.erp.erpsoftware.entity.Vendor v : vendors) {
                    if (v != null && v.getVendorId() != null) {
                        try {
                            java.util.Map<String, Object> summary = service.getVendorFullyReceivedSummary(v.getVendorId());
                            if (summary != null) {
                                Number q = (Number) summary.get("totalFullyReceivedQty");
                                Number c = (Number) summary.get("totalFullyReceivedCost");
                                qtyMap.put(v.getVendorId(), q != null ? q.intValue() : 0);
                                costMap.put(v.getVendorId(), c != null ? c.doubleValue() : 0.0);
                            }
                        } catch (Exception e) {
                            System.err.println("Error calculating summary for vendor " + v.getVendorId() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error populating vendor summaries: " + e.getMessage());
        }
        model.addAttribute("vendorQtyMap", qtyMap);
        model.addAttribute("vendorCostMap", costMap);
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "PayPal Account deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }
}
