package io.github.caseforge.awaken;

public interface Proxy extends ProxyHandler {

    public ProxyHandler getProxyHandler();

    public void setProxyHandler(ProxyHandler proxyHandler);
    
}
