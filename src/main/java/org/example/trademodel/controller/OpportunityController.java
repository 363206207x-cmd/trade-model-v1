package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.OpportunityStateTransitionDO;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.OpportunityStateTransitionMapper;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.OpportunityPriorityRankingService;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.example.trademodel.vo.OpportunityVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {
    private final AssetStateMapper stateMapper;
    private final OpportunityStateTransitionMapper transitionMapper;
    private final OpportunityPriorityRankingService rankingService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public OpportunityController(AssetStateMapper stateMapper,
                                 OpportunityStateTransitionMapper transitionMapper,
                                 OpportunityPriorityRankingService rankingService,
                                 AuthenticatedUserIdResolver userIdResolver) {
        this.stateMapper = stateMapper;
        this.transitionMapper = transitionMapper;
        this.rankingService = rankingService;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping
    public ApiResponse<List<OpportunityVO>> list(@RequestParam(defaultValue = "100") int limit) {
        Long userId = userIdResolver.requireCurrentUserId();
        int safeLimit = Math.max(1, Math.min(500, limit));
        Map<String, OpportunityVO> rows = new LinkedHashMap<>();
        stateMapper.listOwnedByUser(userId, safeLimit).stream()
                .map(OpportunityVO::from)
                .forEach(row -> rows.putIfAbsent(row.opportunityId(), row));
        stateMapper.listEffectiveSystemForUser(userId, safeLimit).stream()
                .map(OpportunityVO::from)
                .forEach(row -> rows.putIfAbsent(row.opportunityId(), row));
        return ApiResponse.success(rows.values().stream()
                .sorted(Comparator.comparing(OpportunityVO::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList());
    }

    @GetMapping("/top")
    public ApiResponse<List<HomeTopAssetProjection>> top(@RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(rankingService.rankForHome(
                userIdResolver.requireCurrentUserId(), limit));
    }

    @GetMapping("/{opportunityId}")
    public ResponseEntity<ApiResponse<OpportunityVO>> detail(@PathVariable String opportunityId) {
        Long userId = userIdResolver.requireCurrentUserId();
        AssetStateDO row = readableState(opportunityId, userId);
        if (row == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("opportunity not found or not owned"));
        }
        return ResponseEntity.ok(ApiResponse.success(OpportunityVO.from(row)));
    }

    @GetMapping("/{opportunityId}/history")
    public ResponseEntity<ApiResponse<List<OpportunityStateTransitionDO>>> history(
            @PathVariable String opportunityId,
            @RequestParam(defaultValue = "100") int limit) {
        Long userId = userIdResolver.requireCurrentUserId();
        int safeLimit = Math.max(1, Math.min(500, limit));
        if (readableState(opportunityId, userId) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("opportunity not found or not owned"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                transitionMapper.listReadableByUser(opportunityId, userId, safeLimit)));
    }

    private AssetStateDO readableState(String opportunityId, Long userId) {
        if (opportunityId == null || opportunityId.isBlank()) return null;
        return stateMapper.selectReadableByUser(opportunityId.trim(), userId);
    }
}
