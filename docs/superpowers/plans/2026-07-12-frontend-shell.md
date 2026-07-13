# Frontend Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a navigable Element Plus administration shell for every documented HRPM domain.

**Architecture:** Route metadata is the single source for navigation groups, page titles, and breadcrumbs. `AppShell` owns responsive navigation and header behavior; `PageFrame` gives all domain views stable title, command, filter, and content regions; domain views remain small static surfaces until API integration.

**Tech Stack:** Vue 3, TypeScript, Vue Router, Pinia, Element Plus, Vite.

---

### Task 1: Register Element Plus And Route Metadata

**Files:**
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/router/index.ts`
- Create: `frontend/src/router/navigation.ts`
- Create: `frontend/src/views/DomainView.vue`

- [ ] **Step 1: Define navigation metadata.** Export groups for Dashboard, Organization, Attendance, Goals and Performance, Workflow, Reports, and System. Each item includes `path`, `title`, `group`, and Element Plus icon name.
- [ ] **Step 2: Register Element Plus.**

```ts
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
```

- [ ] **Step 3: Build routes from metadata.** Keep a specific `DashboardView`; map all other metadata items to `DomainView`; add a catch-all redirect to dashboard.
- [ ] **Step 4: Run `npm.cmd run typecheck`.** Expected: exit code 0.

### Task 2: Build Responsive Application Shell

**Files:**
- Modify: `frontend/src/App.vue`
- Create: `frontend/src/components/layout/AppShell.vue`
- Create: `frontend/src/components/layout/SidebarNavigation.vue`
- Create: `frontend/src/components/layout/TopHeader.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Replace the bare router outlet with `AppShell`.**
- [ ] **Step 2: Render a collapsed-capable Element Plus aside.** Use `navigationGroups` to build menus, keep the selected route synchronized with Vue Router, and expose an accessible collapse button.
- [ ] **Step 3: Render header breadcrumb and account controls.** Breadcrumb derives from route metadata; header contains notifications and account menu icons with text labels/tooltips.
- [ ] **Step 4: Add responsive CSS.** Keep 240px desktop aside and switch to a drawer-style overlay under 900px; preserve visible focus states and 8px radii.
- [ ] **Step 5: Run `npm.cmd run typecheck`.** Expected: exit code 0.

### Task 3: Create Page Frame And Domain Surfaces

**Files:**
- Create: `frontend/src/components/common/PageFrame.vue`
- Create: `frontend/src/components/common/EmptyState.vue`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `frontend/src/views/DomainView.vue`

- [ ] **Step 1: Implement `PageFrame`.** Provide named slots for title actions, filters, and default content. Render title and description from route metadata.
- [ ] **Step 2: Implement `EmptyState`.** Show domain-specific status with a compact icon, title, and supporting copy, without a nested card layout.
- [ ] **Step 3: Build the dashboard.** Show stable metric tiles for Pending Tasks, Leave Requests, Performance Tasks, and Notices, plus a concise pending-work table.
- [ ] **Step 4: Build generic domain pages.** Use the frame to show primary-action buttons, filter controls, column labels, and a domain-specific empty state.
- [ ] **Step 5: Run `npm.cmd run build`.** Expected: type-check and production bundle complete successfully.

### Task 4: Verify The Running Shell

**Files:**
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Run `npm.cmd run dev -- --host 127.0.0.1`.**
- [ ] **Step 2: Inspect dashboard, organization, attendance, performance, workflow, and system routes at desktop and narrow viewport widths.**
- [ ] **Step 3: Fix any overflow, route highlighting, or text-overlap defects found during inspection.**
- [ ] **Step 4: Re-run `npm.cmd run build`.** Expected: exit code 0.
