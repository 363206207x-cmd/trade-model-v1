import Darwin
import Foundation

enum HostSecurityPolicy {
    static func normalizedHost(_ rawHost: String) -> String {
        var host = rawHost.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if host.hasPrefix("[") && host.hasSuffix("]") {
            host.removeFirst()
            host.removeLast()
        }
        while host.hasSuffix(".") {
            host.removeLast()
        }
        return host
    }

    static func isLoopbackHost(_ rawHost: String) -> Bool {
        let host = normalizedHost(rawHost)
        guard !host.isEmpty else { return false }
        if host == "localhost" {
            return true
        }

        if let address = legacyCompatibleIPv4Address(host) {
            return address & 0xff00_0000 == 0x7f00_0000
        }

        let ipv6Host = removingIPv6ZoneIdentifier(from: host)
        var ipv6 = in6_addr()
        guard ipv6Host.withCString({ inet_pton(AF_INET6, $0, &ipv6) }) == 1 else {
            return false
        }
        return withUnsafeBytes(of: &ipv6) { rawBytes in
            let bytes = rawBytes.bindMemory(to: UInt8.self)
            let ipv6Loopback = bytes.prefix(15).allSatisfy { $0 == 0 } && bytes[15] == 1
            let ipv4Mapped = bytes.prefix(10).allSatisfy { $0 == 0 }
                && bytes[10] == 0xff
                && bytes[11] == 0xff
            return ipv6Loopback || (ipv4Mapped && bytes[12] == 127)
        }
    }

    static func canonicalIPv4Address(_ rawHost: String) -> UInt32? {
        let host = normalizedHost(rawHost)
        let labels = host.split(separator: ".", omittingEmptySubsequences: false)
        guard labels.count == 4, labels.allSatisfy({ !$0.isEmpty }) else {
            return nil
        }

        var ipv4 = in_addr()
        guard host.withCString({ inet_pton(AF_INET, $0, &ipv4) }) == 1 else {
            return nil
        }
        return UInt32(bigEndian: ipv4.s_addr)
    }

    static func isIPAddressLiteral(_ rawHost: String) -> Bool {
        let host = normalizedHost(rawHost)
        if legacyCompatibleIPv4Address(host) != nil {
            return true
        }

        let ipv6Host = removingIPv6ZoneIdentifier(from: host)
        var ipv6 = in6_addr()
        return ipv6Host.withCString({ inet_pton(AF_INET6, $0, &ipv6) }) == 1
    }

    private static func legacyCompatibleIPv4Address(_ host: String) -> UInt32? {
        var ipv4 = in_addr()
        if host.withCString({ inet_pton(AF_INET, $0, &ipv4) }) == 1 {
            return UInt32(bigEndian: ipv4.s_addr)
        }
        if host.withCString({ inet_aton($0, &ipv4) }) == 1 {
            return UInt32(bigEndian: ipv4.s_addr)
        }
        return nil
    }

    private static func removingIPv6ZoneIdentifier(from host: String) -> String {
        guard host.contains(":"),
              let zoneSeparator = host.firstIndex(of: "%") else {
            return host
        }
        return String(host[..<zoneSeparator])
    }
}

enum BackendConfigurationError: Error, Equatable {
    case missingBaseURL
    case invalidBaseURL
    case credentialsNotAllowed
    case loopbackNotAllowed
    case insecureDevelopmentHost
    case productionRequiresHTTPS
}

struct WebOrigin: Equatable {
    let scheme: String
    let host: String
    let port: Int

    init?(url: URL) {
        guard let scheme = url.scheme?.lowercased(),
              let host = url.host?.lowercased() else {
            return nil
        }
        self.scheme = scheme
        self.host = host
        self.port = url.port ?? (scheme == "https" ? 443 : 80)
    }
}

struct BackendConfiguration: Equatable {
    static let persistedBaseURLKey = "tradeModel.backendBaseURL"

    let baseURL: URL
    let rootURL: URL
    let environment: AppEnvironment
    let origin: WebOrigin

    init(baseURLString: String?, environment: AppEnvironment) throws {
        let candidate = baseURLString?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !candidate.isEmpty else {
            throw BackendConfigurationError.missingBaseURL
        }
        guard let components = URLComponents(string: candidate),
              let scheme = components.scheme?.lowercased(),
              let rawHost = components.host,
              ["http", "https"].contains(scheme),
              components.query == nil,
              components.fragment == nil,
              let parsedURL = components.url,
              let parsedOrigin = WebOrigin(url: parsedURL) else {
            throw BackendConfigurationError.invalidBaseURL
        }
        let host = HostSecurityPolicy.normalizedHost(rawHost)
        guard !host.isEmpty else {
            throw BackendConfigurationError.invalidBaseURL
        }
        guard components.user == nil, components.password == nil else {
            throw BackendConfigurationError.credentialsNotAllowed
        }
        guard !HostSecurityPolicy.isLoopbackHost(host) else {
            throw BackendConfigurationError.loopbackNotAllowed
        }

        switch environment {
        case .development:
            if scheme == "http", !Self.isPrivateNetworkHost(host) {
                throw BackendConfigurationError.insecureDevelopmentHost
            }
        case .production:
            guard scheme == "https" else {
                throw BackendConfigurationError.productionRequiresHTTPS
            }
        }

        self.baseURL = parsedURL
        self.rootURL = parsedURL
            .appendingPathComponent("dashboard")
            .appendingPathComponent("mobile")
        self.environment = environment
        self.origin = parsedOrigin
    }

    static func resolve(
        environment: AppEnvironment = .current,
        processEnvironment: [String: String] = ProcessInfo.processInfo.environment,
        infoDictionary: [String: Any] = Bundle.main.infoDictionary ?? [:],
        userDefaults: UserDefaults = .standard
    ) throws -> BackendConfiguration {
        let selected: String?
        let shouldPersist: Bool

        if let runtimeValue = processEnvironment["TRADE_MODEL_BASE_URL"] {
            selected = runtimeValue
            shouldPersist = true
        } else if let buildValue = configuredBuildValue(
            infoDictionary["TRADE_MODEL_BASE_URL"]
        ) {
            selected = buildValue
            shouldPersist = true
        } else {
            selected = userDefaults.string(forKey: persistedBaseURLKey)
            shouldPersist = false
        }

        let configuration = try BackendConfiguration(
            baseURLString: selected,
            environment: environment
        )
        if shouldPersist,
           userDefaults.string(forKey: persistedBaseURLKey)
            != configuration.baseURL.absoluteString {
            userDefaults.set(
                configuration.baseURL.absoluteString,
                forKey: persistedBaseURLKey
            )
        }
        return configuration
    }

    static func resolveResult() -> Result<BackendConfiguration, BackendConfigurationError> {
        do {
            return .success(try resolve())
        } catch let error as BackendConfigurationError {
            return .failure(error)
        } catch {
            return .failure(.invalidBaseURL)
        }
    }

    private static func isPrivateNetworkHost(_ host: String) -> Bool {
        if let address = HostSecurityPolicy.canonicalIPv4Address(host) {
            return address & 0xff00_0000 == 0x0a00_0000
                || address & 0xfff0_0000 == 0xac10_0000
                || address & 0xffff_0000 == 0xc0a8_0000
        }
        guard !HostSecurityPolicy.isIPAddressLiteral(host) else {
            return false
        }
        return host.hasSuffix(".local")
    }

    private static func configuredBuildValue(_ rawValue: Any?) -> String? {
        guard let rawValue = rawValue as? String else {
            return nil
        }
        let value = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty,
              !(value.hasPrefix("$(") && value.hasSuffix(")")) else {
            return nil
        }
        return value
    }
}
