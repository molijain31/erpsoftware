package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.SupplierAccountPaypal;
import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.service.SupplierAccountPaypalService;
import com.erp.erpsoftware.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/supplier-accounts")
public class SupplierAccountPaypalController {

    @Autowired
    private SupplierAccountPaypalService service;

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public String list(Model model) {
        try {
            model.addAttribute("accounts", service.getAllSupplierAccounts());
        } catch (Exception e) {
            System.err.println("Error rendering supplier accounts list: " + e.getMessage());
            model.addAttribute("accounts", new java.util.ArrayList<>());
            model.addAttribute("errorMessage", "Error loading supplier accounts: " + e.getMessage());
        }
        return "supplier-account-list";
    }

    @GetMapping("/new")
    public String newAccount(@RequestParam(value = "supplierId", required = false) Long supplierId, Model model) {
        SupplierAccountPaypal account = new SupplierAccountPaypal();
        account.setPaymentMode("UPI");
        if (supplierId != null) {
            account.setSupplierId(supplierId);
            try {
                Supplier s = supplierService.getSupplierById(supplierId);
                if (s != null) {
                    account.setAccountName(s.getSupplierName());
                    account.setSupplierName(s.getSupplierName());
                    account.setPaypalEmail(s.getEmail() != null ? s.getEmail() : "");
                }
            } catch (Exception ignored) {}
        }
        model.addAttribute("account", account);
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        populateSupplierSummaries(model);
        return "supplier-account-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("account") SupplierAccountPaypal account,
                       @RequestParam(value = "brBookingNos", required = false) List<String> brBookingNos,
                       @RequestParam(value = "brPaymentAmounts", required = false) List<Double> brPaymentAmounts,
                       @RequestParam(value = "adjustAmount", required = false) Double adjustAmount,
                       RedirectAttributes redirectAttributes) {
        try {
            service.saveAccount(account, brBookingNos, brPaymentAmounts, adjustAmount);
            redirectAttributes.addFlashAttribute("successMessage", "Supplier Account Payment saved successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving supplier account payment: " + e.getMessage());
        }
        return "redirect:/supplier-accounts";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("account", service.getAccountById(id));
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        populateSupplierSummaries(model);
        return "supplier-account-form";
    }

    @GetMapping("/view/supplier/{supplierId}")
    public String viewSupplierAccount(@PathVariable Long supplierId, Model model) {
        Supplier supplier = supplierService.getSupplierById(supplierId);
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier not found with id: " + supplierId);
        }

        SupplierAccountPaypal account = service.getAllSupplierAccounts().stream()
                .filter(acc -> supplierId.equals(acc.getSupplierId()))
                .findFirst()
                .orElse(null);

        Map<String, Object> summary = service.getSupplierReceivedSummary(supplierId);

        model.addAttribute("supplier", supplier);
        model.addAttribute("account", account);
        model.addAttribute("summary", summary);
        return "supplier-account-view";
    }

    @GetMapping("/view/{id}")
    public String viewAccountById(@PathVariable Long id, Model model) {
        SupplierAccountPaypal account = service.getAccountById(id);
        if (account != null && account.getSupplierId() != null) {
            return viewSupplierAccount(account.getSupplierId(), model);
        }
        throw new IllegalArgumentException("Account not found or no supplier attached for id: " + id);
    }

    @GetMapping("/supplier/{supplierId}/received-summary")
    @ResponseBody
    public Map<String, Object> getSupplierReceivedSummary(@PathVariable Long supplierId) {
        return service.getSupplierReceivedSummary(supplierId);
    }

    @GetMapping("/supplier/{supplierId}/history")
    @ResponseBody
    public Map<String, Object> getSupplierAccountHistory(@PathVariable Long supplierId) {
        return service.getSupplierPaymentHistory(supplierId);
    }

    private void populateSupplierSummaries(Model model) {
        Map<Long, Integer> qtyMap = new HashMap<>();
        Map<Long, Double> costMap = new HashMap<>();
        for (Supplier s : supplierService.getAllSuppliers()) {
            Map<String, Object> summary = service.getSupplierReceivedSummary(s.getSupplierId());
            qtyMap.put(s.getSupplierId(), (Integer) summary.get("totalFullyReceivedQty"));
            costMap.put(s.getSupplierId(), (Double) summary.get("totalFullyReceivedCost"));
        }
        model.addAttribute("supplierQtyMap", qtyMap);
        model.addAttribute("supplierCostMap", costMap);
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "Supplier PayPal Account deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting account: " + e.getMessage());
        }
        return "redirect:/supplier-accounts";
    }
}
