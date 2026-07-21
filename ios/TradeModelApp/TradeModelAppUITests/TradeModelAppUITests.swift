import XCTest

final class TradeModelAppUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testMissingConfigurationShowsFailClosedState() {
        let app = XCUIApplication()
        app.launchEnvironment["TRADE_MODEL_BASE_URL"] = ""
        app.launch()

        XCTAssertTrue(app.staticTexts["configuration-error-title"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.webViews.firstMatch.exists)
    }
}
