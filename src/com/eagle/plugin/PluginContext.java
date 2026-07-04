package com.eagle.plugin;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

public class PluginContext {
    private java.util.List<MenuItemRegistration> menuItems = new java.util.ArrayList<>();
    private java.util.List<ToolbarItemRegistration> toolbarItems = new java.util.ArrayList<>();
    private java.util.List<CommandRegistration> commands = new java.util.ArrayList<>();
    private java.util.List<ApkToolRegistration> apkTools = new java.util.ArrayList<>();
    private java.util.List<NewMenuRegistration> newMenus = new java.util.ArrayList<>();
    private java.util.List<ToolbarSectionRegistration> toolbarSections = new java.util.ArrayList<>();
    private java.util.List<PanelRegistration> panels = new java.util.ArrayList<>();

    public void registerMenuItem(String parentMenu, String label, EventHandler<ActionEvent> handler) {
        menuItems.add(new MenuItemRegistration(parentMenu, label, handler));
    }

    public void registerToolbarItem(String label, Node node) {
        toolbarItems.add(new ToolbarItemRegistration(label, node));
    }

    public void registerCommand(String name, String category, Runnable action) {
        commands.add(new CommandRegistration(name, category, action));
    }

    public void registerApkTool(String name, String description, Runnable action) {
        apkTools.add(new ApkToolRegistration(name, description, action));
    }

    public void registerNewMenu(String title, int position) {
        newMenus.add(new NewMenuRegistration(title, position));
    }

    public void registerToolbarSection(String sectionName, Node... buttons) {
        toolbarSections.add(new ToolbarSectionRegistration(sectionName, java.util.Arrays.asList(buttons)));
    }

    public void registerPanel(String title, Node content) {
        panels.add(new PanelRegistration(title, content));
    }

    public java.util.List<MenuItemRegistration> getMenuItems() { return menuItems; }
    public java.util.List<ToolbarItemRegistration> getToolbarItems() { return toolbarItems; }
    public java.util.List<CommandRegistration> getCommands() { return commands; }
    public java.util.List<ApkToolRegistration> getApkTools() { return apkTools; }
    public java.util.List<NewMenuRegistration> getNewMenus() { return newMenus; }
    public java.util.List<ToolbarSectionRegistration> getToolbarSections() { return toolbarSections; }
    public java.util.List<PanelRegistration> getPanels() { return panels; }

    public static class ApkToolRegistration {
        public final String name;
        public final String description;
        public final Runnable action;
        public ApkToolRegistration(String name, String description, Runnable action) {
            this.name = name;
            this.description = description;
            this.action = action;
        }
    }

    public static class MenuItemRegistration {
        public final String parentMenu;
        public final String label;
        public final EventHandler<ActionEvent> handler;
        public MenuItemRegistration(String parentMenu, String label, EventHandler<ActionEvent> handler) {
            this.parentMenu = parentMenu;
            this.label = label;
            this.handler = handler;
        }
    }

    public static class ToolbarItemRegistration {
        public final String label;
        public final Node node;
        public ToolbarItemRegistration(String label, Node node) {
            this.label = label;
            this.node = node;
        }
    }

    public static class CommandRegistration {
        public final String name;
        public final String category;
        public final Runnable action;
        public CommandRegistration(String name, String category, Runnable action) {
            this.name = name;
            this.category = category;
            this.action = action;
        }
    }

    public static class NewMenuRegistration {
        public final String title;
        public final int position;
        public NewMenuRegistration(String title, int position) {
            this.title = title;
            this.position = position;
        }
    }

    public static class ToolbarSectionRegistration {
        public final String sectionName;
        public final java.util.List<Node> buttons;
        public ToolbarSectionRegistration(String sectionName, java.util.List<Node> buttons) {
            this.sectionName = sectionName;
            this.buttons = buttons;
        }
    }

    public static class PanelRegistration {
        public final String title;
        public final Node content;
        public PanelRegistration(String title, Node content) {
            this.title = title;
            this.content = content;
        }
    }
}
