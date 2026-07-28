package org.example.trademodel.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.example.trademodel.dto.UserPositionResponseDTO;

public class UserPositionVO extends UserPositionResponseDTO {
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }
}
