package com.qu.spring;

public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(String name,Object bean) {
        return bean;
    }

    default Object postProcessAfterInitialization(String name,Object bean) {
        return bean;
    }
}
