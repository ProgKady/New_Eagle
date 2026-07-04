package com.eagle.templates;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of all built-in starter templates.
 * Each template is fully self-contained — no placeholders.
 * Includes HTML/CSS/JS, framework SPA, SSR, and mobile-hybrid scaffolds.
 */
public class TemplateProvider {

    public static List<WebTemplate> getAll() {
        List<WebTemplate> list = new ArrayList<>();
        // Existing web templates
        list.add(blank());
        list.add(landingPage());
        list.add(portfolio());
        list.add(blogTemplate());
        list.add(adminDashboard());
        list.add(loginForm());
        list.add(restaurantMenu());
        list.add(oneCard());
        list.add(ecommerce());
        list.add(documentation());
        list.add(contactForm());
        // Existing Android JS templates
        list.add(androidBlank());
        list.add(androidTodo());
        list.add(androidWeather());
        list.add(androidCalculator());
        list.add(androidListDemo());
        // ---- NEW FRAMEWORK TEMPLATES ----
        list.add(reactVite());
        list.add(vueVite());
        list.add(svelteVite());
        list.add(nextJsScaffold());
        list.add(nuxtScaffold());
        list.add(alpineJsScaffold());
        list.add(astroScaffold());
        list.add(solidJsVite());
        list.add(litVite());
        list.add(viteTypescript());
        list.add(quasarScaffold());
        list.add(framework7Scaffold());
        list.add(onsenUiScaffold());
        list.add(cordovaScaffold());
        list.add(capacitorScaffold());
        return list;
    }

    // ===================================================================
    //  EXISTING WEB TEMPLATES
    // ===================================================================

    private static WebTemplate blank() {
        WebTemplate t = new WebTemplate("blank", "Blank Project", "Empty starter with linked HTML/CSS/JS.", "html");
        t.addFile("index.html",
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>New Project</title>\n" +
                "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Hello, World!</h1>\n" +
                "    <p>Start building your project.</p>\n" +
                "    <script src=\"script.js\"></script>\n" +
                "</body>\n" +
                "</html>\n");
        t.addFile("style.css",
                "* { margin: 0; padding: 0; box-sizing: border-box; }\n\n" +
                "body {\n" +
                "    font-family: -apple-system, Segoe UI, Arial, sans-serif;\n" +
                "    margin: 40px;\n" +
                "    color: #222;\n" +
                "}\n");
        t.addFile("script.js", "console.log('Project loaded');\n");
        return t;
    }

    private static WebTemplate landingPage() {
        WebTemplate t = new WebTemplate("landing", "Landing Page", "Hero, features, pricing, CTA section.", "rocket");
        t.addFile("index.html",
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Nova — Landing Page</title>\n" +
                "<link rel=\"stylesheet\" href=\"style.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "  <header class=\"nav\">\n" +
                "    <div class=\"nav-inner\">\n" +
                "      <span class=\"logo\">Nova</span>\n" +
                "      <nav class=\"links\">\n" +
                "        <a href=\"#features\">Features</a>\n" +
                "        <a href=\"#pricing\">Pricing</a>\n" +
                "        <a href=\"#contact\">Contact</a>\n" +
                "      </nav>\n" +
                "      <button class=\"btn-primary\" id=\"ctaTop\">Get Started</button>\n" +
                "    </div>\n" +
                "  </header>\n\n" +
                "  <section class=\"hero\">\n" +
                "    <h1>Build faster. Ship sooner.</h1>\n" +
                "    <p>Nova helps your team launch products without the busywork.</p>\n" +
                "    <button class=\"btn-primary btn-lg\" id=\"ctaHero\">Start free trial</button>\n" +
                "  </section>\n\n" +
                "  <section class=\"features\" id=\"features\">\n" +
                "    <div class=\"feature-card\">\n" +
                "      <h3>Fast</h3>\n" +
                "      <p>Optimized pipelines that cut build times in half.</p>\n" +
                "    </div>\n" +
                "    <div class=\"feature-card\">\n" +
                "      <h3>Secure</h3>\n" +
                "      <p>End-to-end encryption on every request, by default.</p>\n" +
                "    </div>\n" +
                "    <div class=\"feature-card\">\n" +
                "      <h3>Scalable</h3>\n" +
                "      <p>From one user to one million, with zero config.</p>\n" +
                "    </div>\n" +
                "  </section>\n\n" +
                "  <section class=\"pricing\" id=\"pricing\">\n" +
                "    <h2>Simple pricing</h2>\n" +
                "    <div class=\"price-cards\">\n" +
                "      <div class=\"price-card\">\n" +
                "        <h3>Starter</h3>\n" +
                "        <p class=\"price\">$0<span>/mo</span></p>\n" +
                "        <button class=\"btn-secondary\">Choose Starter</button>\n" +
                "      </div>\n" +
                "      <div class=\"price-card featured\">\n" +
                "        <h3>Pro</h3>\n" +
                "        <p class=\"price\">$19<span>/mo</span></p>\n" +
                "        <button class=\"btn-primary\">Choose Pro</button>\n" +
                "      </div>\n" +
                "      <div class=\"price-card\">\n" +
                "        <h3>Team</h3>\n" +
                "        <p class=\"price\">$49<span>/mo</span></p>\n" +
                "        <button class=\"btn-secondary\">Choose Team</button>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </section>\n\n" +
                "  <footer id=\"contact\">\n" +
                "    <p>&copy; 2026 Nova Inc. All rights reserved.</p>\n" +
                "  </footer>\n\n" +
                "  <script src=\"script.js\"></script>\n" +
                "</body>\n" +
                "</html>\n");
        t.addFile("style.css",
                "* { margin:0; padding:0; box-sizing:border-box; }\n" +
                "body { font-family:-apple-system,Segoe UI,Arial,sans-serif; color:#1d1e25; }\n\n" +
                ".nav { border-bottom:1px solid #eee; position:sticky; top:0; background:#fff; z-index:10; }\n" +
                ".nav-inner { max-width:1100px; margin:0 auto; display:flex; align-items:center; gap:24px; padding:16px 24px; }\n" +
                ".logo { font-weight:800; font-size:20px; color:#6c5ce7; }\n" +
                ".links { display:flex; gap:20px; flex:1; }\n" +
                ".links a { text-decoration:none; color:#555; font-size:14px; }\n\n" +
                ".btn-primary { background:#6c5ce7; color:#fff; border:none; padding:10px 20px; border-radius:8px; font-weight:600; cursor:pointer; }\n" +
                ".btn-primary:hover { background:#5a4bd4; }\n" +
                ".btn-secondary { background:transparent; border:1.5px solid #ddd; padding:10px 20px; border-radius:8px; font-weight:600; cursor:pointer; }\n" +
                ".btn-lg { padding:14px 30px; font-size:16px; }\n\n" +
                ".hero { text-align:center; padding:100px 24px 80px; background:linear-gradient(180deg,#f7f5ff,#fff); }\n" +
                ".hero h1 { font-size:46px; margin-bottom:16px; }\n" +
                ".hero p { font-size:18px; color:#666; margin-bottom:30px; }\n\n" +
                ".features { display:grid; grid-template-columns:repeat(3,1fr); gap:24px; max-width:1100px; margin:0 auto; padding:60px 24px; }\n" +
                ".feature-card { border:1px solid #eee; border-radius:12px; padding:28px; }\n" +
                ".feature-card h3 { margin-bottom:10px; }\n" +
                ".feature-card p { color:#666; font-size:14px; }\n\n" +
                ".pricing { text-align:center; padding:60px 24px; background:#fafafa; }\n" +
                ".pricing h2 { margin-bottom:40px; font-size:30px; }\n" +
                ".price-cards { display:flex; gap:20px; justify-content:center; flex-wrap:wrap; }\n" +
                ".price-card { border:1px solid #eee; border-radius:12px; padding:30px; width:220px; background:#fff; }\n" +
                ".price-card.featured { border-color:#6c5ce7; transform:scale(1.05); box-shadow:0 10px 30px rgba(108,92,231,0.15); }\n" +
                ".price { font-size:30px; font-weight:800; margin:14px 0; }\n" +
                ".price span { font-size:14px; color:#888; font-weight:400; }\n\n" +
                "footer { text-align:center; padding:30px; color:#999; font-size:13px; }\n");
        t.addFile("script.js",
                "document.getElementById('ctaTop').addEventListener('click', startTrial);\n" +
                "document.getElementById('ctaHero').addEventListener('click', startTrial);\n\n" +
                "function startTrial() {\n" +
                "    alert('Welcome! Your free trial is starting...');\n" +
                "}\n");
        return t;
    }

    private static WebTemplate portfolio() {
        WebTemplate t = new WebTemplate("portfolio", "Portfolio", "Personal portfolio with projects grid.", "palette");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Jordan Lee — Portfolio</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <header class=\"hero\">\n" +
                "    <img src=\"avatar.png\" alt=\"avatar\" class=\"avatar\" onerror=\"this.style.display='none'\">\n" +
                "    <h1>Jordan Lee</h1>\n" +
                "    <p>Front-end Developer &amp; UI Designer</p>\n" +
                "    <div class=\"social\">\n" +
                "      <a href=\"#\">GitHub</a><a href=\"#\">LinkedIn</a><a href=\"#\">Twitter</a>\n" +
                "    </div>\n" +
                "  </header>\n\n" +
                "  <section class=\"projects\">\n" +
                "    <h2>Projects</h2>\n" +
                "    <div class=\"grid\" id=\"projectGrid\"></div>\n" +
                "  </section>\n\n" +
                "  <footer><p>&copy; 2026 Jordan Lee</p></footer>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:-apple-system,Segoe UI,Arial,sans-serif;background:#0f0f14;color:#eee;}\n" +
                ".hero{text-align:center;padding:80px 20px 50px;}\n" +
                ".avatar{width:96px;height:96px;border-radius:50%;background:#333;margin-bottom:16px;}\n" +
                ".hero h1{font-size:32px;}\n" +
                ".hero p{color:#aaa;margin:8px 0 18px;}\n" +
                ".social a{color:#8e7cf2;margin:0 10px;text-decoration:none;font-size:14px;}\n" +
                ".projects{max-width:1000px;margin:0 auto;padding:30px 20px 60px;}\n" +
                ".projects h2{margin-bottom:24px;font-size:22px;}\n" +
                ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:20px;}\n" +
                ".card{background:#1a1a22;border-radius:12px;padding:20px;border:1px solid #2a2a35;transition:transform .15s;}\n" +
                ".card:hover{transform:translateY(-4px);border-color:#6c5ce7;}\n" +
                ".card h3{margin-bottom:8px;font-size:16px;}\n" +
                ".card p{color:#999;font-size:13px;line-height:1.5;}\n" +
                "footer{text-align:center;padding:24px;color:#777;font-size:12px;border-top:1px solid #222;}\n");
        t.addFile("script.js",
                "const projects = [\n" +
                "    { title: 'E-commerce Dashboard', desc: 'Analytics dashboard built with vanilla JS and Chart.js.' },\n" +
                "    { title: 'Recipe Finder', desc: 'Search recipes by ingredients using a public API.' },\n" +
                "    { title: 'Weather App', desc: 'Real-time weather with animated icons.' },\n" +
                "    { title: 'Task Manager', desc: 'Drag-and-drop kanban board with local storage.' }\n" +
                "];\n\n" +
                "const grid = document.getElementById('projectGrid');\n" +
                "projects.forEach(p => {\n" +
                "    const card = document.createElement('div');\n" +
                "    card.className = 'card';\n" +
                "    card.innerHTML = `<h3>${p.title}</h3><p>${p.desc}</p>`;\n" +
                "    grid.appendChild(card);\n" +
                "});\n");
        return t;
    }

    private static WebTemplate blogTemplate() {
        WebTemplate t = new WebTemplate("blog", "Blog", "Article list with sidebar layout.", "article");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Wander — Travel Blog</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <header><h1>Wander</h1><p>Stories from the road</p></header>\n" +
                "  <div class=\"layout\">\n" +
                "    <main id=\"posts\"></main>\n" +
                "    <aside>\n" +
                "      <h3>About</h3>\n" +
                "      <p>Travel notes, photos and tips from around the world.</p>\n" +
                "      <h3>Categories</h3>\n" +
                "      <ul><li>Asia</li><li>Europe</li><li>Food</li><li>Tips</li></ul>\n" +
                "    </aside>\n" +
                "  </div>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:Georgia,serif;color:#2c2c2c;background:#fdfcfb;}\n" +
                "header{text-align:center;padding:50px 20px 30px;border-bottom:1px solid #eee;}\n" +
                "header h1{font-size:36px;}\nheader p{color:#888;font-style:italic;}\n" +
                ".layout{max-width:960px;margin:0 auto;display:grid;grid-template-columns:2fr 1fr;gap:40px;padding:40px 20px;}\n" +
                ".post{margin-bottom:40px;padding-bottom:30px;border-bottom:1px solid #eee;}\n" +
                ".post h2{font-size:24px;margin-bottom:6px;}\n" +
                ".post .meta{color:#999;font-size:13px;margin-bottom:12px;font-family:sans-serif;}\n" +
                ".post p{line-height:1.7;color:#444;}\n" +
                "aside h3{font-family:sans-serif;font-size:14px;text-transform:uppercase;color:#999;margin:20px 0 10px;}\n" +
                "aside ul{list-style:none;}\naside li{padding:6px 0;border-bottom:1px solid #eee;font-family:sans-serif;font-size:14px;}\n");
        t.addFile("script.js",
                "const posts = [\n" +
                "    { title: 'Three weeks in Kyoto', date: 'May 2, 2026', excerpt: 'Quiet temples, cherry blossoms, and the best ramen of my life.' },\n" +
                "    { title: 'Hiking the Dolomites', date: 'Apr 14, 2026', excerpt: 'A five-day trek through Italy most dramatic peaks.' },\n" +
                "    { title: 'Street food in Bangkok', date: 'Mar 28, 2026', excerpt: 'A guide to the night markets you should not miss.' }\n" +
                "];\n\n" +
                "const container = document.getElementById('posts');\n" +
                "posts.forEach(p => {\n" +
                "    const el = document.createElement('article');\n" +
                "    el.className = 'post';\n" +
                "    el.innerHTML = `<h2>${p.title}</h2><div class=\"meta\">${p.date}</div><p>${p.excerpt}</p>`;\n" +
                "    container.appendChild(el);\n" +
                "});\n");
        return t;
    }

    private static WebTemplate adminDashboard() {
        WebTemplate t = new WebTemplate("admin", "Admin Dashboard", "Sidebar nav, stat cards, table.", "chart");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Admin Dashboard</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <div class=\"layout\">\n" +
                "    <nav class=\"sidebar\">\n" +
                "      <div class=\"brand\">Admin</div>\n" +
                "      <a class=\"active\" href=\"#\">Dashboard</a>\n" +
                "      <a href=\"#\">Users</a>\n" +
                "      <a href=\"#\">Orders</a>\n" +
                "      <a href=\"#\">Settings</a>\n" +
                "    </nav>\n" +
                "    <main>\n" +
                "      <div class=\"stats\" id=\"stats\"></div>\n" +
                "      <table id=\"table\">\n" +
                "        <thead><tr><th>User</th><th>Email</th><th>Status</th></tr></thead>\n" +
                "        <tbody></tbody>\n" +
                "      </table>\n" +
                "    </main>\n" +
                "  </div>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:-apple-system,Segoe UI,Arial,sans-serif;background:#f4f5f9;}\n" +
                ".layout{display:flex;min-height:100vh;}\n" +
                ".sidebar{width:200px;background:#1d1e2c;color:#fff;padding:20px 0;}\n" +
                ".brand{font-weight:800;padding:0 20px 20px;font-size:18px;}\n" +
                ".sidebar a{display:block;padding:12px 20px;color:#aab;text-decoration:none;font-size:14px;}\n" +
                ".sidebar a.active,.sidebar a:hover{background:#2a2b3d;color:#fff;}\n" +
                "main{flex:1;padding:30px;}\n" +
                ".stats{display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin-bottom:30px;}\n" +
                ".stat-card{background:#fff;border-radius:10px;padding:20px;box-shadow:0 2px 6px rgba(0,0,0,0.05);}\n" +
                ".stat-card h4{color:#888;font-size:12px;text-transform:uppercase;margin-bottom:8px;}\n" +
                ".stat-card .val{font-size:26px;font-weight:800;}\n" +
                "table{width:100%;background:#fff;border-radius:10px;border-collapse:collapse;overflow:hidden;}\n" +
                "th,td{padding:12px 16px;text-align:left;border-bottom:1px solid #eee;font-size:14px;}\n" +
                "th{background:#fafafa;color:#888;font-size:12px;text-transform:uppercase;}\n" +
                ".badge{padding:3px 10px;border-radius:12px;font-size:12px;}\n" +
                ".badge.active{background:#e3fbe8;color:#1e9e4b;}\n" +
                ".badge.pending{background:#fff4e0;color:#c98a13;}\n");
        t.addFile("script.js",
                "const stats = [\n" +
                "    { label: 'Total Users', value: '4,218' },\n" +
                "    { label: 'Revenue', value: '$28,940' },\n" +
                "    { label: 'Open Tickets', value: '12' }\n" +
                "];\n" +
                "const users = [\n" +
                "    { name: 'Amelia Hart', email: 'amelia@mail.com', status: 'active' },\n" +
                "    { name: 'Noah Kim', email: 'noah@mail.com', status: 'pending' },\n" +
                "    { name: 'Sara Diaz', email: 'sara@mail.com', status: 'active' }\n" +
                "];\n\n" +
                "const statsEl = document.getElementById('stats');\n" +
                "stats.forEach(s => {\n" +
                "    const div = document.createElement('div');\n" +
                "    div.className = 'stat-card';\n" +
                "    div.innerHTML = `<h4>${s.label}</h4><div class=\"val\">${s.value}</div>`;\n" +
                "    statsEl.appendChild(div);\n" +
                "});\n\n" +
                "const tbody = document.querySelector('#table tbody');\n" +
                "users.forEach(u => {\n" +
                "    const tr = document.createElement('tr');\n" +
                "    tr.innerHTML = `<td>${u.name}</td><td>${u.email}</td><td><span class=\"badge ${u.status}\">${u.status}</span></td>`;\n" +
                "    tbody.appendChild(tr);\n" +
                "});\n");
        return t;
    }

    private static WebTemplate loginForm() {
        WebTemplate t = new WebTemplate("login", "Login Page", "Centered card login/sign-up form.", "lock");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Sign In</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <div class=\"card\">\n" +
                "    <h1>Welcome back</h1>\n" +
                "    <p class=\"sub\">Sign in to continue</p>\n" +
                "    <form id=\"loginForm\">\n" +
                "      <label>Email</label>\n" +
                "      <input type=\"email\" id=\"email\" placeholder=\"you@example.com\" required>\n" +
                "      <label>Password</label>\n" +
                "      <input type=\"password\" id=\"password\" placeholder=\"......\" required>\n" +
                "      <button type=\"submit\">Sign In</button>\n" +
                "    </form>\n" +
                "    <p class=\"error\" id=\"errorMsg\"></p>\n" +
                "    <p class=\"alt\">Don't have an account? <a href=\"#\">Sign up</a></p>\n" +
                "  </div>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:-apple-system,Segoe UI,Arial,sans-serif;background:linear-gradient(135deg,#6c5ce7,#a29bfe);min-height:100vh;display:flex;align-items:center;justify-content:center;}\n" +
                ".card{background:#fff;border-radius:16px;padding:40px;width:340px;box-shadow:0 20px 50px rgba(0,0,0,0.2);}\n" +
                ".card h1{font-size:24px;margin-bottom:4px;}\n.sub{color:#888;font-size:14px;margin-bottom:24px;}\n" +
                "label{display:block;font-size:13px;color:#555;margin-bottom:6px;margin-top:14px;}\n" +
                "input{width:100%;padding:11px 14px;border:1.5px solid #e0e0e0;border-radius:8px;font-size:14px;}\n" +
                "input:focus{outline:none;border-color:#6c5ce7;}\n" +
                "button{width:100%;margin-top:22px;padding:12px;background:#6c5ce7;color:#fff;border:none;border-radius:8px;font-weight:600;cursor:pointer;font-size:14px;}\n" +
                "button:hover{background:#5a4bd4;}\n" +
                ".error{color:#e74c3c;font-size:13px;margin-top:12px;min-height:14px;}\n" +
                ".alt{text-align:center;font-size:13px;color:#888;margin-top:16px;}\n.alt a{color:#6c5ce7;text-decoration:none;font-weight:600;}\n");
        t.addFile("script.js",
                "document.getElementById('loginForm').addEventListener('submit', function(e) {\n" +
                "    e.preventDefault();\n" +
                "    const email = document.getElementById('email').value;\n" +
                "    const password = document.getElementById('password').value;\n" +
                "    const errorEl = document.getElementById('errorMsg');\n\n" +
                "    if (password.length < 6) {\n" +
                "        errorEl.textContent = 'Password must be at least 6 characters.';\n" +
                "        return;\n" +
                "    }\n" +
                "    errorEl.textContent = '';\n" +
                "    console.log('Signing in as', email);\n" +
                "    alert('Signed in as ' + email);\n" +
                "});\n");
        return t;
    }

    private static WebTemplate restaurantMenu() {
        WebTemplate t = new WebTemplate("restaurant", "Restaurant Menu", "Menu sections with prices.", "food");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Bella Notte — Menu</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <header><h1>Bella Notte</h1><p>Italian Kitchen &amp; Wine Bar</p></header>\n" +
                "  <main id=\"menu\"></main>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:Georgia,serif;background:#1c1410;color:#f3e9d8;}\n" +
                "header{text-align:center;padding:60px 20px 30px;}\n" +
                "header h1{font-size:38px;color:#e8c468;}\nheader p{color:#bba78a;margin-top:6px;}\n" +
                "main{max-width:700px;margin:0 auto;padding:20px 24px 60px;}\n" +
                ".section h2{color:#e8c468;border-bottom:1px solid #4a3a28;padding-bottom:8px;margin:30px 0 16px;font-size:20px;}\n" +
                ".item{display:flex;justify-content:space-between;margin-bottom:14px;}\n" +
                ".item .name{font-weight:bold;}\n.item .desc{color:#bba78a;font-size:13px;display:block;margin-top:2px;}\n" +
                ".item .price{color:#e8c468;font-weight:bold;white-space:nowrap;margin-left:20px;}\n");
        t.addFile("script.js",
                "const menu = {\n" +
                "    'Starters': [\n" +
                "        { name: 'Bruschetta', desc: 'Grilled bread, tomato, basil', price: '$8' },\n" +
                "        { name: 'Caprese Salad', desc: 'Mozzarella, tomato, balsamic', price: '$10' }\n" +
                "    ],\n" +
                "    'Main Course': [\n" +
                "        { name: 'Margherita Pizza', desc: 'San Marzano tomato, mozzarella', price: '$14' },\n" +
                "        { name: 'Spaghetti Carbonara', desc: 'Egg, pancetta, pecorino', price: '$16' }\n" +
                "    ],\n" +
                "    'Desserts': [\n" +
                "        { name: 'Tiramisu', desc: 'Espresso, mascarpone, cocoa', price: '$7' }\n" +
                "    ]\n" +
                "};\n\n" +
                "const container = document.getElementById('menu');\n" +
                "Object.keys(menu).forEach(section => {\n" +
                "    const sec = document.createElement('div');\n" +
                "    sec.className = 'section';\n" +
                "    sec.innerHTML = `<h2>${section}</h2>`;\n" +
                "    menu[section].forEach(item => {\n" +
                "        const row = document.createElement('div');\n" +
                "        row.className = 'item';\n" +
                "        row.innerHTML = `<div><span class=\"name\">${item.name}</span><span class=\"desc\">${item.desc}</span></div><div class=\"price\">${item.price}</div>`;\n" +
                "        sec.appendChild(row);\n" +
                "    });\n" +
                "    container.appendChild(sec);\n" +
                "});\n");
        return t;
    }

    private static WebTemplate oneCard() {
        WebTemplate t = new WebTemplate("coming-soon", "Coming Soon", "Centered countdown / email capture page.", "clock");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Coming Soon</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <div class=\"wrap\">\n" +
                "    <h1>We're launching soon</h1>\n" +
                "    <p>Leave your email and we'll let you know the moment we go live.</p>\n" +
                "    <div class=\"timer\" id=\"timer\">--:--:--:--</div>\n" +
                "    <form id=\"emailForm\">\n" +
                "      <input type=\"email\" id=\"emailInput\" placeholder=\"you@example.com\" required>\n" +
                "      <button type=\"submit\">Notify me</button>\n" +
                "    </form>\n" +
                "    <p class=\"msg\" id=\"msg\"></p>\n" +
                "  </div>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:-apple-system,Segoe UI,Arial,sans-serif;background:#0d0d12;color:#fff;min-height:100vh;display:flex;align-items:center;justify-content:center;}\n" +
                ".wrap{text-align:center;max-width:480px;padding:20px;}\n" +
                "h1{font-size:32px;margin-bottom:12px;}\np{color:#999;margin-bottom:30px;}\n" +
                ".timer{font-size:28px;letter-spacing:4px;color:#8e7cf2;margin-bottom:30px;font-family:monospace;}\n" +
                "form{display:flex;gap:10px;}\n" +
                "input{flex:1;padding:12px 14px;border-radius:8px;border:1px solid #333;background:#1a1a22;color:#fff;}\n" +
                "button{padding:12px 20px;border-radius:8px;border:none;background:#6c5ce7;color:#fff;font-weight:600;cursor:pointer;}\n" +
                "button:hover{background:#5a4bd4;}\n.msg{margin-top:14px;color:#6c5ce7;font-size:14px;}\n");
        t.addFile("script.js",
                "const launchDate = new Date().getTime() + (5 * 24 * 60 * 60 * 1000);\n\n" +
                "function updateTimer() {\n" +
                "    const now = new Date().getTime();\n" +
                "    const diff = launchDate - now;\n" +
                "    if (diff <= 0) { document.getElementById('timer').textContent = '00:00:00:00'; return; }\n" +
                "    const d = Math.floor(diff / (1000 * 60 * 60 * 24));\n" +
                "    const h = Math.floor((diff / (1000 * 60 * 60)) % 24);\n" +
                "    const m = Math.floor((diff / (1000 * 60)) % 60);\n" +
                "    const s = Math.floor((diff / 1000) % 60);\n" +
                "    document.getElementById('timer').textContent =\n" +
                "        [d, h, m, s].map(n => String(n).padStart(2, '0')).join(':');\n" +
                "}\n" +
                "setInterval(updateTimer, 1000);\n" +
                "updateTimer();\n\n" +
                "document.getElementById('emailForm').addEventListener('submit', function(e) {\n" +
                "    e.preventDefault();\n" +
                "    document.getElementById('msg').textContent = 'Thanks! We will notify you at launch.';\n" +
                "    document.getElementById('emailInput').value = '';\n" +
                "});\n");
        return t;
    }

    private static WebTemplate ecommerce() {
        WebTemplate t = new WebTemplate("ecommerce", "E-commerce", "Product grid, cart, and checkout page.", "cart");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Shop — Store</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <header>\n" +
                "    <div class=\"header-inner\">\n" +
                "      <span class=\"logo\">Shop</span>\n" +
                "      <nav>\n" +
                "        <a href=\"#\">Home</a>\n" +
                "        <a href=\"#\">Products</a>\n" +
                "        <a href=\"#\">Cart</a>\n" +
                "      </nav>\n" +
                "    </div>\n" +
                "  </header>\n" +
                "  <main>\n" +
                "    <h1>Featured Products</h1>\n" +
                "    <div class=\"product-grid\" id=\"productGrid\"></div>\n" +
                "  </main>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:-apple-system,Segoe UI,Arial,sans-serif;background:#f8f9fa;color:#222;}\n" +
                "header{border-bottom:1px solid #eee;background:#fff;}\n" +
                ".header-inner{max-width:1100px;margin:0 auto;display:flex;align-items:center;gap:24px;padding:14px 20px;}\n" +
                ".logo{font-weight:800;font-size:20px;color:#333;}\n" +
                "nav{flex:1;}nav a{margin-right:16px;text-decoration:none;color:#555;font-size:14px;}\n" +
                "main{max-width:1100px;margin:0 auto;padding:30px 20px;}\n" +
                "h1{font-size:24px;margin-bottom:24px;}\n" +
                ".product-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:20px;}\n" +
                ".product-card{background:#fff;border-radius:12px;padding:20px;border:1px solid #eee;text-align:center;}\n" +
                ".product-card .img{width:100%;height:120px;background:#f0f0f0;border-radius:8px;margin-bottom:12px;display:flex;align-items:center;justify-content:center;color:#bbb;font-size:13px;}\n" +
                ".product-card h3{font-size:14px;margin-bottom:4px;}\n" +
                ".product-card .price{color:#6c5ce7;font-weight:700;font-size:16px;}\n" +
                ".product-card button{margin-top:10px;padding:8px 16px;background:#6c5ce7;color:#fff;border:none;border-radius:6px;cursor:pointer;}\n");
        t.addFile("script.js",
                "const products = [\n" +
                "    { name: 'Wireless Headphones', price: '$59', img: 'img1' },\n" +
                "    { name: 'Leather Backpack', price: '$89', img: 'img2' },\n" +
                "    { name: 'Smart Watch', price: '$129', img: 'img3' },\n" +
                "    { name: 'Minimalist Wallet', price: '$34', img: 'img4' },\n" +
                "    { name: 'Desk Lamp', price: '$45', img: 'img5' },\n" +
                "    { name: 'Running Shoes', price: '$99', img: 'img6' }\n" +
                "];\n\n" +
                "const grid = document.getElementById('productGrid');\n" +
                "products.forEach(p => {\n" +
                "    const card = document.createElement('div');\n" +
                "    card.className = 'product-card';\n" +
                "    card.innerHTML = `<div class=\"img\">${p.img}</div><h3>${p.name}</h3><div class=\"price\">${p.price}</div><button>Add to Cart</button>`;\n" +
                "    grid.appendChild(card);\n" +
                "});\n");
        return t;
    }

    private static WebTemplate documentation() {
        WebTemplate t = new WebTemplate("docs", "Documentation", "Sidebar docs layout with search and sections.", "book");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Docs — Framework</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <div class=\"layout\">\n" +
                "    <nav class=\"sidebar\">\n" +
                "      <div class=\"brand\">Docs</div>\n" +
                "      <input type=\"text\" placeholder=\"Search...\" class=\"search\">\n" +
                "      <a class=\"active\" href=\"#\">Getting Started</a>\n" +
                "      <a href=\"#\">Installation</a>\n" +
                "      <a href=\"#\">Configuration</a>\n" +
                "      <a href=\"#\">API Reference</a>\n" +
                "      <a href=\"#\">Examples</a>\n" +
                "      <a href=\"#\">Tutorials</a>\n" +
                "    </nav>\n" +
                "    <main>\n" +
                "      <h1>Getting Started</h1>\n" +
                "      <p>Welcome to the Framework documentation. This guide will help you get up and running quickly.</p>\n" +
                "      <h2>Installation</h2>\n" +
                "      <pre><code>npm install framework</code></pre>\n" +
                "      <h2>Quick Start</h2>\n" +
                "      <p>Create a new project and start building.</p>\n" +
                "    </main>\n" +
                "  </div>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:-apple-system,Segoe UI,Arial,sans-serif;color:#222;background:#fff;}\n" +
                ".layout{display:flex;min-height:100vh;}\n" +
                ".sidebar{width:240px;background:#f8f9fa;border-right:1px solid #eee;padding:20px 0;}\n" +
                ".brand{font-weight:800;padding:0 20px 16px;font-size:18px;}\n" +
                ".search{margin:0 16px 12px;padding:8px 12px;border:1px solid #ddd;border-radius:6px;width:calc(100% - 32px);font-size:13px;}\n" +
                ".sidebar a{display:block;padding:10px 20px;color:#555;text-decoration:none;font-size:14px;}\n" +
                ".sidebar a.active,.sidebar a:hover{background:#e8e7ff;color:#6c5ce7;font-weight:600;}\n" +
                "main{flex:1;padding:30px 40px;max-width:800px;}\n" +
                "h1{font-size:28px;margin-bottom:12px;}\n" +
                "h2{font-size:20px;margin:24px 0 10px;}\n" +
                "p{line-height:1.7;color:#555;margin-bottom:16px;}\n" +
                "pre{background:#1d1e2c;color:#fff;padding:16px;border-radius:8px;overflow-x:auto;margin-bottom:20px;}\n" +
                "code{font-family:Consolas,monospace;}\n");
        t.addFile("script.js",
                "document.querySelector('.search').addEventListener('input', function(e) {\n" +
                "    const q = e.target.value.toLowerCase();\n" +
                "    document.querySelectorAll('.sidebar a').forEach(a => {\n" +
                "        a.style.display = a.textContent.toLowerCase().includes(q) ? 'block' : 'none';\n" +
                "    });\n" +
                "});\n");
        return t;
    }

    private static WebTemplate contactForm() {
        WebTemplate t = new WebTemplate("contact", "Contact Form", "Contact page with form, map placeholder, and social links.", "mail");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Contact Us</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n" +
                "  <div class=\"container\">\n" +
                "    <h1>Get in touch</h1>\n" +
                "    <p class=\"sub\">We'd love to hear from you. Send us a message.</p>\n" +
                "    <div class=\"grid\">\n" +
                "      <form id=\"contactForm\">\n" +
                "        <input type=\"text\" placeholder=\"Your Name\" required>\n" +
                "        <input type=\"email\" placeholder=\"Your Email\" required>\n" +
                "        <input type=\"text\" placeholder=\"Subject\">\n" +
                "        <textarea rows=\"5\" placeholder=\"Message\" required></textarea>\n" +
                "        <button type=\"submit\">Send Message</button>\n" +
                "      </form>\n" +
                "      <div class=\"info\">\n" +
                "        <h3>Email</h3>\n" +
                "        <p>hello@example.com</p>\n" +
                "        <h3>Phone</h3>\n" +
                "        <p>+1 (555) 123-4567</p>\n" +
                "        <h3>Address</h3>\n" +
                "        <p>123 Main Street, City</p>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script src=\"script.js\"></script>\n</body>\n</html>\n");
        t.addFile("style.css",
                "*{margin:0;padding:0;box-sizing:border-box;}\n" +
                "body{font-family:-apple-system,Segoe UI,Arial,sans-serif;background:#f8f9fa;color:#222;padding:40px 20px;}\n" +
                ".container{max-width:800px;margin:0 auto;}\n" +
                "h1{font-size:32px;margin-bottom:6px;}\n.sub{color:#888;margin-bottom:30px;}\n" +
                ".grid{display:grid;grid-template-columns:1.5fr 1fr;gap:40px;}\n" +
                "form{display:flex;flex-direction:column;gap:14px;}\n" +
                "input,textarea{padding:12px 14px;border:1.5px solid #ddd;border-radius:8px;font-size:14px;font-family:inherit;}\n" +
                "input:focus,textarea:focus{outline:none;border-color:#6c5ce7;}\n" +
                "button{padding:12px;background:#6c5ce7;color:#fff;border:none;border-radius:8px;font-weight:600;cursor:pointer;font-size:14px;}\n" +
                "button:hover{background:#5a4bd4;}\n" +
                ".info h3{font-size:13px;color:#888;margin-top:18px;margin-bottom:4px;text-transform:uppercase;}\n" +
                ".info p{color:#555;font-size:14px;}\n");
        t.addFile("script.js",
                "document.getElementById('contactForm').addEventListener('submit', function(e) {\n" +
                "    e.preventDefault();\n" +
                "    alert('Thank you! Your message has been sent.');\n" +
                "    this.reset();\n" +
                "});\n");
        return t;
    }

    // ===================================================================
    //  EXISTING ANDROID JS (DROIDSCRIPT) TEMPLATES
    // ===================================================================

    private static WebTemplate androidBlank() {
        WebTemplate t = new WebTemplate("android-blank", "Blank Android JS", "Empty DroidScript app with layout, text, and button.", "android");
        t.addFile("index.js",
                "// DroidScript App - Blank Starter\n" +
                "var app = app.Create();\n\n" +
                "// Create a vertical linear layout\n" +
                "var lay = app.CreateLayout(\"linear\", \"vertical\");\n\n" +
                "// Add a welcome text\n" +
                "var txt = app.CreateText(\"Hello from DroidScript!\");\n" +
                "txt.SetTextSize(22);\n" +
                "lay.AddChild(txt);\n\n" +
                "// Add a button\n" +
                "var btn = app.CreateButton(\"Click Me\");\n" +
                "btn.SetOnClick(function() {\n" +
                "    app.Alert(\"Hello! Welcome to DroidScript.\");\n" +
                "});\n" +
                "lay.AddChild(btn);\n\n" +
                "// Add the layout to the app\n" +
                "app.AddLayout(lay);\n");
        return t;
    }

    private static WebTemplate androidTodo() {
        WebTemplate t = new WebTemplate("android-todo", "Todo App", "DroidScript todo list with add/delete items.", "check");
        t.addFile("index.js",
                "// DroidScript - Todo List App\n" +
                "var app = app.Create();\n" +
                "var lay = app.CreateLayout(\"linear\", \"vertical\");\n" +
                "lay.SetPadding(16, 16, 16, 16);\n\n" +
                "var title = app.CreateText(\"My Todo List\");\n" +
                "title.SetTextSize(26);\n" +
                "title.SetMargins(0, 0, 0, 20);\n" +
                "lay.AddChild(title);\n\n" +
                "// Input row\n" +
                "var inputRow = app.CreateLayout(\"linear\", \"horizontal\");\n" +
                "var input = app.CreateTextEdit(\"\", -1, -1, 0);\n" +
                "input.SetHint(\"Enter a task...\");\n" +
                "input.SetMargins(0, 0, 8, 0);\n" +
                "inputRow.AddChild(input);\n\n" +
                "var addBtn = app.CreateButton(\"Add\");\n" +
                "addBtn.SetOnClick(function() {\n" +
                "    var text = input.GetText();\n" +
                "    if (text.length > 0) {\n" +
                "        addTodoItem(text);\n" +
                "        input.SetText(\"\");\n" +
                "    }\n" +
                "});\n" +
                "inputRow.AddChild(addBtn);\n" +
                "lay.AddChild(inputRow);\n\n" +
                "// Todo list container\n" +
                "var listLay = app.CreateLayout(\"linear\", \"vertical\");\n" +
                "lay.AddChild(listLay);\n\n" +
                "function addTodoItem(text) {\n" +
                "    var row = app.CreateLayout(\"linear\", \"horizontal\");\n" +
                "    row.SetMargins(0, 8, 0, 0);\n" +
                "    var label = app.CreateText(text);\n" +
                "    label.SetTextSize(16);\n" +
                "    row.AddChild(label);\n" +
                "    var delBtn = app.CreateButton(\"X\");\n" +
                "    delBtn.SetOnClick(function() {\n" +
                "        listLay.RemoveChild(row);\n" +
                "    });\n" +
                "    delBtn.SetPosition(1);\n" +
                "    row.AddChild(delBtn);\n" +
                "    listLay.AddChild(row);\n" +
                "}\n\n" +
                "app.AddLayout(lay);\n");
        return t;
    }

    private static WebTemplate androidWeather() {
        WebTemplate t = new WebTemplate("android-weather", "Weather App", "DroidScript weather display with mock data.", "sun");
        t.addFile("index.js",
                "// DroidScript - Weather App\n" +
                "var app = app.Create();\n" +
                "var lay = app.CreateLayout(\"linear\", \"vertical\");\n" +
                "lay.SetPadding(24, 24, 24, 24);\n\n" +
                "var cityText = app.CreateText(\"New York\");\n" +
                "cityText.SetTextSize(28);\n" +
                "cityText.SetMargins(0, 0, 0, 4);\n" +
                "lay.AddChild(cityText);\n\n" +
                "var tempText = app.CreateText(\"72°F\");\n" +
                "tempText.SetTextSize(56);\n" +
                "tempText.SetMargins(0, 0, 0, 4);\n" +
                "lay.AddChild(tempText);\n\n" +
                "var condText = app.CreateText(\"Partly Cloudy\");\n" +
                "condText.SetTextSize(18);\n" +
                "condText.SetMargins(0, 0, 0, 24);\n" +
                "lay.AddChild(condText);\n\n" +
                "// Forecast\n" +
                "var forecast = [\n" +
                "    { day: \"Mon\", hi: \"74\", lo: \"62\" },\n" +
                "    { day: \"Tue\", hi: \"78\", lo: \"65\" },\n" +
                "    { day: \"Wed\", hi: \"71\", lo: \"59\" },\n" +
                "    { day: \"Thu\", hi: \"69\", lo: \"57\" },\n" +
                "    { day: \"Fri\", hi: \"73\", lo: \"60\" }\n" +
                "];\n\n" +
                "for (var i = 0; i < forecast.length; i++) {\n" +
                "    var row = app.CreateLayout(\"linear\", \"horizontal\");\n" +
                "    var dayLbl = app.CreateText(forecast[i].day);\n" +
                "    dayLbl.SetTextSize(16);\n" +
                "    dayLbl.SetMargins(0, 8, 0, 0);\n" +
                "    row.AddChild(dayLbl);\n" +
                "    var tempLbl = app.CreateText(forecast[i].hi + \" / \" + forecast[i].lo);\n" +
                "    tempLbl.SetTextSize(16);\n" +
                "    tempLbl.SetPosition(1);\n" +
                "    tempLbl.SetMargins(0, 8, 0, 0);\n" +
                "    row.AddChild(tempLbl);\n" +
                "    lay.AddChild(row);\n" +
                "}\n\n" +
                "var refreshBtn = app.CreateButton(\"Refresh\");\n" +
                "refreshBtn.SetMargins(0, 24, 0, 0);\n" +
                "refreshBtn.SetOnClick(function() {\n" +
                "    app.Alert(\"Weather data refreshed!\");\n" +
                "});\n" +
                "lay.AddChild(refreshBtn);\n\n" +
                "app.AddLayout(lay);\n");
        return t;
    }

    private static WebTemplate androidCalculator() {
        WebTemplate t = new WebTemplate("android-calc", "Calculator", "DroidScript calculator with basic math operations.", "calc");
        t.addFile("index.js",
                "// DroidScript - Calculator App\n" +
                "var app = app.Create();\n" +
                "var lay = app.CreateLayout(\"linear\", \"vertical\");\n" +
                "lay.SetPadding(16, 16, 16, 16);\n\n" +
                "var display = app.CreateText(\"0\");\n" +
                "display.SetTextSize(40);\n" +
                "display.SetMargins(0, 0, 0, 20);\n" +
                "display.SetTextAlignment(2);\n" +
                "lay.AddChild(display);\n\n" +
                "var current = \"0\";\n" +
                "var operator = \"\";\n" +
                "var prev = \"\";\n\n" +
                "function btnClick(val) {\n" +
                "    if (val === \"C\") {\n" +
                "        current = \"0\"; operator = \"\"; prev = \"\";\n" +
                "    } else if (val === \"=\") {\n" +
                "        if (operator !== \"\" && prev !== \"\") {\n" +
                "            var a = parseFloat(prev);\n" +
                "            var b = parseFloat(current);\n" +
                "            var result = 0;\n" +
                "            if (operator === \"+\") result = a + b;\n" +
                "            else if (operator === \"-\") result = a - b;\n" +
                "            else if (operator === \"*\") result = a * b;\n" +
                "            else if (operator === \"/\" && b !== 0) result = a / b;\n" +
                "            current = String(result);\n" +
                "            operator = \"\"; prev = \"\";\n" +
                "        }\n" +
                "    } else if (\"+-*/\".indexOf(val) >= 0) {\n" +
                "        operator = val; prev = current; current = \"0\";\n" +
                "    } else {\n" +
                "        if (current === \"0\") current = val;\n" +
                "        else current += val;\n" +
                "    }\n" +
                "    display.SetText(current);\n" +
                "}\n\n" +
                "var buttons = [\n" +
                "    [\"7\",\"8\",\"9\",\"/\"],\n" +
                "    [\"4\",\"5\",\"6\",\"*\"],\n" +
                "    [\"1\",\"2\",\"3\",\"-\"],\n" +
                "    [\"C\",\"0\",\"=\",\"+\"]\n" +
                "];\n\n" +
                "for (var r = 0; r < buttons.length; r++) {\n" +
                "    var row = app.CreateLayout(\"linear\", \"horizontal\");\n" +
                "    for (var c = 0; c < buttons[r].length; c++) {\n" +
                "        (function(val) {\n" +
                "            var btn = app.CreateButton(buttons[r][c]);\n" +
                "            btn.SetOnClick(function() { btnClick(val); });\n" +
                "            row.AddChild(btn);\n" +
                "        })(buttons[r][c]);\n" +
                "    }\n" +
                "    lay.AddChild(row);\n" +
                "}\n\n" +
                "app.AddLayout(lay);\n");
        return t;
    }

    private static WebTemplate androidListDemo() {
        WebTemplate t = new WebTemplate("android-list", "List View Demo", "DroidScript list view with images and selection.", "list");
        t.addFile("index.js",
                "// DroidScript - List View Demo\n" +
                "var app = app.Create();\n" +
                "var lay = app.CreateLayout(\"linear\", \"vertical\");\n" +
                "lay.SetPadding(16, 16, 16, 16);\n\n" +
                "var title = app.CreateText(\"Contacts\");\n" +
                "title.SetTextSize(26);\n" +
                "title.SetMargins(0, 0, 0, 16);\n" +
                "lay.AddChild(title);\n\n" +
                "var data = [\n" +
                "    { name: \"Alice Johnson\", phone: \"555-0101\" },\n" +
                "    { name: \"Bob Smith\", phone: \"555-0102\" },\n" +
                "    { name: \"Carol Davis\", phone: \"555-0103\" },\n" +
                "    { name: \"David Wilson\", phone: \"555-0104\" },\n" +
                "    { name: \"Eve Martinez\", phone: \"555-0105\" },\n" +
                "    { name: \"Frank Brown\", phone: \"555-0106\" }\n" +
                "];\n\n" +
                "var list = app.CreateList(data.length, -1, -1);\n" +
                "list.SetOnTouch(function(index) {\n" +
                "    app.Alert(\"Selected: \" + data[index].name);\n" +
                "});\n\n" +
                "// Customize list items\n" +
                "list.SetItemText(0, \"Name: \" + data[0].name + \"\\nPhone: \" + data[0].phone);\n" +
                "list.SetItemText(1, \"Name: \" + data[1].name + \"\\nPhone: \" + data[1].phone);\n" +
                "list.SetItemText(2, \"Name: \" + data[2].name + \"\\nPhone: \" + data[2].phone);\n" +
                "list.SetItemText(3, \"Name: \" + data[3].name + \"\\nPhone: \" + data[3].phone);\n" +
                "list.SetItemText(4, \"Name: \" + data[4].name + \"\\nPhone: \" + data[4].phone);\n" +
                "list.SetItemText(5, \"Name: \" + data[5].name + \"\\nPhone: \" + data[5].phone);\n\n" +
                "lay.AddChild(list);\n\n" +
                "var info = app.CreateText(\"Tap a contact to select\");\n" +
                "info.SetMargins(0, 16, 0, 0);\n" +
                "lay.AddChild(info);\n\n" +
                "app.AddLayout(lay);\n");
        return t;
    }

    // ===================================================================
    //  NEW FRAMEWORK TEMPLATES
    // ===================================================================

    // ---------------------------------------------------------- REACT (VITE)
    private static WebTemplate reactVite() {
        WebTemplate t = new WebTemplate("react-vite", "React (Vite)", "React 18 + Vite SPA with JSX.", "react");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"react-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"type\": \"module\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\",\n" +
                "    \"preview\": \"vite preview\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"react\": \"^18.2.0\",\n" +
                "    \"react-dom\": \"^18.2.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"@vitejs/plugin-react\": \"^4.2.0\",\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n" +
                "import react from '@vitejs/plugin-react';\n\n" +
                "export default defineConfig({\n" +
                "  plugins: [react()]\n" +
                "});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>React App</title>\n" +
                "</head>\n<body>\n" +
                "  <div id=\"root\"></div>\n" +
                "  <script type=\"module\" src=\"/src/main.jsx\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/main.jsx",
                "import React from 'react';\n" +
                "import ReactDOM from 'react-dom/client';\n" +
                "import App from './App';\n" +
                "import './index.css';\n\n" +
                "ReactDOM.createRoot(document.getElementById('root')).render(\n" +
                "  <React.StrictMode>\n" +
                "    <App />\n" +
                "  </React.StrictMode>\n" +
                ");\n");
        t.addFile("src/App.jsx",
                "import './App.css';\n\n" +
                "function App() {\n" +
                "  return (\n" +
                "    <div className=\"app\">\n" +
                "      <h1>Hello React!</h1>\n" +
                "      <p>Edit src/App.jsx and save to reload.</p>\n" +
                "    </div>\n" +
                "  );\n" +
                "}\n\n" +
                "export default App;\n");
        t.addFile("src/App.css",
                ".app {\n" +
                "  text-align: center;\n" +
                "  padding: 40px;\n" +
                "  font-family: system-ui, sans-serif;\n" +
                "}\n");
        t.addFile("src/index.css",
                "* {\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  box-sizing: border-box;\n" +
                "}\n" +
                "body {\n" +
                "  background: #f5f5f5;\n" +
                "  color: #222;\n" +
                "}\n");
        return t;
    }

    // ---------------------------------------------------------- VUE 3 (VITE)
    private static WebTemplate vueVite() {
        WebTemplate t = new WebTemplate("vue-vite", "Vue 3 (Vite)", "Vue 3 + Vite SPA with Composition API.", "vue");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"vue-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"type\": \"module\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\",\n" +
                "    \"preview\": \"vite preview\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"vue\": \"^3.4.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"@vitejs/plugin-vue\": \"^5.0.0\",\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n" +
                "import vue from '@vitejs/plugin-vue';\n\n" +
                "export default defineConfig({\n" +
                "  plugins: [vue()]\n" +
                "});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Vue App</title>\n" +
                "</head>\n<body>\n" +
                "  <div id=\"app\"></div>\n" +
                "  <script type=\"module\" src=\"/src/main.js\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/main.js",
                "import { createApp } from 'vue';\n" +
                "import App from './App.vue';\n" +
                "import './style.css';\n\n" +
                "createApp(App).mount('#app');\n");
        t.addFile("src/App.vue",
                "<template>\n" +
                "  <div class=\"app\">\n" +
                "    <h1>Hello Vue!</h1>\n" +
                "    <p>Edit src/App.vue and save to reload.</p>\n" +
                "  </div>\n" +
                "</template>\n\n" +
                "<script setup>\n" +
                "// Composition API ready\n" +
                "</script>\n\n" +
                "<style scoped>\n" +
                ".app {\n" +
                "  text-align: center;\n" +
                "  padding: 40px;\n" +
                "  font-family: system-ui, sans-serif;\n" +
                "}\n" +
                "</style>\n");
        t.addFile("src/style.css",
                "* {\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  box-sizing: border-box;\n" +
                "}\n" +
                "body {\n" +
                "  background: #f5f5f5;\n" +
                "  color: #222;\n" +
                "}\n");
        return t;
    }

    // ---------------------------------------------------------- SVELTE (VITE)
    private static WebTemplate svelteVite() {
        WebTemplate t = new WebTemplate("svelte-vite", "Svelte (Vite)", "Svelte 4 + Vite SPA with reactive components.", "svelte");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"svelte-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"type\": \"module\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\",\n" +
                "    \"preview\": \"vite preview\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"@sveltejs/vite-plugin-svelte\": \"^3.0.0\",\n" +
                "    \"svelte\": \"^4.2.0\",\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n" +
                "import { svelte } from '@sveltejs/vite-plugin-svelte';\n\n" +
                "export default defineConfig({\n" +
                "  plugins: [svelte()]\n" +
                "});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Svelte App</title>\n" +
                "</head>\n<body>\n" +
                "  <div id=\"app\"></div>\n" +
                "  <script type=\"module\" src=\"/src/main.js\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/main.js",
                "import App from './App.svelte';\n" +
                "import './app.css';\n\n" +
                "const app = new App({ target: document.getElementById('app') });\n\n" +
                "export default app;\n");
        t.addFile("src/App.svelte",
                "<script>\n" +
                "  let name = 'Svelte';\n" +
                "</script>\n\n" +
                "<div class=\"app\">\n" +
                "  <h1>Hello {name}!</h1>\n" +
                "  <p>Edit src/App.svelte and save to reload.</p>\n" +
                "</div>\n\n" +
                "<style>\n" +
                "  .app {\n" +
                "    text-align: center;\n" +
                "    padding: 40px;\n" +
                "    font-family: system-ui, sans-serif;\n" +
                "  }\n" +
                "</style>\n");
        t.addFile("src/app.css",
                "* {\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  box-sizing: border-box;\n" +
                "}\n" +
                "body {\n" +
                "  background: #f5f5f5;\n" +
                "  color: #222;\n" +
                "}\n");
        return t;
    }

    // ---------------------------------------------------------- NEXT.JS
    private static WebTemplate nextJsScaffold() {
        WebTemplate t = new WebTemplate("nextjs", "Next.js", "React SSR framework with pages router.", "next");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"next-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"next dev\",\n" +
                "    \"build\": \"next build\",\n" +
                "    \"start\": \"next start\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"next\": \"^14.0.0\",\n" +
                "    \"react\": \"^18.2.0\",\n" +
                "    \"react-dom\": \"^18.2.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("next.config.js",
                "/** @type {import('next').NextConfig} */\n" +
                "const nextConfig = {};\n\n" +
                "module.exports = nextConfig;\n");
        t.addFile("pages/index.js",
                "import Head from 'next/head';\n\n" +
                "export default function Home() {\n" +
                "  return (\n" +
                "    <div>\n" +
                "      <Head>\n" +
                "        <title>Next.js App</title>\n" +
                "      </Head>\n" +
                "      <main>\n" +
                "        <h1>Hello Next.js!</h1>\n" +
                "        <p>Edit pages/index.js to get started.</p>\n" +
                "      </main>\n" +
                "    </div>\n" +
                "  );\n" +
                "}\n");
        t.addFile("pages/_app.js",
                "import '../styles/globals.css';\n\n" +
                "function MyApp({ Component, pageProps }) {\n" +
                "  return <Component {...pageProps} />;\n" +
                "}\n\n" +
                "export default MyApp;\n");
        t.addFile("styles/globals.css",
                "* {\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  box-sizing: border-box;\n" +
                "}\n" +
                "body {\n" +
                "  font-family: system-ui, sans-serif;\n" +
                "  background: #fff;\n" +
                "  color: #222;\n" +
                "  padding: 40px;\n" +
                "}\n");
        return t;
    }

    // ---------------------------------------------------------- NUXT 3
    private static WebTemplate nuxtScaffold() {
        WebTemplate t = new WebTemplate("nuxt", "Nuxt 3", "Vue 3 meta-framework with file-based routing.", "nuxt");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"nuxt-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"nuxt dev\",\n" +
                "    \"build\": \"nuxt build\",\n" +
                "    \"preview\": \"nuxt preview\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"nuxt\": \"^3.8.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("nuxt.config.ts",
                "export default defineNuxtConfig({});\n");
        t.addFile("app.vue",
                "<template>\n" +
                "  <div>\n" +
                "    <h1>Hello Nuxt!</h1>\n" +
                "    <p>Edit app.vue to get started.</p>\n" +
                "  </div>\n" +
                "</template>\n");
        return t;
    }

    // ---------------------------------------------------------- ALPINE.JS
    private static WebTemplate alpineJsScaffold() {
        WebTemplate t = new WebTemplate("alpine", "Alpine.js", "Lightweight reactive JS framework (CDN or npm).", "alpine");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"alpine-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"npx live-server --port=3000\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"alpinejs\": \"^3.13.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Alpine.js App</title>\n" +
                "  <script defer src=\"./node_modules/alpinejs/dist/cdn.min.js\"></script>\n" +
                "</head>\n<body>\n" +
                "  <div x-data=\"{ count: 0, name: 'Alpine' }\" class=\"app\">\n" +
                "    <h1 x-text=\"'Hello ' + name + '!' \"></h1>\n" +
                "    <p>Count: <span x-text=\"count\"></span></p>\n" +
                "    <button @click=\"count++\">Increment</button>\n" +
                "    <button @click=\"count--\">Decrement</button>\n" +
                "  </div>\n" +
                "</body>\n</html>\n");
        t.addFile("src/app.js",
                "// Alpine.js app entry\n" +
                "import Alpine from 'alpinejs';\n\n" +
                "window.Alpine = Alpine;\n" +
                "Alpine.start();\n" +
                "console.log('Alpine.js ready');\n");
        return t;
    }

    // ---------------------------------------------------------- ASTRO
    private static WebTemplate astroScaffold() {
        WebTemplate t = new WebTemplate("astro", "Astro", "Content-focused static site generator.", "astro");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"astro-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"astro dev\",\n" +
                "    \"build\": \"astro build\",\n" +
                "    \"preview\": \"astro preview\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"astro\": \"^4.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("astro.config.mjs",
                "import { defineConfig } from 'astro/config';\n\n" +
                "export default defineConfig({});\n");
        t.addFile("src/pages/index.astro",
                "---\n" +
                "// Astro page (server-rendered)\n" +
                "const title = 'Astro App';\n" +
                "---\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>{title}</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <h1>Hello Astro!</h1>\n" +
                "  <p>Edit src/pages/index.astro to get started.</p>\n" +
                "</body>\n" +
                "</html>\n");
        return t;
    }

    // ---------------------------------------------------------- SOLID.JS (VITE)
    private static WebTemplate solidJsVite() {
        WebTemplate t = new WebTemplate("solid-vite", "Solid.js (Vite)", "Reactive UI library with Vite.", "solid");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"solid-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"type\": \"module\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\",\n" +
                "    \"preview\": \"vite preview\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"solid-js\": \"^1.8.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"vite\": \"^5.0.0\",\n" +
                "    \"vite-plugin-solid\": \"^2.8.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n" +
                "import solid from 'vite-plugin-solid';\n\n" +
                "export default defineConfig({\n" +
                "  plugins: [solid()]\n" +
                "});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Solid.js App</title>\n" +
                "</head>\n<body>\n" +
                "  <div id=\"root\"></div>\n" +
                "  <script type=\"module\" src=\"/src/main.jsx\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/main.jsx",
                "import { render } from 'solid-js/web';\n" +
                "import App from './App';\n" +
                "import './index.css';\n\n" +
                "render(() => <App />, document.getElementById('root'));\n");
        t.addFile("src/App.jsx",
                "import './App.css';\n\n" +
                "function App() {\n" +
                "  return (\n" +
                "    <div class=\"app\">\n" +
                "      <h1>Hello Solid!</h1>\n" +
                "      <p>Edit src/App.jsx and save to reload.</p>\n" +
                "    </div>\n" +
                "  );\n" +
                "}\n\n" +
                "export default App;\n");
        t.addFile("src/App.css",
                ".app {\n" +
                "  text-align: center;\n" +
                "  padding: 40px;\n" +
                "  font-family: system-ui, sans-serif;\n" +
                "}\n");
        t.addFile("src/index.css",
                "* {\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  box-sizing: border-box;\n" +
                "}\n" +
                "body {\n" +
                "  background: #f5f5f5;\n" +
                "  color: #222;\n" +
                "}\n");
        return t;
    }

    // ---------------------------------------------------------- LIT (VITE)
    private static WebTemplate litVite() {
        WebTemplate t = new WebTemplate("lit-vite", "Lit (Vite)", "Web Components library with Vite.", "lit");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"lit-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"type\": \"module\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\",\n" +
                "    \"preview\": \"vite preview\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"lit\": \"^3.1.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n\n" +
                "export default defineConfig({});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Lit App</title>\n" +
                "</head>\n<body>\n" +
                "  <my-app></my-app>\n" +
                "  <script type=\"module\" src=\"/src/my-app.js\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/my-app.js",
                "import { LitElement, html, css } from 'lit';\n\n" +
                "class MyApp extends LitElement {\n" +
                "  static styles = css`\n" +
                "    :host {\n" +
                "      display: block;\n" +
                "      text-align: center;\n" +
                "      padding: 40px;\n" +
                "      font-family: system-ui, sans-serif;\n" +
                "    }\n" +
                "  `;\n\n" +
                "  render() {\n" +
                "    return html`\n" +
                "      <h1>Hello Lit!</h1>\n" +
                "      <p>Edit src/my-app.js and save to reload.</p>\n" +
                "    `;\n" +
                "  }\n" +
                "}\n\n" +
                "customElements.define('my-app', MyApp);\n");
        return t;
    }

    // ---------------------------------------------------------- VITE + TYPESCRIPT
    private static WebTemplate viteTypescript() {
        WebTemplate t = new WebTemplate("vite-ts", "Vite + TypeScript", "Vanilla TypeScript with Vite.", "ts");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"ts-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"type\": \"module\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\",\n" +
                "    \"preview\": \"vite preview\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"typescript\": \"^5.3.0\",\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n\n" +
                "export default defineConfig({});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>TypeScript App</title>\n" +
                "</head>\n<body>\n" +
                "  <div id=\"app\"></div>\n" +
                "  <script type=\"module\" src=\"/src/main.ts\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/main.ts",
                "import './style.css';\n\n" +
                "const app = document.getElementById('app')!;\n" +
                "app.innerHTML = '<h1>Hello TypeScript!</h1><p>Edit src/main.ts to reload.</p>';\n");
        t.addFile("src/style.css",
                "* {\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  box-sizing: border-box;\n" +
                "}\n" +
                "body {\n" +
                "  font-family: system-ui, sans-serif;\n" +
                "  background: #f5f5f5;\n" +
                "  color: #222;\n" +
                "  padding: 40px;\n" +
                "}\n");
        t.addFile("tsconfig.json",
                "{\n" +
                "  \"compilerOptions\": {\n" +
                "    \"target\": \"ES2020\",\n" +
                "    \"module\": \"ESNext\",\n" +
                "    \"strict\": true,\n" +
                "    \"jsx\": \"preserve\"\n" +
                "  }\n" +
                "}\n");
        return t;
    }

    // ---------------------------------------------------------- QUASAR (VUE + ANDROID)
    private static WebTemplate quasarScaffold() {
        WebTemplate t = new WebTemplate("quasar", "Quasar (Vue)", "Vue 3 + Quasar framework with mobile/Cordova support.", "quasar");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"quasar-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"quasar dev\",\n" +
                "    \"build\": \"quasar build\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"vue\": \"^3.4.0\",\n" +
                "    \"quasar\": \"^2.14.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"@quasar/app-vite\": \"^1.6.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("quasar.config.js",
                "module.exports = function(ctx) {\n" +
                "  return {\n" +
                "    boot: [],\n" +
                "    css: ['app.scss'],\n" +
                "    extras: ['roboto-font', 'material-icons'],\n" +
                "    build: { vueRouterMode: 'history' },\n" +
                "    devServer: { port: 9000 }\n" +
                "  };\n" +
                "};\n");
        t.addFile("src/App.vue",
                "<template>\n" +
                "  <div id=\"q-app\">\n" +
                "    <q-layout view=\"hHh Lpr fFf\">\n" +
                "      <q-header elevated>\n" +
                "        <q-toolbar>\n" +
                "          <q-toolbar-title>Quasar App</q-toolbar-title>\n" +
                "        </q-toolbar>\n" +
                "      </q-header>\n" +
                "      <q-page-container>\n" +
                "        <q-page class=\"flex flex-center\">\n" +
                "          <h3>Hello Quasar!</h3>\n" +
                "        </q-page>\n" +
                "      </q-page-container>\n" +
                "    </q-layout>\n" +
                "  </div>\n" +
                "</template>\n\n" +
                "<script>\n" +
                "export default { name: 'App' };\n" +
                "</script>\n");
        t.addFile("src/main.js",
                "import { createApp } from 'vue';\n" +
                "import { Quasar } from 'quasar';\n" +
                "import quasarLang from 'quasar/lang/en-US';\n" +
                "import quasarIconSet from 'quasar/icon-set/material-icons';\n" +
                "import App from './App.vue';\n\n" +
                "const app = createApp(App);\n" +
                "app.use(Quasar, { lang: quasarLang, iconSet: quasarIconSet });\n" +
                "app.mount('#q-app');\n");
        t.addFile("src/css/app.scss",
                "body {\n" +
                "  margin: 0;\n" +
                "}\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Quasar App</title>\n" +
                "</head>\n<body>\n" +
                "  <div id=\"q-app\"></div>\n" +
                "</body>\n</html>\n");
        return t;
    }

    // ---------------------------------------------------------- FRAMEWORK7
    private static WebTemplate framework7Scaffold() {
        WebTemplate t = new WebTemplate("framework7", "Framework7", "Mobile-first UI framework with iOS/Material themes.", "f7");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"f7-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"framework7\": \"^8.3.0\",\n" +
                "    \"framework7-react\": \"^8.3.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n\n" +
                "export default defineConfig({});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, viewport-fit=cover\">\n" +
                "  <title>Framework7 App</title>\n" +
                "  <link rel=\"stylesheet\" href=\"node_modules/framework7/css/framework7-bundle.min.css\">\n" +
                "</head>\n<body>\n" +
                "  <div id=\"app\"></div>\n" +
                "  <script type=\"module\" src=\"/src/app.js\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/app.js",
                "import Framework7 from 'framework7/lite-bundle';\n" +
                "import Framework7React from 'framework7-react';\n\n" +
                "Framework7.use(Framework7React);\n\n" +
                "const app = new Framework7({\n" +
                "  root: '#app',\n" +
                "  name: 'MyApp',\n" +
                "  theme: 'auto',\n" +
                "  routes: []\n" +
                "});\n\n" +
                "console.log('Framework7 ready');\n");
        return t;
    }

    // ---------------------------------------------------------- ONSEN UI
    private static WebTemplate onsenUiScaffold() {
        WebTemplate t = new WebTemplate("onsenui", "Onsen UI", "Mobile UI components with Material/iOS themes.", "onsen");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"onsen-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"dev\": \"vite\",\n" +
                "    \"build\": \"vite build\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"onsenui\": \"^2.12.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n\n" +
                "export default defineConfig({});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Onsen UI App</title>\n" +
                "  <link rel=\"stylesheet\" href=\"node_modules/onsenui/css/onsenui.min.css\">\n" +
                "  <link rel=\"stylesheet\" href=\"node_modules/onsenui/css/onsen-css-components.min.css\">\n" +
                "</head>\n<body>\n" +
                "  <ons-page>\n" +
                "    <ons-toolbar>\n" +
                "      <div class=\"center\">Onsen UI App</div>\n" +
                "    </ons-toolbar>\n" +
                "    <div style=\"padding: 20px; text-align: center;\">\n" +
                "      <h1>Hello Onsen UI!</h1>\n" +
                "      <ons-button>Click Me</ons-button>\n" +
                "    </div>\n" +
                "  </ons-page>\n" +
                "</body>\n</html>\n");
        return t;
    }

    // ---------------------------------------------------------- CORDOVA
    private static WebTemplate cordovaScaffold() {
        WebTemplate t = new WebTemplate("cordova", "Cordova", "Hybrid mobile app with Apache Cordova.", "cordova");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"cordova-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"build\": \"cordova build android\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"cordova\": \"^12.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("config.xml",
                "<?xml version='1.0' encoding='utf-8'?>\n" +
                "<widget id='com.example.app' version='1.0.0' xmlns='http://www.w3.org/ns/widgets'>\n" +
                "  <name>CordovaApp</name>\n" +
                "  <description>Cordova hybrid app</description>\n" +
                "  <content src='index.html' />\n" +
                "  <access origin='*' />\n" +
                "</widget>\n");
        t.addFile("www/index.html",
                "<!DOCTYPE html>\n<html>\n<head>\n" +
                "  <meta charset='UTF-8'>\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "  <title>Cordova App</title>\n" +
                "  <link rel='stylesheet' href='css/style.css'>\n" +
                "</head>\n<body>\n" +
                "  <h1>Hello Cordova!</h1>\n" +
                "  <p>Edit www/index.html to get started.</p>\n" +
                "  <script src='cordova.js'></script>\n" +
                "  <script src='js/index.js'></script>\n" +
                "</body>\n</html>\n");
        t.addFile("www/css/style.css",
                "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "body { font-family: system-ui, sans-serif; padding: 20px; }\n");
        t.addFile("www/js/index.js",
                "document.addEventListener('deviceready', function() {\n" +
                "  console.log('Cordova ready');\n" +
                "}, false);\n");
        return t;
    }

    // ---------------------------------------------------------- CAPACITOR
    private static WebTemplate capacitorScaffold() {
        WebTemplate t = new WebTemplate("capacitor", "Capacitor", "Modern hybrid mobile runtime (successor to Cordova).", "capacitor");
        t.addFile("package.json",
                "{\n" +
                "  \"name\": \"capacitor-app\",\n" +
                "  \"private\": true,\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"scripts\": {\n" +
                "    \"build\": \"vite build && npx cap sync\",\n" +
                "    \"dev\": \"vite\"\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"@capacitor/core\": \"^5.6.0\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"@capacitor/cli\": \"^5.6.0\",\n" +
                "    \"vite\": \"^5.0.0\"\n" +
                "  }\n" +
                "}\n");
        t.addFile("vite.config.js",
                "import { defineConfig } from 'vite';\n\n" +
                "export default defineConfig({});\n");
        t.addFile("index.html",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Capacitor App</title>\n" +
                "</head>\n<body>\n" +
                "  <div id=\"app\">\n" +
                "    <h1>Hello Capacitor!</h1>\n" +
                "    <p>Edit this HTML to build your mobile app.</p>\n" +
                "  </div>\n" +
                "  <script type=\"module\" src=\"/src/main.js\"></script>\n" +
                "</body>\n</html>\n");
        t.addFile("src/main.js",
                "import { Capacitor } from '@capacitor/core';\n\n" +
                "console.log('Capacitor platform:', Capacitor.getPlatform());\n");
        t.addFile("capacitor.config.json",
                "{\n" +
                "  \"appId\": \"com.example.app\",\n" +
                "  \"appName\": \"CapacitorApp\",\n" +
                "  \"webDir\": \"dist\",\n" +
                "  \"server\": {\n" +
                "    \"androidScheme\": \"https\"\n" +
                "  }\n" +
                "}\n");
        return t;
    }
}
