package io.github.caseforge.awaken;

public interface ResourceProvider {

    Object getBean(String name) throws Exception;
    
    byte[] getResource(String uri) throws Exception;
}
