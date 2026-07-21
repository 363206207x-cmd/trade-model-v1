import SwiftUI

struct ContentView: View {
    private let configurationResult: Result<BackendConfiguration, BackendConfigurationError>
    @StateObject private var webViewState = WebViewState()
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    init(
        configurationResult: Result<BackendConfiguration, BackendConfigurationError> =
            BackendConfiguration.resolveResult()
    ) {
        self.configurationResult = configurationResult
    }

    var body: some View {
        Group {
            switch configurationResult {
            case .success(let configuration):
                browser(configuration: configuration)
            case .failure:
                configurationError
            }
        }
        .background(Color(uiColor: .systemBackground))
    }

    private func browser(configuration: BackendConfiguration) -> some View {
        VStack(spacing: 0) {
            browserToolbar
            Divider()
            ZStack {
                TradeModelWebView(
                    configuration: configuration,
                    state: webViewState,
                    textSizeLevel: MobileWebTextSizeLevel(dynamicTypeSize: dynamicTypeSize)
                )

                switch webViewState.phase {
                case .loading:
                    LoadingView()
                        .transition(.opacity)
                case .content:
                    EmptyView()
                case .error(let issue):
                    NetworkErrorView(issue: issue, retry: webViewState.retry)
                        .transition(.opacity)
                }
            }
        }
        .animation(.easeInOut(duration: 0.18), value: webViewState.phase)
        .accessibilityIdentifier("trade-model-browser")
    }

    private var browserToolbar: some View {
        HStack(spacing: 8) {
            Text("Trade Model")
                .font(.headline)
                .lineLimit(1)

            Spacer(minLength: 12)

            Button(action: webViewState.goBack) {
                Image(systemName: "chevron.backward")
                    .frame(width: 20, height: 20)
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(!webViewState.canGoBack)
            .accessibilityLabel("返回")
            .accessibilityIdentifier("toolbar-back")

            Button(action: webViewState.refresh) {
                Image(systemName: "arrow.clockwise")
                    .frame(width: 20, height: 20)
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("刷新")
            .accessibilityIdentifier("toolbar-refresh")
        }
        .padding(.horizontal, 12)
        .frame(minHeight: 48)
        .background(Color(uiColor: .secondarySystemBackground))
    }

    private var configurationError: some View {
        VStack(spacing: 18) {
            Image(systemName: "network.slash")
                .font(.system(size: 36, weight: .semibold))
                .foregroundStyle(.secondary)

            Text("尚未配置后端地址")
                .font(.headline)
                .accessibilityIdentifier("configuration-error-title")

            Text("请在 Xcode Scheme 中设置 TRADE_MODEL_BASE_URL。")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(28)
        .frame(maxWidth: 430, maxHeight: .infinity)
    }
}

#Preview {
    ContentView()
}
