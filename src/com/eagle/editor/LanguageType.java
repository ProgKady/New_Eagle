package com.eagle.editor;

import java.io.File;

public enum LanguageType {
    HTML, CSS, JAVASCRIPT, TYPESCRIPT, JSX, TSX,
    JSON, MARKDOWN, JAVA, XML, PLAIN,
    PHP, PYTHON, SQL, SCSS, LESS, SASS,
    YAML, SH, DOCKERFILE, ENV, GITIGNORE,
    SVG, VUE, SVELTE, ASTRO, C, CPP, KOTLIN, GO, RUST;

    public static LanguageType fromFile(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) return HTML;
        if (name.endsWith(".css")) return CSS;
        if (name.endsWith(".js") || name.endsWith(".mjs")) return JAVASCRIPT;
        if (name.endsWith(".ts")) return TYPESCRIPT;
        if (name.endsWith(".jsx")) return JSX;
        if (name.endsWith(".tsx")) return TSX;
        if (name.endsWith(".json")) return JSON;
        if (name.endsWith(".md") || name.endsWith(".markdown")) return MARKDOWN;
        if (name.endsWith(".java")) return JAVA;
        if (name.endsWith(".xml") || name.endsWith(".fxml") || name.endsWith(".svg")) return name.endsWith(".svg") ? SVG : XML;
        if (name.endsWith(".php")) return PHP;
        if (name.endsWith(".py")) return PYTHON;
        if (name.endsWith(".sql")) return SQL;
        if (name.endsWith(".scss")) return SCSS;
        if (name.endsWith(".less")) return LESS;
        if (name.endsWith(".sass")) return SASS;
        if (name.endsWith(".yml") || name.endsWith(".yaml")) return YAML;
        if (name.endsWith(".sh") || name.endsWith(".bash") || name.endsWith(".zsh")) return SH;
        if (name.endsWith(".dockerfile") || name.equals("dockerfile")) return DOCKERFILE;
        if (name.endsWith(".env")) return ENV;
        if (name.endsWith(".gitignore")) return GITIGNORE;
        if (name.endsWith(".properties") || name.endsWith(".dat")) return PLAIN;
        if (name.endsWith(".c")) return C;
        if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx")) return CPP;
        if (name.endsWith(".vue")) return VUE;
        if (name.endsWith(".svelte")) return SVELTE;
        if (name.endsWith(".astro")) return ASTRO;
        if (name.endsWith(".kt") || name.endsWith(".kts")) return KOTLIN;
        if (name.endsWith(".go")) return GO;
        if (name.endsWith(".rs")) return RUST;
        return PLAIN;
    }
}
