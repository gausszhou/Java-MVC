package com.example.mvc.controller;

import com.example.mvc.framework.core.ModelAndView;
import com.example.mvc.framework.annotation.GetMapping;
import com.example.mvc.bean.User;
import jakarta.servlet.http.HttpSession;


public class IndexController {

    @GetMapping("/")
    public ModelAndView index(HttpSession session) {
        User user = null;
        if (session != null) {
            user = (User) session.getAttribute("user");
        }
        return new ModelAndView("/index.html", "user", user);
    }

    @GetMapping("/hello")
    public ModelAndView hello(String name) {
        if (name == null) {
            name = "World";
        }
        return new ModelAndView("/hello.html", "name", name);
    }

    @GetMapping("/404")
    public ModelAndView notFound(String name) {
        if (name == null) {
            name = "World";
        }
        return new ModelAndView("/404.html", "name", name);
    }

}
