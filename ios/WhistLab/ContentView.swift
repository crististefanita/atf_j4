import SwiftUI

private enum GameModeChoice: String, CaseIterable, Identifiable {
    case romanian = "ROMANIAN_WHIST"
    case ohHell = "OH_HELL"
    case descending = "DESCENDING"
    case downAndUp = "DOWN_AND_UP"
    case longRomanian = "LONG_ROMANIAN"
    case reverseOhHell = "REVERSE_OH_HELL"

    var id: String { rawValue }

    func title(romanian: Bool) -> String {
        switch self {
        case .romanian: romanian ? "Whist românesc · 1–8–1" : "Romanian Whist · 1–8–1"
        case .ohHell: "Oh Hell"
        case .descending: romanian ? "Descrescător" : "Descending"
        case .downAndUp: "Down & Up"
        case .longRomanian: romanian ? "Whist românesc lung" : "Long Romanian Whist"
        case .reverseOhHell: romanian ? "Oh Hell invers" : "Reverse Oh Hell"
        }
    }
}

struct ContentView: View {
    @State private var store = GameStore()
    @State private var playerCount = 4
    @State private var mode = GameModeChoice.romanian
    @State private var hard = false
    @State private var playerName = Locale.current.language.languageCode?.identifier == "ro" ? "Tu" : "You"
    @State private var streaks = true
    @State private var streakPoints = 10
    @State private var zeroGame = true

    private var isRomanian: Bool { Locale.current.language.languageCode?.identifier == "ro" }

    var body: some View {
        NavigationStack {
            Group {
                if let game = store.game {
                    table(game)
                } else {
                    setup
                }
            }
            .navigationTitle("Whist Lab")
            .navigationBarTitleDisplayMode(.inline)
            .alert(isRomanian ? "Eroare" : "Problem", isPresented: Binding(
                get: { store.errorMessage != nil },
                set: { if !$0 { store.errorMessage = nil } })) {
                Button(isRomanian ? "Închide" : "Close", role: .cancel) { store.errorMessage = nil }
            } message: {
                Text(store.errorMessage ?? "")
            }
        }
        .tint(Color(red: 0.12, green: 0.39, blue: 0.29))
    }

    private var setup: some View {
        Form {
            Section(isRomanian ? "Masa" : "Table") {
                TextField(isRomanian ? "Numele tău" : "Your name", text: $playerName)
                    .textInputAutocapitalization(.words)
                Picker(isRomanian ? "Jucători" : "Players", selection: $playerCount) {
                    ForEach(3...6, id: \.self) { Text("\($0)").tag($0) }
                }
                Picker(isRomanian ? "Variantă" : "Variant", selection: $mode) {
                    ForEach(GameModeChoice.allCases) { choice in
                        Text(choice.title(romanian: isRomanian)).tag(choice)
                    }
                }
                Picker(isRomanian ? "Nivel" : "Difficulty", selection: $hard) {
                    Text(isRomanian ? "Normal" : "Normal").tag(false)
                    Text(isRomanian ? "Dificil" : "Hard").tag(true)
                }
            }

            Section(isRomanian ? "Reguli românești" : "Romanian rules") {
                Toggle(isRomanian ? "Bonus și penalizare de serie" : "Streak bonus and penalty", isOn: $streaks)
                if streaks {
                    Picker(isRomanian ? "Puncte după cinci mâini" : "Points after five hands", selection: $streakPoints) {
                        Text("5").tag(5)
                        Text("10").tag(10)
                    }
                }
                Toggle(isRomanian ? "Permite Joc la 0" : "Allow Play for Zero", isOn: $zeroGame)
            }

            Section {
                Button {
                    store.start(playerCount: playerCount, mode: mode.rawValue, hard: hard,
                                name: playerName, streaks: streaks, bonus: streakPoints,
                                zeroGame: zeroGame && (mode == .romanian || mode == .longRomanian))
                } label: {
                    HStack {
                        Spacer()
                        if store.isBusy { ProgressView().padding(.trailing, 8) }
                        Text(isRomanian ? "Începe partida" : "Start game").fontWeight(.semibold)
                        Spacer()
                    }
                }
                .disabled(store.isBusy || playerName.trimmingCharacters(in: .whitespaces).isEmpty)
            }

            Section {
                Text(isRomanian
                     ? "Jocul rulează local pe dispozitiv. Regulile, licitarea și adversarii folosesc motorul comun cu versiunea Android."
                     : "The game runs locally on this device. Rules, bidding, and opponents use the same shared engine as Android.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func table(_ game: GameState) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                matchSummary(game)
                players(game)
                trick(game)
                actionPanel(game)
                if !game.scoreHistory.isEmpty {
                    scoreHistory(game)
                }
                if !game.history.isEmpty {
                    history(game)
                }
            }
            .padding()
            .frame(maxWidth: 760)
            .frame(maxWidth: .infinity)
        }
        .safeAreaInset(edge: .bottom) {
            if store.isBusy {
                ProgressView(isRomanian ? "Se calculează mutările…" : "Calculating moves…")
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity)
                    .background(.regularMaterial)
            }
        }
    }

    private func matchSummary(_ game: GameState) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(isRomanian ? "MÂNA \(game.handNumber) · \(game.handSize) CĂRȚI" : "HAND \(game.handNumber) · \(game.handSize) CARDS")
                .font(.headline)
                .foregroundStyle(Color(red: 0.12, green: 0.39, blue: 0.29))
            Text("\(isRomanian ? "Variantă" : "Variant"): \(GameModeChoice(rawValue: game.mode)?.title(romanian: isRomanian) ?? game.mode)")
                .font(.subheadline)
            Text("\(isRomanian ? "Atu" : "Trump"): \(trumpName(game.trump))")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
    }

    private func players(_ game: GameState) -> some View {
        VStack(spacing: 0) {
            HStack {
                Text(isRomanian ? "Jucător" : "Player")
                Spacer()
                Text(isRomanian ? "Licitat" : "Bid").frame(width: 54)
                Text(isRomanian ? "Levate" : "Tricks").frame(width: 58)
                Text(isRomanian ? "Total" : "Total").frame(width: 64, alignment: .trailing)
            }
            .font(.caption.weight(.semibold))
            .foregroundStyle(.secondary)
            .padding(.vertical, 8)
            ForEach(game.players) { player in
                Divider()
                HStack(spacing: 8) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(player.name).fontWeight(player.human ? .bold : .regular)
                        if player.seat == game.dealerSeat {
                            Text(isRomanian ? "Dealer" : "Dealer").font(.caption2).foregroundStyle(.secondary)
                        }
                    }
                    Spacer(minLength: 3)
                    Text(player.bid.map(String.init) ?? "—").frame(width: 54)
                    Text("\(player.tricksWon)/\(player.handSize)").frame(width: 58)
                    Text("\(player.totalScore)").frame(width: 64, alignment: .trailing).monospacedDigit()
                }
                .padding(.vertical, 9)
                .background(player.seat == game.currentSeat && game.phase == "PLAYING"
                            ? Color.green.opacity(0.09) : .clear)
            }
        }
        .padding(.horizontal, 12)
        .background(.background, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(.quaternary, lineWidth: 1))
    }

    private func trick(_ game: GameState) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(isRomanian ? "Levată curentă" : "Current trick")
                .font(.headline)
            if game.currentTrick.isEmpty {
                Text(isRomanian ? "Așteaptă prima carte." : "Waiting for the first card.")
                    .foregroundStyle(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(game.currentTrick) { played in
                            VStack(spacing: 4) {
                                cardFace(played.card)
                                Text(game.players[played.seat].name)
                                    .font(.caption2).lineLimit(1)
                            }
                        }
                    }
                }
            }
            if !game.lastTrick.isEmpty, game.phase == "SCORE" {
                Text("\(isRomanian ? "A câștigat ultima levată" : "Last trick won by"): \(game.players[max(0, game.lastTrickWinnerSeat)].name)")
                    .font(.subheadline.weight(.medium))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
    }

    @ViewBuilder
    private func actionPanel(_ game: GameState) -> some View {
        if game.phase == "BIDDING" && game.biddingSeat == 0 {
            VStack(alignment: .leading, spacing: 10) {
                Text(isRomanian ? "Alege licitația" : "Choose your bid").font(.headline)
                if let recommended = game.recommendedBid {
                    Text("\(isRomanian ? "Recomandat de Hard" : "Hard recommendation"): \(recommended)")
                        .font(.subheadline).foregroundStyle(.secondary)
                }
                Picker(isRomanian ? "Levate" : "Tricks", selection: $store.selectedBid) {
                    ForEach(game.allowedBids, id: \.self) { value in Text("\(value)").tag(value) }
                }
                .pickerStyle(.segmented)
                Button(isRomanian ? "Confirmă licitația" : "Confirm bid") { store.bid() }
                    .buttonStyle(.borderedProminent)
                    .disabled(store.isBusy || !game.allowedBids.contains(store.selectedBid))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
        } else if game.phase == "PLAYING" && game.currentSeat == 0 {
            VStack(alignment: .leading, spacing: 10) {
                Text(isRomanian ? "Alege o carte legală" : "Choose a legal card").font(.headline)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(game.legalCards) { card in
                            Button { store.play(card) } label: { cardFace(card) }
                                .buttonStyle(.plain)
                                .disabled(store.isBusy)
                                .accessibilityLabel(card.label)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
        } else if game.phase == "SCORE" {
            VStack(alignment: .leading, spacing: 10) {
                Text(game.finalHand
                     ? (isRomanian ? "Partidă încheiată" : "Match complete")
                     : (isRomanian ? "Mâna s-a încheiat" : "Hand complete"))
                    .font(.headline)
                if game.finalHand && game.allowZeroGame {
                    Text(isRomanian ? "Totalul exact zero valorează 500 de puncte." : "An exact zero match total is worth 500 points.")
                        .font(.subheadline).foregroundStyle(.secondary)
                }
                if game.finalHand {
                    Button(isRomanian ? "Partidă nouă" : "New game", action: store.newGame)
                        .buttonStyle(.borderedProminent)
                } else {
                    Button(isRomanian ? "Mâna următoare" : "Next hand", action: store.nextHand)
                        .buttonStyle(.borderedProminent)
                        .disabled(store.isBusy)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
        } else {
            Text(isRomanian ? "Se așteaptă adversarii…" : "Waiting for opponents…")
                .frame(maxWidth: .infinity)
                .padding()
        }
    }

    private func scoreHistory(_ game: GameState) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(isRomanian ? "Istoricul punctajului" : "Score history")
                .font(.headline)
            ScrollView(.horizontal, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 12) {
                        Text(isRomanian ? "Mână" : "Hand").frame(width: 48, alignment: .leading)
                        ForEach(game.players) { player in
                            Text(player.name).frame(width: 116, alignment: .leading).lineLimit(1)
                        }
                    }
                    .font(.caption.weight(.semibold))
                    .padding(.vertical, 7)
                    ForEach(game.scoreHistory) { row in
                        Divider()
                        HStack(spacing: 12) {
                            Text("\(row.hand)").frame(width: 48, alignment: .leading)
                            ForEach(row.players) { player in
                                Text("\(isRomanian ? "L" : "B") \(player.bid.map(String.init) ?? "—")  ·  \(isRomanian ? "P" : "Pts") \(player.total)")
                                    .frame(width: 116, alignment: .leading)
                                    .font(.caption.monospacedDigit())
                                    .lineLimit(1)
                            }
                        }
                        .padding(.vertical, 7)
                    }
                }
            }
            Text(isRomanian ? "L = licitație · P = punctaj total" : "B = bid · Pts = total points")
                .font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
    }

    private func history(_ game: GameState) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(isRomanian ? "Cărți jucate în mâna curentă" : "Cards played this hand")
                .font(.headline)
            ForEach(game.history) { played in
                HStack {
                    Text(game.players[played.seat].name)
                    Spacer()
                    Text(played.card.label).fontWeight(.semibold)
                        .foregroundStyle(isRed(played.card.suit) ? .red : .primary)
                }
                .font(.subheadline)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
    }

    private func cardFace(_ card: GameCard) -> some View {
        Text(card.label)
            .font(.system(size: 20, weight: .bold, design: .rounded))
            .foregroundStyle(isRed(card.suit) ? .red : .primary)
            .frame(width: 60, height: 82)
            .background(.background, in: RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(.quaternary, lineWidth: 1))
            .shadow(color: .black.opacity(0.07), radius: 3, y: 1)
    }

    private func isRed(_ suit: String) -> Bool { suit == "HEARTS" || suit == "DIAMONDS" }

    private func trumpName(_ trump: String) -> String {
        switch trump {
        case "NO_TRUMP": isRomanian ? "fără atu" : "no trump"
        case "SPADES": isRomanian ? "pică" : "spades"
        case "HEARTS": isRomanian ? "cupă" : "hearts"
        case "CLUBS": isRomanian ? "treflă" : "clubs"
        case "DIAMONDS": isRomanian ? "caro" : "diamonds"
        default: trump
        }
    }
}
