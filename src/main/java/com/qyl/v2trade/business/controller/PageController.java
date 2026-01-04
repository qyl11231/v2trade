package com.qyl.v2trade.business.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器 - 重定向到静态页面
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/login.html";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    @GetMapping("/index")
    public String index() {
        return "redirect:/index.html";
    }

    @GetMapping("/users")
    public String users() {
        return "redirect:/users.html";
    }

    @GetMapping("/apikey")
    public String apiKey() {
        return "redirect:/apikey.html";
    }

    @GetMapping("/settings")
    public String settings() {
        return "redirect:/settings.html";
    }
}
