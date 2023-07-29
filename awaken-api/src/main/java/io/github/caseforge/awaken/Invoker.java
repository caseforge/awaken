package io.github.caseforge.awaken;

public interface Invoker {

    Class<?> getRequestType();
    
    Class<?> getResponseType();
    
    void setTarget(Object target);
    
    Object invoke(Object input) throws Exception;
    
}
