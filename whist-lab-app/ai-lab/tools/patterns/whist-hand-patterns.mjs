import assert from 'node:assert/strict';
const ranks={A:14,K:13,Q:12,J:11};
export const PATTERNS=[
 ['MASTER_TRUMP','Atu fără carte mai mare la adversari','Master trump with no higher outside own/public cards',null,'PROVEN'],
 ['ACE_OFF_TRUMP','As pe culoare laterală','Off-trump ace',1,'CONTEXTUAL'],
 ['KING_SINGLETON','Popă singur','Singleton king',null,'CONTEXTUAL'],
 ['KING_WITH_LOW','Popă cu cel puțin o carte mică','King with at least one low cover',1,'USER_PRIOR'],
 ['KING_JACK_WITH_LOW','Popă-valet cu acoperire și gol între onoruri','King-jack with a cover and an honor gap',null,'CONTEXTUAL'],
 ['KING_QUEEN_JACK_WITH_LOW','Popă-damă-valet cu acoperire','King-queen-jack with a low cover',3,'USER_PRIOR'],
 ['PROMOTABLE_HONOR_GROUP','Grup de onoruri cu acoperiri pentru rangurile lipsă','Honor group with covers for missing higher ranks',null,'CONTEXTUAL'],
 ['KING_QUEEN_WITH_LOW','Popă-damă cu o acoperire mică','King-queen with a low cover',2,'USER_PRIOR'],
 ['QUEEN_TWO_LOW','Damă cu două acoperiri mici','Queen with two low covers',1,'USER_PRIOR'],
 ['QUEEN_SHORT','Damă fără două acoperiri','Queen without two low covers',null,'CONTEXTUAL'],
 ['QUEEN_JACK_TWO_LOW','Damă-valet cu două acoperiri','Queen-jack with two low covers',2,'USER_PRIOR'],
 ['JACK_THREE_LOW','Valet cu trei acoperiri mici','Jack with three low covers',1,'RESEARCH_PRIOR'],
 ['TEN_FOUR_LOW','Zece cu patru cărți mai mici','Ten with four lower cards',1,'RESEARCH_PRIOR'],
 ['ACE_KING','Secvență as-popă','Ace-king sequence',2,'CONTEXTUAL'],
 ['ACE_KING_QUEEN','Secvență as-popă-damă','Ace-king-queen sequence',3,'CONTEXTUAL'],
 ['LONG_TRUMP','Cel puțin trei atuuri','At least three trumps',null,'CONTEXTUAL'],
 ['LOW_TRUMP','Atuuri fără onor','Trumps without an honor',null,'CONTEXTUAL'],
 ['SIDE_VOID','Lipsă pe o culoare laterală','Void in a side suit',null,'CONTEXTUAL'],
 ['LONG_NO_TRUMP_SUIT','Culoare lungă fără atu','Long suit at no trump',null,'CONTEXTUAL']
].map(([id,ro,en,trickHint,status])=>({id,description_ro:ro,description_en:en,trick_hint:trickHint,hint_scope:['ACE_OFF_TRUMP','KING_SINGLETON','KING_WITH_LOW','QUEEN_TWO_LOW','QUEEN_SHORT','JACK_THREE_LOW','TEN_FOUR_LOW'].includes(id)?'HIGHEST_CARD_CAPTURE':'SUIT_TRICKS',status,additive:false}));
function card(code,players){const match=String(code).match(/^(A|K|Q|J|10|[2-9])([SHCD])$/);assert.ok(match,'Invalid card '+code);const rank=ranks[match[1]]??Number(match[1]);assert.ok(rank>=15-2*players,'Card outside reduced Romanian deck');return {code,rank,suit:match[2]};}
export function describeHolding(codes,trump,players=4,handSize=codes.length,publicOut=[]){
 assert.ok(Number.isInteger(players)&&players>=3&&players<=6);assert.ok(Number.isInteger(handSize)&&handSize>=1&&handSize<=8);assert.ok(['S','H','C','D','NT'].includes(trump));assert.equal(new Set(codes).size,codes.length,'Duplicate held card');
 const own=codes.map(c=>card(c,players)),out=new Set(publicOut.map(c=>card(c,players).code));assert.ok(own.every(c=>!out.has(c.code)),'Held card is already public/out');
 const suits=[];
 for(const suit of 'SHCD'){
  const cards=own.filter(c=>c.suit===suit).sort((a,b)=>b.rank-a.rank),held=new Set(cards.map(c=>c.rank)),low=cards.filter(c=>c.rank<=10),top=cards[0]?.rank??0,ids=[];
  const honors=cards.filter(c=>c.rank>=11).map(c=>{const higher=[];for(let rank=c.rank+1;rank<=14;rank++){const code=(rank===14?'A':rank===13?'K':rank===12?'Q':rank===11?'J':rank)+suit;if(!held.has(rank)&&!out.has(code))higher.push(code);}
   return {card:c.code,unseenHigher:higher,smallCovers:low.filter(x=>x.rank<c.rank).length,higherHeld:cards.filter(x=>x.rank>c.rank).length,promotionPossible:low.filter(x=>x.rank<c.rank).length>=higher.length,forcedMasterTrump:suit===trump&&higher.length===0};});
  const masterTrumps=cards.filter(c=>{if(suit!==trump)return false;for(let r=c.rank+1;r<=14;r++){const code=(r===14?'A':r===13?'K':r===12?'Q':r===11?'J':r)+suit;if(!held.has(r)&&!out.has(code))return false;}return true;}).map(c=>c.code);
  if(masterTrumps.length)ids.push('MASTER_TRUMP');if(held.has(14)&&suit!==trump)ids.push('ACE_OFF_TRUMP');if(top===13&&cards.length===1)ids.push('KING_SINGLETON');if(top===13&&!held.has(12)&&!held.has(11)&&low.length>=1)ids.push('KING_WITH_LOW');if(top===13&&held.has(12)&&!held.has(11)&&low.length>=1)ids.push('KING_QUEEN_WITH_LOW');if(top===12&&!held.has(11)&&low.length>=2)ids.push('QUEEN_TWO_LOW');if(top===12&&low.length<2)ids.push('QUEEN_SHORT');if(top===12&&held.has(11)&&low.length>=2)ids.push('QUEEN_JACK_TWO_LOW');if(top===11&&low.length>=3)ids.push('JACK_THREE_LOW');if(top===10&&cards.filter(c=>c.rank<10).length>=4)ids.push('TEN_FOUR_LOW');if(top===13&&held.has(11)&&!held.has(12)&&low.length>=1)ids.push('KING_JACK_WITH_LOW');if(top===13&&held.has(12)&&held.has(11)&&low.length>=1)ids.push('KING_QUEEN_JACK_WITH_LOW');if(honors.length>=2&&honors.every(h=>h.promotionPossible))ids.push('PROMOTABLE_HONOR_GROUP');if(held.has(14)&&held.has(13))ids.push('ACE_KING');if(held.has(14)&&held.has(13)&&held.has(12))ids.push('ACE_KING_QUEEN');if(suit===trump&&cards.length>=3)ids.push('LONG_TRUMP');if(suit===trump&&top>0&&top<11)ids.push('LOW_TRUMP');if(suit!==trump&&!cards.length)ids.push('SIDE_VOID');if(trump==='NT'&&cards.length>=4)ids.push('LONG_NO_TRUMP_SUIT');
  suits.push({suit,cards:cards.map(c=>c.code),length:cards.length,smallCovers:low.length,honors,masterTrumps,patterns:ids,softHintsAreNotAdditive:true});
 }
 return {players,handSize,initialDealtCards:players*handSize,reducedDeckCards:players*8,dealtFraction:handSize/8,trump,suits,forcedMasterTrumpCount:suits.reduce((n,s)=>n+s.masterTrumps.length,0),limit:'Potential promotion and observed capture weights are not guaranteed tricks. Evaluate the joint holding and public auction.'};
}
export function toYaml(value,indent=0){
 const pad=' '.repeat(indent);if(Array.isArray(value))return value.map(item=>item&&typeof item==='object'?pad+'-\n'+toYaml(item,indent+2):pad+'- '+JSON.stringify(item)).join('\n');
 return Object.entries(value).map(([key,item])=>item&&typeof item==='object'?pad+key+':\n'+toYaml(item,indent+2):pad+key+': '+JSON.stringify(item)).join('\n');
}
export function catalog(){return {schema_version:1,status:'RESEARCH_CATALOG_NOT_RUNTIME_CONFIGURATION',generated_from:'whist-lab-app/ai-lab/tools/patterns/whist-hand-patterns.mjs',primary_objective:'EXACT_CONTRACT_PROBABILITY',scope:'Romanian Whist,3–6 players,1–8 cards; actual context controls every soft hint.',weight_semantics:'trick_hint is an initial whole-suit hint, not a calibrated win probability. Measured contextual weights live in external development-only calibration artifacts. No additive honor sum.',runtime_prior_influence:0,required_context:['trump_or_no_trump','initial_hand_size_and_dealt_fraction','higher_cards_held','higher_cards_publicly_out','higher_cards_unseen','low_covers_and_shared_sequences','public_voids_and_ruff_threats','lead_and_table_position','known_bid_prefix_during_bidding','all_contracts_and_remaining_need_during_play','already_over_or_unreachable_contract','last_bid_restriction','series5_or10_and_current_streak'],calibration_policy:{partition:'development_only',heldout:'never read for fitting',unit:'clustered initial full-world/seat/suit; repeated trajectories do not add independent evidence',prior_worlds:20,production_activation:'Requires a separate ablation and untouched evaluation; descriptive association is not causal strength.'},rules:PATTERNS};}
