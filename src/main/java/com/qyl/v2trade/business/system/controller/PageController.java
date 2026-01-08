package com.qyl.v2trade.business.system.controller;

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

    // ==================== 指标管理模块路由 ====================
    
    @GetMapping("/indicator/dashboard")
    public String indicatorDashboard() {
        return "redirect:/indicator/dashboard.html";
    }

    @GetMapping("/indicator/definitions")
    public String indicatorDefinitions() {
        return "redirect:/indicator/definitions.html";
    }

    @GetMapping("/indicator/subscriptions")
    public String indicatorSubscriptions() {
        return "redirect:/indicator/subscriptions.html";
    }

    @GetMapping("/indicator/values")
    public String indicatorValues() {
        return "redirect:/indicator/values.html";
    }

    @GetMapping("/indicator/logs")
    public String indicatorLogs() {
        return "redirect:/indicator/logs.html";
    }
}
