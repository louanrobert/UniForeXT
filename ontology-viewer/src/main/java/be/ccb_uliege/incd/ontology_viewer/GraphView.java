package main.java.be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

/**
 * Manages a new Stage with a WebView that renders a Vis.js Network graph.
 * Shows the 1-hop neighborhood of a selected individual.
 * Double-clicking a node expands its neighborhood without resetting the graph.
 * Already-explored nodes are visually distinguished with a different border color.
 */
public class GraphView {

    private final Stage stage;
    private final WebView webView;
    private final WebEngine webEngine;
    private final JavaBridge bridge;
    private final String initialUri;

    /** Strong reference to prevent GC of the bridge */
    @SuppressWarnings("unused")
    private JavaBridge bridgeRef;

    public GraphView(JavaBridge bridge, String individualUri) {
        this.bridge = bridge;
        this.bridgeRef = bridge;
        this.initialUri = individualUri;

        stage = new Stage();
        stage.setTitle("Graph: " + OntologyService.localName(individualUri));
        stage.setWidth(1000);
        stage.setHeight(700);

        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                // Initialize graph with the individual's neighborhood
                webEngine.executeScript("initGraph('" + escapeJs(individualUri) + "')");
            }
        });

        webEngine.loadContent(buildHtml());

        Scene scene = new Scene(webView);
        stage.setScene(scene);
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }

    private static String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
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
        * { box-sizing: border-box; user-select: none; -webkit-user-select: none; }
        input, textarea, #node-info { user-select: text; -webkit-user-select: text; }
        #toolbar { border-bottom: 1px solid hsl(var(--border) / 0.15); }
        #legend { box-shadow: 0 4px 16px rgba(0,0,0,0.4); border: 1px solid hsl(var(--border) / 0.15); }
        #legend-header:hover { background: hsl(var(--accent)); }
        #legend-header .toggle-arrow { transition: transform 0.2s; }
        #legend.collapsed #legend-header .toggle-arrow { transform: rotate(-90deg); }
        #legend-body { overflow: hidden; transition: max-height 0.25s ease, opacity 0.25s ease; max-height: 500px; opacity: 1; }
        #legend.collapsed #legend-body { max-height: 0; opacity: 0; padding-top: 0; padding-bottom: 0; }
        #node-info { max-height: 150px; box-shadow: 0 4px 16px rgba(0,0,0,0.4); border: 1px solid hsl(var(--border) / 0.15); }
        #node-info h4 { color: hsl(var(--foreground)); margin-bottom: 4px; font-weight: 600; }
        #node-info .uri { color: hsl(var(--muted-foreground)); font-size: 10px; word-break: break-all; }
        .uk-btn { padding-left: 16px; padding-right: 16px; }

        ::-webkit-scrollbar { width: 8px; }
        ::-webkit-scrollbar-track { background: hsl(var(--background)); }
        ::-webkit-scrollbar-thumb { background: hsl(var(--muted)); border-radius: 9999px; }
        ::-webkit-scrollbar-thumb:hover { background: hsl(var(--accent)); }

        /* Hide directional navigation arrows */
        .vis-button.vis-up,
        .vis-button.vis-down,
        .vis-button.vis-left,
        .vis-button.vis-right {
            display: none !important;
        }
        /* Style zoom controls to match theme */
        .vis-navigation { background: transparent; }
        .vis-button.vis-zoomIn,
        .vis-button.vis-zoomOut,
        .vis-button.vis-zoomExtends {
            background-color: hsl(var(--card)) !important;
            border: 1px solid hsl(var(--border) / 0.15) !important;
            border-radius: 6px !important;
            background-image: none !important;
            color: hsl(var(--foreground)) !important;
            width: 30px !important;
            height: 30px !important;
            line-height: 30px !important;
            text-align: center !important;
            cursor: pointer !important;
            margin: 2px !important;
        }
        .vis-button.vis-zoomIn:hover,
        .vis-button.vis-zoomOut:hover,
        .vis-button.vis-zoomExtends:hover {
            background-color: hsl(var(--accent)) !important;
            border-color: hsl(var(--border) / 0.3) !important;
        }
        .vis-button.vis-zoomIn::after { content: '+'; font-size: 18px; font-weight: bold; }
        .vis-button.vis-zoomOut::after { content: '-'; font-size: 18px; font-weight: bold; }
        .vis-button.vis-zoomExtends::after { content: 'fit'; font-size: 16px; }

        /* Expansion dialog */
        #expansion-dialog { box-shadow: 0 8px 24px rgba(0,0,0,0.5); border: 1px solid hsl(var(--border) / 0.15); }
        .type-row:hover { background: hsl(var(--accent)); }
        .dialog-controls input[type="number"]:focus { border-color: hsl(var(--ring)); }
    </style>
    <script src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"></script>
</head>
<body class="bg-background text-foreground flex flex-col h-screen overflow-hidden">
    <div class="bg-card px-6 py-4 flex items-center gap-4 shrink-0 border-b border-border" id="toolbar">
        <h2 class="text-base text-foreground font-semibold tracking-tight">&#x1F578; Neighborhood Graph</h2>
        <button class="uk-btn uk-btn-default uk-btn-xs" onclick="fitGraph()">Fit View</button>
        <button class="uk-btn uk-btn-default uk-btn-xs" onclick="togglePhysics()">Toggle Physics</button>
        <button class="uk-btn uk-btn-default uk-btn-xs" onclick="resetGraph()">Reset</button>
        <span class="text-xs text-muted-foreground ml-auto" id="graph-stats"></span>
    </div>
    <div class="flex-1 relative" id="graph-wrapper">
        <div class="absolute inset-0" id="graph-container"></div>
        <div class="absolute top-2.5 right-2.5 bg-popover rounded-md text-xs z-20 pointer-events-auto overflow-hidden text-popover-foreground min-w-[160px]" id="legend"></div>
        <div class="absolute bottom-2.5 left-2.5 right-2.5 bg-popover rounded-md px-4 py-3 text-xs overflow-y-auto hidden z-20 pointer-events-auto text-popover-foreground" id="node-info"></div>
        <div class="absolute inset-0 bg-black/60 z-[200] hidden justify-center items-center p-4" id="expansion-dialog-overlay">
            <div class="bg-popover rounded-lg px-6 py-5 min-w-[340px] max-w-[440px] max-h-[80vh] overflow-y-auto text-popover-foreground" id="expansion-dialog"></div>
        </div>
    </div>

    <script>
        var network = null;
        var nodesDataSet = null;
        var edgesDataSet = null;
        var exploredNodes = new Set();
        var physicsEnabled = true;
        var initialUri = '';
        var EXPAND_THRESHOLD = 20;
        var pendingExpandNodeId = null;
        var expandedLiteralId = null;  // tracks which literal box is currently expanded

        var EXPLORED_BORDER_COLOR = '#59a14f';  // Green border for explored nodes
        var EXPLORED_BORDER_WIDTH = 3;

        function initGraph(uri) {
            initialUri = uri;
            try {
                nodesDataSet = new vis.DataSet();
                edgesDataSet = new vis.DataSet();
                exploredNodes.clear();

                var container = document.getElementById('graph-container');
                var options = {
                    physics: {
                        enabled: true,
                        solver: 'forceAtlas2Based',
                        forceAtlas2Based: {
                            gravitationalConstant: -100,
                            centralGravity: 0.005,
                            springLength: 220,
                            springConstant: 0.04,
                            damping: 0.4,
                            avoidOverlap: 0.8
                        },
                        minVelocity: 0.75,
                        stabilization: { iterations: 400 }
                    },
                    interaction: {
                        hover: true,
                        tooltipDelay: 200,
                        multiselect: true,
                        navigationButtons: true,
                        keyboard: true
                    },
                    nodes: {
                        font: { color: '#e0e0e0', size: 12 },
                        borderWidth: 2,
                        margin: 10
                    },
                    edges: {
                        font: { color: '#8899aa', size: 10, strokeWidth: 0, background: '#1a1a2e' },
                        color: { color: '#4a4a6a', highlight: '#e94560', hover: '#6a6a8a' },
                        smooth: { type: 'continuous' },
                        length: 280,
                        width: 1.5
                    },
                    layout: {
                        improvedLayout: true
                    }
                };

                network = new vis.Network(container, { nodes: nodesDataSet, edges: edgesDataSet }, options);

                // Draw dot grid on the canvas (moves & scales with the graph)
                var DOT_SPACING = 40;  // world-coordinate spacing between dots
                var DOT_RADIUS = 1.2;
                var DOT_COLOR = 'rgba(160,160,160,0.45)';
                var MIN_SCREEN_SPACING = 12; // minimum pixel spacing to avoid overdraw
                network.on('beforeDrawing', function(ctx) {
                    // ctx is in world coordinates — vis.js already applied the camera transform
                    var scale = network.getScale();
                    // Skip drawing dots if they would be too dense on screen
                    if (DOT_SPACING * scale < MIN_SCREEN_SPACING) return;

                    var viewPos = network.getViewPosition();  // center of viewport in world coords
                    var rect = container.getBoundingClientRect();
                    var halfW = (rect.width / scale) / 2;
                    var halfH = (rect.height / scale) / 2;

                    var left = viewPos.x - halfW;
                    var right = viewPos.x + halfW;
                    var top = viewPos.y - halfH;
                    var bottom = viewPos.y + halfH;

                    // Snap to grid
                    var startX = Math.floor(left / DOT_SPACING) * DOT_SPACING;
                    var startY = Math.floor(top / DOT_SPACING) * DOT_SPACING;

                    ctx.fillStyle = DOT_COLOR;
                    ctx.beginPath();
                    for (var x = startX; x <= right; x += DOT_SPACING) {
                        for (var y = startY; y <= bottom; y += DOT_SPACING) {
                            ctx.moveTo(x + DOT_RADIUS, y);
                            ctx.arc(x, y, DOT_RADIUS, 0, 2 * Math.PI);
                        }
                    }
                    ctx.fill();
                });

                // Double-click to expand
                network.on('doubleClick', function(params) {
                    if (params.nodes.length > 0) {
                        var nodeId = params.nodes[0];
                        if (nodeId.indexOf('_lit_') >= 0) return;
                        expandNodeSmart(nodeId);
                    }
                });

                // Click to show info & expand/collapse literals
                network.on('click', function(params) {
                    if (params.nodes.length > 0) {
                        var nodeId = params.nodes[0];
                        collapseLiteral();  // collapse any previously expanded literal
                        expandLiteral(nodeId);
                        showNodeInfo(nodeId);
                    } else {
                        collapseLiteral();
                        document.getElementById('node-info').style.display = 'none';
                    }
                });

                // Smart-expand the initial node
                expandNodeSmart(uri);

                javaBridge.log('Graph initialized for: ' + uri);
            } catch (e) {
                javaBridge.log('Error initializing graph: ' + e.message);
                document.getElementById('graph-container').innerHTML =
                    '<div class="p-10 text-center text-destructive">' +
                    '<h2>Error</h2><p>' + e.message + '</p></div>';
            }
        }

        function expandNodeSmart(nodeId) {
            if (exploredNodes.has(nodeId)) {
                javaBridge.log('Node already explored: ' + nodeId);
                return;
            }
            try {
                var summaryJson = javaBridge.getNeighborSummaryJson(nodeId);
                var summary = JSON.parse(summaryJson);

                if (summary.totalCount <= EXPAND_THRESHOLD) {
                    // Small neighborhood — load everything
                    var neighborJson = javaBridge.getNeighborsJson(nodeId);
                    var data = JSON.parse(neighborJson);
                    applyExpansionData(nodeId, data);
                } else {
                    // Large neighborhood — show filter dialog
                    showExpansionDialog(nodeId, summary);
                }
            } catch (e) {
                javaBridge.log('Error expanding node: ' + e.message);
            }
        }

        function applyExpansionData(nodeId, data) {
            var existingNodeIds = new Set(nodesDataSet.getIds());
            var existingEdgeKeys = new Set();
            var allEdges = edgesDataSet.get();
            for (var i = 0; i < allEdges.length; i++) {
                var e = allEdges[i];
                existingEdgeKeys.add(e.from + '|' + e.to + '|' + (e.label || ''));
            }

            var newNodes = [];
            var newEdges = [];

            data.nodes.forEach(function(node) {
                if (!existingNodeIds.has(node.id)) {
                    newNodes.push(node);
                }
            });

            data.edges.forEach(function(edge) {
                var key = edge.from + '|' + edge.to + '|' + (edge.label || '');
                if (!existingEdgeKeys.has(key)) {
                    edge.id = 'e_exp_' + Math.random().toString(36).substr(2, 9);
                    newEdges.push(edge);
                }
            });

            if (newNodes.length > 0) nodesDataSet.add(newNodes);
            if (newEdges.length > 0) edgesDataSet.add(newEdges);

            exploredNodes.add(nodeId);
            markNodeExplored(nodeId);

            updateStats();
            updateLegend();
            javaBridge.log('Expanded node: ' + nodeId + ' (+' + newNodes.length + ' nodes, +' + newEdges.length + ' edges)');
        }

        function showExpansionDialog(nodeId, summary) {
            pendingExpandNodeId = nodeId;
            var dialog = document.getElementById('expansion-dialog');
            var nodeLabel = getNodeLabel(nodeId);
            var html = '<h3 class="text-foreground mb-1 text-sm font-semibold">Expand "' + escapeHtml(nodeLabel) + '"</h3>';
            html += '<p class="text-muted-foreground mb-3 text-xs">' + summary.totalCount + ' neighbors found</p>';
            html += '<div class="mb-1.5 text-xs text-muted-foreground">Select types to load:</div>';

            summary.types.sort(function(a, b) { return b.count - a.count; });

            for (var i = 0; i < summary.types.length; i++) {
                var t = summary.types[i];
                var defaultChecked = t.count <= EXPAND_THRESHOLD ? ' checked' : '';
                html += '<label class="type-row flex items-center gap-2 px-2 py-1 rounded-md mb-0.5 cursor-pointer text-xs">';
                html += '<input type="checkbox" class="type-check" value="' + escapeHtml(t.type) + '"' + defaultChecked + '>';
                html += '<span class="type-dot w-3 h-3 rounded-full shrink-0 inline-block" style="background:' + t.color + '"></span>';
                html += '<span>' + escapeHtml(t.type) + '</span>';
                html += '<span class="ml-auto text-muted-foreground text-xs">(' + t.count + ')</span>';
                html += '</label>';
            }

            if (summary.literalCount > 0) {
                html += '<label class="type-row flex items-center gap-2 px-2 py-1 rounded-md mb-0.5 cursor-pointer text-xs">';
                html += '<input type="checkbox" id="include-literals" checked>';
                html += '<span class="type-dot w-3 h-3 rounded-full shrink-0 inline-block" style="background:#f0f0f0;border:1px solid #666"></span>';
                html += '<span>Data properties</span>';
                html += '<span class="ml-auto text-muted-foreground text-xs">(' + summary.literalCount + ')</span>';
                html += '</label>';
            }

            html += '<div class="mt-3.5 flex gap-2 items-center text-xs text-foreground">';
            html += '<label>Max per type:</label>';
            html += '<input type="number" class="px-2 py-1 bg-background border border-border rounded-md text-foreground text-xs outline-none" style="width:60px" id="max-per-type" value="25" min="0" title="0 = no limit">';
            html += '<span class="text-xs text-muted-foreground ml-1">(0 = all)</span>';
            html += '</div>';

            html += '<div class="flex gap-2 mt-4 justify-end">';
            html += '<button class="uk-btn uk-btn-default uk-btn-xs" onclick="selectAllTypes(true)">All</button>';
            html += '<button class="uk-btn uk-btn-default uk-btn-xs" onclick="selectAllTypes(false)">None</button>';
            html += '<button class="uk-btn uk-btn-primary uk-btn-xs" onclick="confirmExpansion()">Expand</button>';
            html += '<button class="uk-btn uk-btn-default uk-btn-xs" onclick="cancelExpansion()">Cancel</button>';
            html += '</div>';

            dialog.innerHTML = html;
            document.getElementById('expansion-dialog-overlay').style.display = 'flex';
        }

        function getNodeLabel(nodeId) {
            var node = nodesDataSet ? nodesDataSet.get(nodeId) : null;
            if (node && node.label) return node.label;
            var hash = nodeId.lastIndexOf('#');
            if (hash >= 0 && hash < nodeId.length - 1) return nodeId.substring(hash + 1);
            var slash = nodeId.lastIndexOf('/');
            if (slash >= 0 && slash < nodeId.length - 1) return nodeId.substring(slash + 1);
            return nodeId;
        }

        function selectAllTypes(checked) {
            var boxes = document.querySelectorAll('.type-check');
            for (var i = 0; i < boxes.length; i++) boxes[i].checked = checked;
            var litBox = document.getElementById('include-literals');
            if (litBox) litBox.checked = checked;
        }

        function confirmExpansion() {
            var nodeId = pendingExpandNodeId;
            if (!nodeId) return;

            var selectedTypes = [];
            var boxes = document.querySelectorAll('.type-check');
            for (var i = 0; i < boxes.length; i++) {
                if (boxes[i].checked) selectedTypes.push(boxes[i].value);
            }
            var litBox = document.getElementById('include-literals');
            var includeLiterals = litBox ? litBox.checked : true;
            var maxPerType = parseInt(document.getElementById('max-per-type').value) || 0;

            closeExpansionDialog();

            if (selectedTypes.length === 0 && !includeLiterals) {
                javaBridge.log('No types selected for expansion');
                return;
            }

            try {
                var typesJson = JSON.stringify(selectedTypes);
                var neighborJson = javaBridge.getFilteredNeighborsJson(
                    nodeId, typesJson, maxPerType, includeLiterals ? 'true' : 'false');
                var data = JSON.parse(neighborJson);
                applyExpansionData(nodeId, data);
                javaBridge.log('Filtered expand: ' + selectedTypes.join(', ') + ' max=' + maxPerType);
            } catch (e) {
                javaBridge.log('Error in filtered expansion: ' + e.message);
            }
        }

        function cancelExpansion() {
            closeExpansionDialog();
        }

        function closeExpansionDialog() {
            document.getElementById('expansion-dialog-overlay').style.display = 'none';
            pendingExpandNodeId = null;
        }

        function markNodeExplored(nodeId) {
            try {
                var node = nodesDataSet.get(nodeId);
                if (node && node.shape !== 'box') {
                    var color = node.color || {};
                    if (typeof color === 'string') {
                        color = { background: color };
                    }
                    color.border = EXPLORED_BORDER_COLOR;
                    nodesDataSet.update({
                        id: nodeId,
                        color: color,
                        borderWidth: EXPLORED_BORDER_WIDTH
                    });
                }
            } catch (e) {}
        }

        function showNodeInfo(nodeId) {
            var node = nodesDataSet.get(nodeId);
            if (!node) return;
            var info = document.getElementById('node-info');
            info.style.display = 'block';
            var html = '<h4>' + escapeHtml(node.label || nodeId) + '</h4>';
            if (node.title) {
                html += '<div class="uri">' + escapeHtml(node.title) + '</div>';
            }
            var isLiteral = (node.shape === 'box') || (node.id && node.id.indexOf('_lit_') >= 0);
            if (!isLiteral) {
                var isExplored = exploredNodes.has(nodeId);
                html += '<div style="margin-top:6px;color:' + (isExplored ? '#59a14f' : '#f28e2b') + '">' +
                        (isExplored ? '&#x2714; Explored' : '&#x25CB; Not explored (double-click to expand)') + '</div>';
            }
            info.innerHTML = html;
        }

        function expandLiteral(nodeId) {
            var node = nodesDataSet.get(nodeId);
            if (!node) return;
            var isLiteral = (node.shape === 'box') || (nodeId && nodeId.indexOf('_lit_') >= 0);
            if (!isLiteral) return;
            // title holds the full untruncated value
            if (!node.title || node.title === node.label) return;
            // Store original short label for later collapse
            nodesDataSet.update({
                id: nodeId,
                label: node.title,
                _shortLabel: node.label,
                font: { size: 12, color: '#222', multi: false },
                widthConstraint: { maximum: 450 }
            });
            expandedLiteralId = nodeId;
        }

        function collapseLiteral() {
            if (!expandedLiteralId) return;
            var node = nodesDataSet.get(expandedLiteralId);
            if (node && node._shortLabel) {
                nodesDataSet.update({
                    id: expandedLiteralId,
                    label: node._shortLabel,
                    _shortLabel: undefined,
                    font: { size: 11, color: '#333' },
                    widthConstraint: undefined
                });
            }
            expandedLiteralId = null;
        }

        function fitGraph() {
            if (network) network.fit({ animation: true });
        }

        function togglePhysics() {
            physicsEnabled = !physicsEnabled;
            if (network) {
                network.setOptions({ physics: { enabled: physicsEnabled } });
            }
        }

        function resetGraph() {
            exploredNodes.clear();
            initGraph(initialUri);
        }

        function updateStats() {
            var nodeCount = nodesDataSet ? nodesDataSet.length : 0;
            var edgeCount = edgesDataSet ? edgesDataSet.length : 0;
            document.getElementById('graph-stats').textContent =
                nodeCount + ' nodes | ' + edgeCount + ' edges | ' + exploredNodes.size + ' explored';
        }

        function updateLegend() {
            var types = {};
            if (nodesDataSet) {
                nodesDataSet.forEach(function(node) {
                    // Skip literal/box nodes (data properties)
                    if (node.shape === 'box') return;
                    if (node.id && node.id.indexOf('_lit_') >= 0) return;
                    if (node.title) {
                        var parts = node.title.split('\\n');
                        if (parts.length > 0) {
                            var type = parts[0];
                            if (!types[type]) {
                                var color = '#4e79a7';
                                if (node.color) {
                                    if (typeof node.color === 'string') color = node.color;
                                    else if (node.color.background) color = node.color.background;
                                }
                                types[type] = color;
                            }
                        }
                    }
                });
            }
            var legend = document.getElementById('legend');
            var wasCollapsed = legend.classList.contains('collapsed');
            var body = '';
            body += '<div class="flex items-center gap-1.5 mb-1"><span class="w-3 h-3 rounded-full inline-block" style="background:#ccc;border:2px solid #59a14f"></span> Explored</div>';
            body += '<div class="flex items-center gap-1.5 mb-1"><span class="w-3 h-3 rounded-full inline-block" style="background:#ccc;border:2px solid #999"></span> Not explored</div>';
            body += '<hr style="border-color:#2a2a4a;margin:6px 0">';
            for (var type in types) {
                body += '<div class="flex items-center gap-1.5 mb-1"><span class="w-3 h-3 rounded-full inline-block" style="background:' + types[type] + '"></span> ' + escapeHtml(type) + '</div>';
            }
            legend.innerHTML = '<div class="px-3 py-2 bg-secondary text-secondary-foreground text-xs font-semibold cursor-pointer select-none flex items-center gap-1.5" id="legend-header" onclick="toggleLegend()"><span class="toggle-arrow" style="font-size:10px">&#x25BC;</span> Legend</div>' +
                               '<div class="px-3 py-2" id="legend-body">' + body + '</div>';
            if (wasCollapsed) legend.classList.add('collapsed');
        }

        function toggleLegend() {
            var legend = document.getElementById('legend');
            legend.classList.toggle('collapsed');
        }

        // Reusable element for escapeHtml (avoids creating a new element each call)
        var _escDiv = document.createElement('div');
        function escapeHtml(text) {
            if (!text) return '';
            _escDiv.textContent = text;
            return _escDiv.innerHTML;
        }
    </script>
</body>
</html>
""";
    }
}
