# Floreant POS 2.0 Beta — Release Notes

**Release:** 2.0 Beta (build 2001)
**Base revision:** r1960  
**Date:** 2026-05-23

This beta release is a major UI and platform refresh covering the order screen, back office, a brand-new Cash & Card Management module, drawer status workflow, dashboard charts, payment flow improvements, and dependency stack upgrades.

---

## Order Screen

### New Look & Feel
- **Card-style menu item buttons** — Square POS-inspired tiles with solid colored background, soft three-layer drop shadow, food image with rounded corners, and a white label band showing item name and price.
- **Square grid layout** — items render as true squares in 5–9 columns with a uniform 10 px gap, resizing fluidly with the window.
- **Removed `TitledBorder`** from all order-screen panels (Category, Group, Menu Item, Ticket). Replaced with 2 px empty padding for a unified look.
- **Consistent action button styling** — Customer, Cook. Inst., Misc Item, Hold Order, Send Kitchen, Cancel Order all share the card-flat style with a left-aligned icon.

### Total Button
- Moved into the bottom action row with width tracking the ticket panel column.
- **Dark orange active state** (#D95B00) with **white text** when cart has items.
- Subtle hover tint that stays orange (no yellow shift).
- Custom flat dollar-sign icon matching the other action button icons.
- Live amount updates on every item add/remove for all order types.
- Action row height reduced to keep UI compact.

### Bug Fixes
- Pay Total button was navigating to Table Selection instead of the Payment screen — fixed.
- Sales report columns showing raw `16.75999…` values — all money columns now formatted to 2 decimal places.

---

## Cash & Card Management

New dialog accessible from the **cash register icon** in the top header bar and from the Other Functions screen. Contains five tabs:

### Dashboard
- **4 KPI cards** — Today's Gross Revenue, Cash in Drawer, Tickets, Charged Tips (aggregated across all today's drawer sessions).
- **Daily Trend chart** — Accountable vs Actual line chart for the current month, one data point per closed drawer session per day.
- **Hourly Sales chart** — bar chart of today's ticket revenue by hour of day (12 AM–11 PM).
- **Card vs Cash pie chart** — donut chart of today's cash vs card payment split.
- **Sales Summary visual** — stacked bars showing Net Sales / Tax / Tips proportions with cash drawer position.
- **Latest Items Sold** — colored bar per item, sourced from today's ticket items ordered by time descending.
- All charts use Arial font with explicit white background (no theme-font or dark-theme inheritance).

### Shifts & Cash Registers  *(Manager permission required)*
- Grid showing every closed drawer session in the selected period.
- **Today / This Month** filter toggle.
- Columns: Status (✓ / –), Date with time, Shift name (derived from Shift table), Opener, Tot. Accountable, Actual.
- **Actual column editable** — select a row, click **Edit Actual Amount** to enter the counted cash via a numeric keypad dialog; saves to `DrawerPullReport.cashToDeposit`.
- **Show Detail** button — pops a modal with the full breakdown for the selected session.
- Live open-drawer row shown at the top with status "Open".
- Grid auto-refreshes every 2 minutes; manual ⟳ Refresh button.

### Drawer Status
- Shows **Drawer is Open** or **Drawer is Closed** state.
- **Open state**: live drawer-pull report (Net Sales, Tax, Tips, Cash/Card receipts, Drawer Accountable, Cash to Deposit).
- **Closed state**: "Drawer is Closed" message only — no stale numbers.
- Action buttons change by state: Open Drawer (closed) ↔ Close Drawer + Drawer Bleed + Payout + Print (open).
- **Drawer Bleed / Payout guard**: amount cannot exceed current drawer balance — shows "Cash withdrawal cannot be more than cash drawer balance."
- **Print button**: prints the current drawer pull report to the configured printer.

### Card Transactions
- Embeds the existing Authorise / Settle / Void / Refund / Batch-Close flow directly inside the tab.

### Tips Payment
- Embedded criteria form: From date, To date, Server dropdown — all baseline-aligned.
- **OK** loads the server tips cashout report inline (summary HTML + detail table).
- **Pay Tips** (green) marks all unpaid tips for the selected server as paid and deducts from drawer balance.

---

## Cash Drawer Workflow

### Drawer Status Dialog (standalone)
- Full-screen modal with dimmed backdrop replaces the old Yes/No nag.
- **Closed state**: user dropdown (clocked-in users with Drawer Assignment permission), opening amount, numeric keypad, Open Drawer button.
- **Open state**: Opened-at time, cashier, opening balance, current balance, Dismiss + Close Drawer buttons.
- Close Drawer generates a full drawer-pull report and prints it.
- **Drawer Shift label**: stamped at open time as `<ShiftName> <serial>` (e.g., "Morning 02"), derived from the Shift table and a count of today's prior sessions.
- Shift label stored in `TERMINAL_PROPERTIES` — no schema change.

---

## Back Office

### Sidebar Navigation
- Section header fonts now **Bold 14pt Black** — previously faded MUTED gray.
- Headers remain black in both collapsed and expanded states.

### Explorers — Master-Detail Split Layout
- **Order Types**: top half grid, bottom half inline edit form with three sections (Identity & Ticket Setup, Workflow & Payment, Display Options). Click to select, edit, Save/Reset/New.
- **Currencies**: enlarged dialog (80% of parent), split grid + form with properly aligned 34 px-height inputs (Name 260 px, Code 120 px, Symbol/Rate 100–140 px).
- **User Types**: split grid + 3-column permission grid (Ticket Permissions | Admin & Manager Permissions | Other Permissions). Select-all / Select-none per column.

### Configuration Dialog
- Dialog enlarged 50% (1024×700 → 1536×1050).
- **Terminal Configuration**: two-column grouped layout (Terminal Identity + Display & UI left; Session & Ordering Behavior + Maintenance right). Cash Drawer section removed — managed via Peripheral tab. Barcode/Kitchen-print options moved to Print tab.

### Other Functions Screen
- Trimmed to **Back Office** and **Kitchen Display** only.
- All cash/drawer/tips/card-txn functions moved to the new Cash & Card Management dialog.

---

## Logging / Security

- **Log4j 1.x removed** — replaced by `log4j-over-slf4j` 2.0.13 + `logback-classic` 1.3.14.  
  No behavioral change; scanner false-positives on log4j are eliminated.

---

## Payment / Credit Card

- **Authorize.Net** — updated and tested against Sandbox; supports US, Canada, UK, Australia.
- **New Card Entry Dialog** — three tabs (Keyed / Swipe / Auth Code) with network detection and "Try Dummy Test Card" button in sandbox mode.

---

## Training Mode

- Toggle now triggers a full restart instead of a live hot-swap.
- `Restaurant.getName()` returns "Training Restaurant" when training mode is active.
- Login-screen checkbox shows a "Restart Required" confirmation.

---

## UI & Theming

- **Button Style configuration** — choose Classic or Accent with 12 preset colors and per-item accent override via `ItemAccentDialog`.
- **Material icon painter** — all 30 food category icon keys now resolve to SVG paths (aliases cover pizza → local_pizza, burger → lunch_dining, etc.).
- **Footer**: updated to "A Product of OROCUBE LLC · Since 2008".
- **License Dialog**: white-background scroll pane, tracking URLs with `?ref=floreantpos2`.

---

## New Source Files

| File | Purpose |
|------|---------|
| `ui/dialog/DrawerStatusDialog.java` | Full-screen drawer open/close modal with numeric keypad |
| `ui/dialog/CashCardManagementDialog.java` | Five-tab Cash & Card Management dialog |
| `main/TrainingMode.java` | Training-mode preference persistence |
| `bo/ui/BackOfficeSidebarPanel.java` | Responsive collapsible back-office sidebar |
| `swing/CardPosButtonUI.java` | Card-style ButtonUI with shadow and hover states |
| `swing/ToggleSwitchButton.java` | iOS-style pill toggle component |
| `swing/ButtonStyleConfig.java` | Button style preset and accent color registry |
| `swing/ButtonStyleDialog.java` | Operator-facing button-style chooser |
| `swing/ItemAccentDialog.java` | Per-item accent color picker |
| `swing/MaterialIconPainter.java` | Google Material Symbols SVG renderer |
| `config/ui/ButtonStyleConfigurationView.java` | Back-office button style configuration |
| `bo/actions/EditButtonStyleAction.java` | Open button style editor action |
| `bo/actions/MenuJsonExportAction.java` | Export menu to JSON |
| `bo/actions/MenuJsonImportAction.java` | Import menu from JSON |
| `ui/views/payment/CardEntryDialog.java` | Unified card-entry dialog (keyed/swipe/auth-code) |
| `resources/logback.xml` | Logback runtime configuration |

---

## Build & Run

```bash
mvn package -DskipTests
cd target/floreant-2.0
java -jar floreantpos.jar
```

JDK 8 or newer · Maven 3.6+ · Tested on Windows 11 and Ubuntu 22.04.

---

## Known Limitations (Beta)

- Authorize.Net tested against Sandbox only — verify against live credentials before production.
- Multi-Tab toggle in Back Office resets to OFF on restart (in-memory only).
- Sales Balance Report — "Previous Sale (Before Drawer Reset)" option may show an error if no drawer-pull data exists for the selected period; stack trace visible in `output.log`.
- Button Style Accent mode finalized for order screen; kitchen display and report views planned for next release.
