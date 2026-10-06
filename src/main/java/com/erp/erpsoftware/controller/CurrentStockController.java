package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.CurrentStock;
import com.erp.erpsoftware.service.CurrentStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/stocks")
public class CurrentStockController {

    @Autowired
    private CurrentStockService service;

    @GetMapping
    public String list(Model model) {
        List<CurrentStock> allStocks = service.getAllStocks();
        List<CurrentStock> inStockList = new ArrayList<>();
        double totalStock = 0.0;

        if (allStocks != null) {
            for (CurrentStock stock : allStocks) {
                if (stock != null && stock.getCurrentStock() != null && stock.getCurrentStock() > 0.0001) {
                    inStockList.add(stock);
                    totalStock += stock.getCurrentStock();
                }
            }
        }

        model.addAttribute("stocks", inStockList);
        model.addAttribute("totalStock", totalStock);

        return "Current-stock";
    }
}