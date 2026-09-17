//
//  SubscriptionTier.swift
//  WhisperMate
//
//  Created by WhisperMate on 2025-01-24.
//

import Foundation

// MARK: - Usage Limits

public enum UsageLimits {
    public static let freeTrialWordLimit = 5000
}

// MARK: - Subscription Tier

public enum SubscriptionTier: String, Codable {
    case free
    case pro
    case lifetime

    /// Whether the user has a paid subscription (pro or lifetime)
    public var isPaid: Bool {
        self == .pro || self == .lifetime
    }

    public var displayName: String {
        switch self {
        case .free:
            return "Free Trial"
        case .pro:
            return "Pro"
        case .lifetime:
            return "Lifetime"
        }
    }

    public var wordLimit: Int {
        switch self {
        case .free:
            return UsageLimits.freeTrialWordLimit
        case .pro, .lifetime:
            return Int.max // Unlimited
        }
    }

    public var price: String {
        switch self {
        case .free:
            return "$0"
        case .pro:
            return "$8.49/month"
        case .lifetime:
            return "One-time purchase"
        }
    }

    public var features: [String] {
        switch self {
        case .free:
            return [
                "\(UsageLimits.freeTrialWordLimit.formatted()) trial words",
                "Full transcription features",
                "Local storage",
            ]
        case .pro, .lifetime:
            return [
                "Unlimited transcriptions",
                "Included API access",
                "Priority support",
                "Cloud sync (coming soon)",
            ]
        }
    }
}
