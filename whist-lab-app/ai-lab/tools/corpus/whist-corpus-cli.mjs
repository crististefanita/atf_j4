import {summarizeCorpus,exportDecisionExamples} from './whist-corpus.mjs';
import {replayArchive} from './whist-archive-replay.mjs';

const [command,input,output]=process.argv.slice(2);
try {
  if(command==='validate'&&input){const summary=await summarizeCorpus(input);console.log(JSON.stringify({status:'PASS',games:summary.games,distinctDeals:summary.distinctDeals,cards:summary.index.reduce((n,g)=>n+g.cards,0)},null,2));}
  else if(command==='replay'&&input){const {game,audit}=await replayArchive(input,output);console.log(`PASS ${game.id}: ${audit.hands} hands, ${audit.tricks} tricks, ${audit.cards} cards`);for(const h of game.hands)console.log(`Hand ${h.number}: ${h.size} cards, dealer ${h.dealer}, trump ${h.trump}; bids ${h.bids.map(b=>`${b.seat}:${b.bid}`).join(' ')}; won ${h.scores.map(s=>s.won).join(',')}; totals ${h.scores.map(s=>s.total).join(',')}`);}
  else if(command==='examples'&&input&&output)console.log(JSON.stringify(await exportDecisionExamples(input,output),null,2));
  else throw new Error('Usage: node whist-lab-app/ai-lab/tools/corpus/whist-corpus-cli.mjs validate <corpus-folder> | replay <game.json> <sha256> | examples <development-game.json> <new-output.jsonl>');
} catch(error){console.error(error.message);process.exitCode=1;}
