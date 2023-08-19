package io.github.caseforge.awaken.spring;

import java.io.ByteArrayOutputStream;
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
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;

import io.github.caseforge.awaken.Fault;
import io.github.caseforge.awaken.FaultHandler;
import io.github.caseforge.awaken.FaultImpl;
import io.github.caseforge.awaken.Invoker;
import io.github.caseforge.awaken.InvokerBucket;
import io.github.caseforge.awaken.ResourceProvider;
import io.github.caseforge.awaken.annotation.Provider;
import io.github.caseforge.awaken.annotation.Validator;
import io.github.caseforge.awaken.core.Coder;
import io.github.caseforge.awaken.core.InvokerRegister;

/**
 * 框架入口
 */
public abstract class AbstractAwakenStarter implements InvokerBucket, FaultHandler, ResourceProvider, ApplicationContextAware, BeanDefinitionRegistryPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractAwakenStarter.class);
    
    private static final String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

    protected Coder coder = new Coder();
    
    protected ApplicationContext applicationContext;

    protected BeanDefinitionRegistry registry;

    protected Map<String, Method> svcMethodMap = new HashMap<String, Method>();

    protected Map<String, Invoker> invokerMap = new HashMap<String, Invoker>();

    protected Map<String, Integer> faultCodeMap = new HashMap<String, Integer>();

    protected Map<String, String> faultMsgMap = new HashMap<String, String>();

    @Override
    public Invoker findInvoker(String id) {
        return invokerMap.get(id);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        LOGGER.info("Starting awaken framework ");
        this.registry = registry;
        ItfScanner itfScanner = new ItfScanner(registry, coder);
        itfScanner.scan(getBasePackages());
        svcMethodMap = itfScanner.getSvcMethodMap();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {

            registStaticValidator(applicationContext);
            
            registValidatorAndProvider(applicationContext);

            // 注册invoker
            Set<String> uriSet = svcMethodMap.keySet();

            InvokerRegister invokerRegister = new InvokerRegister(coder, this, invokerMap);

            for (String uri : uriSet) {
                Method method = svcMethodMap.get(uri);
                invokerRegister.regist(uri, method);
            }

            // 初始化异常处理
            initFaultHandler(applicationContext);

        } catch (Exception e) {
            LOGGER.error("The application encountered an error after startup ", e);
            throw new RuntimeException(e);
        }
    }

    protected void registStaticValidator(ApplicationContext ctx) throws Exception {
        Resource[] resources = ctx.getResources(CLASSPATH_ALL_URL_PREFIX + "validators.properties");
        for (Resource resource : resources) {
            if (resource.exists()) {
                registStaticValidator(resource);
            }
        }
    }
    
    protected void registStaticValidator(Resource resource) throws Exception {
        InputStream inputStream = null;
        try {
            LOGGER.info("Loading validator declare from {}", resource.getURI());
            inputStream = resource.getInputStream();
            Properties props = new Properties();
            props.load(new InputStreamReader(inputStream, "utf-8"));

            Set<String> validatorNames = props.stringPropertyNames();

            for (String validatorName : validatorNames) {
                LOGGER.info("Regist validator {}", validatorName);
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(props.getProperty(validatorName));
                builder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
                this.registry.registerBeanDefinition(validatorName, builder.getBeanDefinition());
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
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
            LOGGER.error("The value of annotation cannot be an empty string on method {}", method.toGenericString());
            throw new Exception("The value of annotation cannot be an empty string on method " + method.toGenericString());
        }

        if (method.getParameterCount() < 1) {
            LOGGER.error("Method {} must contain at least one parameter", method.toGenericString());
            throw new Exception("Method " + method.toGenericString() + " must contain at least one parameter");
        }

        if (!method.getReturnType().equals(void.class)) {
            LOGGER.error("Method {} must return void type", method.toGenericString());
            throw new Exception("Method " + method.toGenericString() + " must return void type");
        }

        Class<?> validatorType = coder.getValidatorType(method);

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(validatorType);
        builder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        builder.addPropertyReference("refer", beanName);

        String validatorName = validator.value();
        this.registry.registerBeanDefinition(validatorName, builder.getBeanDefinition());
        
        LOGGER.info("Regist validator {}", validatorName);
    }

    protected void registProvider(String beanName, Method method, Provider provider) throws Exception {
        if (provider.value().trim().length() < 1) {
            LOGGER.error("The value of annotation cannot be an empty string on method {}", method.toGenericString());
            throw new Exception("The value of annotation cannot be an empty string on method " + method.toGenericString());
        }

        if (method.getParameterCount() < 1) {
            LOGGER.error("Method {} must contain at least one parameter", method.toGenericString());
            throw new Exception("Method " + method.toGenericString() + " must contain at least one parameter");
        }

        if (!method.getReturnType().equals(method.getParameters()[0].getType())) {
            LOGGER.error("The return value of the method {} must be of the same type as the first parameter", method.toGenericString());
            throw new Exception("The return value of the method " + method.toGenericString() + " must be of the same type as the first parameter");
        }

        Class<?> providerType = coder.getProviderType(method);

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(providerType);
        builder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        builder.addPropertyReference("refer", beanName);

        String providerName = provider.value();
        this.registry.registerBeanDefinition(providerName, builder.getBeanDefinition());
        
        LOGGER.info("Regist provider {}", providerName);
    }

    protected void initFaultHandler(ApplicationContext ctx) throws Exception {
        initFaultHandler(Throwable.class.getName(), "1");
        Resource[] resources = ctx.getResources(CLASSPATH_ALL_URL_PREFIX + "exception.properties");
        for (Resource resource : resources) {
            if (resource.exists()) {
                initFaultHandler(resource);
            }
        }
    }

    protected void initFaultHandler(Resource resource) throws Exception {
        InputStream inputStream = null;
        try {
            LOGGER.info("Loading exception declare from {}", resource.getURI());
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

    @Override
    public Object getBean(String name) throws Exception {
        return this.applicationContext.getBean(name);
    }

    @Override
    public byte[] getResource(String uri) throws Exception {
        Resource[] resources = applicationContext.getResources(CLASSPATH_ALL_URL_PREFIX + uri);
        
        if (resources == null || resources.length < 1) {
            return null;
        }
        if (!resources[0].exists()) {
            return null;
        }

        InputStream inputStream = resources[0].getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] bs = new byte[512];
            int len = 0;
            while ((len = inputStream.read(bs)) > -1) {
                baos.write(bs, 0, len);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return baos.toByteArray();
    }

    public abstract String[] getBasePackages();

}
