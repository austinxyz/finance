## 1. Initialize shadcn-vue CLI

- [x] 1.1 Install shadcn-vue CLI: `cd frontend && npx shadcn-vue@latest init` — accept defaults for Vue, Tailwind CSS, path aliases (`@/`)
- [x] 1.2 Verify `frontend/components.json` was created with correct `aliases.components` → `@/components/ui`
- [x] 1.3 Verify `frontend/src/index.css` contains `:root { --background: ...; --primary: ...; }` CSS custom properties
- [x] 1.4 Verify `frontend/tailwind.config.cjs` includes `tailwindcss-animate` plugin and updated `content` paths

## 2. Replace Existing UI Components

- [x] 2.1 Run `npx shadcn-vue@latest add button --overwrite` to replace `src/components/ui/Button.vue`
- [x] 2.2 Run `npx shadcn-vue@latest add card --overwrite` to replace `src/components/ui/Card.vue`, `CardContent.vue`, `CardHeader.vue`, `CardTitle.vue`
- [x] 2.3 Open `frontend/src/views/liabilities/LiabilityDetail.vue` and verify imports still resolve correctly (adjust import paths if CLI changes file structure)
- [ ] 2.4 Start dev server (`npm run dev`) and manually verify LiabilityDetail page renders without errors

## 3. Add New Components

- [x] 3.1 Run `npx shadcn-vue@latest add badge input label separator`
- [x] 3.2 Run `npx shadcn-vue@latest add select dialog table tabs tooltip`
- [x] 3.3 Confirm all new component files exist under `src/components/ui/`

## 4. Dependency Cleanup

- [x] 4.1 Run `npm install` to resolve any updated peer dependencies from the CLI init
- [x] 4.2 Check for and resolve any `radix-vue` version warnings in npm output
- [x] 4.3 Run `npm run build` and confirm no CSS purge warnings and no build errors

## 5. Test

- [x] 5.1 Run `npm test` — confirm existing Vitest tests still pass
- [ ] 5.2 (Manual) Open the app in browser, navigate to Liability Detail page, confirm Card/Button render correctly
- [ ] 5.3 (Manual) Confirm no visual regressions on at least 3 other pages (Dashboard, Runway Analysis, Property Calculator)
