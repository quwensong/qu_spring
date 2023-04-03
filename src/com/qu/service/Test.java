package com.qu.service;

import com.qu.spring.QuApplicationContext;

public class Test {
    public static void main(String[] args) {
        QuApplicationContext applicationContext = new QuApplicationContext(AppConfig.class);
        UserService userService1 = (UserService)applicationContext.getBean("userService");

        userService1.test();


    }
}
