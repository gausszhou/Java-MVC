package com.example.mvc.controller;

import com.example.mvc.bean.SignInBean;
import com.example.mvc.framework.core.ModelAndView;
import com.example.mvc.framework.annotation.GetMapping;
import com.example.mvc.bean.User;
import com.example.mvc.framework.annotation.PostMapping;
import com.example.mvc.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

public class UserController {
    UserService userService = new UserService();

    @GetMapping("/signIn")
    public ModelAndView signIn() {
        return new ModelAndView("/signIn.html");
    }

    @PostMapping("/signIn")
    public ModelAndView doSignIn(SignInBean signInBean, HttpServletResponse response, HttpSession session) throws IOException {
        User user = userService.getUserByEmail(signInBean.email);
        if (user == null || !user.password.equals(signInBean.password)) {
            response.setContentType("application/json");
            PrintWriter pw = response.getWriter();
            pw.write("{\"error\":\"Bad email or password\"}");
            pw.flush();
        } else {
            if (session != null) session.setAttribute("user", user);
            response.setContentType("application/json");
            PrintWriter pw = response.getWriter();
            pw.write("{\"result\":true}");
            pw.flush();
        }
        return null;
    }

    @GetMapping("/signout")
    public ModelAndView signout(HttpSession session) {
        session.removeAttribute("user");
        return new ModelAndView("redirect:/");
    }

    @GetMapping("/user/profile")
    public ModelAndView profile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return new ModelAndView("redirect:/signin");
        }
        return new ModelAndView("/profile.html", "user", user);
    }
}
