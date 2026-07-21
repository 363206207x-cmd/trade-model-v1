import Combine
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

    func testRepeatedContentNavigationStateIsIdempotent() {
        let state = WebViewState()
        var publicationCount = 0
        let observation = state.objectWillChange.sink { publicationCount += 1 }

        state.didFinishNavigation(canGoBack: false)
        let publicationCountAfterFirstCompletion = publicationCount
        state.didFinishNavigation(canGoBack: false)

        XCTAssertEqual(publicationCount, publicationCountAfterFirstCompletion)
        withExtendedLifetime(observation) {}
    }

    func testNetworkFailureShowsRetry() {
        let state = WebViewState()
        state.didFailNavigation()

        guard case .error = state.phase else {
            return XCTFail("Expected a redacted network error state")
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
        let configuredRoot = URL(string: "https://app.example.test/dashboard/mobile")!
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
}
