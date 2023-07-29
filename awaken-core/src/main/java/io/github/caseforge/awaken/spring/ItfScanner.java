package io.github.caseforge.awaken.spring;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.classreading.MetadataReader;

import io.github.caseforge.awaken.annotation.SPI;
import io.github.caseforge.awaken.annotation.Svc;
import io.github.caseforge.awaken.core.Coder;

public class ItfScanner extends ClassPathBeanDefinitionScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItfScanner.class);
    
    private Coder coder;

    private Map<String, Method> svcMethodMap = new HashMap<String, Method>();

    public ItfScanner(BeanDefinitionRegistry registry, Coder coder) {
        super(registry);
        this.coder = coder;
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
        try {
            String beanClassName = definitionHolder.getBeanDefinition().getBeanClassName();
            
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("loading type {} from scan", beanClassName);
            }
            
            Class<?> beanClass = Class.forName(beanClassName);
            Svc svc = beanClass.getAnnotation(Svc.class);
            SPI spi = beanClass.getAnnotation(SPI.class);

            if (svc != null) {
                String prefix = svc.value();
                Method[] methods = beanClass.getMethods();

                for (Method method : methods) {
                    String uri = buildUri(prefix, method.getName());
                    svcMethodMap.put(uri, method);
                    
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("mapping uri [{}] to method {}", uri, method);
                    }
                }
            }

            if (spi != null) {
                String proxyHandlerName = spi.value();
                
                Class<?> proxyType = coder.getProxyType(beanClass);
                
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(proxyType);
                builder.addPropertyReference("proxyHandler", proxyHandlerName);
                
               registry.registerBeanDefinition(definitionHolder.getBeanName(), builder.getBeanDefinition());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String buildUri(String prefix, String suffix) {
        String result = "/" + prefix + "/" + suffix;
        result = result.replaceAll("/+", "/");
        int p = result.length();
        while (result.endsWith("/")) {
            result = result.substring(0, p - 1);
        }
        return result;
    }

    @Override
    protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
        return true;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface();
    }

    public Map<String, Method> getSvcMethodMap() {
        return svcMethodMap;
    }

}
