package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.service.AccountLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/reports")
public class AccountLedgerController {

    @Autowired
    private AccountLedgerService accountLedgerService;

    @GetMapping("/account-ledger")
    public String accountLedgerReport(Model model) {
        try {
            model.addAttribute("suppliers", accountLedgerService.getAllSuppliers());
            model.addAttribute("vendors", accountLedgerService.getAllVendors());
        } catch (Exception e) {
            System.err.println("Error loading suppliers/vendors for account ledger report: " + e.getMessage());
        }
        return "account-ledger-report";
    }

    @GetMapping("/account-ledger/data")
    @ResponseBody
    public Map<String, Object> getAccountLedgerData(@RequestParam(value = "type", defaultValue = "supplier") String type,
                                                    @RequestParam(value = "id") Long id) {
        return accountLedgerService.getAccountLedgerData(type, id);
    }
}
