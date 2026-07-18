package org.example.trademodel.dashboard;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderCallProfileDashboardContractTest {
    private static final Path DASHBOARD = Path.of("src/main/resources/templates/dashboard.html");

    @Test
    void dashboardShowsBaseAndEffectiveProfilesWithChineseLabels() throws Exception {
        String html = Files.readString(DASHBOARD);
        assertThat(html).contains(
                "调用基础档位",
                "当前实际档位",
                "升档原因",
                "持仓价格刷新",
                "关注资产刷新",
                "候选资产刷新",
                "发现池扫描",
                "Provider 总体状态",
                "<option value=\"AUTO\">自动</option>",
                "<option value=\"LOW\">低频</option>",
                "<option value=\"STANDARD\">标准</option>",
                "<option value=\"HIGH\">高频</option>")
                .doesNotContain("<option value=\"EMERGENCY\"");
    }

    @Test
    void dashboardLoadsStatusWithoutTriggeringProviderRefresh() throws Exception {
        String html = Files.readString(DASHBOARD);
        assertThat(html).contains(
                "fetch(\"/api/provider-call/runtime-status\")",
                "fetch(\"/api/provider-call/base-profile\"",
                "method: \"PUT\"")
                .doesNotContain("/api/provider-call/refresh", "ProviderSnapshotRefreshService");
    }

    @Test
    void failedSaveRestoresPriorSelectionAndUsesChineseFeedback() throws Exception {
        String html = Files.readString(DASHBOARD);
        assertThat(html).contains(
                "var previous = providerProfileState.savedProfile;",
                "setText(\"providerProfileMessage\", \"正在保存\")",
                "setText(\"providerProfileMessage\", \"档位已生效\")",
                "select.value = previous;",
                "档位保存失败");
    }

    @Test
    void profileControlHasMobileLayoutContract() throws Exception {
        String html = Files.readString(DASHBOARD);
        assertThat(html).contains(
                "@media (max-width: 720px)",
                ".provider-profile-control",
                "grid-template-columns: 1fr",
                "id=\"providerProfileSaveButton\"");
    }
}
