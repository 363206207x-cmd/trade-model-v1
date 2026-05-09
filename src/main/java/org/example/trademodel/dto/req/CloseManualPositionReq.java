package org.example.trademodel.dto.req;

import java.math.BigDecimal;

public class CloseManualPositionReq {
    private BigDecimal exitPrice;
    private String closeReason;
    private String userActionType;
    private String userRemark;

    public BigDecimal getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(BigDecimal exitPrice) {
        this.exitPrice = exitPrice;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public String getUserActionType() {
        return userActionType;
    }

    public void setUserActionType(String userActionType) {
        this.userActionType = userActionType;
    }

    public String getUserRemark() {
        return userRemark;
    }

    public void setUserRemark(String userRemark) {
        this.userRemark = userRemark;
    }
}
