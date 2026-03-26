package com.insurance.autoinsurance.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
@RequestMapping("/calculator")
public class PremiumCalculatorController {

    @GetMapping
    public String page(Model model) {
        model.addAttribute("activePage", "calculator");
        return "tools/calculator";
    }

    @GetMapping("/api/calculate")
    @ResponseBody
    public ResponseEntity<?> calculate(
            @RequestParam String coverageType,
            @RequestParam int vehicleAge,
            @RequestParam double idv,
            @RequestParam(required = false, defaultValue = "false") boolean zeroDep,
            @RequestParam(required = false, defaultValue = "false") boolean roadside,
            @RequestParam(required = false, defaultValue = "false") boolean engine,
            @RequestParam(required = false, defaultValue = "0") int ncbPercent) {

        // Base rate by coverage type
        double baseRate;
        switch (coverageType) {
            case "Comprehensive":    baseRate = 0.034; break;
            case "Third Party":      baseRate = 0.010; break;
            case "Own Damage":       baseRate = 0.025; break;
            case "Zero Dep":         baseRate = 0.042; break;
            case "EV Comprehensive": baseRate = 0.038; break;
            default:                 baseRate = 0.030;
        }
        // Age depreciation
        double ageFactor = vehicleAge <= 1 ? 1.0 : vehicleAge <= 3 ? 0.85 :
                           vehicleAge <= 5 ? 0.70 : vehicleAge <= 7 ? 0.60 : 0.50;

        double basePremium = idv * baseRate * ageFactor;

        // Add-on premiums
        double addons = 0;
        if (zeroDep)  addons += basePremium * 0.15;
        if (roadside) addons += 750;
        if (engine)   addons += basePremium * 0.10;

        // NCB discount
        double ncbDiscount = basePremium * (ncbPercent / 100.0);

        double netPremium = basePremium + addons - ncbDiscount;
        double gst        = netPremium * 0.18;
        double totalPremium = netPremium + gst;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("idv",           idv);
        res.put("basePremium",   Math.round(basePremium * 100) / 100.0);
        res.put("addons",        Math.round(addons * 100) / 100.0);
        res.put("ncbDiscount",   Math.round(ncbDiscount * 100) / 100.0);
        res.put("netPremium",    Math.round(netPremium * 100) / 100.0);
        res.put("gst",           Math.round(gst * 100) / 100.0);
        res.put("totalPremium",  Math.round(totalPremium * 100) / 100.0);
        res.put("monthlyCost",   Math.round(totalPremium / 12 * 100) / 100.0);
        return ResponseEntity.ok(res);
    }
}
