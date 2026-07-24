import XCTest

final class XcodeProjectContractTests: XCTestCase {
    func testNoTeamIdCommitted() throws {
        let project = try projectFile()
        XCTAssertFalse(project.contains("DEVELOPMENT_TEAM"))
    }

    func testNoProvisioningProfileCommitted() throws {
        let project = try projectFile()
        XCTAssertFalse(project.contains("PROVISIONING_PROFILE_SPECIFIER"))
        XCTAssertFalse(project.contains("PROVISIONING_PROFILE ="))
    }

    func testNoXcuserdataCommitted() {
        let enumerator = FileManager.default.enumerator(
            at: projectRoot,
            includingPropertiesForKeys: nil
        )
        var offendingPaths: [String] = []
        while let file = enumerator?.nextObject() as? URL {
            if file.pathComponents.contains("xcuserdata") || file.pathExtension == "xcuserstate" {
                offendingPaths.append(file.path)
            }
        }
        XCTAssertTrue(offendingPaths.isEmpty, "Unexpected user-specific Xcode files")
    }

    func testIphoneTargetConfigured() throws {
        let project = try projectFile()
        XCTAssertTrue(project.contains("TARGETED_DEVICE_FAMILY = 1;"))
        XCTAssertFalse(project.contains("TARGETED_DEVICE_FAMILY = \"1,2\";"))
    }

    func testDeploymentTargetSupported() throws {
        let project = try projectFile()
        XCTAssertTrue(project.contains("IPHONEOS_DEPLOYMENT_TARGET = 17.0;"))
    }

    private var projectRoot: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }

    private func projectFile() throws -> String {
        try String(
            contentsOf: projectRoot
                .appendingPathComponent("TradeModelApp.xcodeproj")
                .appendingPathComponent("project.pbxproj")
        )
    }
}
