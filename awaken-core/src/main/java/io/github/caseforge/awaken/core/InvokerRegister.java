package io.github.caseforge.awaken.core;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.caseforge.awaken.Invoker;
import io.github.caseforge.awaken.ResourceProvider;
import io.github.caseforge.awaken.assignment.Assignment;
import io.github.caseforge.awaken.assignment.AssignmentBuilder;
import io.github.caseforge.awaken.validation.Validation;
import io.github.caseforge.awaken.validation.ValidationBuilder;

public class InvokerRegister {

    protected static final Logger LOGGER = LoggerFactory.getLogger(InvokerRegister.class);
    
    private Map<String, Invoker> invokerMap;

    private ResourceProvider resourceProvider;

    private Coder coder;

    private Gson gson = new Gson();

    public InvokerRegister() {
        
    }
    
    public InvokerRegister(Coder coder, ResourceProvider resourceProvider, Map<String, Invoker> invokerMap) {
        super();
        this.coder = coder;
        this.resourceProvider = resourceProvider;
        this.invokerMap = invokerMap;
    }

    public void regist(String uri, Method method) throws Exception {
        JsonObject publishConfig = loadPublishConfig(uri);
        
        if (publishConfig == null) {
            LOGGER.info("No config found for [{}]", uri);
            return;
        }
        
        Set<String> rules = publishConfig.keySet();
        Class<?> invokerType = coder.getInvokerType(method);
        
        for (String rule : rules) {
            JsonObject ruleJsonObject = publishConfig.get(rule).getAsJsonObject();
            String beanName = ruleJsonObject.get("beanName").getAsString();
            Object target = resourceProvider.getBean(beanName);
            if (target == null) {
                throw new Exception("Error in file /rules" + uri + ".json no bean found for name " + beanName);
            }
            AbstractInvoker invoker = (AbstractInvoker) invokerType.getDeclaredConstructor().newInstance();
            invoker.setTarget(target);
            Invoker wrapInvoker = wrap(invoker, ruleJsonObject);
            invokerMap.put(uri + "@" + rule, wrapInvoker);
            LOGGER.info("Publish [{}] by rule [{}]", uri, rule);
        }
        
    }
    
    private Invoker wrap(Invoker invoker, JsonObject jsonObject) throws Exception {
        VerifyableInvoker verifyableInvoker = new VerifyableInvoker();
        verifyableInvoker.setDelegate(invoker);
        
        JsonElement assignJsonElement = jsonObject.get("assignment");
        if (assignJsonElement != null && !assignJsonElement.isJsonNull()) {
            JsonObject assignmentJsonObject = assignJsonElement.getAsJsonObject();
            Assignment assignment = AssignmentBuilder.build(assignmentJsonObject, resourceProvider);
            verifyableInvoker.setAssignment(assignment);
        }
        
        JsonElement validateJsonElement = jsonObject.get("validation");
        if (validateJsonElement != null && !validateJsonElement.isJsonNull()) {
            JsonObject validateJsonObject = validateJsonElement.getAsJsonObject();
            Validation validation = ValidationBuilder.build(validateJsonObject, resourceProvider);
            verifyableInvoker.setValidation(validation);
        }
        
        JsonElement maskJsonElement = jsonObject.get("mask");
        if (maskJsonElement != null && !maskJsonElement.isJsonNull()) {
            JsonObject maskJsonObject = maskJsonElement.getAsJsonObject();
            Assignment mask = AssignmentBuilder.build(maskJsonObject, resourceProvider);
            verifyableInvoker.setMask(mask);
        }
        
        return verifyableInvoker;
    }

    private JsonObject loadPublishConfig(String uri) throws Exception {
        byte[] resource = resourceProvider.getResource("/rules" + uri + ".json");
        if (resource == null) {
            return null;
        }
        return gson.fromJson(new String(resource, "utf-8"), JsonObject.class);
    }

    public Map<String, Invoker> getInvokerMap() {
        return invokerMap;
    }

    public void setInvokerMap(Map<String, Invoker> invokerMap) {
        this.invokerMap = invokerMap;
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public Coder getCoder() {
        return coder;
    }

    public void setCoder(Coder coder) {
        this.coder = coder;
    }

}
