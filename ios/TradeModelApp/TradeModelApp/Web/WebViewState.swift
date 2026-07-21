import SwiftUI

@MainActor
final class WebViewState: ObservableObject {
    struct NetworkIssue: Equatable {
        let title: String
        let message: String
    }

    enum Phase: Equatable {
        case loading
        case content
        case error(NetworkIssue)
    }

    @Published private(set) var phase: Phase = .loading
    @Published private(set) var canGoBack = false

    private var retryHandler: (() -> Void)?
    private var backHandler: (() -> Void)?
    private var refreshHandler: (() -> Void)?

    func configureActions(
        retry: @escaping () -> Void,
        goBack: @escaping () -> Void,
        refresh: @escaping () -> Void
    ) {
        retryHandler = retry
        backHandler = goBack
        refreshHandler = refresh
    }

    func didStartNavigation() {
        phase = .loading
    }

    func didReceiveServerRedirect() {
        // Login and dashboard redirects remain ordinary same-origin navigation.
    }

    func didFinishNavigation(canGoBack: Bool) {
        if self.canGoBack != canGoBack {
            self.canGoBack = canGoBack
        }
        if phase != .content {
            phase = .content
        }
    }

    func didFailNavigation() {
        phase = .error(
            NetworkIssue(
                title: "无法连接服务器",
                message: "请检查网络和后端地址后重试。"
            )
        )
    }

    func retry() {
        phase = .loading
        retryHandler?()
    }

    func refresh() {
        phase = .loading
        refreshHandler?()
    }

    func goBack() {
        guard canGoBack else { return }
        backHandler?()
    }
}
