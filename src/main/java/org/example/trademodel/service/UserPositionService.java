package org.example.trademodel.service;

import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.vo.UserPositionVO;

import java.util.List;

public interface UserPositionService {
    UserPositionVO manualOpen(CreateUserPositionReq request);

    UserPositionVO manualClose(Long id, CloseUserPositionReq request);

    List<UserPositionVO> listOpenPositions();

    UserPositionVO findById(Long id);
}
