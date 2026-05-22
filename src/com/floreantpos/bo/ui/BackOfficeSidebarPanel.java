package com.floreantpos.bo.ui;

import java.awt.BasicStroke;
import com.floreantpos.swing.PosUIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;

public class BackOfficeSidebarPanel extends JPanel {

    // ── responsive sizing ─────────────────────────────────────────────────
    static final int COLLAPSED_WIDTH = 56;

    static int expandedWidth() {
        int sw = Toolkit.getDefaultToolkit().getScreenSize().width;
        return Math.max(220, Math.min(300, sw * 14 / 100));
    }

    // ── palette ───────────────────────────────────────────────────────────
    static final Color BG          = new Color(0xFF, 0xFF, 0xFF);  // white
    static final Color HEADER_BG   = new Color(0xF8, 0xF8, 0xF8); // very subtle off-white
    static final Color ITEM_HOVER  = new Color(0xD8, 0xD8, 0xD8);
    static final Color DIVIDER     = new Color(0xE0, 0xE0, 0xE0); // subtle right shadow
    static final Color TEXT        = new Color(0x1A, 0x1A, 0x1A);
    static final Color MUTED       = new Color(0x60, 0x60, 0x60);
    static final Color ACCENT      = new Color(0x55, 0x55, 0x55);
    static final Color ICON_CLR    = new Color(0x40, 0x40, 0x40);

    // ── state ─────────────────────────────────────────────────────────────
    private boolean               collapsed = false;
    private JButton               toggleBtn;
    private JButton               titleBtn;
    private final JPanel          sectionsPanel;
    private final List<SectionPanel> sections = new ArrayList<>();

    /** Fires with the label of whichever item the user clicks. */
    java.util.function.Consumer<String> onItemSelected;

    public void setOnItemSelected(java.util.function.Consumer<String> listener) {
        this.onItemSelected = listener;
    }

    public BackOfficeSidebarPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new MatteBorder(0, 0, 0, 1, DIVIDER));

        // ── top bar ───────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(HEADER_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        topBar.setOpaque(false);
        topBar.setPreferredSize(new Dimension(0, 52));
        topBar.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));

        titleBtn = new JButton("  \u2261  BACK OFFICE");
        titleBtn.setFont(new java.awt.Font(javax.swing.UIManager.getFont("Label.font").getFamily(), java.awt.Font.BOLD, PosUIManager.getFontSize(14)));
        titleBtn.setForeground(TEXT);
        titleBtn.setHorizontalAlignment(SwingConstants.LEFT);
        titleBtn.setBorderPainted(false);
        titleBtn.setContentAreaFilled(false);
        titleBtn.setFocusPainted(false);

        toggleBtn = makeToggleBtn("\u25C4");
        toggleBtn.addActionListener(e -> toggleCollapse());

        topBar.add(titleBtn,   BorderLayout.CENTER);
        topBar.add(toggleBtn,  BorderLayout.EAST);

        // ── sections scroll area ──────────────────────────────────────────
        sectionsPanel = new JPanel();
        sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
        sectionsPanel.setBackground(BG);

        JScrollPane scroll = new JScrollPane(sectionsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));

        add(topBar, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
        setPreferredSize(new Dimension(expandedWidth(), 0));
    }

    // ── public API ────────────────────────────────────────────────────────

    public SectionBuilder section(String title) {
        SectionPanel sp = new SectionPanel(title, this);
        sections.add(sp);
        sectionsPanel.add(sp);
        return new SectionBuilder(sp);
    }

    public SectionBuilder section(String title, boolean permitted) {
        if (!permitted) return new SectionBuilder(null);
        return section(title);
    }

    public void finalizeSidebar() {
        sectionsPanel.add(Box.createVerticalGlue());
        sectionsPanel.revalidate();
    }

    // ── accordion callback ────────────────────────────────────────────────

    void onSectionExpanding(SectionPanel expanding) {
        for (SectionPanel sp : sections) {
            if (sp != expanding && sp.isSectionExpanded()) {
                sp.setSectionExpanded(false);
            }
        }
    }

    // ── collapse toggle ───────────────────────────────────────────────────

    private void toggleCollapse() {
        collapsed = !collapsed;
        if (collapsed) {
            setPreferredSize(new Dimension(COLLAPSED_WIDTH, 0));
            toggleBtn.setText("\u25BA");
            titleBtn.setVisible(false);
        } else {
            setPreferredSize(new Dimension(expandedWidth(), 0));
            toggleBtn.setText("\u25C4");
            titleBtn.setVisible(true);
        }
        for (SectionPanel sp : sections) sp.setCollapsedMode(collapsed);
        revalidate();
        repaint();
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static JButton makeToggleBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    g.setColor(ITEM_HOVER);
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.fillRoundRect(4, 4, getWidth()-8, getHeight()-8, 6, 6);
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new java.awt.Font(javax.swing.UIManager.getFont("Label.font").getFamily(), java.awt.Font.PLAIN, PosUIManager.getFontSize(13)));
        btn.setForeground(MUTED);
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── SectionBuilder ────────────────────────────────────────────────────

    public static class SectionBuilder {
        private final SectionPanel section;
        SectionBuilder(SectionPanel s) { this.section = s; }

        public SectionBuilder item(Action action) {
            if (section != null && action != null) {
                String name = (String) action.getValue(Action.NAME);
                if (name == null) name = action.toString();
                section.addItem(name, action);
            }
            return this;
        }

        public SectionBuilder item(String label, Action action) {
            if (section != null && action != null) section.addItem(label, action);
            return this;
        }

        public SectionBuilder divider() {
            if (section != null) section.addDivider();
            return this;
        }
    }

    // ── SectionPanel ──────────────────────────────────────────────────────

    static class SectionPanel extends JPanel {
        private final String                 title;
        private final BackOfficeSidebarPanel owner;
        private final JPanel                 itemsPanel;
        private final JButton                headerBtn;
        private final Icon                   sectionIcon;
        private final List<ItemRow>          itemRows = new ArrayList<>();
        private boolean                      sectionExpanded = false;
        private boolean                      collapsedMode   = false;

        @Override
        public Dimension getMaximumSize() {
            // When items are hidden, cap height to header only so BoxLayout adds no extra space
            if (!sectionExpanded || collapsedMode) {
                return new Dimension(Integer.MAX_VALUE, 43); // header height + 1px border
            }
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        SectionPanel(String title, BackOfficeSidebarPanel owner) {
            this.title       = title;
            this.owner       = owner;
            this.sectionIcon = SidebarIcons.forSection(title, PosUIManager.getFontSize(18));

            setLayout(new BorderLayout(0, 0));
            setBackground(BG);
            setOpaque(true);
            setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));

            // ── header button ─────────────────────────────────────────────
            headerBtn = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(HEADER_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    if (getModel().isRollover()) {
                        g2.setColor(new Color(255, 255, 255, 18));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    if (sectionExpanded) {
                        g2.setColor(ACCENT);
                        g2.fillRect(0, 0, 3, getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            headerBtn.setIcon(sectionIcon);
            headerBtn.setText(title.toUpperCase() + "  \u25B8");
            headerBtn.setFont(new java.awt.Font(javax.swing.UIManager.getFont("Label.font").getFamily(), java.awt.Font.PLAIN, PosUIManager.getFontSize(13)));
            headerBtn.setForeground(MUTED);
            headerBtn.setHorizontalAlignment(SwingConstants.LEFT);
            headerBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
            headerBtn.setIconTextGap(10);
            headerBtn.setPreferredSize(new Dimension(0, 42));
            headerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            headerBtn.setBorderPainted(false);
            headerBtn.setContentAreaFilled(false);
            headerBtn.setFocusPainted(false);
            headerBtn.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));
            headerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            headerBtn.setToolTipText(title);
            headerBtn.addActionListener(e -> {
                if (collapsedMode) {
                    // Expand sidebar then expand this section
                    owner.collapsed = true;
                    owner.toggleCollapse();
                    if (!sectionExpanded) toggleSection();
                } else {
                    toggleSection();
                }
            });

            // ── items panel ───────────────────────────────────────────────
            itemsPanel = new JPanel();
            itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
            itemsPanel.setBackground(BG);
            itemsPanel.setVisible(false);

            add(headerBtn,  BorderLayout.NORTH);
            add(itemsPanel, BorderLayout.CENTER);
        }

        void toggleSection() {
            if (!sectionExpanded) owner.onSectionExpanding(this);
            setSectionExpanded(!sectionExpanded);
        }

        void setSectionExpanded(boolean expand) {
            sectionExpanded = expand;
            itemsPanel.setVisible(expand && !collapsedMode);
            String arrow = expand ? "  \u25BE" : "  \u25B8";
            if (!collapsedMode) {
                headerBtn.setText(title.toUpperCase() + arrow);
            }
            headerBtn.setForeground(expand ? TEXT : MUTED);
            // Invalidate from here up so BoxLayout re-queries getMaximumSize()
            invalidate();
            if (getParent() != null) getParent().revalidate();
        }

        boolean isSectionExpanded() { return sectionExpanded; }

        void setCollapsedMode(boolean mode) {
            collapsedMode = mode;
            if (mode) {
                // Icon only
                headerBtn.setText(null);
                headerBtn.setHorizontalAlignment(SwingConstants.CENTER);
                headerBtn.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                headerBtn.setIconTextGap(0);
                itemsPanel.setVisible(false);
            } else {
                // Icon + text
                String arrow = sectionExpanded ? "  \u25BE" : "  \u25B8";
                headerBtn.setText(title.toUpperCase() + arrow);
                headerBtn.setHorizontalAlignment(SwingConstants.LEFT);
                headerBtn.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));
                headerBtn.setIconTextGap(10);
                itemsPanel.setVisible(sectionExpanded);
            }
            for (ItemRow row : itemRows) row.setCollapsedMode(mode);
            revalidate();
            repaint();
        }

        void addItem(String label, Action action) {
            Icon icon = SidebarIcons.forLabel(label, PosUIManager.getFontSize(16));
            ItemRow row = new ItemRow(label, icon, action, owner);
            itemRows.add(row);
            itemsPanel.add(row);
        }

        void addDivider() {
            JPanel div = new JPanel();
            div.setBackground(DIVIDER);
            div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            div.setPreferredSize(new Dimension(0, 1));
            itemsPanel.add(div);
        }
    }

    // ── ItemRow ───────────────────────────────────────────────────────────

    static class ItemRow extends JPanel {
        private final JButton btn;
        private final String  label;

        ItemRow(String label, Icon icon, Action action, BackOfficeSidebarPanel sidebar) {
            this.label = label;
            setLayout(new BorderLayout());
            setBackground(BG);
            setOpaque(true);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

            btn = new JButton(label) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isRollover() || getModel().isPressed()) {
                        g2.setColor(ITEM_HOVER);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(ACCENT);
                        g2.fillRect(0, 0, 3, getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            btn.setIcon(icon);
            btn.setFont(new java.awt.Font(javax.swing.UIManager.getFont("Label.font").getFamily(), java.awt.Font.PLAIN, PosUIManager.getFontSize(13)));
            btn.setForeground(TEXT);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            btn.setIconTextGap(10);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            btn.setPreferredSize(new Dimension(0, 36));
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 8));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(label);
            btn.addActionListener(e -> {
                if (sidebar.onItemSelected != null) sidebar.onItemSelected.accept(label);
                // Single-tab mode: close every open tab before opening the new one
                if (!BackOfficeWindow.isMultiTabsEnabled()) {
                    BackOfficeWindow bow = (BackOfficeWindow)
                        SwingUtilities.getAncestorOfClass(BackOfficeWindow.class, sidebar);
                    if (bow != null) {
                        bow.getTabbedPane().removeAll();
                    }
                }
                action.actionPerformed(new ActionEvent(btn, ActionEvent.ACTION_PERFORMED,
                    (String) action.getValue(Action.ACTION_COMMAND_KEY)));
            });
            add(btn, BorderLayout.CENTER);
        }

        void setCollapsedMode(boolean collapsed) {
            if (collapsed) {
                btn.setText(null);
                btn.setHorizontalAlignment(SwingConstants.CENTER);
                btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            } else {
                btn.setText(label);
                btn.setHorizontalAlignment(SwingConstants.LEFT);
                btn.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 8));
            }
        }
    }

    // ── SidebarIcons ──────────────────────────────────────────────────────

    static class SidebarIcons {

        static Icon forSection(String title, int sz) {
            String t = title.toLowerCase();
            if (t.contains("admin"))                        return draw(GEAR,       sz);
            if (t.contains("menu") || t.contains("explo")) return draw(MENU_GRID,  sz);
            if (t.contains("report"))                       return draw(BAR_CHART,  sz);
            if (t.contains("table") || t.contains("floor")) return draw(TABLE_ICON, sz);
            if (t.contains("help"))                         return draw(INFO,       sz);
            return draw(DOT, sz);
        }

        static Icon forLabel(String label, int sz) {
            String l = label.toLowerCase();
            if (l.contains("configure") || l.contains("restaurant")) return draw(GEAR,       sz);
            if (l.contains("currency"))                              return draw(DOLLAR,     sz);
            if (l.contains("user type"))                             return draw(PERSONS,    sz);
            if (l.contains("user"))                                  return draw(PERSON,     sz);
            if (l.contains("gratuiti"))                              return draw(PERCENT,    sz);
            if (l.contains("export"))                                return draw(EXPORT,     sz);
            if (l.contains("import"))                                return draw(IMPORT,     sz);
            if (l.contains("language"))                              return draw(GLOBE,      sz);
            if (l.contains("order type") || l.contains("orders type")) return draw(LIST,    sz);
            if (l.contains("category"))                              return draw(GRID,       sz);
            if (l.contains("modifier group"))                        return draw(SLIDERS,    sz);
            if (l.contains("modifier"))                              return draw(SLIDERS,    sz);
            if (l.contains("multiplier"))                            return draw(TIMES,      sz);
            if (l.contains("group"))                                 return draw(FOLDER,     sz);
            if (l.contains("item") && l.contains("size"))           return draw(RULER,      sz);
            if (l.contains("pizza"))                                 return draw(PIZZA,      sz);
            if (l.contains("crust"))                                 return draw(CIRCLE,     sz);
            if (l.contains("item"))                                  return draw(TAG,        sz);
            if (l.contains("shift"))                                 return draw(CLOCK,      sz);
            if (l.contains("coupon"))                                return draw(SCISSORS,   sz);
            if (l.contains("cooking") || l.contains("instruction")) return draw(FLAME,      sz);
            if (l.contains("tax"))                                   return draw(PERCENT,    sz);
            if (l.contains("payment"))                               return draw(CARD,       sz);
            if (l.contains("drawer"))                                return draw(DRAWER,     sz);
            if (l.contains("ticket"))                                return draw(RECEIPT,    sz);
            if (l.contains("attendance"))                            return draw(CALENDAR,   sz);
            if (l.contains("payroll"))                               return draw(DOLLAR,     sz);
            if (l.contains("labor") || l.contains("hourly"))        return draw(CLOCK,      sz);
            if (l.contains("employee"))                              return draw(PERSON,     sz);
            if (l.contains("server") || l.contains("productivity")) return draw(PERSON,     sz);
            if (l.contains("statistic") || l.contains("key"))       return draw(STAR,       sz);
            if (l.contains("journal"))                               return draw(BOOK,       sz);
            if (l.contains("credit"))                                return draw(CARD,       sz);
            if (l.contains("exception"))                             return draw(WARNING,    sz);
            if (l.contains("balance"))                               return draw(BALANCE,    sz);
            if (l.contains("menu usage"))                            return draw(GRID,       sz);
            if (l.contains("analysis") || l.contains("report"))     return draw(BAR_CHART,  sz);
            if (l.contains("detail"))                                return draw(LIST,       sz);
            if (l.contains("open ticket"))                           return draw(RECEIPT,    sz);
            if (l.contains("sales"))                                 return draw(BAR_CHART,  sz);
            if (l.contains("table") || l.contains("floor"))         return draw(TABLE_ICON, sz);
            if (l.contains("update"))                                return draw(UPLOAD,     sz);
            if (l.contains("plugin"))                                return draw(PLUG,       sz);
            if (l.contains("about"))                                 return draw(INFO,       sz);
            return draw(DOT, sz);
        }

        // icon type IDs
        static final int GEAR=0, DOLLAR=1, PERSON=2, PERSONS=3, PERCENT=4,
            EXPORT=5, IMPORT=6, GLOBE=7, LIST=8, GRID=9, FOLDER=10, TAG=11,
            SLIDERS=12, TIMES=13, PIZZA=14, CIRCLE=15, CLOCK=16, SCISSORS=17,
            FLAME=18, CARD=19, DRAWER=20, RECEIPT=21, CALENDAR=22, BAR_CHART=23,
            TABLE_ICON=24, INFO=25, UPLOAD=26, PLUG=27, DOT=28, MENU_GRID=29,
            RULER=30, BOOK=31, WARNING=32, BALANCE=33, STAR=34;

        static Icon draw(int type, int sz) {
            BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(ICON_CLR);
            float sw = Math.max(1f, sz / 12f);
            g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int m = Math.max(1, sz / 6);
            int w = sz - 2 * m;
            int cx = sz / 2, cy = sz / 2;
            switch (type) {
                case GEAR: {
                    g.drawOval(m + 1, m + 1, w - 2, w - 2);
                    int or2 = w / 2 + m;
                    for (int i = 0; i < 6; i++) {
                        double a = i * Math.PI / 3;
                        g.drawLine((int)(cx+(or2-2)*Math.cos(a)), (int)(cy+(or2-2)*Math.sin(a)),
                                   (int)(cx+(or2+2)*Math.cos(a)), (int)(cy+(or2+2)*Math.sin(a)));
                    }
                    int ir = w / 5;
                    g.drawOval(cx - ir, cy - ir, ir * 2, ir * 2);
                    break;
                }
                case DOLLAR: {
                    g.drawLine(cx, m / 2, cx, sz - m / 2);
                    int dw = w - 2;
                    g.drawArc(m + 1, m, dw, sz / 2 - m, 0, 180);
                    g.drawArc(m + 1, sz / 2 - 1, dw, sz / 2 - m, 180, 180);
                    break;
                }
                case PERSON: {
                    int hr = sz / 5;
                    g.drawOval(cx - hr, sz / 8, hr * 2, hr * 2);
                    g.drawArc(sz / 5, sz / 8 + hr * 2, sz * 3 / 5, sz / 2, 0, 180);
                    break;
                }
                case PERSONS: {
                    int hr = sz / 6;
                    g.drawOval(sz / 5 - hr, sz / 8, hr * 2, hr * 2);
                    g.drawArc(sz / 8, sz / 8 + hr * 2, sz * 2 / 5, sz / 3, 0, 180);
                    g.drawOval(sz / 2, sz / 8, hr * 2, hr * 2);
                    g.drawArc(sz * 3 / 8, sz / 8 + hr * 2, sz * 2 / 5, sz / 3, 0, 180);
                    break;
                }
                case PERCENT: {
                    g.drawLine(sz - m, m, m, sz - m);
                    int r = sz / 5;
                    g.drawOval(m, m, r, r);
                    g.drawOval(sz - m - r, sz - m - r, r, r);
                    break;
                }
                case EXPORT: {
                    g.drawLine(cx, sz - m, cx, m);
                    int hw = sz / 4;
                    g.drawLine(cx - hw, m + hw, cx, m);
                    g.drawLine(cx + hw, m + hw, cx, m);
                    g.drawLine(m, sz - m, sz - m, sz - m);
                    break;
                }
                case IMPORT: {
                    g.drawLine(cx, m, cx, sz - m);
                    int hw = sz / 4;
                    g.drawLine(cx - hw, sz - m - hw, cx, sz - m);
                    g.drawLine(cx + hw, sz - m - hw, cx, sz - m);
                    g.drawLine(m, m, sz - m, m);
                    break;
                }
                case GLOBE: {
                    g.drawOval(m, m, w, w);
                    g.drawLine(m, cy, sz - m, cy);
                    g.drawArc(cx - w / 4, m, w / 2, w, 0, 360);
                    break;
                }
                case LIST: {
                    int lm = m + sz / 5;
                    for (int i = 0; i < 3; i++) {
                        int ly = sz / 4 + i * sz / 4;
                        g.fillOval(m, ly - 1, 3, 3);
                        g.drawLine(lm, ly, sz - m, ly);
                    }
                    break;
                }
                case GRID: {
                    int gap = sz / 8, cs = (w - gap) / 2;
                    for (int r2 = 0; r2 < 2; r2++) for (int c = 0; c < 2; c++) {
                        g.fillRoundRect(m + c * (cs + gap), m + r2 * (cs + gap), cs, cs, 2, 2);
                    }
                    break;
                }
                case FOLDER: {
                    g.fillRoundRect(m, sz * 3 / 8, w, sz / 2, 3, 3);
                    g.fillRoundRect(m, sz / 4, w / 3, sz / 7, 3, 3);
                    break;
                }
                case TAG: {
                    int[] xs = {m, sz-m, sz-m, sz*3/5, m};
                    int[] ys = {m, m, sz*3/5, sz-m, sz*3/5};
                    g.drawPolygon(xs, ys, 5);
                    g.fillOval(m + 2, sz / 2, 3, 3);
                    break;
                }
                case SLIDERS: {
                    int x1 = m + w/4, x2 = cx, x3 = sz - m - w/4;
                    g.drawLine(x1, m, x1, sz-m); g.drawLine(x2, m, x2, sz-m); g.drawLine(x3, m, x3, sz-m);
                    int s2 = sz / 5;
                    g.fillOval(x1-s2/2, sz/3-s2/2, s2, s2);
                    g.fillOval(x2-s2/2, sz*2/3-s2/2, s2, s2);
                    g.fillOval(x3-s2/2, sz/2-s2/2, s2, s2);
                    break;
                }
                case TIMES: {
                    g.setStroke(new BasicStroke(Math.max(1.5f, sz/8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(m, m, sz-m, sz-m);
                    g.drawLine(sz-m, m, m, sz-m);
                    break;
                }
                case PIZZA: {
                    g.drawOval(m, m, w, w);
                    g.drawLine(cx, m, cx, sz-m);
                    g.drawLine(m, cy, sz-m, cy);
                    g.drawLine(cx, cy, sz-m, m);
                    break;
                }
                case CIRCLE: {
                    g.drawOval(m, m, w, w);
                    int m2 = m + w / 4;
                    g.drawOval(m2, m2, sz - 2*m2, sz - 2*m2);
                    break;
                }
                case CLOCK: {
                    g.drawOval(m, m, w, w);
                    g.drawLine(cx, cy, cx, m + 2);
                    g.drawLine(cx, cy, sz - m - 2, cy);
                    break;
                }
                case SCISSORS: {
                    int r2 = sz / 5;
                    g.drawOval(m, sz / 4 - r2, r2, r2);
                    g.drawOval(m, sz*3/4 - r2, r2, r2);
                    g.drawLine(m + r2, sz / 4, sz - m, m);
                    g.drawLine(m + r2, sz*3/4, sz - m, sz - m);
                    break;
                }
                case FLAME: {
                    int[] fx = {cx, sz-m, sz*3/5, sz*3/4, cx, sz/4, sz*2/5, m};
                    int[] fy = {m, sz/3, sz/2, sz-m, sz*2/3, sz-m, sz/2, sz/3};
                    g.drawPolygon(fx, fy, 8);
                    break;
                }
                case CARD: {
                    g.drawRoundRect(m, sz/3, w, sz/3, 3, 3);
                    g.drawLine(m, sz/3 + sz/9, sz-m, sz/3 + sz/9);
                    g.fillRect(m + sz/8, sz/3 + 2, sz/8, sz/12);
                    break;
                }
                case DRAWER: {
                    g.drawRoundRect(m, m, w, w, 4, 4);
                    g.drawLine(m, cy, sz-m, cy);
                    g.fillRoundRect(sz/3, cy + sz/10, sz/3, sz/10, 3, 3);
                    break;
                }
                case RECEIPT: {
                    g.drawRect(m, m, w, w);
                    int lm2 = m + 3, rw = w - 6;
                    g.drawLine(lm2, sz/3, lm2+rw, sz/3);
                    g.drawLine(lm2, sz/2, lm2+rw, sz/2);
                    g.drawLine(lm2, sz*2/3, lm2+rw*2/3, sz*2/3);
                    break;
                }
                case CALENDAR: {
                    g.drawRoundRect(m, sz/4, w, sz-sz/4-m, 3, 3);
                    g.drawLine(m, sz*3/8, sz-m, sz*3/8);
                    g.drawLine(sz/3, m, sz/3, sz/3);
                    g.drawLine(sz*2/3, m, sz*2/3, sz/3);
                    for (int cr = 0; cr < 2; cr++) for (int cc = 0; cc < 3; cc++) {
                        g.fillOval(m+(cc+1)*w/4-1, sz*3/8+(cr+1)*sz/7-1, 3, 3);
                    }
                    break;
                }
                case BAR_CHART: {
                    int bw2 = (w - 4) / 3;
                    int[] bh = {sz/2, sz*2/3, sz/3};
                    for (int i = 0; i < 3; i++) {
                        g.fillRoundRect(m + i*(bw2+2), sz-m-bh[i], bw2, bh[i], 2, 2);
                    }
                    g.drawLine(m, sz-m, sz-m, sz-m);
                    break;
                }
                case TABLE_ICON: {
                    int cw2 = w / 3, ch2 = w / 2;
                    for (int r2 = 0; r2 < 2; r2++) for (int c = 0; c < 3; c++) {
                        g.drawRect(m + c*cw2, m + r2*ch2, cw2, ch2);
                    }
                    break;
                }
                case INFO: {
                    g.drawOval(m, m, w, w);
                    g.drawLine(cx, sz/3, cx, sz*2/3);
                    g.fillOval(cx-1, sz/4, 3, 3);
                    break;
                }
                case UPLOAD: {
                    g.drawRect(m, cy, w, sz/2-m);
                    g.drawLine(cx, m, cx, cy);
                    int hw2 = sz/5;
                    g.drawLine(cx-hw2, m+hw2, cx, m);
                    g.drawLine(cx+hw2, m+hw2, cx, m);
                    break;
                }
                case PLUG: {
                    g.drawRect(cx-sz/5, sz/5, sz*2/5, sz/3);
                    g.drawLine(cx-sz/8, sz/5, cx-sz/8, sz/8);
                    g.drawLine(cx+sz/8, sz/5, cx+sz/8, sz/8);
                    g.drawLine(cx, sz/5+sz/3, cx, sz-m);
                    break;
                }
                case MENU_GRID: {
                    int gs = (w) / 4;
                    for (int i = 1; i <= 3; i++) g.drawLine(m, m+i*gs, sz-m, m+i*gs);
                    break;
                }
                case RULER: {
                    g.drawRect(m, sz/3, w, sz/3);
                    int ts = w / 4;
                    for (int i = 1; i < 4; i++) {
                        int rx = m + i*ts;
                        g.drawLine(rx, sz/3, rx, sz/3 + (i==2 ? sz/4 : sz/8));
                    }
                    break;
                }
                case BOOK: {
                    g.drawRoundRect(m, m, w, w, 3, 3);
                    g.drawLine(m+w/5, m, m+w/5, sz-m);
                    int bm = m+w/5+3;
                    g.drawLine(bm, sz/3, sz-m-2, sz/3);
                    g.drawLine(bm, sz/2, sz-m-2, sz/2);
                    break;
                }
                case WARNING: {
                    int[] wsx = {cx, sz-m, m};
                    int[] wsy = {m, sz-m, sz-m};
                    g.drawPolygon(wsx, wsy, 3);
                    g.drawLine(cx, sz/3, cx, sz*2/3);
                    g.fillOval(cx-1, sz*3/4, 3, 3);
                    break;
                }
                case BALANCE: {
                    g.drawLine(cx, m, cx, sz-m);
                    g.drawLine(m, sz/3, sz-m, sz/3);
                    g.drawArc(m, sz/3, w/3, sz/4, 0, 180);
                    g.drawArc(sz-m-w/3, sz/3, w/3, sz/4, 0, 180);
                    g.drawLine(m, sz-m, sz-m, sz-m);
                    break;
                }
                case STAR: {
                    int or3 = w/2, ir2 = w/4, n = 5;
                    int[] sxs = new int[n*2], sys = new int[n*2];
                    for (int i = 0; i < n*2; i++) {
                        double a = Math.PI*i/n - Math.PI/2;
                        int r2 = (i%2==0) ? or3 : ir2;
                        sxs[i] = cx + (int)(r2*Math.cos(a));
                        sys[i] = cy + (int)(r2*Math.sin(a));
                    }
                    g.drawPolygon(sxs, sys, n*2);
                    break;
                }
                default: {
                    int dr = sz/4;
                    g.fillOval(cx-dr, cy-dr, dr*2, dr*2);
                    break;
                }
            }
            g.dispose();
            return new ImageIcon(img);
        }
    }
}
