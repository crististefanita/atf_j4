import Foundation
import Observation

struct GameCard: Decodable, Identifiable, Hashable {
    let suit: String
    let rank: Int
    let label: String
    var id: String { "\(suit)-\(rank)" }
}

struct PlayedGameCard: Decodable, Identifiable {
    let seat: Int
    let card: GameCard
    var id: String { "\(seat)-\(card.id)" }
}

struct GamePlayer: Decodable, Identifiable {
    let seat: Int
    let name: String
    let human: Bool
    let handSize: Int
    let bid: Int?
    let tricksWon: Int
    let totalScore: Int
    let streak: Int
    let streakPoints: Int
    var id: Int { seat }
}

struct ScoreHistoryPlayer: Decodable, Identifiable {
    let seat: Int
    let bid: Int?
    let tricks: Int?
    let points: Int
    let total: Int
    var id: Int { seat }
}

struct ScoreHistoryRow: Decodable, Identifiable {
    let hand: Int
    let players: [ScoreHistoryPlayer]
    var id: Int { hand }
}

struct GameState: Decodable {
    let phase: String
    let mode: String
    let handNumber: Int
    let handSize: Int
    let trump: String
    let dealerSeat: Int
    let biddingSeat: Int
    let currentSeat: Int
    let bidsSubmitted: Int
    let bidTotal: Int
    let recommendedBid: Int?
    let lastTrickWinnerSeat: Int
    let finalHand: Bool
    let allowZeroGame: Bool
    let players: [GamePlayer]
    let hand: [GameCard]
    let currentTrick: [PlayedGameCard]
    let lastTrick: [PlayedGameCard]
    let allowedBids: [Int]
    let legalCards: [GameCard]
    let history: [PlayedGameCard]
    let scoreHistory: [ScoreHistoryRow]
}

@MainActor
@Observable
final class GameStore {
    private let checkpointKey = "whistlab.ios.checkpoint.v1"
    private(set) var game: GameState?
    var selectedBid = 0
    var isBusy = false
    var errorMessage: String?

    init() {
        guard let checkpoint = UserDefaults.standard.string(forKey: checkpointKey),
              !checkpoint.isEmpty else { return }
        do {
            let state = try Self.decode(WhistGameBridge.restoreGame(withCheckpoint: checkpoint))
            game = state
            selectedBid = state.recommendedBid ?? state.allowedBids.first ?? 0
        } catch {
            UserDefaults.standard.removeObject(forKey: checkpointKey)
            errorMessage = Self.message(for: error, romanian: Self.isRomanian)
        }
    }

    func start(playerCount: Int, mode: String, hard: Bool, name: String,
               streaks: Bool, bonus: Int, zeroGame: Bool) {
        let difficulty = hard ? "HARD" : "NORMAL"
        perform {
            WhistGameBridge.startGame(
                withPlayerCount: Int32(playerCount), mode: mode, difficulty: difficulty,
                humanName: name, streakBonuses: streaks, streakPoints: Int32(bonus),
                allowZeroGame: zeroGame, seed: Int64.random(in: 1...Int64.max))
        }
    }

    func bid() {
        let value = Int32(selectedBid)
        perform { WhistGameBridge.submitBid(withBid: value) }
    }

    func play(_ card: GameCard) {
        let suit = card.suit
        let rank = Int32(card.rank)
        perform { WhistGameBridge.playCard(withSuitName: suit, rank: rank) }
    }

    func nextHand() {
        perform { WhistGameBridge.startNextHand() }
    }

    func newGame() {
        game = nil
        errorMessage = nil
        UserDefaults.standard.removeObject(forKey: checkpointKey)
    }

    private func perform(_ action: @escaping @Sendable () -> String) {
        guard !isBusy else { return }
        isBusy = true
        errorMessage = nil
        Task {
            do {
                let json = await Task.detached(priority: .userInitiated, operation: action).value
                let state = try Self.decode(json)
                game = state
                selectedBid = state.recommendedBid ?? state.allowedBids.first ?? 0
                UserDefaults.standard.set(WhistGameBridge.saveCheckpoint(), forKey: checkpointKey)
            } catch {
                errorMessage = Self.message(for: error, romanian: Self.isRomanian)
            }
            isBusy = false
        }
    }

    private static func decode(_ json: String) throws -> GameState {
        let data = Data(json.utf8)
        if let response = try? JSONSerialization.jsonObject(with: data) as? [String: String],
           let message = response["error"] {
            throw NSError(domain: "WhistGameBridge", code: 1,
                          userInfo: [NSLocalizedDescriptionKey: message])
        }
        return try JSONDecoder().decode(GameState.self, from: data)
    }

    private static var isRomanian: Bool {
        Locale.current.language.languageCode?.identifier == "ro"
    }

    private static func message(for error: Error, romanian: Bool) -> String {
        let detail = (error as NSError).localizedDescription
        return romanian ? "Nu am putut continua partida: \(detail)" : "The game could not continue: \(detail)"
    }
}
