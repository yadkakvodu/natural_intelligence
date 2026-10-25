package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class NewsWebController {

    @GetMapping("/news")
    public String showNewsPage(Model model) {


        model.addAttribute("pageTitle", "Новостной консультант");

        // Возвращаем имя шаблона (Spring сам найдет news.html в папке templates)
        return "index";
    }
}