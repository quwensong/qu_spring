package com.qu.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class QuApplicationContext {

    private Class configClass;
    private ConcurrentHashMap<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    //单例池
    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>();
    //保存实现了BeanPostProcessor接口的class
    private ArrayList<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public QuApplicationContext(Class configClass) {
        this.configClass = configClass;

        //1.扫描
        /*
         * 例：A.isAnnotationPresent(B.class);
         * 大白话：B类型的注解是否在A类上。
         */
        if(configClass.isAnnotationPresent(ComponentScan.class)){
            ComponentScan componentScanAnnotation = (ComponentScan)configClass.getAnnotation(ComponentScan.class);
            // 此处得到@ComponentScan的扫描路径
            String path = componentScanAnnotation.value();//扫描路径 com.qu.service
            path = path.replace(".","/");

            ClassLoader classLoader = QuApplicationContext.class.getClassLoader();
            // 得到类的包路径(此处为com.sbi.service格式需要改成com/sbi/service)
            URL resource = classLoader.getResource(path);
            // 得到包下的文件
            File file = new File(resource.getFile());

            if(file.isDirectory()){
                // 得到包下所有的文件信息
                File[] files = file.listFiles();

                for (File f : files) {
                    String fileName = f.getAbsolutePath();

                    if(fileName.endsWith(".class")){
                        /*
                         *Class.forName得到的class是已经初始化完成的；
                         * Class.forName(xxx.xx.xx)的作用是要求JVM查找并加载指定的类，
                         * 也就是说JVM会执行该类的静态代码段
                         *
                         * Classloder.loaderClass得到的class是还没有链接的；
                         * 例如：数据库驱动加载就是使用Class.froName(“com.mysql.jdbc.Driver”),
                         *
                         * 而loadClass直接就干"加载类"的这些事，不干多余的事儿。
                         * 而class.forName默认是老好人，就把加载静态代码块的工作也一并做了
                         */
                        String className = fileName.substring(fileName.lastIndexOf("com"), fileName.indexOf(".class"));
                        // 此处最终需要com.qu.service
                        className = className.replace("\\",".");
                        Class<?> clazz = null;
                        try {
                            //获取添加了@Component注解对应的类 clazz
                            clazz = classLoader.loadClass(className);

                            if(clazz.isAnnotationPresent(Component.class)){
                                /*
                                 * 扫描@Comptent注解发现有实现BeanPostProcessor的类
                                 * 如果是A.isAssignableFrom(B) 确定一个类(B)是不是继承来自于另一个父类(A)，
                                 * 一个接口(A)是不是实现了另外一个接口(B)，或者两个类相同。
                                 *
                                 * 1.通过无参构造函数，创建一个新的实例对象
                                 * Object obj=clazz.newInstance();//相当于调用new
                                 * 2.通过参数个数和类型类确定调用哪个构造函数
                                 * Constructor constructor=clazz.getConstructor(String.class);
                                 * 3.调用有参构造函数，并且传递参数值
                                 * Object obj=constructor.newInstance("郭靖");相当于调用new
                                 */
                                if(BeanPostProcessor.class.isAssignableFrom(clazz)){
                                    System.out.println("999");
                                    // 就将此类通过反射创建出来
                                    //getDeclaredConstructor 该Class的所有已声明的构造函数(私有，公共，保护，默认)。
                                    BeanPostProcessor o = (BeanPostProcessor)clazz.getDeclaredConstructor().newInstance();
                                    // 将这个后置处理器加到list里
                                    beanPostProcessorList.add(o);
                                }


                                // 描述类创建
                                BeanDefinition beanDefinition = new BeanDefinition();
                                Component component = clazz.getAnnotation(Component.class);
                                // 拿到@Component的value值也就是bean的名字
                                String beanName = component.value();
                                //如果没有@Component注解没有传入bean名字，就默认用类名字
                                if (beanName.equals("")){
                                    //Introspector.decapitalize 如果字符串的第二个字符非大写，则将字符串的第一个字符转换为小写,并返回转换后的字符
                                    //getSimpleName 得到类的简写名称
                                    beanName = Introspector.decapitalize(clazz.getSimpleName());
                                }

                                beanDefinition.setType(clazz);
                                // 判断这个类是否还持有@Scope的其他作用域（spring 一共有四种作用域，默认不指定的情况下就是单例）
                                if(clazz.isAnnotationPresent(Scope.class)){
                                    Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                    String value = scopeAnnotation.value();
                                    beanDefinition.setScope(value);
                                }else{
                                    beanDefinition.setScope("singleton");
                                }
                                // 将类信息放到beanDefinitionMap里
                                beanDefinitionMap.put(beanName,beanDefinition);
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        //2.扫描完毕以后，创建单例 bean 对象池
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName,bean);
            }
        }

    }
    //创建bean
    private Object createBean(String beanName,BeanDefinition beanDefinition){
        //bean对应的类 clazz
        Class clazz = beanDefinition.getType();
        try {
            //1.创建bean对象
            Object instance = clazz.getConstructor().newInstance();
            //2.获取类的所有属性 getDeclaredFields主要功能是获取指定类所声明的属性(包括public、protected、private)
            //依赖注入

            /*
             * Field.set()方法的功能:
             *    为对象的Field属性设置value
             * Field.set()方法的语法:
             *    set(Object obj, Object value)
             *      为变量设置新值
             */
            // 先通过反射拿到这个clazz的私有属性
            Field[] declaredFields = instance.getClass().getDeclaredFields();// 或者 clazz.getDeclaredFields()
            //遍历私有属性
            for (Field field : declaredFields) {
                // 判断私有属性的是否持有@Autowired
                if(field.isAnnotationPresent(Autowired.class)){
                    // 如果持有@Autowired 就从单例池里拿这个bean并赋值
                    Object bean = getBean(field.getName());
                    //field.setAccessible(true) 设置可以访问private变量的变量值
                    field.setAccessible(true);
                    // 实现依赖注入
                    field.set(instance, bean);
                }
            }
            //3.Aware回调以及初始化方法
            // 如果当前对象实现了BeanNameAware接口
            // instanceof 它左边的对象是否是它右边的类的实例
            //左边是对象，右边是类；当对象是右边类或子类所创建对象时，返回true；否则，返回false。
            if(instance instanceof BeanNameAware){
                ((BeanNameAware) instance).setBeanName(beanName);
            }
            //在原生的Spring框架中，除了Aware，和初始化回调方法,还有一个叫后置处理器（BeanPostProcessor）可以自定义Bean
            //4.BeanPostProcessor的前置方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(beanName,instance);

            }
            //5.初始化回调
            if (instance instanceof InitializingBean){
                ((InitializingBean) instance).afterPropertiesSet();
            }
            // 如此一来，一个bean的生命周期已经进行了 创建对象->属性依赖注入->Aware回调->初始化回调
            //6.BeanPostProcessor的后置方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(beanName,instance);
            }

            /*
             *注意：
             * Aware和初始化回调还是和 BeanPostProcessor的用法上还是有区别
             * ware和初始化回调：是指当前类实现接口，对当前Bean进行自定义设置
             *
             * 后置处理器（BeanPostProcessor）：是可以对所有的Bean进行改造，当然也可以对某一个进行自定义设置
             * 也就是说，每个bean创建都会调用
             */



            //初始化以后就实现 AOP(面向切面编程，通过预编译方式和运行期间动态代理实现程序功能的统一维护)




            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object getBean(String beanName){
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if(beanDefinition == null){
            throw new NullPointerException();
        }else{
            String scope = beanDefinition.getScope();
            //如果是单例bean，直接从单例池拿
            if(scope.equals("singleton")){
                Object o = singletonObjects.get(beanName);
                if(o == null){
                    Object bean = createBean(beanName, beanDefinition);
                    singletonObjects.put(beanName,bean);
                    // return singletonObjects.get(beanName);
                }
                return o;
                //多例
            }else{
                return createBean(beanName, beanDefinition);
            }
        }
    }

}
