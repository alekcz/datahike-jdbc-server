#!/usr/bin/env bash
docker-compose up -d
clj -T:build clean
clj -M:run
docker-compose down