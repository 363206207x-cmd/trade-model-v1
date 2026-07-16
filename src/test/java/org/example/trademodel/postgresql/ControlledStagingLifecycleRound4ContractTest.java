package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingLifecycleRound4ContractTest {

    @TempDir
    Path tempDir;

    @Test
    void ruleValueMutationBlocksRecovery() throws Exception {
        assertThat(runner()).contains(
                "cfg-ai-conflict-level3-max",
                "expect_recovery_contract_rejection RULE_VALUE_MUTATION");
        assertThat(ruleDefaults()).contains(
                "rule_id, rule_type, rule_key, rule_value, description, version, enabled",
                "EXCEPT",
                "exact versioned rule-default contract mismatch");
    }

    @Test
    void ruleKeyTypeVersionAndEnabledMutationsBlockRecovery() throws Exception {
        assertThat(runner()).contains(
                "expect_recovery_contract_rejection RULE_KEY_MUTATION",
                "expect_recovery_contract_rejection RULE_TYPE_MUTATION",
                "expect_recovery_contract_rejection RULE_VERSION_MUTATION",
                "expect_recovery_contract_rejection RULE_DISABLED_MUTATION");
    }

    @Test
    void unexpectedAndMissingRuleRowsBlockRecovery() throws Exception {
        assertThat(runner()).contains(
                "expect_recovery_contract_rejection UNEXPECTED_RULE_ROW",
                "expect_recovery_contract_rejection MISSING_RULE_ROW");
    }

    @Test
    void v7RuleValueDriftBlocksSteadyStart() throws Exception {
        assertThat(runner()).contains(
                "cfg-provider-scan-data-quality",
                "cfg-deriv-min-data-quality",
                "expect_full_readonly_verify_rejection V7_PROVIDER_RULE_VALUE",
                "expect_full_readonly_verify_rejection V7_DERIV_RULE_VALUE");
        assertThat(steadyVerify()).contains("postgres-versioned-contract-verify.sh 7 STEADY_STATE");
    }

    @Test
    void versionedRuleDefaultsContainCanonicalRowsRatherThanIdsOnly() throws Exception {
        assertThat(ruleDefaults()).contains(
                "cfg-hot-reset-price-move",
                "hot_reset_config.extreme_price_move_ratio_threshold",
                "cfg-provider-scan-data-quality",
                "provider.scan.data_quality_deterioration_score",
                "cfg-deriv-min-data-quality",
                "derivatives_decision_config.derivatives_min_data_quality_score");
        assertThat(ruleDefaults()).doesNotContain("SELECT rule_id FROM expected_for_version");
    }

    @Test
    void schemaContractCoversCompletePostgresqlCatalogSurface() throws Exception {
        assertThat(schemaContract()).contains(
                "SCHEMA|", "RELATION|", "COLUMN|", "CONSTRAINT|", "INDEX|",
                "SEQUENCE|", "POLICY|", "TRIGGER|", "ROUTINE|", "TYPE|",
                "FOREIGN_TABLE|", "FDW|", "FOREIGN_SERVER|", "EXTENSION|",
                "format_type", "pg_get_constraintdef", "pg_get_indexdef",
                "relrowsecurity", "attidentity", "attgenerated");
    }

    @Test
    void schemaContractHasAuthoritativeV1ThroughV7Fingerprints() throws Exception {
        String verifier = versionedVerifier();
        for (int version = 1; version <= 7; version++) {
            assertThat(verifier).contains(version + ") expected_schema_fingerprint=");
        }
        assertThat(verifier).doesNotContain("PENDING_V");
        assertThat(recoveryVerify()).contains(
                "postgres-versioned-contract-verify.sh \"${applied_version}\" RECOVERY");
        assertThat(versionedVerifier()).contains(
                "STEADY_STATE_SCHEMA_CONTRACT: MATCH_EXACT_V7");
    }

    @Test
    void recoverySchemaDriftFixturesFailClosed() throws Exception {
        assertThat(runner()).contains(
                "expect_recovery_contract_rejection DROP_UNIQUE_INDEX",
                "expect_recovery_contract_rejection DROP_SAFETY_CHECK",
                "expect_recovery_contract_rejection ALTER_COLUMN_TYPE",
                "expect_recovery_contract_rejection ALTER_COLUMN_NULLABILITY",
                "expect_recovery_contract_rejection ALTER_COLUMN_DEFAULT",
                "expect_recovery_contract_rejection EXTRA_COLUMN",
                "expect_recovery_contract_rejection ROW_LEVEL_SECURITY",
                "expect_recovery_contract_rejection MISSING_COLUMN");
    }

    @Test
    void steadyStateSchemaDriftFixturesFailClosed() throws Exception {
        assertThat(runner()).contains(
                "expect_full_readonly_verify_rejection V7_MISSING_INDEX",
                "tm_decision_result ALTER COLUMN valid_from",
                "expect_full_readonly_verify_rejection V7_OFFSET_COLUMN_TYPE",
                "expect_full_readonly_verify_rejection V7_ROW_LEVEL_SECURITY");
    }

    @Test
    void publicAndColumnPrivilegesAreNormalized() throws Exception {
        assertThat(grants()).contains(
                "REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC",
                "REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM PUBLIC",
                "information_schema.column_privileges",
                "INSERT', 'UPDATE', 'REFERENCES",
                "REVOKE ALL ON TABLES FROM PUBLIC",
                "REVOKE ALL ON SEQUENCES FROM PUBLIC");
    }

    @Test
    void effectivePrivilegesAreCheckedInsteadOfDirectRowsOnly() throws Exception {
        assertThat(steadyVerify()).contains(
                "has_table_privilege(checked_role.role_name, c.oid, 'SELECT')",
                "has_table_privilege(checked_role.role_name, c.oid, 'INSERT')",
                "has_table_privilege(checked_role.role_name, c.oid, 'UPDATE')",
                "acl.grantee = 0",
                "READONLY_EFFECTIVE_TABLE_PRIVILEGES: PASS",
                "READONLY_COLUMN_PRIVILEGES: PASS",
                "PUBLIC_WRITE_PRIVILEGES: NONE");
    }

    @Test
    void publicAndColumnPrivilegeFixturesFailClosed() throws Exception {
        assertThat(runner()).contains(
                "expect_full_readonly_verify_rejection PUBLIC_EFFECTIVE_UPDATE",
                "expect_full_readonly_verify_rejection PUBLIC_INSERT",
                "expect_full_readonly_verify_rejection APP_COLUMN_UPDATE",
                "expect_full_readonly_verify_rejection BACKUP_COLUMN_UPDATE",
                "expect_full_readonly_verify_rejection PUBLIC_SEQUENCE_USAGE");
    }

    @Test
    void writeProbeDisablesSessionReadOnlyBeforePermissionProbe() throws Exception {
        assertThat(P3hContractTestSupport.read("deploy/p3h/p3h-app-readonly-probe.sh"))
                .contains("SET default_transaction_read_only=off; UPDATE flyway_schema_history")
                .contains("P3H_APP_ROLE_PROBE: BLOCKED_WRITE_ALLOWED");
    }

    @Test
    void hostnameWithSemicolonFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_STAGING_HOSTNAME", "stage.example.invalid;load_module");
    }

    @Test
    void hostnameWithNewlineFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_STAGING_HOSTNAME",
                "stage.example.invalid\nserver_name injected.invalid");
    }

    @Test
    void hostnameWithNginxDirectiveFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_STAGING_HOSTNAME",
                "stage.example.invalid{include=/tmp/x;}");
    }

    @Test
    void hostnameWithLeadingDashFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_STAGING_HOSTNAME", "-stage.example.invalid");
    }

    @Test
    void invalidDnsLabelFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_STAGING_HOSTNAME", "stage.-invalid.example");
    }

    @Test
    void sshHostOptionInjectionFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_SSH_HOST", "-oProxyCommand=invalid");
    }

    @Test
    void sshHostWithUserInfoFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_SSH_HOST", "operator@stage.example.invalid");
    }

    @Test
    void sshUserWithAtSignFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_SSH_USER", "p3h@deploy");
    }

    @Test
    void sshUserWithWhitespaceFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_SSH_USER", "p3h deploy");
    }

    @Test
    void sshUserOptionInjectionFailsBeforeNetwork() throws Exception {
        assertRejectedBeforeNetwork("P3H_SSH_USER", "-oProxyCommand");
    }

    @Test
    void canonicalStagingDnsPasses() throws Exception {
        ScriptOutput output = runInputContract("STAGING_HOSTNAME", "stage.example.invalid");
        assertThat(output.exitCode()).isZero();
        assertThat(output.output()).contains("STAGING_HOSTNAME_CONTRACT: PASS_STRICT_DNS");
    }

    @Test
    void canonicalSshHostAndUserPass() throws Exception {
        assertThat(runInputContract("SSH_HOST", "stage.example.invalid").exitCode()).isZero();
        assertThat(runInputContract("SSH_HOST", "192.0.2.10").exitCode()).isZero();
        ScriptOutput user = runInputContract("SSH_USER", "p3h-deploy");
        assertThat(user.exitCode()).isZero();
        assertThat(user.output()).contains("SSH_USER_CONTRACT: PASS_STRICT");
    }

    @Test
    void inputValidationRunsAfterAttestationReadAndBeforeNetwork() throws Exception {
        String deployment = deploymentRunner();
        assertThat(deployment.indexOf("if ! attestation_is_strict"))
                .isLessThan(deployment.indexOf("p3h_validate_staging_hostname"));
        assertThat(deployment.indexOf("p3h_validate_staging_hostname"))
                .isLessThan(deployment.indexOf("NETWORK_ACCESS_STARTED=1"));
        assertThat(deployment.indexOf("p3h_validate_ssh_user"))
                .isLessThan(deployment.indexOf("ssh-keyscan -T 10"));
    }

    @Test
    void canonicalHostnameIsRenderedAndCheckedByNginx() throws Exception {
        assertThat(runner()).contains(
                "P3H_STAGING_HOSTNAME=stage.example.invalid proxy nginx -t",
                "BLOCKED_CANONICAL_STAGING_NGINX_RENDER");
    }

    private void assertRejectedBeforeNetwork(String key, String value) throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.put(key, value);

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains(
                "SERVER_ACCESS: NOT_ATTEMPTED",
                "SECRET_ACCESS: NOT_ATTEMPTED",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
        assertThat(result.output()).doesNotContain(value);
    }

    private ScriptOutput runInputContract(String type, String value) throws Exception {
        Process process = new ProcessBuilder(
                "bash", "scripts/p3h-controlled-input-contract.sh", type, value)
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptOutput(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private String runner() throws Exception {
        return P3hContractTestSupport.read("scripts/controlled-p3h-compose-offline-smoke.sh");
    }

    private String deploymentRunner() throws Exception {
        return P3hContractTestSupport.read("scripts/controlled-staging-readonly-deployment-p3h.sh");
    }

    private String recoveryVerify() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-greenfield-recovery-verify.sh");
    }

    private String steadyVerify() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-steady-state-verify.sh");
    }

    private String grants() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-readonly-grants.sql");
    }

    private String ruleDefaults() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-rule-defaults-verify.sql");
    }

    private String schemaContract() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-schema-contract.sql");
    }

    private String versionedVerifier() throws Exception {
        return P3hContractTestSupport.read("deploy/p3h/postgres-versioned-contract-verify.sh");
    }

    private record ScriptOutput(int exitCode, String output) {
    }
}
