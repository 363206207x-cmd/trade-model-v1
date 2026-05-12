package org.example.trademodel.vo;

import java.util.ArrayList;
import java.util.List;

public class PushWatchlistConfigVO {
    private String ruleKey;
    private List<String> symbols = new ArrayList<>();
    private Boolean enabled;
    private String ruleValue;

    public String getRuleKey() {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }
}
