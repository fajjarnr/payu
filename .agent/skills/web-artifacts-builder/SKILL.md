---
name: web-artifacts-builder
description: Suite of tools for creating elaborate, multi-component PayU HTML artifacts using modern frontend web technologies (React, Tailwind CSS, shadcn/ui). Use for complex artifacts requiring state management, routing, or shadcn/ui components.
---

# Web Artifacts Builder (PayU Edition)

To build powerful frontend artifacts for PayU, follow ini steps:
1. Initialize the artifact project using `.agent/skills/web-artifacts-builder/scripts/init-artifact.sh`
2. Develop your artifact by editing the generated code (uses local components from `/components/ui`)
3. Bundle all code into a single HTML file using `.agent/skills/web-artifacts-builder/scripts/bundle-artifact.sh`
4. Display/Share the artifact

**Stack**: React 18 + TypeScript + Vite + Parcel (bundling) + Tailwind CSS + shadcn/ui

## Design & Style Guidelines

Follow the **Premium Emerald** design system:
- **Primary Color**: `#10b981` (emerald-500)
- **Typography**: Outfit (Headers) & Inter (UI)
- **Aesthetics**: Glassmorphism, subtle noise, layered shadows.

## Quick Start

### Step 1: Initialize Artifact Project

Run the initialization script to create a new React project. This will automatically copy shadcn/ui components from the project root.

```bash
bash .agent/skills/web-artifacts-builder/scripts/init-artifact.sh <artifact-name>
cd <artifact-name>
```

### Step 2: Develop Your Artifact

Edit `src/App.tsx`. You can import components like this:
```tsx
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
```

### Step 3: Bundle to Single HTML File

To bundle the app into a single HTML file:
```bash
bash ../.agent/skills/web-artifacts-builder/scripts/bundle-artifact.sh
```

This creates `bundle.html` in the artifact directory.

## Reference
Local components are located in `.agent/resources/shadcn/ui`.
