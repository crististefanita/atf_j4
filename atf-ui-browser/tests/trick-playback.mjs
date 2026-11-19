import assert from 'node:assert/strict';
import {runInNewContext} from 'node:vm';

/** Shared shell contract promoted from the delivered Rentz AI/human handoff regression. */
export async function verifyTrickPlayback(shell) {
  const start = shell.indexOf('async function playCards(');
  const end = shell.indexOf('function applySeatFacts(', start);
  assert.ok(start >= 0 && end > start, 'The delivered playback boundary must exist');
  const source = shell.slice(start, end);
  const observations = [];
  const check = (name, expected, actual) => {
    assert.deepEqual(actual, expected, name);
    observations.push({name, expected, actual});
  };
for (const count of [3, 4, 5, 6]) for (const reduced of [false, true]) for (const humanLast of [false, true]) {
  const context = `${count}/${reduced}/${humanLast}`;
  const life = [], visible = [], seats = Array.from({length:count}, () => ({}));
  let collected, release;
  const done = new Promise(resolve => {release = resolve;});
  const center = {isConnected:true, replaceChildren() {visible.length = 0; life.push("clear");},
    append(card) {visible.push(card.seat); life.push("card:" + card.seat);}};
  const order = humanLast ? [...Array.from({length:count-1}, (_,i)=>i+1), 0]
    : Array.from({length:count}, (_,i)=>i);
  const cue = {cards:order.map(seat=>({seat})), holdMillis:1000, travelMillis:500,
    seatFacts:Array.from({length:count}, () => "after")};
  const steps = order.map((seat,index)=>({play:{seat,card:{rank:"J",suit:"C"}}, startsTrick:index===0,
    bids:"Levate",delayMillis:seat===0?0:550,seatFacts:seats.map(()=>"before"),
    collection:index===count-1?cue:null}));
  const vm = {stage:{dataset:{},querySelector:selector=>selector===".cards-on-table"?center:{},
      querySelectorAll:selector=>selector===".seat"?seats:[{dataset:{face:"JC"},remove(){life.push("hand-remove");}}]},
    matchMedia:()=>({matches:reduced}),placeTable:async()=>{},fitBidSummary:()=>{},
    markSeat:(node,active)=>{if(active) life.push("active:"+seats.indexOf(node));},
    applySeatFacts:()=>{},playedCard:play=>play,placePlayed:()=>{},
    state:{view:{seats}},setup:{},collectionFace:()=>{},
    requestAnimationFrame:callback=>callback(),setTimeout:(callback,delay)=>{life.push("wait:"+delay);callback();},
    collect:(_stage,value)=>{collected=value;life.push("collect");return {done};}};
  const run = runInNewContext("(" + source.trim() + ")", vm)(steps);
  for (let turn=0;turn<64 && !collected;turn++) await Promise.resolve();
  check(context+": all faces stay visible before collection", order, [...visible]);
  check(context+": exactly the accepted faces are collected", cue, collected);
  check(context+": next view cannot clear the held trick", 1, life.filter(x=>x==="clear").length);
  check(context+": every actor is marked before its card appears", true,
    order.every(seat=>life.indexOf("active:"+seat)<life.indexOf("card:"+seat)));
  check(context+": automated cards keep shared first-frame delay", count-1, life.filter(x=>x==="wait:550").length);
  check(context+": own card leaves the fan once", 1, life.filter(x=>x==="hand-remove").length);
  release(); await run;
  check(context+": table clears only after collection completes", 0, visible.length);
  check(context+": reduced motion still waits for collection completion", 2, life.filter(x=>x==="clear").length);
}

return {scenarios:16, observations};
}

/** The actual collection module must clone each public face at its measured origin. */
export async function verifyCollectionPoses(source) {
  const observations=[];
  for(const players of [3,4,5,6])for(const scale of [.75,1,1.25])for(const reduced of [false,true]){
    const poses=[],timers=new Map();let sequence=0;
    const box=seat=>({left:40+seat*25*scale,top:20+seat*12*scale,width:48*scale,height:70*scale});
    const face=seat=>({offsetWidth:48,offsetHeight:70,style:{},getBoundingClientRect:()=>box(seat),
      cloneNode(){return {...this,style:{},animate(frames,timing){poses.push({seat,style:{...this.style},frames,timing});return {cancel(){}};}};}});
    const faces=Array.from({length:players},(_,seat)=>face(seat));
    const table={getBoundingClientRect:()=>({left:40,top:20,width:300*scale,height:200*scale}),
      querySelector:selector=>faces[Number(/data-seat="(\d+)"/.exec(selector)[1])]};
    const seats=faces.map(value=>({getBoundingClientRect:value.getBoundingClientRect}));
    const stage={dataset:{},offsetWidth:414,scrollTop:120,
      getBoundingClientRect:()=>({left:10,top:8,width:414*scale,height:896*scale}),
      querySelectorAll:()=>seats,querySelector:()=>table,append(){}};
    const clock={setTimeout(callback,delay){timers.set(++sequence,{callback,delay});return sequence;},clearTimeout(id){timers.delete(id);}};
    const collect=runInNewContext(source.replaceAll('export function','function')+';collect',{
      matchMedia:()=>({matches:reduced}),document:{createElement:()=>({setAttribute(){},append(){},remove(){}})}});
    const cue={winningSeat:players-1,cards:faces.map((_,seat)=>({seat,card:{}})),holdMillis:1000,travelMillis:500,displayMillis:1500};
    const motion=collect(stage,cue,()=>{throw Error('A measured public face must be reused');},
      [{offset:0,travel:0,scale:1,alpha:1},{offset:1,travel:1,scale:.18,alpha:0}],clock);
    let settled=false;motion.done.then(()=>settled=true);await Promise.resolve();assert.equal(settled,false);
    assert.equal(timers.size,1);assert.equal([...timers.values()][0].delay,1500);
    assert.equal(poses.length,reduced?0:players);
    const target=box(players-1);
    for(const pose of poses){
      const original=box(pose.seat),left=Number.parseFloat(pose.style.left),top=Number.parseFloat(pose.style.top);
      assert.equal(left,(original.left+original.width/2-10)/scale);
      assert.equal(top,(original.top+original.height/2-8)/scale+120);
      assert.equal(pose.style.width,'48px');assert.equal(pose.style.height,'70px');
      const end=/translate\((-?[\d.]+)px, (-?[\d.]+)px\) scale/.exec(pose.frames.at(-1).transform);
      assert.ok(Math.abs(left+Number(end[1])-(target.left+target.width/2-10)/scale)<.0001);
      assert.ok(Math.abs(top+Number(end[2])-((target.top+target.height/2-8)/scale+120))<.0001);
      assert.deepEqual([pose.timing.delay,pose.timing.duration,pose.timing.fill],[1000,500,'both']);
    }
    [...timers.values()][0].callback();await motion.done;assert.equal(settled,true);assert.equal(timers.size,0);
    motion.cancel();assert.equal(timers.size,0);
    observations.push({players,scale,reduced,animatedFaces:poses.length,hold:1500,sourceFacesUnchanged:faces.every(value=>Object.keys(value.style).length===0)});
    assert.ok(observations.at(-1).sourceFacesUnchanged);
  }
  return {scenarios:24,observations};
}
