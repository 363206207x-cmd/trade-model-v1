import Foundation

enum TrustedNavigationDecision: Equatable {
    case allowInWebView
    case openExternally(URL)
    case block
}

struct TrustedNavigationPolicy {
    let trustedOrigin: WebOrigin

    func decision(for url: URL) -> TrustedNavigationDecision {
        guard url.user == nil, url.password == nil,
              let scheme = url.scheme?.lowercased() else {
            return .block
        }

        switch scheme {
        case "http", "https":
            guard let candidateOrigin = WebOrigin(url: url) else {
                return .block
            }
            guard !HostSecurityPolicy.isLoopbackHost(candidateOrigin.host) else {
                return .block
            }
            if candidateOrigin == trustedOrigin {
                return .allowInWebView
            }
            return scheme == "https" ? .openExternally(url) : .block
        case "mailto", "tel":
            return .openExternally(url)
        default:
            return .block
        }
    }
}
