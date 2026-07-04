package com.eagle.builder;

import java.util.*;

/**
 * A single component placed on the visual builder canvas.
 * Holds its type, text/content, inline style properties, attributes, and children.
 */
public class VisualComponent {

    private String id;
    private ComponentType type;
    private String text = "";
    private String icon;                    // خاص بالأيقونات والـ Icon Button

    /** CSS-property -> value, e.g. "background-color" -> "#6c5ce7" */
    private final Map<String, String> styles = new LinkedHashMap<>();

    /** HTML attribute -> value, e.g. "href" -> "#", "src" -> "image.png", "placeholder" ... */
    private final Map<String, String> attributes = new LinkedHashMap<>();

    /** Custom CSS injected as additional inline style */
    private String customCss = "";

    private final List<VisualComponent> children = new ArrayList<>();

    public VisualComponent(ComponentType type) {
        this.id = "c_" + UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.styles.clear();
        this.attributes.clear();
        applyDefaults();
    }

    private void applyDefaults() {
        switch (type) {

            // ==================== TEXT & TYPOGRAPHY ====================
            case HEADING:
                text = "العنوان الرئيسي";
                styles.put("font-size", "32px");
                styles.put("font-weight", "bold");
                styles.put("color", "#1e2937");
                styles.put("margin", "20px 0 12px 0");
                break;

            case SUBHEADING:
                text = "عنوان فرعي";
                styles.put("font-size", "24px");
                styles.put("font-weight", "600");
                styles.put("color", "#334155");
                styles.put("margin", "16px 0 10px 0");
                break;

            case PARAGRAPH:
            case RICH_TEXT:
                text = "هذا نص فقرة عادي. يمكنك تعديله بالضغط عليه.";
                styles.put("font-size", "16px");
                styles.put("line-height", "1.7");
                styles.put("color", "#475569");
                styles.put("margin", "8px 0");
                break;

            case QUOTE:
                text = "“هذا اقتباس مميز يعبر عن فكرة مهمة.”";
                styles.put("font-size", "18px");
                styles.put("font-style", "italic");
                styles.put("color", "#64748b");
                styles.put("padding", "20px 24px");
                styles.put("border-left", "5px solid #6366f1");
                styles.put("background-color", "#f8fafc");
                styles.put("margin", "16px 0");
                break;

            // ==================== BUTTONS & INTERACTIVE ====================
            case BUTTON:
                text = "اضغط هنا";
                styles.put("background-color", "#6366f1");
                styles.put("color", "#ffffff");
                styles.put("padding", "12px 28px");
                styles.put("border-radius", "8px");
                styles.put("border", "none");
                styles.put("font-weight", "600");
                styles.put("cursor", "pointer");
                break;

            case ICON_BUTTON:
                icon = "🔘";
                text = "";
                styles.put("background-color", "#6366f1");
                styles.put("color", "#ffffff");
                styles.put("padding", "12px");
                styles.put("border-radius", "8px");
                styles.put("border", "none");
                styles.put("cursor", "pointer");
                break;

            case LINK:
                text = "رابط لصفحة أخرى";
                attributes.put("href", "#");
                attributes.put("target", "_blank");
                styles.put("color", "#4f46e5");
                styles.put("text-decoration", "underline");
                styles.put("font-weight", "500");
                break;

            // ==================== FORM ELEMENTS ====================
            case INPUT:
                attributes.put("type", "text");
                attributes.put("placeholder", "أدخل النص هنا...");
                styles.put("padding", "12px 16px");
                styles.put("border", "2px solid #e2e8f0");
                styles.put("border-radius", "8px");
                styles.put("background-color", "#ffffff");
                break;

            case TEXTAREA:
                attributes.put("placeholder", "اكتب رسالتك هنا...");
                styles.put("padding", "14px 16px");
                styles.put("border", "2px solid #e2e8f0");
                styles.put("border-radius", "8px");
                styles.put("min-height", "110px");
                styles.put("background-color", "#ffffff");
                break;

            case SELECT:
            case MULTISELECT:
                attributes.put("value", "");
                styles.put("padding", "12px 16px");
                styles.put("border", "2px solid #e2e8f0");
                styles.put("border-radius", "8px");
                styles.put("background-color", "#ffffff");
                break;

            case CHECKBOX:
                text = "تفعيل الخيار";
                attributes.put("type", "checkbox");
                styles.put("accent-color", "#6366f1");
                break;

            case RADIO:
                text = "اختيار واحد";
                attributes.put("type", "radio");
                styles.put("accent-color", "#6366f1");
                break;

            case TOGGLE:
                text = "تشغيل / إيقاف";
                attributes.put("type", "checkbox");
                styles.put("accent-color", "#22c55e");
                break;

            case DATE_PICKER:
                attributes.put("type", "date");
                styles.put("padding", "12px 16px");
                styles.put("border", "2px solid #e2e8f0");
                styles.put("border-radius", "8px");
                break;

            case FILE_UPLOAD:
                text = "رفع ملف";
                attributes.put("type", "file");
                attributes.put("accept", "image/*,.pdf,.docx,.zip");
                styles.put("background-color", "#f1f5f9");
                styles.put("border", "2px dashed #94a3b8");
                styles.put("padding", "24px");
                styles.put("text-align", "center");
                styles.put("border-radius", "8px");
                break;

            case SLIDER:
                attributes.put("type", "range");
                attributes.put("min", "0");
                attributes.put("max", "100");
                attributes.put("value", "45");
                styles.put("accent-color", "#6366f1");
                break;

            case RATING:
                text = "★★★★☆";
                styles.put("font-size", "28px");
                styles.put("color", "#fbbf24");
                break;

            // ==================== MEDIA & FILES ====================
            case IMAGE:
                attributes.put("src", "https://via.placeholder.com/600x340/6366f1/ffffff?text=صورة");
                attributes.put("alt", "صورة توضيحية");
                styles.put("border-radius", "12px");
                styles.put("box-shadow", "0 4px 12px rgba(0,0,0,0.1)");
                break;

            case VIDEO:
                attributes.put("src", "https://www.w3schools.com/html/mov_bbb.mp4");
                attributes.put("controls", "true");
                styles.put("border-radius", "12px");
                styles.put("background-color", "#1e2937");
                break;

            case AUDIO:
                attributes.put("controls", "true");
                styles.put("width", "100%");
                break;

            case CAROUSEL:
            case GALLERY:
                styles.put("padding", "12px");
                styles.put("border-radius", "10px");
                styles.put("background-color", "#f8fafc");
                break;

            // ==================== ICONS ====================
            case ICON:
                icon = "\u2B50";
                text = "";
                styles.put("font-size", "32px");
                styles.put("color", "#6366f1");
                break;

            // ==================== LAYOUT ====================
            case CONTAINER:
                styles.put("background-color", "#f8fafc");
                styles.put("border", "2px dashed #cbd5e1");
                styles.put("border-radius", "12px");
                styles.put("padding", "24px");
                styles.put("min-height", "120px");
                break;

            case ROW:
                styles.put("background-color", "#f1f5f9");
                styles.put("border", "2px dashed #cbd5e1");
                styles.put("border-radius", "12px");
                styles.put("padding", "20px");
                styles.put("min-height", "100px");
                break;

            case FLEXBOX:
                styles.put("display", "flex");
                styles.put("flex-wrap", "wrap");
                styles.put("gap", "12px");
                styles.put("background-color", "#f8fafc");
                styles.put("border", "2px dashed #94a3b8");
                styles.put("border-radius", "12px");
                styles.put("padding", "20px");
                styles.put("min-height", "100px");
                break;

            case STACK:
                styles.put("display", "flex");
                styles.put("flex-direction", "column");
                styles.put("gap", "12px");
                styles.put("background-color", "#f8fafc");
                styles.put("border", "2px dashed #94a3b8");
                styles.put("border-radius", "12px");
                styles.put("padding", "20px");
                styles.put("min-height", "100px");
                break;

            case WRAP:
                styles.put("display", "flex");
                styles.put("flex-wrap", "wrap");
                styles.put("gap", "16px");
                styles.put("background-color", "#f1f5f9");
                styles.put("border", "2px dashed #94a3b8");
                styles.put("border-radius", "12px");
                styles.put("padding", "16px");
                styles.put("min-height", "80px");
                break;

            case GRID:
                styles.put("display", "grid");
                styles.put("grid-template-columns", "repeat(auto-fit, minmax(250px, 1fr))");
                styles.put("gap", "16px");
                styles.put("padding", "20px");
                styles.put("border", "2px dashed #94a3b8");
                styles.put("border-radius", "12px");
                break;

            case CARD:
            case HERO:
                styles.put("background-color", "#ffffff");
                styles.put("border-radius", "16px");
                styles.put("box-shadow", "0 10px 15px -3px rgba(0, 0, 0, 0.1)");
                styles.put("padding", "32px");
                break;

            case HEADER:
                styles.put("background-color", "#1e2937");
                styles.put("color", "#ffffff");
                styles.put("padding", "20px 28px");
                break;

            case FOOTER:
                styles.put("background-color", "#0f172a");
                styles.put("color", "#e2e8f0");
                styles.put("padding", "28px");
                break;

            case DIVIDER:
                styles.put("border-top", "3px solid #e2e8f0");
                styles.put("margin", "28px 0");
                break;

            case SPACER:
                styles.put("height", "48px");
                styles.put("background-color", "transparent");
                break;

            // ==================== NAVIGATION & OTHERS ====================
            case NAVIGATION:
                styles.put("background-color", "#1e2937");
                styles.put("color", "#e2e8f0");
                styles.put("padding", "16px 24px");
                break;

            case BADGE:
            case CHIP:
                text = "جديد";
                styles.put("background-color", "#22c55e");
                styles.put("color", "#ffffff");
                styles.put("padding", "6px 14px");
                styles.put("border-radius", "9999px");
                styles.put("font-size", "13px");
                break;

            case AVATAR:
                attributes.put("src", "https://via.placeholder.com/64");
                styles.put("width", "64px");
                styles.put("height", "64px");
                styles.put("border-radius", "50%");
                styles.put("border", "3px solid #e0e7ff");
                break;

            case ALERT:
            case TOAST:
                text = "تم بنجاح!";
                styles.put("background-color", "#ecfdf5");
                styles.put("color", "#10b981");
                styles.put("padding", "16px 20px");
                styles.put("border-radius", "10px");
                break;

            case PROGRESS:
                attributes.put("value", "75");
                attributes.put("max", "100");
                styles.put("accent-color", "#6366f1");
                break;

            // ==================== ADVANCED ====================
            case MODAL:
            case FORM:
            case TABLE:
            case ACCORDION:
            case MAP:
            case CHART:
            case QR_CODE:
            case EMBED:
                styles.put("background-color", "#ffffff");
                styles.put("border", "2px solid #e2e8f0");
                styles.put("border-radius", "12px");
                styles.put("padding", "24px");
                styles.put("min-height", "180px");
                break;

            // ==================== PRE-BUILT BLOCKS ====================
            case HEADER_BLOCK:
                styles.put("background", "linear-gradient(135deg, #1e2937, #0f172a)");
                styles.put("color", "#ffffff");
                styles.put("padding", "16px 28px");
                styles.put("border-radius", "12px");
                styles.put("min-height", "64px");
                populateHeaderBlock();
                break;
            case HERO_BLOCK:
                styles.put("background", "linear-gradient(135deg, #6366f1, #8b5cf6)");
                styles.put("color", "#ffffff");
                styles.put("padding", "60px 40px");
                styles.put("border-radius", "16px");
                styles.put("text-align", "center");
                styles.put("min-height", "340px");
                populateHeroBlock();
                break;
            case FEATURES_BLOCK:
                styles.put("background-color", "#f8fafc");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                populateFeaturesBlock();
                break;
            case PRICING_BLOCK:
                styles.put("background-color", "#f1f5f9");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                populatePricingBlock();
                break;
            case TESTIMONIALS_BLOCK:
                styles.put("background-color", "#ffffff");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                populateTestimonialsBlock();
                break;
            case FAQ_BLOCK:
                styles.put("background-color", "#f8fafc");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                populateFaqBlock();
                break;
            case CONTACT_BLOCK:
                styles.put("background-color", "#ffffff");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                styles.put("max-width", "640px");
                populateContactBlock();
                break;
            case FOOTER_BLOCK:
                styles.put("background-color", "#0f172a");
                styles.put("color", "#e2e8f0");
                styles.put("padding", "40px 32px 20px");
                styles.put("border-radius", "12px");
                populateFooterBlock();
                break;
            case TEAM_BLOCK:
                styles.put("background-color", "#f8fafc");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                populateTeamBlock();
                break;
            case STATS_BLOCK:
                styles.put("background", "linear-gradient(135deg, #1e2937, #334155)");
                styles.put("color", "#ffffff");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                populateStatsBlock();
                break;
            case CTA_BLOCK:
                styles.put("background", "linear-gradient(135deg, #6366f1, #a855f7)");
                styles.put("color", "#ffffff");
                styles.put("padding", "56px 40px");
                styles.put("border-radius", "16px");
                styles.put("text-align", "center");
                populateCtaBlock();
                break;
            case NEWSLETTER_BLOCK:
                styles.put("background-color", "#f1f5f9");
                styles.put("padding", "48px 32px");
                styles.put("border-radius", "16px");
                styles.put("text-align", "center");
                populateNewsletterBlock();
                break;

            default:
                text = type.getLabel();
                styles.put("padding", "20px");
                styles.put("border", "2px dashed #94a3b8");
                styles.put("border-radius", "10px");
                break;
        }
    }

    private void addChildBlock(ComponentType type, String text, java.util.Map<String, String> styles) {
        VisualComponent c = new VisualComponent(type);
        c.setText(text);
        c.getStyles().putAll(styles);
        children.add(c);
    }

    private void populateHeaderBlock() {
        addChildBlock(ComponentType.HEADING, "My Brand",
            put("font-size", "22px", "color", "#ffffff", "margin", "0"));
        VisualComponent nav = new VisualComponent(ComponentType.NAVIGATION);
        nav.getStyles().put("display", "flex");
        nav.getStyles().put("gap", "20px");
        addChild(nav);
        String[] links = {"Home", "About", "Services", "Contact"};
        for (String l : links) {
            VisualComponent lnk = new VisualComponent(ComponentType.LINK);
            lnk.setText(l);
            lnk.getStyles().put("color", "#cbd5e1");
            lnk.getStyles().put("text-decoration", "none");
            nav.addChild(lnk);
        }
        VisualComponent btn = new VisualComponent(ComponentType.BUTTON);
        btn.setText("Get Started");
        btn.getStyles().put("background-color", "#6366f1");
        btn.getStyles().put("color", "#ffffff");
        btn.getStyles().put("padding", "10px 20px");
        btn.getStyles().put("border-radius", "8px");
        addChild(btn);
    }

    private void populateHeroBlock() {
        addChildBlock(ComponentType.HEADING, "Build Stunning Websites",
            put("font-size", "40px", "font-weight", "bold", "color", "#ffffff", "margin", "0 0 8px 0"));
        addChildBlock(ComponentType.PARAGRAPH, "Create beautiful, responsive websites with our intuitive builder.",
            put("font-size", "18px", "color", "#e2e8f0", "margin", "0 0 24px 0", "max-width", "600px"));
        addChildBlock(ComponentType.BUTTON, "Get Started",
            put("background-color", "#ffffff", "color", "#6366f1", "padding", "14px 32px", "border-radius", "8px", "font-weight", "bold"));
        addChildBlock(ComponentType.BUTTON, "Learn More",
            put("background", "transparent", "color", "#ffffff", "padding", "14px 28px", "border-radius", "8px", "border", "2px solid #ffffff"));
    }

    private void populateFeaturesBlock() {
        addChildBlock(ComponentType.HEADING, "Our Features",
            put("font-size", "36px", "text-align", "center", "color", "#1e2937", "margin", "0 0 32px 0"));
        String[][] features = {
            {"⚡", "Lightning Fast", "Optimized for speed with lazy loading and CDN."},
            {"🔒", "Secure", "Enterprise-grade security for your data."},
            {"🎨", "Customizable", "Full control over every design element."}
        };
        for (String[] f : features) {
            VisualComponent card = new VisualComponent(ComponentType.CARD);
            card.getStyles().put("background-color", "#ffffff");
            card.getStyles().put("padding", "24px");
            card.getStyles().put("border-radius", "12px");
            card.getStyles().put("box-shadow", "0 4px 12px rgba(0,0,0,0.08)");
            card.addChild(new VisualComponent(ComponentType.HEADING));
            card.getChildren().get(0).setText(f[0]);
            card.getChildren().get(0).getStyles().put("font-size", "36px");
            card.addChild(new VisualComponent(ComponentType.SUBHEADING));
            card.getChildren().get(1).setText(f[1]);
            card.getChildren().get(1).getStyles().put("font-size", "20px");
            card.addChild(new VisualComponent(ComponentType.PARAGRAPH));
            card.getChildren().get(2).setText(f[2]);
            card.getChildren().get(2).getStyles().put("color", "#64748b");
            addChild(card);
        }
    }

    private void populatePricingBlock() {
        addChildBlock(ComponentType.HEADING, "Pricing Plans",
            put("font-size", "36px", "text-align", "center", "color", "#1e2937", "margin", "0 0 32px 0"));
        String[][] plans = {
            {"Starter", "Free", "1 project, basic support"},
            {"Pro", "$29/mo", "Unlimited projects, priority support"},
            {"Enterprise", "$99/mo", "Custom solutions, dedicated manager"}
        };
        for (String[] p : plans) {
            VisualComponent card = new VisualComponent(ComponentType.CARD);
            card.getStyles().put("background-color", "#ffffff");
            card.getStyles().put("padding", "28px");
            card.getStyles().put("border-radius", "12px");
            card.getStyles().put("text-align", "center");
            card.addChild(new VisualComponent(ComponentType.SUBHEADING));
            card.getChildren().get(0).setText(p[0]);
            card.addChild(new VisualComponent(ComponentType.HEADING));
            card.getChildren().get(1).setText(p[1]);
            card.getChildren().get(1).getStyles().put("color", "#6366f1");
            card.addChild(new VisualComponent(ComponentType.PARAGRAPH));
            card.getChildren().get(2).setText(p[2]);
            card.getChildren().get(2).getStyles().put("color", "#64748b");
            card.addChild(new VisualComponent(ComponentType.BUTTON));
            card.getChildren().get(3).setText("Choose Plan");
            card.getChildren().get(3).getStyles().put("background-color", "#6366f1");
            card.getChildren().get(3).getStyles().put("color", "#ffffff");
            addChild(card);
        }
    }

    private void populateTestimonialsBlock() {
        addChildBlock(ComponentType.HEADING, "What Our Clients Say",
            put("font-size", "36px", "text-align", "center", "color", "#1e2937", "margin", "0 0 32px 0"));
        String[][] testimonials = {
            {"👤", "John Doe", "CEO, Company", "Amazing platform! Built my site in minutes."},
            {"👤", "Jane Smith", "Designer", "The best builder I've ever used."},
            {"👤", "Bob Wilson", "Developer", "Powerful yet simple. Highly recommended."}
        };
        for (String[] t : testimonials) {
            VisualComponent card = new VisualComponent(ComponentType.CARD);
            card.getStyles().put("background-color", "#ffffff");
            card.getStyles().put("padding", "24px");
            card.getStyles().put("border-radius", "12px");
            card.getStyles().put("text-align", "center");
            card.addChild(new VisualComponent(ComponentType.HEADING));
            card.getChildren().get(0).setText(t[0]);
            card.getChildren().get(0).getStyles().put("font-size", "36px");
            card.addChild(new VisualComponent(ComponentType.PARAGRAPH));
            card.getChildren().get(1).setText(t[3]);
            card.getChildren().get(1).getStyles().put("font-style", "italic");
            card.addChild(new VisualComponent(ComponentType.CHIP));
            card.getChildren().get(2).setText(t[1] + " - " + t[2]);
            addChild(card);
        }
    }

    private void populateFaqBlock() {
        addChildBlock(ComponentType.HEADING, "Frequently Asked Questions",
            put("font-size", "36px", "text-align", "center", "color", "#1e2937", "margin", "0 0 32px 0"));
        String[][] faqs = {
            {"What is this builder?", "Drag-and-drop tool for creating websites visually."},
            {"Is it free?", "Yes, basic features are free forever."},
            {"Can I export my site?", "Export to HTML/CSS/JS with one click."}
        };
        for (String[] faq : faqs) {
            VisualComponent accordion = new VisualComponent(ComponentType.ACCORDION);
            accordion.setText(faq[0]);
            accordion.addChild(new VisualComponent(ComponentType.PARAGRAPH));
            accordion.getChildren().get(0).setText(faq[1]);
            addChild(accordion);
        }
    }

    private void populateContactBlock() {
        addChildBlock(ComponentType.HEADING, "Contact Us",
            put("font-size", "32px", "color", "#1e2937", "margin", "0 0 24px 0", "text-align", "center"));
        addChildBlock(ComponentType.INPUT, "",
            put("width", "100%", "margin", "0 0 12px 0"));
        children.get(children.size()-1).getAttributes().put("placeholder", "Your Name");
        addChildBlock(ComponentType.INPUT, "",
            put("width", "100%", "margin", "0 0 12px 0"));
        children.get(children.size()-1).getAttributes().put("placeholder", "Your Email");
        addChildBlock(ComponentType.TEXTAREA, "",
            put("width", "100%", "margin", "0 0 16px 0"));
        children.get(children.size()-1).getAttributes().put("placeholder", "Your Message");
        addChildBlock(ComponentType.BUTTON, "Send Message",
            put("background-color", "#6366f1", "color", "#ffffff", "padding", "14px 32px", "border-radius", "8px", "width", "100%"));
    }

    private void populateFooterBlock() {
        addChildBlock(ComponentType.HEADING, "MyBrand",
            put("font-size", "20px", "color", "#ffffff", "margin", "0 0 16px 0"));
        addChildBlock(ComponentType.PARAGRAPH, "© 2026 All rights reserved.",
            put("color", "#94a3b8", "font-size", "14px"));
        addChildBlock(ComponentType.DIVIDER, "",
            put("border-color", "#334155", "margin", "16px 0"));
        addChildBlock(ComponentType.PARAGRAPH, "Made with ❤️ by Eagle IDE",
            put("color", "#64748b", "font-size", "12px", "text-align", "center"));
    }

    private void populateTeamBlock() {
        addChildBlock(ComponentType.HEADING, "Meet Our Team",
            put("font-size", "36px", "text-align", "center", "color", "#1e2937", "margin", "0 0 32px 0"));
        String[][] members = {
            {"👩‍💼", "Sarah Johnson", "CEO"},
            {"👨‍💻", "Mike Chen", "CTO"},
            {"🎨", "Emily Davis", "Designer"},
            {"📊", "Alex Brown", "Marketing"}
        };
        for (String[] m : members) {
            VisualComponent card = new VisualComponent(ComponentType.CARD);
            card.getStyles().put("background-color", "#ffffff");
            card.getStyles().put("padding", "20px");
            card.getStyles().put("border-radius", "12px");
            card.getStyles().put("text-align", "center");
            card.addChild(new VisualComponent(ComponentType.HEADING));
            card.getChildren().get(0).setText(m[0]);
            card.getChildren().get(0).getStyles().put("font-size", "32px");
            card.addChild(new VisualComponent(ComponentType.SUBHEADING));
            card.getChildren().get(1).setText(m[1]);
            card.getChildren().get(1).getStyles().put("font-size", "18px");
            card.addChild(new VisualComponent(ComponentType.PARAGRAPH));
            card.getChildren().get(2).setText(m[2]);
            card.getChildren().get(2).getStyles().put("color", "#64748b");
            addChild(card);
        }
    }

    private void populateStatsBlock() {
        String[][] stats = {
            {"10K+", "Users"},
            {"500+", "Projects"},
            {"99%", "Uptime"},
            {"24/7", "Support"}
        };
        for (String[] s : stats) {
            VisualComponent card = new VisualComponent(ComponentType.CARD);
            card.getStyles().put("background-color", "rgba(255,255,255,0.1)");
            card.getStyles().put("padding", "20px");
            card.getStyles().put("border-radius", "12px");
            card.getStyles().put("text-align", "center");
            card.addChild(new VisualComponent(ComponentType.HEADING));
            card.getChildren().get(0).setText(s[0]);
            card.getChildren().get(0).getStyles().put("font-size", "32px");
            card.getChildren().get(0).getStyles().put("color", "#ffffff");
            card.addChild(new VisualComponent(ComponentType.PARAGRAPH));
            card.getChildren().get(1).setText(s[1]);
            card.getChildren().get(1).getStyles().put("color", "#94a3b8");
            addChild(card);
        }
    }

    private void populateCtaBlock() {
        addChildBlock(ComponentType.HEADING, "Ready to Get Started?",
            put("font-size", "36px", "font-weight", "bold", "color", "#ffffff", "margin", "0 0 12px 0"));
        addChildBlock(ComponentType.PARAGRAPH, "Join thousands of happy users today.",
            put("font-size", "18px", "color", "#e2e8f0", "margin", "0 0 28px 0"));
        addChildBlock(ComponentType.BUTTON, "Sign Up Free →",
            put("background-color", "#ffffff", "color", "#6366f1", "padding", "16px 36px", "border-radius", "8px", "font-weight", "bold"));
    }

    private void populateNewsletterBlock() {
        addChildBlock(ComponentType.HEADING, "Stay Updated",
            put("font-size", "32px", "color", "#1e2937", "margin", "0 0 8px 0"));
        addChildBlock(ComponentType.PARAGRAPH, "Subscribe to our newsletter for the latest updates.",
            put("color", "#64748b", "margin", "0 0 24px 0"));
        addChildBlock(ComponentType.INPUT, "",
            put("width", "280px", "padding", "12px 16px", "border-radius", "8px", "margin-right", "8px"));
        children.get(children.size()-1).getAttributes().put("placeholder", "your@email.com");
        addChildBlock(ComponentType.BUTTON, "Subscribe",
            put("background-color", "#6366f1", "color", "#ffffff", "padding", "12px 24px", "border-radius", "8px"));
    }

    private static java.util.Map<String, String> put(String... kvs) {
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) map.put(kvs[i], kvs[i+1]);
        return map;
    }

    // ==================== HELPER METHODS ====================
    public boolean isContainer() {
        return type == ComponentType.CONTAINER || type == ComponentType.ROW ||
               type == ComponentType.GRID || type == ComponentType.CARD ||
               type == ComponentType.SECTION || type == ComponentType.HERO ||
               type == ComponentType.FORM || type == ComponentType.NAVIGATION ||
               type == ComponentType.HEADER || type == ComponentType.FOOTER ||
               type == ComponentType.ASIDE || type == ComponentType.SIDEBAR ||
               type == ComponentType.FEATURE_SECTION || type == ComponentType.TESTIMONIALS ||
               type == ComponentType.PRICING || type == ComponentType.FAQ ||
               type == ComponentType.STATS || type == ComponentType.CAROUSEL ||
               type == ComponentType.GALLERY || type == ComponentType.DRAWER ||
               type == ComponentType.MODAL || type == ComponentType.POPOVER ||
               type == ComponentType.ACCORDION || type == ComponentType.LIST ||
               type == ComponentType.FLEXBOX || type == ComponentType.STACK ||
               type == ComponentType.WRAP ||
               type == ComponentType.HEADER_BLOCK || type == ComponentType.HERO_BLOCK ||
               type == ComponentType.FEATURES_BLOCK || type == ComponentType.PRICING_BLOCK ||
               type == ComponentType.TESTIMONIALS_BLOCK || type == ComponentType.FAQ_BLOCK ||
               type == ComponentType.CONTACT_BLOCK || type == ComponentType.FOOTER_BLOCK ||
               type == ComponentType.TEAM_BLOCK || type == ComponentType.STATS_BLOCK ||
               type == ComponentType.CTA_BLOCK || type == ComponentType.NEWSLETTER_BLOCK;
    }

    public boolean supportsText() {
        return type != ComponentType.IMAGE && type != ComponentType.VIDEO &&
               type != ComponentType.AUDIO && type != ComponentType.SPACER &&
               type != ComponentType.DIVIDER && type != ComponentType.PROGRESS;
    }

    public boolean supportsSrc() {
        return type == ComponentType.IMAGE || type == ComponentType.VIDEO ||
               type == ComponentType.AUDIO || type == ComponentType.EMBED;
    }

    public boolean supportsHref() {
        return type == ComponentType.LINK;
    }

    public boolean supportsIcon() {
        return type == ComponentType.ICON || type == ComponentType.ICON_BUTTON;
    }

    public boolean supportsFile() {
        return type == ComponentType.FILE_UPLOAD || type == ComponentType.IMAGE;
    }

    // ==================== GETTERS & SETTERS ====================
    public String getId() { return id; }
    public ComponentType getType() { return type; }

    public String getText() { return text; }
    public void setText(String text) { 
        if (supportsText()) this.text = text != null ? text : ""; 
    }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { 
        if (supportsIcon()) this.icon = icon; 
    }

    public Map<String, String> getStyles() { return styles; }
    public Map<String, String> getAttributes() { return attributes; }
    public String getCustomCss() { return customCss; }
    public void setCustomCss(String s) { customCss = s != null ? s : ""; }
    public List<VisualComponent> getChildren() { return children; }

    public void addChild(VisualComponent child) {
        if (isContainer() && child != null) {
            children.add(child);
        }
    }
}