import fs from 'node:fs/promises';import {catalog,toYaml,describeHolding} from './whist-hand-patterns.mjs';
const [command,file]=process.argv.slice(2);
try{if(command==='catalog'&&file){await fs.writeFile(file,toYaml(catalog())+'\n');console.log('Generated research YAML catalog: '+file);}
else if(command==='describe'&&file){const input=JSON.parse(await fs.readFile(file));console.log(JSON.stringify(describeHolding(input.hand,input.trump,input.players,input.handSize,input.publicOut??[]),null,2));}
else throw new Error('Usage: whist-hand-patterns-cli.mjs catalog <generated-yaml> | describe <public-own-hand.json>');}catch(e){console.error(e.stack);process.exitCode=1;}
