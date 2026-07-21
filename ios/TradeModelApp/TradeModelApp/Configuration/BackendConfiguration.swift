import Foundation

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
              let host = components.host?.lowercased(),
              ["http", "https"].contains(scheme),
              components.query == nil,
              components.fragment == nil,
              let parsedURL = components.url,
              let parsedOrigin = WebOrigin(url: parsedURL) else {
            throw BackendConfigurationError.invalidBaseURL
        }
        guard components.user == nil, components.password == nil else {
            throw BackendConfigurationError.credentialsNotAllowed
        }
        guard !Self.isLoopback(host) else {
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

    private static func isLoopback(_ host: String) -> Bool {
        host == "localhost" || host == "127.0.0.1" || host == "::1"
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
