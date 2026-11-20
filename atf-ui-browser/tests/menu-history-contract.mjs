import assert from 'node:assert/strict';

/** Contract portable to a consumer: entries originate in its native menu, callbacks in its adapter. */
export function verifyMenuEntries(bind, entries) {
  const calls=[],routes=[...entries].reverse().map(entry=>({...entry,action:()=>calls.push(entry.id)}));
  const bound=bind(routes,entries);
  assert.deepEqual(bound.map(item=>({id:item.id,label:item.label})),entries);
  assert.deepEqual(calls,[]);
  for(const item of bound)item.action();
  assert.deepEqual(calls,entries.map(entry=>entry.id));
  assert.equal(Object.isFrozen(bound),true);
  assert.throws(()=>bind(routes,[entries[0],entries[0]]),TypeError);
  assert.throws(()=>bind(routes,[{id:'missing',label:'Missing'}]),TypeError);
  assert.throws(()=>bind(routes,[{id:entries[0].id,label:' '}]),TypeError);
  return {entries:bound.length,callbacks:calls,labels:bound.map(item=>item.label),invalidPlansRefused:true};
}

export function verifyHistoryMeasurements(actual, policy) {
  assert.ok(actual.weights.length>0);
  assert.ok(actual.weights.every(weight=>String(weight)===policy['--history-heading-weight']));
  assert.ok(actual.dividers.every(width=>width===parseInt(policy['--history-divider-width'])));
  assert.ok(actual.colors.every(color=>color===policy['--history-divider-color']));
  assert.equal(actual.lastPlayerVisible,true);
  return {cells:actual.weights.length,dividers:actual.dividers.length,lastPlayerVisible:true};
}
