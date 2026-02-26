package main.java.be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Manages the WebView that renders the Vis.js Timeline.
 * Individuals with date properties are shown on the timeline.
 * Double-clicking an item opens its neighborhood graph.
 */
public class TimelineView {

    private final BorderPane root;
    private final WebView webView;
    private final WebEngine webEngine;
    private final JavaBridge bridge;

    /** Strong reference to prevent GC of the bridge */
    @SuppressWarnings("unused")
    private JavaBridge bridgeRef;

    public TimelineView(JavaBridge bridge) {
        this.bridge = bridge;
        this.bridgeRef = bridge;

        root = new BorderPane();
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Set up the JS bridge once the page is loaded
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                // Inject data and initialize
                webEngine.executeScript("initTimeline()");
            }
        });

        webEngine.loadContent(buildHtml());
        root.setCenter(webView);
    }

    public BorderPane getRoot() {
        return root;
    }

    private String buildHtml() {
        return """
<!DOCTYPE html>
<html lang="en" class="dark uk-theme-zinc uk-radii-md uk-shadows-sm uk-font-sm">
<head>
    <meta charset="UTF-8">
    <link rel="preconnect" href="https://rsms.me/" />
    <link rel="stylesheet" href="https://rsms.me/inter/inter.css" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/franken-ui@latest/dist/css/core.min.css" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/franken-ui@2.1.2/dist/css/utilities.min.css"/>
    <style>
        :root { font-family: Inter, sans-serif; font-feature-settings: 'liga' 1, 'calt' 1; }
        @supports (font-variation-settings: normal) { :root { font-family: InterVariable, sans-serif; } }
    </style>
    <style>
        * { user-select: none; -webkit-user-select: none; }
        input, textarea, #tooltip { user-select: text; -webkit-user-select: text; }
        #minimap { border-top: 1px solid hsl(var(--border) / 0.15); }
        #minimap-viewport {
            background: hsl(var(--primary) / 0.10);
            border-left: 2px solid hsl(var(--primary) / 0.5);
            border-right: 2px solid hsl(var(--primary) / 0.5);
        }
        /* Sidebar */
        #sidebar { border-left: 1px solid hsl(var(--border) / 0.15); transition: width 0.25s ease; }
        #sidebar.expanded { width: 280px; }
        #sidebar.collapsed { width: 36px; }
        #sidebar-header { border-bottom: 1px solid hsl(var(--border) / 0.15); }
        #sidebar-header .toggle-icon { transition: transform 0.25s; }
        #sidebar.collapsed #sidebar-header .toggle-icon {
            transform: rotate(180deg);
        }
        #sidebar.collapsed #sidebar-header .toggle-label,
        #sidebar.collapsed #sidebar-filter,
        #sidebar.collapsed #undated-list { display: none; }
        #sidebar-filter input:focus { border-color: hsl(var(--ring)); }
        .undated-item { border-left: 3px solid hsl(var(--ring)); transition: background 0.15s; }
        .undated-item:hover { background: hsl(var(--accent)); }
        #tooltip { max-width: 400px; z-index: 1000; box-shadow: 0 4px 16px rgba(0,0,0,0.4); border: 1px solid hsl(var(--border) / 0.15); }

        /* Vis.js Timeline dark theme overrides */
        .vis-timeline {
            border: none !important;
            background: hsl(var(--background)) !important;
        }
        .vis-panel.vis-bottom, .vis-panel.vis-top,
        .vis-panel.vis-left, .vis-panel.vis-right {
            border-color: rgba(255, 255, 255, 0.08) !important;
        }
        .vis-time-axis .vis-text {
            color: hsl(var(--muted-foreground)) !important;
            font-size: 11px !important;
        }
        .vis-time-axis .vis-grid.vis-minor {
            border-color: rgba(255, 255, 255, 0.08) !important;
        }
        .vis-time-axis .vis-grid.vis-major {
            border-color: rgba(255, 255, 255, 0.18) !important;
        }
        .vis-labelset .vis-label {
            color: hsl(var(--foreground)) !important;
            background: hsl(var(--card)) !important;
            border-bottom: 2px solid hsl(var(--border) / 0.6) !important;
        }
        .vis-foreground .vis-group {
            border-bottom: 2px solid hsl(var(--border) / 0.6) !important;
        }
        .vis-foreground .vis-group:nth-child(even) {
            background: rgba(255, 255, 255, 0.06) !important;
        }
        .vis-foreground .vis-group:nth-child(odd) {
            background: rgba(255, 255, 255, 0.02) !important;
        }
        .vis-labelset .vis-label:nth-child(even) {
            background: rgba(255, 255, 255, 0.08) !important;
        }
        .vis-item {
            border-radius: 6px !important;
            font-size: 11px !important;
        }
        .vis-item.vis-selected {
            border-color: hsl(var(--ring)) !important;
            box-shadow: 0 0 0 2px hsl(var(--ring) / 0.25) !important;
        }
        .vis-current-time {
            background-color: hsl(var(--primary)) !important;
        }
        .vis-custom-time {
            background-color: hsl(142 71% 45%) !important;
        }
        .uk-btn { padding-left: 16px; padding-right: 16px; }

        ::-webkit-scrollbar { width: 8px; }
        ::-webkit-scrollbar-track { background: hsl(var(--background)); }
        ::-webkit-scrollbar-thumb { background: hsl(var(--muted)); border-radius: 9999px; }
        ::-webkit-scrollbar-thumb:hover { background: hsl(var(--accent)); }

        /* Filter bar */
        #filter-bar { border-bottom: 1px solid hsl(var(--border) / 0.15); }
        #name-filter { width: 180px; }
        #name-filter:focus { border-color: hsl(var(--ring)); }
        #name-filter::placeholder { color: hsl(var(--muted-foreground)); }
        .category-chip { border: 1px solid transparent; transition: all 0.15s; }
        .category-chip.active { color: #fff; }
        .category-chip.inactive { color: hsl(var(--muted-foreground)); background: hsl(var(--muted)) !important; border-color: hsl(var(--border) / 0.15); opacity: 0.5; }
        .category-chip.inactive .chip-dot { opacity: 0.3; }
        /* Context menu */
        #context-menu { min-width: 210px; z-index: 500; box-shadow: 0 8px 24px rgba(0,0,0,0.5); border: 1px solid hsl(var(--border) / 0.15); }
        .ctx-item { transition: background 0.1s; }
        .ctx-item:hover { background: hsl(var(--accent)); }
        .ctx-separator { height: 1px; background: hsl(var(--border) / 0.15); margin: 4px 0; }
        /* Filter tags */
        .filter-tag.include { background: hsl(142 71% 45% / 0.15); color: hsl(142 71% 45%); border: 1px solid hsl(142 71% 45% / 0.25); }
        .filter-tag.exclude { background: hsl(var(--destructive) / 0.15); color: hsl(var(--destructive)); border: 1px solid hsl(var(--destructive) / 0.25); }
        .filter-tag .tag-remove:hover { opacity: 1; }
    </style>
    <script src="https://unpkg.com/vis-timeline/standalone/umd/vis-timeline-graph2d.min.js"></script>
    <link href="https://unpkg.com/vis-timeline/styles/vis-timeline-graph2d.min.css" rel="stylesheet" type="text/css" />
</head>
<body class="bg-background text-foreground flex flex-col h-screen overflow-hidden">
    <div class="flex flex-row gap-4 p-4 border-b" id="header">
        <h1 class="grow uk-h4">&#x1F50D; Ontology Timeline Viewer</h1>
        <span class="shrink-0 text-xs text-muted-foreground ml-auto" id="stats"></span>
    </div>
    <div id="filter-bar" class="bg-card flex items-center gap-2.5 px-4 py-2 shrink-0 flex-wrap">
        <span class="shrink-0 uppercase text-muted-foreground">Filter:</span>
        <input type="text" class="uk-input bg-background border border-border rounded-md text-foreground text-xs outline-none shrink-0" id="name-filter" placeholder="Search by name..." oninput="applyFilters()">
        <div class="flex flex-row" id="category-chips"></div>
        <span class="text-xs text-muted-foreground shrink-0" id="filter-match-count"></span>
        <div class="flex items-center gap-1 flex-wrap" id="filter-tags"></div>
        <div class="ml-auto flex gap-1.5 shrink-0" id="filter-actions">
            <button class="uk-btn uk-btn-default uk-btn-xs" onclick="selectAllCategories()">All</button>
            <button class="uk-btn uk-btn-default uk-btn-xs" onclick="selectNoCategories()">None</button>
            <button class="uk-btn uk-btn-ghost uk-btn-xs" onclick="clearFilters()">Clear</button>
        </div>
    </div>
    <div class="hidden fixed bg-popover rounded-md py-1 text-xs text-popover-foreground" id="context-menu"></div>
    <div class="flex flex-1 overflow-hidden" id="main-content">
        <div class="flex-1 flex flex-col overflow-hidden" id="timeline-area">
            <div class="flex-1 overflow-hidden" id="timeline-container"></div>
            <div class="h-8 bg-card relative shrink-0 cursor-pointer" id="minimap"><canvas class="w-full h-full block" id="minimap-canvas"></canvas><div class="absolute top-0 h-full pointer-events-none" id="minimap-viewport"></div></div>
        </div>
        <div id="sidebar" class="expanded bg-card flex flex-col overflow-hidden">
            <div class="px-3 py-2.5 bg-secondary text-xs font-semibold text-secondary-foreground shrink-0 flex items-center gap-2 cursor-pointer select-none whitespace-nowrap overflow-hidden" id="sidebar-header" onclick="toggleSidebar()">
                <span class="toggle-icon text-base shrink-0">&#x25B6;</span>
                <span class="toggle-label overflow-hidden">Undated Individuals</span>
            </div>
            <div class="px-3 py-2 shrink-0" id="sidebar-filter">
                <input type="text" class="uk-input w-full bg-background border border-border rounded-md text-foreground text-xs outline-none" id="filter-input" placeholder="Filter by name..." oninput="filterUndated()">
            </div>
            <div class="flex-1 overflow-y-auto px-3 py-2" id="undated-list"></div>
        </div>
    </div>
    <div class="hidden absolute bg-popover rounded-md p-3 text-xs pointer-events-none text-popover-foreground" id="tooltip"></div>

    <script>
        var timeline = null;
        var allUndated = [];
        var allItems = [];
        var itemsDataSet = null;
        var globalMin = null;
        var globalMax = null;
        var typeColorMap = {};
        var activeCategories = new Set();
        var groupSet = null;
        var allGroups = [];
        var nameIncludes = [];  // [{text:'...'}] — item must match at least one
        var nameExcludes = [];  // [{text:'...'}] — item must not match any

        function initTimeline() {
            try {
                var itemsJson = javaBridge.getTimelineItemsJson();
                var groupsJson = javaBridge.getTimelineGroupsJson();
                var undatedJson = javaBridge.getUndatedIndividualsJson();

                allItems = JSON.parse(itemsJson);
                var groups = JSON.parse(groupsJson);
                allUndated = JSON.parse(undatedJson);

                // Update stats
                document.getElementById('stats').textContent =
                    allItems.length + ' dated items | ' + allUndated.length + ' undated items';

                // Compute global date range from items
                var dates = allItems.map(function(it) { return new Date(it.start).getTime(); });
                if (dates.length > 0) {
                    var rawMin = Math.min.apply(null, dates);
                    var rawMax = Math.max.apply(null, dates);
                    var range = rawMax - rawMin;
                    var padding = Math.max(range * 0.10, 1000 * 60 * 60 * 24 * 2); // 10% or at least 2 days
                    globalMin = new Date(rawMin - padding);
                    globalMax = new Date(rawMax + padding);
                } else {
                    globalMin = new Date();
                    globalMax = new Date();
                }

                // Build type->color map from items
                allItems.forEach(function(it) {
                    if (it.style && it.group && !typeColorMap[it.group]) {
                        var m = it.style.match(/background-color:\s*([^;]+)/);
                        if (m) typeColorMap[it.group] = m[1].trim();
                    }
                });

                // Build category chips
                allGroups = groups;
                buildCategoryChips(groups);

                // Create timeline
                var container = document.getElementById('timeline-container');
                itemsDataSet = new vis.DataSet(allItems);
                groupSet = new vis.DataSet(groups);

                var options = {
                    height: '100%',
                    stack: true,
                    showMajorLabels: true,
                    showMinorLabels: true,
                    zoomMin: 1000 * 60 * 60,       // 1 hour
                    zoomMax: globalMax.getTime() - globalMin.getTime() + 1000 * 60 * 60 * 24,
                    min: globalMin,
                    max: globalMax,
                    orientation: { axis: 'top' },
                    tooltip: {
                        followMouse: true,
                        overflowMethod: 'cap'
                    },
                    margin: { item: 5 },
                    verticalScroll: true
                };

                timeline = new vis.Timeline(container, itemsDataSet, groupSet, options);

                // Double-click to open graph
                timeline.on('doubleClick', function(props) {
                    if (props.item) {
                        javaBridge.openGraphForIndividual(props.item);
                    }
                });

                // Right-click context menu
                timeline.on('contextmenu', function(props) {
                    props.event.preventDefault();
                    var x = props.event.clientX || props.event.pageX;
                    var y = props.event.clientY || props.event.pageY;

                    if (props.item) {
                        // Right-clicked on an event
                        var item = itemsDataSet.get(props.item);
                        if (item) showEventContextMenu(x, y, item);
                    } else if (props.group) {
                        // Right-clicked on a group label area
                        showCategoryContextMenu(x, y, props.group);
                    } else {
                        hideContextMenu();
                    }
                });

                // Hide context menu on click anywhere
                document.addEventListener('click', function() { hideContextMenu(); });

                // Update minimap on range change
                timeline.on('rangechanged', function() { updateMinimapViewport(); });
                timeline.on('rangechange', function() { updateMinimapViewport(); });

                // Populate undated sidebar
                renderUndated(allUndated);

                // Draw minimap
                drawMinimap();
                updateMinimapViewport();
                setupMinimapInteraction();

                // Redraw minimap on resize
                window.addEventListener('resize', function() {
                    drawMinimap();
                    updateMinimapViewport();
                });

                javaBridge.log('Timeline initialized with ' + allItems.length + ' items');
            } catch (e) {
                javaBridge.log('Error initializing timeline: ' + e.message);
                document.getElementById('timeline-container').innerHTML =
                    '<div style="padding:40px;text-align:center;color:#ef4444;">' +
                    '<h2>Error loading timeline</h2><p>' + e.message + '</p></div>';
            }
        }

        /* ── Minimap (colored event scrollbar) ── */

        function drawMinimap() {
            var canvas = document.getElementById('minimap-canvas');
            var rect = canvas.parentElement.getBoundingClientRect();
            canvas.width = rect.width * (window.devicePixelRatio || 1);
            canvas.height = rect.height * (window.devicePixelRatio || 1);
            var ctx = canvas.getContext('2d');
            ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
            var w = rect.width;
            var h = rect.height;

            ctx.clearRect(0, 0, w, h);

            // Background
            ctx.fillStyle = '#171717';
            ctx.fillRect(0, 0, w, h);

            if (!globalMin || !globalMax || allItems.length === 0) return;

            var tMin = globalMin.getTime();
            var tMax = globalMax.getTime();
            var tRange = tMax - tMin;
            if (tRange <= 0) return;

            // Draw tick marks for each event
            var barHeight = h - 6;
            allItems.forEach(function(item) {
                var t = new Date(item.start).getTime();
                var x = ((t - tMin) / tRange) * w;
                var color = typeColorMap[item.group] || '#4e79a7';
                ctx.fillStyle = color;
                ctx.globalAlpha = 0.7;
                ctx.fillRect(x - 1, 3, 2.5, barHeight);
            });
            ctx.globalAlpha = 1.0;
        }

        function updateMinimapViewport() {
            if (!timeline || !globalMin || !globalMax) return;
            var win = timeline.getWindow();
            var tMin = globalMin.getTime();
            var tMax = globalMax.getTime();
            var tRange = tMax - tMin;
            if (tRange <= 0) return;

            var container = document.getElementById('minimap');
            var cw = container.getBoundingClientRect().width;

            var leftPct = (win.start.getTime() - tMin) / tRange;
            var rightPct = (win.end.getTime() - tMin) / tRange;
            leftPct = Math.max(0, Math.min(1, leftPct));
            rightPct = Math.max(0, Math.min(1, rightPct));

            var vp = document.getElementById('minimap-viewport');
            vp.style.left = (leftPct * cw) + 'px';
            vp.style.width = ((rightPct - leftPct) * cw) + 'px';
        }

        function setupMinimapInteraction() {
            var minimap = document.getElementById('minimap');
            var dragging = false;

            function minimapNav(e) {
                if (!timeline || !globalMin || !globalMax) return;
                var rect = minimap.getBoundingClientRect();
                var pct = (e.clientX - rect.left) / rect.width;
                pct = Math.max(0, Math.min(1, pct));

                var tMin = globalMin.getTime();
                var tMax = globalMax.getTime();
                var tRange = tMax - tMin;
                var win = timeline.getWindow();
                var winSize = win.end.getTime() - win.start.getTime();
                var center = tMin + pct * tRange;
                timeline.setWindow(center - winSize / 2, center + winSize / 2, { animation: false });
            }

            minimap.addEventListener('mousedown', function(e) {
                dragging = true;
                minimapNav(e);
            });
            window.addEventListener('mousemove', function(e) {
                if (dragging) minimapNav(e);
            });
            window.addEventListener('mouseup', function() {
                dragging = false;
            });
        }

        /* ── Sidebar collapse/expand ── */

        function toggleSidebar() {
            var sidebar = document.getElementById('sidebar');
            if (sidebar.classList.contains('expanded')) {
                sidebar.classList.remove('expanded');
                sidebar.classList.add('collapsed');
            } else {
                sidebar.classList.remove('collapsed');
                sidebar.classList.add('expanded');
            }
            // Redraw minimap after layout settles
            setTimeout(function() {
                drawMinimap();
                updateMinimapViewport();
            }, 300);
        }

        function renderUndated(items) {
            var list = document.getElementById('undated-list');
            list.innerHTML = '';
            items.forEach(function(item) {
                var div = document.createElement('div');
                div.className = 'undated-item px-3 py-2.5 mb-2.5 bg-background rounded-md cursor-pointer text-xs';
                div.innerHTML = '<span class="inline-block px-1.5 py-0.5 bg-secondary rounded-full text-muted-foreground mb-1 font-medium" style="font-size:10px">' + escapeHtml(item.type) + '</span>' +
                                '<span class="block text-foreground break-all">' + escapeHtml(item.label) + '</span>';
                div.ondblclick = function() {
                    javaBridge.openGraphForIndividual(item.uri);
                };
                list.appendChild(div);
            });
        }

        function filterUndated() {
            var query = document.getElementById('filter-input').value.toLowerCase();
            var filtered = allUndated.filter(function(item) {
                return item.label.toLowerCase().indexOf(query) >= 0 ||
                       item.type.toLowerCase().indexOf(query) >= 0;
            });
            renderUndated(filtered);
        }

        /* ── Timeline event filtering ── */

        function buildCategoryChips(groups) {
            var container = document.getElementById('category-chips');
            container.innerHTML = '';
            // Count items per group
            var counts = {};
            allItems.forEach(function(it) {
                counts[it.group] = (counts[it.group] || 0) + 1;
            });
            groups.forEach(function(g) {
                activeCategories.add(g.id);
                var color = typeColorMap[g.id] || '#4e79a7';
                var chip = document.createElement('span');
                chip.className = 'category-chip active flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs cursor-pointer select-none whitespace-nowrap';
                chip.style.background = color + '33';
                chip.dataset.type = g.id;
                chip.dataset.color = color;
                chip.innerHTML = '<span class="chip-dot w-2 h-2 rounded-full shrink-0" style="background:' + color + '"></span>' +
                                 escapeHtml(g.content || g.id) +
                                 '<span class="chip-count text-muted-foreground ml-0.5" style="font-size:10px">(' + (counts[g.id] || 0) + ')</span>';
                chip.onclick = function() { toggleCategory(g.id); };
                container.appendChild(chip);
            });
        }

        function toggleCategory(type) {
            if (activeCategories.has(type)) {
                activeCategories.delete(type);
            } else {
                activeCategories.add(type);
            }
            refreshCategoryChips();
            applyFilters();
        }

        function selectAllCategories() {
            var chips = document.querySelectorAll('.category-chip');
            chips.forEach(function(c) { activeCategories.add(c.dataset.type); });
            refreshCategoryChips();
            applyFilters();
        }

        function selectNoCategories() {
            activeCategories.clear();
            refreshCategoryChips();
            applyFilters();
        }

        function clearFilters() {
            document.getElementById('name-filter').value = '';
            nameIncludes = [];
            nameExcludes = [];
            selectAllCategories();
            renderFilterTags();
        }

        function refreshCategoryChips() {
            var chips = document.querySelectorAll('.category-chip');
            chips.forEach(function(c) {
                var type = c.dataset.type;
                var color = c.dataset.color;
                if (activeCategories.has(type)) {
                    c.className = 'category-chip active flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs cursor-pointer select-none whitespace-nowrap';
                    c.style.background = color + '33';
                } else {
                    c.className = 'category-chip inactive flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs cursor-pointer select-none whitespace-nowrap';
                    c.style.background = '';
                }
            });
        }

        function applyFilters() {
            var nameQuery = (document.getElementById('name-filter').value || '').toLowerCase();

            var filtered = allItems.filter(function(item) {
                // Category filter
                if (!activeCategories.has(item.group)) return false;

                var text = (item.content || '').toLowerCase();
                var id = (item.id || '').toLowerCase();

                // Search box filter
                if (nameQuery.length > 0) {
                    if (text.indexOf(nameQuery) < 0 && id.indexOf(nameQuery) < 0) return false;
                }

                // Name includes: item must match at least one
                if (nameIncludes.length > 0) {
                    var matchesAny = false;
                    for (var i = 0; i < nameIncludes.length; i++) {
                        var q = nameIncludes[i].text.toLowerCase();
                        if (text.indexOf(q) >= 0 || id.indexOf(q) >= 0) { matchesAny = true; break; }
                    }
                    if (!matchesAny) return false;
                }

                // Name excludes: item must not match any
                for (var j = 0; j < nameExcludes.length; j++) {
                    var eq = nameExcludes[j].text.toLowerCase();
                    if (text.indexOf(eq) >= 0 || id.indexOf(eq) >= 0) return false;
                }

                return true;
            });

            // Update the DataSet
            itemsDataSet.clear();
            itemsDataSet.add(filtered);

            // Hide empty groups, show non-empty ones
            if (groupSet) {
                var populatedGroups = new Set();
                filtered.forEach(function(it) { populatedGroups.add(it.group); });
                allGroups.forEach(function(g) {
                    var isActive = activeCategories.has(g.id);
                    var hasItems = populatedGroups.has(g.id);
                    var visible = isActive && hasItems;
                    groupSet.update({ id: g.id, visible: visible });
                });
            }

            // Update match count
            document.getElementById('filter-match-count').textContent =
                filtered.length + ' / ' + allItems.length + ' events';

            // Redraw minimap with filtered items only
            drawMinimapFiltered(filtered);
        }

        /* ── Context menu ── */

        function showCategoryContextMenu(x, y, groupId) {
            var menu = document.getElementById('context-menu');
            var label = groupId;
            allGroups.forEach(function(g) { if (g.id === groupId) label = g.content || g.id; });
            var html = '<div class="ctx-header px-4 py-1 text-muted-foreground uppercase font-medium" style="font-size:10px;letter-spacing:0.5px">Category: ' + escapeHtml(label) + '</div>';
            html += '<div class="ctx-item px-4 py-1.5 cursor-pointer flex items-center gap-2 text-foreground" onclick="ctxShowOnly(\\'' + escJs(groupId) + '\\')"><span class="ctx-icon text-xs shrink-0 text-center" style="width:18px">\u2714</span> Show only this</div>';
            if (activeCategories.has(groupId)) {
                html += '<div class="ctx-item px-4 py-1.5 cursor-pointer flex items-center gap-2 text-foreground" onclick="ctxExcludeCategory(\\'' + escJs(groupId) + '\\')"><span class="ctx-icon text-xs shrink-0 text-center" style="width:18px">\u2716</span> Exclude this</div>';
            } else {
                html += '<div class="ctx-item px-4 py-1.5 cursor-pointer flex items-center gap-2 text-foreground" onclick="ctxIncludeCategory(\\'' + escJs(groupId) + '\\')"><span class="ctx-icon text-xs shrink-0 text-center" style="width:18px">\u2795</span> Include this</div>';
            }
            menu.innerHTML = html;
            positionMenu(menu, x, y);
        }

        function showEventContextMenu(x, y, item) {
            var menu = document.getElementById('context-menu');
            // Extract plain text from content (may contain HTML)
            var tmp = document.createElement('div');
            tmp.innerHTML = item.content || '';
            var plainName = tmp.textContent || tmp.innerText || '';
            var shortName = plainName.length > 30 ? plainName.substring(0, 27) + '...' : plainName;
            var groupLabel = item.group;
            allGroups.forEach(function(g) { if (g.id === item.group) groupLabel = g.content || g.id; });

            var html = '<div class="ctx-header px-4 py-1 text-muted-foreground uppercase font-medium" style="font-size:10px;letter-spacing:0.5px">Event: ' + escapeHtml(shortName) + '</div>';
            html += '<div class="ctx-item px-4 py-1.5 cursor-pointer flex items-center gap-2 text-foreground" onclick="ctxIncludeName(\\'' + escJs(plainName) + '\\')"><span class="ctx-icon text-xs shrink-0 text-center" style="width:18px">\u2714</span> Include \"' + escapeHtml(shortName) + '\"</div>';
            html += '<div class="ctx-item px-4 py-1.5 cursor-pointer flex items-center gap-2 text-foreground" onclick="ctxExcludeName(\\'' + escJs(plainName) + '\\')"><span class="ctx-icon text-xs shrink-0 text-center" style="width:18px">\u2716</span> Exclude \"' + escapeHtml(shortName) + '\"</div>';
            html += '<div class="ctx-separator my-1"></div>';
            html += '<div class="ctx-header px-4 py-1 text-muted-foreground uppercase font-medium" style="font-size:10px;letter-spacing:0.5px">Category: ' + escapeHtml(groupLabel) + '</div>';
            html += '<div class="ctx-item px-4 py-1.5 cursor-pointer flex items-center gap-2 text-foreground" onclick="ctxShowOnly(\\'' + escJs(item.group) + '\\')"><span class="ctx-icon text-xs shrink-0 text-center" style="width:18px">\u2714</span> Show only ' + escapeHtml(groupLabel) + '</div>';
            html += '<div class="ctx-item px-4 py-1.5 cursor-pointer flex items-center gap-2 text-foreground" onclick="ctxExcludeCategory(\\'' + escJs(item.group) + '\\')"><span class="ctx-icon text-xs shrink-0 text-center" style="width:18px">\u2716</span> Exclude ' + escapeHtml(groupLabel) + '</div>';
            menu.innerHTML = html;
            positionMenu(menu, x, y);
        }

        function positionMenu(menu, x, y) {
            menu.style.display = 'block';
            menu.style.left = x + 'px';
            menu.style.top = y + 'px';
            // Adjust if overflowing
            var rect = menu.getBoundingClientRect();
            if (rect.right > window.innerWidth) menu.style.left = (x - rect.width) + 'px';
            if (rect.bottom > window.innerHeight) menu.style.top = (y - rect.height) + 'px';
        }

        function hideContextMenu() {
            document.getElementById('context-menu').style.display = 'none';
        }

        function escJs(s) {
            var bs = String.fromCharCode(92);
            return s.split(bs).join(bs + bs).split("'").join(bs + "'");
        }

        function ctxShowOnly(groupId) {
            hideContextMenu();
            activeCategories.clear();
            activeCategories.add(groupId);
            refreshCategoryChips();
            applyFilters();
        }

        function ctxExcludeCategory(groupId) {
            hideContextMenu();
            activeCategories.delete(groupId);
            refreshCategoryChips();
            applyFilters();
        }

        function ctxIncludeCategory(groupId) {
            hideContextMenu();
            activeCategories.add(groupId);
            refreshCategoryChips();
            applyFilters();
        }

        function ctxIncludeName(name) {
            hideContextMenu();
            // Don't add duplicates
            for (var i = 0; i < nameIncludes.length; i++) {
                if (nameIncludes[i].text === name) return;
            }
            nameIncludes.push({ text: name });
            renderFilterTags();
            applyFilters();
        }

        function ctxExcludeName(name) {
            hideContextMenu();
            for (var i = 0; i < nameExcludes.length; i++) {
                if (nameExcludes[i].text === name) return;
            }
            nameExcludes.push({ text: name });
            renderFilterTags();
            applyFilters();
        }

        function removeInclude(idx) {
            nameIncludes.splice(idx, 1);
            renderFilterTags();
            applyFilters();
        }

        function removeExclude(idx) {
            nameExcludes.splice(idx, 1);
            renderFilterTags();
            applyFilters();
        }

        function renderFilterTags() {
            var container = document.getElementById('filter-tags');
            var html = '';
            nameIncludes.forEach(function(f, i) {
                var short = f.text.length > 20 ? f.text.substring(0, 17) + '...' : f.text;
                html += '<span class="filter-tag include inline-flex items-center gap-1 px-2 py-0.5 rounded-full cursor-default whitespace-nowrap font-medium" style="font-size:10px" title="Include: ' + escapeHtml(f.text) + '">';
                html += '\u2714 ' + escapeHtml(short);
                html += '<span class="tag-remove cursor-pointer text-xs ml-0.5" style="opacity:0.7" onclick="removeInclude(' + i + ')">&times;</span></span>';
            });
            nameExcludes.forEach(function(f, i) {
                var short = f.text.length > 20 ? f.text.substring(0, 17) + '...' : f.text;
                html += '<span class="filter-tag exclude inline-flex items-center gap-1 px-2 py-0.5 rounded-full cursor-default whitespace-nowrap font-medium" style="font-size:10px" title="Exclude: ' + escapeHtml(f.text) + '">';
                html += '\u2716 ' + escapeHtml(short);
                html += '<span class="tag-remove cursor-pointer text-xs ml-0.5" style="opacity:0.7" onclick="removeExclude(' + i + ')">&times;</span></span>';
            });
            container.innerHTML = html;
        }

        function drawMinimapFiltered(items) {
            var canvas = document.getElementById('minimap-canvas');
            var rect = canvas.parentElement.getBoundingClientRect();
            canvas.width = rect.width * (window.devicePixelRatio || 1);
            canvas.height = rect.height * (window.devicePixelRatio || 1);
            var ctx = canvas.getContext('2d');
            ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
            var w = rect.width;
            var h = rect.height;

            ctx.clearRect(0, 0, w, h);
            ctx.fillStyle = '#171717';
            ctx.fillRect(0, 0, w, h);

            if (!globalMin || !globalMax || items.length === 0) return;

            var tMin = globalMin.getTime();
            var tMax = globalMax.getTime();
            var tRange = tMax - tMin;
            if (tRange <= 0) return;

            var barHeight = h - 6;
            items.forEach(function(item) {
                var t = new Date(item.start).getTime();
                var x = ((t - tMin) / tRange) * w;
                var color = typeColorMap[item.group] || '#4e79a7';
                ctx.fillStyle = color;
                ctx.globalAlpha = 0.7;
                ctx.fillRect(x - 1, 3, 2.5, barHeight);
            });
            ctx.globalAlpha = 1.0;
        }

        function escapeHtml(text) {
            var div = document.createElement('div');
            div.appendChild(document.createTextNode(text));
            return div.innerHTML;
        }
    </script>
    <script type="module" src="https://cdn.jsdelivr.net/npm/franken-ui@latest/dist/js/core.iife.js"></script>
</body>
</html>
""";
    }
}
