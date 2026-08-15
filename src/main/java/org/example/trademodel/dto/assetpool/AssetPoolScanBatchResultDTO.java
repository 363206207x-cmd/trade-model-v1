package org.example.trademodel.dto.assetpool;

import java.util.List;

public record AssetPoolScanBatchResultDTO(
        String overallState,
        int successCount,
        int partialCount,
        int failedCount,
        List<AssetPoolScanResultDTO> perAssetResults
) {
    public AssetPoolScanBatchResultDTO {
        perAssetResults = perAssetResults == null ? List.of() : List.copyOf(perAssetResults);
    }

    public static AssetPoolScanBatchResultDTO from(List<AssetPoolScanResultDTO> results) {
        List<AssetPoolScanResultDTO> safe = results == null ? List.of() : List.copyOf(results);
        int success = (int) safe.stream().filter(item -> item != null && "SUCCESS".equals(item.state())).count();
        int partial = (int) safe.stream().filter(item -> item != null && "PARTIAL".equals(item.state())).count();
        int failed = safe.size() - success - partial;
        String overall = safe.isEmpty() ? "EMPTY"
                : failed == 0 && partial == 0 ? "SUCCESS"
                : success == 0 && partial == 0 ? "FAILED" : "PARTIAL";
        return new AssetPoolScanBatchResultDTO(overall, success, partial, failed, safe);
    }
}
