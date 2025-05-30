package com.foodkg.test_jena.controler;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.stereotype.Controller;

@Controller
public class FoodWebController {

    // ===== REDIRECTIONS VERS LE FICHIER STATIQUE =====

    @GetMapping("/")
    public String home() {
        return "forward:/static/food-kg-frontend.html";
    }

    @GetMapping("/app")
    public String app() {
        return "forward:/static/food-kg-frontend.html";
    }

    @GetMapping("/frontend")
    public String frontend() {
        return "forward:/static/food-kg-frontend.html";
    }

    @GetMapping("/search")
    public String search() {
        return "forward:/static/food-kg-frontend.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "forward:/static/food-kg-frontend.html";
    }

    @GetMapping("/ui")
    public String ui() {
        return "forward:/static/food-kg-frontend.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/static/food-kg-frontend.html";
    }

    // ===== GESTION DES ROUTES SPA =====
    @GetMapping({ "/food/**", "/app/**", "/frontend/**" })
    public String spaRoutes() {
        return "forward:/static/food-kg-frontend.html";
    }
}