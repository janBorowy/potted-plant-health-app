# Sensor data generator

This Python 3 CLI emulates a sensor by sending every measurement through
`POST /api/measurements` with the sensor key in the `X-API-Key` header. It does
not connect to or write to the database. No third-party Python packages are
required.

## Parameters

| Option | Description |
| --- | --- |
| `--base-url` | Required API base URL, for example `http://localhost:8080`. The generator appends `/api/measurements`. |
| `--api-key` | Sensor API key. Alternatively set `SENSOR_API_KEY`; the key is never stored in the source. |
| `--mode` | Required: `batch` or `realtime`. |
| `--start-time`, `--end-time` | Required in batch mode. ISO-8601 timestamps with a timezone, such as `2026-01-01T00:00:00Z`. |
| `--samples` | Required in batch mode; positive number of measurements. Multiple timestamps are evenly spaced with both endpoints included. A single sample uses `start-time`. |
| `--interval` | Required in realtime mode; positive number of seconds between measurement submissions. The first measurement is sent immediately. |
| `--min-value`, `--max-value` | Required finite numeric bounds. Generated measurements are strictly inside this range. |
| `--signal-type` | `sinusoidal` (default) or `randomContinuous`. |
| `--variation` | Maximum absolute change for `randomContinuous`; defaults to `1.0`. Values are kept within the configured range. |
| `--period` | Sinusoidal cycle length in seconds in either mode; defaults to `60`. |

The sinusoid starts at its minimum at the first batch timestamp or when realtime
starts, and completes one full cycle every `--period` seconds. In batch mode,
timestamps outside that cycle continue through the same repeating waveform.
`randomContinuous` starts at a random in-range value and then applies a uniformly
random delta between `-variation` and `+variation` for each measurement.

## Examples

Send 24 example measurements over the past day:

```sh
python3 data-generator/sensor-data-generator.py \
  --base-url http://localhost:8080 \
  --api-key 'YOUR_SENSOR_API_KEY' \
  --mode batch \
  --start-time 2026-01-01T00:00:00Z \
  --end-time 2026-01-02T00:00:00Z \
  --samples 24 \
  --min-value 10 --max-value 40 \
  --signal-type sinusoidal --period 86400
```

Stream random-walk measurements every 5 seconds (stop with Ctrl+C):

```sh
export SENSOR_API_KEY='YOUR_SENSOR_API_KEY'
python3 data-generator/sensor-data-generator.py \
  --base-url http://localhost:8080 \
  --mode realtime --interval 5 \
  --min-value 10 --max-value 40 \
  --signal-type randomContinuous --variation 2
```

The API accepts timestamps from the past but rejects timestamps more than five
minutes in the future. For `batch` mode, choose an end time that is not in the
future. A failed HTTP request stops the generator and reports the API response.
