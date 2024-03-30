package com.example.mvc.service;

import com.example.mvc.bean.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {
    private Map<String, User> userDatabase = createUserDataBase();

    private Map<String, User> createUserDataBase() {
        List<User> users = new ArrayList<>();
        users.add(new User("bob@example.com", "bob123", "Bob", "This is bob."));
        users.add(new User("tom@example.com", "tomcat", "Tom", "This is tom."));
        Map<String, User> userDatabase = new HashMap<>();
        users.forEach(user -> {
            userDatabase.put(user.email, user);
        });
        return userDatabase;
    }

    public User getUserByEmail(String email){
        return userDatabase.get(email);
    }
}
