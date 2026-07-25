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

    func testBackButtonHasAtLeast44PointTouchTargetAndVoiceOverLabel() {
        let app = configuredApplication()
        app.launch()

        let button = app.buttons["返回"]
        XCTAssertTrue(button.waitForExistence(timeout: 10))
        recordTouchTargetEvidence(name: "BACK_BUTTON_FRAME", button: button, app: app)
        XCTAssertGreaterThanOrEqual(button.frame.width, 44)
        XCTAssertGreaterThanOrEqual(button.frame.height, 44)
        XCTAssertEqual(button.label, "返回")
    }

    func testRefreshButtonHasAtLeast44PointTouchTargetAndVoiceOverLabel() {
        let app = configuredApplication()
        app.launch()

        let button = app.buttons["刷新"]
        XCTAssertTrue(button.waitForExistence(timeout: 10))
        recordTouchTargetEvidence(name: "REFRESH_BUTTON_FRAME", button: button, app: app)
        XCTAssertGreaterThanOrEqual(button.frame.width, 44)
        XCTAssertGreaterThanOrEqual(button.frame.height, 44)
        XCTAssertEqual(button.label, "刷新")
    }

    private func recordTouchTargetEvidence(name: String, button: XCUIElement, app: XCUIApplication) {
        let frame = button.frame
        print("\(name): \(frame.width)x\(frame.height)pt")
        XCTContext.runActivity(named: "\(name)=\(frame.width)x\(frame.height)pt") { activity in
            let attachment = XCTAttachment(screenshot: app.screenshot())
            attachment.name = "\(name)-44pt-evidence"
            attachment.lifetime = .keepAlways
            activity.add(attachment)
        }
    }

    private func configuredApplication() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["TRADE_MODEL_BASE_URL"] = "https://app.example.test"
        return app
    }
}
