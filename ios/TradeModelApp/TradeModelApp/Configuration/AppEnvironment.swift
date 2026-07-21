import Foundation

enum AppEnvironment: String, Equatable {
    case development
    case production

    static var current: AppEnvironment {
        let configured = Bundle.main.object(
            forInfoDictionaryKey: "TRADE_MODEL_APP_ENVIRONMENT"
        ) as? String
        return AppEnvironment(rawValue: configured?.lowercased() ?? "") ?? .production
    }
}
