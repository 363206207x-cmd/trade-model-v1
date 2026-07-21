import SwiftUI
import WebKit

struct TradeModelWebView: UIViewRepresentable {
    let configuration: BackendConfiguration
    @ObservedObject var state: WebViewState
    var externalOpener: (URL) -> Void = { url in
        UIApplication.shared.open(url)
    }

    func makeCoordinator() -> WebViewCoordinator {
        WebViewCoordinator(
            rootURL: configuration.rootURL,
            navigationPolicy: TrustedNavigationPolicy(trustedOrigin: configuration.origin),
            state: state,
            externalOpener: externalOpener
        )
    }

    func makeUIView(context: Context) -> WKWebView {
        let webConfiguration = WKWebViewConfiguration()
        webConfiguration.websiteDataStore = WKWebsiteDataStore.default()

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
        context.coordinator.updateNavigationState()
    }
}
