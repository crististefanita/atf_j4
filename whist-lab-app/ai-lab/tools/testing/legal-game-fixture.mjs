// Constructed component data, not a retained game, trained policy or strength evaluation.
const suits=['S','H','C','D'];
const ranks={11:'J',12:'Q',13:'K',14:'A'};
const suit=card=>suits.indexOf(card.at(-1));
const rank=card=>({J:11,Q:12,K:13,A:14})[card.slice(0,-1)]??Number(card.slice(0,-1));

export function createLegalGame({seed=19,players=4,seriesEnabled=true,seriesBonus=5}={}) {
    if(!Number.isInteger(seed)||seed<0||seed>0xffffffff)throw Error('Fixture seed must be unsigned 32-bit');
    if(!Number.isInteger(players)||players<3||players>6)throw Error('Fixture requires three to six seats');
    if(typeof seriesEnabled!=='boolean'||![5,10].includes(seriesBonus))throw Error('Invalid fixture series rules');
    let state=seed;
    const next=()=>state=(Math.imul(state,1664525)+1013904223)>>>0;
    const sizes=[...Array(players).fill(1),2,3,4,5,6,7,...Array(players).fill(8),7,6,5,4,3,2,...Array(players).fill(1)];
    const totals=Array(players).fill(0),streaks=Array(players).fill(0),hands=[];
    for(const [index,size] of sizes.entries()) {
        const deck=[];
        for(const color of suits)for(let value=15-2*players;value<=14;value++)deck.push(`${ranks[value]??value}${color}`);
        for(let i=deck.length-1;i>0;i--){const j=next()%(i+1);[deck[i],deck[j]]=[deck[j],deck[i]];}
        const dealer=index%players,turnedCard=size===8?null:deck[players*size],trump=turnedCard?suit(turnedCard):-1;
        const remaining=Array.from({length:players},(_,seat)=>deck.slice(seat*size,(seat+1)*size));
        const deal=remaining.map((cards,seat)=>({seat,cards:[...cards]}));
        const bids=[];let sum=0;
        for(let offset=1;offset<=players;offset++) {
            const seat=(dealer+offset)%players;
            let bid=next()%(size+1);
            if(offset===players&&sum+bid===size)bid=(bid+1)%(size+1);
            bids.push({seat,bid});sum+=bid;
        }
        let actor=(dealer+1)%players;
        const won=Array(players).fill(0),tricks=[];
        for(let round=0;round<size;round++) {
            const plays=[];let led=-1;
            for(let offset=0;offset<players;offset++) {
                const own=remaining[actor];
                const follow=own.filter(c=>suit(c)===led),cuts=own.filter(c=>suit(c)===trump);
                const allowed=led<0?own:follow.length?follow:cuts.length?cuts:own;
                const card=allowed[next()%allowed.length];
                if(led<0)led=suit(card);
                plays.push({seat:actor,card});own.splice(own.indexOf(card),1);actor=(actor+1)%players;
            }
            const power=p=>(suit(p.card)===trump?200:suit(p.card)===led?100:0)+rank(p.card);
            const winning=plays.reduce((a,b)=>power(a)>=power(b)?a:b);
            actor=winning.seat;won[actor]++;tricks.push({plays,winner:actor});
        }
        const scores=[];
        for(let seat=0;seat<players;seat++) {
            const bid=bids.find(b=>b.seat===seat).bid,exact=bid===won[seat];
            const base=exact?5+won[seat]:-Math.abs(bid-won[seat]);let series=0;
            if(seriesEnabled) {
                streaks[seat]=exact?Math.max(0,streaks[seat]):Math.min(0,streaks[seat]);
                if(size>1)streaks[seat]+=exact?1:-1;
                if(streaks[seat]===5){series=seriesBonus;streaks[seat]=0;}
                if(streaks[seat]===-5){series=-5;streaks[seat]=0;}
            }
            totals[seat]+=base+series;
            scores.push({seat,bid,won:won[seat],base,series,streak:streaks[seat],total:totals[seat]});
        }
        hands.push({number:index+1,size,dealer,trump:trump<0?'NT':suits[trump],turnedCard,deal,bids,tricks,scores});
    }
    return {schemaVersion:1,id:`generated-component-${players}-${seed}`,partition:'development',
        source:{kind:'GENERATED_COMPONENT_FIXTURE',visibility:'Constructed legal trace; not training or strength evidence'},
        rules:{variant:'ROMANIAN_WHIST',players,seriesEnabled,seriesBonus},seed:String(seed),
        players:Array.from({length:players},(_,seat)=>({seat,name:`Fixture ${seat}`,controller:seat%2?'Expert fixture':'IndependentPlayer fixture'})),
        hands,finalScores:[...totals]};
}
