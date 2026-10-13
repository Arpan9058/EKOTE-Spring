package com.ekote.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Return the name of the template file for the home page.
        // Based on your file structure, this would be src/main/resources/templates/index.html

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/error")
    public String errorhandle() {
        return "error";
    }

    @GetMapping("/IssueReturn")
    public String issueReturn() {
        return "issueReturn";
    }

    @GetMapping("/Maintenance")
    public String maintenance() {
        return "addMaintenance";
    }


    @GetMapping("/HelpDashb")
    public String HelpDashb() {
        return "HelpDashb";
    }

    @GetMapping("/AddSpecific")
    public String add(){
        return "gunInfoForm";
    }


    @GetMapping("/QRCode")
    public String qr(){
        return "generateQr";
    }

    @GetMapping("/inventory_management")
    public String inventory(){
        return "inventory_management";
    }


}
