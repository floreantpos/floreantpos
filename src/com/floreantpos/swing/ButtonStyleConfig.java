package com.floreantpos.swing;

import java.awt.Color;
import java.awt.Font;

import com.floreantpos.config.AppConfig;

public class ButtonStyleConfig {

    public static final String STYLE_CLASSIC = "classic"; //$NON-NLS-1$
    public static final String STYLE_ACCENT  = "accent";  //$NON-NLS-1$

    // ── 12 predefined accent colors ───────────────────────────────────────
    public static final String[][] ACCENT_COLORS = {
        {"Red",      "#EF4444"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Orange",   "#F97316"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Amber",    "#F59E0B"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Sky Blue", "#0EA5E9"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Green",    "#22C55E"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Lime",     "#84CC16"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Cyan",     "#06B6D4"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Pink",     "#EC4899"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Brown",    "#92400E"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Yellow",   "#EAB308"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Ochre",    "#A16207"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"Violet",   "#8B5CF6"}, //$NON-NLS-1$ //$NON-NLS-2$
    };

    // ── Food category icons [key, emoji, label] ───────────────────────────
    public static final String[][] FOOD_ICONS = {
        {"pizza",     "🍕", "Pizza"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"burger",    "🍔", "Burger"},    //$NON-NLS-1$ //$NON-NLS-2$
        {"chicken",   "🍗", "Chicken"},   //$NON-NLS-1$ //$NON-NLS-2$
        {"seafood",   "🐟", "Seafood"},   //$NON-NLS-1$ //$NON-NLS-2$
        {"salad",     "🥗", "Salad"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"veggie",    "🥦", "Veggie"},    //$NON-NLS-1$ //$NON-NLS-2$
        {"drink",     "🥤", "Drink"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"dessert",   "🍰", "Dessert"},   //$NON-NLS-1$ //$NON-NLS-2$
        {"coffee",    "☕",  "Coffee"},   //$NON-NLS-1$ //$NON-NLS-2$
        {"breakfast", "🍳", "Breakfast"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"sides",     "🍟", "Sides"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"combo",     "📦", "Combo"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"pasta",     "🍝", "Pasta"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"sushi",     "🍣", "Sushi"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"tacos",     "🌮", "Tacos"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"soup",      "🍲", "Soup"},      //$NON-NLS-1$ //$NON-NLS-2$
        {"steak",     "🥩", "Steak"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"sandwich",  "🥪", "Sandwich"},  //$NON-NLS-1$ //$NON-NLS-2$
        {"hotdog",    "🌭", "Hot Dog"},   //$NON-NLS-1$ //$NON-NLS-2$
        {"bakery",    "🥐", "Bakery"},    //$NON-NLS-1$ //$NON-NLS-2$
        {"beer",      "🍺", "Beer"},      //$NON-NLS-1$ //$NON-NLS-2$
        {"wine",      "🍷", "Wine"},      //$NON-NLS-1$ //$NON-NLS-2$
        {"cocktail",  "🍹", "Cocktail"},  //$NON-NLS-1$ //$NON-NLS-2$
        {"icecream",  "🍦", "Ice Cream"}, //$NON-NLS-1$ //$NON-NLS-2$
        {"bbq",       "🍖", "BBQ"},       //$NON-NLS-1$ //$NON-NLS-2$
        {"donut",     "🍩", "Donut"},     //$NON-NLS-1$ //$NON-NLS-2$
        {"kids",      "🍭", "Kids"},      //$NON-NLS-1$ //$NON-NLS-2$
        {"product",   "🍽", "Default"},   //$NON-NLS-1$ //$NON-NLS-2$
    };

    // ── 12 Material Symbol icons shown in the Config tab ──────────────────
    // inventory_2 replaces tapas (less common)
    public static final String[][] CONFIG_ICONS = {
        {"local_pizza",   "Pizza"},         //$NON-NLS-1$
        {"lunch_dining",  "Burger"},        //$NON-NLS-1$
        {"kebab_dining",  "Chicken"},       //$NON-NLS-1$
        {"set_meal",      "Seafood"},       //$NON-NLS-1$
        {"eco",           "Salad"},         //$NON-NLS-1$
        {"nutrition",     "Vegetarian"},    //$NON-NLS-1$
        {"local_drink",   "Drinks"},        //$NON-NLS-1$
        {"icecream",      "Dessert"},       //$NON-NLS-1$
        {"local_cafe",    "Coffee / Tea"},  //$NON-NLS-1$
        {"egg_alt",       "Breakfast"},     //$NON-NLS-1$
        {"inventory_2",   "Sides"},         //$NON-NLS-1$
        {"room_service",  "Combo / Special"}, //$NON-NLS-1$
    };

    private static final String KEY_STYLE  = "btn.style";          //$NON-NLS-1$
    private static final String KEY_ACCENT = "btn.default.accent"; //$NON-NLS-1$
    private static final String KEY_ICON   = "btn.default.icon";   //$NON-NLS-1$

    // ── Style ─────────────────────────────────────────────────────────────
    public static String  getStyle()        { return AppConfig.getString(KEY_STYLE, STYLE_CLASSIC); }
    public static void    setStyle(String s){ AppConfig.put(KEY_STYLE, s); }
    public static boolean isAccentMode()    { return STYLE_ACCENT.equals(getStyle()); }

    // ── Default accent hex ────────────────────────────────────────────────
    public static String getDefaultAccentHex()           { return AppConfig.getString(KEY_ACCENT, ACCENT_COLORS[0][1]); }
    public static void   setDefaultAccentHex(String hex) { AppConfig.put(KEY_ACCENT, hex); }
    public static Color  getDefaultAccentColor()         { return parseHex(getDefaultAccentHex()); }

    // ── Default icon key ──────────────────────────────────────────────────
    public static String getDefaultIconKey()             { return AppConfig.getString(KEY_ICON, "product"); } //$NON-NLS-1$
    public static void   setDefaultIconKey(String k)     { AppConfig.put(KEY_ICON, k); }

    // ── Helpers ───────────────────────────────────────────────────────────
    public static Color parseHex(String hex) {
        if (hex == null || hex.length() < 7) return new Color(0xEF, 0x44, 0x44);
        try {
            return new Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
        } catch (Exception e) { return new Color(0xEF, 0x44, 0x44); }
    }

    public static String getIconEmoji(String key) {
        for (String[] ic : FOOD_ICONS) if (ic[0].equals(key)) return ic[1];
        return FOOD_ICONS[FOOD_ICONS.length - 1][1];
    }

    public static Font getEmojiFont(float size) {
        for (String name : new String[]{"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji"}) {
            java.awt.Font f = new java.awt.Font(name, java.awt.Font.PLAIN, (int) size);
            if (!f.getFamily().equals("Dialog")) return f.deriveFont(size); //$NON-NLS-1$
        }
        return new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, (int) size).deriveFont(size);
    }
}
