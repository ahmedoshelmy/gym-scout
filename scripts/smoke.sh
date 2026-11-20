#!/usr/bin/env bash
# End-to-end check against a running stack (default: the compose web proxy on :8091).
# Health, scout near a Cairo point (seeded gyms ordered by distance), log two sessions -> a PR,
# progress shows it, membership cost per visit.
set -euo pipefail
BASE="${1:-http://localhost:8091}"
command -v jq >/dev/null || { echo "jq is required"; exit 2; }

post() { curl -fsS -X POST "$BASE$1" -H 'Content-Type: application/json' -d "$2"; }

curl -fsS "$BASE/healthz" >/dev/null
curl -fsS "$BASE/actuator/health" | jq -e '.status == "UP"' >/dev/null || { echo "FAIL api health"; exit 1; }
echo "health: web ok, api UP"

scout=$(curl -fsS "$BASE/api/scout?lat=30.0444&lon=31.2357&radius=5000&limit=10&visitsPerWeek=3")
n=$(jq '.results | length' <<<"$scout")
[ "$n" -ge 3 ] || { echo "FAIL scout returned $n gyms"; exit 1; }
echo "scout: $n gyms, top: $(jq -c '.results[0] | {name, score, reasons: .reasons[0:2]}' <<<"$scout")"

near=$(curl -fsS "$BASE/api/places/gyms?lat=30.0444&lon=31.2357&radius=5000&limit=20")
sorted=$(jq '[.[].distanceMeters] | . == sort' <<<"$near")
[ "$sorted" = "true" ] || { echo "FAIL nearby gyms not ordered by distance"; exit 1; }
echo "nearby: $(jq length <<<"$near") gyms ordered by distance ($(jq -c '[.[0:3][] | .distanceMeters | floor]' <<<"$near") m)"

gym=$(jq -r '.[0].gym.id' <<<"$near")
d1=$(date -d '-6 days' +%F); d2=$(date -d '-2 days' +%F)
post /api/training/sessions "{\"performedOn\":\"$d1\",\"sets\":[{\"exercise\":\"Squat\",\"weightKg\":80,\"reps\":5}]}" >/dev/null
second=$(post /api/training/sessions "{\"performedOn\":\"$d2\",\"gymId\":\"$gym\",\"sets\":[{\"exercise\":\"Squat\",\"weightKg\":90,\"reps\":5}]}")
prs=$(jq '.records | length' <<<"$second")
[ "$prs" -ge 1 ] || { echo "FAIL second session found no PR: $second"; exit 1; }
echo "session logged with PR: $(jq -c '.records[0] | {exercise, kind, value, previous}' <<<"$second")"

found=0
for _ in $(seq 1 20); do
  progress=$(curl -fsS "$BASE/api/training/progress?weeks=4")
  if jq -e '.recentRecords | any(.exercise == "Squat")' <<<"$progress" >/dev/null; then found=1; break; fi
  sleep 0.5
done
[ "$found" = 1 ] || { echo "FAIL progress has no Squat record"; exit 1; }
echo "progress: $(jq -c '.bests' <<<"$progress")"

# Explicit dates: the API's "today" is the server's clock (UTC in the container), not this shell's.
start=$(date -d '-10 days' +%F)
end=$(date -d '+20 days' +%F)
m=$(post /api/memberships "{\"gymId\":\"$gym\",\"planName\":\"Monthly\",\"priceMinor\":90000,\"currency\":\"EGP\",\"startsOn\":\"$start\",\"endsOn\":\"$end\"}")
mid=$(jq -r .membership.id <<<"$m")
post "/api/memberships/$mid/visits" "{\"on\":\"$d1\"}" >/dev/null
after=$(post "/api/memberships/$mid/visits" "{\"on\":\"$d2\"}")
visits=$(jq .stats.visits <<<"$after")
[ "$visits" -ge 1 ] || { echo "FAIL membership visits = $visits"; exit 1; }
echo "membership: $visits visits, cost per visit so far $(jq .stats.costPerVisitMinor <<<"$after") minor units"
echo "SMOKE PASS"
