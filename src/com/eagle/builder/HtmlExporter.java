package com.eagle.builder;

import java.util.Map;

/**
 * Converts a tree of VisualComponent into clean HTML + CSS output.
 * Each component gets a unique class (its id) carrying its styles, so the
 * generated CSS file stays organized and human-readable/editable afterwards.
 */
public class HtmlExporter {

    public static String toHtml(VisualComponent root, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>").append(escape(title)).append("</title>\n");
        sb.append("    <link rel=\"stylesheet\" href=\"style.css\">\n");
        sb.append("</head>\n<body>\n");
        for (VisualComponent child : root.getChildren()) {
            appendComponent(sb, child, 1);
        }
        sb.append("    <script src=\"script.js\"></script>\n");
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

        private static void appendComponent(StringBuilder sb, VisualComponent c, int depth) {
        String indent = repeat(" ", depth);
        String cls = "comp-" + c.getId();

        switch (c.getType()) {

            // ==================== TEXT & TYPOGRAPHY ====================
            case HEADING:
                sb.append(indent).append("<h2 class=\"").append(cls).append("\">")
                .append(escape(c.getText())).append("</h2>\n");
                break;

            case SUBHEADING:
                sb.append(indent).append("<h3 class=\"").append(cls).append("\">")
                .append(escape(c.getText())).append("</h3>\n");
                break;

            case PARAGRAPH:
            case RICH_TEXT:
                sb.append(indent).append("<p class=\"").append(cls).append("\">")
                .append(escape(c.getText())).append("</p>\n");
                break;

            case QUOTE:
                sb.append(indent).append("<blockquote class=\"").append(cls).append("\">")
                .append(escape(c.getText())).append("</blockquote>\n");
                break;

            // ==================== INTERACTIVE & BUTTONS ====================
            case BUTTON:
            case ICON_BUTTON:
                sb.append(indent).append("<button class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">").append(escape(c.getText())).append("</button>\n");
                break;

            case LINK:
                sb.append(indent).append("<a class=\"").append(cls).append("\"");
                if (!c.getAttributes().containsKey("href")) {
                    sb.append(" href=\"#\"");
                }
                appendAttrs(sb, c);
                sb.append(">").append(escape(c.getText())).append("</a>\n");
                break;

            // ==================== FORM ELEMENTS ====================
            case INPUT:
                sb.append(indent).append("<input class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case TEXTAREA:
                sb.append(indent).append("<textarea class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">").append(escape(c.getText())).append("</textarea>\n");
                break;

            case SELECT:
            case MULTISELECT:
                sb.append(indent).append("<select class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                sb.append(indent).append("  <option value=\"\">اختر...</option>\n");
                sb.append(indent).append("</select>\n");
                break;

            case CHECKBOX:
                sb.append(indent).append("<input type=\"checkbox\" class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case RADIO:
                sb.append(indent).append("<input type=\"radio\" class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case TOGGLE:
                sb.append(indent).append("<input type=\"checkbox\" class=\"").append(cls).append(" toggle\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case DATE_PICKER:
                sb.append(indent).append("<input type=\"date\" class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case FILE_UPLOAD:
                sb.append(indent).append("<input type=\"file\" class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case SLIDER:
                sb.append(indent).append("<input type=\"range\" class=\"").append(cls).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case RATING:
                sb.append(indent).append("<div class=\"").append(cls).append(" rating\">★★★★★</div>\n");
                break;

            // ==================== MEDIA ====================
            case IMAGE:
                sb.append(indent).append("<img class=\"").append(cls).append("\"");
                if (!c.getAttributes().containsKey("src")) {
                    sb.append(" src=\"https://via.placeholder.com/600x340/6366f1/ffffff?text=Image\"");
                }
                sb.append(" alt=\"").append(escape(c.getAttributes().getOrDefault("alt", c.getText()))).append("\"");
                appendAttrs(sb, c);
                sb.append(">\n");
                break;

            case VIDEO:
                sb.append(indent).append("<video class=\"").append(cls).append("\"");
                if (!c.getAttributes().containsKey("src")) {
                    sb.append(" src=\"https://www.w3schools.com/html/mov_bbb.mp4\"");
                }
                appendAttrs(sb, c);
                sb.append(" controls>\n");
                String vSrc = c.getAttributes().get("src");
                if (vSrc != null && !vSrc.isEmpty()) {
                    sb.append(indent).append("  <source src=\"").append(escape(vSrc)).append("\">\n");
                }
                sb.append(indent).append("</video>\n");
                break;

            case AUDIO:
                sb.append(indent).append("<audio class=\"").append(cls).append("\"");
                if (!c.getAttributes().containsKey("src")) {
                    sb.append(" src=\"https://www.w3schools.com/html/horse.mp3\"");
                }
                appendAttrs(sb, c);
                sb.append(" controls>\n");
                String aSrc = c.getAttributes().get("src");
                if (aSrc != null && !aSrc.isEmpty()) {
                    sb.append(indent).append("  <source src=\"").append(escape(aSrc)).append("\">\n");
                }
                sb.append(indent).append("</audio>\n");
                break;

            case CAROUSEL:
            case GALLERY:
                sb.append(indent).append("<div class=\"").append(cls).append(" carousel\">\n");
                for (VisualComponent child : c.getChildren()) {
                    appendComponent(sb, child, depth + 1);
                }
                sb.append(indent).append("</div>\n");
                break;

            // ==================== LAYOUT & CONTAINERS ====================
            case CONTAINER:
            case ROW:
            case SECTION:
            case CARD:
            case HEADER:
            case FOOTER:
            case ASIDE:
            case HERO:
            case FEATURE_SECTION:
            case FORM:
            case TESTIMONIALS:
            case PRICING:
            case FAQ:
            case STATS:
            case FLEXBOX:
            case STACK:
            case WRAP:
            case HEADER_BLOCK:
            case HERO_BLOCK:
            case FEATURES_BLOCK:
            case PRICING_BLOCK:
            case TESTIMONIALS_BLOCK:
            case FAQ_BLOCK:
            case CONTACT_BLOCK:
            case FOOTER_BLOCK:
            case TEAM_BLOCK:
            case STATS_BLOCK:
            case CTA_BLOCK:
            case NEWSLETTER_BLOCK:
                sb.append(indent).append("<div class=\"").append(cls).append("\">\n");
                for (VisualComponent child : c.getChildren()) {
                    appendComponent(sb, child, depth + 1);
                }
                sb.append(indent).append("</div>\n");
                break;

            case GRID:
                sb.append(indent).append("<div class=\"").append(cls).append(" grid\">\n");
                for (VisualComponent child : c.getChildren()) {
                    appendComponent(sb, child, depth + 1);
                }
                sb.append(indent).append("</div>\n");
                break;

            case DIVIDER:
                sb.append(indent).append("<hr class=\"").append(cls).append("\">\n");
                break;

            case SPACER:
                sb.append(indent).append("<div class=\"").append(cls).append(" spacer\"></div>\n");
                break;

            // ==================== NAVIGATION ====================
            case NAVIGATION:
            case TABS:
            case BREADCRUMB:
            case DROPDOWN:
            case SIDEBAR:
                sb.append(indent).append("<nav class=\"").append(cls).append("\">\n");
                for (VisualComponent child : c.getChildren()) {
                    appendComponent(sb, child, depth + 1);
                }
                sb.append(indent).append("</nav>\n");
                break;

            case PAGINATION:
                sb.append(indent).append("<div class=\"").append(cls).append(" pagination\">1 2 3 …</div>\n");
                break;

            case STEPPER:
                sb.append(indent).append("<div class=\"").append(cls).append(" stepper\">1 → 2 → 3</div>\n");
                break;

            // ==================== DATA DISPLAY ====================
            case TABLE:
            case DATA_TABLE:
                sb.append(indent).append("<table class=\"").append(cls).append("\">\n");
                sb.append(indent).append("  <thead><tr><th>Header</th></tr></thead>\n");
                sb.append(indent).append("  <tbody><tr><td>Data</td></tr></tbody>\n");
                sb.append(indent).append("</table>\n");
                break;

            case LIST:
                sb.append(indent).append("<ul class=\"").append(cls).append("\">\n");
                for (VisualComponent child : c.getChildren()) {
                    sb.append(indent + "  <li>");
                    appendComponent(sb, child, 0);
                    sb.append("</li>\n");
                }
                sb.append(indent).append("</ul>\n");
                break;

            case ACCORDION:
                sb.append(indent).append("<details class=\"").append(cls).append("\">\n");
                sb.append(indent).append("  <summary>").append(escape(c.getText())).append("</summary>\n");
                for (VisualComponent child : c.getChildren()) {
                    appendComponent(sb, child, depth + 1);
                }
                sb.append(indent).append("</details>\n");
                break;

            // ==================== FEEDBACK & OVERLAYS ====================
            case MODAL:
            case DRAWER:
            case POPOVER:
                sb.append(indent).append("<div class=\"").append(cls).append(" modal\">\n");
                for (VisualComponent child : c.getChildren()) {
                    appendComponent(sb, child, depth + 1);
                }
                sb.append(indent).append("</div>\n");
                break;

            case ALERT:
            case TOAST:
                sb.append(indent).append("<div class=\"").append(cls).append(" alert\">")
                  .append(escape(c.getText())).append("</div>\n");
                break;

            case PROGRESS:
                sb.append(indent).append("<progress class=\"").append(cls).append("\" value=\"60\" max=\"100\"></progress>\n");
                break;

            case SPINNER:
                sb.append(indent).append("<div class=\"").append(cls).append(" spinner\"></div>\n");
                break;

            case TOOLTIP:
                sb.append(indent).append("<span class=\"").append(cls).append("\" title=\"")
                  .append(escape(c.getText())).append("\">Hover me</span>\n");
                break;

            // ==================== INDICATORS & MISC ====================
            case BADGE:
            case CHIP:
                sb.append(indent).append("<span class=\"").append(cls).append(" badge\">")
                  .append(escape(c.getText())).append("</span>\n");
                break;

            case AVATAR:
                sb.append(indent).append("<img class=\"").append(cls).append(" avatar\" src=\"https://via.placeholder.com/48\" alt=\"Avatar\">\n");
                break;

            case ICON:
                String icon = c.getIcon();
                sb.append(indent).append("<span class=\"").append(cls).append(" icon\">")
                  .append(icon).append("</span>\n");
                break;

            case MAP:
            case CHART:
            case EMBED:
            case QR_CODE:
            case CALENDAR:
                sb.append(indent).append("<div class=\"").append(cls).append("\">[")
                  .append(c.getType().getLabel()).append("]</div>\n");
                break;

            // Default fallback
            default:
                sb.append(indent).append("<div class=\"").append(cls).append(" unknown\">")
                  .append(escape(c.getText() != null ? c.getText() : c.getType().getLabel()))
                  .append("</div>\n");
                break;
        }
    }

    private static void appendAttrs(StringBuilder sb, VisualComponent c) {
        for (Map.Entry<String, String> attr : c.getAttributes().entrySet()) {
            sb.append(" ").append(attr.getKey()).append("=\"").append(escape(attr.getValue())).append("\"");
        }
    }

    public static String toCss(VisualComponent root) {
        StringBuilder sb = new StringBuilder();
        sb.append("/* Generated by Eagel Visual Builder */\n");
        sb.append("body { margin: 0; font-family: -apple-system, Segoe UI, Arial, sans-serif; position: relative; min-height: 100vh; }\n\n");
        collectCss(root, sb);
        return sb.toString();
    }

    private static void collectCss(VisualComponent c, StringBuilder sb) {
        int zIndex = 1;
        for (VisualComponent child : c.getChildren()) {
            writeCssRule(child, sb, zIndex++);
            collectCss(child, sb);
        }
    }

    private static void writeCssRule(VisualComponent child, StringBuilder sb, int zIndex) {
        sb.append(".comp-").append(child.getId()).append(" {\n");
        String xs = child.getAttributes().get("x");
        String ys = child.getAttributes().get("y");
        boolean isAbsolute = xs != null && ys != null;
        if (isAbsolute) {
            sb.append("    position: absolute;\n");
            sb.append("    z-index: ").append(zIndex).append(";\n");
            sb.append("    left: ").append(xs).append("px;\n");
            sb.append("    top: ").append(ys).append("px;\n");
        }
        String ws = child.getStyles().get("width");
        String hs = child.getStyles().get("height");
        if (ws != null && isAbsolute) sb.append("    width: ").append(ws).append("px;\n");
        if (hs != null && isAbsolute) sb.append("    height: ").append(hs).append("px;\n");
        for (Map.Entry<String, String> style : child.getStyles().entrySet()) {
            String key = style.getKey();
            if ("width".equals(key) || "height".equals(key)) continue;
            if ("z-index".equals(key)) continue;
            sb.append("    ").append(key).append(": ").append(style.getValue()).append(";\n");
        }
        String customCss = child.getCustomCss();
        if (customCss != null && !customCss.trim().isEmpty()) {
            for (String line : customCss.split(";")) {
                line = line.trim();
                if (!line.isEmpty()) {
                    sb.append("    ").append(line).append(";\n");
                }
            }
        }
        sb.append("}\n\n");
    }

    public static String toJs(VisualComponent root) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by Eagle Visual Builder\n");
        sb.append("document.addEventListener('DOMContentLoaded', function() {\n");
        collectJs(root, sb);
        sb.append("});\n");
        return sb.toString();
    }

    private static void collectJs(VisualComponent c, StringBuilder sb) {
        String id = c.getId();
        for (Map.Entry<String, String> attr : c.getAttributes().entrySet()) {
            String key = attr.getKey();
            String val = attr.getValue();
            if (val == null || val.trim().isEmpty()) continue;
            if (key.startsWith("on") && key.length() > 2) {
                String event = key.substring(2); // onclick -> click, onmouseover -> mouseover
                sb.append("  document.querySelector('.comp-").append(id).append("')");
                sb.append(".addEventListener('").append(event).append("', function(e) {\n");
                sb.append("    ").append(val).append("\n");
                sb.append("  });\n");
            }
        }
        for (VisualComponent child : c.getChildren()) {
            collectJs(child, sb);
        }
    }

    private static String repeat(String s, int times) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < times; i++) b.append(s);
        return b.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
