package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.RealScanInputContractDTO;

public interface RealScanInputContractGuardValidator {

    RealScanInputContractDTO validate(RealScanInputContractDTO input);
}
