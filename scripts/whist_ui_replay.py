"""Projection publică și verificare independentă a mâinilor românești încheiate."""
import hashlib
import json
from pathlib import Path

SUITS = ("CLUBS", "DIAMONDS", "HEARTS", "SPADES")


def check(ok, message):
    if not ok:
        raise ValueError(message)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def card(value):
    suit, rank = value["suit"], value["rank"]
    check(suit in SUITS and type(rank) is int and 2 <= rank <= 14, "Invalid card")
    return [suit, rank]


def export(directory, run_id, completed):
    hands = []
    for ordinal in range(1, completed + 1):
        prefix = f"ro-h{ordinal:02d}"
        deal_file = directory / (prefix + "-deal-lab-state.json")
        score_file = directory / (prefix + "-score-lab-state.json")
        start, end = [json.loads(f.read_bytes()) for f in (deal_file, score_file)]
        n, dealer = start["playerCount"], start["dealerSeat"]
        check(start["phase"] == "BIDDING" and end["phase"] == "SCORE"
              and start["handIndex"] == end["handIndex"] == ordinal - 1, "Mismatched hand checkpoints")
        check(start["mode"] == "ROMANIAN_WHIST" and n == len(end["players"]), "Unsupported rules or seats")
        order = [(dealer + 1 + j) % n for j in range(n)]
        bids = [[seat, end["players"][seat]["bid"]] for seat in order]
        plays = [[p["seat"], *card(p)] for p in end["history"]]
        winners = [plays[j][0] for j in range(n, len(plays), n)] + [end["lastTrickWinnerSeat"]]
        player = next(p for p in start["players"] if p["human"])
        seat = player["seat"]
        hands.append({
            "hand": ordinal, "size": start["handSize"], "dealer": dealer, "trump": start["trump"],
            "turnedCard": card(start["turnedCard"]) if start.get("turnedCard") else None,
            "seriesEnabled": start["streakBonuses"], "seriesReward": start["streakBonusPoints"],
            "beforeTotals": [p["totalScore"] for p in start["players"]],
            "beforeStreaks": [p["streak"] for p in start["players"]],
            "observerAtBid": {"seat": seat, "ownCards": [card(c) for c in player["hand"]],
                              "priorBids": bids[:order.index(seat)]},
            "exPost": {"bids": bids, "plays": plays, "winners": winners,
                       "tricksWon": [p["tricksWon"] for p in end["players"]],
                       "scores": [p["totalScore"] for p in end["players"]],
                       "streaks": [p["streak"] for p in end["players"]],
                       "seriesPoints": [p["streakPoints"] for p in end["players"]]},
            "evidence": {p.name: digest(p) for p in (deal_file, score_file)}})
    return {"schema": "j4.public-romanian-trace/1", "sourceRunId": run_id,
            "units": "Seats zero-based. Cards [English suit, rank 2..14]. Plays [seat,suit,rank] in order.",
            "scope": "Completed hands only. Bids ordered by the betting rule. Intermediate winners observed "
                     "as the next lead, final winner from score state. Ex-post plays are outcomes, not training labels.",
            "hands": hands}


def replay(trace):
    check(trace["schema"] == "j4.public-romanian-trace/1", "Unknown replay schema")
    moves = tricks = 0
    previous = None
    for number, h in enumerate(trace["hands"], 1):
        e = h["exPost"]
        n, size = len(e["bids"]), h["size"]
        check(3 <= n <= 6 and size > 0 and h["hand"] == number, "Bad hand shape")
        check(len(e["plays"]) == n * size and len(e["winners"]) == size, "Incomplete hand")
        leader = (h["dealer"] + 1) % n
        if previous:
            check(h["beforeTotals"] == previous["exPost"]["scores"] and h["beforeStreaks"] == previous["exPost"]["streaks"]
                  and h["dealer"] == (previous["dealer"] + 1) % n, "Score, streak or dealer discontinuity")
        bids = dict(e["bids"])
        check([seat for seat, _ in e["bids"]] == [(leader + i) % n for i in range(n)]
              and all(type(b) is int and 0 <= b <= size for b in bids.values())
              and sum(bids.values()) != size, "Illegal bid order, value or final total")
        holdings = [set() for _ in range(n)]
        seen = set()
        for seat, suit, rank in e["plays"]:
            value = tuple(card({"suit": suit, "rank": rank}))
            check(0 <= seat < n and value not in seen, "Duplicate or invalid played card")
            seen.add(value)
            holdings[seat].add(value)
        check(all(len(cards) == size for cards in holdings), "Wrong seat card counts")
        turned = h["turnedCard"]
        if turned:
            check(turned[0] == h["trump"] and tuple(turned) not in seen, "Invalid turned trump")
        obs = h["observerAtBid"]
        check(set(map(tuple, obs["ownCards"])) == holdings[obs["seat"]], "Observer hand differs from observed plays")
        check(obs["priorBids"] == e["bids"][:(obs["seat"] - leader) % n], "Future bid exposed")
        won = [0] * n
        for offset in range(0, len(e["plays"]), n):
            trick = e["plays"][offset:offset+n]
            led = trick[0][1]
            for j, (seat, suit, rank) in enumerate(trick):
                check(seat == (leader + j) % n, "Wrong turn")
                hand = holdings[seat]
                legal = {c for c in hand if c[0] == led} or {c for c in hand if c[0] == h["trump"]} or hand
                check(j == 0 or (suit, rank) in legal, "Failure to follow suit or trump")
                hand.remove((suit, rank))
            leader = max(trick, key=lambda p: (p[1] == h["trump"], p[1] == led, p[2]))[0]
            check(leader == e["winners"][offset // n], "Trick winner mismatch")
            won[leader] += 1
        check(won == e["tricksWon"], "Trick counts differ")
        for seat, actual in enumerate(won):
            exact, prior = actual == bids[seat], h["beforeStreaks"][seat]
            series = h["seriesEnabled"]
            streak = (max(prior, 0) + (size > 1)) if exact else (min(prior, 0) - (size > 1))
            bonus = h["seriesReward"] if streak == 5 else -5 if streak == -5 else 0
            if not series:
                bonus = streak = 0
            delta = (5 + actual if exact else -abs(actual - bids[seat])) + bonus
            check(e["seriesPoints"][seat] == bonus and e["streaks"][seat] == (0 if abs(streak) == 5 else streak)
                  and e["scores"][seat] == h["beforeTotals"][seat] + delta, "Score or series mismatch")
        moves += n * size
        tricks += size
        previous = h
    return {"hands": len(trace["hands"]), "tricks": tricks, "moves": moves, "status": "PASS" if moves else "NOT_RUN"}


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Reverifică trace-ul public, fără emulator.")
    parser.add_argument("trace", type=Path)
    print(json.dumps(replay(json.loads(parser.parse_args().trace.read_bytes()))))
