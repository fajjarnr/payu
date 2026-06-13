---
description: Advanced AI Orchestration, Task Management, and Core Engineering Principles for PayU
---

# 🛰️ Advanced AI Orchestration Protocol

This protocol defines how AI agents should operate to ensure high quality, reliability, and autonomy.

## 0. Context Loading (ALWAYS FIRST)

**Before ANY non-trivial task, load the platform truth:**

1. Read `.agents/context/ROADMAP.md` — Current platform scores, P0 blockers, service scoreboard
2. Reference `docs/roadmap/TODOS.md` for full roadmap and P0/P1/P2 priorities
3. Reference `docs/guides/REMEDIATION_PLAYBOOK.md` for step-by-step fix instructions (R-001 through R-016)
4. Reference `docs/guides/LESSONS.md` for implementation patterns and anti-patterns

**Production Readiness: 48/100** — There are 5 P0 blockers. All work should either:
- Directly fix a P0/P1 issue, OR
- Not introduce new issues that contradict the remediation plan

## 1. Workflow Orchestration
- **Plan Mode Default**: Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions).
- **Graceful Halt**: If something goes sideways, STOP and re-plan immediately – don't keep pushing.
- **Verification-First Planning**: Use plan mode for verification steps, not just building.
- **Detailed Specs**: Write detailed specs upfront to reduce ambiguity.

## 2. Subagent Strategy
- **Liberal Subagent Usage**: Use subagents liberally to keep main context window clean.
- **Offload & Parallelize**: Offload research, exploration, and parallel analysis to subagents.
- **Compute Scaling**: For complex problems, throw more compute at it via subagents.
- **Focused Execution**: One task per subagent for focused execution.

## 3. Self-Improvement Loop
- **Pattern Capturing**: After ANY correction from the user: update `docs/guides/LESSONS.md` with the pattern.
- **Recursive Rules**: Write rules for yourself that prevent the same mistake.
- **Ruthlessly Iterate**: Iterate on these lessons until mistake rate drops.
- **Pre-Session Review**: Review lessons at session start for relevant project context.

## 4. Verification Before Done
- **Proof of Work**: Never mark a task complete without proving it works.
- **Behavioral Diffing**: Diff behavior between main and your changes when relevant.
- **Staff Engineer Standard**: Ask yourself: "Would a staff engineer approve this?"
- **E2E Validation**: Run tests, check logs, demonstrate correctness explicitly.

## 5. Demand Elegance (Balanced)
- **Elegance Pause**: For non-trivial changes: pause and ask "is there a more elegant way?"
- **Refactoring for Quality**: If a fix feels hacky: "Knowing everything I know now, implement the elegant solution".
- **Avoid Over-engineering**: Skip this for simple, obvious fixes – don't over-engineer.
- **Internal Critique**: Challenge your own work before presenting it.

## 6. Autonomous Bug Fixing
- **Just Fix It**: When given a bug report: just fix it. Don't ask for hand-holding.
- **Evidence-Based Resolution**: Point at logs, errors, failing tests – then resolve them.
- **Zero Context Switching**: Aim for zero context switching required from the user.
- **Proactive Maintenance**: Go fix failing CI tests without being told how.

## 📋 Task Management Protocol
- **Plan First**: Write plan to `docs/roadmap/TODOS.md` with checkable items.
- **Verify Plan**: Check in before starting implementation.
- **Track Progress**: Mark items complete as you go.
- **Explain Changes**: High-level summary at each step.
- **Document Results**: Add review section to `docs/roadmap/TODOS.md`.
- **Capture Lessons**: Update `docs/guides/LESSONS.md` after corrections.

## ⚖️ Core Engineering Principles
- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.
