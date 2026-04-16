# FlowCopilot Shell UI Design

Date: 2026-04-16
Status: Written, awaiting user spec review
Scope: Frontend shell only

## Goal

Redesign the global UI shell of FlowCopilot so the product feels like a professional operator workspace instead of a tab-heavy admin panel. The shell must unify chat, workflow, and knowledge base pages under one visual and structural system without redesigning each business module from scratch in this phase.

## Problem Summary

The current frontend shell has three structural problems:

1. The left area behaves like a management sidebar with tabs instead of a stable product navigation layer.
2. The main workspace is weak, so pages feel like separate demos rather than one product.
3. Detail, status, and secondary context are mixed into default layouts instead of appearing on demand.

The result is inconsistent hierarchy, weak focus on the primary task, and a stronger Ant Design default feel than a deliberate product shell.

## Approved Direction

The shell direction is fixed to the following choices:

- Product tone: professional operator workspace
- Shell structure: compact multi-pane workspace
- Right side behavior: on-demand detail panel, not permanently visible
- Left navigation style: icon plus short label
- Theme: light primary shell
- Information density: balanced
- Layout variant: balanced command workspace

## Design Principles

1. Primary work first. The main canvas always has priority over secondary context.
2. Global navigation is for module switching only. It must not hold object-level lists such as chat sessions or knowledge base entries.
3. Secondary context appears only when requested. Status, properties, logs, approvals, references, and metadata belong in drawers or contextual side panels.
4. Hierarchy comes from spacing, borders, and typography before decoration.
5. The shell must feel coherent across chat, workflow, and knowledge base pages even when those pages have different internal behaviors.

## Shell Architecture

The shell uses four persistent layers:

1. Left navigation rail
2. Top command bar
3. Main content canvas
4. Optional right detail panel

### Left Navigation Rail

Purpose:

- Switch between top-level modules
- Show current active module clearly
- Keep product identity visible

Behavior:

- Fixed narrow width
- Each item shows an icon and a short label
- Active state uses multiple signals: tint, border, icon emphasis, and label emphasis
- No tabbed module switching inside the rail

Recommended top-level entries:

- Chat
- Workflow
- Knowledge Base
- Settings as a lower-priority utility destination

The rail must answer only: "Which module am I in?"

### Top Command Bar

Purpose:

- Show current page title and context
- Host page-level search, filter entry points, and the primary action
- Provide a stable page header pattern shared across modules

Behavior:

- Compact horizontal control band
- Light surface with subtle separation from canvas
- Contains title and context copy on the left
- Contains search/filter entry and one primary action on the right
- May include contextual switches for a page, but not module navigation tabs

The command bar must answer only: "What page is this and what is the main thing I can do here?"

### Main Content Canvas

Purpose:

- Hold the primary working surface for the current module
- Stay visually dominant

Behavior:

- Light surface inside a larger light shell
- Uses single-column or two-zone composition depending on page needs
- Avoids dense card mosaics as a default pattern
- Uses containers sparingly and prefers larger grouped surfaces

The canvas must answer only: "What am I working on right now?"

### Right Detail Panel

Purpose:

- Reveal secondary context without permanently shrinking the workspace

Allowed content:

- Workflow trace and node detail
- Approval state
- Logs and runtime status
- References and sources
- Document metadata
- Item properties or detail previews

Behavior:

- Closed by default
- Opened from explicit user action or contextual selection
- Reclaims width when closed
- Slides in as an on-demand drawer or contextual panel

The detail panel must answer only: "What extra context do I need right now?"

## Navigation And Page Composition

### Global Rule

Global navigation owns module switching. Each page owns its own lists, object selection, filters, and secondary context.

This means the shell stops carrying business-specific collection UIs that belong inside pages.

### Chat Page

Shell behavior:

- Uses the standard top command bar
- Prioritizes the conversation surface
- Session list moves into the page as a secondary panel, sheet, or contextual list
- Right detail panel can later hold agent detail, retrieval references, or run status if needed

Outcome:

- Chat feels like a focused workspace instead of a blank page plus management sidebar

### Workflow Page

Shell behavior:

- Uses the full shell most aggressively
- Main area holds the workflow workspace and active execution view
- Right detail panel is the natural home for trace, approval, node snapshot, runtime detail, and logs

Outcome:

- Workflow becomes the flagship workspace page and best expression of the shell system

### Knowledge Base Page

Shell behavior:

- Uses the command bar for switching, uploading, filtering, and main actions
- Main area holds the document list and the current working surface
- Right detail panel is optional and only opens for metadata or item detail

Outcome:

- Knowledge base becomes operational and structured instead of stacked generic cards

## Visual Language

### Tone

The visual direction is a restrained Swiss-influenced enterprise workspace:

- light primary shell
- dark typography
- blue used as the structural emphasis color
- balanced density
- strong borders and spacing discipline
- minimal decorative gradients

This is not a marketing hero interface and not a dark cyber dashboard.

### Base Color Roles

- Canvas background: `#F8FAFC`
- Surface background: `#FFFFFF`
- Primary text: `#0F172A`
- Structural accent and active states: `#2563EB`
- Success status: `#22C55E`
- Neutral borders and dividers: slate 200 range

Color must never be the only way state is expressed.

### Surface Rules

- Use 1px borders and surface contrast before relying on shadows
- Use rounded containers, but not oversized soft-card aesthetics
- Keep shadows restrained and secondary
- Use subtle glass treatment only for the command bar or elevated controls where needed

### Typography Rules

- Clear page title hierarchy in the command bar
- Short supporting copy only where it helps orientation
- Avoid large marketing-style statements in product pages
- Long labels and names must truncate gracefully

## Interaction Rules

### Active And Status States

All active and status states must use more than color:

- active: tint plus border plus icon or label emphasis
- error: message plus icon or label plus alert styling
- success: text or icon plus color
- warning: visible text and contextual explanation

### Motion

Allowed motion:

- hover feedback
- detail drawer entrance and exit
- list and panel transitions

Motion constraints:

- 150-220ms preferred
- no layout-jumping scale effects
- no ornamental motion without functional value
- reduced-motion preferences should be respected

### Responsive Behavior

On smaller screens, collapse in this order:

1. Hide or collapse the right detail panel first
2. Compress or simplify command bar actions
3. Move page-internal secondary panels to drawers or sheets
4. Reduce shell padding only after the previous steps

The mobile shell must keep the same semantics, but it must not force a desktop multi-pane layout into a narrow screen.

## Accessibility Rules

The shell implementation must include:

- clear keyboard focus states
- keyboard reachable controls for all shell actions
- logical tab order aligned with visual order
- skip-to-main-content support because the shell is nav-heavy
- sequential heading structure
- high-contrast text in light mode

## Out Of Scope

This design does not yet redefine:

- message bubble styling in depth
- workflow information architecture inside each module
- knowledge base table schema
- agent creation and modal content strategy
- backend or API changes

Those belong to later page-level or module-level design work.

## Implementation Guardrails

When implementing this shell:

1. Replace the current tab-driven sidebar with a true navigation rail.
2. Introduce a reusable shell header or command bar component.
3. Create a consistent page container API so chat, workflow, and knowledge base pages plug into the same shell frame.
4. Add a reusable right-side detail drawer pattern instead of page-specific ad hoc side columns.
5. Avoid default Ant Design visual styling where it conflicts with the shell language. Ant Design can remain as a behavior layer, but the shell should not look like stock Ant Design.

## Success Criteria

The shell redesign is successful when:

- the app immediately reads as one product, not several unrelated screens
- module switching is obvious and stable
- the main workspace is always the visual priority
- secondary context no longer permanently crowds the layout
- chat, workflow, and knowledge base pages feel related without becoming identical
