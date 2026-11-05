import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';

const suits=['S','H','C','D'];
const names=['SPADES','HEARTS','CLUBS','DIAMONDS'];
const rankText={11:'J',12:'Q',13:'K',14:'A'};
const check=(ok,message)=>{if(!ok)throw new Error(message);};
export function cardCode(card){return `${rankText[card.rank]??card.rank}${suits[names.indexOf(card.suit)]}`;}
function card(code){const m=/^(10|[2-9JQKA])([SHCD])$/.exec(code);check(m,`Invalid card ${code}`);return {suit:suits.indexOf(m[2]),rank:({J:11,Q:12,K:13,A:14})[m[1]]??Number(m[1])};}
function winner(plays,trump){const led=card(plays[0].card).suit;let best=plays[0];const power=p=>{const c=card(p.card);return (c.suit===trump?200:c.suit===led?100:0)+c.rank;};for(const p of plays)if(power(p)>power(best))best=p;return best.seat;}
function legal(hand,led,trump){if(led<0)return hand;const follows=hand.filter(c=>card(c).suit===led);if(follows.length)return follows;const cuts=hand.filter(c=>card(c).suit===trump);return cuts.length?cuts:hand;}
function schedule(n){return [...Array(n).fill(1),2,3,4,5,6,7,...Array(n).fill(8),7,6,5,4,3,2,...Array(n).fill(1)];}
function equal(expected,actual,message){check(JSON.stringify(expected)===JSON.stringify(actual),`${message}: expected ${JSON.stringify(expected)}, actual ${JSON.stringify(actual)}`);}
function properties(text){return Object.fromEntries(text.split(/\r?\n/).filter(l=>l&&!l.startsWith('#')).map(l=>{const i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));}

/** Full information belongs to the post-game corpus, never to a player's decision input. */
export function validateGame(game,onComparison=null){
  let scope="Game";const compare=(expected,actual,name)=>{if(onComparison)onComparison({scope,name,expected,actual,passed:JSON.stringify(expected)===JSON.stringify(actual)});equal(expected,actual,name);};
  check(typeof game.id==='string'&&/^[a-zA-Z0-9_.-]+$/.test(game.id),'Safe game identity');check(['development','heldout','reference'].includes(game.partition),'Dataset partition');
  compare(1,game.schemaVersion,'Schema');const n=game.rules.players;check(n>=3&&n<=6,'Table size');
  check(Number.isInteger(n),'Integer table size');check(typeof game.rules.seriesEnabled==='boolean','Series rule must be boolean');
  check(typeof game.seed==='string'&&/^-?\d+$/.test(game.seed),'Seed must be an exact decimal string');
  compare('ROMANIAN_WHIST',game.rules.variant,'Rule profile');compare(n,game.players.length,'Players');
  game.players.forEach((p,seat)=>{compare(seat,p.seat,'Player seat');check(typeof p.controller==='string'&&p.controller.length>0,'Controller provenance');});
  check([5,10].includes(game.rules.seriesBonus),'Positive series reward');
  const sizes=schedule(n);compare(sizes.length,game.hands.length,'Complete schedule');
  let totals=Array(n).fill(0),streaks=Array(n).fill(0),moves=0,tricks=0,bids=0;
  const initialDealer=game.hands[0].dealer;
  check(Number.isInteger(initialDealer)&&initialDealer>=0&&initialDealer<n,'Initial dealer');
  for(let h=0;h<sizes.length;h++){
    const hand=game.hands[h],size=sizes[h],trump=suits.indexOf(hand.trump);scope=`Hand ${h+1}`;
    compare(h+1,hand.number,'Hand number');compare(size,hand.size,'Hand size');compare((initialDealer+h)%n,hand.dealer,'Dealer rotation');
    compare(n,hand.deal.length,'Deal seats');const remaining=hand.deal.map((p,seat)=>{compare(seat,p.seat,'Deal seat');compare(size,p.cards.length,'Deal size');return [...p.cards];});
    const distinct=new Set();for(const cards of remaining)for(const c of cards){check(card(c).rank>=15-2*n,'Reduced deck rank');check(!distinct.has(c),'Duplicate dealt card');distinct.add(c);}
    if(size===8){compare('NT',hand.trump,'Eight cards no trump');compare(null,hand.turnedCard,'No undealt card at eight');}
    else {check(trump>=0,'Turned trump');check(hand.turnedCard&&!distinct.has(hand.turnedCard),'Turned card must be undealt');const turned=card(hand.turnedCard);check(turned.rank>=15-2*n&&turned.suit===trump,'Turned card suit/rank');}
    compare(n,hand.bids.length,'All bids');const targets=Array(n);let sum=0;
    for(let b=0;b<n;b++){const bid=hand.bids[b];compare((hand.dealer+1+b)%n,bid.seat,'Bidding order');check(Number.isInteger(bid.bid)&&bid.bid>=0&&bid.bid<=size,'Bid range');if(b===n-1)check(sum+bid.bid!==size,'Forbidden final bid total');targets[bid.seat]=bid.bid;sum+=bid.bid;bids++;}
    compare(size,hand.tricks.length,'Complete trick count');const won=Array(n).fill(0);let actor=(hand.dealer+1)%n;
    for(const trick of hand.tricks){compare(n,trick.plays.length,'Trick size');let led=-1;
      for(const play of trick.plays){compare(actor,play.seat,'Card actor');const allowed=legal(remaining[actor],led,trump);if(onComparison)onComparison({scope,name:`Legal card, seat ${actor}, play ${moves+1}`,expected:`one of [${allowed.join(', ')}]`,actual:play.card,passed:allowed.includes(play.card)});check(allowed.includes(play.card),'Illegal follow-suit/mandatory-trump card');const at=remaining[actor].indexOf(play.card);check(at>=0,'Card ownership');remaining[actor].splice(at,1);if(led<0)led=card(play.card).suit;actor=(actor+1)%n;moves++;}
      actor=winner(trick.plays,trump);compare(actor,trick.winner,'Trick winner');won[actor]++;tricks++;
    }
    check(remaining.every(cards=>cards.length===0),'Unplayed cards');compare(n,hand.scores.length,'All scores');
    for(let seat=0;seat<n;seat++){
      const row=hand.scores[seat];compare(seat,row.seat,'Score seat');compare(targets[seat],row.bid,'Immutable bid');compare(won[seat],row.won,'Won counter');
      const exact=won[seat]===targets[seat],base=exact?5+won[seat]:-Math.abs(won[seat]-targets[seat]);let series=0;
      if(game.rules.seriesEnabled){streaks[seat]=exact?Math.max(0,streaks[seat]):Math.min(0,streaks[seat]);if(size!==1)streaks[seat]+=exact?1:-1;if(streaks[seat]===5){series=game.rules.seriesBonus;streaks[seat]=0;}if(streaks[seat]===-5){series=-5;streaks[seat]=0;}}
      totals[seat]+=base+series;compare(base,row.base,'Base contract points');compare(series,row.series,'Series points');compare(streaks[seat],row.streak,'Series counter');compare(totals[seat],row.total,'Cumulative score');
    }
  }
  compare(totals,game.finalScores,'Final scores');return {status:'PASS',hands:game.hands.length,bids,tricks,cards:moves,finalScores:totals};
}

/** Public-information decision records for training/replay. Outcome is a label, never an observation. */
export function* decisionExamples(game){
  validateGame(game);const n=game.rules.players,sizes=schedule(n);let totals=Array(n).fill(0),streaks=Array(n).fill(0);
  for(const hand of game.hands){
    const remaining=hand.deal.map(d=>[...d.cards]),bids=Array(n).fill(null),won=Array(n).fill(0),history=[];let trick=[];
    const record=(seat,kind,action)=>({gameId:game.id,partition:game.partition,handNumber:hand.number,seat,controller:game.players[seat].controller,kind,
      observation:{handSize:hand.size,dealer:hand.dealer,trump:hand.trump,turnedCard:hand.turnedCard,ownHand:[...remaining[seat]],players:Array.from({length:n},(_,s)=>({seat:s,remaining:remaining[s].length,bid:bids[s],won:won[s],total:totals[s],streak:streaks[s]})),trick:trick.map(p=>({...p})),history:history.map(p=>({...p})),futureHandSizes:sizes.slice(hand.number),seriesEnabled:game.rules.seriesEnabled,seriesBonus:game.rules.seriesBonus},
      action,outcome:{handBid:hand.scores[seat].bid,handWon:hand.scores[seat].won,handBase:hand.scores[seat].base,handSeries:hand.scores[seat].series,finalScore:game.finalScores[seat]}});
    for(const bid of hand.bids){yield record(bid.seat,'bid',{bid:bid.bid});bids[bid.seat]=bid.bid;}
    for(const completed of hand.tricks){trick=[];for(const play of completed.plays){yield record(play.seat,'card',{card:play.card});remaining[play.seat].splice(remaining[play.seat].indexOf(play.card),1);history.push({...play});trick.push({...play});}won[completed.winner]++;}
    totals=hand.scores.map(s=>s.total);streaks=hand.scores.map(s=>s.streak);
  }
}

export async function exportDecisionExamples(gameFile,outputFile){
  const game=JSON.parse(await fs.readFile(gameFile,'utf8'));check(game.partition==='development','Training examples must come from development only');
  const lines=[...decisionExamples(game)].map(d=>JSON.stringify(d));await fs.mkdir(path.dirname(outputFile),{recursive:true});
  await fs.writeFile(outputFile,lines.join('\n')+'\n',{flag:'wx'});return {status:'PASS',decisions:lines.length,outputFile};
}

export async function saveGame(corpusDir,game){
  const validation=validateGame(game);check(/^[a-zA-Z0-9_.-]+$/.test(game.id),'Safe game ID');
  const folder=path.join(corpusDir,'games',game.partition);check(['development','heldout','reference'].includes(game.partition),'Dataset partition');await fs.mkdir(folder,{recursive:true});
  const json=JSON.stringify(game,null,2)+'\n',file=path.join(folder,game.id+'.json');
  try{await fs.writeFile(file,json,{flag:'wx'});}catch(e){if(e.code!=='EEXIST')throw e;equal(json,await fs.readFile(file,'utf8'),'Existing game is immutable');}
  return {file,sha256:crypto.createHash('sha256').update(json).digest('hex'),...validation};
}

export async function exportAndroidRun(evidenceDir,corpusDir,policyTag='round-v2'){
  const result=properties(await fs.readFile(path.join(evidenceDir,'romanian-match-result.properties'),'utf8'));
  const audit=properties(await fs.readFile(path.join(evidenceDir,'offline-audit-result.properties'),'utf8'));
  equal('PASS',result.status,'Live result');equal('PASS',audit.status,'Offline audit');equal('24',result.handsCompleted,'Complete four-seat game');
  const first=JSON.parse(await fs.readFile(path.join(evidenceDir,'ro-h01-deal-lab-state.json'),'utf8'));check(Number.isSafeInteger(first.seed),'Seed requires lossless parsing');
  const game={schemaVersion:1,id:`android-${policyTag}-${first.seed}`,partition:'development',source:{kind:'android',package:'ro.whistlab.app',evidenceRun:path.basename(path.dirname(evidenceDir)),visibility:'post-game full deal; not a decision input'},rules:{variant:'ROMANIAN_WHIST',players:first.playerCount,seriesEnabled:first.streakBonuses??false,seriesBonus:first.streakBonusPoints??5},seed:String(first.seed),players:first.players.map(p=>({seat:p.seat,name:p.name,controller:p.human?`IndependentPlayer ${policyTag}`:`Expert ${policyTag}`})),hands:[],finalScores:JSON.parse(result.finalTotals)};
  for(let h=0;h<24;h++){
    const prefix=`ro-h${String(h+1).padStart(2,'0')}`;
    const start=JSON.parse(await fs.readFile(path.join(evidenceDir,prefix+'-deal-lab-state.json'),'utf8'));
    const end=JSON.parse(await fs.readFile(path.join(evidenceDir,prefix+'-score-lab-state.json'),'utf8'));
    const hand={number:h+1,size:start.handSize,dealer:start.dealerSeat,trump:suits[names.indexOf(start.trump)]??'NT',turnedCard:start.turnedCard?cardCode(start.turnedCard):null,deal:start.players.map(p=>({seat:p.seat,cards:p.hand.map(cardCode)})),bids:[],tricks:[],scores:end.players.map(p=>({seat:p.seat,bid:p.bid,won:p.tricksWon,base:p.tricksWon===p.bid?5+p.tricksWon:-Math.abs(p.tricksWon-p.bid),series:p.streakPoints??0,total:p.totalScore,streak:p.streak??0}))};
    for(let i=1;i<=first.playerCount;i++){const seat=(hand.dealer+i)%first.playerCount;hand.bids.push({seat,bid:end.players[seat].bid});}
    for(let i=0;i<end.history.length;i+=first.playerCount){const plays=end.history.slice(i,i+first.playerCount).map(p=>({seat:p.seat,card:cardCode(p)}));hand.tricks.push({plays,winner:winner(plays,suits.indexOf(hand.trump))});}
    game.hands.push(hand);
  }
  const provenance=JSON.parse(await fs.readFile(path.join(path.dirname(evidenceDir),'apk-provenance.json'),'utf8'));game.source.apkSha256=provenance.sha256.toLowerCase();
  return saveGame(corpusDir,game);
}

export async function summarizeCorpus(corpusDir){
  const files=[];async function walk(dir){for(const e of await fs.readdir(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())await walk(p);else if(e.name.endsWith('.json'))files.push(p);}}await walk(path.join(corpusDir,'games'));
  const policies={},index=[],dealPartitions=new Map(),identities=new Set();
  for(const file of files.sort()){const game=JSON.parse(await fs.readFile(file,'utf8'));const audit=validateGame(game);check(!identities.has(game.id),"Duplicate game identity");identities.add(game.id);equal(game.id+".json",path.basename(file),"Game filename identity");equal(game.partition,path.relative(path.join(corpusDir,'games'),file).split(path.sep)[0],'Partition folder');const dealHash=crypto.createHash('sha256').update(JSON.stringify(game.hands.map(h=>[h.dealer,h.trump,h.turnedCard,h.deal]))).digest('hex');const oldPartition=dealPartitions.get(dealHash);check(!oldPartition||oldPartition===game.partition,'Same deals leaked across dataset partitions');dealPartitions.set(dealHash,game.partition);index.push({id:game.id,partition:game.partition,dealHash,file:path.relative(corpusDir,file).replaceAll('\\','/'),...audit});const best=Math.max(...game.finalScores),ties=game.finalScores.filter(s=>s===best).length;
    if(game.partition!=='development')continue;
    for(const player of game.players){let key=player.controller;if(key.includes('candidate-v3')){const revision=game.id.match(/^learning-(candidate\d+)-(player|expert)-vs-/)?.[1]??game.id.split('-seed')[0];key+=' / '+revision;}const p=policies[key]??={games:0,points:0,wins:0,exact:0,contracts:0,absoluteBidError:0,byHandSize:{}};p.games++;p.points+=game.finalScores[player.seat];p.wins+=game.finalScores[player.seat]===best?1/ties:0;
      for(const hand of game.hands){const r=hand.scores[player.seat];p.contracts++;p.exact+=r.bid===r.won?1:0;p.absoluteBidError+=Math.abs(r.bid-r.won);const b=p.byHandSize[hand.size]??={exact:0,contracts:0};b.contracts++;b.exact+=r.bid===r.won?1:0;}
    }
  }
  const summary={schemaVersion:1,games:index.length,distinctDeals:dealPartitions.size,policiesScope:'development only; heldout excluded; candidate revisions grouped by immutable experiment ID',policies,index};await fs.writeFile(path.join(corpusDir,'index.json'),JSON.stringify(summary,null,2)+'\n');return summary;
}
