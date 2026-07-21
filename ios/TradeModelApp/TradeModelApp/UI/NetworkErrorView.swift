import SwiftUI

struct NetworkErrorView: View {
    let issue: WebViewState.NetworkIssue
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(.secondary)

            Text(issue.title)
                .font(.headline)

            Text(issue.message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button(action: retry) {
                Label("重试", systemImage: "arrow.clockwise")
                    .frame(minWidth: 96, minHeight: 44)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(28)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemBackground))
        .accessibilityIdentifier("network-error-view")
    }
}
