package com.floreantpos.ui.views.payment;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.floreantpos.IconFactory;
import com.floreantpos.Messages;
import com.floreantpos.config.CardConfig;
import com.floreantpos.main.Application;
import com.floreantpos.model.PaymentType;
import com.floreantpos.swing.FocusedTextField;
import com.floreantpos.swing.POSToggleButton;
import com.floreantpos.swing.POSTextField;
import com.floreantpos.swing.PosButton;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.swing.QwertyKeyPad;
import com.floreantpos.ui.TitlePanel;
import com.floreantpos.ui.dialog.POSDialog;

import net.miginfocom.swing.MigLayout;

public class CardEntryDialog extends POSDialog implements CardInputProcessor {

    public enum Tab { KEYED, SWIPE, AUTH_CODE }

    // ── Card network metadata ─────────────────────────────────────────────
    private enum CardNetwork {
        VISA      ("visa_card.png",    16, 3, "CVV"),
        MASTERCARD("master_card.png",  16, 3, "CVV"),
        AMEX      ("am_ex_card.png",   15, 4, "CID"),
        DISCOVER  ("discover_card.png",16, 3, "CVV"),
        UNKNOWN   (null,               16, 3, "CVV");

        final String icon;
        final int    cardLen, cvvLen;
        final String cvvLabel;
        CardNetwork(String icon, int cardLen, int cvvLen, String cvvLabel) {
            this.icon = icon; this.cardLen = cardLen;
            this.cvvLen = cvvLen; this.cvvLabel = cvvLabel;
        }
    }

    // ── palette ────────────────────────────────────────────────────────────
    private static final Color ACCENT        = new Color(0x1A, 0x6E, 0xBD);
    private static final Color TAB_INACTIVE  = new Color(0x88, 0x88, 0x88);
    private static final Color TAB_BAR_BG    = new Color(0xF6, 0xF6, 0xF6);
    private static final Color TAB_ACTIVE_BG = Color.WHITE;
    private static final Color BORDER_CLR    = new Color(0xDE, 0xDE, 0xDE);
    private static final Color LABEL_FG      = new Color(0x44, 0x44, 0x44);
    private static final Color HINT_FG       = new Color(0xAA, 0xAA, 0xAA);
    private static final Color SWIPE_CARD    = new Color(0xD0, 0xD8, 0xE8);
    private static final Color SWIPE_STRIPE  = new Color(0x88, 0x99, 0xBB);
    private static final Color NET_ACTIVE_BG = new Color(0xE8, 0xF2, 0xFF);
    private static final Color NET_ACTIVE_BR = new Color(0x1A, 0x6E, 0xBD);
    private static final Color NET_IDLE_BG   = new Color(0xF5, 0xF5, 0xF5);
    private static final Color NET_IDLE_BR   = new Color(0xDD, 0xDD, 0xDD);

    // ── dialog state ──────────────────────────────────────────────────────
    private CardInputListener cardInputListener;
    private Tab               activeTab    = Tab.KEYED;
    private CardNetwork       activeNet    = CardNetwork.UNKNOWN;
    private CardLayout        cardLayout;
    private JPanel            contentArea;
    private JToggleButton[]   tabBtns      = new JToggleButton[3];

    // Keyed entry
    private JTextField   tfCardNumber;
    private JTextField   tfExpMonth;
    private JTextField   tfExpYear;
    private JTextField   tfCvv;
    private JLabel       lblCvv;
    private MaxLenFilter cardFilter;
    private MaxLenFilter cvvFilter;
    private POSToggleButton[] netBtns      = new POSToggleButton[4];
    private PosButton    btnSubmitKeyed;

    // Swipe
    private JPasswordField tfSwipe;
    private String         cardString;

    // Auth code
    private FocusedTextField tfAuthCode;
    private POSToggleButton  btnVisa, btnMaster, btnAmex, btnDiscover;
    private POSToggleButton  btnDebitVisa, btnDebitMaster;
    private PosButton        btnSubmitGlobal;

    // Test cards (sandbox only)
    private static final String[][] TEST_CARDS = {
        {"4111111111111111", "12", "28", "123",  "VISA"},
        {"4007000000027",    "12", "28", "123",  "VISA"},
        {"5424000000000015", "12", "28", "123",  "MASTERCARD"},
        {"2223000010309703", "12", "28", "123",  "MASTERCARD"},
        {"370000000000002",  "12", "28", "1234", "AMEX"},
        {"6011000000000012", "12", "28", "123",  "DISCOVER"},
    };

    // ─────────────────────────────────────────────────────────────────────
    public CardEntryDialog(CardInputListener listener) {
        super(Application.getPosWindow(), true);
        this.cardInputListener = listener;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUI();
        selectDefaultTab();
    }

    // ── Top-level UI ──────────────────────────────────────────────────────

    private void buildUI() {
        setPreferredSize(new Dimension(PosUIManager.getSize(920), PosUIManager.getSize(640)));

        TitlePanel title = new TitlePanel();
        title.setTitle("Card Payment");
        getContentPane().add(title, BorderLayout.NORTH);

        JPanel topArea = new JPanel(new BorderLayout(0, 0));
        topArea.add(buildTabBar(), BorderLayout.NORTH);
        if (CardConfig.isSandboxMode()) {
            topArea.add(buildSandboxBanner(), BorderLayout.SOUTH);
        }

        JPanel center = new JPanel(new BorderLayout());
        center.add(topArea,          BorderLayout.NORTH);
        center.add(buildContent(),   BorderLayout.CENTER);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── Tab bar ────────────────────────────────────────────────────────────

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new GridLayout(1, 3)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(TAB_BAR_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, PosUIManager.getSize(68)));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));

        ButtonGroup group = new ButtonGroup();
        tabBtns[0] = makeTab("Keyed Entry", new KeyboardIcon(), 0);
        tabBtns[1] = makeTab("Swipe Card",  new SwipeCardIcon(), 1);
        tabBtns[2] = makeTab("Auth Code",   new AuthCodeIcon(),  2);

        for (JToggleButton b : tabBtns) { group.add(b); bar.add(b); }
        tabBtns[1].setEnabled(CardConfig.isSwipeCardSupported());
        tabBtns[2].setEnabled(CardConfig.isExtTerminalSupported());
        return bar;
    }

    private JToggleButton makeTab(String label, Icon icon, int idx) {
        JToggleButton btn = new JToggleButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean on  = isSelected();
                boolean off = !isEnabled();
                g2.setColor(on ? TAB_ACTIVE_BG : TAB_BAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (on) {
                    g2.setColor(ACCENT);
                    g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                    g2.setColor(BORDER_CLR);
                    g2.drawLine(0, 0, 0, getHeight() - 1);
                    g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);
                }
                Color col = off ? BORDER_CLR : (on ? ACCENT : TAB_INACTIVE);
                int iw = icon.getIconWidth(), ih = icon.getIconHeight();
                int ix = (getWidth() - iw) / 2, iy = 8;
                BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
                Graphics2D ig = img.createGraphics();
                ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                icon.paintIcon(this, ig, 0, 0);
                ig.setComposite(java.awt.AlphaComposite.SrcIn);
                ig.setColor(col); ig.fillRect(0, 0, iw, ih);
                ig.dispose();
                g2.drawImage(img, ix, iy, null);
                g2.setColor(col);
                Font f = new Font(Font.DIALOG, on ? Font.BOLD : Font.PLAIN, 13);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(label);
                g2.drawString(label, (getWidth() - tw) / 2, iy + ih + 3 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> switchTab(Tab.values()[idx]));
        return btn;
    }

    private void switchTab(Tab tab) {
        activeTab = tab;
        tabBtns[0].setSelected(tab == Tab.KEYED);
        tabBtns[1].setSelected(tab == Tab.SWIPE);
        tabBtns[2].setSelected(tab == Tab.AUTH_CODE);
        cardLayout.show(contentArea, tab.name());
        repaint();
    }

    private void selectDefaultTab() {
        switch (CardConfig.getCardReader()) {
            case SWIPE:             switchTab(Tab.SWIPE);     break;
            case EXTERNAL_TERMINAL: switchTab(Tab.AUTH_CODE); break;
            default:                switchTab(Tab.KEYED);     break;
        }
    }

    // ── Content ────────────────────────────────────────────────────────────

    private JPanel buildContent() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(Color.WHITE);
        contentArea.add(buildKeyedPanel(),  Tab.KEYED.name());
        contentArea.add(buildSwipePanel(),  Tab.SWIPE.name());
        contentArea.add(buildAuthPanel(),   Tab.AUTH_CODE.name());
        return contentArea;
    }

    // ── Keyed Entry ────────────────────────────────────────────────────────

    private JPanel buildKeyedPanel() {
        JPanel p = new JPanel(new MigLayout("insets 16 28 8 28, fill", "[grow]", "[][][grow]"));
        p.setOpaque(false);

        // ── Network icon buttons ─────────────────────────────────────────
        JPanel netRow = new JPanel(new MigLayout("insets 0, al center", "[]8[]8[]8[]", ""));
        netRow.setOpaque(false);

        CardNetwork[] nets = {CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX, CardNetwork.DISCOVER};
        ButtonGroup   netGroup = new ButtonGroup();
        for (int i = 0; i < nets.length; i++) {
            final CardNetwork net = nets[i];
            POSToggleButton nb = new POSToggleButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean sel = isSelected();
                    g2.setColor(sel ? NET_ACTIVE_BG : NET_IDLE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(sel ? NET_ACTIVE_BR : NET_IDLE_BR);
                    g2.setStroke(new BasicStroke(sel ? 1.8f : 1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    if (!sel) {
                        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.55f));
                    }
                    super.paintComponent(g2);
                    g2.dispose();
                }
            };
            nb.setIcon(IconFactory.getIcon("/ui_icons/", net.icon));
            nb.setPreferredSize(new Dimension(PosUIManager.getSize(72), PosUIManager.getSize(46)));
            nb.setBorderPainted(false);
            nb.setContentAreaFilled(false);
            nb.setFocusPainted(false);
            nb.addActionListener(e -> applyNetwork(net));
            netGroup.add(nb);
            netRow.add(nb);
            netBtns[i] = nb;
        }
        if (CardConfig.isSandboxMode()) {
            JPanel netAndTest = new JPanel(new MigLayout("insets 0", "[grow][]", ""));
            netAndTest.setOpaque(false);
            netAndTest.add(netRow, "growx");
            PosButton btnTestCard = new PosButton("Try Dummy Test Card");
            btnTestCard.addActionListener(e -> fillTestCard());
            netAndTest.add(btnTestCard, "");
            p.add(netAndTest, "growx, wrap, gapy 0 10");
        } else {
            p.add(netRow, "growx, wrap, gapy 0 10");
        }

        // ── Card fields ──────────────────────────────────────────────────
        JPanel fieldsPanel = new JPanel(new MigLayout("insets 0", "[grow]", "[]6[]"));
        fieldsPanel.setOpaque(false);

        // Card number
        JLabel lblCard = fieldLabel("Card Number");
        fieldsPanel.add(lblCard, "wrap");
        cardFilter   = new MaxLenFilter(16);
        tfCardNumber = hintField("0000  0000  0000  0000", cardFilter, 20);
        tfCardNumber.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        fieldsPanel.add(tfCardNumber, "growx, wrap, gapy 0 10");

        // Month / Year / CVV in a row
        JPanel subRow = new JPanel(new MigLayout("insets 0", "[grow][grow][grow]", "[]4[]"));
        subRow.setOpaque(false);

        lblCvv = fieldLabel("CVV");
        JLabel lblMo = fieldLabel("Month  (MM)");
        JLabel lblYr = fieldLabel("Year  (YY)");

        tfExpMonth = hintField("MM", new MaxLenFilter(2), 4);
        tfExpYear  = hintField("YY", new MaxLenFilter(4), 6);
        cvvFilter  = new MaxLenFilter(3);
        tfCvv      = hintField("CVV", cvvFilter, 5);

        tfExpMonth.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        tfExpYear .setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        tfCvv     .setFont(new Font(Font.DIALOG, Font.BOLD, 15));

        subRow.add(lblMo, "wrap");
        subRow.add(tfExpMonth, "growx");
        JPanel yrPanel = new JPanel(new MigLayout("insets 0", "[grow]", "[]4[]")); yrPanel.setOpaque(false);
        yrPanel.add(lblYr, "wrap"); yrPanel.add(tfExpYear, "growx");
        JPanel cvvPanel = new JPanel(new MigLayout("insets 0", "[grow]", "[]4[]")); cvvPanel.setOpaque(false);
        cvvPanel.add(lblCvv, "wrap"); cvvPanel.add(tfCvv, "growx");

        subRow.add(new JPanel() {{ setOpaque(false); }}, ""); // spacer
        subRow.add(yrPanel, ""); subRow.add(cvvPanel, "");

        // rebuild properly
        subRow.removeAll();
        JPanel moCol = col(fieldLabel("Month  (MM)"), tfExpMonth);
        JPanel yrCol = col(fieldLabel("Year  (YY)"), tfExpYear);
        JPanel cvCol = col(lblCvv, tfCvv);
        subRow.add(moCol, "growx");
        subRow.add(yrCol, "growx");
        subRow.add(cvCol, "growx");

        fieldsPanel.add(subRow, "growx");
        p.add(fieldsPanel, "growx, wrap, gapy 0 12");

        // ── Numpad ───────────────────────────────────────────────────────
        QwertyKeyPad kp = new QwertyKeyPad();
        p.add(kp, "grow");

        // ── Wire auto-advance + BIN detection ────────────────────────────
        wireCardNumber();
        wireAutoAdvance(tfExpMonth, 2, tfExpYear);
        wireAutoAdvance(tfExpYear,  4, tfCvv);
        wireCvvComplete();

        return p;
    }

    private JPanel col(JLabel label, JTextField field) {
        JPanel p = new JPanel(new MigLayout("insets 0 4 0 4", "[grow]", "[]4[]"));
        p.setOpaque(false);
        p.add(label, "wrap");
        p.add(field, "growx");
        return p;
    }

    // ── BIN detection & network switching ────────────────────────────────

    private void wireCardNumber() {
        tfCardNumber.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onCardInput(); }
            public void removeUpdate(DocumentEvent e) { onCardInput(); }
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void onCardInput() {
        String raw = tfCardNumber.getText().replaceAll("[^0-9]", "");
        CardNetwork detected = detectNetwork(raw);
        if (detected != CardNetwork.UNKNOWN && detected != activeNet) {
            applyNetwork(detected);
            // Highlight matching button
            CardNetwork[] nets = {CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX, CardNetwork.DISCOVER};
            for (int i = 0; i < nets.length; i++) {
                if (nets[i] == detected) { netBtns[i].setSelected(true); break; }
            }
        }
        // auto-advance when full
        if (raw.length() >= activeNet.cardLen) {
            SwingUtilities.invokeLater(() -> tfExpMonth.requestFocusInWindow());
        }
    }

    private CardNetwork detectNetwork(String digits) {
        if (digits.isEmpty()) return CardNetwork.UNKNOWN;
        if (digits.startsWith("4"))                                          return CardNetwork.VISA;
        if (digits.length() >= 2) {
            int d2 = Integer.parseInt(digits.substring(0, 2));
            if (d2 >= 51 && d2 <= 55)                                       return CardNetwork.MASTERCARD;
            if (d2 == 34 || d2 == 37)                                       return CardNetwork.AMEX;
            if (d2 == 65)                                                    return CardNetwork.DISCOVER;
        }
        if (digits.length() >= 4) {
            int d4 = Integer.parseInt(digits.substring(0, 4));
            if (d4 == 6011 || (d4 >= 6440 && d4 <= 6499))                  return CardNetwork.DISCOVER;
            if (d4 >= 2221 && d4 <= 2720)                                   return CardNetwork.MASTERCARD;
        }
        return CardNetwork.UNKNOWN;
    }

    private void applyNetwork(CardNetwork net) {
        activeNet = net;
        // Update card number max length
        cardFilter.max = net.cardLen;
        // Trim if already over
        String current = tfCardNumber.getText().replaceAll("[^0-9]", "");
        if (current.length() > net.cardLen) {
            tfCardNumber.setText(current.substring(0, net.cardLen));
        }
        // Update CVV max + label
        cvvFilter.max = net.cvvLen;
        lblCvv.setText(net.cvvLabel + "  (" + net.cvvLen + " digits)");
        String cvvHint = net.cvvLen == 4 ? "CID " : "CVV";
        if (tfCvv.getText().isEmpty() || tfCvv.getForeground().equals(HINT_FG)) {
            tfCvv.setForeground(HINT_FG);
            tfCvv.setText(cvvHint);
        }
        tfCvv.repaint();
    }

    // ── Auto-advance wiring ───────────────────────────────────────────────

    private void wireAutoAdvance(JTextField from, int maxLen, JTextField next) {
        from.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (from.getText().length() >= maxLen)
                    SwingUtilities.invokeLater(() -> next.requestFocusInWindow());
            }
            public void removeUpdate(DocumentEvent e) {}
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void wireCvvComplete() {
        tfCvv.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (tfCvv.getText().length() >= activeNet.cvvLen)
                    SwingUtilities.invokeLater(() -> {
                        if (btnSubmitKeyed != null) btnSubmitKeyed.requestFocusInWindow();
                    });
            }
            public void removeUpdate(DocumentEvent e) {}
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    // ── Swipe Card ─────────────────────────────────────────────────────────

    private JPanel buildSwipePanel() {
        JPanel p = new JPanel(new MigLayout("fill, insets 20 32 16 32", "[grow]", "[][][]"));
        p.setOpaque(false);
        p.add(buildSwipeGraphic(), "growx, h 150!, wrap, gapy 0 8");

        JLabel hint = new JLabel("Swipe your card or paste track data below", JLabel.CENTER);
        hint.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        hint.setForeground(new Color(0x77, 0x77, 0x77));
        p.add(hint, "growx, wrap, gapy 0 14");

        tfSwipe = new JPasswordField();
        tfSwipe.setFont(new Font(Font.DIALOG, Font.PLAIN, 18));
        tfSwipe.addActionListener(e -> submitCard());
        if (Application.getInstance().isDevelopmentMode()) {
            tfSwipe.setText("%B4111111111111111^SHAH/RIAR^2103101000000000020000831000000?;4111111111111111=2103101000020000831?");
        }
        p.add(tfSwipe, "growx");
        return p;
    }

    private JPanel buildSwipeGraphic() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cw = 180, ch = 112;
                int cx = (getWidth() - cw) / 2 - 30, cy = (getHeight() - ch) / 2;
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(cx + 4, cy + 4, cw, ch, 14, 14);
                g2.setColor(SWIPE_CARD);
                g2.fillRoundRect(cx, cy, cw, ch, 14, 14);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(cx, cy, cw, ch, 14, 14);
                g2.setColor(SWIPE_STRIPE);
                g2.fillRect(cx, cy + 26, cw, 18);
                g2.setColor(new Color(0xE0, 0xC8, 0x70));
                g2.fillRoundRect(cx + 18, cy + 56, 32, 24, 5, 5);
                g2.setColor(new Color(0xC8, 0xA8, 0x50));
                g2.drawRoundRect(cx + 18, cy + 56, 32, 24, 5, 5);
                g2.drawLine(cx + 30, cy + 56, cx + 30, cy + 80);
                g2.drawLine(cx + 18, cy + 68, cx + 50, cy + 68);
                int ax = cx + cw + 50, ay = cy + ch / 2;
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx + cw + 12, ay, ax, ay);
                int[] xp = {ax, ax - 14, ax - 14};
                int[] yp = {ay, ay - 9,  ay + 9};
                g2.fillPolygon(xp, yp, 3);
                g2.dispose();
            }
        };
    }

    // ── Auth Code ──────────────────────────────────────────────────────────

    private JPanel buildAuthPanel() {
        JPanel p = new JPanel(new MigLayout("insets 16 24 8 24", "[grow]", "[][][grow]"));
        p.setOpaque(false);

        // ── Info notice ──────────────────────────────────────────────────
        JPanel notice = new JPanel(new MigLayout("insets 10 14 10 14, fill", "[24px][grow]", "")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE8, 0xF2, 0xFF));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xB0, 0xCE, 0xF0));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        notice.setOpaque(false);
        JLabel noticeIcon = new JLabel("ℹ");
        noticeIcon.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        noticeIcon.setForeground(ACCENT);
        JLabel noticeText = new JLabel(
            "<html><b>Processing via standalone terminal.</b><br>"
            + "<font color='#555555'>The card was charged on an external terminal. "
            + "Select the card network and enter the authorization code "
            + "provided by the terminal to complete the transaction.</font></html>");
        noticeText.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        notice.add(noticeIcon, "aligny top");
        notice.add(noticeText, "growx");
        p.add(notice, "growx, wrap, gapy 0 12");

        // ── Network icons (same style as Keyed) ──────────────────────────
        JPanel netRow = new JPanel(new MigLayout("insets 0, al center", "[]8[]8[]8[]8[]8[]", ""));
        netRow.setOpaque(false);

        String[] files   = {"visa_card.png","master_card.png","am_ex_card.png","discover_card.png","visa_card.png","master_card.png"};
        String[] tips    = {"Visa","MasterCard","Amex","Discover","Visa Debit","MC Debit"};
        POSToggleButton[] authNetBtns = new POSToggleButton[6];
        ButtonGroup grp = new ButtonGroup();

        for (int i = 0; i < files.length; i++) {
            final int idx = i;
            POSToggleButton nb = new POSToggleButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean sel = isSelected();
                    g2.setColor(sel ? NET_ACTIVE_BG : NET_IDLE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(sel ? NET_ACTIVE_BR : NET_IDLE_BR);
                    g2.setStroke(new BasicStroke(sel ? 1.8f : 1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    if (!sel) g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.55f));
                    super.paintComponent(g2);
                    g2.dispose();
                }
            };
            nb.setIcon(IconFactory.getIcon("/ui_icons/", files[i]));
            nb.setToolTipText(tips[i]);
            nb.setPreferredSize(new Dimension(PosUIManager.getSize(68), PosUIManager.getSize(42)));
            nb.setBorderPainted(false); nb.setContentAreaFilled(false); nb.setFocusPainted(false);
            grp.add(nb); netRow.add(nb);
            authNetBtns[i] = nb;
        }
        btnVisa = authNetBtns[0]; btnMaster = authNetBtns[1];
        btnAmex = authNetBtns[2]; btnDiscover = authNetBtns[3];
        btnDebitVisa = authNetBtns[4]; btnDebitMaster = authNetBtns[5];
        btnVisa.setSelected(true);
        p.add(netRow, "growx, wrap, gapy 0 12");

        // ── Authorization code + keypad ──────────────────────────────────
        JPanel codeArea = new JPanel(new MigLayout("insets 0", "[][grow]", "[][][grow]"));
        codeArea.setOpaque(false);
        codeArea.add(fieldLabel("Authorization Code"), "alignx trailing");
        tfAuthCode = new FocusedTextField();
        tfAuthCode.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        tfAuthCode.setColumns(14);
        codeArea.add(tfAuthCode, "growx, wrap, gapy 0 10");
        codeArea.add(new QwertyKeyPad(), "skip 1, grow");
        p.add(codeArea, "grow");
        return p;
    }

    private POSToggleButton cardBtn(String file) {
        POSToggleButton b = new POSToggleButton();
        b.setIcon(IconFactory.getIcon("/ui_icons/", file));
        return b;
    }

    // ── Footer ─────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.add(new JSeparator(), BorderLayout.NORTH);
        JPanel inner = new JPanel(new MigLayout("al center, insets 10 0 10 0"));
        inner.setOpaque(false);

        btnSubmitGlobal = new PosButton(Messages.getString("SwipeCardDialog.3"));
        btnSubmitGlobal.addActionListener(e -> submitCard());
        btnSubmitKeyed = btnSubmitGlobal;

        PosButton btnCancel = new PosButton(Messages.getString("SwipeCardDialog.4"));
        btnCancel.addActionListener(e -> { setCanceled(true); dispose(); });

        inner.add(btnSubmitGlobal);
        inner.add(btnCancel);
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    // ── Submit ─────────────────────────────────────────────────────────────

    private void submitCard() {
        if (activeTab == Tab.SWIPE) {
            cardString = new String(tfSwipe.getPassword());
        }
        setCanceled(false);
        dispose();
        PaymentType pt = (activeTab == Tab.AUTH_CODE) ? resolvePaymentType() : PaymentType.CREDIT_CARD;
        cardInputListener.cardInputted(this, pt);
    }

    private PaymentType resolvePaymentType() {
        if (btnMaster.isSelected())      return PaymentType.CREDIT_MASTER_CARD;
        if (btnAmex.isSelected())        return PaymentType.CREDIT_AMEX;
        if (btnDiscover.isSelected())    return PaymentType.CREDIT_DISCOVERY;
        if (btnDebitVisa.isSelected())   return PaymentType.DEBIT_VISA;
        if (btnDebitMaster.isSelected()) return PaymentType.DEBIT_MASTER_CARD;
        return PaymentType.CREDIT_VISA;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public Tab    getActiveTab()         { return activeTab; }
    public String getCardNumber()        { return tfCardNumber.getText().replaceAll("[^0-9]", ""); }
    public String getExpMonth()          { return tfExpMonth.getText().replaceAll("[^0-9]", ""); }
    public String getExpYear()           { return tfExpYear.getText().replaceAll("[^0-9]", ""); }
    public String getCvv()               {
        String v = tfCvv.getText();
        return v.equals("CVV") || v.equals("CID ") ? "" : v;
    }
    public String getCardString()        { return cardString; }
    public String getAuthorizationCode() { return tfAuthCode.getText(); }
    public PaymentType getSelectedPaymentType() { return resolvePaymentType(); }

    // ── Helpers ────────────────────────────────────────────────────────────

    // ── Sandbox banner ─────────────────────────────────────────────────────

    private JPanel buildSandboxBanner() {
        JPanel banner = new JPanel(new MigLayout("insets 8 14 8 14, fill", "[22px][grow]", "[][]")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xFF, 0xF3, 0xCD));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0xFF, 0xD0, 0x6A));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        banner.setOpaque(false);

        JLabel icon = new JLabel("⚠");
        icon.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        icon.setForeground(new Color(0x7D, 0x59, 0x04));

        JLabel msg = new JLabel("<html><b>Test Mode Active</b> &mdash; "
            + "Transactions are simulated and no real payments will be collected.</html>");
        msg.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        msg.setForeground(new Color(0x5C, 0x40, 0x00));

        JLabel fix = new JLabel("<html><font color='#7D5904'>To accept live payments, go to "
            + "<b>Back Office → Configure → Card</b> and uncheck <b>Sandbox Mode</b>.</font></html>");
        fix.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));

        banner.add(icon, "spany 2, aligny top");
        banner.add(msg,  "growx, wrap");
        banner.add(fix,  "growx");
        return banner;
    }

    // ── Fill test card ─────────────────────────────────────────────────────

    private void fillTestCard() {
        String[] card = TEST_CARDS[(int) (Math.random() * TEST_CARDS.length)];
        String number = card[0], month = card[1], year = card[2], cvv = card[3];
        String network = card[4];

        // Select matching network icon
        CardNetwork[] nets = {CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX, CardNetwork.DISCOVER};
        for (int i = 0; i < nets.length; i++) {
            if (nets[i].name().equals(network)) {
                applyNetwork(nets[i]);
                netBtns[i].setSelected(true);
                break;
            }
        }

        // Fill fields (filter allows digits only — test numbers are all digits)
        tfCardNumber.setText(number);
        tfExpMonth.setText(month);
        tfExpYear.setText(year);
        tfCvv.setText(cvv);

        switchTab(Tab.KEYED);
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        lbl.setForeground(LABEL_FG);
        return lbl;
    }

    private JTextField hintField(String hint, MaxLenFilter filter, int cols) {
        JTextField f = new JTextField(cols) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(HINT_FG);
                    g2.setFont(getFont());
                    Insets ins = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, ins.left + 2, ins.top + fm.getAscent() + 1);
                    g2.dispose();
                }
            }
        };
        ((AbstractDocument) f.getDocument()).setDocumentFilter(filter);
        return f;
    }

    // ── MaxLenFilter ───────────────────────────────────────────────────────

    static class MaxLenFilter extends DocumentFilter {
        int max;
        MaxLenFilter(int max) { this.max = max; }

        @Override public void insertString(FilterBypass fb, int off, String str, AttributeSet a)
                throws BadLocationException {
            if (str == null) return;
            String onlyDigits = str.replaceAll("[^0-9]", "");
            if (fb.getDocument().getLength() + onlyDigits.length() <= max)
                super.insertString(fb, off, onlyDigits, a);
        }

        @Override public void replace(FilterBypass fb, int off, int len, String str, AttributeSet a)
                throws BadLocationException {
            if (str == null) return;
            String onlyDigits = str.replaceAll("[^0-9]", "");
            int newLen = fb.getDocument().getLength() - len + onlyDigits.length();
            if (newLen <= max) super.replace(fb, off, len, onlyDigits, a);
            else if (fb.getDocument().getLength() - len < max) {
                int allowed = max - (fb.getDocument().getLength() - len);
                super.replace(fb, off, len, onlyDigits.substring(0, Math.min(allowed, onlyDigits.length())), a);
            }
        }
    }

    // ── Flat tab icons ─────────────────────────────────────────────────────

    static class KeyboardIcon implements Icon {
        private static final int S = 28;
        public int getIconWidth()  { return S; }
        public int getIconHeight() { return S; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(1, 5, S - 2, S - 10, 4, 4);
            int ks = 4, kg = 3;
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 4; col++)
                    g2.fillRoundRect(4 + col * (ks + kg), 9 + row * (ks + kg), ks, ks, 1, 1);
            g2.dispose();
        }
    }

    static class SwipeCardIcon implements Icon {
        private static final int S = 28;
        public int getIconWidth()  { return S; }
        public int getIconHeight() { return S; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(1, 4, S - 6, S - 12, 3, 3);
            g2.fillRect(1, 9, S - 6, 4);
            int ax = S - 2, ay = S - 6;
            g2.drawLine(6, ay, ax, ay);
            g2.drawLine(ax - 5, ay - 4, ax, ay);
            g2.drawLine(ax - 5, ay + 4, ax, ay);
            g2.dispose();
        }
    }

    static class AuthCodeIcon implements Icon {
        private static final int S = 28;
        public int getIconWidth()  { return S; }
        public int getIconHeight() { return S; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int[] sx = {S/2, S-3, S-3, S/2, 3,  3};
            int[] sy = {2,   6,   16,  S-2, 16, 6};
            g2.drawPolygon(sx, sy, 6);
            g2.drawLine(8, 13, 12, 17);
            g2.drawLine(12, 17, 20, 9);
            g2.dispose();
        }
    }
}
