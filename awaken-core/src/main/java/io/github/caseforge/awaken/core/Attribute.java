package io.github.caseforge.awaken.core;

public class Attribute {

    private String name;

    private Class<?> type;

    private String descriptor;

    private String signature;

    public Attribute() {

    }

    public Attribute(String name, Class<?> type, String descriptor, String signature) {
        super();
        this.name = name;
        this.type = type;
        this.descriptor = descriptor;
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

}
