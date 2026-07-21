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

        var ipv4 = in_addr()
        if host.withCString({ inet_pton(AF_INET, $0, &ipv4) }) == 1 {
            let address = UInt32(bigEndian: ipv4.s_addr)
            return address & 0xff00_0000 == 0x7f00_0000
        }

        var ipv6 = in6_addr()
        guard host.withCString({ inet_pton(AF_INET6, $0, &ipv6) }) == 1 else {
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
        self.rootURL = parsedURL.appendingPathComponent("dashboard")
        self.environment = environment
        self.origin = parsedOrigin
    }

    static func resolve(
        environment: AppEnvironment = .current,
        processEnvironment: [String: String] = ProcessInfo.processInfo.environment,
        infoDictionary: [String: Any] = Bundle.main.infoDictionary ?? [:]
    ) throws -> BackendConfiguration {
        let environmentValue = processEnvironment["TRADE_MODEL_BASE_URL"]?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let plistValue = (infoDictionary["TRADE_MODEL_BASE_URL"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let selected = environmentValue?.isEmpty == false ? environmentValue : plistValue
        return try BackendConfiguration(baseURLString: selected, environment: environment)
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
        if host.hasSuffix(".local") {
            return true
        }
        let octets = host.split(separator: ".").compactMap { Int($0) }
        guard octets.count == 4, octets.allSatisfy({ (0...255).contains($0) }) else {
            return false
        }
        return octets[0] == 10
            || (octets[0] == 172 && (16...31).contains(octets[1]))
            || (octets[0] == 192 && octets[1] == 168)
    }
}
