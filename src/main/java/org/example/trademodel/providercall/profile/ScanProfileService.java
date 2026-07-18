package org.example.trademodel.providercall.profile;

public interface ScanProfileService {
    ScanProfileResponse get(String userId);
    ScanProfileResponse update(String userId, ScanProfileUpdateRequest request);

    default ScanProfileResponse update(String userId, String actor, ScanProfileUpdateRequest request) {
        return update(userId, request);
    }
}
