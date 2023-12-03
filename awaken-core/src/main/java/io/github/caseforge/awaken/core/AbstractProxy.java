package io.github.caseforge.awaken.core;

import java.lang.reflect.Method;

import io.github.caseforge.awaken.Proxy;
import io.github.caseforge.awaken.ProxyHandler;

public class AbstractProxy implements Proxy {

    private String name;
    
    private ProxyHandler proxyHandler;
    
    @Override
    public Object handle(String name, Method method, Object input, Class<?> responseType) throws Exception {
        return proxyHandler.handle(name, method, input, responseType);
    }

    @Override
    public ProxyHandler getProxyHandler() {
        return proxyHandler;
    }

    @Override
    public void setProxyHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }


}
