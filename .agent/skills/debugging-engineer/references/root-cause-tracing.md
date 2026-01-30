# Root Cause Tracing

## Overview
Bugs often manifest deep in the call stack. Your instinct is to fix where the error appears, but that's treating a symptom.
**Core principle:** Trace backward through the call chain until you find the original trigger, then fix at the source.

## The Tracing Process

### 1. Observe the Symptom
```
Error: git init failed in /Users/jesse/project/packages/core
```

### 2. Find Immediate Cause
**What code directly causes this?**
```typescript
await execFileAsync('git', ['init'], { cwd: projectDir });
```

### 3. Ask: What Called This?
Trace the caller.
```typescript
WorktreeManager.createSessionWorktree(projectDir, sessionId)
  → called by Session.initializeWorkspace()
  → called by Session.create()
```

### 4. Keep Tracing Up
**What value was passed?**
- `projectDir = ''` (empty string!)
- Empty string as `cwd` resolves to `process.cwd()` (Source Code Dir!)

### 5. Find Original Trigger
**Where did empty string come from?**
```typescript
const context = setupCoreTest(); // Returns { tempDir: '' }
Project.create('name', context.tempDir); // Accessed before beforeEach!
```

**Root cause:** Top-level variable initialization accessing empty value.
**Fix:** Made tempDir a getter that throws if accessed before beforeEach.

## Tracing in Tests
When you can't trace manually, add instrumentation:

```typescript
// Before the problematic operation
async function problematicOp(directory: string) {
  const stack = new Error().stack; // Capture Stack
  console.error('DEBUG TRACE:', {
    directory,
    stack,
  });
  // ... operation
}
```

**Critical:** Use `console.error()` in tests (not logger) to ensure visibility in CI logs.
