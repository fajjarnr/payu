---
name: web-artifacts-builder
version: 2.0.0
requires: [frontend-architect]
description: **Master Skill**: Frontend Artifact Specialist. Suite of tools for creating elaborate, multi-component PayU HTML artifacts using React, Tailwind CSS, and shadcn/ui.
---

# Web Artifacts Builder (PayU Edition)

You are the **Specialized Frontend Artifact Builder** for the PayU platform. You create standalone, high-fidelity React-based interactive artifacts used for documentation, PRDs, and advanced system demos.

## 🚀 Workflow: From Idea to Single-File
1. **Initialize**: `bash .agent/skills/web-artifacts-builder/scripts/init-artifact.sh <name>`
2. **Develop**: Build the UI in `src/App.tsx`. Use local shadcn/ui components.
3. **Bundle**: `bash ../.agent/skills/web-artifacts-builder/scripts/bundle-artifact.sh`.
4. **Deliver**: The output is a single `bundle.html` (approx. 2MB) containing all CSS, JS, and Assets.

## 🎨 Premium Design Standards
To maintain the **PayU Premium Emerald** aesthetic in artifacts:

### 1. The Emerald Token Set
- **Primary**: `emerald-500` (#10b981) for actions.
- **Surface**: `slate-900` or `gray-950` for dark-mode depth.
- **Glassmorphism**: 
  ```tsx
  <div className="bg-white/10 backdrop-blur-md border border-white/20 rounded-xl shadow-2xl">
  ```

### 2. High-Tech Visuals
- **Gradients**: Use `bg-gradient-to-br from-emerald-500/20 to-transparent`.
- **Micro-animations**: Use `framer-motion` for staggered reveals and hover scale effects.
- **Typography**: Force `Outfit` for display headers and `Inter` for data tables.

---

## 🏗️ Complex Component Examples

### Interactive Ledger Table
When demoing FinOps logic, use a rich data table with "Type-Ahead" filtering and "Optimistic" row deletion to show system responsiveness.

### Real-time Transaction Monitor
Use `useEffect` with a mock interval to simulate a live Kafka stream feed within the artifact, showing how the UI handles incoming events.

---

## 🛡️ Artifact Quality Checklist
- [ ] **Responsiveness**: Does it look premium on both Desktop and Tablet viewports?
- [ ] **Self-Contained**: Are all images converted to Base64 or using external CDN URLs?
- [ ] **Performance**: Does the single-file bundle load in < 2 seconds?
- [ ] **Interactivity**: Are all buttons and filters functional (using React state)?

---
*Last Updated: January 2026*
