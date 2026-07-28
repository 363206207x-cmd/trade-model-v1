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
            try stringValue("window.location.search", in: webView),
            "?selectedSymbol=ETHUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-independent]').outerHTML", in: webView),
            originalPosition
        )
    }

    func testDesktopOverviewRuntimeRefreshUsesOnlyHomeProjection() throws {
        let webView = try loadDesktopTemplate()

        XCTAssertTrue(waitUntil("window.__overviewRequests.length === 1", in: webView, timeout: 2))
        XCTAssertTrue(try booleanValue(
            "window.__overviewRequests.every(request => request.url.startsWith('/api/dashboard/home?'))",
            in: webView
        ))

        try run("void window.refreshDashboard()", in: webView)
        XCTAssertTrue(waitUntil("window.__overviewRequests.length === 2", in: webView, timeout: 2))
        XCTAssertTrue(try booleanValue(
            "window.__overviewRequests.every(request => request.url.startsWith('/api/dashboard/home?'))",
            in: webView
        ))
        XCTAssertTrue(try booleanValue(
            "window.__overviewRequests.every(request => request.options.method === 'GET')",
            in: webView
        ))
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

    func testUnverifiedExecutionPlanKeepsBackendStateAndClearsPreviouslyVisibleBoundaries() throws {
        let webView = try loadFixture()

        try run("document.querySelector('[data-symbol=\"ETHUSDT\"]').click()", in: webView)
        XCTAssertTrue(waitUntil("window.__pendingRequests.length === 1", in: webView))
        try run("window.__resolveDashboard(0, 'ETHUSDT', false)", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-execution-field=\"statusLabel\"]').textContent === 'READY_ETHUSDT'",
            in: webView
        ))

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-execution-field=\"statusLabel\"]').textContent", in: webView),
            "READY_ETHUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-execution-field=\"direction\"]').textContent", in: webView),
            "--"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-execution-field=\"entryZone\"]').textContent", in: webView),
            "--"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-execution-field=\"stopLoss\"]').textContent", in: webView),
            "--"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-execution-field=\"takeProfitRules\"]').textContent", in: webView),
            "--"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-execution-field=\"blockedReason\"]').textContent", in: webView),
            "RISK_ETHUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-execution-conflict]').textContent", in: webView),
            "RISK_ETHUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.getElementById('execution-advice').dataset.exactPlanVisible", in: webView),
            "false"
        )
    }

    func testHomeNavigationResetsScrollAndFocusesTitle() throws {
        let webView = try loadFixture()
        try run("window.scrollTo(0, 900)", in: webView)
        XCTAssertTrue(waitUntil("window.scrollY > 0", in: webView))

        try run("document.querySelector('[data-home-nav]').click()", in: webView)
        XCTAssertTrue(waitUntil(
            "window.scrollY === 0 && document.activeElement.id === 'mobile-page-context'",
            in: webView,
            timeout: 2
        ))
    }

    func testSelectedThirdAssetStaysVisibleWithoutMovingTheDocument() throws {
        let webView = try loadFixture()

        try run("document.querySelector('[data-symbol=\"SOLUSDT\"]').click()", in: webView)
        XCTAssertTrue(waitUntil("window.__pendingRequests.length === 1", in: webView))
        try run("window.__resolveDashboard(0, 'SOLUSDT')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-symbol=\"SOLUSDT\"]').getAttribute('aria-checked') === 'true'",
            in: webView
        ))
        XCTAssertTrue(waitUntil(
            "(() => { const pager = document.querySelector('.asset-pager').getBoundingClientRect(); const card = document.querySelector('[data-symbol=\"SOLUSDT\"]').getBoundingClientRect(); return document.querySelector('.asset-pager').scrollLeft > 0 && card.left >= pager.left - 1 && card.right <= pager.right + 1; })()",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(try numberValue("window.scrollX", in: webView), 0)
        XCTAssertTrue(try booleanValue(
            "(() => { const pager = document.querySelector('.asset-pager').getBoundingClientRect(); const card = document.querySelector('[data-symbol=\"SOLUSDT\"]').getBoundingClientRect(); return card.left >= pager.left - 1 && card.right <= pager.right + 1; })()",
            in: webView
        ))
    }

    func testDeepLinkedThirdAssetIsSelectedAndKeyboardReachableOnFirstRender() throws {
        let webView = try loadFixture(selectedSymbol: "SOLUSDT")

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-selected-asset-token]').textContent", in: webView),
            "SOLUSDT"
        )
        XCTAssertEqual(
            try numberValue("document.querySelectorAll('.asset-card[aria-checked=\"true\"]').length", in: webView),
            1
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('.asset-card[aria-checked=\"true\"]').dataset.symbol", in: webView),
            "SOLUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-symbol=\"SOLUSDT\"]').getAttribute('tabindex')", in: webView),
            "0"
        )

        try run("document.querySelector('[data-symbol=\"SOLUSDT\"]').focus()", in: webView)

        XCTAssertEqual(
            try stringValue("document.activeElement.dataset.symbol", in: webView),
            "SOLUSDT"
        )
        XCTAssertEqual(
            try stringValue("window.location.search", in: webView),
            "?selectedSymbol=SOLUSDT"
        )
    }

    func testMobileHomeAnalysisEntryUsesOnlyCurrentAuthoritativeAnalysisId() throws {
        let webView = try loadFixture()
        let link = "document.querySelector('[data-asset-detail-link]')"

        XCTAssertTrue(try booleanValue(
            "new URL(\(link).href).searchParams.get('analysisId') === 'ANA_BTCUSDT'",
            in: webView
        ))
        XCTAssertTrue(try booleanValue(
            "new URL(\(link).href).searchParams.get('selectedSymbol') === 'BTCUSDT'",
            in: webView
        ))

        try run("document.querySelector('[data-symbol=\"ETHUSDT\"]').click()", in: webView)
        XCTAssertTrue(waitUntil("window.__pendingRequests.length === 1", in: webView))
        try run("window.__resolveDashboard(0, 'ETHUSDT', true, true)", in: webView)
        XCTAssertTrue(waitUntil(
            "new URL(\(link).href).searchParams.get('analysisId') === 'ANA_ETHUSDT'",
            in: webView
        ))

        try run("document.querySelector('[data-symbol=\"SOLUSDT\"]').click()", in: webView)
        XCTAssertTrue(waitUntil("window.__pendingRequests.length === 2", in: webView))
        try run("window.__resolveDashboard(1, 'SOLUSDT', true, false)", in: webView)
        XCTAssertTrue(waitUntil("\(link).getAttribute('aria-disabled') === 'true'", in: webView))
        XCTAssertEqual(try stringValue("\(link).getAttribute('href') || ''", in: webView), "")
        XCTAssertEqual(try stringValue("\(link).textContent", in: webView), "当前不可查看")
    }

    func testAiSummaryKeepsExactlyThreeRolesVisibleWithoutChangingAsset() throws {
        let webView = try loadFixture()
        let selectedAsset = try stringValue(
            "document.querySelector('[data-selected-asset-token]').textContent",
            in: webView
        )

        XCTAssertEqual(
            try numberValue("document.querySelectorAll('[data-ai-role-summary]').length", in: webView),
            3
        )
        XCTAssertEqual(
            try stringValue(
                "Array.from(document.querySelectorAll('[data-ai-role-summary]')).map(node => node.dataset.aiRoleSummary).join('|')",
                in: webView
            ),
            "GPT_FINAL|GEMINI_REVIEW|GROK_CHALLENGE"
        )
        XCTAssertEqual(
            try numberValue(
                "document.querySelector('[data-mobile-home-view]').querySelectorAll('[data-role-panel], [role=\"tablist\"]').length",
                in: webView
            ),
            0
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

        XCTAssertEqual(defaultBody, 16, accuracy: 0.01)
        XCTAssertEqual(largeBody, 18, accuracy: 0.01)
        XCTAssertGreaterThan(largeBody, defaultBody)
        XCTAssertGreaterThan(largeExecution, defaultExecution)
        XCTAssertEqual(
            try stringValue("document.documentElement.dataset.mobileTextSize", in: webView),
            "large"
        )
    }

    func testWatchSearchAndUnavailableAddKeepExistingThreeAssetContract() throws {
        let webView = try loadFixture()

        XCTAssertEqual(try numberValue("document.querySelectorAll('.asset-select').length", in: webView), 3)
        XCTAssertGreaterThanOrEqual(
            try numberValue("document.querySelector('[data-asset-search-toggle]').getBoundingClientRect().height", in: webView),
            44
        )
        XCTAssertGreaterThanOrEqual(
            try numberValue("document.querySelector('[data-asset-add]').getBoundingClientRect().height", in: webView),
            44
        )

        try run("document.querySelector('[data-asset-search-toggle]').click()", in: webView)
        try run("""
            const input = document.querySelector('[data-asset-search-input]');
            input.value = 'ETH';
            input.dispatchEvent(new Event('input', { bubbles: true }));
            """, in: webView)

        XCTAssertEqual(
            try numberValue("document.querySelectorAll('.asset-search-result').length", in: webView),
            1
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('.asset-search-result').textContent", in: webView),
            "ETHUSDT"
        )

        try run("document.querySelector('.asset-search-result').click()", in: webView)
        XCTAssertTrue(waitUntil("window.__pendingRequests.length === 1", in: webView))
        try run("window.__resolveDashboard(0, 'ETHUSDT')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-selected-asset-token]').textContent === 'ETHUSDT'",
            in: webView
        ))
        XCTAssertTrue(try booleanValue("document.querySelector('[data-asset-add]').disabled", in: webView))
        XCTAssertEqual(
            try stringValue("document.getElementById('watch-add-contract-status').textContent", in: webView),
            "添加资产暂未开放"
        )
        XCTAssertTrue(try booleanValue(
            "Object.keys(localStorage).every(key => !key.includes('dashboard_custom_symbols'))",
            in: webView
        ))
        XCTAssertEqual(try numberValue("document.querySelectorAll('.asset-select').length", in: webView), 3)
    }

    func testApprovedInformationArchitectureHasSevenSectionsAndFiveNavigationItems() throws {
        let webView = try loadFixture()

        XCTAssertEqual(
            try stringValue(
                "Array.from(document.querySelectorAll('[data-mobile-home-view] > header, [data-mobile-home-view] > section')).map(node => node.id || node.className).join('|')",
                in: webView
            ),
            "mobile-header|mobile-status|mobile-alerts|watch-assets|execution-advice|position-monitor|ai-review"
        )
        XCTAssertEqual(
            try stringValue(
                "Array.from(document.querySelectorAll('.bottom-nav button, .bottom-nav a')).map(node => node.textContent.trim()).join('|')",
                in: webView
            ),
            "首页|持仓|AI分析|消息|我的"
        )
        XCTAssertEqual(
            try numberValue("document.querySelectorAll('.bottom-nav button, .bottom-nav a').length", in: webView),
            5
        )
        XCTAssertEqual(
            try numberValue("document.querySelectorAll('.bottom-nav [data-unavailable-nav][aria-disabled=\"true\"]').length", in: webView),
            2
        )
        XCTAssertEqual(try numberValue("document.querySelectorAll('.status-cell').length", in: webView), 8)
        XCTAssertEqual(try numberValue("document.querySelectorAll('[data-ai-role-summary]').length", in: webView), 3)
        XCTAssertEqual(
            try stringValue("Array.from(document.querySelectorAll('[data-ai-role-summary]')).map(node => node.dataset.aiRoleSummary).join('|')", in: webView),
            "GPT_FINAL|GEMINI_REVIEW|GROK_CHALLENGE"
        )
    }

    func testNativeToolbarProjectionDoesNotRepeatVisibleProductTitle() throws {
        let webView = try loadFixture()

        XCTAssertEqual(
            try numberValue("Array.from(document.querySelectorAll('h1')).filter(node => !node.closest('[hidden]')).length", in: webView),
            1
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('h1').textContent", in: webView),
            "首页"
        )
        XCTAssertEqual(try booleanValue("document.body.textContent.includes('TRADE MODEL V1')", in: webView), true)
        XCTAssertTrue(try booleanValue(
            "document.getElementById('mobile-page-context').getBoundingClientRect().height > 0",
            in: webView
        ))
    }

    func testHeaderSearchAndPositionNavigationStayInsideExistingMobileProjection() throws {
        let webView = try loadFixture()

        try run("document.querySelector('[data-header-search]').click()", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-asset-search-toggle]').getAttribute('aria-expanded') === 'true' && document.activeElement.matches('[data-asset-search-input]')",
            in: webView,
            timeout: 2
        ))
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-home-nav]').getAttribute('aria-current')", in: webView),
            "page"
        )

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-nav]').getAttribute('href')", in: webView),
            "/dashboard/mobile/positions"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-home-nav]').getAttribute('aria-current')", in: webView),
            "page"
        )
        XCTAssertEqual(try numberValue("document.querySelectorAll('.bottom-nav [aria-current]').length", in: webView), 1)
    }

    func testAiAnalysisTabUsesAuthoritativeIdentityAndReusesFe03Detail() throws {
        let webView = try loadFixture()

        try run("document.querySelector('[data-ai-nav]').click()", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-mobile-home-view]').hidden && !document.querySelector('[data-mobile-ai-view]').hidden",
            in: webView,
            timeout: 2
        ))
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-ai-nav]').getAttribute('aria-current')", in: webView),
            "page"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-ai-analysis-id]').textContent", in: webView),
            "ANA_BTCUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-ai-analysis-root]').dataset.analysisIdentity", in: webView),
            "verified"
        )
        XCTAssertTrue(try booleanValue(
            "document.querySelector('[data-ai-analysis-detail-link]').getAttribute('href').includes('analysisId=ANA_BTCUSDT')",
            in: webView
        ))
        XCTAssertEqual(
            try stringValue(
                "Array.from(document.querySelectorAll('[data-ai-analysis-tab]')).map(node => node.dataset.aiAnalysisTab).join('|')",
                in: webView
            ),
            "GPT_FINAL|GEMINI_REVIEW|GROK_CHALLENGE"
        )
        XCTAssertTrue(try booleanValue("document.querySelector('.ai-analysis-search').disabled", in: webView))
        XCTAssertEqual(
            try stringValue("document.querySelector('.ai-analysis-role-tabs').getAttribute('role')", in: webView),
            "tablist"
        )
        XCTAssertGreaterThanOrEqual(
            try numberValue(
                "Math.min(...Array.from(document.querySelectorAll('[data-ai-analysis-tab], [data-ai-analysis-detail-link]')).map(node => node.getBoundingClientRect().height))",
                in: webView
            ),
            44
        )
        try run("document.documentElement.dataset.mobileTextSize = 'accessibility'", in: webView)
        XCTAssertGreaterThan(
            try numberValue("parseFloat(getComputedStyle(document.getElementById('mobile-ai-analysis-title')).fontSize)", in: webView),
            20
        )
        XCTAssertLessThanOrEqual(
            try numberValue("document.documentElement.scrollWidth", in: webView),
            try numberValue("window.innerWidth", in: webView)
        )

        try run("document.querySelector('[data-ai-analysis-tab=\"GEMINI_REVIEW\"]').click()", in: webView)
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-ai-analysis-tab][aria-selected=\"true\"]').dataset.aiAnalysisTab", in: webView),
            "GEMINI_REVIEW"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-ai-analysis-role-panel]:not([hidden])').dataset.aiAnalysisRolePanel", in: webView),
            "GEMINI_REVIEW"
        )

        try run("document.querySelector('[data-home-nav]').click()", in: webView)
        XCTAssertTrue(waitUntil(
            "!document.querySelector('[data-mobile-home-view]').hidden && document.querySelector('[data-mobile-ai-view]').hidden",
            in: webView,
            timeout: 2
        ))
    }

    func testOnlyAssetPagerCanOverflowHorizontallyAndAllVisibleNavigationTargetsFit() throws {
        let webView = try loadFixture(width: 440, height: 852)

        XCTAssertGreaterThan(
            try numberValue("document.querySelector('.asset-pager').scrollWidth", in: webView),
            try numberValue("document.querySelector('.asset-pager').clientWidth", in: webView)
        )
        XCTAssertEqual(try booleanValue("document.documentElement.scrollWidth > window.innerWidth", in: webView), false)
        XCTAssertEqual(try booleanValue("document.body.scrollWidth > window.innerWidth", in: webView), false)
        XCTAssertEqual(
            try numberValue("Array.from(document.querySelectorAll('.bottom-nav button, .bottom-nav a')).filter(node => node.getBoundingClientRect().height < 44).length", in: webView),
            0
        )
    }

    func testApprovedLayoutCapturesLightAndDarkEvidence() throws {
        let appearances: [(CGFloat, CGFloat, UIUserInterfaceStyle, String, String)] = [
            (440, 956, .light, "default", "p3-u2-mobile-home-17pm-light"),
            (440, 956, .dark, "default", "p3-u2-mobile-home-17pm-dark"),
            (440, 956, .light, "accessibility", "p3-u2-mobile-home-17pm-large-text"),
            (428, 926, .light, "default", "p3-u2-mobile-home-12pm-light"),
            (428, 926, .dark, "default", "p3-u2-mobile-home-12pm-dark"),
            (428, 926, .light, "accessibility", "p3-u2-mobile-home-12pm-large-text")
        ]

        for (width, height, style, textSize, name) in appearances {
            let webView = try loadFixture(width: width, height: height, interfaceStyle: style)
            try run("document.documentElement.dataset.mobileTheme = '\(style == .dark ? "dark" : "light")'", in: webView)
            try run("document.documentElement.dataset.mobileTextSize = '\(textSize)'", in: webView)
            let background = try stringValue("getComputedStyle(document.body).backgroundColor", in: webView)
            if style == .dark {
                XCTAssertEqual(background, "rgb(16, 18, 20)")
            } else {
                XCTAssertEqual(background, "rgb(232, 235, 239)")
            }
            let captured = expectation(description: "captured \(name)")
            var snapshot: UIImage?
            var snapshotError: Error?
            webView.takeSnapshot(with: nil) { image, error in
                snapshot = image
                snapshotError = error
                captured.fulfill()
            }
            wait(for: [captured], timeout: 5)
            XCTAssertNil(snapshotError)
            let image = try XCTUnwrap(snapshot)
            XCTAssertEqual(image.size.width, width, accuracy: 0.01)
            XCTAssertEqual(image.size.height, height, accuracy: 0.01)
            let attachment = XCTAttachment(image: image)
            attachment.name = name
            attachment.lifetime = .keepAlways
            add(attachment)
        }
    }

    func testRootDoesNotScrollHorizontallyAcrossSupportedPhoneModes() throws {
        let scenarios: [(CGFloat, CGFloat, UIUserInterfaceStyle, String)] = [
            (440, 852, .light, "standard"),
            (440, 852, .dark, "large"),
            (440, 852, .light, "accessibility"),
            (428, 746, .light, "large"),
            (428, 746, .dark, "standard"),
            (428, 746, .dark, "accessibility")
        ]

        for (width, height, style, textSize) in scenarios {
            let webView = try loadFixture(width: width, height: height, interfaceStyle: style)
            try run("document.documentElement.dataset.mobileTextSize = '\(textSize)'", in: webView)
            try run("window.scrollTo(200, 0)", in: webView)

            XCTAssertLessThanOrEqual(
                try numberValue("document.documentElement.scrollWidth", in: webView),
                try numberValue("window.innerWidth", in: webView),
                "document overflow at width \(width), style \(style.rawValue), text \(textSize)"
            )
            XCTAssertLessThanOrEqual(
                try numberValue("document.body.scrollWidth", in: webView),
                try numberValue("window.innerWidth", in: webView),
                "body overflow at width \(width), style \(style.rawValue), text \(textSize)"
            )
            XCTAssertEqual(try numberValue("window.scrollX", in: webView), 0)
            XCTAssertGreaterThan(
                try numberValue("document.querySelector('.asset-pager').scrollWidth", in: webView),
                try numberValue("document.querySelector('.asset-pager').clientWidth", in: webView)
            )
        }
    }

    func testLargeTextContentCanScrollClearOfBottomNavigationOn12ProMax() throws {
        for textSize in ["large", "accessibility"] {
            let webView = try loadFixture(width: 428, height: 746)
            try run("document.documentElement.dataset.mobileTextSize = '\(textSize)'", in: webView)
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

            XCTAssertLessThanOrEqual(markerBottom, navigationTop - 12, "text size: \(textSize)")
            XCTAssertEqual(
                try booleanValue("document.documentElement.scrollWidth > window.innerWidth", in: webView),
                false,
                "text size: \(textSize)"
            )
        }
    }

    func testAssetDetailAnalysisEntryRequiresTheCurrentAuthoritativeIdentity() throws {
        let webView = try loadAssetDetailFixture(selectedSymbol: "BTCUSDT")
        let link = "document.querySelector('[data-analysis-detail-link]')"
        XCTAssertTrue(waitUntil("window.__assetRequests.length === 1", in: webView))

        XCTAssertTrue(try booleanValue("\(link).hidden", in: webView))
        XCTAssertEqual(try stringValue("getComputedStyle(\(link)).display", in: webView), "none")
        XCTAssertEqual(try numberValue("\(link).getBoundingClientRect().height", in: webView), 0)
        XCTAssertEqual(try stringValue("\(link).getAttribute('href') || ''", in: webView), "")

        try run("window.__resolveAsset(0, 'BTCUSDT', 'ana-a')", in: webView)
        XCTAssertTrue(waitUntil("\(link).hidden === false", in: webView))
        XCTAssertEqual(try stringValue("getComputedStyle(\(link)).display", in: webView), "flex")
        XCTAssertTrue(try booleanValue(
            "new URL(\(link).href).searchParams.get('analysisId') === 'ana-a'",
            in: webView
        ))
        XCTAssertTrue(try booleanValue(
            "new URL(\(link).href).searchParams.get('selectedSymbol') === 'BTCUSDT'",
            in: webView
        ))

        try run("window.__startAsset('BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__assetRequests.length === 2", in: webView))
        XCTAssertTrue(try booleanValue("\(link).hidden", in: webView))
        XCTAssertEqual(try stringValue("getComputedStyle(\(link)).display", in: webView), "none")
        XCTAssertEqual(try stringValue("\(link).getAttribute('href') || ''", in: webView), "")

        try run("window.__startAsset('ETHUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__assetRequests.length === 3", in: webView))
        XCTAssertTrue(waitUntil("window.__assetRequests[1].aborted === true", in: webView))
        try run("window.__resolveAsset(1, 'BTCUSDT', 'ana-stale')", in: webView)
        XCTAssertTrue(waitUntil("window.__assetRequests[1].settled === true", in: webView))
        XCTAssertTrue(try booleanValue("\(link).hidden", in: webView))
        XCTAssertEqual(try stringValue("\(link).getAttribute('href') || ''", in: webView), "")

        try run("window.__resolveAsset(2, 'ETHUSDT', 'ana-b')", in: webView)
        XCTAssertTrue(waitUntil("\(link).hidden === false", in: webView))
        XCTAssertTrue(try booleanValue(
            "new URL(\(link).href).searchParams.get('analysisId') === 'ana-b'",
            in: webView
        ))

        try run("window.__startAsset('SOLUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__assetRequests.length === 4", in: webView))
        try run("window.__resolveAsset(3, 'SOLUSDT', null)", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-page-status]').dataset.status === 'ready'",
            in: webView
        ))
        XCTAssertTrue(try booleanValue("\(link).hidden", in: webView))
        XCTAssertEqual(try stringValue("getComputedStyle(\(link)).display", in: webView), "none")
        XCTAssertEqual(try stringValue("\(link).getAttribute('href') || ''", in: webView), "")

        try run("window.__startAsset('DOGEUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__assetRequests.length === 5", in: webView))
        try run("window.__rejectAsset(4)", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-page-status]').dataset.status === 'error'",
            in: webView
        ))
        XCTAssertTrue(try booleanValue("\(link).hidden", in: webView))
        XCTAssertEqual(try stringValue("getComputedStyle(\(link)).display", in: webView), "none")
        XCTAssertEqual(try stringValue("\(link).getAttribute('href') || ''", in: webView), "")
    }

    func testAnalysisDetailNewestResponseWinsAfterRetryStartsAnotherIdentity() throws {
        let webView = try loadAnalysisDetailFixture(analysisId: "ana-old")
        XCTAssertTrue(waitUntil("window.__analysisRequests.length === 1", in: webView))

        try run("window.__startAnalysis('ana-new')", in: webView)
        XCTAssertTrue(waitUntil("window.__analysisRequests.length === 2", in: webView))
        XCTAssertTrue(waitUntil("window.__analysisRequests[0].aborted === true", in: webView))
        XCTAssertTrue(try booleanValue(
            "window.__analysisRequests[1].url.endsWith('/ana-new')",
            in: webView
        ))

        try run("window.__resolveAnalysis(1, 'ana-new', 'NEW_DIRECTION')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-analysis-field=\"direction\"]').textContent === 'NEW_DIRECTION'",
            in: webView
        ))

        try run("""
            window.__resolveAnalysis(0, 'ana-old', 'OLD_DIRECTION');
            setTimeout(function() { window.__oldSuccessSettled = true; }, 0);
            """, in: webView)
        XCTAssertTrue(waitUntil("window.__oldSuccessSettled === true", in: webView))
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-analysis-field=\"direction\"]').textContent", in: webView),
            "NEW_DIRECTION"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-analysis-detail-root]').dataset.pageState", in: webView),
            "PARTIAL_DATA"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-analysis-detail-root]').getAttribute('aria-busy')", in: webView),
            "false"
        )
    }

    func testAnalysisDetailStaleFailureAndFinallyCannotReplacePendingRequestState() throws {
        let webView = try loadAnalysisDetailFixture(analysisId: "ana-old")
        XCTAssertTrue(waitUntil("window.__analysisRequests.length === 1", in: webView))

        try run("window.__startAnalysis('ana-new')", in: webView)
        XCTAssertTrue(waitUntil("window.__analysisRequests.length === 2", in: webView))
        try run("""
            window.__rejectAnalysis(0);
            setTimeout(function() { window.__oldFailureSettled = true; }, 0);
            """, in: webView)
        XCTAssertTrue(waitUntil("window.__oldFailureSettled === true", in: webView))

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-analysis-detail-root]').dataset.pageState", in: webView),
            "LOADING"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-analysis-detail-root]').getAttribute('aria-busy')", in: webView),
            "true"
        )
        XCTAssertTrue(try booleanValue("document.querySelector('[data-analysis-content]').hidden", in: webView))

        try run("window.__resolveAnalysis(1, 'ana-new', 'NEW_DIRECTION')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-analysis-field=\"direction\"]').textContent === 'NEW_DIRECTION'",
            in: webView
        ))
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-analysis-detail-root]').getAttribute('aria-busy')", in: webView),
            "false"
        )
    }

    func testAnalysisDetailDynamicTypeAndFrozenFirstViewportGeometry() throws {
        let webView = try loadAnalysisDetailFixture(analysisId: "ana-current")
        XCTAssertTrue(waitUntil("window.__analysisRequests.length === 1", in: webView))
        try run("window.__resolveAnalysis(0, 'ana-current', 'CURRENT_DIRECTION')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-analysis-detail-root]').dataset.pageState === 'PARTIAL_DATA'",
            in: webView
        ))

        XCTAssertEqual(
            try numberValue("parseFloat(getComputedStyle(document.querySelector('.title-stack h1')).fontSize)", in: webView),
            20,
            accuracy: 0.01
        )
        XCTAssertEqual(
            try numberValue("document.querySelector('.analysis-navigation').getBoundingClientRect().top", in: webView),
            22,
            accuracy: 0.5
        )
        XCTAssertEqual(
            try numberValue("document.querySelector('.analysis-navigation').getBoundingClientRect().height", in: webView),
            48,
            accuracy: 0.5
        )
        XCTAssertEqual(
            try numberValue("document.querySelector('.asset-context').getBoundingClientRect().top", in: webView),
            90,
            accuracy: 0.5
        )
        XCTAssertEqual(
            try numberValue("document.querySelector('.asset-context').getBoundingClientRect().height", in: webView),
            172,
            accuracy: 1
        )
        XCTAssertEqual(
            try numberValue("document.querySelector('.market-judgment').getBoundingClientRect().top", in: webView),
            282,
            accuracy: 1
        )
        XCTAssertEqual(try numberValue("document.querySelectorAll('.run-meta, .sync-status').length", in: webView), 0)

        let defaultBody = try numberValue("parseFloat(getComputedStyle(document.body).fontSize)", in: webView)
        let defaultTab = try numberValue(
            "parseFloat(getComputedStyle(document.querySelector('[data-role-tab]')).fontSize)",
            in: webView
        )
        try run("document.documentElement.dataset.mobileTextSize = 'accessibility'", in: webView)

        XCTAssertEqual(defaultBody, 16, accuracy: 0.01)
        XCTAssertEqual(
            try numberValue("parseFloat(getComputedStyle(document.body).fontSize)", in: webView),
            20.8,
            accuracy: 0.01
        )
        XCTAssertGreaterThan(
            try numberValue("parseFloat(getComputedStyle(document.querySelector('.title-stack h1')).fontSize)", in: webView),
            20
        )
        XCTAssertGreaterThan(
            try numberValue("parseFloat(getComputedStyle(document.querySelector('[data-role-tab]')).fontSize)", in: webView),
            defaultTab
        )
        XCTAssertFalse(try booleanValue("document.documentElement.scrollWidth > window.innerWidth", in: webView))
        XCTAssertFalse(try booleanValue("document.body.scrollWidth > window.innerWidth", in: webView))
        XCTAssertEqual(
            try numberValue("""
                Array.from(document.querySelectorAll('button,a'))
                  .filter(node => {
                    const rect = node.getBoundingClientRect();
                    const style = getComputedStyle(node);
                    return rect.width > 0 && rect.height > 0
                      && style.display !== 'none' && style.visibility !== 'hidden'
                      && (rect.width < 44 || rect.height < 44);
                  }).length
                """, in: webView),
            0
        )
    }

    func testPositionMonitoringRequiresExplicitSelectionBeforeExactReads() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        XCTAssertEqual(
            try stringValue("window.__positionRequests[0].url", in: webView),
            "/api/dashboard/home?limit=20"
        )
        try run("window.__resolvePositionHome(0)", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-page-status]').textContent === '等待选择'",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(try numberValue("window.__positionRequests.length", in: webView), 1)
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-selection-state] strong').textContent", in: webView),
            "请选择具体持仓"
        )
        XCTAssertTrue(try booleanValue("document.querySelector('[data-selected-position]').hidden", in: webView))
        XCTAssertEqual(
            try numberValue("document.querySelectorAll('[data-position-list] [data-position-id]').length", in: webView),
            2
        )
    }

    func testPositionMonitoringBindsDetailAndLogsToTheSameExactPositionId() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "42")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        XCTAssertEqual(
            try stringValue("window.__positionRequests[0].url", in: webView),
            "/api/dashboard/home?limit=20&positionId=42"
        )
        try run("window.__resolvePositionHome(0)", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        XCTAssertEqual(
            try stringValue("window.__positionRequests[1].url", in: webView),
            "/api/user-positions/42"
        )

        try run("window.__resolvePositionDetail(1, '42', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 3", in: webView, timeout: 2))
        XCTAssertEqual(
            try stringValue("window.__positionRequests[2].url", in: webView),
            "/api/review/positions/42/monitor-logs?limit=20"
        )
        try run("window.__resolvePositionLogs(2, '42')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-monitor-log-count]').textContent === '1 条'",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-selected-position-id]').textContent", in: webView),
            "positionId · 42"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-field=\"symbol\"]').textContent", in: webView),
            "BTCUSDT"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-field=\"entryPrice\"]').textContent", in: webView),
            "66000"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-monitor-field=\"monitorStatus\"]').textContent", in: webView),
            "LOGIC_VALID"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-monitor-log-list] strong').textContent", in: webView),
            "LOGIC_VALID"
        )
        XCTAssertTrue(try booleanValue(
            "window.__positionRequests.every(request => request.options.method === 'GET')",
            in: webView
        ))
    }

    func testPositionMonitoringPreservesLargePositionIdAsString() throws {
        let positionId = "9007199254740993"
        let webView = try loadPositionMonitoringFixture(positionId: positionId)

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        XCTAssertEqual(
            try stringValue("window.__positionRequests[0].url", in: webView),
            "/api/dashboard/home?limit=20&positionId=\(positionId)"
        )
        try run(
            "window.__resolvePositionHome(0, { positionId: '\(positionId)' })",
            in: webView
        )
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        XCTAssertEqual(
            try stringValue("window.__positionRequests[1].url", in: webView),
            "/api/user-positions/\(positionId)"
        )

        try run("window.__resolvePositionDetail(1, '\(positionId)', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 3", in: webView, timeout: 2))
        XCTAssertEqual(
            try stringValue("window.__positionRequests[2].url", in: webView),
            "/api/review/positions/\(positionId)/monitor-logs?limit=20"
        )
        try run("window.__resolvePositionLogs(2, '\(positionId)')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-monitor-log-count]').textContent === '1 条'",
            in: webView,
            timeout: 2
        ))
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-selected-position-id]').textContent", in: webView),
            "positionId · \(positionId)"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-list] .active').dataset.positionId", in: webView),
            positionId
        )
    }

    func testPositionMonitoringLogFailureDoesNotClaimWaitingMonitor() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "42")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        try run("window.__resolvePositionHome(0, { waiting: true })", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        try run("window.__resolvePositionDetail(1, '42', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 3", in: webView, timeout: 2))
        try run("window.__rejectPositionRequest(2)", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-page-status]').dataset.status === 'partial'",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-monitor-field=\"monitorStatus\"]').textContent", in: webView),
            "当前不可查看"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-monitor-card]').dataset.monitorStatus", in: webView),
            "MONITOR_DATA_UNAVAILABLE"
        )
        XCTAssertFalse(try booleanValue(
            "document.querySelector('[data-monitor-field=\"monitorStatus\"]').textContent === 'WAITING_MONITOR'",
            in: webView
        ))
    }

    func testPositionMonitoringEmptyLogReadConfirmsWaitingMonitor() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "42")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        try run("window.__resolvePositionHome(0, { waiting: true })", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        try run("window.__resolvePositionDetail(1, '42', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 3", in: webView, timeout: 2))
        try run("window.__resolvePositionLogsEmpty(2)", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-monitor-field=\"monitorStatus\"]').textContent === 'WAITING_MONITOR'",
            in: webView,
            timeout: 2
        ))
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-page-status]').dataset.status", in: webView),
            "ready"
        )
    }

    func testPositionMonitoringExistingLogsDoNotConfirmAWaitingSummary() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "42")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        try run("window.__resolvePositionHome(0, { waiting: true })", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        try run("window.__resolvePositionDetail(1, '42', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 3", in: webView, timeout: 2))
        try run("window.__resolvePositionLogs(2, '42')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-monitor-log-count]').textContent === '1 条'",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(
            try stringValue("document.querySelector('[data-monitor-log-list] strong').textContent", in: webView),
            "LOGIC_VALID"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-monitor-field=\"monitorStatus\"]').textContent", in: webView),
            "当前不可查看"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-page-status]').dataset.status", in: webView),
            "partial"
        )
    }

    func testPositionMonitoringRejectsCrossPositionLogsWithoutRenderingThem() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "42")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        try run("window.__resolvePositionHome(0)", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        try run("window.__resolvePositionDetail(1, '42', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 3", in: webView, timeout: 2))
        try run("window.__resolvePositionLogs(2, '99')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-monitor-log-error]').hidden === false",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(
            try numberValue("document.querySelector('[data-monitor-log-list]').children.length", in: webView),
            0
        )
        XCTAssertFalse(try booleanValue("document.body.textContent.includes('LOG_99')", in: webView))
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-page-status]').textContent", in: webView),
            "部分数据可用"
        )
    }

    func testPositionMonitoringRejectsMismatchedPositionDetailBeforeReadingLogs() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "42")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        try run("window.__resolvePositionHome(0)", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        try run("window.__resolvePositionDetail(1, '43', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-page-state-title]').textContent === '持仓读取失败'",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(try numberValue("window.__positionRequests.length", in: webView), 2)
        XCTAssertTrue(try booleanValue("document.querySelector('[data-selected-position]').hidden", in: webView))
        XCTAssertTrue(try booleanValue("document.querySelector('[data-position-content]').hidden", in: webView))
    }

    func testPositionMonitoringLoadFailureRetriesTheSameIdentityAndRemainsAccessible() throws {
        let webView = try loadPositionMonitoringFixture(positionId: "42")

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        try run("window.__rejectPositionRequest(0)", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-position-retry]').hidden === false",
            in: webView,
            timeout: 2
        ))
        try run("document.querySelector('[data-position-retry]').click()", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))

        XCTAssertEqual(
            try stringValue("window.__positionRequests[1].url", in: webView),
            "/api/dashboard/home?limit=20&positionId=42"
        )
        XCTAssertFalse(try booleanValue("document.documentElement.scrollWidth > window.innerWidth", in: webView))
        XCTAssertFalse(try booleanValue("document.body.scrollWidth > window.innerWidth", in: webView))
        XCTAssertEqual(
            try numberValue("""
                Array.from(document.querySelectorAll('button,a[href]'))
                  .filter(node => {
                    const rect = node.getBoundingClientRect();
                    const style = getComputedStyle(node);
                    return rect.width > 0 && rect.height > 0
                      && style.display !== 'none' && style.visibility !== 'hidden'
                      && rect.height < 44;
                  }).length
                """, in: webView),
            0
        )
    }

    func testPositionMonitoringDesktopProjectionKeepsTheSameReadonlyContract() throws {
        let webView = try loadPositionMonitoringFixture(
            positionId: "42",
            width: 1440,
            height: 900
        )

        XCTAssertTrue(waitUntil("window.__positionRequests.length === 1", in: webView, timeout: 2))
        try run("window.__resolvePositionHome(0)", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 2", in: webView, timeout: 2))
        try run("window.__resolvePositionDetail(1, '42', 'BTCUSDT')", in: webView)
        XCTAssertTrue(waitUntil("window.__positionRequests.length === 3", in: webView, timeout: 2))
        try run("window.__resolvePositionLogs(2, '42')", in: webView)
        XCTAssertTrue(waitUntil(
            "document.querySelector('[data-monitor-log-count]').textContent === '1 条'",
            in: webView,
            timeout: 2
        ))

        XCTAssertEqual(
            try stringValue("getComputedStyle(document.querySelector('.desktop-sidebar')).display", in: webView),
            "flex"
        )
        XCTAssertEqual(
            try stringValue("getComputedStyle(document.querySelector('.mobile-navigation')).display", in: webView),
            "none"
        )
        XCTAssertFalse(try booleanValue(
            "document.querySelector('[data-selected-position]').hidden",
            in: webView
        ))
        XCTAssertEqual(
            try stringValue("getComputedStyle(document.querySelector('.monitor-layout')).display", in: webView),
            "grid"
        )
        let monitorColumns = try stringValue(
            "getComputedStyle(document.querySelector('.monitor-layout')).gridTemplateColumns",
            in: webView
        )
        XCTAssertTrue(monitorColumns.contains("800px"), monitorColumns)
        XCTAssertTrue(monitorColumns.contains("280px"), monitorColumns)
        XCTAssertEqual(
            try stringValue("document.querySelector('.desktop-navigation .active').getAttribute('href')", in: webView),
            "/dashboard/positions"
        )
        XCTAssertEqual(
            try stringValue("document.querySelector('[data-position-list] [data-position-id=\"42\"]').getAttribute('href')", in: webView),
            "/dashboard/positions?positionId=42"
        )
        XCTAssertFalse(try booleanValue("document.documentElement.scrollWidth > window.innerWidth", in: webView))
    }

    private func loadPositionMonitoringFixture(
        positionId: String,
        width: CGFloat = 430,
        height: CGFloat = 932
    ) throws -> WKWebView {
        let loaded = expectation(description: "position monitoring fixture loaded")
        let delegate = NavigationDelegate { loaded.fulfill() }
        navigationDelegate = delegate
        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        webView.navigationDelegate = delegate
        webView.loadHTMLString(
            try positionMonitoringFixtureHTML(positionId: positionId, mobileView: width < 760),
            baseURL: URL(string: "https://app.example.test/dashboard/mobile/positions")
        )
        wait(for: [loaded], timeout: 5)
        XCTAssertTrue(waitUntil("document.readyState === 'complete'", in: webView))
        return webView
    }

    private func positionMonitoringFixtureHTML(positionId: String, mobileView: Bool) throws -> String {
        let repositoryRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let templateURL = repositoryRoot
            .appendingPathComponent("src/main/resources/templates/position-monitoring.html")
        let styleURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/css/position-monitoring.css")
        let contractURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/js/frontend-contract.js")
        let scriptURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/js/position-monitoring.js")
        guard FileManager.default.fileExists(atPath: templateURL.path),
              FileManager.default.fileExists(atPath: styleURL.path),
              FileManager.default.fileExists(atPath: contractURL.path),
              FileManager.default.fileExists(atPath: scriptURL.path) else {
            throw FixtureError.missingProductionResource
        }

        var html = try String(contentsOf: templateURL)
        let styles = try String(contentsOf: styleURL)
        let contractScript = try String(contentsOf: contractURL)
        let positionScript = try String(contentsOf: scriptURL)
        let requestProbe = """
        <script>
          window.__positionRequests = [];
          window.fetch = function(url, options) {
            return new Promise(function(resolve, reject) {
              var request = {
                url: String(url),
                options: options || {},
                resolve: resolve,
                reject: reject,
                aborted: false
              };
              if (request.options.signal) {
                request.options.signal.addEventListener('abort', function() {
                  request.aborted = true;
                  reject(new DOMException('Aborted', 'AbortError'));
                }, { once: true });
              }
              window.__positionRequests.push(request);
            });
          };
          window.__resolvePositionHome = function(index, options) {
            options = options || {};
            var primaryPositionId = String(options.positionId || '42');
            var waiting = options.waiting === true;
            window.__positionRequests[index].resolve({
              ok: true,
              status: 200,
              json: function() {
                return Promise.resolve({
                  code: 200,
                  data: {
                    positions: [
                      {
                        positionId: primaryPositionId,
                        symbol: 'BTCUSDT',
                        direction: 'LONG',
                        directionLabel: '多',
                        positionStatus: 'OPEN',
                        entryLogicStatus: waiting ? 'WAITING_MONITOR' : 'LOGIC_VALID',
                        entryLogicStatusLabel: waiting ? '等待首次监控' : '逻辑仍成立',
                        directionSupportStatusLabel: waiting ? '等待首次监控' : '仍支持原方向',
                        reversalStatusLabel: waiting ? '等待首次监控' : '未反转',
                        riskLevelLabel: waiting ? '等待首次监控' : '中',
                        monitorConclusion: waiting ? null : '当前逻辑仍成立',
                        suggestedManualActionText: waiting ? '等待首次监控' : '继续人工监控',
                        lastMonitorAt: waiting ? null : '2026-07-28T10:00:00'
                      },
                      {
                        positionId: '43',
                        symbol: 'BTCUSDT',
                        direction: 'SHORT',
                        directionLabel: '空',
                        positionStatus: 'PARTIALLY_CLOSED',
                        entryLogicStatus: 'HIGH_RISK',
                        lastMonitorAt: '2026-07-28T10:05:00'
                      }
                    ]
                  }
                });
              }
            });
          };
          window.__resolvePositionDetail = function(index, id, symbol) {
            window.__positionRequests[index].resolve({
              ok: true,
              status: 200,
              json: function() {
                return Promise.resolve({
                  code: 200,
                  data: {
                    id: String(id),
                    assetSymbol: symbol,
                    side: 'LONG',
                    status: 'OPEN',
                    entryPrice: 66000,
                    quantity: 0.25,
                    leverage: 2,
                    stopLoss: 64000,
                    takeProfit: 70000,
                    openedAt: '2026-07-28T09:00:00'
                  }
                });
              }
            });
          };
          window.__resolvePositionLogs = function(index, id) {
            window.__positionRequests[index].resolve({
              ok: true,
              status: 200,
              json: function() {
                return Promise.resolve({
                  code: 200,
                  data: [{
                    positionId: String(id),
                    logicStatus: 'LOGIC_VALID',
                    reason: 'LOG_' + id,
                    riskLevel: 'MEDIUM',
                    suggestedAction: 'HOLD_REVIEW',
                    sourceStatusLabel: '来源已验证',
                    createdAt: '2026-07-28T10:00:00'
                  }]
                });
              }
            });
          };
          window.__resolvePositionLogsEmpty = function(index) {
            window.__positionRequests[index].resolve({
              ok: true,
              status: 200,
              json: function() {
                return Promise.resolve({
                  code: 200,
                  data: []
                });
              }
            });
          };
          window.__rejectPositionRequest = function(index) {
            window.__positionRequests[index].reject(new Error('POSITION_FAILURE'));
          };
          var positionRoot = document.querySelector('[data-position-monitor-root]');
          positionRoot.dataset.requestedPositionId = '\(positionId)';
          positionRoot.dataset.invalidPositionId = 'false';
          positionRoot.dataset.mobileView = '\(mobileView)';
        </script>
        """
        html = html.replacingOccurrences(
            of: "<link rel=\"stylesheet\" th:href=\"@{/css/position-monitoring.css}\" href=\"/css/position-monitoring.css\">",
            with: "<style>\(styles)</style>"
        )
        html = html.replacingOccurrences(
            of: "<script th:src=\"@{/js/frontend-contract.js}\" src=\"/js/frontend-contract.js\" defer></script>",
            with: ""
        )
        html = html.replacingOccurrences(
            of: "<script th:src=\"@{/js/position-monitoring.js}\" src=\"/js/position-monitoring.js\" defer></script>",
            with: ""
        )
        html = html.replacingOccurrences(
            of: "</body>",
            with: "<script>\(contractScript)</script>\(requestProbe)<script>\(positionScript)</script></body>"
        )
        return html
    }

    private func loadAssetDetailFixture(
        selectedSymbol: String,
        width: CGFloat = 430,
        height: CGFloat = 932
    ) throws -> WKWebView {
        let loaded = expectation(description: "asset detail fixture loaded")
        let delegate = NavigationDelegate { loaded.fulfill() }
        navigationDelegate = delegate
        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        webView.navigationDelegate = delegate
        webView.loadHTMLString(
            try assetDetailFixtureHTML(selectedSymbol: selectedSymbol),
            baseURL: URL(string: "https://app.example.test/dashboard/asset-detail")
        )
        wait(for: [loaded], timeout: 5)
        XCTAssertTrue(waitUntil("document.readyState === 'complete'", in: webView))
        return webView
    }

    private func assetDetailFixtureHTML(selectedSymbol: String) throws -> String {
        let repositoryRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let templateURL = repositoryRoot
            .appendingPathComponent("src/main/resources/templates/asset-detail.html")
        let styleURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/css/asset-detail.css")
        let contractURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/js/frontend-contract.js")
        let scriptURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/js/asset-detail.js")
        guard FileManager.default.fileExists(atPath: templateURL.path),
              FileManager.default.fileExists(atPath: styleURL.path),
              FileManager.default.fileExists(atPath: contractURL.path),
              FileManager.default.fileExists(atPath: scriptURL.path) else {
            throw FixtureError.missingProductionResource
        }

        var html = try String(contentsOf: templateURL)
        let styles = try String(contentsOf: styleURL)
        let contractScript = try String(contentsOf: contractURL)
        let detailScript = try String(contentsOf: scriptURL)
        let requestProbe = """
        <script>
          window.__assetRequests = [];
          window.fetch = function(url, options) {
            return new Promise(function(resolve, reject) {
              var request = {
                url: String(url),
                options: options || {},
                resolve: resolve,
                reject: reject,
                aborted: false,
                settled: false
              };
              if (request.options.signal) {
                request.options.signal.addEventListener('abort', function() {
                  request.aborted = true;
                }, { once: true });
              }
              window.__assetRequests.push(request);
            });
          };
          window.__startAsset = function(nextSymbol) {
            document.querySelector('[data-asset-detail-root]').dataset.selectedSymbol = nextSymbol;
            document.querySelector('[data-request-retry]').click();
          };
          window.__resolveAsset = function(index, responseSymbol, analysisId) {
            var request = window.__assetRequests[index];
            request.resolve({
              ok: true,
              status: 200,
              json: function() {
                return Promise.resolve({
                  code: 200,
                  data: {
                    selectedSymbol: responseSymbol,
                    assets: [{
                      slotType: 'DECISION',
                      rawSymbol: responseSymbol,
                      symbol: responseSymbol,
                      analysisId: analysisId,
                      currentConclusion: 'CONCLUSION_' + responseSymbol
                    }],
                    aiDecision: null,
                    executionSuggestion: null
                  }
                });
              }
            });
            setTimeout(function() { request.settled = true; }, 0);
          };
          window.__rejectAsset = function(index) {
            var request = window.__assetRequests[index];
            request.reject(new Error('ASSET_FAILURE'));
            setTimeout(function() { request.settled = true; }, 0);
          };
          var assetRoot = document.querySelector('[data-asset-detail-root]');
          assetRoot.dataset.selectedSymbol = '\(selectedSymbol)';
          assetRoot.dataset.mobileView = 'true';
        </script>
        """
        html = html.replacingOccurrences(
            of: "<link rel=\"stylesheet\" th:href=\"@{/css/asset-detail.css}\" href=\"/css/asset-detail.css\">",
            with: "<style>\(styles)</style>"
        )
        html = html.replacingOccurrences(
            of: "<script th:src=\"@{/js/frontend-contract.js}\" src=\"/js/frontend-contract.js\" defer></script>",
            with: ""
        )
        html = html.replacingOccurrences(
            of: "<script th:src=\"@{/js/asset-detail.js}\" src=\"/js/asset-detail.js\" defer></script>",
            with: ""
        )
        html = html.replacingOccurrences(
            of: "</body>",
            with: "<script>\(contractScript)</script>\(requestProbe)<script>\(detailScript)</script></body>"
        )
        return html
    }

    private func loadAnalysisDetailFixture(
        analysisId: String,
        width: CGFloat = 430,
        height: CGFloat = 932
    ) throws -> WKWebView {
        let loaded = expectation(description: "analysis detail fixture loaded")
        let delegate = NavigationDelegate { loaded.fulfill() }
        navigationDelegate = delegate
        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        webView.navigationDelegate = delegate
        webView.loadHTMLString(
            try analysisDetailFixtureHTML(analysisId: analysisId),
            baseURL: URL(string: "https://app.example.test/dashboard/analysis-detail")
        )
        wait(for: [loaded], timeout: 5)
        XCTAssertTrue(waitUntil("document.readyState === 'complete'", in: webView))
        return webView
    }

    private func analysisDetailFixtureHTML(analysisId: String) throws -> String {
        let repositoryRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let templateURL = repositoryRoot
            .appendingPathComponent("src/main/resources/templates/analysis-detail.html")
        let styleURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/css/analysis-detail.css")
        let contractURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/js/frontend-contract.js")
        let scriptURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/js/analysis-detail.js")
        guard FileManager.default.fileExists(atPath: templateURL.path),
              FileManager.default.fileExists(atPath: styleURL.path),
              FileManager.default.fileExists(atPath: contractURL.path),
              FileManager.default.fileExists(atPath: scriptURL.path) else {
            throw FixtureError.missingProductionResource
        }

        var html = try String(contentsOf: templateURL)
        let styles = try String(contentsOf: styleURL)
        let contractScript = try String(contentsOf: contractURL)
        let detailScript = try String(contentsOf: scriptURL)
        let requestProbe = """
        <script>
          window.__analysisRequests = [];
          window.fetch = function(url, options) {
            return new Promise(function(resolve, reject) {
              var request = {
                url: String(url),
                options: options || {},
                resolve: resolve,
                reject: reject,
                aborted: false
              };
              if (request.options.signal) {
                request.options.signal.addEventListener('abort', function() {
                  request.aborted = true;
                }, { once: true });
              }
              window.__analysisRequests.push(request);
            });
          };
          window.__startAnalysis = function(nextAnalysisId) {
            document.querySelector('[data-analysis-detail-root]').dataset.analysisId = nextAnalysisId;
            document.querySelector('[data-request-retry]').click();
          };
          window.__resolveAnalysis = function(index, responseAnalysisId, direction) {
            var request = window.__analysisRequests[index];
            request.resolve({
              ok: true,
              status: 200,
              json: function() {
                return Promise.resolve({
                  code: 200,
                  data: {
                    run: {
                      analysisId: responseAnalysisId,
                      symbol: 'BTCUSDT',
                      status: 'SUCCESS'
                    },
                    decision: {
                      marketBiasHierarchy: direction,
                      confidenceLevel: 'HIGH',
                      conclusionSummary: 'SUMMARY_' + responseAnalysisId,
                      multiTfConvergence: 'STRONG',
                      aiConflictLevel: 'LOW'
                    },
                    marketEnvironment: {
                      environmentType: 'TREND',
                      riskMode: 'BALANCED'
                    },
                    evidenceTopItems: [],
                    scoreTopItems: []
                  }
                });
              }
            });
          };
          window.__rejectAnalysis = function(index) {
            window.__analysisRequests[index].reject(new Error('STALE_FAILURE'));
          };
          var analysisRoot = document.querySelector('[data-analysis-detail-root]');
          analysisRoot.dataset.analysisId = '\(analysisId)';
          analysisRoot.dataset.selectedSymbol = 'BTCUSDT';
          analysisRoot.dataset.mobileView = 'true';
        </script>
        """
        html = html.replacingOccurrences(
            of: "<link rel=\"stylesheet\" th:href=\"@{/css/analysis-detail.css}\" href=\"/css/analysis-detail.css\">",
            with: "<style>\(styles)</style>"
        )
        html = html.replacingOccurrences(
            of: "<script th:src=\"@{/js/frontend-contract.js}\" src=\"/js/frontend-contract.js\" defer></script>",
            with: ""
        )
        html = html.replacingOccurrences(
            of: "<script th:src=\"@{/js/analysis-detail.js}\" src=\"/js/analysis-detail.js\" defer></script>",
            with: ""
        )
        html = html.replacingOccurrences(
            of: "</body>",
            with: "<script>\(contractScript)</script>\(requestProbe)<script>\(detailScript)</script></body>"
        )
        return html
    }

    private func loadFixture(
        width: CGFloat = 440,
        height: CGFloat = 852,
        interfaceStyle: UIUserInterfaceStyle = .light,
        selectedSymbol: String = "BTCUSDT"
    ) throws -> WKWebView {
        let loaded = expectation(description: "mobile fixture loaded")
        let delegate = NavigationDelegate { loaded.fulfill() }
        navigationDelegate = delegate
        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        webView.overrideUserInterfaceStyle = interfaceStyle
        webView.navigationDelegate = delegate
        webView.loadHTMLString(
            try fixtureHTML(selectedSymbol: selectedSymbol),
            baseURL: URL(string: "https://app.example.test/dashboard/mobile")
        )
        wait(for: [loaded], timeout: 5)
        XCTAssertTrue(waitUntil("document.readyState === 'complete'", in: webView))
        return webView
    }

    private func loadDesktopTemplate() throws -> WKWebView {
        let loaded = expectation(description: "desktop template loaded")
        let delegate = NavigationDelegate { loaded.fulfill() }
        navigationDelegate = delegate
        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: 1440, height: 900))
        webView.navigationDelegate = delegate
        webView.loadHTMLString(
            try desktopTemplateHTML(),
            baseURL: URL(string: "https://app.example.test/dashboard")
        )
        wait(for: [loaded], timeout: 5)
        XCTAssertTrue(waitUntil("document.readyState === 'complete'", in: webView))
        return webView
    }

    private func desktopTemplateHTML() throws -> String {
        let repositoryRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let templateURL = repositoryRoot
            .appendingPathComponent("src/main/resources/templates/dashboard.html")
        let contractURL = repositoryRoot
            .appendingPathComponent("src/main/resources/static/js/frontend-contract.js")
        guard FileManager.default.fileExists(atPath: templateURL.path),
              FileManager.default.fileExists(atPath: contractURL.path) else {
            throw FixtureError.missingProductionResource
        }

        var html = try String(contentsOf: templateURL)
        let contractScript = try String(contentsOf: contractURL)
        let requestProbe = """
        <script>
          window.__overviewRequests = [];
          window.setInterval = function() { return 0; };
          window.clearInterval = function() {};
          window.fetch = function(url, options) {
            return new Promise(function(resolve, reject) {
              var request = {
                url: String(url),
                options: options || {},
                resolve: resolve,
                reject: reject,
                aborted: false
              };
              if (request.options.signal) {
                request.options.signal.addEventListener('abort', function() {
                  request.aborted = true;
                  reject(new DOMException('Aborted', 'AbortError'));
                }, { once: true });
              }
              window.__overviewRequests.push(request);
            });
          };
        </script>
        """
        html = html.replacingOccurrences(
            of: "<script th:src=\"@{/js/frontend-contract.js}\" src=\"/js/frontend-contract.js\"></script>",
            with: "<script>\(contractScript)</script>\(requestProbe)"
        )
        html = html.replacingOccurrences(
            of: "<script src=\"/js/alert-explain.js\"></script>",
            with: ""
        )
        return html
    }

    private func fixtureHTML(selectedSymbol: String) throws -> String {
        let bundle = Bundle(for: DashboardMobileDomInteractionTests.self)
        guard let scriptURL = bundle.url(forResource: "dashboard-mobile", withExtension: "js"),
              let styleURL = bundle.url(forResource: "dashboard-mobile", withExtension: "css") else {
            throw FixtureError.missingProductionResource
        }
        let script = try String(contentsOf: scriptURL)
        let styles = try String(contentsOf: styleURL)
        let contractURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("src/main/resources/static/js/frontend-contract.js")
        guard FileManager.default.fileExists(atPath: contractURL.path) else {
            throw FixtureError.missingProductionResource
        }
        let contractScript = try String(contentsOf: contractURL)
        let btcSelected = selectedSymbol == "BTCUSDT"
        let ethSelected = selectedSymbol == "ETHUSDT"
        let solSelected = selectedSymbol == "SOLUSDT"
        return """
        <!doctype html>
        <html lang="zh-CN">
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>\(styles)</style>
          <script>\(contractScript)</script>
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
            window.__resolveDashboard = function(index, symbol, verified, analysisAvailable) {
              var request = window.__pendingRequests[index];
              var data = {
                selectedSymbol: symbol,
                assets: [{
                  symbol: symbol,
                  rawSymbol: symbol,
                  analysisId: analysisAvailable === false ? null : 'ANA_' + symbol,
                  worthOpening: true
                }],
                executionSuggestion: {
                  status: 'USABLE_REVIEW_PLAN',
                  statusLabel: 'READY_' + symbol,
                  blockedReason: 'RISK_' + symbol,
                  sourceExecutionPlanId: verified === false ? null : 'PLAN_' + symbol,
                  direction: 'DIR_' + symbol,
                  entryZone: 'ENTRY_' + symbol,
                  stopLoss: 'STOP_' + symbol,
                  takeProfitRules: 'TP_' + symbol,
                  leverageSuggestion: 'LEV_' + symbol,
                  positionSuggestion: 'POS_' + symbol,
                  invalidCondition: 'INVALID_' + symbol,
                  validFrom: 'FROM_' + symbol,
                  expiresAt: 'UNTIL_' + symbol
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
                    { role: 'GPT_FINAL', resultAvailable: true, finalConclusion: 'GPT_' + symbol },
                    { role: 'GEMINI_REVIEW', resultAvailable: true, reviewConclusion: 'GEMINI_' + symbol },
                    { role: 'GROK_CHALLENGE', resultAvailable: true, challengeConclusion: 'GROK_' + symbol }
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
          <main class="mobile-home" data-mobile-home-root data-mobile-home-view>
            <header class="mobile-header">
              <div class="product-lockup">
                <p class="product-name">TRADE MODEL V1</p>
                <h1 class="page-heading" id="mobile-page-context" tabindex="-1">首页</h1>
              </div>
              <div class="header-actions">
                <button class="header-action" type="button" data-header-search>搜索</button>
                <button class="header-action" type="button" data-header-alerts-nav>消息</button>
              </div>
            </header>
            <section class="status-section" id="mobile-status">
              <h2 id="mobile-status-title" tabindex="-1">决策状态</h2>
              <dl class="status-grid status-grid-primary">
                <div class="status-cell"><dt>市场趋势</dt><dd>震荡</dd></div>
                <div class="status-cell"><dt>风险等级</dt><dd>中</dd></div>
                <div class="status-cell"><dt>数据质量</dt><dd>82</dd></div>
                <div class="status-cell"><dt>AI 冲突</dt><dd>低</dd></div>
              </dl>
              <div class="system-summary-heading"><h3>系统摘要</h3><span>全局状态</span></div>
              <dl class="status-grid status-grid-system">
                <div class="status-cell"><dt>AI 系统</dt><dd>正常</dd></div>
                <div class="status-cell"><dt>待复核机会</dt><dd>2</dd></div>
                <div class="status-cell"><dt>冲突阻断</dt><dd>否</dd></div>
                <div class="status-cell"><dt>Hot Reset</dt><dd>未触发</dd></div>
              </dl>
            </section>
            <section class="alert-event-section" id="mobile-alerts">
              <div class="alert-event-grid">
                <article class="signal-panel"><h2 id="mobile-alert-title" tabindex="-1">实时告警</h2><p>暂无告警</p></article>
                <article class="signal-panel"><h2>关键事件</h2><p>暂无关键事件</p></article>
              </div>
            </section>
            <section class="watch-section" id="watch-assets">
              <div class="section-heading watch-heading">
                <h2 id="mobile-watch-title" tabindex="-1" aria-label="重点资产监控">重点资产</h2>
                <div class="watch-actions">
                  <button class="watch-tool-button" type="button" data-asset-search-toggle
                          aria-expanded="false" aria-controls="mobile-asset-search">搜索</button>
                  <button class="watch-tool-button" type="button" data-asset-add disabled
                          aria-disabled="true" aria-describedby="watch-add-contract-status">添加</button>
                  <a class="watch-tool-button watch-detail-link" data-asset-detail-link
                     data-enabled-label="分析详情" data-disabled-label="当前不可查看"
                     aria-disabled="true" tabindex="-1">当前不可查看</a>
                </div>
              </div>
              <div class="asset-search-panel" id="mobile-asset-search" hidden>
                <label for="mobile-asset-search-input">搜索当前重点资产</label>
                <input id="mobile-asset-search-input" data-asset-search-input>
                <ul class="asset-search-results" data-asset-search-results></ul>
              </div>
              <p class="watch-action-status" data-watch-action-status></p>
              <p class="watch-contract-note" id="watch-add-contract-status">添加资产暂未开放</p>
              <strong data-selected-asset-token>\(selectedSymbol)</strong>
              <div class="asset-pager" role="radiogroup">
                <button class="asset-card asset-select\(btcSelected ? " is-selected" : "")" data-symbol="BTCUSDT" data-analysis-id="ANA_BTCUSDT" data-direction-label="震荡" data-worth-opening="true" data-asset-state="observing" data-selected="\(btcSelected)" aria-checked="\(btcSelected)" tabindex="\(btcSelected ? 0 : -1)">
                  <span class="asset-card-top"><span class="asset-symbol">BTCUSDT</span><span class="asset-state">观察中</span></span>
                  <span class="asset-price-score"><span><small>当前价格</small><b>66000</b></span><span><small>综合评分</small><b>82</b></span></span>
                  <span class="asset-core-grid"><span><small>方向</small><b>震荡</b></span><span><small>置信度</small><b>中</b></span><span><small>风险等级</small><b>中</b></span></span>
                </button>
                <button class="asset-card asset-select\(ethSelected ? " is-selected" : "")" data-symbol="ETHUSDT" data-analysis-id="ANA_ETHUSDT" data-direction-label="偏多" data-worth-opening="true" data-asset-state="candidate" data-selected="\(ethSelected)" aria-checked="\(ethSelected)" tabindex="\(ethSelected ? 0 : -1)">
                  <span class="asset-card-top"><span class="asset-symbol">ETHUSDT</span><span class="asset-state">待复核候选</span></span>
                  <span class="asset-price-score"><span><small>当前价格</small><b>3500</b></span><span><small>综合评分</small><b>78</b></span></span>
                  <span class="asset-core-grid"><span><small>方向</small><b>偏多</b></span><span><small>置信度</small><b>中</b></span><span><small>风险等级</small><b>中</b></span></span>
                </button>
                <button class="asset-card asset-select\(solSelected ? " is-selected" : "")" data-symbol="SOLUSDT" data-analysis-id="ANA_SOLUSDT" data-direction-label="偏空" data-worth-opening="false" data-asset-state="high_risk" data-selected="\(solSelected)" aria-checked="\(solSelected)" tabindex="\(solSelected ? 0 : -1)">
                  <span class="asset-card-top"><span class="asset-symbol">SOLUSDT</span><span class="asset-state">高风险</span></span>
                  <span class="asset-price-score"><span><small>当前价格</small><b>144</b></span><span><small>综合评分</small><b>73</b></span></span>
                  <span class="asset-core-grid"><span><small>方向</small><b>偏空</b></span><span><small>置信度</small><b>低</b></span><span><small>风险等级</small><b>高</b></span></span>
                </button>
              </div>
            </section>
            <section class="execution-section" id="execution-advice" data-exact-plan-visible="false">
              <h2 id="mobile-execution-title" tabindex="-1">执行建议</h2>
              <strong data-execution-field="statusLabel">等待同步</strong>
              <p data-execution-field="blockedReason">暂无补充说明</p>
              <dl class="definition-list execution-grid">
                <div><dt>方向</dt><dd data-execution-field="direction">--</dd></div>
                <div><dt>是否值得开仓</dt><dd data-execution-field="worthOpening">待同步</dd></div>
                <div><dt>入场区间</dt><dd data-execution-field="entryZone">--</dd></div>
                <div><dt>止损</dt><dd data-execution-field="stopLoss">--</dd></div>
                <div><dt>止盈方案</dt><dd data-execution-field="takeProfitRules">--</dd></div>
                <div><dt>杠杆建议</dt><dd data-execution-field="leverageSuggestion">--</dd></div>
                <div><dt>仓位建议</dt><dd data-execution-field="positionSuggestion">--</dd></div>
                <div><dt>计划失效条件</dt><dd data-execution-field="invalidCondition">--</dd></div>
                <div><dt>有效开始</dt><dd data-execution-field="validFrom">--</dd></div>
                <div><dt>有效结束</dt><dd data-execution-field="expiresAt">--</dd></div>
              </dl>
              <div class="conflict-block-summary"><span>冲突阻断</span><strong data-execution-conflict>--</strong></div>
            </section>
            <section class="position-section" id="position-monitor" data-position-independent>
              <h2 id="mobile-position-title" tabindex="-1">持仓监控</h2>
              <p>BTCUSDT / 多 · 等待首次监控</p>
            </section>
            <section class="ai-section" id="ai-review">
              <h2 id="mobile-ai-title" tabindex="-1">AI 三角色复核</h2>
              <strong data-ai-run-status>等待同步</strong>
              <span data-consistency-field="consistencyLevel">等待同步</span>
              <span data-consistency-field="level">--</span>
              <span data-consistency-field="confused">否</span>
              <span data-consistency-field="consistencySummary">等待同步</span>
              <div class="ai-role-summary-list" data-ai-role-root>
                <article class="ai-role-summary-card" data-ai-role-summary="GPT_FINAL"><div class="role-heading"><div><span>GPT_FINAL</span><h3>最终裁决官</h3></div><strong>待同步</strong></div><p class="role-status">当前观点待同步</p></article>
                <article class="ai-role-summary-card" data-ai-role-summary="GEMINI_REVIEW"><div class="role-heading"><div><span>GEMINI_REVIEW</span><h3>冲突复核官</h3></div><strong>待同步</strong></div><p class="role-status">当前复核待同步</p></article>
                <article class="ai-role-summary-card" data-ai-role-summary="GROK_CHALLENGE"><div class="role-heading"><div><span>GROK_CHALLENGE</span><h3>反方挑战官</h3></div><strong>待同步</strong></div><p class="role-status">当前挑战待同步</p></article>
              </div>
            </section>
            <div style="height: 1800px"></div>
            <div id="fixture-end-marker" style="height: 1px"></div>
          </main>
          <main class="mobile-home mobile-ai-analysis" id="mobile-ai-analysis"
                data-mobile-ai-view data-ai-analysis-root hidden>
            <header class="mobile-header ai-analysis-header">
              <h1 id="mobile-ai-analysis-title" tabindex="-1">AI 分析</h1>
              <span data-ai-analysis-state-status>正在同步</span>
            </header>
            <section class="ai-analysis-toolbar">
              <h2>单资产分析</h2>
              <input class="ai-analysis-search" type="search" placeholder="暂未开放"
                     disabled aria-disabled="true">
              <p>市场资产搜索与观察资产写入暂未开放。</p>
            </section>
            <section class="ai-analysis-context-section">
              <span data-ai-analysis-symbol>--</span>
              <dl class="ai-analysis-context-grid">
                <div class="full-row"><dt>analysisId</dt><dd data-ai-analysis-id>待同步</dd></div>
                <div><dt>规则基础方向</dt><dd data-ai-analysis-direction>--</dd></div>
                <div><dt>一致性等级</dt><dd data-ai-analysis-consistency-level>等待同步</dd></div>
                <div class="full-row"><dt>一致性摘要</dt><dd data-ai-analysis-consistency-summary>等待同步</dd></div>
              </dl>
            </section>
            <section class="ai-analysis-role-section">
              <strong data-ai-analysis-run-status>等待同步</strong>
              <div class="ai-analysis-role-tabs" role="tablist">
                <button type="button" role="tab" aria-selected="true"
                        data-ai-analysis-tab="GPT_FINAL">GPT_FINAL</button>
                <button type="button" role="tab" aria-selected="false" tabindex="-1"
                        data-ai-analysis-tab="GEMINI_REVIEW">GEMINI_REVIEW</button>
                <button type="button" role="tab" aria-selected="false" tabindex="-1"
                        data-ai-analysis-tab="GROK_CHALLENGE">GROK_CHALLENGE</button>
              </div>
              <div class="ai-analysis-role-panels" data-ai-analysis-role-root>
                <article class="ai-analysis-role-panel" role="tabpanel"
                         data-ai-analysis-role-panel="GPT_FINAL">
                  <div class="role-heading"><div><span>GPT_FINAL</span><h3>最终裁决官</h3></div><strong data-ai-analysis-role-status>待同步</strong></div>
                  <p data-ai-analysis-role-summary>当前观点待同步</p>
                  <dl class="role-summary-metrics"><div><dt>方向</dt><dd data-ai-analysis-role-direction>--</dd></div><div><dt>置信度</dt><dd data-ai-analysis-role-confidence>--</dd></div></dl>
                </article>
                <article class="ai-analysis-role-panel" role="tabpanel" hidden
                         data-ai-analysis-role-panel="GEMINI_REVIEW">
                  <div class="role-heading"><div><span>GEMINI_REVIEW</span><h3>冲突复核官</h3></div><strong data-ai-analysis-role-status>待同步</strong></div>
                  <p data-ai-analysis-role-summary>当前复核待同步</p>
                </article>
                <article class="ai-analysis-role-panel" role="tabpanel" hidden
                         data-ai-analysis-role-panel="GROK_CHALLENGE">
                  <div class="role-heading"><div><span>GROK_CHALLENGE</span><h3>反方挑战官</h3></div><strong data-ai-analysis-role-status>待同步</strong></div>
                  <p data-ai-analysis-role-summary>当前挑战待同步</p>
                </article>
              </div>
            </section>
            <section class="ai-analysis-deep-section">
              <p data-ai-analysis-detail-status>需要权威 analysisId</p>
              <a class="ai-analysis-detail-link" data-ai-analysis-detail-link
                 data-enabled-label="查看 Analysis Detail"
                 data-disabled-label="当前不可查看"
                 aria-disabled="true" tabindex="-1">当前不可查看</a>
            </section>
          </main>
          <nav class="bottom-nav" data-mobile-five-tab-navigation>
            <button type="button" data-home-nav aria-current="page">首页</button>
            <a href="/dashboard/mobile/positions" data-position-nav>持仓</a>
            <button type="button" data-ai-nav>AI分析</button>
            <button type="button" data-message-nav data-unavailable-nav aria-disabled="true">消息</button>
            <button type="button" data-profile-nav data-unavailable-nav aria-disabled="true">我的</button>
          </nav>
          <p class="nav-availability-status visually-hidden" data-nav-availability-status aria-live="polite"></p>
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
