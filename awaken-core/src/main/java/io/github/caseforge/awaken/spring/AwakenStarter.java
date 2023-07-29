package io.github.caseforge.awaken.spring;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;

import io.github.caseforge.awaken.Fault;
import io.github.caseforge.awaken.FaultHandler;
import io.github.caseforge.awaken.FaultImpl;
import io.github.caseforge.awaken.Invoker;
import io.github.caseforge.awaken.InvokerBucket;
import io.github.caseforge.awaken.annotation.Provider;
import io.github.caseforge.awaken.annotation.Validator;
import io.github.caseforge.awaken.core.Coder;
import io.github.caseforge.awaken.core.InvokerRegister;

public class AwakenStarter implements InvokerBucket, FaultHandler, BeanDefinitionRegistryPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwakenStarter.class);

    private Coder coder = new Coder();

    private String[] basePackages;

    private BeanDefinitionRegistry registry;

    private Map<String, Method> svcMethodMap = new HashMap<String, Method>();

    private Map<String, Invoker> invokerMap = new HashMap<String, Invoker>();

    private Map<String, Integer> faultCodeMap = new HashMap<String, Integer>();

    private Map<String, String> faultMsgMap = new HashMap<String, String>();

    @Override
    public Invoker findInvoker(String id) {
        return invokerMap.get(id);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
        ItfScanner itfScanner = new ItfScanner(registry, coder);
        itfScanner.scan(getBasePackages());
        svcMethodMap = itfScanner.getSvcMethodMap();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {

            ApplicationContext ctx = event.getApplicationContext();

            SpringResourceProvider resourceProvider = new SpringResourceProvider(ctx);

            registValidatorAndProvider(ctx);

            // 注册invoker
            Set<String> uriSet = svcMethodMap.keySet();

            InvokerRegister invokerRegister = new InvokerRegister();
            invokerRegister.setCoder(coder);
            invokerRegister.setInvokerMap(invokerMap);

            invokerRegister.setResourceProvider(resourceProvider);

            for (String uri : uriSet) {
                Method method = svcMethodMap.get(uri);

                invokerRegister.regist(uri, method);
            }

            // 初始化异常处理
            initFaultHandler(ctx);

        } catch (Exception e) {
            LOGGER.error("the application encountered an error after startup ", e);
            throw new RuntimeException(e);
        }
    }

    protected void registValidatorAndProvider(ApplicationContext ctx) throws Exception {
        String[] beanDefinitionNames = ctx.getBeanDefinitionNames();
        for (String beanName : beanDefinitionNames) {
            Object bean = ctx.getBean(beanName);
            registValidatorAndProvider(beanName, bean);
        }
    }

    protected void registValidatorAndProvider(String beanName, Object bean) throws Exception {
        Class<? extends Object> beanType = bean.getClass();
        // 这里拿到public方法，是能够被调用到的
        Method[] methods = beanType.getMethods();

        for (Method method : methods) {
            Validator validator = method.getAnnotation(Validator.class);
            Provider provider = method.getAnnotation(Provider.class);
            if (validator != null) {
                registValidator(beanName, method, validator);
            }

            if (provider != null) {
                registProvider(beanName, method, provider);
            }
        }
    }

    protected void registValidator(String beanName, Method method, Validator validator) throws Exception {
        if (validator.value().trim().length() < 1) {
            LOGGER.error("the value of annotation cannot be an empty string on method " + method.toGenericString());
            throw new Exception("the value of annotation cannot be an empty string on method " + method.toGenericString());
        }

        if (method.getParameterCount() < 1) {
            LOGGER.error("method " + method.toGenericString() + " must contain at least one parameter");
            throw new Exception("method " + method.toGenericString() + " must contain at least one parameter");
        }

        if (!method.getReturnType().equals(void.class)) {
            LOGGER.error("method " + method.toGenericString() + " must return void type");
            throw new Exception("method " + method.toGenericString() + " must return void type");
        }

        Class<?> validatorType = coder.getValidatorType(method);

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(validatorType);
        builder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        builder.addPropertyReference("refer", beanName);

        this.registry.registerBeanDefinition(validator.value(), builder.getBeanDefinition());
    }

    protected void registProvider(String beanName, Method method, Provider provider) throws Exception {
        if (provider.value().trim().length() < 1) {
            LOGGER.error("the value of annotation cannot be an empty string on method " + method.toGenericString());
            throw new Exception("the value of annotation cannot be an empty string on method " + method.toGenericString());
        }

        if (method.getParameterCount() < 1) {
            LOGGER.error("method " + method.toGenericString() + " must contain at least one parameter");
            throw new Exception("method " + method.toGenericString() + " must contain at least one parameter");
        }

        if (!method.getReturnType().equals(method.getParameters()[0].getType())) {
            LOGGER.error("the return value of the method " + method.toGenericString() + " must be of the same type as the first parameter");
            throw new Exception("the return value of the method " + method.toGenericString() + " must be of the same type as the first parameter");
        }

        Class<?> providerType = coder.getProviderType(method);

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(providerType);
        builder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        builder.addPropertyReference("refer", beanName);

        this.registry.registerBeanDefinition(provider.value(), builder.getBeanDefinition());
    }

    protected void initFaultHandler(ApplicationContext ctx) throws Exception {
        initFaultHandler(Throwable.class.getName(), "1");
        Resource[] resources = ctx.getResources("/exception.properties");
        for (Resource resource : resources) {
            if (resource.exists()) {
                initFaultHandler(resource);
            }
        }
    }

    private void initFaultHandler(Resource resource) throws Exception {
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
            Properties props = new Properties();
            props.load(new InputStreamReader(inputStream, "utf-8"));

            Set<String> exceptionTypeNames = props.stringPropertyNames();

            for (String exceptionTypeName : exceptionTypeNames) {
                String configValue = props.getProperty(exceptionTypeName);
                initFaultHandler(exceptionTypeName, configValue);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void initFaultHandler(String exceptionType, String configValue) throws Exception {
        StringBuilder codeBuilder = new StringBuilder();
        StringBuilder msgBuilder = null;
        StringBuilder currentOpObj = codeBuilder;

        char[] chars = configValue.toCharArray();
        for (char c : chars) {
            if (msgBuilder == null && ',' == c) {
                msgBuilder = new StringBuilder();
                currentOpObj = msgBuilder;
                continue;
            }
            currentOpObj.append(c);
        }

        faultCodeMap.put(exceptionType, Integer.valueOf(codeBuilder.toString()));

        if (msgBuilder != null) {
            faultMsgMap.put(exceptionType, msgBuilder.toString());
        }
    }

    @Override
    public Fault handle(Throwable t) {
        if (t instanceof Fault) {
            return (Fault) t;
        }

        Class<?> type = t.getClass();

        FaultImpl faultImpl = new FaultImpl();
        
        for (;;) {
            String typeName = type.getName();
            Integer code = faultCodeMap.get(typeName);
            if (code != null) {
                String msg = faultMsgMap.get(typeName);
                faultImpl.setCode(code);
                if (msg != null) {
                    faultImpl.setMsg(msg);
                } else {
                    faultImpl.setMsg(t.getMessage());
                }
                break;
            } 
            type = type.getSuperclass();
        }

        return faultImpl;
    }

    public String[] getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }

}
