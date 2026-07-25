import Foundation
import WebKit

@MainActor
final class WebViewCoordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
    private let rootURL: URL
    private let navigationPolicy: TrustedNavigationPolicy
    private let state: WebViewState
    private let externalOpener: (URL) -> Void
    private var textSizeLevel: MobileWebTextSizeLevel
    private weak var webView: WKWebView?

    init(
        rootURL: URL,
        navigationPolicy: TrustedNavigationPolicy,
        state: WebViewState,
        textSizeLevel: MobileWebTextSizeLevel,
        externalOpener: @escaping (URL) -> Void
    ) {
        self.rootURL = rootURL
        self.navigationPolicy = navigationPolicy
        self.state = state
        self.textSizeLevel = textSizeLevel
        self.externalOpener = externalOpener
    }

    func attach(webView: WKWebView) {
        self.webView = webView
        state.configureActions(
            retry: { [weak self] in self?.loadRoot() },
            goBack: { [weak webView] in webView?.goBack() },
            refresh: { [weak webView] in webView?.reload() }
        )
    }

    func loadRoot() {
        webView?.load(
            URLRequest(
                url: rootURL,
                cachePolicy: .useProtocolCachePolicy,
                timeoutInterval: 30
            )
        )
    }

    func updateTextSize(_ newLevel: MobileWebTextSizeLevel, in webView: WKWebView) {
        guard newLevel != textSizeLevel else { return }
        textSizeLevel = newLevel
        let controller = webView.configuration.userContentController
        controller.removeAllUserScripts()
        controller.addUserScript(newLevel.userScript)
        webView.evaluateJavaScript(newLevel.attributeScript)
    }

    @objc func refresh(_ sender: UIRefreshControl) {
        state.refresh()
        sender.endRefreshing()
    }

    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let url = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }
        handle(url: url, webView: webView, decisionHandler: decisionHandler)
    }

    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation?) {
        state.didStartNavigation()
    }

    func webView(_ webView: WKWebView, didReceiveServerRedirectForProvisionalNavigation navigation: WKNavigation?) {
        state.didReceiveServerRedirect()
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation?) {
        webView.evaluateJavaScript(textSizeLevel.attributeScript)
        state.didFinishNavigation(canGoBack: webView.canGoBack)
    }

    func webView(
        _ webView: WKWebView,
        didFailProvisionalNavigation navigation: WKNavigation?,
        withError error: Error
    ) {
        handleNavigationFailure(error)
    }

    func webView(
        _ webView: WKWebView,
        didFail navigation: WKNavigation?,
        withError error: Error
    ) {
        handleNavigationFailure(error)
    }

    func webView(
        _ webView: WKWebView,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        completionHandler(.performDefaultHandling, nil)
    }

    func webView(
        _ webView: WKWebView,
        createWebViewWith configuration: WKWebViewConfiguration,
        for navigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures
    ) -> WKWebView? {
        guard navigationAction.targetFrame == nil,
              let url = navigationAction.request.url else {
            return nil
        }

        switch navigationPolicy.decision(for: url) {
        case .allowInWebView:
            webView.load(navigationAction.request)
        case .openExternally(let externalURL):
            externalOpener(externalURL)
        case .block:
            break
        }
        return nil
    }

    static func shouldReportNavigationFailure(_ error: Error) -> Bool {
        let error = error as NSError
        return error.domain != NSURLErrorDomain || error.code != NSURLErrorCancelled
    }

    private func handleNavigationFailure(_ error: Error) {
        guard Self.shouldReportNavigationFailure(error) else { return }
        state.didFailNavigation()
    }

    private func handle(
        url: URL,
        webView: WKWebView,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        switch navigationPolicy.decision(for: url) {
        case .allowInWebView:
            decisionHandler(.allow)
        case .openExternally(let externalURL):
            externalOpener(externalURL)
            decisionHandler(.cancel)
        case .block:
            decisionHandler(.cancel)
        }
    }
}
