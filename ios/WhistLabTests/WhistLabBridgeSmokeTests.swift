import XCTest
@testable import WhistLab

final class WhistLabBridgeSmokeTests: XCTestCase {
    func testSharedEnginePlaysAndRestoresACompleteRomanianHand() throws {
        let initial = try Self.state(WhistGameBridge.startGame(
            withPlayerCount: 4,
            mode: "ROMANIAN_WHIST",
            difficulty: "NORMAL",
            humanName: "Tester",
            streakBonuses: false,
            streakPoints: 5,
            allowZeroGame: false,
            seed: 20261107))

        XCTAssertEqual(initial["phase"] as? String, "BIDDING")
        XCTAssertEqual(initial["handSize"] as? Int, 1)
        XCTAssertEqual(initial["biddingSeat"] as? Int, 0)
        let allowedBids = try XCTUnwrap(initial["allowedBids"] as? [Int])
        let bid: Int
        if let recommended = initial["recommendedBid"] as? Int {
            bid = recommended
        } else {
            bid = try XCTUnwrap(allowedBids.first)
        }

        let playing = try Self.state(WhistGameBridge.submitBid(withBid: Int32(bid)))
        XCTAssertEqual(playing["phase"] as? String, "PLAYING")
        let legalCards = try XCTUnwrap(playing["legalCards"] as? [[String: Any]])
        let card = try XCTUnwrap(legalCards.first)

        let completedJson = WhistGameBridge.playCard(
            withSuitName: try XCTUnwrap(card["suit"] as? String),
            rank: Int32(try XCTUnwrap(card["rank"] as? Int)))
        let completedHand = try Self.state(completedJson)
        XCTAssertEqual(completedHand["phase"] as? String, "SCORE")
        XCTAssertEqual((completedHand["history"] as? [[String: Any]])?.count, 4)
        XCTAssertEqual((completedHand["scoreHistory"] as? [[String: Any]])?.count, 1)

        let checkpoint = WhistGameBridge.saveCheckpoint()
        XCTAssertFalse(checkpoint.isEmpty)
        XCTAssertEqual(completedJson, WhistGameBridge.restoreGame(withCheckpoint: checkpoint))
    }

    func testInvalidCheckpointReturnsBridgeErrorWithoutObjectiveCException() throws {
        let response = try Self.state(WhistGameBridge.restoreGame(withCheckpoint: "not-base64"))
        XCTAssertNotNil(response["error"] as? String)
        XCTAssertTrue(WhistGameBridge.currentState().isEmpty)
    }

    private static func state(_ json: String) throws -> [String: Any] {
        try XCTUnwrap(JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String: Any])
    }
}
