package org.example.trademodel.service;

import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.vo.UserPositionVO;

import java.util.List;

public interface UserPositionService {
    UserPositionVO manualOpenForUser(Long userId, CreateUserPositionReq request);

    UserPositionVO manualCloseForUser(Long id, Long userId, CloseUserPositionReq request);

    List<UserPositionVO> listOpenPositionsForUser(Long userId);

    List<UserPositionVO> listClosedPositionsForUser(Long userId, int limit);

    int countClosedPositionsForUser(Long userId);

    UserPositionVO findByIdForUser(Long id, Long userId);
}
