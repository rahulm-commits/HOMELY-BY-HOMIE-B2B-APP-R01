---
name: Field Operations & Wholesale Commerce
colors:
  surface: '#f1fcf4'
  surface-dim: '#d1ddd5'
  surface-bright: '#f1fcf4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#ebf6ee'
  surface-container: '#e5f1e8'
  surface-container-high: '#dfebe3'
  surface-container-highest: '#dae5dd'
  on-surface: '#141e19'
  on-surface-variant: '#3f4942'
  inverse-surface: '#28332d'
  inverse-on-surface: '#e8f4eb'
  outline: '#6f7a71'
  outline-variant: '#bfc9bf'
  surface-tint: '#186c45'
  primary: '#004529'
  on-primary: '#ffffff'
  primary-container: '#005f3a'
  on-primary-container: '#88d7a8'
  inverse-primary: '#88d7a8'
  secondary: '#006d43'
  on-secondary: '#ffffff'
  secondary-container: '#99f2bd'
  on-secondary-container: '#087147'
  tertiary: '#7e000b'
  on-tertiary: '#ffffff'
  tertiary-container: '#a80716'
  on-tertiary-container: '#ffb3ac'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#a4f4c3'
  primary-fixed-dim: '#88d7a8'
  on-primary-fixed: '#002111'
  on-primary-fixed-variant: '#005231'
  secondary-fixed: '#9bf5c0'
  secondary-fixed-dim: '#80d9a5'
  on-secondary-fixed: '#002111'
  on-secondary-fixed-variant: '#005231'
  tertiary-fixed: '#ffdad6'
  tertiary-fixed-dim: '#ffb3ac'
  on-tertiary-fixed: '#410003'
  on-tertiary-fixed-variant: '#93000f'
  background: '#f1fcf4'
  on-background: '#141e19'
  surface-variant: '#dae5dd'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.005em
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.03em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1.5rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

The design system is engineered for high-velocity B2B wholesale grocery operations, field sales logistics, and inventory fulfillment. It embodies a reliable, practical, and highly systematic aesthetic tailored for field managers, warehouse operators, and store procurement leads. 

The visual style blends **Modern Corporate** clarity with **High-Density Utility**:
- **Clarity under pressure:** High scanability, rapid numerical comprehension, and tactile visual cues built for fast order entry and SKU reconciliation on both desktop consoles and mobile devices.
- **Practical Trust:** Grounded deep botanical greens denote agricultural freshness and fiscal reliability, balanced against light neutral substrates that maintain eye comfort during 8-hour operational shifts.
- **Controlled Density:** Maximized screen real estate with precise line heights, compact vertical paddings, and explicit grid structures without feeling cluttered.

## Colors

The palette establishes strict hierarchical clarity to streamline repetitive field workflows:

- **Primary (`#18794E`):** Directs the primary action path—order submission, balance settlement, primary tabs, and active state indicators.
- **Secondary (`#E8F5EC`):** Acts as a structural tint for selected states, batch status tags, table row highlights, and soft badge backgrounds.
- **Tertiary / Destructive (`#D32F2F`):** Dedicated to stockout warnings, overdue credit terms, validation errors, and cancelled dispatch alerts.
- **Surfaces & Backgrounds:** Canvas background is pure white (`#FFFFFF`) to ensure maximum legibility for high-contrast tabular data. Canvas containers and neutral panels use `#F7F9F8` to clearly segment bulk data regions.
- **Borders & Rules:** Subtle structural containment via `#E1E7E3` delineates cards, table lines, and input bounds without inducing visual fatigue.
- **Text & Content:** Primary copy uses deep charcoal (`#202824`) for optimal contrast ratios compliant with AAA accessibility standards. Supporting attributes, metadata timestamps, and units of measure employ `#68736D`.

## Typography

The design system exclusively implements **Inter** across all structural tiers to maintain metric uniformity, dense numeric legibility, and tabular numeral alignment.

- **Numeric & Financial Display:** All tabular quantities (crate quantities, kilogram metrics, wholesale pricing, GST calculations) must employ OpenType tabular figures (`font-variant-numeric: tabular-nums`) to ensure vertical decimal alignment across enterprise tables.
- **Hierarchy:** Titles favor medium-to-bold weights with tight letter spacing for authority without excess height. Labels and micro-metadata utilize bold weights at `11px` and `12px` to guarantee legibility on sunlight-exposed mobile screens in wholesale yards.
- **Responsiveness:** Displays step down seamlessly on hand-held terminals via mobile-specific tokens (`headline-xl-mobile`, `headline-lg-mobile`), preventing awkward wrapping of merchant and shop titles.

## Layout & Spacing

The layout is built upon an 8pt base grid with a 4pt sub-grid specifically reserved for compact data rows, metadata tags, and inline badges.

- **Desktop (12 Columns):** Content conforms to a fluid 12-column grid with a `1.5rem` (24px) outer margin and `1rem` (16px) gutter. Density remains high to allow simultaneous viewing of multi-tier product catalogs and pending dispatch baskets.
- **Handheld / Tablet Reflow:** Mobile views collapse to a single or 2-column card stream with `1rem` (16px) margin and `0.75rem` (12px) gutter. Actions pin to fixed bottom sheets to enable single-handed field operation.
- **Data Spacing Rhythm:** Component interiors favor compact spacing (`space-sm` for badge interiors and metric chips; `space-md` for standard card padding; `space-lg` for card clusters).

## Elevation & Depth

To maximize clarity and reduce GPU overhead on rugged field devices, visual hierarchy prioritizes crisp borders and low-profile ambient shadows rather than heavy elevation tiers.

- **Base Layer (Surface Level 0):** Canvas `#FFFFFF` with structural panels tinted `#F7F9F8`.
- **Card & Cell Layer (Surface Level 1):** Flat `#FFFFFF` outlined with a 1px solid `#E1E7E3` border. Provides structural definition without elevation bleed.
- **Raised Interactive Layer (Surface Level 2):** Applied to active search popovers, dropdown filter trays, and floating summary drawers. Uses an ambient, diffused shadow: `0 4px 12px -2px rgba(32, 40, 36, 0.06), 0 2px 4px -1px rgba(32, 40, 36, 0.04)`.
- **Modal / Overlay Layer (Surface Level 3):** Modal sheets and dispatch confirmations use `0 12px 28px -4px rgba(32, 40, 36, 0.12), 0 4px 8px -2px rgba(32, 40, 36, 0.06)` alongside a 30% `#202824` backdrop veil.

## Shapes

The design system standardizes on precise, clean corners that maintain structural integrity while softening dense enterprise screens:

- **Standard Containers (`roundedness: 2` / `0.5rem` / 8px):** Applied to core content cards, data tables, order summary panels, and input fields.
- **Major Groupings (`rounded-xl` / `1.5rem` / 24px):** Used exclusively for high-level module containers, dashboard summaries, and mobile bottom sheet overlays.
- **Identifiers & Micro-Tags:** Badges, operational tags, and Shop IDs strictly diverge into continuous pill geometry (`rounded-full` / 9999px) to visually distinguish immutable metadata from actionable rectangular inputs.

## Components

### Buttons
- **Primary:** Background `#18794E`, text `#FFFFFF`, 8px border radius, height 40px (36px in compact table bars). Hover: `#136340`. Active: `#0F4F33`.
- **Secondary / Ghost:** Border 1px solid `#E1E7E3`, background `#FFFFFF`, text `#202824`. Hover: background `#F7F9F8`.
- **Destructive:** Border 1px solid `#D32F2F`, text `#D32F2F`, background transparent. Hover: background `#FFEBEE`.

### Badges & Shop IDs
- **Shop ID Pill Badges:** Displayed as a continuous pill (`border-radius: 9999px`). Background `#E8F5EC`, text `#18794E`, font `label-sm`, padding `2px 8px`. Monospace numeric sequence (e.g., `SHOP-9042`).
- **Status Badges:** Fulfillment tags (Delivered, Pending, Dispatched) utilize low-saturation tints with high-contrast text:
  - *Active/Dispatched:* `#E8F5EC` fill, `#18794E` text.
  - *Action Required/Overdue:* `#FDE8E8` fill, `#D32F2F` text.
  - *Draft:* `#F7F9F8` fill with 1px `#E1E7E3` border, `#68736D` text.

### Inputs & Metadata Fields
- **Phone Number Input (India):** Fixed `+91` flag prefix locked in `#F7F9F8` container with right-side separator `#E1E7E3`. Input field body uses `Inter`, 14px, formatted automatically with 5+5 digit space groupings (`98765 43210`).
- **Location Metadata Widget:** Integrated input combining latitude/longitude pins, sub-locality, and pincode tags. Sits on a 1px `#E1E7E3` border with an inline `#18794E` map pin icon and `#68736D` assistive helper text below.
- **States:** Default border `#E1E7E3`; focus border `#18794E` with a 2px `#E8F5EC` outer halo; error state replaces border with `#D32F2F` and renders an inline alert icon.

### Cards & Tables
- **High-Density Order Cards:** Outer container uses `rounded-xl` with 1px `#E1E7E3` border and `#FFFFFF` background. Header rows feature a fixed height of 44px with a subtle `#F7F9F8` background division containing the Shop ID pill, customer tier tag, and route allocation timestamp.
- **Catalog Line-Items:** Compact height (48px per line), inline steppers for crate and weight incrementation, and automatic real-time wholesale price updates with bold tabular numbers.

### Selection Controls
- **Checkboxes & Radios:** 18px box size with 4px corner radius for checkboxes, full circle for radios. Selected fill `#18794E` with white checkmark. Inactive state: 1.5px border in `#68736D`.