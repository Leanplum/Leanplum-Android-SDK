package com.leanplum.models;

import java.util.Map;

public class ABTest {
    private Double id;
    private Double variantId;
    private Map<String, Object> vars;

    public ABTest() {
    }

    public ABTest(Double id, Double variantId, Map<String, Object> vars) {
        this.id = id;
        this.variantId = variantId;
        this.vars = vars;
    }

    public Double getId() {
        return id;
    }

    public void setId(Double id) {
        this.id = id;
    }

    public Double getVariantId() {
        return id;
    }

    public void setVariantId(Double variantId) {
        this.variantId = variantId;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setVars(Map<String, Object> vars) {
        this.vars = vars;
    }
}
