package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class FrequencyMatrixVersionService {
    private final ProviderCallProperties properties;

    public FrequencyMatrixVersionService(ProviderCallProperties properties) {
        this.properties = properties;
    }

    public String currentVersion() {
        StringBuilder canonical = new StringBuilder("FREQUENCY_MATRIX_V1");
        for (RuntimeScanProfile profile : RuntimeScanProfile.values()) {
            canonical.append('|').append(profile);
            for (ProviderDatasetType dataset : ProviderDatasetType.values()) {
                canonical.append('|').append(dataset);
                for (AssetPriority priority : AssetPriority.values()) {
                    canonical.append(':').append(priority).append('=')
                            .append(properties.intervalSeconds(profile, priority, dataset));
                }
            }
            canonical.append("|analysis=").append(properties.fullAnalysisDebounceSeconds(profile));
        }
        return "FM-" + sha256(canonical.toString()).substring(0, 16).toUpperCase();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
