import Combine
import WebKit
import XCTest
@testable import TradeModelApp

@MainActor
final class WebViewStateTests: XCTestCase {
    func testInitialStateIsLoading() {
        XCTAssertEqual(WebViewState().phase, .loading)
    }

    func testSuccessfulLoadShowsContent() {
        let state = WebViewState()
        state.didFinishNavigation(canGoBack: false)
        XCTAssertEqual(state.phase, .content)
    }

    func testRepeatedSuccessfulLoadDoesNotRepublishUnchangedState() {
        let state = WebViewState()
        var publicationCount = 0
        let cancellable = state.objectWillChange.sink {
            publicationCount += 1
        }

        state.didFinishNavigation(canGoBack: false)
        publicationCount = 0
        state.didFinishNavigation(canGoBack: false)

        XCTAssertEqual(publicationCount, 0)
        withExtendedLifetime(cancellable) {}
    }

    func testNetworkFailureShowsRetry() {
        let state = WebViewState()
        state.didFailNavigation()

        guard case .error = state.phase else {
            return XCTFail("Expected a redacted network error state")
        }
    }

    func testCancelledNavigationErrorIsNotReportable() {
        XCTAssertFalse(
            WebViewCoordinator.shouldReportNavigationFailure(URLError(.cancelled))
        )
    }

    func testRealNetworkErrorIsReportable() {
        XCTAssertTrue(
            WebViewCoordinator.shouldReportNavigationFailure(
                URLError(.notConnectedToInternet)
            )
        )
    }

    func testCancelledNavigationCallbacksDoNotShowNetworkError() {
        let state = WebViewState()
        let coordinator = makeCoordinator(state: state)
        let webView = WKWebView()
        state.didFinishNavigation(canGoBack: false)

        coordinator.webView(
            webView,
            didFailProvisionalNavigation: nil,
            withError: URLError(.cancelled)
        )
        XCTAssertEqual(state.phase, .content)

        coordinator.webView(
            webView,
            didFail: nil,
            withError: URLError(.cancelled)
        )
        XCTAssertEqual(state.phase, .content)
    }

    func testRealNavigationFailureShowsNetworkError() {
        let state = WebViewState()
        let coordinator = makeCoordinator(state: state)

        coordinator.webView(
            WKWebView(),
            didFailProvisionalNavigation: nil,
            withError: URLError(.notConnectedToInternet)
        )

        guard case .error = state.phase else {
            return XCTFail("Expected a real network failure to remain reportable")
        }
    }

    func testLoginRedirectIsNotTreatedAsFailure() {
        let state = WebViewState()
        state.didStartNavigation()
        state.didReceiveServerRedirect()
        state.didFinishNavigation(canGoBack: true)

        XCTAssertEqual(state.phase, .content)
        XCTAssertTrue(state.canGoBack)
    }

    func testRetryReloadsConfiguredRoot() {
        let state = WebViewState()
        var loadedURL: URL?
        let configuredRoot = URL(string: "https://app.example.test/dashboard")!
        state.configureActions(
            retry: { loadedURL = configuredRoot },
            goBack: {},
            refresh: {}
        )
        state.didFailNavigation()

        state.retry()

        XCTAssertEqual(loadedURL, configuredRoot)
        XCTAssertEqual(state.phase, .loading)
    }

    private func makeCoordinator(state: WebViewState) -> WebViewCoordinator {
        let rootURL = URL(string: "https://app.example.test")!
        return WebViewCoordinator(
            rootURL: rootURL,
            navigationPolicy: TrustedNavigationPolicy(
                trustedOrigin: WebOrigin(url: rootURL)!
            ),
            state: state,
            externalOpener: { _ in }
        )
    }
}
