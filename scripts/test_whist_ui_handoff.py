import copy
import json
from pathlib import Path
import tempfile
import unittest
from whist_ui_handoff import publish, publish_pointers
from whist_ui_replay import replay


class WhistHandoffTest(unittest.TestCase):
    def setUp(self):
        self.trace = {"schema":"j4.public-romanian-trace/1", "hands":[{
            "hand":1, "size":1, "dealer":3, "trump":"HEARTS", "turnedCard":["HEARTS",14],
            "seriesEnabled":True, "seriesReward":10, "beforeTotals":[0]*4, "beforeStreaks":[0]*4,
            "observerAtBid":{"seat":0,"ownCards":[["CLUBS",8]],"priorBids":[]},
            "exPost":{"bids":[[0,0],[1,0],[2,0],[3,0]],
                      "plays":[[0,"CLUBS",8],[1,"CLUBS",10],[2,"HEARTS",2],[3,"DIAMONDS",12]],
                      "winners":[2],"tricksWon":[0,0,1,0],"scores":[5,5,-1,5],"seriesPoints":[0]*4,"streaks":[0]*4}}]}

    def test_follow_and_trump_winner_and_negative_score(self):
        self.assertEqual(replay(self.trace), {"hands":1,"tricks":1,"moves":4,"status":"PASS"})

    def test_rejects_wrong_actor_winner_score_and_future_information(self):
        for key, value in (("scores",[5,5,1,5]),("winners",[1]),("plays",[[1,"CLUBS",8]]*4),("bids",[[0,1],[1,0],[2,0],[3,0]])):
            with self.subTest(key=key):
                altered=copy.deepcopy(self.trace)
                altered["hands"][0]["exPost"][key]=value
                with self.assertRaises(ValueError):
                    replay(altered)
        self.trace["hands"][0]["observerAtBid"]["priorBids"]=[[1,0]]
        with self.assertRaisesRegex(ValueError,"Future"):
            replay(self.trace)

    def test_later_failure_and_older_republication_preserve_the_accepted_scope(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory=Path(tmp)
            common={"scenario":"Pilot","scope":"hard-pilot","htmlUri":"file:///report.html",
                    "metadataUri":"file:///meta.json","verdict":{"native":"PASS","journey":"PASS","publication":"PASS"}}
            failed=dict(common, runId="b", endedAt="2026-11-04T06:00:00+00:00", accepted=False)
            passed=dict(common, runId="a", endedAt="2026-11-04T05:00:00+00:00", accepted=True)
            publish_pointers(directory,failed)
            self.assertIsNone(json.loads((directory/"latest-accepted.json").read_text()))
            for candidate in (passed,failed,passed):
                publish_pointers(directory,candidate)
            self.assertEqual(json.loads((directory/"latest-attempt.json").read_text())["runId"],"b")
            self.assertEqual(json.loads((directory/"latest-accepted.json").read_text())["runId"],"a")

    def test_unexecuted_attempt_has_no_html_no_acceptance_and_no_stale_lock(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            spec=dict(runId="lease-refused", sourceRunId="launcher-1", scope="pilot", package="ro.whistlab.app",
                      attemptStatus="NOT_RUN", reason="device owned", scenario="Pilot", requestedScope="6 hands",
                      effectiveScope="0 hands", profile={}, endedAt="2026-11-04T05:00:00+00:00",
                      timingBasis="launcher receipt", failureStage="ADMISSION", limits=[])
            record=publish(root,spec)
            self.assertIsNone(record["htmlUri"])
            self.assertEqual(publish(root,spec), record)
            with self.assertRaisesRegex(ValueError,"Immutable"):
                publish(root,dict(spec,reason="different"))
            self.assertFalse((root/"whist-lab/.publish.lock").exists())
            self.assertIsNone(json.loads((root/"whist-lab/scopes/pilot/latest-accepted.json").read_text()))


if __name__ == "__main__":
    unittest.main()
