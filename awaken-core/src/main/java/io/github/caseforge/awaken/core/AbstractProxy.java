package io.github.caseforge.awaken.core;

import java.lang.reflect.Method;

import io.github.caseforge.awaken.Proxy;
import io.github.caseforge.awaken.ProxyHandler;

public class AbstractProxy implements Proxy {

    private ProxyHandler proxyHandler;
    
    @Override
    public Object handle(Method method, Object input, Class<?> responseType) throws Exception {
        return proxyHandler.handle(method, input, responseType);
    }

    @Override
    public ProxyHandler getProxyHandler() {
        return proxyHandler;
    }

    @Override
    public void setProxyHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

}
