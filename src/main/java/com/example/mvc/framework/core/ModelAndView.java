package com.example.mvc.framework.core;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

    public HashMap<String, Object> model;
    public String view;

    public ModelAndView(String view) {
        System.out.println("ModelAndView");
        this.view = view;
        this.model = new HashMap<>();
    }

    public ModelAndView(String view, String name, Object value) {
        this.view = view;
        this.model = new HashMap<>();
        this.model.put(name, value);
    }

    public ModelAndView(String view, Map<String, Object> model) {
        this.view = view;
        this.model = new HashMap<>(model);
    }
}
