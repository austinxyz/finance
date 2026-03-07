## Why

The project currently uses shadcn-vue primitives (radix-vue, cva, tailwind-merge) and has 5 manually written UI components, but was never formally initialized with the shadcn-vue CLI — there is no `components.json` and the component set is incomplete. Formally adopting shadcn-vue provides a CLI-managed component library with consistent design tokens, a complete accessible component set, and a clear upgrade path.

## What Changes

- Initialize shadcn-vue CLI (`components.json`, CSS variable design tokens in `tailwind.config.cjs`)
- Replace 5 manually written components (`Button`, `Card`, `CardContent`, `CardHeader`, `CardTitle`) with CLI-managed shadcn-vue equivalents
- Add missing high-value components currently absent but needed across the app: `Badge`, `Select`, `Dialog`, `Table`, `Tabs`, `Separator`, `Input`, `Label`, `Tooltip`
- Update Tailwind config and `index.css` to include shadcn-vue CSS custom properties

## Non-goals

- Full redesign of existing page layouts
- Migration of all Tailwind utility classes to shadcn components
- Backend changes (none required)

## Capabilities

### New Capabilities
- `shadcn-vue-component-library`: CLI-managed shadcn-vue component library with design tokens, `components.json`, and an expanded set of accessible UI primitives

### Modified Capabilities
*(none — no existing spec behavior changes)*

## Impact

- **Frontend only**: `components.json`, `tailwind.config.cjs`, `src/index.css`, `src/components/ui/` directory
- **No backend changes**: zero API or DB schema changes
- **Dependency changes**: add `@shadcn-vue/cli` (dev), update `radix-vue` to latest, verify `tailwindcss` CSS variable support
- **No breaking changes to existing pages**: existing components will be replaced with drop-in equivalents
