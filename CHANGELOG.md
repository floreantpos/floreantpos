# Floreant POS 2.0 Beta — Release Notes

**Release:** 2.0 Beta (build 2001)
**Base revision:** r1960
**Date:** 2026-05-22

This beta release is a major UI and platform refresh. The order-taking screen, back-office, payment flow, and dependency stack have all been updated. Below is a feature-by-feature summary of what's new.

---

## Order Screen

### New Look & Feel
- **Card-style menu item buttons** — Square POS-inspired tiles with solid colored background (sourced from back-office button setting), soft three-layer drop shadow, food image filling the upper area with rounded corners, and a white label band with bolded item name and price at the bottom.
- **Square grid layout** — items render as true squares in 5–9 columns with a uniform 10 px gap, resizing fluidly with the window.
- **Removed boxed `TitledBorder`** from all order-screen panels (Category, Group, Menu Item, Ticket views). Replaced with subtle 2 px empty padding so panels read as a unified screen instead of separated boxes.
- **Consistent action button styling** — every action button (Customer, Cook. Inst., Misc Item, Hold Order, Send Kitchen, Cancel Order) now uses the new card-flat style with a left-aligned icon and bold label.

### Ticket View Window Improvements
- **Total button moved to the action row** — sits on the left of the bottom action bar, immediately under the ticket items panel, with its width tracking the ticket panel column so it visually belongs to that side.
- **Live total** — the Total button shows the running ticket amount in real time as items are added or removed, on every order type (not just retail).
- **Amber highlight when active** — the Total button turns amber (#FFB800) when the cart has items, returns to neutral when empty.
- **Action row compressed 20 %** — slimmer bottom row leaves more vertical space for the menu grid.

### Order Type Icons
- New consistent icon set: Dine In, Take Out, Delivery, Bar Tab, Drive Thru, Catering, Retail, Online.

---

## Back Office

### Cleaned-up Navigation
- **Sidebar redesigned** as `BackOfficeSidebarPanel`:
  - Responsive width (220–300 px), scales with screen.
  - Collapsible to icon-only mode (56 px) via a single toggle.
  - Accordion sections with auto-selected header icons per section.
  - Hover highlight and a left-edge accent bar on the active item.
  - Section headers grouped logically (Settings, Menu Management, Reports, etc.).

### Multi-Tab Toggle
- New **"Enable Multiple Tabs"** switch on the back-office status bar:
  - **Off** (default) — clicking a sidebar item closes other tabs first, so only one explorer is open at a time. Cleaner for small screens.
  - **On** — tabs accumulate side by side (the classic behavior).
- Toggle uses a new pill-shaped `ToggleSwitchButton` (iOS / Material style).

### Accent Color Button Mode
- New **Button Style** configuration screen lets the operator switch the whole UI between two presentation modes:
  - **Classic** — traditional flat buttons (default).
  - **Accent** — buttons render with a configurable accent color and optional category icon.
- **12 preset accent colors:** Red, Orange, Amber, Sky Blue, Green, Lime, Cyan, Pink, Brown, Yellow, Ochre, Violet.
- **Per-item accent override** via the new `ItemAccentDialog` so individual menu items can have a distinct color regardless of category default.
- **Material-style icon rendering** via the new `MaterialIconPainter` for crisp, scalable category icons.

### Explorer Grid Improvements
- **Row padding** — text/date cells in all explorers now render with 6 px top/bottom padding (4 px on left/right). Row height auto-rises to 34 px minimum so labels are readable instead of cramped. Applies to Menu Group, Menu Category, Menu Item, Modifier, Multiplier, Order Type, Pizza Item, Pizza Modifier explorers and the Ticket explorer.
- **Alternating row colors** — odd rows tinted with the L&F's alternate-row color for easier scanning.
- **Uniform action buttons** — Add / Edit / Delete / Change-Menu-Category buttons now share consistent padding via `ExplorerButtonPanel.createButton()`.

### Menu Import / Export
- **`MenuJsonExportAction`** — export the full menu (categories, groups, items, modifiers) to a JSON file for backup or transfer between terminals.
- **`MenuJsonImportAction`** — import a previously exported JSON menu, with options for merge or replace.

---

## Payment / Credit Card Processing

### Authorize.Net
- **Updated and tested** end-to-end against the Authorize.Net Sandbox.
- Configuration view now clearly shows supported regions: **United States, Canada, United Kingdom, Australia.**
- Sandbox mode toggle in card configuration with separate sandbox credentials.

### New Card Entry Dialog
- **Unified `CardEntryDialog`** with three tabs:
  - **Keyed Entry** — manual card-number / expiry / CVV input with format validation.
  - **Swipe Card** — magnetic-stripe reader input (when swipe is configured for the terminal).
  - **Auth Code** — entry of a pre-authorized code from an external terminal (when external terminal is configured).
- **Test mode helpers** — when sandbox mode is active, a **"Try Dummy Test Card"** button auto-fills a randomly chosen valid sandbox card so cashiers can verify the flow without typing real numbers; a **"Test Mode Active"** banner is shown at the top of the dialog so it can't be mistaken for production.
- **Card network detection** — the dialog highlights Visa, Mastercard, Amex, or Discover as the card number is entered.

---

## Training Mode

- **Restart-based switch.** Toggling Training mode now triggers a full application restart instead of a live database hot-swap, which was fragile and could leave Hibernate in an inconsistent state.
- **New `TrainingMode` helper:**
  - `savePreference(boolean)` persists the choice to `floreantpos.config.properties`.
  - `loadAndApply()` reads the preference and configures the JDBC connect string **before** the database is initialized, so the orange "Training Mode" banner is visible from the first frame and the correct database is in use from the very first connection.
- **Restaurant-name guard** — when Training mode is active, `Restaurant.getName()` returns "Training Restaurant", so receipts, kitchen prints, and reports cannot be mistaken for live data.
- **Login screen** — the Training checkbox now prompts a "Restart Required" confirmation before saving; cancel reverts the checkbox.

---

## License Dialog

- White-background scroll pane so HTML license content renders consistently regardless of Look & Feel theme.
- Outbound buttons updated with tracking parameter (`?ref=floreantpos2`):
  - Get ORO POS Trial → `pos.orocube.com`
  - Why Upgrade? → `pos.orocube.com/oro-pos-vs-floreant-pos/`
  - Floreant Plugins → `shop.orocube.com/floreant/`
  - Need Card Reader? → `pos.orocube.com/payments/`

---

## Logging / Security

- **Log4j 1.x removed entirely.** Although Floreant only used log4j 1.x (which was **not** affected by Log4Shell / CVE-2021-44228), some security scanners flagged its mere presence as a false positive. We've replaced it with:
  - `log4j-over-slf4j` **2.0.13** — a drop-in bridge that routes legacy log4j 1.x API calls into SLF4J.
  - `logback-classic` **1.3.14** — modern logging backend.
- **New `resources/logback.xml`** for runtime logger configuration.
- Net effect: no behavioral change for existing code, but security scans will no longer raise log4j-related false positives.

---

## New Source Files Added

| File | Purpose |
|------|---------|
| `src/com/floreantpos/main/TrainingMode.java` | Training-mode preference + startup apply. |
| `src/com/floreantpos/bo/ui/BackOfficeSidebarPanel.java` | Responsive collapsible back-office sidebar. |
| `src/com/floreantpos/swing/CardPosButtonUI.java` | Card-style ButtonUI with shadow + hover/press states. |
| `src/com/floreantpos/swing/ToggleSwitchButton.java` | Pill-shaped iOS/Material toggle component. |
| `src/com/floreantpos/swing/ButtonStyleConfig.java` | Button style preset + accent color registry. |
| `src/com/floreantpos/swing/ButtonStyleDialog.java` | Operator-facing button-style chooser dialog. |
| `src/com/floreantpos/swing/ItemAccentDialog.java` | Per-item accent color picker. |
| `src/com/floreantpos/swing/MaterialIconPainter.java` | Material-style icon renderer. |
| `src/com/floreantpos/config/ui/ButtonStyleConfigurationView.java` | Back-office Button Style configuration view. |
| `src/com/floreantpos/bo/actions/EditButtonStyleAction.java` | Back-office action — open Button Style editor. |
| `src/com/floreantpos/bo/actions/MenuJsonExportAction.java` | Menu → JSON export action. |
| `src/com/floreantpos/bo/actions/MenuJsonImportAction.java` | JSON → menu import action. |
| `src/com/floreantpos/ui/views/payment/CardEntryDialog.java` | Unified Keyed / Swipe / Auth-Code card dialog. |
| `resources/logback.xml` | Logback runtime configuration. |

Plus a new icon set under `config/ui_icons/` (`btn_*.png` for action buttons; `ordertype_*.png` for order-type chips).

---

## Bug Fixes

- **Pay Total navigated to Table Selection** instead of the Payment screen on retail tickets. Wiring corrected to call `doPayNow()` rather than `doFinishOrder()`.
- **"Cancel" → "Cancel Order"** label clarified.
- **Total button icon** standardized to `settle_ticket.png` so all action-row icons share the same flat style.
- **Multi-Tab toggle ancestor lookup** — sidebar item rows now resolve the parent `BackOfficeWindow` via `SwingUtilities.getAncestorOfClass`, fixing a race where the static instance could be null on first click.

---

## Known Limitations (Beta)

- The Authorize.Net integration is tested only against the Sandbox endpoint. Live-mode behavior should be re-verified against a real merchant account before production use.
- The Multi-Tab toggle preserves its setting in memory only; it resets to OFF on restart. Persistence will land in a follow-up.
- The Button Style "Accent" mode is finalized for the order screen; coverage for kitchen display and report views is in progress.

---

## Build & Run

```bash
mvn package -DskipTests
cd target/floreant-2.0
java -jar floreantpos.jar
```

JDK 8 or newer; Maven 3.6+; tested on Windows 11 and Ubuntu 22.04.
