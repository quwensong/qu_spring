package com.qu.spring;

/**
 * Bean的定义类
 */
public class BeanDefinition {
    private String scope;
    private Class type;

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }



}
