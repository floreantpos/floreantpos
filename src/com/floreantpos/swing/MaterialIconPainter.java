package com.floreantpos.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders Google Material Symbol icons (viewBox 0 -960 960 960) using the
 * real SVG path data. Paths are parsed once and cached.
 */
public final class MaterialIconPainter {

    private MaterialIconPainter() {}

    // ── SVG path data (Google Material Symbols Rounded) ────────────────────
    private static final Map<String, String> PATHS = new HashMap<String, String>();
    static {
        PATHS.put("local_pizza", //$NON-NLS-1$
            "M480-840q88 0 179 30t161 84q16 12 24 29t8 35q0 11-3.5 22.5T838-617L547-180" +
            "q-12 18-30 27t-37 9q-19 0-37-9t-30-27L122-617q-7-11-10-22t-3-22q0-18 8-35" +
            "t24-29q70-53 160.5-84T480-840Zm0 80q-83 0-153 28t-139 70l292 438 292-438" +
            "q-69-43-138.5-70.5T480-760ZM380-560q25 0 42.5-17.5T440-620q0-25-17.5-42.5" +
            "T380-680q-25 0-42.5 17.5T320-620q0 25 17.5 42.5T380-560Zm100 200q25 0 42.5-17.5" +
            "T540-420q0-25-17.5-42.5T480-480q-25 0-42.5 17.5T420-420q0 25 17.5 42.5T480-360Zm0 136Z"); //$NON-NLS-1$

        PATHS.put("lunch_dining", //$NON-NLS-1$
            "M160-120q-33 0-56.5-23.5T80-200v-80q0-17 11.5-28.5T120-320h720q17 0 28.5 11.5" +
            "T880-280v80q0 33-23.5 56.5T800-120H160Zm0-120v40h640v-40H160Zm320-180q-36 0-57 20" +
            "t-77 20q-56 0-76-20t-56-20q-28 0-45.5 13.5T121-385q-16 5-28.5-6.5T80-420q0-17 11.5-29.5" +
            "T119-470q17-9 37-19.5t58-10.5q56 0 76 20t56 20q36 0 57-20t77-20q56 0 77 20t57 20" +
            "q36 0 56-20t76-20q36 0 57 10t38 19q16 8 27.5 21t11.5 30q0 17-12 28.5t-28 6.5" +
            "q-29-8-45.5-21.5T750-420q-36 0-58 20t-78 20q-56 0-77-20t-57-20Zm0-420q74 0 145.5 13.5" +
            "t128 42q56.5 28.5 91.5 74T880-600q0 17-11.5 28.5T840-560H120q-17 0-28.5-11.5T80-600" +
            "q0-65 35-110.5t91.5-74q56.5-28.5 128-42T480-840Zm0 80q-124 0-207.5 31T166-640h628" +
            "q-23-58-106.5-89T480-760Zm0 520Zm0-400Z"); //$NON-NLS-1$

        PATHS.put("kebab_dining", //$NON-NLS-1$
            "M280-40q-13 0-21.5-8.5T250-70v-130h-30q-42 0-71-29t-29-71q0-42 29-71t71-29h30v-40" +
            "h-90q-17 0-28.5-11.5T120-480v-120q0-17 11.5-28.5T160-640h90v-40h-30q-42 0-71-29" +
            "t-29-71q0-42 29-71t71-29h30v-10q0-13 8.5-21.5T280-920q13 0 21.5 8.5T310-890v10h30" +
            "q42 0 71 29t29 71q0 42-29 71t-71 29h-30v40h90q17 0 28.5 11.5T440-600v120q0 17-11.5 28.5" +
            "T400-440h-90v40h30q42 0 71 29t29 71q0 42-29 71t-71 29h-30v130q0 13-8.5 21.5T280-40Zm400 0" +
            "q-13 0-21.5-8.5T650-70v-130h-30q-42 0-71-29t-29-71q0-42 29-71t71-29h30v-40h-90q-17 0-28.5-11.5" +
            "T520-480v-120q0-17 11.5-28.5T560-640h90v-40h-30q-42 0-71-29t-29-71q0-42 29-71t71-29h30v-10" +
            "q0-13 8.5-21.5T680-920q13 0 21.5 8.5T710-890v10h30q42 0 71 29t29 71q0 42-29 71t-71 29h-30v40" +
            "h90q17 0 28.5 11.5T840-600v120q0 17-11.5 28.5T800-440h-90v40h30q42 0 71 29t29 71q0 42-29 71" +
            "t-71 29h-30v130q0 13-8.5 21.5T680-40ZM220-760h120q8 0 14-6t6-14q0-8-6-14t-14-6H220q-8 0-14 6" +
            "t-6 14q0 8 6 14t14 6Zm400 0h120q8 0 14-6t6-14q0-8-6-14t-14-6H620q-8 0-14 6t-6 14q0 8 6 14t14 6Z" +
            "M200-520h160v-40H200v40Zm400 0h160v-40H600v40ZM220-280h120q8 0 14-6t6-14q0-8-6-14t-14-6H220" +
            "q-8 0-14 6t-6 14q0 8 6 14t14 6Zm400 0h120q8 0 14-6t6-14q0-8-6-14t-14-6H620q-8 0-14 6t-6 14" +
            "q0 8 6 14t14 6Z"); //$NON-NLS-1$

        PATHS.put("set_meal", //$NON-NLS-1$
            "M120-360q-33 0-56.5-23.5T40-440v-360q0-33 23.5-56.5T120-880h720q33 0 56.5 23.5" +
            "T920-800v360q0 33-23.5 56.5T840-360H120Zm0-440v360h720v-360H120Zm692 544-659 34" +
            "q-13 1-22-7t-10-21q-1-13 7.5-22t21.5-10l659-34q13-1 22 7t10 21q1 13-7.5 22T812-256Z" +
            "m-2 135H150q-13 0-21.5-8.5T120-151q0-13 8.5-21.5T150-181h660q13 0 21.5 8.5T840-151" +
            "q0 13-8.5 21.5T810-121ZM410-500q74 0 142.5-26T672-606q5 34 31 55t61 27q11 2 23.5.5" +
            "T800-542v-156q0-17-12.5-18.5t-23.5.5q-35 7-61 28t-31 56q-53-52-120.5-80T410-740" +
            "q-79 0-142 23.5T152-633q-1 1-5 13 0 4 5 13 53 60 116 83.5T410-500ZM120-800v360-360Z"); //$NON-NLS-1$

        PATHS.put("eco", //$NON-NLS-1$
            "M216-176q-45-45-70.5-104T120-402q0-63 24-124.5T222-642q60-60 169.5-91T675-759" +
            "q26 1 48 11t39 27q17 17 27 39.5t11 48.5q2 82-4.5 151.5t-21 125.5q-14.5 56-37 99.5" +
            "T684-182q-53 53-112.5 77.5T450-80q-65 0-127-25.5T216-176Zm112-16q29 17 59.5 24.5" +
            "T450-160q46 0 91-18.5t86-59.5q18-18 36.5-50.5t32-85Q709-426 716-500.5t2-177.5" +
            "q-49-2-110.5-1.5T485-670q-61 9-116 29t-90 55q-45 45-62 89t-17 85q0 59 22.5 103.5" +
            "T262-246q42-80 111-153.5T534-520q-72 63-125.5 142.5T328-192Zm0 0Zm0 0Z"); //$NON-NLS-1$

        PATHS.put("nutrition", //$NON-NLS-1$
            "M480-120q-117 0-198.5-81.5T200-400q0-94 55.5-168.5T401-669q-20-5-39-14.5T328-708" +
            "q-33-33-42.5-78.5T281-879q47-5 92.5 4.5T452-832q23 23 33.5 52t13.5 61q13-31 31.5-58.5" +
            "T572-828q11-11 28-11t28 11q11 11 11 28t-11 28q-22 22-39 48.5T564-667q88 28 142 101.5" +
            "T760-400q0 117-81.5 198.5T480-120Zm0-80q83 0 141.5-58.5T680-400q0-83-58.5-141.5" +
            "T480-600q-83 0-141.5 58.5T280-400q0 83 58.5 141.5T480-200Zm0-200Z"); //$NON-NLS-1$

        PATHS.put("local_drink", //$NON-NLS-1$
            "M279-80q-31 0-53.5-20.5T200-151l-69-630q-5-40 22-69.5t67-29.5h520q40 0 67 29.5" +
            "t22 69.5l-69 630q-3 30-25.5 50.5T681-80H279Zm-43-480 44 400h400l44-400H236Zm-10-80" +
            "h508l16-160H210l16 160Zm254 360q-14 0-24-10t-10-24q0-15 8.5-34.5T480-393q17 25 25.5 44.5" +
            "T514-314q0 14-10 24t-24 10Zm0 80q48 0 81-33t33-81q0-47-27.5-91T511-483q-6-8-14.5-11.5" +
            "T480-498q-8 0-16.5 3.5T449-483q-28 34-55.5 78T366-314q0 48 33 81t81 33Zm-244 40h488-488Z"); //$NON-NLS-1$

        PATHS.put("icecream", //$NON-NLS-1$
            "M120-560q0-51 29.5-92t74.5-58q18-91 89.5-150.5T480-920q95 0 166.5 59.5T736-710" +
            "q45 17 74.5 58t29.5 92q0 75-53 119t-119 41L517-108q-5 11-14.5 16T482-87q-11 0-21-5" +
            "t-15-16L294-400q-71 3-122.5-41T120-560Zm160 80q15 0 29.5-5t26.5-17l22-22 26 16" +
            "q21 14 45.5 21t50.5 7q26 0 50.5-7t45.5-21l26-16 22 22q12 12 26.5 17t29.5 5" +
            "q33 0 56.5-23.5T760-560q0-30-19-52.5T692-640l-30-4-2-32q-5-69-57-116.5T480-840" +
            "q-71 0-123 47.5T300-676l-2 32-30 6q-30 6-49 27t-19 51q0 33 23.5 56.5T280-480Z" +
            "m202 266 108-210q-24 12-52 18t-58 6q-27 0-54.5-6T372-424l110 210Zm-2-446Z"); //$NON-NLS-1$

        PATHS.put("local_cafe", //$NON-NLS-1$
            "M200-120q-17 0-28.5-11.5T160-160q0-17 11.5-28.5T200-200h560q17 0 28.5 11.5" +
            "T800-160q0 17-11.5 28.5T760-120H200Zm120-160q-66 0-113-47t-47-113v-320q0-33 23.5-56.5" +
            "T240-840h560q33 0 56.5 23.5T880-760v120q0 33-23.5 56.5T800-560h-80v120q0 66-47 113" +
            "t-113 47H320Zm0-80h240q33 0 56.5-23.5T640-440v-320H240v320q0 33 23.5 56.5T320-360Z" +
            "m400-280h80v-120h-80v120ZM320-360h-80 400-320Z"); //$NON-NLS-1$

        PATHS.put("egg_alt", //$NON-NLS-1$
            "M640-80q-67 0-101.5-22.5T480-150q-19-20-36.5-35T399-200q-45 0-100-15.5t-103.5-51" +
            "Q147-302 114-359T80-499q-2-167 82.5-274T399-880q71 0 120 20.5t84.5 51.5q35.5 31 60 68.5" +
            "T710-667q12 20 24 36.5t26 30.5q60 60 90 105t30 136q0 120-74.5 199.5T640-80Zm0-80" +
            "q57 0 108.5-56.5T800-359q0-66-19.5-97T704-544q-21-20-37.5-44.5T633-639q-41-65-87-113" +
            "t-147-48q-129 0-185 92.5T160-500q1 67 29 110t66.5 67.5Q294-298 334-289t65 9q51 0 82 24.5" +
            "t51 45.5q22 23 42.5 36.5T640-160ZM480-340q58 0 99-41t41-99q0-58-41-99t-99-41q-58 0-99 41" +
            "t-41 99q0 58 41 99t99 41Zm-1-140Z"); //$NON-NLS-1$

        PATHS.put("tapas", //$NON-NLS-1$
            "M280-40q-17 0-28.5-11.5T240-80v-320h-80q-42 0-71-29t-29-71q0-42 29-71t71-29h80v-40" +
            "h-80q-42 0-71-29t-29-71q0-42 29-71t71-29h80v-40q0-17 11.5-28.5T280-920q17 0 28.5 11.5" +
            "T320-880v40h80q42 0 71 29t29 71q0 42-29 71t-71 29h-80v40h80q42 0 71 29t29 71q0 42-29 71" +
            "t-71 29h-80v320q0 17-11.5 28.5T280-40ZM160-480h240q8 0 14-6t6-14q0-8-6-14t-14-6H160" +
            "q-8 0-14 6t-6 14q0 8 6 14t14 6Zm0-240h240q8 0 14-6t6-14q0-8-6-14t-14-6H160q-8 0-14 6" +
            "t-6 14q0 8 6 14t14 6Zm520 600v-286q-53-14-86.5-56.5T560-560v-320q0-17 11.5-28.5" +
            "T600-920h240q17 0 28.5 11.5T880-880v320q0 55-33.5 97.5T760-406v286h40q17 0 28.5 11.5" +
            "T840-80q0 17-11.5 28.5T800-40H640q-17 0-28.5-11.5T600-80q0-17 11.5-28.5T640-120h40Z" +
            "m40-360q33 0 56.5-23.5T800-560v-80H640v80q0 33 23.5 56.5T720-480Zm-80-240h160v-120H640v120Z"); //$NON-NLS-1$

        PATHS.put("room_service", //$NON-NLS-1$
            "M120-200q-17 0-28.5-11.5T80-240q0-17 11.5-28.5T120-280h720q17 0 28.5 11.5T880-240" +
            "q0 17-11.5 28.5T840-200H120Zm0-120v-40q0-128 78.5-226T400-710v-10q0-33 23.5-56.5" +
            "T480-800q33 0 56.5 23.5T560-720v10q124 26 202 124t78 226v40H120Zm82-80h556" +
            "q-14-104-93-172t-185-68q-106 0-184.5 68T202-400Zm278 0Z"); //$NON-NLS-1$

        PATHS.put("inventory_2", //$NON-NLS-1$
            "M200-80q-33 0-56.5-23.5T120-160v-451q-18-11-29-28.5T80-680v-120q0-33 23.5-56.5" +
            "T160-880h640q33 0 56.5 23.5T880-800v120q0 23-11 40.5T840-611v451q0 33-23.5 56.5" +
            "T760-80H200Zm0-520v440h560v-440H200Zm-40-80h640v-120H160v120Zm240 280h160q17 0 28.5-11.5" +
            "T600-440q0-17-11.5-28.5T560-480H400q-17 0-28.5 11.5T360-440q0 17 11.5 28.5T400-400Z" +
            "m80 20Z"); //$NON-NLS-1$
    }

    // ── Cached parsed paths ────────────────────────────────────────────────
    private static final Map<String, Path2D.Double> CACHE = new HashMap<String, Path2D.Double>();

    /** Draw icon centred at (cx,cy) inside a square of side sz. */
    public static boolean paint(Graphics2D g2, String name, int cx, int cy, int sz) {
        return paint(g2, name, cx, cy, sz, g2.getColor());
    }

    public static boolean paint(Graphics2D g2, String name, int cx, int cy, int sz, Color color) {
        String d = PATHS.get(name);
        if (d == null) return false;

        Path2D.Double path;
        synchronized (CACHE) {
            path = CACHE.get(name);
            if (path == null) { path = parseSvgPath(d); CACHE.put(name, path); }
        }

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // SVG viewBox 0 -960 960 960 → sz×sz centred at (cx,cy)
        double scale = sz / 960.0;
        AffineTransform at = new AffineTransform();
        at.translate(cx - sz / 2.0, cy - sz / 2.0);
        at.scale(scale, scale);
        at.translate(0, 960);

        g.transform(at);
        g.setColor(color);
        g.fill(path);
        g.dispose();
        return true;
    }

    // ── SVG path parser ───────────────────────────────────────────────────

    private static Path2D.Double parseSvgPath(String d) {
        Path2D.Double path = new Path2D.Double();
        Pattern tok = Pattern.compile(
            "[MmLlHhVvQqTtZz]|[-+]?(?:[0-9]*\\.)?[0-9]+(?:[eE][-+]?[0-9]+)?"); //$NON-NLS-1$
        Matcher m = tok.matcher(d);
        List<String> tokens = new ArrayList<String>();
        while (m.find()) tokens.add(m.group());

        int i = 0; int sz = tokens.size();
        double cx = 0, cy = 0, mx = 0, my = 0, lqcx = 0, lqcy = 0;
        boolean lastQ = false;
        char cmd = 'M';

        while (i < sz) {
            String t = tokens.get(i);
            char c0 = t.charAt(0);
            if (Character.isLetter(c0)) { cmd = c0; i++; }
            if (i >= sz && cmd != 'Z' && cmd != 'z') break;

            switch (cmd) {
                case 'Z': case 'z': path.closePath(); cx=mx; cy=my; lastQ=false; break;
                case 'M': { double x=g(tokens,i++),y=g(tokens,i++); path.moveTo(x,y); cx=mx=x; cy=my=y; lastQ=false; cmd='L'; break; }
                case 'm': { double x=cx+g(tokens,i++),y=cy+g(tokens,i++); path.moveTo(x,y); cx=mx=x; cy=my=y; lastQ=false; cmd='l'; break; }
                case 'L': { double x=g(tokens,i++),y=g(tokens,i++); path.lineTo(x,y); cx=x; cy=y; lastQ=false; break; }
                case 'l': { double x=cx+g(tokens,i++),y=cy+g(tokens,i++); path.lineTo(x,y); cx=x; cy=y; lastQ=false; break; }
                case 'H': { double x=g(tokens,i++); path.lineTo(x,cy); cx=x; lastQ=false; break; }
                case 'h': { double x=cx+g(tokens,i++); path.lineTo(x,cy); cx=x; lastQ=false; break; }
                case 'V': { double y=g(tokens,i++); path.lineTo(cx,y); cy=y; lastQ=false; break; }
                case 'v': { double y=cy+g(tokens,i++); path.lineTo(cx,y); cy=y; lastQ=false; break; }
                case 'Q': { double x1=g(tokens,i++),y1=g(tokens,i++),x=g(tokens,i++),y=g(tokens,i++); path.quadTo(x1,y1,x,y); lqcx=x1;lqcy=y1;cx=x;cy=y;lastQ=true; break; }
                case 'q': { double x1=cx+g(tokens,i++),y1=cy+g(tokens,i++),x=cx+g(tokens,i++),y=cy+g(tokens,i++); path.quadTo(x1,y1,x,y); lqcx=x1;lqcy=y1;cx=x;cy=y;lastQ=true; break; }
                case 'T': { double tcx=lastQ?2*cx-lqcx:cx,tcy=lastQ?2*cy-lqcy:cy,x=g(tokens,i++),y=g(tokens,i++); path.quadTo(tcx,tcy,x,y); lqcx=tcx;lqcy=tcy;cx=x;cy=y;lastQ=true; break; }
                case 't': { double tcx=lastQ?2*cx-lqcx:cx,tcy=lastQ?2*cy-lqcy:cy,x=cx+g(tokens,i++),y=cy+g(tokens,i++); path.quadTo(tcx,tcy,x,y); lqcx=tcx;lqcy=tcy;cx=x;cy=y;lastQ=true; break; }
                case 'C': { double x1=g(tokens,i++),y1=g(tokens,i++),x2=g(tokens,i++),y2=g(tokens,i++),x=g(tokens,i++),y=g(tokens,i++); path.curveTo(x1,y1,x2,y2,x,y); cx=x;cy=y;lastQ=false; break; }
                case 'c': { double x1=cx+g(tokens,i++),y1=cy+g(tokens,i++),x2=cx+g(tokens,i++),y2=cy+g(tokens,i++),x=cx+g(tokens,i++),y=cy+g(tokens,i++); path.curveTo(x1,y1,x2,y2,x,y); cx=x;cy=y;lastQ=false; break; }
                default: i++; break;
            }
        }
        return path;
    }

    private static double g(List<String> tokens, int idx) {
        return Double.parseDouble(tokens.get(idx));
    }
}
