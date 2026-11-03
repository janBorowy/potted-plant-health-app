#!/usr/bin/env bash

set -euo pipefail

usage() {
  echo "Usage: $0 <api-url> <api-key>" >&2
}

if [[ $# -ne 2 || -z "$1" || -z "$2" ]]; then
  usage
  exit 2
fi

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)

python3 "$script_dir/sensor-data-generator.py" \
  --base-url "$1" \
  --api-key "$2" \
  --mode realtime \
  --interval 1 \
  --min-value -50 \
  --max-value 100 \
  --signal-type sinusoidal \
  --period 7