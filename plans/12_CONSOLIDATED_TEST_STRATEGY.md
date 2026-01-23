# Consolidated Test Strategy - Bridge Layer Testing

**Date**: 2026-01-23
**Philosophy**: Test the bridge between Kotlin code and keytool, not the tool itself
**Focus**: Parameter validation, output parsing, error mapping

---

## TESTING PHILOSOPHY

KeyToolAPI is a **bridge layer** between Kotlin code and the keytool command-line tool. We test:

1. **Parameter Validation** - Do we accept right/possible values?
   - Enum values are used correctly
   - Invalid values are rejected
   - Auto-selection logic works
   - Parameter bounds are enforced

2. **Output Parsing** - Do we parse the tool's output correctly?
   - Standard output is parsed correctly
   - Different output formats are handled
   - Parsing errors are caught
   - Data is extracted accurately

3. **Error Mapping** - Do we map tool errors to our error types?
   - Exit codes are captured
   - Error messages are extracted
   - Keytool errors become KeytoolResult.Error
   - Error context is preserved

**Out of Scope**:
- Tool behavior/bugs (not our problem)
- Workflow combinations (tool's responsibility)
- Multi-keystore workflows (redundant with function testing)
- Tool-specific edge cases (like validity = 0)

---

## CURRENT TEST COVERAGE ANALYSIS

### Unit Tests (KeyToolAPITest.kt) - 21 tests
Status: **Good foundation, incomplete on parameter validation**

**What's Well Tested**:
- ✅ Output parsing for all 16 functions (list, cert, csr, crl)
- ✅ Error message extraction (5+ error types)
- ✅ Some parameter variations (custom sigalg, custom keysize)
- ✅ Exit code validation (line 198: `assertEquals(1, error.exitCode)`)

**What's Missing**:
- ❌ Complete parameter validation for all algorithms
  - Only RSA and EC tested (missing DSA, EdDSA)
  - Only AES tested for symmetric (missing TripleDES)
  - Limited key size variants
- ❌ Comprehensive error message patterns
  - Not all 25+ error conditions covered
  - Missing specific error message validation
- ❌ Output parsing edge cases
  - Different output formats (verbose vs quiet)
  - Locale-dependent parsing (dates, numbers)
  - Special characters in DN parsing

**Tests to Keep**: All 21 tests (good baseline)

**Tests to Add**: ~15-20 tests for missing parameter validation and error messages

---

### Integration Tests (KeyToolAPIIntegrationTests.kt) - 31 tests
Status: **Solid functional coverage, some workflow removal needed**

**What's Well Tested**:
- ✅ All 16 functions with real keytool
- ✅ Core functionality for each function
- ✅ Error cases (wrong password, nonexistent alias)
- ✅ Algorithm variants (RSA sizes, EC curves)
- ✅ Type-safe enum usage throughout

**What's Problematic**:
- ❌ Workflow tests (test_28, test_29) - These are tool responsibility
  - test_28 (sign-and-import): Tests 3 functions in combination
  - test_29 (move-keys): Tests 2 functions in combination
  - **Action**: Convert to single-function tests or remove

**What's Missing**:
- ❌ DSA key generation
- ❌ EdDSA key generation
- ❌ TripleDES secret key generation
- ❌ More EC curves (P-256, P-521 - only P-384 tested)
- ❌ More RSA sizes (2048/3072/4096 tested, but more variants)
- ❌ Explicit format conversion test (JKS → PKCS12 with types)
- ❌ Output format variations (verbose vs non-verbose for list)
- ❌ More error scenarios per function

**Tests to Keep**: tests_01-27, test_30-31 (25 tests)

**Tests to Refactor/Remove**: test_28, test_29 (workflow tests - 2 tests)

**Tests to Add**: ~12-18 tests for missing algorithm coverage and error scenarios

---

## THREE PILLAR TEST GAPS

### PILLAR 1: Parameter Validation

**Missing Algorithm Coverage**:

1. **genKeyPair() - Missing Key Types**
   - ✅ RSA (2048, 4096 tested)
   - ✅ EC with P-384 (only)
   - ❌ DSA (not tested)
   - ❌ EdDSA / Ed25519 (not tested)
   - ❌ EC with P-256 (not tested)
   - ❌ EC with P-521 (not tested)
   - **Action**: Add 4-5 integration tests for missing key types

2. **genSecKey() - Missing Key Types**
   - ✅ AES (256-bit tested)
   - ❌ TripleDES (not tested)
   - ❌ AES with other sizes (128, 192 not tested)
   - **Action**: Add 2-3 integration tests

3. **Signature Algorithm Auto-Selection** (Unit Test)
   - ✅ Test explicit sigalg specification
   - ❌ Test auto-selection logic
   - ❌ Test that auto-selection picks correct algorithm for key type
   - **Action**: Add 3-4 unit tests for auto-selection logic

4. **Parameter Bounds** (Unit Tests)
   - ✅ Invalid key size (10000) tested for RSA
   - ❌ Edge cases for validity period
   - ❌ Edge cases for key size (minimum, maximum)
   - ❌ Alias name edge cases (length, special chars)
   - ❌ DN component validation
   - **Action**: Add 4-6 unit tests for edge cases

---

### PILLAR 2: Output Parsing

**Current Coverage**:
- ✅ List output (complete with all fields)
- ✅ Certificate output (owner, issuer, fingerprints)
- ✅ CSR output (subject, algorithm, extensions)
- ✅ CRL output (issuer, dates, revoked certs)

**Missing**:
- ❌ List output in non-verbose mode
- ❌ Parsing output with special characters in DN
- ❌ Different locale output (date formats, numbers)
- ❌ Empty/minimal output scenarios
- ❌ Output with Unicode characters

**Action**: Add 3-5 unit tests for output parsing edge cases

---

### PILLAR 3: Error Mapping

**Current Coverage**:
- ✅ Wrong password (list, storepasswd)
- ✅ File not found (list, printcert, printcsr, printcrl, importcert, importkeystore)
- ✅ Parsing errors (list, printcert, printcsr, printcrl)
- ✅ Alias errors (export, delete, changealias)
- ✅ Invalid parameter (genkeypair dname, genseckey algo)
- ✅ Unrecoverable key (keypasswd wrong password)

**Missing Error Types** (from OpenJDK reference):
- ❌ Invalid key size for algorithm
- ❌ Insufficient key password strength
- ❌ Insufficient store password strength
- ❌ Invalid DN format
- ❌ Invalid alias format
- ❌ Certificate already exists in store
- ❌ Certificate chain validation failure
- ❌ Encrypted key entry requires keypass
- ❌ Format mismatch (JKS vs PKCS12)
- ❌ Keystore corrupted/tampered
- ❌ Unknown key algorithm
- ❌ Unsupported provider
- ❌ CRL not found
- ❌ Invalid certificate in chain

**Action**: Add 8-12 unit tests for error message mapping

---

## RECOMMENDED TEST ADDITIONS

### Unit Tests to Add (~15-20 new tests)

**Parameter Validation (4-6 tests)**:
- `testGenKeyPairInvalidKeySize_tooSmall()` - RSA < 512
- `testGenKeyPairInvalidKeySize_tooBig()` - RSA > 16384
- `testGenKeyPairInvalidDName()` - Invalid DN format
- `testGenSecKeyInvalidKeySize()` - Invalid size for algorithm
- `testGenSecKeyAES128()` - Different AES size
- `testGenSecKeyAES192()` - Different AES size

**Signature Algorithm Auto-Selection (3-4 tests)**:
- `testGenKeyPairAutoSelectRSA2048()` - Should pick SHA256withRSA
- `testGenKeyPairAutoSelectRSA4096()` - Should pick SHA512withRSA
- `testGenKeyPairAutoSelectEC256()` - Should pick SHA256withECDSA
- `testGenKeyPairAutoSelectEC384()` - Should pick SHA384withECDSA

**Output Parsing Edge Cases (3-5 tests)**:
- `testListOutputParsingQuietMode()` - Non-verbose output
- `testPrintCertParsingWithSpecialChars()` - DN with special chars
- `testCertificateFingerprintParsing()` - Multiple hash algorithms
- `testCSRParsingWithoutExtensions()` - Minimal CSR output
- `testCRLParsingEmptyRevocationList()` - No revoked certs

**Error Message Mapping (5-8 tests)**:
- `testGenKeyPairErrorInvalidKeySize()` - Bad key size error
- `testGenSecKeyErrorInvalidAlgorithm()` - Unsupported algorithm
- `testImportCertErrorInvalidCertificate()` - Invalid cert data
- `testGenCertErrorMissingKeyPassword()` - Required param missing
- `testListErrorKeystoreCorrupted()` - Tampered keystore
- `testImportKeystoreErrorFormatMismatch()` - JKS/PKCS12 conflict
- `testKeyPasswdErrorWeakPassword()` - Min length violation
- `testDeleteErrorAliasNotFound()` - Alias doesn't exist

---

### Integration Tests to Add (~12-18 new tests)

**Algorithm Coverage (5-7 tests)**:
- `test_32_genKeyPair_dsa_default()` - DSA 2048
- `test_33_genKeyPair_eddsa_ed25519()` - EdDSA Ed25519
- `test_34_genKeyPair_eddsa_ed448()` - EdDSA Ed448
- `test_35_genKeyPair_ec_256()` - EC P-256 (not P-384)
- `test_36_genKeyPair_ec_521()` - EC P-521 (not P-384)
- `test_37_genSecKey_tripledes()` - TripleDES symmetric
- `test_38_genSecKey_aes_128()` - AES 128-bit variant

**Output Format Variations (2-3 tests)**:
- `test_39_list_quiet_mode()` - Non-verbose list
- `test_40_list_with_special_chars_in_dn()` - DN with special chars
- `test_41_exportCert_der_and_pem()` - Both formats

**Error Scenario Coverage (3-4 tests)**:
- `test_42_import_cert_invalid_certificate()` - Bad cert data
- `test_43_genKeyPair_duplicate_alias()` - Already exists
- `test_44_changeAlias_to_existing_alias()` - Target already exists
- `test_45_importKeystore_missing_source_file()` - Source not found (already covered)

**Format Conversion (1 test)**:
- `test_46_importKeystore_jks_to_pkcs12()` - Explicit format types
  - Create JKS keystore
  - Import to PKCS12 with explicit srcstoretype/deststoretype
  - Verify format changed

---

## TESTS TO REMOVE OR REFACTOR

### Remove Workflow Tests (2 tests)

**test_28_workflow_sign_and_import()**:
- **Problem**: Tests 3 functions in sequence (genKeyPair → certReq → genCert → importCert)
- **Why remove**: This is tool responsibility, not bridge testing
- **Alternative**: Keep genCert, remove the multi-step orchestration

**test_29_workflow_move_keys()**:
- **Problem**: Tests bulk importKeystore operation (really just 2 functions repeated)
- **Why remove**: Each importKeystore call should work independently
- **Alternative**: Keep single importKeystore tests

**Action**: Remove both tests. Saves 2 tests.

---

## NEW TEST FILE STRUCTURE

No new files needed. Keep existing structure:

```
composeApp/src/jvmTest/kotlin/unidesk/com/br/keymanager/core/
├── KeyToolAPITest.kt (21 existing + 15-20 new = 36-41 unit tests)
└── KeyToolAPIIntegrationTests.kt (31 existing - 2 workflows + 12-18 new = 41-47 integration tests)
```

**Total**: 77-88 tests (up from 52)

---

## IMPLEMENTATION ROADMAP

### Phase 1: Unit Test Expansion (5-8 hours)
1. ✅ Keep all 21 existing unit tests
2. Add 15-20 new unit tests for:
   - Parameter validation edge cases
   - Signature algorithm auto-selection
   - Output parsing variations
   - Error message mapping
3. Verify all pass with mock output

### Phase 2: Integration Test Refactoring (3-5 hours)
1. ✅ Keep tests 1-27, 30-31 (25 tests)
2. ❌ Remove tests 28-29 (2 workflow tests)
3. Add 12-18 new tests for:
   - Missing algorithm types
   - Output format variations
   - Error scenarios
   - Format conversion

### Phase 3: Verify Bridge Contract (2-3 hours)
1. Run full test suite (77-88 tests)
2. Verify all parameter values are handled
3. Verify all error messages are mapped
4. Verify all output formats are parsed

**Total Effort**: 10-16 hours

---

## SUCCESS CRITERIA

✅ **Parameter Validation**:
- All 16 functions tested with valid parameters
- All enum values exercised
- Invalid parameters produce errors
- Auto-selection logic verified

✅ **Output Parsing**:
- All output formats parsed correctly
- Edge cases handled (empty, special chars, etc.)
- Parsing errors caught
- Data extracted accurately

✅ **Error Mapping**:
- All common keytool errors captured
- Exit codes preserved
- Error messages extracted
- KeytoolResult.Error used correctly

✅ **No Workflow Tests**:
- Each function tested independently
- No multi-function orchestration
- Bridge layer only, not tool testing

---

## REFERENCES

**OpenJDK Keytool Source**:
- https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/sun/security/tools/keytool/Main.java

**Type-Safe Enums** (Already Implemented):
- `KeyAlgorithm.kt` - 15+ algorithms with validation
- `SignatureAlgorithm.kt` - 20+ signatures with auto-selection
- `ECCurve.kt` - NIST curves (P-256, P-384, P-521)
- `KeystoreFormat.kt` - 5 format types

---

## NEXT STEPS

1. Implement Phase 1: Add 15-20 unit tests
2. Implement Phase 2: Refactor and expand integration tests
3. Run full suite and verify all pass
4. Update this document with actual counts
5. Close out remaining phases (4-6 from original plan)

---

**Status**: Ready for implementation
**Owner**: Assistant
**Next Review**: After Phase 1 complete
