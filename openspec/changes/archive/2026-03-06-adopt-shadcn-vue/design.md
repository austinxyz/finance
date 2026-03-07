## Context

The project is a Vue 3 app using Tailwind CSS with 5 hand-rolled shadcn-style components. Dependencies like `radix-vue`, `cva`, `clsx`, and `tailwind-merge` are already installed — the primitives are in place. What's missing is the CLI tooling and `components.json` that makes shadcn-vue components formally managed, upgradeable, and consistent.

The shadcn-ui v4.0 release (React) introduced a new registry system and CLI overhaul. The Vue equivalent is **shadcn-vue**, which tracks shadcn-ui and has adopted compatible patterns. We adopt shadcn-vue (not the React shadcn-ui) since the app is Vue 3.

## Goals / Non-Goals

**Goals:**
- Initialize `components.json` and CSS design tokens
- Replace 5 existing manual components with CLI-generated equivalents
- Add 9 missing commonly-needed components
- Keep all existing page layouts unchanged

**Non-Goals:**
- Refactoring pages to use new components beyond the 5 replacements
- Upgrading to a new major version of Tailwind (stays on v3)
- Dark mode support (design token foundation is laid, but dark mode not wired)

## Decisions

### Decision 1: shadcn-vue over shadcn-ui (React)
**Choice**: Use [shadcn-vue](https://www.shadcn-vue.com/) — the official Vue port.
**Rationale**: The project is Vue 3. The upstream `shadcn-ui` repo is React-only. shadcn-vue tracks shadcn-ui closely and shares the same design system, CLI patterns, and `components.json` format.

### Decision 2: CSS Variables (default) over inline Tailwind tokens
**Choice**: Use shadcn-vue's default CSS variable approach (`--background`, `--primary`, etc.) in `index.css`.
**Rationale**: This is the shadcn-vue default and enables future dark mode with a single class toggle. The existing app uses raw Tailwind colors; the 5 replaced components will adopt CSS vars; other pages are unaffected.

### Decision 3: Replace existing 5 components, don't add alongside
**Choice**: Overwrite `Button.vue`, `Card.vue`, `CardContent.vue`, `CardHeader.vue`, `CardTitle.vue` with CLI-generated versions.
**Rationale**: The existing components are already shadcn-style — the CLI output is a drop-in replacement. Keeping both creates confusion. The only consumer of these components is `LiabilityDetail.vue`.

### Decision 4: Add 9 new components proactively
**Choice**: Add `Badge`, `Select`, `Dialog`, `Table`, `Tabs`, `Separator`, `Input`, `Label`, `Tooltip` via CLI.
**Rationale**: These are the most commonly needed across the app (forms, tables, dialogs). Having them available prevents future ad-hoc re-implementations.

## Risks / Trade-offs

- **CSS variable conflicts**: Adding shadcn-vue CSS vars to `index.css` could override existing Tailwind color usage. → Mitigation: audit existing color references before adding vars; scope vars to `:root` only.
- **radix-vue version mismatch**: shadcn-vue CLI pins specific radix-vue versions. → Mitigation: run `npm install` after CLI init and resolve any peer dependency warnings.
- **5 replaced components change APIs**: CLI-generated components may have slightly different prop signatures. → Mitigation: check `LiabilityDetail.vue` after replacement; adjust if needed.

## Migration Plan

1. Install shadcn-vue CLI and run `npx shadcn-vue@latest init` (generates `components.json`, updates `tailwind.config.cjs`, adds CSS vars to `index.css`)
2. Add the 9 new components via `npx shadcn-vue@latest add <name>`
3. Overwrite the 5 existing components via `npx shadcn-vue@latest add --overwrite button card`
4. Verify `LiabilityDetail.vue` still compiles and renders correctly
5. Run Vitest suite to catch any prop/import regressions

**Rollback**: git revert. All changes are frontend-only with no backend or DB impact.
