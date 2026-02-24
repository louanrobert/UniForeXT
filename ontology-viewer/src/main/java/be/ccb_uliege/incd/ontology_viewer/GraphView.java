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
        #toolbar {
            background: linear-gradient(135deg, #16213e, #0f3460);
            padding: 10px 20px;
            display: flex;
            align-items: center;
            gap: 12px;
            border-bottom: 2px solid #e94560;
            flex-shrink: 0;
        }
        #toolbar h2 {
            font-size: 15px;
            color: #e94560;
            font-weight: 600;
        }
        #toolbar .info {
            font-size: 12px;
            color: #8899aa;
            margin-left: auto;
        }
        .btn {
            padding: 5px 12px;
            background: #0f3460;
            border: 1px solid #2a2a4a;
            border-radius: 4px;
            color: #c0c0c0;
            cursor: pointer;
            font-size: 12px;
            transition: all 0.2s;
        }
        .btn:hover {
            background: #e94560;
            color: white;
            border-color: #e94560;
        }
        #graph-wrapper {
            flex: 1;
            position: relative;
        }
        #graph-container {
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
        }
        #legend {
            position: absolute;
            top: 10px;
            right: 10px;
            background: rgba(22,33,62,0.95);
            border: 1px solid #2a2a4a;
            border-radius: 6px;
            font-size: 11px;
            z-index: 20;
            pointer-events: auto;
            overflow: hidden;
        }
        #legend-header {
            padding: 8px 12px;
            color: #e94560;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            user-select: none;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        #legend-header:hover {
            background: rgba(233,69,96,0.1);
        }
        #legend-header .toggle-arrow {
            font-size: 10px;
            transition: transform 0.2s;
        }
        #legend.collapsed #legend-header .toggle-arrow {
            transform: rotate(-90deg);
        }
        #legend-body {
            padding: 0 12px 8px 12px;
        }
        #legend.collapsed #legend-body {
            display: none;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-bottom: 4px;
        }
        .legend-dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            display: inline-block;
        }
        #node-info {
            position: absolute;
            bottom: 10px;
            left: 10px;
            right: 10px;
            background: rgba(22,33,62,0.95);
            border: 1px solid #2a2a4a;
            border-radius: 6px;
            padding: 12px 16px;
            font-size: 12px;
            max-height: 150px;
            overflow-y: auto;
            display: none;
            z-index: 20;
            pointer-events: auto;
        }
        #node-info h4 { color: #e94560; margin-bottom: 4px; }
        #node-info .uri { color: #8899aa; font-size: 10px; word-break: break-all; }

        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-track { background: #1a1a2e; }
        ::-webkit-scrollbar-thumb { background: #2a2a4a; border-radius: 3px; }

        /* Hide directional navigation arrows */
        .vis-button.vis-up,
        .vis-button.vis-down,
        .vis-button.vis-left,
        .vis-button.vis-right {
            display: none !important;
        }
        /* Style zoom controls to match dark theme */
        .vis-navigation {
            background: transparent;
        }
        .vis-button.vis-zoomIn,
        .vis-button.vis-zoomOut,
        .vis-button.vis-zoomExtends {
            background-color: rgba(22,33,62,0.9) !important;
            border: 1px solid #2a2a4a !important;
            border-radius: 4px !important;
            background-image: none !important;
            color: #e0e0e0 !important;
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
            background-color: #e94560 !important;
            border-color: #e94560 !important;
        }
        .vis-button.vis-zoomIn::after { content: '+'; font-size: 18px; font-weight: bold; }
        .vis-button.vis-zoomOut::after { content: '-'; font-size: 18px; font-weight: bold; }
        .vis-button.vis-zoomExtends::after { content: 'fit'; font-size: 16px; }

        /* Expansion dialog */
        #expansion-dialog-overlay {
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0,0,0,0.6);
            z-index: 200;
            display: none;
            justify-content: center;
            align-items: center;
        }
        #expansion-dialog {
            background: #16213e;
            border: 1px solid #e94560;
            border-radius: 8px;
            padding: 20px 24px;
            min-width: 340px;
            max-width: 440px;
            max-height: 80vh;
            overflow-y: auto;
            color: #e0e0e0;
            box-shadow: 0 8px 32px rgba(0,0,0,0.5);
        }
        #expansion-dialog h3 {
            color: #e94560;
            margin-bottom: 4px;
            font-size: 15px;
        }
        .type-row {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 5px 8px;
            border-radius: 4px;
            margin-bottom: 2px;
            cursor: pointer;
            font-size: 13px;
        }
        .type-row:hover {
            background: rgba(233,69,96,0.1);
        }
        .type-row .count {
            margin-left: auto;
            color: #8899aa;
            font-size: 12px;
        }
        .type-dot {
            width: 12px; height: 12px;
            border-radius: 50%;
            flex-shrink: 0;
            display: inline-block;
        }
        .dialog-controls {
            margin-top: 14px;
            display: flex;
            gap: 8px;
            align-items: center;
            font-size: 13px;
        }
        .dialog-controls input[type="number"] {
            padding: 4px 8px;
            background: #0f3460;
            border: 1px solid #2a2a4a;
            border-radius: 4px;
            color: #e0e0e0;
            font-size: 12px;
            width: 60px;
        }
        .dialog-actions {
            display: flex;
            gap: 8px;
            margin-top: 16px;
            justify-content: flex-end;
        }
        .dialog-actions .expand-btn {
            background: #e94560 !important;
            color: white !important;
            border-color: #e94560 !important;
        }
    </style>
    <script src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"></script>
</head>
<body>
    <div id="toolbar">
        <h2>&#x1F578; Neighborhood Graph</h2>
        <button class="btn" onclick="fitGraph()">Fit View</button>
        <button class="btn" onclick="togglePhysics()">Toggle Physics</button>
        <button class="btn" onclick="resetGraph()">Reset</button>
        <span class="info" id="graph-stats"></span>
    </div>
    <div id="graph-wrapper">
        <div id="graph-container"></div>
        <div id="legend"></div>
        <div id="node-info"></div>
        <div id="expansion-dialog-overlay">
            <div id="expansion-dialog"></div>
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
                    '<div style="padding:40px;text-align:center;color:#e94560;">' +
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
            edgesDataSet.forEach(function(e) {
                existingEdgeKeys.add(e.from + '|' + e.to + '|' + (e.label || ''));
            });

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
            var html = '<h3>Expand "' + escapeHtml(nodeLabel) + '"</h3>';
            html += '<p style="color:#8899aa;margin-bottom:12px">' + summary.totalCount + ' neighbors found</p>';
            html += '<div style="margin-bottom:6px;font-size:12px;color:#8899aa">Select types to load:</div>';

            summary.types.sort(function(a, b) { return b.count - a.count; });

            for (var i = 0; i < summary.types.length; i++) {
                var t = summary.types[i];
                var defaultChecked = t.count <= EXPAND_THRESHOLD ? ' checked' : '';
                html += '<label class="type-row">';
                html += '<input type="checkbox" class="type-check" value="' + escapeHtml(t.type) + '"' + defaultChecked + '>';
                html += '<span class="type-dot" style="background:' + t.color + '"></span>';
                html += '<span>' + escapeHtml(t.type) + '</span>';
                html += '<span class="count">(' + t.count + ')</span>';
                html += '</label>';
            }

            if (summary.literalCount > 0) {
                html += '<label class="type-row">';
                html += '<input type="checkbox" id="include-literals" checked>';
                html += '<span class="type-dot" style="background:#f0f0f0;border:1px solid #666"></span>';
                html += '<span>Data properties</span>';
                html += '<span class="count">(' + summary.literalCount + ')</span>';
                html += '</label>';
            }

            html += '<div class="dialog-controls">';
            html += '<label>Max per type:</label>';
            html += '<input type="number" id="max-per-type" value="25" min="0" title="0 = no limit">';
            html += '<span style="font-size:11px;color:#666;margin-left:4px">(0 = all)</span>';
            html += '</div>';

            html += '<div class="dialog-actions">';
            html += '<button class="btn" onclick="selectAllTypes(true)">All</button>';
            html += '<button class="btn" onclick="selectAllTypes(false)">None</button>';
            html += '<button class="btn expand-btn" onclick="confirmExpansion()">Expand</button>';
            html += '<button class="btn" onclick="cancelExpansion()">Cancel</button>';
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
            body += '<div class="legend-item"><span class="legend-dot" style="background:#ccc;border:2px solid #59a14f"></span> Explored</div>';
            body += '<div class="legend-item"><span class="legend-dot" style="background:#ccc;border:2px solid #999"></span> Not explored</div>';
            body += '<hr style="border-color:#2a2a4a;margin:6px 0">';
            for (var type in types) {
                body += '<div class="legend-item"><span class="legend-dot" style="background:' + types[type] + '"></span> ' + escapeHtml(type) + '</div>';
            }
            legend.innerHTML = '<div id="legend-header" onclick="toggleLegend()"><span class="toggle-arrow">&#x25BC;</span> Legend</div>' +
                               '<div id="legend-body">' + body + '</div>';
            if (wasCollapsed) legend.classList.add('collapsed');
        }

        function toggleLegend() {
            var legend = document.getElementById('legend');
            legend.classList.toggle('collapsed');
        }

        function escapeHtml(text) {
            if (!text) return '';
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
