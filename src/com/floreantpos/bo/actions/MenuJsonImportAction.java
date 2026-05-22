package com.floreantpos.bo.actions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floreantpos.PosLog;
import com.floreantpos.model.MenuCategory;
import com.floreantpos.model.MenuGroup;
import com.floreantpos.model.MenuItem;
import com.floreantpos.model.MenuItemModifierGroup;
import com.floreantpos.model.MenuModifier;
import com.floreantpos.model.ModifierGroup;
import com.floreantpos.model.TaxGroup;
import com.floreantpos.model.dao.GenericDAO;
import com.floreantpos.model.dao.TaxGroupDAO;
import com.floreantpos.util.POSUtil;

public class MenuJsonImportAction extends AbstractAction {

    public MenuJsonImportAction() {
        super("Import Menu Items"); //$NON-NLS-1$
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json")); //$NON-NLS-1$

        if (chooser.showOpenDialog(POSUtil.getFocusedWindow()) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();

        // ── Step 1: Parse & validate JSON ────────────────────────────────
        JsonNode root;
        try {
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readTree(file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(POSUtil.getFocusedWindow(),
                "Invalid JSON file:\n" + ex.getMessage(), //$NON-NLS-1$
                "Import Failed", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return;
        }

        if (!root.has("categories")) { //$NON-NLS-1$
            JOptionPane.showMessageDialog(POSUtil.getFocusedWindow(),
                "This file does not appear to be a FloreantPOS menu export.\n" //$NON-NLS-1$
                + "Expected a 'categories' section but none was found.", //$NON-NLS-1$
                "Import Failed", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return;
        }

        // ── Step 2: Confirm ───────────────────────────────────────────────
        String metaInfo = buildMetaInfo(root);
        int confirm = JOptionPane.showConfirmDialog(POSUtil.getFocusedWindow(),
            "Import menu data from:\n  " + file.getName() + "\n\n" + metaInfo //$NON-NLS-1$
            + "\n\nExisting items with matching IDs will be imported as new entries.\nProceed?", //$NON-NLS-1$
            "Confirm Import", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE); //$NON-NLS-1$
        if (confirm != JOptionPane.YES_OPTION) return;

        // ── Step 3: Import ────────────────────────────────────────────────
        ImportStats stats = new ImportStats();
        try {
            runImport(root, stats);
        } catch (Exception ex) {
            PosLog.error(getClass(), ex.getMessage());
            stats.fatalError = ex.getMessage();
        }

        // ── Step 4: Summary dialog ────────────────────────────────────────
        showSummary(stats);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void runImport(JsonNode root, ImportStats stats) {
        GenericDAO dao = new GenericDAO();

        // Build tax group cache (name → entity) for linking items to tax groups
        Map<String, TaxGroup> taxGroupCache = new LinkedHashMap<String, TaxGroup>();
        try {
            List<TaxGroup> tgs = TaxGroupDAO.getInstance().findAll();
            for (TaxGroup tg : tgs) {
                if (tg.getName() != null) taxGroupCache.put(tg.getName().toLowerCase(), tg);
            }
        } catch (Exception ignored) {}

        // Maps: JSON "id" field → saved entity (for relationship wiring)
        Map<Integer, MenuCategory> catMap = new LinkedHashMap<Integer, MenuCategory>();
        Map<Integer, MenuGroup>    grpMap = new LinkedHashMap<Integer, MenuGroup>();
        Map<Integer, ModifierGroup> mgMap = new LinkedHashMap<Integer, ModifierGroup>();

        JsonNode cats = root.path("categories"); //$NON-NLS-1$

        // ── Phase 1: Categories ───────────────────────────────────────────
        stats.log("Importing categories..."); //$NON-NLS-1$
        for (JsonNode c : cats) {
            String name = c.path("name").asText("?"); //$NON-NLS-1$
            try {
                MenuCategory cat = new MenuCategory();
                cat.setName(name);
                cat.setButtonColorCode(parseColor(c, "button_color")); //$NON-NLS-1$
                cat.setTextColorCode(parseColor(c, "text_color")); //$NON-NLS-1$
                dao.save(cat);
                catMap.put(c.path("id").asInt(-1), cat); //$NON-NLS-1$
                stats.categoriesOk++;
            } catch (Exception ex) {
                stats.categoriesFail++;
                stats.log("  SKIP category '" + name + "': " + ex.getMessage()); //$NON-NLS-1$
            }
        }

        // ── Phase 2: Groups ───────────────────────────────────────────────
        stats.log("Importing groups..."); //$NON-NLS-1$
        for (JsonNode c : cats) {
            MenuCategory savedCat = catMap.get(c.path("id").asInt(-1)); //$NON-NLS-1$
            for (JsonNode g : c.path("groups")) { //$NON-NLS-1$
                String name = g.path("name").asText("?"); //$NON-NLS-1$
                try {
                    MenuGroup grp = new MenuGroup();
                    grp.setName(name);
                    grp.setParent(savedCat);
                    grp.setButtonColorCode(parseColor(g, "button_color")); //$NON-NLS-1$
                    grp.setTextColorCode(parseColor(g, "text_color")); //$NON-NLS-1$
                    dao.save(grp);
                    grpMap.put(g.path("id").asInt(-1), grp); //$NON-NLS-1$
                    stats.groupsOk++;
                } catch (Exception ex) {
                    stats.groupsFail++;
                    stats.log("  SKIP group '" + name + "': " + ex.getMessage()); //$NON-NLS-1$
                }
            }
        }

        // ── Phase 3: Collect unique modifier groups across all items ───────
        stats.log("Importing modifier groups and modifiers..."); //$NON-NLS-1$
        Map<Integer, JsonNode> uniqueMGs = new LinkedHashMap<Integer, JsonNode>();
        for (JsonNode c : cats)
            for (JsonNode g : c.path("groups")) //$NON-NLS-1$
                for (JsonNode item : g.path("items")) //$NON-NLS-1$
                    for (JsonNode mg : item.path("modifier_groups")) { //$NON-NLS-1$
                        int mgId = mg.path("id").asInt(-1); //$NON-NLS-1$
                        if (!uniqueMGs.containsKey(mgId)) uniqueMGs.put(mgId, mg);
                    }

        for (Map.Entry<Integer, JsonNode> entry : uniqueMGs.entrySet()) {
            int jsonMgId   = entry.getKey();
            JsonNode mgNode = entry.getValue();
            String mgName  = mgNode.path("name").asText("?"); //$NON-NLS-1$
            try {
                ModifierGroup mg = new ModifierGroup();
                mg.setName(mgName);
                dao.save(mg);
                mgMap.put(jsonMgId, mg);
                stats.modifierGroupsOk++;

                // ── Phase 4: Modifiers within this group ─────────────────
                for (JsonNode modNode : mgNode.path("modifiers")) { //$NON-NLS-1$
                    String modName = modNode.path("name").asText("?"); //$NON-NLS-1$
                    try {
                        MenuModifier mod = new MenuModifier();
                        mod.setName(modName);
                        mod.setModifierGroup(mg);
                        if (modNode.has("price"))       mod.setPrice(modNode.path("price").asDouble()); //$NON-NLS-1$
                        if (modNode.has("extra_price")) mod.setExtraPrice(modNode.path("extra_price").asDouble()); //$NON-NLS-1$
                        mod.setButtonColor(parseColor(modNode, "button_color")); //$NON-NLS-1$
                        mod.setTextColor(parseColor(modNode, "text_color")); //$NON-NLS-1$
                        dao.save(mod);
                        stats.modifiersOk++;
                    } catch (Exception ex) {
                        stats.modifiersFail++;
                        stats.log("  SKIP modifier '" + modName + "' in group '" + mgName + "': " + ex.getMessage()); //$NON-NLS-1$
                    }
                }
            } catch (Exception ex) {
                stats.modifierGroupsFail++;
                stats.log("  SKIP modifier group '" + mgName + "': " + ex.getMessage()); //$NON-NLS-1$
            }
        }

        // ── Phase 5 & 6: Items + modifier links ───────────────────────────
        stats.log("Importing items..."); //$NON-NLS-1$
        for (JsonNode c : cats) {
            for (JsonNode g : c.path("groups")) { //$NON-NLS-1$
                MenuGroup savedGrp = grpMap.get(g.path("id").asInt(-1)); //$NON-NLS-1$
                for (JsonNode itemNode : g.path("items")) { //$NON-NLS-1$
                    String itemName = itemNode.path("name").asText("?"); //$NON-NLS-1$
                    try {
                        MenuItem item = new MenuItem();
                        item.setName(itemName);
                        item.setParent(savedGrp);
                        if (itemNode.has("price"))     item.setPrice(itemNode.path("price").asDouble()); //$NON-NLS-1$
                        if (itemNode.has("buy_price")) item.setBuyPrice(itemNode.path("buy_price").asDouble()); //$NON-NLS-1$
                        item.setButtonColorCode(parseColor(itemNode, "button_color")); //$NON-NLS-1$
                        item.setTextColorCode(parseColor(itemNode, "text_color")); //$NON-NLS-1$

                        // Tax group by name
                        String tgName = itemNode.path("tax_group").asText(null); //$NON-NLS-1$
                        if (tgName != null && taxGroupCache.containsKey(tgName.toLowerCase())) {
                            item.setTaxGroup(taxGroupCache.get(tgName.toLowerCase()));
                        }

                        // Attach modifier groups
                        List<MenuItemModifierGroup> migs = new ArrayList<MenuItemModifierGroup>();
                        for (JsonNode mgRef : itemNode.path("modifier_groups")) { //$NON-NLS-1$
                            int mgId = mgRef.path("id").asInt(-1); //$NON-NLS-1$
                            ModifierGroup savedMg = mgMap.get(mgId);
                            if (savedMg != null) {
                                MenuItemModifierGroup mig = new MenuItemModifierGroup();
                                mig.setModifierGroup(savedMg);
                                migs.add(mig);
                            }
                        }
                        item.setMenuItemModiferGroups(migs);

                        dao.save(item);

                        // Save modifier links
                        for (MenuItemModifierGroup mig : migs) {
                            try { dao.save(mig); } catch (Exception ignored) {}
                        }

                        stats.itemsOk++;
                    } catch (Exception ex) {
                        stats.itemsFail++;
                        stats.log("  SKIP item '" + itemName + "': " + ex.getMessage()); //$NON-NLS-1$
                    }
                }
            }
        }

        stats.log("Done."); //$NON-NLS-1$
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Integer parseColor(JsonNode node, String key) {
        JsonNode val = node.path(key);
        if (val.isMissingNode() || val.isNull()) return null;
        String hex = val.asText("").trim(); //$NON-NLS-1$
        if (hex.startsWith("#") && hex.length() == 7) { //$NON-NLS-1$
            try {
                int r = Integer.parseInt(hex.substring(1, 3), 16);
                int g = Integer.parseInt(hex.substring(3, 5), 16);
                int b = Integer.parseInt(hex.substring(5, 7), 16);
                return new java.awt.Color(r, g, b).getRGB();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String buildMetaInfo(JsonNode root) {
        JsonNode meta = root.path("meta"); //$NON-NLS-1$
        if (meta.isMissingNode()) return ""; //$NON-NLS-1$
        StringBuilder sb = new StringBuilder();
        if (!meta.path("store_name").asText("").isEmpty()) //$NON-NLS-1$
            sb.append("Store: ").append(meta.path("store_name").asText()).append("\n"); //$NON-NLS-1$
        if (!meta.path("exported_at").asText("").isEmpty()) //$NON-NLS-1$
            sb.append("Exported: ").append(meta.path("exported_at").asText()).append("\n"); //$NON-NLS-1$
        if (!meta.path("pos_version").asText("").isEmpty()) //$NON-NLS-1$
            sb.append("Version: ").append(meta.path("pos_version").asText()); //$NON-NLS-1$
        return sb.toString();
    }

    private void showSummary(ImportStats s) {
        StringBuilder sb = new StringBuilder();
        if (s.fatalError != null) {
            sb.append("A fatal error stopped the import early:\n").append(s.fatalError).append("\n\n"); //$NON-NLS-1$
        }
        sb.append("Import Summary\n"); //$NON-NLS-1$
        sb.append("─────────────────────────────\n"); //$NON-NLS-1$
        sb.append(row("Categories",      s.categoriesOk,      s.categoriesFail));
        sb.append(row("Groups",          s.groupsOk,          s.groupsFail));
        sb.append(row("Modifier Groups", s.modifierGroupsOk,  s.modifierGroupsFail));
        sb.append(row("Modifiers",       s.modifiersOk,       s.modifiersFail));
        sb.append(row("Items",           s.itemsOk,           s.itemsFail));
        sb.append("─────────────────────────────\n"); //$NON-NLS-1$

        int totalFail = s.categoriesFail + s.groupsFail + s.modifierGroupsFail + s.modifiersFail + s.itemsFail;
        int totalOk   = s.categoriesOk   + s.groupsOk   + s.modifierGroupsOk   + s.modifiersOk   + s.itemsOk;
        sb.append(String.format("Total: %d imported, %d skipped%n", totalOk, totalFail)); //$NON-NLS-1$

        if (!s.messages.isEmpty()) {
            sb.append("\nDetails:\n"); //$NON-NLS-1$
            for (String msg : s.messages) sb.append("  ").append(msg).append("\n"); //$NON-NLS-1$
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)); //$NON-NLS-1$
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(500, 300));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(totalFail == 0 ? "Import completed successfully." : "Import completed with some issues."), BorderLayout.NORTH); //$NON-NLS-1$
        panel.add(scroll, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        int type = totalFail == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(POSUtil.getFocusedWindow(), panel, "Import Result", type); //$NON-NLS-1$
    }

    private String row(String label, int ok, int fail) {
        return String.format("  %-20s %3d imported,  %3d skipped%n", label + ":", ok, fail); //$NON-NLS-1$
    }

    // ── Stats tracker ──────────────────────────────────────────────────────

    private static class ImportStats {
        int  categoriesOk, categoriesFail;
        int  groupsOk, groupsFail;
        int  modifierGroupsOk, modifierGroupsFail;
        int  modifiersOk, modifiersFail;
        int  itemsOk, itemsFail;
        String fatalError;
        List<String> messages = new ArrayList<String>();
        void log(String msg) { messages.add(msg); }
    }
}
