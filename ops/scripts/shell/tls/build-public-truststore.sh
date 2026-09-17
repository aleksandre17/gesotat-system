#!/usr/bin/env bash
# Build a PKCS12 truststore that holds only PUBLIC CA/server certificates.
#
# A truststore contains no private material, so it carries no password: no
# certificate encryption and no MAC. Integrity comes from a read-only mount
# and the printed SHA-256. The JVM loads it with javax.net.ssl.trustStore and
# trustStoreType=PKCS12 and no trustStorePassword, so no credential exists to
# leak through JAVA_TOOL_OPTIONS, docker inspect or diagnostics.
#
# Usage: build-public-truststore.sh <output.p12> <alias>=<cert.pem> [...]
# Needs:  docker (keytool runs in a pinned JDK image; nothing installed on host)
set -euo pipefail

JDK_IMAGE="${JDK_IMAGE:-eclipse-temurin:17-jdk-jammy}"

usage() { echo "usage: $0 <output.p12> <alias>=<cert.pem> [<alias>=<cert.pem> ...]" >&2; exit 2; }
[[ $# -ge 2 ]] || usage

out="$(realpath -m "$1")"; shift
[[ ! -e "$out" ]] || { echo "refusing to overwrite existing $out" >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

imports=()
for pair in "$@"; do
  alias="${pair%%=*}"; cert="${pair#*=}"
  [[ -n "$alias" && -f "$cert" && "$alias" != "$pair" ]] || usage
  grep -q "BEGIN PRIVATE KEY\|BEGIN RSA PRIVATE KEY\|BEGIN EC PRIVATE KEY" "$cert" && {
    echo "refusing: $cert contains a private key" >&2; exit 1; }
  cp "$cert" "$work/$alias.pem"
  imports+=("$alias")
done

docker run --rm --user "$(id -u):$(id -g)" -v "$work:/work" --entrypoint bash "$JDK_IMAGE" -c '
set -euo pipefail
for alias in "$@"; do
  keytool -J-Dkeystore.pkcs12.certProtectionAlgorithm=NONE -J-Dkeystore.pkcs12.macAlgorithm=NONE \
    -importcert -noprompt -storetype PKCS12 -keystore /work/truststore.p12 -storepass unused \
    -alias "$alias" -file "/work/$alias.pem" >/dev/null
done
# Proof: the store opens without any password.
keytool -list -storetype PKCS12 -keystore /work/truststore.p12 </dev/null | grep -c trustedCertEntry
' _ "${imports[@]}"

install -m 0444 "$work/truststore.p12" "$out"
echo "truststore: $out"
sha256sum "$out"
