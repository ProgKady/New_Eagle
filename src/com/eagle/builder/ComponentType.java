package com.eagle.builder;

import com.eagle.icons.IconManager;
import javafx.scene.image.ImageView;

public enum ComponentType {

    // ==================== TEXT & TYPOGRAPHY ====================
    HEADING("Heading", IconManager.TEXT_TOOL, Category.TEXT),
    SUBHEADING("Subheading", IconManager.TEXT_TOOL, Category.TEXT),
    PARAGRAPH("Paragraph", IconManager.TEXT_TOOL, Category.TEXT),
    RICH_TEXT("Rich Text", IconManager.TEXT_TOOL, Category.TEXT),
    QUOTE("Quote", IconManager.TEXT_TOOL, Category.TEXT),

    // ==================== BASIC INTERACTIVE ====================
    BUTTON("Button", IconManager.BUTTON_TOOL, Category.BASIC),
    ICON_BUTTON("Icon Button", IconManager.BUTTON_TOOL, Category.BASIC),
    LINK("Link", IconManager.BUTTON_TOOL, Category.BASIC),
    BUTTON_GROUP("Button Group", IconManager.BUTTON_TOOL, Category.BASIC),

    // ==================== FORM ELEMENTS ====================
    INPUT("Input Field", IconManager.FORM_TOOL, Category.FORMS),
    TEXTAREA("Textarea", IconManager.FORM_TOOL, Category.FORMS),
    SELECT("Select", IconManager.FORM_TOOL, Category.FORMS),
    MULTISELECT("Multi Select", IconManager.FORM_TOOL, Category.FORMS),
    CHECKBOX("Checkbox", IconManager.FORM_TOOL, Category.FORMS),
    RADIO("Radio Button", IconManager.FORM_TOOL, Category.FORMS),
    TOGGLE("Toggle / Switch", IconManager.FORM_TOOL, Category.FORMS),
    DATE_PICKER("Date Picker", IconManager.FORM_TOOL, Category.FORMS),
    TIME_PICKER("Time Picker", IconManager.FORM_TOOL, Category.FORMS),
    FILE_UPLOAD("File Upload", IconManager.FORM_TOOL, Category.FORMS),
    SLIDER("Slider", IconManager.FORM_TOOL, Category.FORMS),
    RATING("Rating", IconManager.FORM_TOOL, Category.FORMS),

    // ==================== MEDIA ====================
    IMAGE("Image", IconManager.MEDIA_TOOL, Category.MEDIA),
    VIDEO("Video", IconManager.MEDIA_TOOL, Category.MEDIA),
    AUDIO("Audio", IconManager.MEDIA_TOOL, Category.MEDIA),
    CAROUSEL("Carousel", IconManager.MEDIA_TOOL, Category.MEDIA),
    GALLERY("Image Gallery", IconManager.MEDIA_TOOL, Category.MEDIA),

    // ==================== LAYOUT ====================
    CONTAINER("Container", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    ROW("Row", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    GRID("Grid", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    SECTION("Section", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    CARD("Card", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    DIVIDER("Divider", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    SPACER("Spacer", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    ASIDE("Aside", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    HEADER("Header", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    FOOTER("Footer", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    FLEXBOX("Flexbox", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    STACK("Stack", IconManager.LAYOUT_TOOL, Category.LAYOUT),
    WRAP("Wrap", IconManager.LAYOUT_TOOL, Category.LAYOUT),

    // ==================== NAVIGATION ====================
    NAVIGATION("Navigation", IconManager.LAYOUT_TOOL, Category.NAVIGATION),
    TABS("Tabs", IconManager.LAYOUT_TOOL, Category.NAVIGATION),
    BREADCRUMB("Breadcrumb", IconManager.LAYOUT_TOOL, Category.NAVIGATION),
    PAGINATION("Pagination", IconManager.LAYOUT_TOOL, Category.NAVIGATION),
    STEPPER("Stepper", IconManager.LAYOUT_TOOL, Category.NAVIGATION),
    DROPDOWN("Dropdown Menu", IconManager.LAYOUT_TOOL, Category.NAVIGATION),
    SIDEBAR("Sidebar", IconManager.LAYOUT_TOOL, Category.NAVIGATION),

    // ==================== DATA DISPLAY ====================
    TABLE("Table", IconManager.DATA_TOOL, Category.DATA),
    DATA_TABLE("Data Table", IconManager.DATA_TOOL, Category.DATA),
    LIST("List", IconManager.DATA_TOOL, Category.DATA),
    ACCORDION("Accordion", IconManager.DATA_TOOL, Category.DATA),
    TIMELINE("Timeline", IconManager.DATA_TOOL, Category.DATA),

    // ==================== FEEDBACK & OVERLAYS ====================
    MODAL("Modal", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    DRAWER("Drawer", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    ALERT("Alert", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    TOAST("Toast", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    PROGRESS("Progress Bar", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    SPINNER("Spinner", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    SKELETON("Skeleton", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    TOOLTIP("Tooltip", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),
    POPOVER("Popover", IconManager.FEEDBACK_TOOL, Category.FEEDBACK),

    // ==================== INDICATORS & MISC ====================
    BADGE("Badge", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    CHIP("Chip / Tag", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    AVATAR("Avatar", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    AVATAR_GROUP("Avatar Group", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    ICON("Icon", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    MAP("Map", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    CHART("Chart", IconManager.DATA_TOOL, Category.INDICATORS),
    EMBED("Embed", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    QR_CODE("QR Code", IconManager.INDICATOR_TOOL, Category.INDICATORS),
    CALENDAR("Calendar", IconManager.INDICATOR_TOOL, Category.INDICATORS),

    // ==================== ADVANCED / SPECIAL ====================
    FORM("Form", IconManager.FORM_TOOL, Category.ADVANCED),
    HERO("Hero Section", IconManager.ADVANCED_TOOL, Category.ADVANCED),
    TESTIMONIALS("Testimonials", IconManager.FEEDBACK_TOOL, Category.ADVANCED),
    PRICING("Pricing Cards", IconManager.ADVANCED_TOOL, Category.ADVANCED),
    FAQ("FAQ", IconManager.FEEDBACK_TOOL, Category.ADVANCED),
    STATS("Statistics", IconManager.DATA_TOOL, Category.ADVANCED),
    FEATURE_SECTION("Feature Section", IconManager.ADVANCED_TOOL, Category.ADVANCED),

    // ==================== PRE-BUILT BLOCKS ====================
    HEADER_BLOCK("Header Block", IconManager.LAYOUT_TOOL, Category.BLOCKS),
    HERO_BLOCK("Hero Block", IconManager.ADVANCED_TOOL, Category.BLOCKS),
    FEATURES_BLOCK("Features Block", IconManager.ADVANCED_TOOL, Category.BLOCKS),
    PRICING_BLOCK("Pricing Block", IconManager.ADVANCED_TOOL, Category.BLOCKS),
    TESTIMONIALS_BLOCK("Testimonials Block", IconManager.FEEDBACK_TOOL, Category.BLOCKS),
    FAQ_BLOCK("FAQ Block", IconManager.FEEDBACK_TOOL, Category.BLOCKS),
    CONTACT_BLOCK("Contact Block", IconManager.FORM_TOOL, Category.BLOCKS),
    FOOTER_BLOCK("Footer Block", IconManager.LAYOUT_TOOL, Category.BLOCKS),
    TEAM_BLOCK("Team Block", IconManager.INDICATOR_TOOL, Category.BLOCKS),
    STATS_BLOCK("Stats Block", IconManager.DATA_TOOL, Category.BLOCKS),
    CTA_BLOCK("CTA Block", IconManager.BUTTON_TOOL, Category.BLOCKS),
    NEWSLETTER_BLOCK("Newsletter Block", IconManager.FORM_TOOL, Category.BLOCKS);

    public enum Category {
        TEXT("Text & Typography"),
        BASIC("Basic Interactive"),
        FORMS("Form Elements"),
        MEDIA("Media"),
        LAYOUT("Layout & Structure"),
        NAVIGATION("Navigation"),
        DATA("Data Display"),
        FEEDBACK("Feedback & Overlays"),
        INDICATORS("Indicators & Misc"),
        ADVANCED("Advanced"),
        BLOCKS("Pre-built Blocks");

        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    private final String label;
    private final String iconName;
    private final Category category;

    ComponentType(String label, String iconName, Category category) {
        this.label = label;
        this.iconName = iconName;
        this.category = category;
    }

    public String getLabel() { return label; }
    public String getIcon() { return iconName; }
    public Category getCategory() { return category; }
    public String getDisplayName() { return label; }

    public ImageView getIconView() {
        return getIconView(16);
    }

    public ImageView getIconView(int size) {
        return IconManager.imageView(iconName, size);
    }
}