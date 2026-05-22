package com.floreantpos.bo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.Set;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import com.floreantpos.swing.ToggleSwitchButton;

import com.floreantpos.actions.AboutAction;
import com.floreantpos.actions.UpdateAction;
import com.floreantpos.bo.actions.AttendanceHistoryAction;
import com.floreantpos.bo.actions.CategoryExplorerAction;
import com.floreantpos.bo.actions.ConfigureRestaurantAction;
import com.floreantpos.bo.actions.CookingInstructionExplorerAction;
import com.floreantpos.bo.actions.CouponExplorerAction;
import com.floreantpos.bo.actions.CreditCardReportAction;
import com.floreantpos.bo.actions.CurrencyExplorerAction;
import com.floreantpos.bo.actions.CustomPaymentReportAction;
import com.floreantpos.bo.actions.DataExportAction;
import com.floreantpos.bo.actions.DataImportAction;
import com.floreantpos.bo.actions.EditButtonStyleAction;
import com.floreantpos.bo.actions.MenuJsonExportAction;
import com.floreantpos.bo.actions.MenuJsonImportAction;
import com.floreantpos.bo.actions.DrawerPullReportExplorerAction;
import com.floreantpos.bo.actions.EmployeeAttendanceAction;
import com.floreantpos.bo.actions.GroupExplorerAction;
import com.floreantpos.bo.actions.HourlyLaborReportAction;
import com.floreantpos.bo.actions.ItemExplorerAction;
import com.floreantpos.bo.actions.JournalReportAction;
import com.floreantpos.bo.actions.KeyStatisticsSalesReportAction;
import com.floreantpos.bo.actions.LanguageSelectionAction;
import com.floreantpos.bo.actions.MenuItemSizeExplorerAction;
import com.floreantpos.bo.actions.MenuUsageReportAction;
import com.floreantpos.bo.actions.ModifierExplorerAction;
import com.floreantpos.bo.actions.ModifierGroupExplorerAction;
import com.floreantpos.bo.actions.MultiplierExplorerAction;
import com.floreantpos.bo.actions.OpenTicketSummaryReportAction;
import com.floreantpos.bo.actions.OrdersTypeExplorerAction;
import com.floreantpos.bo.actions.PayrollReportAction;
import com.floreantpos.bo.actions.PizzaCrustExplorerAction;
import com.floreantpos.bo.actions.PizzaExplorerAction;
import com.floreantpos.bo.actions.PizzaItemExplorerAction;
import com.floreantpos.bo.actions.PizzaModifierExplorerAction;
import com.floreantpos.bo.actions.PluginsAction;
import com.floreantpos.bo.actions.SalesAnalysisReportAction;
import com.floreantpos.bo.actions.SalesBalanceReportAction;
import com.floreantpos.bo.actions.SalesDetailReportAction;
import com.floreantpos.bo.actions.SalesExceptionReportAction;
import com.floreantpos.bo.actions.SalesReportAction;
import com.floreantpos.bo.actions.ServerProductivityReportAction;
import com.floreantpos.bo.actions.ShiftExplorerAction;
import com.floreantpos.bo.actions.TaxExplorerAction;
import com.floreantpos.bo.actions.TicketExplorerAction;
import com.floreantpos.bo.actions.UserExplorerAction;
import com.floreantpos.bo.actions.UserTypeExplorerAction;
import com.floreantpos.bo.actions.ViewGratuitiesAction;
import com.floreantpos.config.AppConfig;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.customPayment.CustomPaymentBrowserAction;
import com.floreantpos.extension.ExtensionManager;
import com.floreantpos.extension.FloreantPlugin;
import com.floreantpos.extension.OrderServiceExtension;
import com.floreantpos.main.Application;
import com.floreantpos.main.TrainingMode;
import com.floreantpos.model.User;
import com.floreantpos.model.UserPermission;
import com.floreantpos.model.UserType;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.table.ShowTableBrowserAction;
import com.jidesoft.swing.JideTabbedPane;

public class BackOfficeWindow extends javax.swing.JFrame {

    private static final String POSY          = "bwy";
    private static final String POSX          = "bwx";
    private static final String WINDOW_HEIGHT = "bwheight";
    private static final String WINDOW_WIDTH  = "bwwidth";

    // ── colors ─────────────────────────────────────────────────────────────
    private static final Color HDR_BG        = new Color(0xF0, 0xF0, 0xF0);
    private static final Color HDR_BORDER    = new Color(0xCE, 0xCE, 0xCE);
    private static final Color HDR_TEXT      = new Color(0x1A, 0x1A, 0x1A);
    private static final Color HDR_MUTED     = new Color(0x60, 0x60, 0x60);
    private static final Color HDR_ACCENT    = new Color(0x55, 0x55, 0x55);

    private static final Color STATUS_TRAIN_BG   = new Color(0xFF, 0xED, 0xC8); // light amber
    private static final Color STATUS_TRAIN_TEXT = new Color(0x7A, 0x3D, 0x00);
    private static final Color STATUS_PROD_BG    = new Color(0xE8, 0xE8, 0xE8);
    private static final Color STATUS_PROD_TEXT  = new Color(0x1A, 0x1A, 0x1A);

    // ── multi-tab preference ────────────────────────────────────────────────
    private static boolean multiTabsEnabled = false;

    public static boolean isMultiTabsEnabled() { return multiTabsEnabled; }

    // ── components ──────────────────────────────────────────────────────────
    private static BackOfficeWindow instance;
    private BackOfficeSidebarPanel  sidebarPanel;
    private User                    user;
    private JLabel                  contentTitleLabel;
    private JPanel                  statusBar;

    // ── kept for plugin API compatibility ───────────────────────────────────
    private JMenu    floorPlanMenu;
    private JMenuBar menuBar = new JMenuBar();

    public BackOfficeWindow() {
        instance = this;
        setIconImage(Application.getApplicationIcon().getImage());
        initComponents();
        positionWindow();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { close(); }
        });
        setTitle(Application.getTitle() + " - " + com.floreantpos.POSConstants.BACK_OFFICE);
        applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
    }

    private void positionWindow() {
        int width  = AppConfig.getInt(WINDOW_WIDTH,  960);
        int height = AppConfig.getInt(WINDOW_HEIGHT, 680);
        setSize(width, height);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width  - width)  >> 1;
        int y = (screenSize.height - height) >> 1;
        x = AppConfig.getInt(POSX, x);
        y = AppConfig.getInt(POSY, y);
        setLocation(x, y);
    }

    private void initComponents() {
        tabbedPane = new JideTabbedPane();
        tabbedPane.setTabShape(JideTabbedPane.SHAPE_WINDOWS);
        tabbedPane.setShowCloseButtonOnTab(true);
        tabbedPane.setTabInsets(new Insets(5, 5, 5, 5));
        Font font = new Font(tabbedPane.getFont().getName(), Font.PLAIN, PosUIManager.getDefaultFontSize());
        tabbedPane.setFont(font);

        sidebarPanel = new BackOfficeSidebarPanel();

        // ── content title bar ──────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(HDR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // accent line on left edge
                g2.setColor(HDR_ACCENT);
                g2.fillRect(0, 8, 3, getHeight() - 16);
            }
        };
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(0, 46));
        titleBar.setBorder(new MatteBorder(0, 0, 1, 0, HDR_BORDER));

        contentTitleLabel = new JLabel("  Back Office");
        contentTitleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
        contentTitleLabel.setForeground(HDR_TEXT);
        contentTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));

        JLabel userLabel = new JLabel();
        userLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN));
        userLabel.setForeground(HDR_MUTED);
        userLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        if (user != null) {
            userLabel.setText(user.getFullName() + "  \u25CF  ");
        }

        titleBar.add(contentTitleLabel, BorderLayout.CENTER);
        titleBar.add(userLabel,         BorderLayout.EAST);

        // ── status bar ────────────────────────────────────────────────────
        statusBar = buildStatusBar();

        // ── center area ───────────────────────────────────────────────────
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        centerPanel.add(titleBar,   BorderLayout.NORTH);
        centerPanel.add(tabbedPane, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().add(sidebarPanel, BorderLayout.WEST);
        getContentPane().add(centerPanel,  BorderLayout.CENTER);
        getContentPane().add(statusBar,    BorderLayout.SOUTH);
    }

    private JPanel buildStatusBar() {
        boolean training = TrainingMode.isEnabled();
        Color bg   = training ? STATUS_TRAIN_BG   : STATUS_PROD_BG;
        Color fg   = training ? STATUS_TRAIN_TEXT  : STATUS_PROD_TEXT;

        String icon = training ? "\u26A0" : "\u25CF";   // ⚠ or ●
        String mode = training
            ? "  TRAINING MODE  \u2014  Transactions will NOT be saved to the live database"
            : "  PRODUCTION MODE  \u2014  All changes affect live data";

        JPanel bar = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setBackground(bg);
        bar.setPreferredSize(new Dimension(0, 36));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, bg.darker()));

        JLabel msgLabel = new JLabel(icon + mode);
        msgLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
        msgLabel.setForeground(fg);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));

        String restaurant = "";
        try { restaurant = Application.getInstance().getRestaurant().getName(); } catch (Exception ignored) {}

        // ── right side: toggle + restaurant name ──────────────────────────
        Color mutedFg = training ? new Color(0x7A, 0x3D, 0x00) : new Color(0x60, 0x60, 0x60);

        JLabel toggleLabel = new JLabel("Multiple Tabs");
        toggleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN));
        toggleLabel.setForeground(mutedFg);

        ToggleSwitchButton toggle = new ToggleSwitchButton();
        toggle.setSelected(multiTabsEnabled);
        toggle.addActionListener(e -> multiTabsEnabled = toggle.isSelected());
        toggle.setToolTipText("When off, opening a sidebar item closes all other tabs");

        JLabel restaurantLabel = new JLabel("  " + restaurant + "  ");
        restaurantLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN));
        restaurantLabel.setForeground(mutedFg);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(toggleLabel);
        rightPanel.add(toggle);
        rightPanel.add(restaurantLabel);

        bar.add(msgLabel,   BorderLayout.CENTER);
        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    private void createMenus() {
        if (user == null) return;

        UserType userType = user.getType();
        Set<UserPermission> permissions = (userType != null) ? userType.getPermissions() : null;
        boolean fullAccess = (userType == null) || TrainingMode.isEnabled();

        boolean canAdmin   = fullAccess || (permissions != null && permissions.contains(UserPermission.PERFORM_ADMINISTRATIVE_TASK));
        boolean canExplore = fullAccess || (permissions != null && permissions.contains(UserPermission.VIEW_EXPLORERS));
        boolean canReport  = fullAccess || (permissions != null && permissions.contains(UserPermission.VIEW_REPORTS));

        // Rebuild sidebar
        BorderLayout layout = (BorderLayout) getContentPane().getLayout();
        if (layout.getLayoutComponent(BorderLayout.WEST) != null) {
            getContentPane().remove(layout.getLayoutComponent(BorderLayout.WEST));
        }
        sidebarPanel = new BackOfficeSidebarPanel();
        getContentPane().add(sidebarPanel, BorderLayout.WEST);

        // Update user label in title bar and status bar
        JPanel centerPanel = (JPanel) layout.getLayoutComponent(BorderLayout.CENTER);
        if (centerPanel != null) {
            JPanel titleBar = (JPanel) ((BorderLayout) centerPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            if (titleBar != null) {
                JLabel userLabel = (JLabel) ((BorderLayout) titleBar.getLayout()).getLayoutComponent(BorderLayout.EAST);
                if (userLabel != null && user != null) {
                    userLabel.setText(user.getFullName() + "  \u25CF  ");
                }
            }
        }

        // Rebuild status bar to reflect current training mode
        if (layout.getLayoutComponent(BorderLayout.SOUTH) != null) {
            getContentPane().remove(layout.getLayoutComponent(BorderLayout.SOUTH));
        }
        statusBar = buildStatusBar();
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        // Wire title update
        sidebarPanel.setOnItemSelected(label -> {
            contentTitleLabel.setText("  " + label);
            contentTitleLabel.repaint();
        });

        // ── ADMINISTRATION ──────────────────────────────────────────────
        sidebarPanel.section("Administration", canAdmin)
                .item(new ConfigureRestaurantAction())
                .item(new CurrencyExplorerAction())
                .item(new UserExplorerAction())
                .item(new UserTypeExplorerAction())
                .item(new ViewGratuitiesAction())
                .divider()
                .item(new MenuJsonExportAction())
                .item(new MenuJsonImportAction())
                .item(new LanguageSelectionAction());

        // ── MENU & EXPLORERS ────────────────────────────────────────────
        BackOfficeSidebarPanel.SectionBuilder explorerSection =
                sidebarPanel.section("Menu & Explorers", canExplore);
        if (TerminalConfig.isMultipleOrderSupported()) {
            explorerSection.item(new OrdersTypeExplorerAction());
        }
        explorerSection
                .item(new CategoryExplorerAction())
                .item(new GroupExplorerAction())
                .item(new ItemExplorerAction())
                .divider()
                .item(new ModifierGroupExplorerAction())
                .item(new ModifierExplorerAction())
                .item(new MultiplierExplorerAction())
                .divider()
                .item(new PizzaExplorerAction())
                .item(new MenuItemSizeExplorerAction())
                .item(new PizzaCrustExplorerAction())
                .item(new PizzaItemExplorerAction())
                .item(new PizzaModifierExplorerAction())
                .divider()
                .item(new ShiftExplorerAction())
                .item(new CouponExplorerAction())
                .item(new CookingInstructionExplorerAction())
                .item(new TaxExplorerAction())
                .item(new CustomPaymentBrowserAction())
                .item(new DrawerPullReportExplorerAction())
                .item(new TicketExplorerAction())
                .item(new AttendanceHistoryAction());

        OrderServiceExtension plugin =
                (OrderServiceExtension) ExtensionManager.getPlugin(OrderServiceExtension.class);
        if (plugin != null) {
            // plugin.createCustomerMenu — not available for sidebar; skip
        }

        // ── REPORTS ─────────────────────────────────────────────────────
        sidebarPanel.section("Reports", canReport)
                .item(new SalesReportAction())
                .item(new OpenTicketSummaryReportAction())
                .item(new HourlyLaborReportAction())
                .item(new PayrollReportAction())
                .item(new EmployeeAttendanceAction())
                .divider()
                .item(new KeyStatisticsSalesReportAction())
                .item(new SalesAnalysisReportAction())
                .item(new SalesBalanceReportAction())
                .item(new SalesDetailReportAction())
                .item(new SalesExceptionReportAction())
                .item(new MenuUsageReportAction())
                .item(new ServerProductivityReportAction())
                .item(new JournalReportAction())
                .divider()
                .item(new CreditCardReportAction())
                .item(new CustomPaymentReportAction());

        // ── TABLES & FLOOR ───────────────────────────────────────────────
        sidebarPanel.section("Tables & Floor")
                .item(new ShowTableBrowserAction());

        // ── HELP ────────────────────────────────────────────────────────
        sidebarPanel.section("Help")
                .item(new UpdateAction())
                .item(new PluginsAction())
                .item(new AboutAction());

        for (FloreantPlugin fp : ExtensionManager.getPlugins()) {
            fp.initBackoffice();
        }

        sidebarPanel.finalizeSidebar();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new BackOfficeWindow().setVisible(true));
    }

    // Variables
    private JideTabbedPane tabbedPane;

    public javax.swing.JTabbedPane getTabbedPane() { return tabbedPane; }

    private void saveSizeAndLocation() {
        AppConfig.putInt(WINDOW_WIDTH,  getWidth());
        AppConfig.putInt(WINDOW_HEIGHT, getHeight());
        AppConfig.putInt(POSX,          getX());
        AppConfig.putInt(POSY,          getY());
    }

    public void close() {
        saveSizeAndLocation();
        getTabbedPane().removeAll();
        dispose();
    }

    public static BackOfficeWindow getInstance() {
        if (instance == null) instance = new BackOfficeWindow();
        return instance;
    }

    public JMenuBar getBackOfficeMenuBar() { return menuBar; }
    public JMenu getFloorPlanMenu()        { return floorPlanMenu; }
    public void  setFloorPlanMenu(JMenu m) { this.floorPlanMenu = m; }

    public void setBackOfficeUser(User backOfficeUser) {
        this.user = backOfficeUser;
        createMenus();
    }
}
