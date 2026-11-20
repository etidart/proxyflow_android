#!/bin/bash
set -euo pipefail
cp -r proxyflow/internal proxyflow_wrapper/
find proxyflow_wrapper/internal -type f -exec sed -i 's/github.com\/etidart\/proxyflow\//github.com\/etidart\/proxyflow_android\/proxyflow_wrapper\//g' {} +
cd proxyflow_wrapper
go install golang.org/x/mobile/cmd/gomobile@latest
export PATH="$(go env GOPATH)/bin:$PATH"
go mod tidy
gomobile init
mkdir -p ../proxyflow_android/app/libs/
gomobile bind -target=android -androidapi=24 -tags=xray -ldflags="-checklinkname=0" -o ../proxyflow_android/app/libs/proxyflow.aar .
