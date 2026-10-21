#!/usr/bin/env bash

# batch insert 3 sensor measurement data with timestamps from (now - 1 hour) to (now)

set -euo pipefail

usage() {
  echo "Usage: $0 <api-url> <sensor-1-api-key> <sensor-2-api-key> <sensor-3-api-key>" >&2
}

if [[ $# -ne 4 ]]; then
  usage
  exit 2
fi

api_url=$1
sensor_1_key=$2
sensor_2_key=$3
sensor_3_key=$4

if [[ -z "$api_url" || -z "$sensor_1_key" || -z "$sensor_2_key" || -z "$sensor_3_key" ]]; then
  echo "API URL and all three API keys must be non-empty." >&2
  usage
  exit 2
fi

if [[ "$sensor_1_key" == "$sensor_2_key" || "$sensor_1_key" == "$sensor_3_key" || "$sensor_2_key" == "$sensor_3_key" ]]; then
  echo "The three sensor API keys must be distinct." >&2
  exit 2
fi

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
generator="$script_dir/sensor-data-generator.py"
now_epoch=$(date -u +%s)
start_time=$(date -u -d "@$((now_epoch - 3600))" '+%Y-%m-%dT%H:%M:%SZ')
end_time=$(date -u -d "@$now_epoch" '+%Y-%m-%dT%H:%M:%SZ')

python3 "$generator" \
  --base-url "$api_url" --api-key "$sensor_1_key" --mode batch \
  --start-time "$start_time" --end-time "$end_time" --samples 30 \
  --min-value -20 --max-value 100 --signal-type sinusoidal --period 360 &
sensor_1_pid=$!

python3 "$generator" \
  --base-url "$api_url" --api-key "$sensor_2_key" --mode batch \
  --start-time "$start_time" --end-time "$end_time" --samples 30 \
  --min-value 0 --max-value 6000 --signal-type randomContinuous --variation 100 &
sensor_2_pid=$!

python3 "$generator" \
  --base-url "$api_url" --api-key "$sensor_3_key" --mode batch \
  --start-time "$start_time" --end-time "$end_time" --samples 30 \
  --min-value 0 --max-value 100 --signal-type randomContinuous --variation 5 &
sensor_3_pid=$!

exit_status=0
for pid in "$sensor_1_pid" "$sensor_2_pid" "$sensor_3_pid"; do
  if ! wait "$pid"; then
    exit_status=1
  fi
done

exit "$exit_status"
