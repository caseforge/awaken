package io.github.caseforge.awaken;

import java.lang.reflect.Method;

public interface ProxyHandler {

    Object handle(String name, Method method, Object input, Class<?> responseType) throws Exception;
    
}
