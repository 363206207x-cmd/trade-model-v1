import WebKit
import XCTest

@MainActor
final class DashboardMobileDomInteractionTests: XCTestCase {
    private var navigationDelegate: NavigationDelegate?

    func testAssetSwitchUpdatesExecutionAndAiWithoutMutatingPositionDom() throws {
        let webView = try loadFixture()
        let originalPosition = try stringValue(
            "document.querySelector('[data-position-independent]').outerHTML",
            in: webView
        )

        try run("document.querySelector('[data-symbol=\"ETHUSDT\"]').click()", in: webView)
        XCTAssertTrue(waitUntil("window.__pendingRequests.length === 1", in: webView))
        try run("window.__resolveDashboard(0, 'ETHUSDT')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-execution-field=\"direction\"]').textContent === 'DIR_ETHUSDT'",
            in: webView
        ))

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-selected-asset-token]').textContent", in: webView),
            "ETHUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-ai-run-status]').textContent", in: webView),
            "AI_ETHUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-independent]').outerHTML", in: webView),
            originalPosition
        )
    }

    func testRapidSameAssetRequestsKeepBusyUntilLatestRequestFinishes() throws {
        let webView = try loadFixture()
        let cardSelector = "document.querySelector('[data-symbol=\"BTCUSDT\"]')"

        try run("\(cardSelector).click(); \(cardSelector).click()", in: webView)
        XCTAssertTrue(waitUntil("window.__pendingRequests.length === 2", in: webView))
        XCTAssertTrue(waitUntil("window.__pendingRequests[0].aborted === true", in: webView))
        XCTAssertEqual(try stringValue("\(cardSelector).getAttribute('aria-busy')", in: webView), "true")

        try run("window.__resolveDashboard(1, 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("!\(cardSelector).hasAttribute('aria-busy')", in: webView))
        XCTAssertEqual(try booleanValue("\(cardSelector).hasAttribute('aria-busy')", in: webView), false)
    }

    func testHomeNavigationResetsScrollAndFocusesTitle() throws {
        let webView = try loadFixture()
        try run("window.scrollTo(0, 900)", in: webView)
        XCTAssertTrue(waitUntil("window.scrollY > 0", in: webView))

        try run("document.querySelector('[data-home-nav]').click()", in: webView)
        XCTAssertTrue(waitUntil(
            "window.scrollY === 0 && document.activeElement.id === 'mobile-home-title'",
            in: webView,
            timeout: 2
        ))
    }

    func testAiRoleSwitchKeepsOnePanelVisibleWithoutChangingAsset() throws {
        let webView = try loadFixture()
        let selectedAsset = try stringValue(
            "document.querySelector('[data-selected-asset-token]').textContent",
            in: webView
        )

        try run("document.querySelector('[data-role=\"GEMINI_REVIEW\"]').click()", in: webView)

        XCTAssertEqual(
            try numberValue("document.querySelectorAll('[data-role][aria-selected=\"true\"]').length", in: webView),
            1
        )
        XCTAssertEqual(
            try numberValue("Array.from(document.querySelectorAll('[data-role-panel]')).filter(p => !p.hidden).length", in: webView),
            1
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-role][aria-selected=\"true\"]').dataset.role", in: webView),
            "GEMINI_REVIEW"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-selected-asset-token]').textContent", in: webView),
            selectedAsset
        )
    }

    func testDynamicTypeAttributeChangesComputedMobileFontSizes() throws {
        let webView = try loadFixture()
        let defaultBody = try numberValue("parseFloat(getComputedStyle(document.body).fontSize)", in: webView)
        let defaultExecution = try numberValue(
            "parseFloat(getComputedStyle(document.querySelector('[data-execution-field=\"direction\"]')).fontSize)",
            in: webView
        )

        try run("document.documentElement.dataset.mobileTextSize = 'large'", in: webView)
        let largeBody = try numberValue("parseFloat(getComputedStyle(document.body).fontSize)", in: webView)
        let largeExecution = try numberValue(
            "parseFloat(getComputedStyle(document.querySelector('[data-execution-field=\"direction\"]')).fontSize)",
            in: webView
        )

        XCTAssertEqual(defaultBody, 17, accuracy: 0.01)
        XCTAssertEqual(largeBody, 18.36, accuracy: 0.01)
        XCTAssertGreaterThan(largeBody, defaultBody)
        XCTAssertGreaterThan(largeExecution, defaultExecution)
        XCTAssertEqual(
            try stringValue("document.documentElement.dataset.mobileTextSize", in: webView),
            "large"
        )
    }

    func testLargeTextContentCanScrollClearOfBottomNavigationOn12ProMax() throws {
        let webView = try loadFixture(width: 428, height: 746)
        try run("document.documentElement.dataset.mobileTextSize = 'large'", in: webView)
        try run("window.scrollTo(0, document.documentElement.scrollHeight)", in: webView)

        XCTAssertTrue(waitUntil("window.scrollY > 0", in: webView))
        let markerBottom = try numberValue(
            "document.getElementById('fixture-end-marker').getBoundingClientRect().bottom",
            in: webView
        )
        let navigationTop = try numberValue(
            "document.querySelector('.bottom-nav').getBoundingClientRect().top",
            in: webView
        )

        XCTAssertLessThanOrEqual(markerBottom, navigationTop - 12)
        XCTAssertEqual(try booleanValue("document.documentElement.scrollWidth > window.innerWidth", in: webView), false)
    }

    private func loadFixture(width: CGFloat = 440, height: CGFloat = 852) throws -> WKWebView {
        let loaded = expectation(description: "mobile fixture loaded")
        let delegate = NavigationDelegate { loaded.fulfill() }
        navigationDelegate = delegate
        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        webView.navigationDelegate = delegate
        webView.loadHTMLString(try fixtureHTML(), baseURL: URL(string: "https://app.example.test"))
        wait(for: [loaded], timeout: 5)
        XCTAssertTrue(waitUntil("document.readyState === 'complete'", in: webView))
        return webView
    }

    private func fixtureHTML() throws -> String {
        let bundle = Bundle(for: DashboardMobileDomInteractionTests.self)
        guard let scriptURL = bundle.url(forResource: "dashboard-mobile", withExtension: "js"),
              let styleURL = bundle.url(forResource: "dashboard-mobile", withExtension: "css") else {
            throw FixtureError.missingProductionResource
        }
        let script = try String(contentsOf: scriptURL)
        let styles = try String(contentsOf: styleURL)
        return """
        <!doctype html>
        <html lang="zh-CN">
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>\(styles)</style>
          <script>
            window.__pendingRequests = [];
            window.fetch = function(url, options) {
              return new Promise(function(resolve, reject) {
                var request = { url: url, options: options, resolve: resolve, reject: reject, aborted: false };
                options.signal.addEventListener('abort', function() {
                  request.aborted = true;
                  reject(new DOMException('Aborted', 'AbortError'));
                }, { once: true });
                window.__pendingRequests.push(request);
              });
            };
            window.__resolveDashboard = function(index, symbol) {
              var request = window.__pendingRequests[index];
              var data = {
                selectedSymbol: symbol,
                executionSuggestion: {
                  statusLabel: 'READY_' + symbol,
                  blockedReason: 'RISK_' + symbol,
                  direction: 'DIR_' + symbol,
                  entryZone: 'ENTRY_' + symbol
                },
                aiDecision: {
                  runStatusLabel: 'AI_' + symbol,
                  consistency: {
                    consistencyLevel: 'CONSISTENT_' + symbol,
                    level: 'NONE',
                    confused: false,
                    aiApplicable: true
                  },
                  tabs: [
                    { role: 'GPT_FINAL', finalConclusion: 'GPT_' + symbol },
                    { role: 'GEMINI_REVIEW', reviewConclusion: 'GEMINI_' + symbol },
                    { role: 'GROK_CHALLENGE', challengeConclusion: 'GROK_' + symbol }
                  ]
                }
              };
              request.resolve({
                ok: true,
                json: function() { return Promise.resolve({ code: 200, data: data }); }
              });
            };
          </script>
        </head>
        <body>
          <main class="mobile-home">
            <header class="mobile-header"><h1 id="mobile-home-title" tabindex="-1">首页总览</h1></header>
            <section class="watch-section">
              <strong data-selected-asset-token>BTCUSDT</strong>
              <div class="asset-pager" role="radiogroup">
                <button class="asset-card asset-select is-selected" data-symbol="BTCUSDT" aria-checked="true">BTCUSDT</button>
                <button class="asset-card asset-select" data-symbol="ETHUSDT" aria-checked="false">ETHUSDT</button>
              </div>
            </section>
            <section class="execution-section">
              <strong data-execution-field="statusLabel">等待同步</strong>
              <p data-execution-field="blockedReason">暂无补充说明</p>
              <dl class="definition-list execution-compact-grid">
                <div><dt>方向</dt><dd data-execution-field="direction">--</dd></div>
                <div><dt>入场区间</dt><dd data-execution-field="entryZone">--</dd></div>
              </dl>
            </section>
            <section data-position-independent><p>POSITION_A_STATIC</p></section>
            <section>
              <strong data-ai-run-status>等待同步</strong>
              <span data-consistency-field="consistencyLevel">等待同步</span>
              <span data-consistency-field="level">--</span>
              <span data-consistency-field="confused">否</span>
              <span data-consistency-field="consistencySummary">等待同步</span>
              <span data-consistency-field="aiApplicable">不适用</span>
              <span data-consistency-field="consistencyScore">--</span>
              <span data-consistency-field="directionalPushBlocked">否</span>
              <span data-consistency-field="downgradeReason">暂无降级原因</span>
              <div data-ai-role-root>
                <div role="tablist">
                  <button id="mobile-role-tab-GPT_FINAL" data-role="GPT_FINAL" aria-selected="true">GPT</button>
                  <button id="mobile-role-tab-GEMINI_REVIEW" data-role="GEMINI_REVIEW" aria-selected="false">Gemini</button>
                  <button id="mobile-role-tab-GROK_CHALLENGE" data-role="GROK_CHALLENGE" aria-selected="false">Grok</button>
                </div>
                <article data-role-panel="GPT_FINAL">GPT panel</article>
                <article data-role-panel="GEMINI_REVIEW" hidden>Gemini panel</article>
                <article data-role-panel="GROK_CHALLENGE" hidden>Grok panel</article>
              </div>
            </section>
            <div style="height: 1800px"></div>
            <div id="fixture-end-marker" style="height: 1px"></div>
          </main>
          <nav class="bottom-nav"><button type="button" data-home-nav>首页</button></nav>
          <script>\(script)</script>
        </body>
        </html>
        """
    }

    private func waitUntil(_ expression: String, in webView: WKWebView, timeout: TimeInterval = 1) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        repeat {
            if (try? booleanValue(expression, in: webView)) == true {
                return true
            }
            RunLoop.main.run(until: Date().addingTimeInterval(0.02))
        } while Date() < deadline
        return false
    }

    private func run(_ script: String, in webView: WKWebView) throws {
        _ = try evaluate(script, in: webView)
    }

    private func stringValue(_ script: String, in webView: WKWebView) throws -> String {
        guard let result = try evaluate(script, in: webView) as? String else {
            throw FixtureError.unexpectedJavaScriptValue
        }
        return result
    }

    private func numberValue(_ script: String, in webView: WKWebView) throws -> Double {
        guard let result = try evaluate(script, in: webView) as? NSNumber else {
            throw FixtureError.unexpectedJavaScriptValue
        }
        return result.doubleValue
    }

    private func booleanValue(_ script: String, in webView: WKWebView) throws -> Bool {
        guard let result = try evaluate("Boolean(\(script))", in: webView) as? NSNumber else {
            throw FixtureError.unexpectedJavaScriptValue
        }
        return result.boolValue
    }

    private func evaluate(_ script: String, in webView: WKWebView) throws -> Any? {
        let completed = expectation(description: "JavaScript evaluated")
        var value: Any?
        var evaluationError: Error?
        webView.evaluateJavaScript(script) { result, error in
            value = result
            evaluationError = error
            completed.fulfill()
        }
        wait(for: [completed], timeout: 5)
        if let evaluationError {
            throw evaluationError
        }
        return value
    }

    private final class NavigationDelegate: NSObject, WKNavigationDelegate {
        private let finish: () -> Void

        init(finish: @escaping () -> Void) {
            self.finish = finish
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation?) {
            finish()
        }
    }

    private enum FixtureError: Error {
        case missingProductionResource
        case unexpectedJavaScriptValue
    }
}
