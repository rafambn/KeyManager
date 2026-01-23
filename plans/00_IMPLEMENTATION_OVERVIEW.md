# Complete Test Suite Rewrite Plan
## KeyToolAPI Based on OpenJDK Keytool Test Patterns

**Status**: Planning Phase (Awaiting User Review)
**Start Date**: 2026-01-22
**Target**: Full OpenJDK-aligned test coverage for all 16 KeyToolAPI functions

---

## 1. EXECUTIVE SUMMARY

This document outlines a comprehensive, phase-by-phase approach to rewrite the KeyToolAPI test suite based on **OpenJDK keytool test patterns** from the official Java repository (`test/jdk/sun/security/tools/keytool/`).

### Key Decision: Option B - Complete Rewrite
- **Why**: Current 50 mock-based tests are arbitrary and don't match real keytool behavior
- **How**: Implement 6 phases over structured iterations
- **Outcome**: 150-200+ tests covering all real-world scenarios OpenJDK validates

### Three Test Categories (OpenJDK Model)
1. **Unit Tests** (Mocked) - 60-70 tests
   - Output parsing validation
   - Error message mapping
   - Exit code handling
2. **Integration Tests** (Real keytool) - 50-70 tests
   - Functional operations with actual keystore files
   - Certificate chain validation and deduplication
   - Format conversion (JKS ↔ PKCS12)
   - Algorithm strength detection
3. **Edge Case & Regression Tests** - 30-40 tests
   - Known keytool issues from OpenJDK bug tracker
   - Platform-specific behaviors
   - Security validation (weak algorithms, empty subjects)

---

## 2. PHASE BREAKDOWN

### Phase 1: Research & Planning ✓ COMPLETE
**Duration**: Research completed
**Output**: Reference documents and analysis
**Files Created**:
- `00_IMPLEMENTATION_OVERVIEW.md` (this file)
- `01_REFERENCE_OPENJDK_FINDINGS.md` - Complete OpenJDK analysis
- `02_REFERENCE_TEST_PATTERNS.md` - Reusable test patterns
- `03_REFERENCE_ALGORITHM_COVERAGE.md` - All algorithms to test
- `04_REFERENCE_ERROR_CONDITIONS.md` - All error conditions
- `05_REFERENCE_CERTIFICATE_CHAINS.md` - Chain testing strategies

**Next**: User reviews and approves structure

---

### Phase 2: Unit Tests Rewrite
**Focus**: Mock-based tests for parsing and basic operations
**Document**: `PHASE_2_UNIT_TESTS_PLAN.md`
**Deliverables**:
- 60-70 unit tests using mocked ProcessBuilder output
- Tests for all 16 functions' success/error/edge cases
- Parsing validation tests (catch malformed outputs)
- Exit code handling tests

---

### Phase 3: Integration Tests
**Focus**: Real keytool operations with actual keystores
**Document**: `PHASE_3_INTEGRATION_TESTS_PLAN.md`
**Deliverables**:
- 50-70 integration tests using real keytool binary
- Temporary keystore creation and cleanup
- Certificate chain generation and validation
- Multi-operation workflows (generate → sign → import → list)

---

### Phase 4: Edge Cases & Regression Tests
**Focus**: Known issues and special scenarios
**Document**: `PHASE_4_EDGE_CASES_PLAN.md`
**Deliverables**:
- 30-40 edge case tests
- Tests for bugs documented in OpenJDK
- Security validations (weak algorithms, empty subjects)
- Platform-specific behaviors

---

### Phase 5: Test Data Management
**Focus**: Efficient test keystore and certificate generation
**Document**: `PHASE_5_TEST_DATA_PLAN.md`
**Deliverables**:
- Test keystore generation utilities
- Pre-built keystores for regression testing
- Certificate chain fixtures
- PBE algorithm test data

---

### Phase 6: Verification & Documentation
**Focus**: Test coverage analysis and patterns guide
**Document**: `PHASE_6_VERIFICATION_PLAN.md`
**Deliverables**:
- Coverage matrix showing which OpenJDK tests we're replicating
- Test organization guide
- Best practices for adding new tests
- Documentation of test data and fixtures

---

## 3. IMPLEMENTATION FLOW

```
User Reviews Phases 1-6 Plans
        ↓
User Approves Approach
        ↓
PHASE 2: Rewrite Unit Tests
├─ Refactor existing 50 tests
├─ Add missing test cases
├─ Implement new parsing edge cases
└─ Build & verify passes
        ↓
PHASE 3: Implement Integration Tests
├─ Create test keystore generation
├─ Implement multi-operation workflows
├─ Test certificate chain scenarios
└─ Build & integration tests pass
        ↓
PHASE 4: Add Edge Cases & Regression Tests
├─ Implement known issue tests
├─ Add security validation tests
├─ Add platform-specific tests
└─ All tests pass
        ↓
PHASE 5: Organize Test Data
├─ Create data fixtures
├─ Pre-generate test keystores
├─ Document test data strategy
└─ Verify data format consistency
        ↓
PHASE 6: Verification & Documentation
├─ Run full test suite (150-200+ tests)
├─ Generate coverage report
├─ Document test patterns
└─ Final review
```

---

## 4. SCOPE: WHAT'S COVERED FROM OpenJDK

### From OpenJDK keytool Tests (45 test classes):

**Core Operations** (all 16 functions):
- ✓ Generate keypair (`genKeyPair`) - GenerateAll, KeyAlg patterns
- ✓ Generate secret key (`genSecKey`) - PKCS12Passwd, SecretKeyKS patterns
- ✓ Generate certificate (`genCert`) - GenerateAll, SelfIssued patterns
- ✓ Certificate request (`certReq`) - GenKeyPairSigner patterns
- ✓ Export certificate (`exportCert`) - PrintSSL, ReadJar patterns
- ✓ Import certificate (`importCert`) - DupImport, SelfIssued, EmptySubject patterns
- ✓ Import keystore (`importKeystore`) - JKStoPKCS12, TryStore patterns
- ✓ Import password (`importPass`) - PKCS12Passwd patterns
- ✓ Delete alias (`delete`) - DupCommands, CloseFile patterns
- ✓ Change alias (`changeAlias`) - RealType patterns
- ✓ Key password (`keyPasswd`) - PKCS12Passwd, StorePasswords patterns
- ✓ Store password (`storePasswd`) - StorePasswords patterns
- ✓ Print certificate (`printCert`) - PrintSSL, UnknownAndUnparseable patterns
- ✓ Print certificate request (`printCertReq`) - GenKeyPairSigner patterns
- ✓ Print CRL (`printCrl`) - GenerateAll patterns
- ✓ List (`list`) - ListOrder, StartDateTest patterns

**Algorithm Coverage** (30+ algorithms):
- RSA (512-bit, 1024-bit, 2048-bit, 4096-bit, PSS variants)
- DSA (2048-bit)
- EC (P-256, P-384, P-521, secp256r1, secp384r1, secp521r1)
- EdDSA (Ed25519, Ed448)
- XDH (key exchange)
- DES, AES (for secret keys)
- PBE (18 variants with MD5/SHA1/SHA512)

**Format Testing**:
- JKS (default format)
- PKCS12 (RFC standard)
- JCEKS (Java Cryptography Extension)
- KeychainStore (macOS)
- PKCS11/NSS (hardware tokens)

**Certificate Chain Scenarios** (15+ patterns):
- ✓ Hierarchical chains (CA → CA1 → E1)
- ✓ Self-issued certificates
- ✓ Certificate deduplication during import
- ✓ Authority Key Identifier propagation
- ✓ Chain auto-discovery with missing intermediates
- ✓ Subject Alternative Names (SAN) in chains
- ✓ Empty subject with SAN extension
- ✓ Certificate chain reconstruction

**Security Validations**:
- ✓ Weak algorithm detection (MD5, SHA-1, 512-bit RSA)
- ✓ Deprecated algorithm warnings (JKS format deprecation)
- ✓ Empty subject without SAN (invalid)
- ✓ Weak symmetric keys (DES)
- ✓ Malformed certificate handling

**Error Conditions** (25+ error scenarios):
- No -keyalg specified
- Duplicate commands
- Certificate chain establishment failure
- Public key mismatch during import
- Non-probeable keystore formats
- Missing resource exceptions
- File stream closure issues
- Ambiguous extension options
- And 17 more...

---

## 5. FILE STRUCTURE AFTER IMPLEMENTATION

```
composeApp/src/jvmTest/kotlin/unidesk/com/br/keymanager/core/
├── KeyToolAPIUnitTests.kt                 (60-70 unit tests)
├── KeyToolAPIIntegrationTests.kt          (50-70 integration tests)
├── KeyToolAPIEdgeCaseTests.kt             (30-40 edge case tests)
├── KeyToolAPITestData.kt                  (Shared test data + helpers)
├── KeyToolAPITestFixtures.kt              (Pre-built keystores)
└── OpenJDKTestPatterns.md                 (Test patterns reference)

plans/
├── 01_REFERENCE_OPENJDK_FINDINGS.md       (OpenJDK analysis)
├── 02_REFERENCE_TEST_PATTERNS.md          (Reusable patterns)
├── 03_REFERENCE_ALGORITHM_COVERAGE.md     (Algorithm matrix)
├── 04_REFERENCE_ERROR_CONDITIONS.md       (Error scenarios)
├── 05_REFERENCE_CERTIFICATE_CHAINS.md     (Chain testing)
├── PHASE_2_UNIT_TESTS_PLAN.md            (Unit test strategy)
├── PHASE_3_INTEGRATION_TESTS_PLAN.md      (Integration strategy)
├── PHASE_4_EDGE_CASES_PLAN.md            (Edge case strategy)
├── PHASE_5_TEST_DATA_PLAN.md             (Data management)
└── PHASE_6_VERIFICATION_PLAN.md          (Verification)
```

---

## 6. ESTIMATED SCOPE

| Category | Current | Target | Delta |
|---|---|---|---|
| **Total Tests** | 50 | 150-200+ | +100-150 |
| **Unit Tests** | 50 | 60-70 | +10-20 |
| **Integration Tests** | 0 | 50-70 | +50-70 |
| **Edge Cases** | 0 | 30-40 | +30-40 |
| **Test Files** | 1 | 3-4 | +2-3 |
| **Test Utilities** | Basic | Comprehensive | Extended |
| **Test Data Files** | Mock strings | Real keystores + fixtures | Generated |

---

## 7. KEY IMPROVEMENTS OVER CURRENT TESTS

### Current (50 Tests - Mock-based)
```kotlin
✗ All tests use mock ProcessBuilder output strings
✗ No integration with real keytool binary
✗ No certificate chain testing
✗ Limited error scenario coverage
✗ No edge case validation from OpenJDK
✗ Tests based on arbitrary selection
✗ No security validation tests
✗ No format conversion testing
```

### After Rewrite (150-200+ Tests - OpenJDK-aligned)
```kotlin
✓ 60-70 unit tests with mocked output
✓ 50-70 integration tests using real keytool
✓ 15+ certificate chain scenarios
✓ 25+ error condition tests
✓ 15+ edge case tests from OpenJDK
✓ Tests based on official Java test patterns
✓ 8+ security validation tests
✓ 10+ format conversion and compatibility tests
✓ Algorithm coverage for 30+ algorithms
✓ Context recovery via reference documents in plans/
```

---

## 8. NEXT STEPS

**User Actions Required**:
1. Review all 6 phase plans in the `plans/` directory
2. Review reference documents for accuracy
3. Provide feedback or ask clarifying questions
4. Approve the overall approach
5. Approve each phase before implementation begins

**I Will**:
1. Wait for your review
2. Create detailed specification for each phase
3. Prepare phase implementation upon approval
4. Save progress to files to avoid context issues

---

## 9. QUESTIONS FOR USER

1. **Test Execution Strategy**: Should integration tests be:
   - Skipped by default (optional `-PintTest` flag)?
   - Run as part of regular build?
   - Separated into a different test suite?

2. **Test Data Generation**: Should pre-built keystores:
   - Be generated during test setup (slower, fresh)?
   - Be pre-generated and committed (faster, but needs updating)?
   - Use a hybrid approach?

3. **Platform Coverage**: Should we support:
   - Linux only (current)?
   - Windows + macOS + Linux (full OpenJDK coverage)?
   - Conditional tests for KeychainStore (macOS)?

4. **Performance Constraints**: Is there a:
   - Maximum total test runtime?
   - Timeout limits for individual tests?
   - Concurrency preferences (run tests in parallel)?

5. **Context Management**: Are the reference files in `plans/`:
   - Sufficient for context recovery?
   - Too many/too few files?
   - Right level of detail?

---

**Created**: 2026-01-22
**Plan Status**: Ready for Review
**Next Review**: Awaiting user approval before Phase 2 begins
