"""Quick authenticated COMSOL server connectivity check.

Run after starting ``comsolmphserver``. This validates the same client path used
by the MCP server's Python dependency, so it is a stronger check than seeing a
listening TCP port.
"""

from __future__ import annotations

import argparse
import time

import mph


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", type=int, default=2036)
    args = parser.parse_args()

    started = time.time()
    client = mph.Client(host=args.host, port=args.port)
    elapsed = time.time() - started

    print(f"Connected to COMSOL {client.version} at {args.host}:{args.port}")
    print(f"Connection time: {elapsed:.2f} s")
    print(f"Loaded models: {client.names()}")

    try:
        client.disconnect()
    except Exception:
        pass


if __name__ == "__main__":
    main()
