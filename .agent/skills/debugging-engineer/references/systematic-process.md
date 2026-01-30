# Systematic Debugging Process

## Overview

Random fixes waste time and create new bugs. Quick patches mask underlying issues.

**Core principle:** ALWAYS find root cause before attempting fixes. Symptom fixes are failure.

## The Iron Law

```
NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

If you haven't completed Phase 1, you cannot propose fixes.

## The Four Phases

You MUST complete each phase before proceeding to the next.

### Phase 1: Root Cause Investigation

**BEFORE attempting ANY fix:**

1. **Read Error Messages Carefully**
   - Don't skip past errors or warnings
   - They often contain the exact solution
   - Read stack traces completely

2. **Reproduce Consistently**
   - Can you trigger it reliably?
   - If not → gather more data, don't guess.

3. **Trace Data Flow**
   - Where does bad value originate?
   - What called this with bad value?
   - Keep tracing up until you find the source.
   - See `root-cause-tracing.md` for technique.

### Phase 2: Pattern Analysis

1. **Find Working Examples**
   - Locate similar working code in same codebase.
   - What works that's similar to what's broken?

2. **Identify Differences**
   - List every difference, however small.
   - Don't assume "that can't matter".

### Phase 3: Hypothesis and Testing

1. **Form Single Hypothesis**
   - State clearly: "I think X is the root cause because Y".

2. **Test Minimally**
   - Make the SMALLEST possible change to test hypothesis.
   - Start with a **Failing Test Case**.

### Phase 4: Implementation

1. **Implement Single Fix**
   - Address the root cause identified.
   - ONE change at a time.

2. **Verify Fix**
   - Test passes now?
   - No other tests broken?

## Red Flags - STOP and Follow Process

If you catch yourself thinking:
- "Quick fix for now, investigate later"
- "Just try changing X and see if it works"
- "One more fix attempt" (when already tried 2+)
- "It's probably X, let me fix that"

**STOP using the tool**. Return to Phase 1.
