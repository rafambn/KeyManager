# KeyManager Test Planning - ACTIVE DOCUMENTS

**Last Updated**: 2026-01-23
**Status**: Focused on bridge layer testing (parameter validation, output parsing, error mapping)

---

## ACTIVE DOCUMENTS (Use These)

### 1. **CONSOLIDATED_TEST_STRATEGY.md** ⭐ START HERE
**What**: Complete testing strategy for the bridge layer
**Why**: Single source of truth for what tests are needed and why
**Contains**:
- Testing philosophy (3 pillars: parameters, parsing, errors)
- Current coverage analysis (21 unit tests, 31 integration tests)
- Gap analysis by pillar
- Recommended test additions (15-20 unit, 12-18 integration)
- Implementation roadmap
- Success criteria

**Use when**: Planning test changes, understanding what to test next

---

### 2. **CURRENT_STATUS_AND_PROGRESS.md**
**What**: Status of Phases 0-3 implementation
**Updated**: Yes - reflects type-safe enums, async architecture, 31 integration tests
**Use when**: Need to understand what's already done

---

## REFERENCE DOCUMENTS (Keep for Context)

These files contain useful reference information but are not the primary planning documents.

### Type Safety & Algorithms
- `03_REFERENCE_ALGORITHM_COVERAGE.md` - All algorithms supported
- `06_TYPE_SAFETY_REDESIGN.md` - Enum design documentation

### OpenJDK Analysis
- `01_REFERENCE_OPENJDK_FINDINGS.md` - OpenJDK test analysis
- `04_REFERENCE_ERROR_CONDITIONS.md` - Error types from OpenJDK

### Implementation Details
- `02_REFERENCE_TEST_PATTERNS.md` - Mocking patterns, test structure
- `05_REFERENCE_CERTIFICATE_CHAINS.md` - Certificate chain testing

---

## ARCHIVED/OBSOLETE DOCUMENTS

These documents are no longer accurate and should be ignored:

- ❌ `00_IMPLEMENTATION_OVERVIEW.md` - Original 6-phase plan (superseded)
- ❌ `07_REVISED_MASTER_PLAN.md` - Interim plan (superseded)
- ❌ `09_REVISED_NEXT_STEPS.md` - Offered Options A/B/C/D (superseded by focused strategy)
- ❌ `10_TEST_COVERAGE_BREAKDOWN.md` - Function-by-function breakdown (superseded)
- ❌ `11_USER_FEEDBACK_ANALYSIS.md` - Analysis of old assumptions (superseded)
- ❌ `PHASE_2_UNIT_TESTS_PLAN.md` - Old unit test strategy (superseded)
- ❌ `PHASE_3_INTEGRATION_TESTS_PLAN.md` - Old integration strategy (superseded)

---

## FILE CHANGES SUMMARY

**Removed from Active Planning**:
- Workflow testing recommendations (sign-and-import, move-keys, etc.)
- Multi-keystore operation testing
- Format conversion as primary test
- Edge case testing for tool bugs
- Phases 4, 5, 6 references (not relevant)

**Refocused On**:
- Parameter validation (right and possible values)
- Output parsing (correct extraction)
- Error mapping (correct error handling)
- Bridge layer contract only

**Removed Tests**:
- test_28_workflow_sign_and_import
- test_29_workflow_move_keys

**Added Tests**:
- ~15-20 unit tests for parameter validation and error handling
- ~12-18 integration tests for algorithm coverage

---

## HOW TO USE THESE DOCUMENTS

### I'm new to this project
1. Read `CURRENT_STATUS_AND_PROGRESS.md` (current state)
2. Read `CONSOLIDATED_TEST_STRATEGY.md` (what to test and why)

### I'm implementing the test changes
1. Use `CONSOLIDATED_TEST_STRATEGY.md` as your checklist
2. Reference `02_REFERENCE_TEST_PATTERNS.md` for mocking patterns
3. Reference `03_REFERENCE_ALGORITHM_COVERAGE.md` for algorithm specifics
4. Reference `04_REFERENCE_ERROR_CONDITIONS.md` for error message details

### I need to understand the type-safe API
1. Read `06_TYPE_SAFETY_REDESIGN.md`
2. Read `03_REFERENCE_ALGORITHM_COVERAGE.md`
3. Check the actual enum files in source code

### I need to understand OpenJDK context
1. Read `01_REFERENCE_OPENJDK_FINDINGS.md`
2. Read `04_REFERENCE_ERROR_CONDITIONS.md`

---

## QUICK REFERENCE: TEST COUNTS

### Current (52 tests)
- Unit tests: 21 (mocked output)
- Integration tests: 31 (real keytool)

### Target (77-88 tests)
- Unit tests: 36-41 (+15-20)
- Integration tests: 41-47 (+12-18, -2 workflow tests)

### By Pillar
**Parameter Validation**: +8-10 tests
**Output Parsing**: +3-5 tests
**Error Mapping**: +8-12 tests
**Algorithm Coverage**: +5-7 tests (integration)

---

## KEY PRINCIPLES

✅ **Test the Bridge**: Kotlin code ↔ keytool
✅ **Not the Tool**: Don't test keytool behavior or tool bugs
✅ **Not Workflows**: Each function independent
✅ **Three Pillars**: Parameters, Parsing, Errors

---

**Next Action**: Implement Phase 1 (add 15-20 unit tests) from CONSOLIDATED_TEST_STRATEGY.md
