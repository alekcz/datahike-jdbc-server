#!/usr/bin/env bash
chmod +x ./bin/run-integrationtests
docker-compose up -d
./bin/run-integrationtests
docker-compose down
