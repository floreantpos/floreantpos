package com.floreantpos.bo.actions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.floreantpos.PosLog;
import com.floreantpos.main.Application;
import com.floreantpos.model.MenuCategory;
import com.floreantpos.model.MenuGroup;
import com.floreantpos.model.MenuItem;
import com.floreantpos.model.MenuItemModifierGroup;
import com.floreantpos.model.MenuModifier;
import com.floreantpos.model.ModifierGroup;
import com.floreantpos.model.Restaurant;
import com.floreantpos.model.Tax;
import com.floreantpos.model.dao.GenericDAO;
import com.floreantpos.model.dao.MenuCategoryDAO;
import com.floreantpos.model.dao.MenuGroupDAO;
import com.floreantpos.model.dao.MenuItemDAO;
import com.floreantpos.model.dao.TaxDAO;
import com.floreantpos.util.POSUtil;

public class MenuJsonExportAction extends AbstractAction {

    private static final String POS_VERSION = "floreantpos-1.5.1"; //$NON-NLS-1$
    private static final String FORMAT_VER  = "floreantpos-menu-v1"; //$NON-NLS-1$

    public MenuJsonExportAction() {
        super("Export Menu Items"); //$NON-NLS-1$
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // ── Format selection dialog ───────────────────────────────────────
        JRadioButton rbJson  = new JRadioButton("<html><b>JSON</b> — Full menu tree with colors and modifier groups</html>"); //$NON-NLS-1$
        JRadioButton rbXlsx  = new JRadioButton("<html><b>Spreadsheet (XLSX)</b> — Flat format compatible with POS import tools<br>" //$NON-NLS-1$
                                               + "<font color='#666666'>Items and Modifiers on separate sheets</font></html>"); //$NON-NLS-1$
        rbJson.setSelected(true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbJson); bg.add(rbXlsx);

        JPanel fmt = new JPanel(new GridLayout(2, 1, 0, 10));
        fmt.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        fmt.add(rbJson); fmt.add(rbXlsx);

        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.add(new JLabel("Select export format:"), BorderLayout.NORTH); //$NON-NLS-1$
        wrap.add(fmt, BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(POSUtil.getFocusedWindow(),
            wrap, "Export Menu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE); //$NON-NLS-1$
        if (choice != JOptionPane.OK_OPTION) return;

        boolean exportXlsx = rbXlsx.isSelected();

        // ── File chooser ──────────────────────────────────────────────────
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);

        String storePart = sanitize(getStoreName());
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()); //$NON-NLS-1$
        String ext = exportXlsx ? ".xlsx" : ".json"; //$NON-NLS-1$ //$NON-NLS-2$
        chooser.setFileFilter(exportXlsx
            ? new FileNameExtensionFilter("Excel Spreadsheet (*.xlsx)", "xlsx") //$NON-NLS-1$
            : new FileNameExtensionFilter("JSON Files (*.json)", "json")); //$NON-NLS-1$
        chooser.setSelectedFile(new File("floreantpos_" + timestamp + "_" + storePart + ext)); //$NON-NLS-1$

        if (chooser.showSaveDialog(POSUtil.getFocusedWindow()) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(ext))
            file = new File(file.getAbsolutePath() + ext);

        if (file.exists()) {
            int ok = JOptionPane.showConfirmDialog(POSUtil.getFocusedWindow(),
                "Overwrite file: " + file.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
            if (ok != JOptionPane.YES_OPTION) return;
        }

        try {
            if (exportXlsx) exportXlsx(file);
            else             exportJson(file);
            JOptionPane.showMessageDialog(POSUtil.getFocusedWindow(),
                "Menu exported successfully:\n" + file.getAbsolutePath(), //$NON-NLS-1$
                "Export Complete", JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$
        } catch (Exception ex) {
            PosLog.error(getClass(), ex.getMessage());
            JOptionPane.showMessageDialog(POSUtil.getFocusedWindow(),
                "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // JSON Export  (unchanged logic)
    // ══════════════════════════════════════════════════════════════════════

    private void exportJson(File file) throws Exception {
        Session session = null;
        try {
            session = new GenericDAO().createNewSession();
            List<Tax>          taxes      = TaxDAO.getInstance().findAll(session);
            List<MenuCategory> categories = MenuCategoryDAO.getInstance().findAll(session);
            List<MenuGroup>    groups     = MenuGroupDAO.getInstance().findAll(session);
            List<MenuItem>     items      = MenuItemDAO.getInstance().findAll(session);

            Map<Integer, List<MenuGroup>> groupsByCat = byParent(groups);
            Map<Integer, List<MenuItem>>  itemsByGrp  = byParentItem(items);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            ObjectNode root = mapper.createObjectNode();

            Restaurant r = safeRestaurant();
            String exportedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); //$NON-NLS-1$
            root.put("_info", buildInfoComment(r, exportedAt)); //$NON-NLS-1$

            ObjectNode meta = root.putObject("meta"); //$NON-NLS-1$
            meta.put("pos_version",   POS_VERSION); //$NON-NLS-1$
            meta.put("export_format", FORMAT_VER); //$NON-NLS-1$
            meta.put("exported_at",   exportedAt); //$NON-NLS-1$
            if (r != null) {
                meta.put("store_name",    safe(r.getName())); //$NON-NLS-1$
                meta.put("address_line1", safe(r.getAddressLine1())); //$NON-NLS-1$
                meta.put("address_line2", safe(r.getAddressLine2())); //$NON-NLS-1$
                meta.put("address_line3", safe(r.getAddressLine3())); //$NON-NLS-1$
                meta.put("zip_code",      safe(r.getZipCode())); //$NON-NLS-1$
            }
            meta.put("note", "Images are not included. Button and text colors are exported as hex values."); //$NON-NLS-1$

            ArrayNode taxArr = root.putArray("taxes"); //$NON-NLS-1$
            for (Tax tax : taxes) {
                ObjectNode t = taxArr.addObject();
                t.put("id", tax.getId()); t.put("name", safe(tax.getName())); //$NON-NLS-1$ //$NON-NLS-2$
                if (tax.getRate() != null) t.put("rate_percent", tax.getRate()); //$NON-NLS-1$
            }

            ArrayNode catArr = root.putArray("categories"); //$NON-NLS-1$
            for (MenuCategory cat : categories) {
                ObjectNode c = catArr.addObject();
                c.put("id", cat.getId()); c.put("name", safe(cat.getName())); //$NON-NLS-1$ //$NON-NLS-2$
                putColor(c, "button_color", cat.getButtonColorCode()); putColor(c, "text_color", cat.getTextColorCode()); //$NON-NLS-1$ //$NON-NLS-2$
                ArrayNode grpArr = c.putArray("groups"); //$NON-NLS-1$
                for (MenuGroup grp : safeList(groupsByCat.get(cat.getId()))) {
                    ObjectNode g = grpArr.addObject();
                    g.put("id", grp.getId()); g.put("name", safe(grp.getName())); //$NON-NLS-1$ //$NON-NLS-2$
                    putColor(g, "button_color", grp.getButtonColorCode()); putColor(g, "text_color", grp.getTextColorCode()); //$NON-NLS-1$ //$NON-NLS-2$
                    ArrayNode itemArr = g.putArray("items"); //$NON-NLS-1$
                    for (MenuItem item : safeList(itemsByGrp.get(grp.getId()))) {
                        ObjectNode itm = itemArr.addObject();
                        itm.put("id", item.getId()); itm.put("name", safe(item.getName())); //$NON-NLS-1$ //$NON-NLS-2$
                        if (item.getPrice()    != null) itm.put("price",     item.getPrice()); //$NON-NLS-1$
                        if (item.getBuyPrice() != null) itm.put("buy_price", item.getBuyPrice()); //$NON-NLS-1$
                        putColor(itm, "button_color", item.getButtonColorCode()); putColor(itm, "text_color", item.getTextColorCode()); //$NON-NLS-1$ //$NON-NLS-2$
                        if (item.getTaxGroup() != null) itm.put("tax_group", safe(item.getTaxGroup().getName())); //$NON-NLS-1$
                        List<MenuItemModifierGroup> migs = item.getMenuItemModiferGroups();
                        if (migs != null && !migs.isEmpty()) {
                            ArrayNode mgArr = itm.putArray("modifier_groups"); //$NON-NLS-1$
                            for (MenuItemModifierGroup mig : migs) {
                                ModifierGroup mg = mig.getModifierGroup();
                                if (mg == null) continue;
                                ObjectNode mgNode = mgArr.addObject();
                                mgNode.put("id", mg.getId()); mgNode.put("name", safe(mg.getName())); //$NON-NLS-1$ //$NON-NLS-2$
                                ArrayNode modArr = mgNode.putArray("modifiers"); //$NON-NLS-1$
                                Set<MenuModifier> mods = mg.getModifiers();
                                if (mods == null) continue;
                                for (MenuModifier mod : mods) {
                                    ObjectNode m = modArr.addObject();
                                    m.put("name", safe(mod.getName())); //$NON-NLS-1$
                                    if (mod.getPrice() != null) m.put("price", mod.getPrice()); //$NON-NLS-1$
                                    if (mod.getExtraPrice() != null && mod.getExtraPrice() > 0) m.put("extra_price", mod.getExtraPrice()); //$NON-NLS-1$
                                    putColor(m, "button_color", mod.getButtonColor()); putColor(m, "text_color", mod.getTextColor()); //$NON-NLS-1$ //$NON-NLS-2$
                                }
                            }
                        }
                    }
                }
            }
            mapper.writeValue(file, root);
        } finally {
            if (session != null) try { session.close(); } catch (Exception ignored) {}
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Excel (XLSX) Export
    // ══════════════════════════════════════════════════════════════════════

    private void exportXlsx(File file) throws Exception {
        Session session = null;
        try {
            session = new GenericDAO().createNewSession();
            List<MenuCategory> categories = MenuCategoryDAO.getInstance().findAll(session);
            List<MenuGroup>    groups     = MenuGroupDAO.getInstance().findAll(session);
            List<MenuItem>     items      = MenuItemDAO.getInstance().findAll(session);

            Map<Integer, List<MenuGroup>> groupsByCat = byParent(groups);
            Map<Integer, List<MenuItem>>  itemsByGrp  = byParentItem(items);
            Map<Integer, MenuCategory>    catById      = new HashMap<Integer, MenuCategory>();
            for (MenuCategory c : categories) catById.put(c.getId(), c);
            Map<Integer, MenuGroup>       grpById      = new HashMap<Integer, MenuGroup>();
            for (MenuGroup g : groups) grpById.put(g.getId(), g);

            // Collect unique modifier groups (deduplicated)
            Map<Integer, ModifierGroup> uniqueMGs = new LinkedHashMap<Integer, ModifierGroup>();
            for (MenuItem item : items) {
                List<MenuItemModifierGroup> migs = item.getMenuItemModiferGroups();
                if (migs == null) continue;
                for (MenuItemModifierGroup mig : migs) {
                    ModifierGroup mg = mig.getModifierGroup();
                    if (mg != null && !uniqueMGs.containsKey(mg.getId())) uniqueMGs.put(mg.getId(), mg);
                }
            }

            XSSFWorkbook wb = new XSSFWorkbook();

            // ── Sheet 1: Menu Items ───────────────────────────────────────
            Sheet itemSheet = wb.createSheet("Menu Items"); //$NON-NLS-1$
            itemSheet.createFreezePane(0, 1);

            String[] itemHeaders = {
                "Product Class", "Product Category", "Product Group", "Product Name", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "Product Description", "Price", "Cost", "SKU", "Barcode", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                "Active", "Fractional Unit", "Inventory Item", "Allow Price Override", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "Button Color", "Hot or Cold", "Not Returnable" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            };
            CellStyle headerStyle = buildHeaderStyle(wb, new byte[]{(byte)0x1A, (byte)0x6E, (byte)0xBD});
            CellStyle evenStyle   = buildRowStyle(wb, new byte[]{(byte)0xF2, (byte)0xF6, (byte)0xFB});
            CellStyle oddStyle    = buildRowStyle(wb, new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF});

            writeRow(itemSheet, 0, itemHeaders, headerStyle);

            int rowIdx = 1;
            for (MenuCategory cat : categories) {
                for (MenuGroup grp : safeList(groupsByCat.get(cat.getId()))) {
                    for (MenuItem item : safeList(itemsByGrp.get(grp.getId()))) {
                        CellStyle rs = (rowIdx % 2 == 0) ? evenStyle : oddStyle;
                        String btnColor = hexColor(item.getButtonColorCode());
                        writeRow(itemSheet, rowIdx++, new Object[]{
                            "",                           // Product Class
                            safe(cat.getName()),           // Product Category
                            safe(grp.getName()),           // Product Group
                            safe(item.getName()),          // Product Name
                            "",                           // Product Description
                            item.getPrice() != null ? item.getPrice() : 0.0,   // Price
                            item.getBuyPrice() != null ? item.getBuyPrice() : 0.0, // Cost
                            "",                           // SKU
                            "",                           // Barcode
                            "Yes",                        // Active
                            "No",                         // Fractional Unit
                            "No",                         // Inventory Item
                            "No",                         // Allow Price Override
                            btnColor,                     // Button Color
                            "",                           // Hot or Cold
                            "No"                          // Not Returnable
                        }, rs);
                    }
                }
            }
            autoSize(itemSheet, itemHeaders.length);

            // ── Sheet 2: Modifiers ────────────────────────────────────────
            Sheet modSheet = wb.createSheet("Modifiers"); //$NON-NLS-1$
            modSheet.createFreezePane(0, 1);

            String[] modHeaders = {
                "Modifier Class", "Modifier Group", "Modifier Name", "Modifier Description", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "Price", "Cost", "SKU", "Active", "Pizza Modifier", "Button Color" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            };
            CellStyle modHeaderStyle = buildHeaderStyle(wb, new byte[]{(byte)0x14, (byte)0x54, (byte)0x23});
            writeRow(modSheet, 0, modHeaders, modHeaderStyle);

            int modRow = 1;
            for (ModifierGroup mg : uniqueMGs.values()) {
                Set<MenuModifier> mods = mg.getModifiers();
                if (mods == null || mods.isEmpty()) continue;
                for (MenuModifier mod : mods) {
                    CellStyle rs = (modRow % 2 == 0) ? evenStyle : oddStyle;
                    writeRow(modSheet, modRow++, new Object[]{
                        "",                           // Modifier Class
                        safe(mg.getName()),            // Modifier Group
                        safe(mod.getName()),           // Modifier Name
                        "",                           // Description
                        mod.getPrice() != null ? mod.getPrice() : 0.0, // Price
                        "",                           // Cost
                        "",                           // SKU
                        "Yes",                        // Active
                        "No",                         // Pizza Modifier
                        hexColor(mod.getButtonColor())// Button Color
                    }, rs);
                }
            }
            autoSize(modSheet, modHeaders.length);

            // ── Sheet 3: Instructions ─────────────────────────────────────
            Sheet infoSheet = wb.createSheet("Info"); //$NON-NLS-1$
            CellStyle infoHdr = buildHeaderStyle(wb, new byte[]{(byte)0x55, (byte)0x55, (byte)0x55});
            writeRow(infoSheet, 0, new String[]{"FloreantPOS Menu Export"}, infoHdr); //$NON-NLS-1$
            Restaurant r = safeRestaurant();
            int ir = 1;
            writeRow(infoSheet, ir++, new String[]{"Version:", POS_VERSION}, null); //$NON-NLS-1$
            writeRow(infoSheet, ir++, new String[]{"Exported:", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())}, null); //$NON-NLS-1$ //$NON-NLS-2$
            if (r != null) {
                writeRow(infoSheet, ir++, new String[]{"Store:", safe(r.getName())}, null); //$NON-NLS-1$
                if (isSet(r.getAddressLine1())) writeRow(infoSheet, ir++, new String[]{"Address:", r.getAddressLine1()}, null); //$NON-NLS-1$
                if (isSet(r.getZipCode()))      writeRow(infoSheet, ir++, new String[]{"ZIP:", r.getZipCode()}, null); //$NON-NLS-1$
            }
            writeRow(infoSheet, ir+1, new String[]{"Note:", "Images are not included. Button colors are exported as hex codes."}, null); //$NON-NLS-1$ //$NON-NLS-2$
            autoSize(infoSheet, 2);

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
            wb.close();

        } finally {
            if (session != null) try { session.close(); } catch (Exception ignored) {}
        }
    }

    // ── Excel helpers ─────────────────────────────────────────────────────

    private CellStyle buildHeaderStyle(Workbook wb, byte[] rgb) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle buildRowStyle(Workbook wb, byte[] rgb) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeRow(Sheet sheet, int rowNum, Object[] values, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object v = values[i];
            if (v instanceof Number) cell.setCellValue(((Number) v).doubleValue());
            else                     cell.setCellValue(v != null ? v.toString() : ""); //$NON-NLS-1$
            if (style != null) cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            try { sheet.autoSizeColumn(i); } catch (Exception ignored) {}
        }
    }

    private String hexColor(Integer code) {
        if (code == null || code == 0) return ""; //$NON-NLS-1$
        Color c = new Color(code);
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()); //$NON-NLS-1$
    }

    // ── Shared helpers ────────────────────────────────────────────────────

    private Map<Integer, List<MenuGroup>> byParent(List<MenuGroup> groups) {
        Map<Integer, List<MenuGroup>> map = new HashMap<Integer, List<MenuGroup>>();
        for (MenuGroup g : groups) {
            if (g.getParent() == null) continue;
            Integer k = g.getParent().getId();
            if (!map.containsKey(k)) map.put(k, new ArrayList<MenuGroup>());
            map.get(k).add(g);
        }
        return map;
    }

    private Map<Integer, List<MenuItem>> byParentItem(List<MenuItem> items) {
        Map<Integer, List<MenuItem>> map = new HashMap<Integer, List<MenuItem>>();
        for (MenuItem i : items) {
            if (i.getParent() == null) continue;
            Integer k = i.getParent().getId();
            if (!map.containsKey(k)) map.put(k, new ArrayList<MenuItem>());
            map.get(k).add(i);
        }
        return map;
    }

    private <T> List<T> safeList(List<T> l) { return l != null ? l : new ArrayList<T>(); }

    private void putColor(ObjectNode node, String key, Integer code) {
        if (code != null && code != 0) {
            Color c = new Color(code);
            node.put(key, String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue())); //$NON-NLS-1$
        }
    }

    private String buildInfoComment(Restaurant r, String exportedAt) {
        StringBuilder sb = new StringBuilder("FloreantPOS Menu Export | Version: ").append(POS_VERSION) //$NON-NLS-1$
            .append(" | Exported: ").append(exportedAt); //$NON-NLS-1$
        if (r != null) {
            sb.append(" | Store: ").append(safe(r.getName())); //$NON-NLS-1$
            if (isSet(r.getAddressLine1())) sb.append(", ").append(r.getAddressLine1()); //$NON-NLS-1$
            if (isSet(r.getAddressLine2())) sb.append(", ").append(r.getAddressLine2()); //$NON-NLS-1$
            if (isSet(r.getAddressLine3())) sb.append(", ").append(r.getAddressLine3()); //$NON-NLS-1$
            if (isSet(r.getZipCode()))      sb.append(" ").append(r.getZipCode()); //$NON-NLS-1$
        }
        sb.append(" | Note: images not included; colors exported as hex"); //$NON-NLS-1$
        return sb.toString();
    }

    private Restaurant safeRestaurant() {
        try { return Application.getInstance().getRestaurant(); } catch (Exception e) { return null; }
    }

    private String getStoreName() {
        try { return Application.getInstance().getRestaurant().getName(); }
        catch (Exception e) { return "store"; } //$NON-NLS-1$
    }

    private String safe(String s)   { return s != null ? s : ""; } //$NON-NLS-1$
    private boolean isSet(String s) { return s != null && !s.trim().isEmpty(); }
    private String sanitize(String s) {
        return s != null ? s.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_") : "store"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
