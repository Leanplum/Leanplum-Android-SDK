package com.leanplum.models;

import java.util.HashMap;
import java.util.Map;

public class ABTest {
    private Long id;
    private Long variantId;
    private Map<String, Object> vars;

    public ABTest() {
    }

    public ABTest(Long id, Long variantId, Map<String, Object> vars) {
        this.id = id;
        this.variantId = variantId;
        this.vars = vars;
    }

    //todo: move to framework
    public ABTest(Map<String, Object> dict) {
        Map<String, Object> vars = (Map<String, Object>) dict.get("vars");
        this.id = (Long) dict.get("id");
        this.variantId = (Long) dict.get("variantId");
        this.vars = vars;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVariantId() {
        return id;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setVars(Map<String, Object> vars) {
        this.vars = vars;
    }

    //todo: move to framework
    public Map<String, Object> asDictionary() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", id);
        dict.put("variantId", variantId);
        dict.put("vars", vars);
        return dict;
    }

}
