#!/usr/bin/env python3

import argparse
import math
import os
import random
import sys
import time
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

SIGNAL_TYPES = ("sinusoidal", "randomContinuous")
REQUEST_TIMEOUT_SECONDS = 10


def parse_timestamp(value: str) -> datetime:
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as error:
        raise argparse.ArgumentTypeError(f"invalid ISO-8601 timestamp: {value}") from error

    if parsed.tzinfo is None or parsed.utcoffset() is None:
        raise argparse.ArgumentTypeError("timestamps must include a timezone, for example +00:00 or Z")
    return parsed.astimezone(timezone.utc)


def positive_float(value: str) -> float:
    try:
        parsed = float(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("must be a number") from error
    if not math.isfinite(parsed) or parsed <= 0:
        raise argparse.ArgumentTypeError("must be a finite number greater than zero")
    return parsed


def positive_int(value: str) -> int:
    try:
        parsed = int(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("must be an integer") from error
    if parsed <= 0:
        raise argparse.ArgumentTypeError("must be greater than zero")
    return parsed


def create_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate sensor measurements and submit them via POST /api/measurements."
    )
    parser.add_argument("--base-url", "--baseUrl", required=True, help="API base URL, e.g. http://localhost:8080")
    parser.add_argument(
        "--api-key",
        "--apiKey",
        default=None,
        help="sensor API key (can instead be supplied with SENSOR_API_KEY)",
    )
    parser.add_argument("--mode", choices=("batch", "realtime"), required=True)
    parser.add_argument("--start-time", "--startTime", type=parse_timestamp, help="batch start, ISO-8601 with timezone")
    parser.add_argument("--end-time", "--endTime", type=parse_timestamp, help="batch end, ISO-8601 with timezone")
    parser.add_argument("--samples", type=positive_int, help="number of measurements in batch mode")
    parser.add_argument(
        "--interval",
        type=positive_float,
        help="seconds between realtime measurements",
    )
    parser.add_argument("--min-value", "--minValue", type=float, required=True)
    parser.add_argument("--max-value", "--maxValue", type=float, required=True)
    parser.add_argument(
        "--signal-type",
        "--signalType",
        choices=SIGNAL_TYPES,
        default="sinusoidal",
        help="sinusoidal or randomContinuous",
    )
    parser.add_argument("--variation", type=float, default=1.0, help="maximum random-walk step size")
    parser.add_argument(
        "--period",
        dest="period",
        type=positive_float,
        default=60.0,
        help="sinusoidal period length in seconds, in either mode (default: 60)",
    )
    return parser


def validate_args(parser: argparse.ArgumentParser, args: argparse.Namespace) -> None:
    if not args.api_key:
        parser.error("--api-key is required unless SENSOR_API_KEY is set")
    if not args.api_key.strip():
        parser.error("API key cannot be blank")
    if not math.isfinite(args.min_value) or not math.isfinite(args.max_value):
        parser.error("invalid min-value or max-value")
    if args.min_value >= args.max_value:
        parser.error("min-value must be less than max-value")
    if not math.isfinite(args.variation) or args.variation < 0:
        parser.error("invalid variation")

    if args.mode == "batch":
        missing = [name for name, value in (
            ("--start-time", args.start_time),
            ("--end-time", args.end_time),
            ("--samples", args.samples),
        ) if value is None]
        if missing:
            parser.error(f"batch mode requires {', '.join(missing)}")
        if args.end_time < args.start_time:
            parser.error("--end-time must not be earlier than --start-time")
        if args.samples > 1 and args.end_time == args.start_time:
            parser.error("batch start and end times must differ when samples is greater than one")
    elif args.interval is None:
        parser.error("realtime mode requires --interval")


def interior_bounds(min_value: float, max_value: float) -> tuple[float, float]:
    return math.nextafter(min_value, max_value), math.nextafter(max_value, min_value)


def sinusoidal_value(
        min_value: float,
        max_value: float,
        phase: float,
) -> float:
    midpoint = min_value + (max_value - min_value) / 2
    amplitude = (max_value - min_value) / 2
    value = midpoint + amplitude * math.sin(phase)
    lower, upper = interior_bounds(min_value, max_value)
    return min(max(value, lower), upper)


def generate_value(
        signal_type: str,
        min_value: float,
        max_value: float,
        variation: float,
        random: random.Random,
        previous_value: float | None = None,
        phase: float = 0.0,
) -> float:
    if signal_type == "sinusoidal":
        return sinusoidal_value(min_value, max_value, phase)

    lower, upper = interior_bounds(min_value, max_value)
    if previous_value is None:
        return random.uniform(lower, upper)
    return min(max(previous_value + random.uniform(-variation, variation), lower), upper)


def measurement_url(base_url: str) -> str:
    return f"{base_url.rstrip('/')}/api/measurements"


def post_measurement(url: str, api_key: str, value: float, timestamp: datetime) -> None:
    payload = (
        f'{{"value":{value!r},"timestamp":"{timestamp.astimezone(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")}"}}'
    ).encode("utf-8")
    request = Request(
        url,
        data=payload,
        headers={"Content-Type": "application/json", "X-API-Key": api_key},
        method="POST",
    )
    with urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
        if response.status != 201:
            raise RuntimeError(f"API returned unexpected HTTP status {response.status}")


def submit_measurement(
        url: str,
        api_key: str,
        value: float,
        timestamp: datetime,
        index: int,
) -> None:
    try:
        post_measurement(url, api_key, value, timestamp)
    except HTTPError as error:
        details = error.read().decode("utf-8", errors="replace").strip()
        message = f"API rejected measurement {index} with HTTP {error.code}"
        if details:
            message += f": {details}"
        raise RuntimeError(message) from error
    except (TimeoutError, URLError) as error:
        raise RuntimeError(f"could not send measurement {index}: {error}") from error
    print(f"Sent measurement {index}: value={value:.8g}, timestamp={timestamp.isoformat()}")


def run_batch(args: argparse.Namespace, url: str) -> None:
    random = random.Random()
    previous_value = None
    count = args.samples
    duration = args.end_time - args.start_time

    for index in range(count):
        fraction = index / (count - 1) if count > 1 else 0
        timestamp = args.start_time + duration * fraction
        elapsed_seconds = (timestamp - args.start_time).total_seconds()
        phase = (2 * math.pi * elapsed_seconds / args.period) - math.pi / 2
        value = generate_value(
            args.signal_type,
            args.min_value,
            args.max_value,
            args.variation,
            random,
            previous_value,
            phase,
        )
        submit_measurement(url, args.api_key, value, timestamp, index + 1)
        previous_value = value


def run_realtime(args: argparse.Namespace, url: str) -> None:
    rng = random.Random()
    previous_value = None
    started_at = time.monotonic()
    index = 0
    next_send = started_at

    try:
        while True:
            delay = next_send - time.monotonic()
            if delay > 0:
                time.sleep(delay)

            timestamp = datetime.now(timezone.utc)
            elapsed = time.monotonic() - started_at
            phase = (2 * math.pi * elapsed / args.period) - math.pi / 2
            value = generate_value(
                args.signal_type,
                args.min_value,
                args.max_value,
                args.variation,
                rng,
                previous_value,
                phase,
            )
            index += 1
            submit_measurement(url, args.api_key, value, timestamp, index)
            previous_value = value
            next_send = time.monotonic() + args.interval
    except KeyboardInterrupt:
        print("\nStopped by user.", file=sys.stderr)


def main() -> int:
    parser = create_argument_parser()
    args = parser.parse_args()
    if args.api_key is None:
        args.api_key = os.environ.get("SENSOR_API_KEY")
    validate_args(parser, args)
    url = measurement_url(args.base_url)

    try:
        if args.mode == "batch":
            run_batch(args, url)
        else:
            run_realtime(args, url)
    except RuntimeError as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
