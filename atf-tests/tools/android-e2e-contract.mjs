export const androidFlows = Object.freeze({
  'signed-release-smoke': 'WhistReleaseSmokeE2eTest#signedApkOpensItsRealHomeAndConfiguration',
  'signed-release-bidding': 'WhistReleaseBidE2eTest#signedRecommendationManualChoiceAndSavedContractFollowTheRealRomanianFlow',
  'signed-release-auto-game': 'WhistReleaseBidE2eTest#autoGamePlaysOnlyTheCurrentHandAndReturnsControlForTheNext',
  'history-release-sanity': 'WhistReleaseHomeE2eTest#completedHandHistoryResumesWithRomanianAndEnglishHeadings',
  'signed-release-achievements': 'WhistReleaseHomeE2eTest#signedAchievementsCatalogueOpensWithOrderedLocalProgress',
  'signed-release-home-profile': 'WhistReleaseHomeE2eTest#sharedHomePreservesLanguageProfileCancellationAndRestart',
  'responsive-ui-sanity': 'WhistReleaseBidE2eTest#signedReleaseKeepsTheMaximumHandVisibleAcrossPortraitProfiles'
});

export const signedE2eProtocol = 'ATF_J24_ANDROID_SIGNED_RELEASE_E2E';
export const signedExecutionProfileSince = '0.21.5';
const profilePattern = /^[1-9][0-9]{2,3}x[1-9][0-9]{2,3}@[1-9][0-9]{2}$/;

export function androidExecutionProfile(api, displayProfile, responsiveProfiles) {
  const matrix = typeof responsiveProfiles === 'string' ? responsiveProfiles.split(',') : [];
  if (!Number.isSafeInteger(api) || api < 1 || api > 999 || !profilePattern.test(displayProfile)
      || !matrix.length || matrix.length > 4 || new Set(matrix).size !== matrix.length
      || !matrix.every(profile => profilePattern.test(profile))) throw Error('ANDROID_E2E_INVALID_PROFILE');
  return {api, displayProfile, responsiveProfiles: matrix.join(',')};
}

export function scenarioFor(group, profile) {
  if (!Object.hasOwn(androidFlows, group)) throw Error('ANDROID_E2E_UNKNOWN_GROUP:' + group);
  return {selection: androidFlows[group], api: profile.api, displayProfile: profile.displayProfile,
    responsiveProfiles: group === 'responsive-ui-sanity' ? profile.responsiveProfiles : ''};
}

export function e2ePaths(group) {
  const root = 'reports/e2e/' + group;
  return {report: root + '/report.html', executionReceipt: root + '/execution.json'};
}

export function disposablePaths(leaseId) {
  const directory = /^[a-f0-9]{32}$/.test(leaseId ?? '') ? 'reports/e2e/isolation/' + leaseId : 'reports/e2e';
  return {readyReceipt: directory + '/data-isolation-ready.json', cleanupReceipt: directory + '/data-isolation-cleanup.json'};
}
