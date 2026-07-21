import SwiftUI
import WebKit

enum MobileWebTextSizeLevel: String, CaseIterable {
    case defaultSize = "default"
    case large
    case extraLarge = "extra-large"
    case accessibility

    init(dynamicTypeSize: DynamicTypeSize) {
        if dynamicTypeSize.isAccessibilitySize {
            self = .accessibility
        } else if dynamicTypeSize >= .xxLarge {
            self = .extraLarge
        } else if dynamicTypeSize >= .xLarge {
            self = .large
        } else {
            self = .defaultSize
        }
    }

    static func sanitized(_ rawValue: String) -> MobileWebTextSizeLevel {
        MobileWebTextSizeLevel(rawValue: rawValue) ?? .defaultSize
    }

    var attributeScript: String {
        "document.documentElement.setAttribute('data-mobile-text-size', '\(rawValue)');"
    }

    var userScript: WKUserScript {
        WKUserScript(
            source: attributeScript,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        )
    }
}

struct TradeModelWebView: UIViewRepresentable {
    let configuration: BackendConfiguration
    @ObservedObject var state: WebViewState
    let textSizeLevel: MobileWebTextSizeLevel
    var externalOpener: (URL) -> Void = { url in
        UIApplication.shared.open(url)
    }

    func makeCoordinator() -> WebViewCoordinator {
        WebViewCoordinator(
            rootURL: configuration.rootURL,
            navigationPolicy: TrustedNavigationPolicy(trustedOrigin: configuration.origin),
            state: state,
            textSizeLevel: textSizeLevel,
            externalOpener: externalOpener
        )
    }

    func makeUIView(context: Context) -> WKWebView {
        let webConfiguration = WKWebViewConfiguration()
        webConfiguration.websiteDataStore = WKWebsiteDataStore.default()
        webConfiguration.userContentController.addUserScript(textSizeLevel.userScript)

        let webView = WKWebView(frame: .zero, configuration: webConfiguration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .automatic

        let refreshControl = UIRefreshControl()
        refreshControl.addTarget(
            context.coordinator,
            action: #selector(WebViewCoordinator.refresh(_:)),
            for: .valueChanged
        )
        webView.scrollView.refreshControl = refreshControl

        context.coordinator.attach(webView: webView)
        context.coordinator.loadRoot()
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.updateTextSize(textSizeLevel, in: webView)
        context.coordinator.updateNavigationState()
    }
}
