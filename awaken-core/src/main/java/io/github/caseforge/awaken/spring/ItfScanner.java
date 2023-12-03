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
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

import io.github.caseforge.awaken.annotation.Fn;
import io.github.caseforge.awaken.annotation.SPI;
import io.github.caseforge.awaken.annotation.Svc;
import io.github.caseforge.awaken.core.Coder;

public class ItfScanner extends ClassPathBeanDefinitionScanner {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

            if (logger.isInfoEnabled()) {
                logger.info("Loading type {} from scan", beanClassName);
            }

            Class<?> beanClass = ClassUtils.forName(beanClassName, ItfScanner.class.getClassLoader());
            SPI spi = beanClass.getAnnotation(SPI.class);

            if (spi != null) {

                if (logger.isInfoEnabled()) {
                    logger.info("Prepare spi {}", beanClassName);
                }

                String proxyHandlerName = spi.value();

                Class<?> proxyType = coder.getProxyType(beanClass);

                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(proxyType);
                builder.addPropertyReference("proxyHandler", proxyHandlerName);
                builder.addPropertyValue("name", definitionHolder.getBeanName());

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
        ClassMetadata classMetadata = metadataReader.getClassMetadata();

        if (!classMetadata.isInterface()) {
            return false;
        }

        boolean hasSvcAnnotation = metadataReader.getAnnotationMetadata().hasAnnotation(Svc.class.getName());

        if (hasSvcAnnotation) {
            try {
                String beanClassName = metadataReader.getClassMetadata().getClassName();
                Class<?> beanClass = ClassUtils.forName(beanClassName, ItfScanner.class.getClassLoader());
                Svc svc = beanClass.getAnnotation(Svc.class);

                if (svc != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Prepare svc {}", beanClassName);
                    }

                    String prefix = svc.value();
                    Method[] methods = beanClass.getMethods();

                    for (Method method : methods) {
                        String suffix = method.getName();
                        Fn fnAnnotation = method.getAnnotation(Fn.class);

                        if (fnAnnotation != null && !"".equals(fnAnnotation.value().trim())) {
                            suffix = fnAnnotation.value();
                        }

                        String uri = buildUri(prefix, suffix);
                        svcMethodMap.put(uri, method);

                        if (logger.isInfoEnabled()) {
                            logger.info("Mapping uri [{}] to method {}", uri, method);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return metadataReader.getAnnotationMetadata().hasAnnotation(SPI.class.getName());
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface();
    }

    public Map<String, Method> getSvcMethodMap() {
        return svcMethodMap;
    }

}
