package io.github.caseforge.awaken.assignment;

import java.util.List;

import io.github.caseforge.awaken.Provider;

public abstract class AbstractNode implements Node {

    protected String name;

    protected List<Node> children;

    protected Provider provider;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

}
