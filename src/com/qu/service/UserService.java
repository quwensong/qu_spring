package com.qu.service;

import com.qu.spring.*;

@Scope
@Component("userService")
public class UserService implements BeanNameAware, InitializingBean {

    @Autowired
    private SchoolDao schoolDao;
    private String beanName;

    public void test(){
        System.out.println(schoolDao);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() {

    }
}
