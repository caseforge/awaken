package io.github.caseforge.awaken;

public interface Proxy extends ProxyHandler {

    public String getName();

    public void setName(String name);
    
    public ProxyHandler getProxyHandler();

    public void setProxyHandler(ProxyHandler proxyHandler);
    
}
