#!/bin/bash

set -euo pipefail

cd "$(dirname "$0")" || exit 1

sudo cp /opt/server/server.crt client/src/main/resources/
sudo cp /opt/server/truststore.p12 client/src/main/resources/
