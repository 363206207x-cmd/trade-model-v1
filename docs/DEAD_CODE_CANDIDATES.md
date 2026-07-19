# Dead Code Candidates

This file records deletion candidates and explicit framework-referenced exclusions that must be kept.

No deletion is allowed unless all conditions are true:

1. Risk is LOW.
2. Recommendation is DELETE.
3. There is no production reference.
4. There is no test reference.
5. The file is not required by future phases in docs/PROJECT_DELIVERY_CONTRACT.md.
6. Maven tests pass after deletion.

No deletion is allowed in the contract-lock task.

| File | Candidate Type | Production Reference | Test Reference | Schema Support | Needed By Contract Future Phase | Risk | Recommendation | Reason |
|---|---|---|---|---|---|---|---|---|
| src/main/java/org/example/trademodel/dto/point/ReviewOnlyPointProposalDisplayDTO.java | review-only / placeholder / point-domain candidate | Unknown without dependency trace | Unknown without dependency trace | None found in scan | Possible P0-2 source-gate reference or frozen point-domain reference | UNKNOWN | DEFER | Scan found review-only placeholder semantics; deletion risk cannot be proven LOW. |
| src/main/java/org/example/trademodel/assembler/point/ReviewOnlyNumericPointProposalAssembler.java | review-only / point-domain candidate | Unknown without dependency trace | Test file exists | None found in scan | Possible P0-2 source-gate reference or frozen point-domain reference | UNKNOWN | DEFER | Candidate/Point files are frozen, not automatically dead. |
| src/main/java/org/example/trademodel/validator/point/NumericPointSafetyValidator.java | point-domain validator candidate | Unknown without dependency trace | Test file exists | None found in scan | Possible P0-2 source-gate reference | UNKNOWN | DEFER | Could be reused by ExecutionPlan Source Gate; cannot delete in P0-0. |
| src/main/java/org/example/trademodel/service/watchlistscan/NoOpOpportunityPushExternalChannelPolicy.java | no-op external channel policy candidate | Production safety reference possible | Test file exists | None found in scan | P1-1 PushRecheck semantic hardening may need no-external-channel evidence | UNKNOWN | DEFER | No-op policy is safety evidence, not proven dead. |
| src/test/java/org/example/trademodel/service/watchlistscan/MarketReadRequestTestOnlyWiring.java | test-only wiring candidate | Test only | Test reference exists | None | Could support existing tests | UNKNOWN | DEFER | Test-only helper cannot be deleted without targeted dependency and test proof. |


---

## P0-0 Reconciliation Scan Note (Historical)

This task does not delete files and does not mark any candidate DELETE.

The scan found many `review-only`, `placeholder`, `duplicate`, `no-op`, `Preview`, `Mock`, `Candidate`, `Point`, and `Signal` references, but none are classified as LOW-risk DELETE in this governance draft because future-phase ownership and test references still need explicit migration evidence.

Recommendation for all ambiguous candidates in this draft: DEFER.

This note describes the original P0-0 scan only. The independently verified
P3-CLEAN2A-1B evidence below supersedes the original zero-count conclusion only
for the eight exact candidates listed there. All unrelated historical candidates
remain `DEFER`.

---

## P3-CLEAN2A-1B E3 Evidence Gate (PR #1132)

This section is the formal Rule 17 evidence record for the exact eight-file
deletion batch proposed by PR #1132. It does not expand the delete scope and it
does not claim merged-main effectivity.

| Status field | Value |
|---|---|
| EVIDENCE_STATUS | `PR_BRANCH_REVIEW_PENDING` |
| DELETION_STATUS | `PROPOSED_IN_PR_1132` |
| EFFECTIVE_STATUS | `NOT_EFFECTIVE_UNTIL_MERGED_MAIN` |
| EVIDENCE_LEVEL | `E3` |
| REQUIRED_RISK_FOR_DELETE | `LOW` |
| REQUIRED_ACTION_FOR_DELETE | `DELETE` |
| LOW_DELETE_CANDIDATE_COUNT | `8` |
| FRAMEWORK_KEEP_COUNT | `1` |
| DELETE_BATCH_LIMIT | `8_OF_10` |
| CAPABILITY_CHANGE | `NONE` |
| BEHAVIOR_CHANGE | `NONE_EXPECTED` |

Evidence methods used for every delete candidate:

- exact fully-qualified-name and simple-type repository searches over production,
  tests, resources, scripts, and documentation;
- import, class-literal, reflection, serialization, schema, configuration,
  template, architecture-test, and build-tool role inspection;
- introduction-history inspection with `git log --diff-filter=A`;
- post-delete compile, targeted test, full Maven test, and CI gates required
  before the branch can be accepted.

The seven marker classes and `PushTypeEnum` were all introduced by commit
`4a185172f86ee5e96f637bb63547fc4d5e4757ba`, whose metadata identifies a
Cursor-era bulk-generated change. None is named as a required asset by
`docs/PROJECT_DELIVERY_CONTRACT.md`.

### Current decision summary

| Candidate ID | File | Candidate Type | Production Reference | Test Reference | Schema Support | Needed By Contract Future Phase | Risk | Recommendation | Reason |
|---|---|---|---:|---:|---|---|---|---|---|
| CLEAN-001A | `src/main/java/org/example/trademodel/config/PackageInfo.java` | generated marker placeholder | 0 | 0 | None | No | LOW | DELETE | Empty non-annotated marker with no direct or dynamic role. |
| CLEAN-001B | `src/main/java/org/example/trademodel/converter/PackageInfo.java` | generated marker placeholder | 0 | 0 | None | No | LOW | DELETE | Empty non-annotated marker; deleting it leaves no directory contract. |
| CLEAN-001C | `src/main/java/org/example/trademodel/dto/PackageInfo.java` | generated marker placeholder | 0 | 0 | None | No | LOW | DELETE | Empty marker with no DTO, serialization, or API role. |
| CLEAN-001D | `src/main/java/org/example/trademodel/dto/req/PackageInfo.java` | generated marker placeholder | 0 | 0 | None | No | LOW | DELETE | Empty marker with no request, validation, or serialization role. |
| CLEAN-001E | `src/main/java/org/example/trademodel/enums/PackageInfo.java` | generated marker placeholder | 0 | 0 | None | No | LOW | DELETE | Empty non-enum marker with no enum or configuration role. |
| CLEAN-001F | `src/main/java/org/example/trademodel/mapper/PackageInfo.java` | generated marker placeholder | 0 | 0 | None | No | LOW | DELETE | Concrete non-annotated marker with no MyBatis mapper or alias role. |
| CLEAN-001G | `src/main/java/org/example/trademodel/validator/PackageInfo.java` | generated marker placeholder | 0 | 0 | None | No | LOW | DELETE | Empty non-annotated marker; deleting it leaves no directory contract. |
| CLEAN-002 | `src/main/java/org/example/trademodel/enums/PushTypeEnum.java` | unused enum | 0 type references | 0 type references | None | No | LOW | DELETE | Enum type is unused; same-name strings are independent string contracts. |

### CLEAN-001A

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001A` |
| PATH | `src/main/java/org/example/trademodel/config/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.config.PackageInfo` |
| CATEGORY | `GENERATED_MARKER_PLACEHOLDER` |
| DIRECT_PRODUCTION_REFERENCES | `0` |
| DIRECT_TEST_REFERENCES | `0` |
| DYNAMIC_REFERENCES | `0` |
| SPRING_FRAMEWORK_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| TEMPLATE_ROLE | `NONE` |
| REFLECTION_ROLE | `NONE` |
| SERIALIZATION_ROLE | `NONE` |
| ARCHITECTURE_TEST_ROLE | `NONE` |
| BUILD_TOOL_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

### CLEAN-001B

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001B` |
| PATH | `src/main/java/org/example/trademodel/converter/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.converter.PackageInfo` |
| CATEGORY | `GENERATED_MARKER_PLACEHOLDER` |
| DIRECT_PRODUCTION_REFERENCES | `0` |
| DIRECT_TEST_REFERENCES | `0` |
| DYNAMIC_REFERENCES | `0` |
| SPRING_FRAMEWORK_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| TEMPLATE_ROLE | `NONE` |
| REFLECTION_ROLE | `NONE` |
| SERIALIZATION_ROLE | `NONE` |
| ARCHITECTURE_TEST_ROLE | `NONE` |
| BUILD_TOOL_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| DIRECTORY_AFTER_DELETE | `EMPTY` |
| EMPTY_DIRECTORY_CONTRACT | `NONE` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

### CLEAN-001C

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001C` |
| PATH | `src/main/java/org/example/trademodel/dto/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.dto.PackageInfo` |
| CATEGORY | `GENERATED_MARKER_PLACEHOLDER` |
| DIRECT_PRODUCTION_REFERENCES | `0` |
| DIRECT_TEST_REFERENCES | `0` |
| DYNAMIC_REFERENCES | `0` |
| SPRING_FRAMEWORK_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| TEMPLATE_ROLE | `NONE` |
| REFLECTION_ROLE | `NONE` |
| SERIALIZATION_ROLE | `NONE` |
| ARCHITECTURE_TEST_ROLE | `NONE` |
| BUILD_TOOL_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

### CLEAN-001D

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001D` |
| PATH | `src/main/java/org/example/trademodel/dto/req/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.dto.req.PackageInfo` |
| CATEGORY | `GENERATED_MARKER_PLACEHOLDER` |
| DIRECT_PRODUCTION_REFERENCES | `0` |
| DIRECT_TEST_REFERENCES | `0` |
| DYNAMIC_REFERENCES | `0` |
| SPRING_FRAMEWORK_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| TEMPLATE_ROLE | `NONE` |
| REFLECTION_ROLE | `NONE` |
| SERIALIZATION_ROLE | `NONE` |
| ARCHITECTURE_TEST_ROLE | `NONE` |
| BUILD_TOOL_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

### CLEAN-001E

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001E` |
| PATH | `src/main/java/org/example/trademodel/enums/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.enums.PackageInfo` |
| CATEGORY | `GENERATED_MARKER_PLACEHOLDER` |
| DIRECT_PRODUCTION_REFERENCES | `0` |
| DIRECT_TEST_REFERENCES | `0` |
| DYNAMIC_REFERENCES | `0` |
| SPRING_FRAMEWORK_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| TEMPLATE_ROLE | `NONE` |
| REFLECTION_ROLE | `NONE` |
| SERIALIZATION_ROLE | `NONE` |
| ARCHITECTURE_TEST_ROLE | `NONE` |
| BUILD_TOOL_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

### CLEAN-001F

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001F` |
| PATH | `src/main/java/org/example/trademodel/mapper/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.mapper.PackageInfo` |
| CATEGORY | `GENERATED_MARKER_PLACEHOLDER` |
| DIRECT_PRODUCTION_REFERENCES | `0` |
| DIRECT_TEST_REFERENCES | `0` |
| DYNAMIC_REFERENCES | `0` |
| SPRING_FRAMEWORK_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| TEMPLATE_ROLE | `NONE` |
| REFLECTION_ROLE | `NONE` |
| SERIALIZATION_ROLE | `NONE` |
| ARCHITECTURE_TEST_ROLE | `NONE` |
| BUILD_TOOL_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

### CLEAN-001G

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001G` |
| PATH | `src/main/java/org/example/trademodel/validator/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.validator.PackageInfo` |
| CATEGORY | `GENERATED_MARKER_PLACEHOLDER` |
| DIRECT_PRODUCTION_REFERENCES | `0` |
| DIRECT_TEST_REFERENCES | `0` |
| DYNAMIC_REFERENCES | `0` |
| SPRING_FRAMEWORK_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| TEMPLATE_ROLE | `NONE` |
| REFLECTION_ROLE | `NONE` |
| SERIALIZATION_ROLE | `NONE` |
| ARCHITECTURE_TEST_ROLE | `NONE` |
| BUILD_TOOL_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| DIRECTORY_AFTER_DELETE | `EMPTY` |
| EMPTY_DIRECTORY_CONTRACT | `NONE` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

### CLEAN-002

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-002` |
| PATH | `src/main/java/org/example/trademodel/enums/PushTypeEnum.java` |
| SYMBOL | `org.example.trademodel.enums.PushTypeEnum` |
| CATEGORY | `UNUSED_ENUM` |
| ENUM_CONSTANTS | `PREPARE_PUSH`, `CONFIRM_PUSH`, `WARNING_PUSH` |
| DIRECT_PRODUCTION_TYPE_REFERENCES | `0` |
| DIRECT_TEST_TYPE_REFERENCES | `0` |
| IMPORT_REFERENCES | `0` |
| CLASS_LITERAL_REFERENCES | `0` |
| REFLECTION_REFERENCES | `0` |
| JACKSON_ROLE | `NONE` |
| MYBATIS_ROLE | `NONE` |
| DATABASE_ROLE | `NONE` |
| CONFIG_ROLE | `NONE` |
| PUBLIC_API_ROLE | `NONE` |
| SCHEMA_SUPPORT | `NONE` |
| NEEDED_BY_CONTRACT_FUTURE_PHASE | `NO` |
| SAME_NAME_STRING_USAGE | `PRESENT_BUT_NOT_TYPE_DEPENDENT` |
| STRING_CONTRACT_IMPACT | `NONE` |
| EXTERNAL_BINARY_COMPATIBILITY_RISK | `LOW` |
| INTRODUCED_COMMIT | `4a185172f86ee5e96f637bb63547fc4d5e4757ba` |
| CREATION_CONTEXT | `CURSOR_ERA_BULK_GENERATED_ARCHITECTURE_PLACEHOLDER` |
| EVIDENCE_LEVEL | `E3` |
| RISK | `LOW` |
| ACTION | `DELETE` |

The string literals `PREPARE_PUSH`, `CONFIRM_PUSH`, and `WARNING_PUSH` remain in
the derivatives review path where applicable. Repository inspection proves
those strings are not imports, class literals, enum deserialization, or other
type-dependent uses of `PushTypeEnum`.

### CLEAN-001-EXCLUDED-ENTITY

| Evidence field | Value |
|---|---|
| CANDIDATE_ID | `CLEAN-001-EXCLUDED-ENTITY` |
| PATH | `src/main/java/org/example/trademodel/entity/PackageInfo.java` |
| SYMBOL | `org.example.trademodel.entity.PackageInfo` |
| CATEGORY | `FRAMEWORK_REFERENCED` |
| MYBATIS_ROLE | `TYPE_ALIAS` |
| TYPE_ALIASES_PACKAGE | `org.example.trademodel.entity` |
| ALIAS | `packageinfo` |
| ALIAS_TARGET | `org.example.trademodel.entity.PackageInfo` |
| VALIDATION | `REAL_SQLSESSIONFACTORY_TYPE_ALIAS_REGISTRY` |
| RISK | `HIGH_IF_DELETED` |
| ACTION | `KEEP` |
| DELETE_ALLOWED | `NO` |

The retained alias contract is validated by
`MyBatisEntityPackageInfoAliasContractTest`, which loads the real Spring Boot
`SqlSessionFactory`, resolves `packageinfo` from MyBatis `TypeAliasRegistry`, and
asserts that the target is exactly `org.example.trademodel.entity.PackageInfo`.

### Exact governed paths

LOW_DELETE_PATHS:

1. `src/main/java/org/example/trademodel/config/PackageInfo.java`
2. `src/main/java/org/example/trademodel/converter/PackageInfo.java`
3. `src/main/java/org/example/trademodel/dto/PackageInfo.java`
4. `src/main/java/org/example/trademodel/dto/req/PackageInfo.java`
5. `src/main/java/org/example/trademodel/enums/PackageInfo.java`
6. `src/main/java/org/example/trademodel/enums/PushTypeEnum.java`
7. `src/main/java/org/example/trademodel/mapper/PackageInfo.java`
8. `src/main/java/org/example/trademodel/validator/PackageInfo.java`

FRAMEWORK_KEEP_PATHS:

1. `src/main/java/org/example/trademodel/entity/PackageInfo.java`
