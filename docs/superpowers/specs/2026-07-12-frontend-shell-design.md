# Frontend Shell Design

## Scope

Build a runnable Vue 3 management-console shell before implementing individual business flows. The shell gives every documented business area a stable route, navigation entry, page title, action area, filter area, and informative empty state.

## Layout

The application uses an Element Plus desktop layout: a fixed left navigation rail, a top header with breadcrumb, notification entry, and account menu, and a scrollable main content area. The collapsed navigation state is retained in the browser. On narrow screens the rail becomes an overlay drawer so routes remain usable.

## Route Structure

Routes follow the documented domains: dashboard; organization and personnel; attendance; goals and performance; workflow; reports; and system management. Each route receives title and navigation metadata. Route groups are visible by default during the framework stage; later a permission-aware navigation store will hide unauthorized items after the authenticated menu API is available.

## Reusable Page Frame

`PageHeader` renders the title, supporting context, and primary command slot. `PageState` renders loading, empty, and unavailable states without nesting cards. Placeholder domain views use the same frame with a concise table or metrics surface, so users can inspect complete information architecture before API integration.

## Visual System

The shell uses a neutral white/gray operational surface with a restrained blue action color, compact desktop typography, 8px control radii, visible keyboard focus, and Element Plus icons. Navigation, buttons, and panels keep stable dimensions; no decorative gradients or oversized marketing composition are used.

## Verification

Run `npm.cmd run typecheck` and `npm.cmd run build`. Start Vite and inspect the dashboard plus an organization, attendance, performance, workflow, and system route at desktop and narrow widths.
