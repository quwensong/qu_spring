package com.qu.service;

import com.qu.spring.BeanPostProcessor;
import com.qu.spring.Component;

@Component
public class QuBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(String beanName,Object bean) {
        if(beanName.equals("quBeanPostProcessor")){
            System.out.println("后置处理器，初始化前");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName,Object bean) {
        if(beanName.equals("quBeanPostProcessor")){
            System.out.println("后置处理器，初始化后");
        }
        return bean;
    }

}
