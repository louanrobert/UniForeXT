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
<html>
<head>
    <meta charset="UTF-8">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #1a1a2e;
            color: #e0e0e0;
            display: flex;
            flex-direction: column;
            height: 100vh;
            overflow: hidden;
        }
        #header {
            background: linear-gradient(135deg, #16213e, #0f3460);
            padding: 12px 24px;
            display: flex;
            align-items: center;
            gap: 16px;
            border-bottom: 2px solid #e94560;
            flex-shrink: 0;
        }
        #header h1 {
            font-size: 18px;
            color: #e94560;
            font-weight: 600;
        }
        #header .stats {
            font-size: 13px;
            color: #8899aa;
            margin-left: auto;
        }
        #main-content {
            display: flex;
            flex: 1;
            overflow: hidden;
        }
        #timeline-area {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        #timeline-container {
            flex: 1;
            overflow: hidden;
        }
        /* Minimap scrollbar */
        #minimap {
            height: 32px;
            background: #16213e;
            border-top: 1px solid #2a2a4a;
            position: relative;
            flex-shrink: 0;
            cursor: pointer;
        }
        #minimap canvas {
            width: 100%;
            height: 100%;
            display: block;
        }
        #minimap-viewport {
            position: absolute;
            top: 0;
            height: 100%;
            background: rgba(233, 69, 96, 0.15);
            border-left: 2px solid #e94560;
            border-right: 2px solid #e94560;
            pointer-events: none;
        }
        /* Sidebar */
        #sidebar {
            background: #16213e;
            border-left: 1px solid #2a2a4a;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            transition: width 0.25s ease;
        }
        #sidebar.expanded {
            width: 280px;
        }
        #sidebar.collapsed {
            width: 36px;
        }
        #sidebar-header {
            padding: 10px 12px;
            background: #0f3460;
            font-size: 14px;
            font-weight: 600;
            color: #e94560;
            border-bottom: 1px solid #2a2a4a;
            flex-shrink: 0;
            display: flex;
            align-items: center;
            gap: 8px;
            cursor: pointer;
            user-select: none;
            white-space: nowrap;
            overflow: hidden;
        }
        #sidebar-header .toggle-icon {
            font-size: 16px;
            flex-shrink: 0;
            transition: transform 0.25s;
        }
        #sidebar.collapsed #sidebar-header .toggle-icon {
            transform: rotate(180deg);
        }
        #sidebar-header .toggle-label {
            overflow: hidden;
        }
        #sidebar.collapsed #sidebar-header .toggle-label,
        #sidebar.collapsed #sidebar-filter,
        #sidebar.collapsed #undated-list {
            display: none;
        }
        #sidebar-filter {
            padding: 8px 12px;
            flex-shrink: 0;
        }
        #sidebar-filter input {
            width: 100%;
            padding: 6px 10px;
            background: #1a1a2e;
            border: 1px solid #2a2a4a;
            border-radius: 4px;
            color: #e0e0e0;
            font-size: 12px;
            outline: none;
        }
        #sidebar-filter input:focus {
            border-color: #e94560;
        }
        #undated-list {
            flex: 1;
            overflow-y: auto;
            padding: 8px 12px;
        }
        .undated-item {
            padding: 8px 10px;
            margin-bottom: 4px;
            background: #1a1a2e;
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
            border-left: 3px solid #4e79a7;
            transition: background 0.2s;
        }
        .undated-item:hover {
            background: #2a2a4a;
        }
        .undated-item .type-badge {
            display: inline-block;
            padding: 1px 6px;
            background: #0f3460;
            border-radius: 3px;
            font-size: 10px;
            color: #8899aa;
            margin-bottom: 4px;
        }
        .undated-item .name {
            display: block;
            color: #c0c0c0;
            word-break: break-all;
        }
        #tooltip {
            display: none;
            position: absolute;
            background: #16213e;
            border: 1px solid #e94560;
            border-radius: 6px;
            padding: 12px;
            max-width: 400px;
            font-size: 12px;
            z-index: 1000;
            box-shadow: 0 4px 12px rgba(0,0,0,0.5);
            pointer-events: none;
        }

        /* Vis.js Timeline dark theme overrides */
        .vis-timeline {
            border: none !important;
            background: #1a1a2e !important;
        }
        .vis-panel.vis-bottom, .vis-panel.vis-top,
        .vis-panel.vis-left, .vis-panel.vis-right {
            border-color: #2a2a4a !important;
        }
        .vis-time-axis .vis-text {
            color: #8899aa !important;
            font-size: 11px !important;
        }
        .vis-time-axis .vis-grid.vis-minor {
            border-color: #2a2a4a !important;
        }
        .vis-time-axis .vis-grid.vis-major {
            border-color: #3a3a5a !important;
        }
        .vis-labelset .vis-label {
            color: #c0c0c0 !important;
            background: #16213e !important;
            border-bottom: 1px solid #2a2a4a !important;
        }
        .vis-foreground .vis-group {
            border-bottom: 1px solid #2a2a4a !important;
        }
        .vis-item {
            border-radius: 4px !important;
            font-size: 11px !important;
        }
        .vis-item.vis-selected {
            border-color: #e94560 !important;
            box-shadow: 0 0 8px rgba(233,69,96,0.5) !important;
        }
        .vis-current-time {
            background-color: #e94560 !important;
        }
        .vis-custom-time {
            background-color: #59a14f !important;
        }
        ::-webkit-scrollbar { width: 8px; }
        ::-webkit-scrollbar-track { background: #1a1a2e; }
        ::-webkit-scrollbar-thumb { background: #2a2a4a; border-radius: 4px; }
        ::-webkit-scrollbar-thumb:hover { background: #3a3a5a; }
    </style>
    <script src="https://unpkg.com/vis-timeline/standalone/umd/vis-timeline-graph2d.min.js"></script>
    <link href="https://unpkg.com/vis-timeline/styles/vis-timeline-graph2d.min.css" rel="stylesheet" type="text/css" />
</head>
<body>
    <div id="header">
        <h1>&#x1F50D; Ontology Timeline Viewer</h1>
        <span class="stats" id="stats"></span>
    </div>
    <div id="main-content">
        <div id="timeline-area">
            <div id="timeline-container"></div>
            <div id="minimap"><canvas id="minimap-canvas"></canvas><div id="minimap-viewport"></div></div>
        </div>
        <div id="sidebar" class="expanded">
            <div id="sidebar-header" onclick="toggleSidebar()">
                <span class="toggle-icon">&#x25B6;</span>
                <span class="toggle-label">Undated Individuals</span>
            </div>
            <div id="sidebar-filter">
                <input type="text" id="filter-input" placeholder="Filter by name..." oninput="filterUndated()">
            </div>
            <div id="undated-list"></div>
        </div>
    </div>
    <div id="tooltip"></div>

    <script>
        var timeline = null;
        var allUndated = [];
        var allItems = [];
        var globalMin = null;
        var globalMax = null;
        var typeColorMap = {};

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

                // Create timeline
                var container = document.getElementById('timeline-container');
                var dataSet = new vis.DataSet(allItems);
                var groupSet = new vis.DataSet(groups);

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

                timeline = new vis.Timeline(container, dataSet, groupSet, options);

                // Double-click to open graph
                timeline.on('doubleClick', function(props) {
                    if (props.item) {
                        javaBridge.openGraphForIndividual(props.item);
                    }
                });

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
                    '<div style="padding:40px;text-align:center;color:#e94560;">' +
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
            ctx.fillStyle = '#16213e';
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
                div.className = 'undated-item';
                div.innerHTML = '<span class="type-badge">' + escapeHtml(item.type) + '</span>' +
                                '<span class="name">' + escapeHtml(item.label) + '</span>';
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

        function escapeHtml(text) {
            var div = document.createElement('div');
            div.appendChild(document.createTextNode(text));
            return div.innerHTML;
        }
    </script>
</body>
</html>
""";
    }
}
