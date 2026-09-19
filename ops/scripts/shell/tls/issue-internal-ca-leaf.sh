#!/usr/bin/env bash
# Issue a server certificate for an internal hostname from a small internal CA,
# creating the CA on first use and reusing it afterwards.
#
# Why a CA and not another self-signed certificate: OpenSSL-based clients (curl,
# browsers, Go, Python) accept a self-issued certificate only when that exact
# certificate is in their truststore. Re-issuing a self-signed certificate — even
# with the same key and subject — therefore breaks every such client that already
# trusts the old one (proven: `error 18 at 0 depth lookup: self-signed
# certificate`). A CA is distributed once and then covers every future hostname,
# and issuing a new leaf never invalidates an existing trust anchor.
#
# The script never touches certificates it was not asked to write, so an existing
# edge certificate keeps working untouched. Private keys are created with mode
# 0600, never copied, never printed.
#
# It is idempotent: an existing CA is reused, and a leaf that already carries the
# requested SAN list, is signed by that CA, and is not near expiry is left alone.
#
# Usage:
#   issue-internal-ca-leaf.sh --dir <tls-dir> --ca-cn "<CA common name>" \
#       --san "DNS:host.example[,DNS:...,IP:...]" \
#       [--ca-cert ca.crt] [--ca-key ca.key] [--ca-days 1825] \
#       [--leaf-cert files.crt] [--leaf-key files.key] [--leaf-cn <cn>] \
#       [--leaf-days 397] [--key-type ec|rsa] [--curve prime256v1] \
#       [--rsa-bits 3072] [--force]
#
# The leaf certificate file is written as a chain: leaf first, then the CA, so a
# server can present it as-is. The CA certificate is public material and is the
# only file clients need to install.
#
# Environment fallbacks: TLS_DIR, CA_CN, LEAF_SAN.
#
# Needs: openssl 1.1.1+ (tested on 3.0), coreutils. No network, no docker.
set -euo pipefail

dir="${TLS_DIR:-}"
ca_cn="${CA_CN:-}"
san="${LEAF_SAN:-}"
ca_cert="ca.crt"; ca_key="ca.key"; ca_days=1825
leaf_cert=""; leaf_key=""; leaf_cn=""; leaf_days=397
key_type="ec"; curve="prime256v1"; rsa_bits=3072
force=0

die() { echo "issue-internal-ca-leaf: $*" >&2; exit 1; }
usage() { sed -n '3,33p' "$0" >&2; exit 2; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dir)        dir="${2:-}"; shift 2 ;;
    --ca-cn)      ca_cn="${2:-}"; shift 2 ;;
    --san)        san="${2:-}"; shift 2 ;;
    --ca-cert)    ca_cert="${2:-}"; shift 2 ;;
    --ca-key)     ca_key="${2:-}"; shift 2 ;;
    --ca-days)    ca_days="${2:-}"; shift 2 ;;
    --leaf-cert)  leaf_cert="${2:-}"; shift 2 ;;
    --leaf-key)   leaf_key="${2:-}"; shift 2 ;;
    --leaf-cn)    leaf_cn="${2:-}"; shift 2 ;;
    --leaf-days)  leaf_days="${2:-}"; shift 2 ;;
    --key-type)   key_type="${2:-}"; shift 2 ;;
    --curve)      curve="${2:-}"; shift 2 ;;
    --rsa-bits)   rsa_bits="${2:-}"; shift 2 ;;
    --force)      force=1; shift ;;
    -h|--help)    usage ;;
    *)            die "unknown argument: $1" ;;
  esac
done

[[ -n "$dir" && -n "$ca_cn" && -n "$san" ]] || usage
[[ -d "$dir" ]] || die "directory not found: $dir"
command -v openssl >/dev/null || die "openssl is required"
[[ "$ca_days"   =~ ^[0-9]+$ && "$ca_days"   -gt 0 ]] || die "--ca-days must be a positive integer"
[[ "$leaf_days" =~ ^[0-9]+$ && "$leaf_days" -gt 0 ]] || die "--leaf-days must be a positive integer"
[[ "$leaf_days" -le 397 ]] || die "--leaf-days must not exceed 397 (CA/Browser Forum maximum for server certificates)"
case "$key_type" in ec|rsa) ;; *) die "--key-type must be ec or rsa" ;; esac

# --- SAN list ---------------------------------------------------------------

normalise_san() {
  tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | sed '/^$/d' | LC_ALL=C sort -u
}
want_san="$(printf '%s' "$san" | normalise_san)"
[[ -n "$want_san" ]] || die "--san produced an empty list"
while read -r entry; do
  [[ "$entry" == *:* ]] || die "SAN entry must be typed (DNS:, IP:, URI:, email:): $entry"
done <<< "$want_san"

# The leaf common name defaults to the first DNS entry, so nothing is hardcoded.
if [[ -z "$leaf_cn" ]]; then
  leaf_cn="$(printf '%s' "$san" | tr ',' '\n' | tr -d ' ' | sed -n 's/^DNS://p' | head -n1)"
  [[ -n "$leaf_cn" ]] || die "--leaf-cn is required when the SAN list has no DNS entry"
fi
# Default file names follow the leaf common name's first label.
[[ -n "$leaf_cert" ]] || leaf_cert="${leaf_cn%%.*}.crt"
[[ -n "$leaf_key"  ]] || leaf_key="${leaf_cn%%.*}.key"

ca_cert="$dir/$ca_cert"; ca_key="$dir/$ca_key"
leaf_cert="$dir/$leaf_cert"; leaf_key="$dir/$leaf_key"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup() { [[ -e "$1" ]] || return 0; cp -p "$1" "$1.bak-$stamp"; echo "backup:        $1.bak-$stamp"; }

new_key() {
  if [[ "$key_type" == ec ]]; then
    openssl ecparam -name "$curve" -genkey -noout -out "$1" 2>/dev/null
  else
    openssl genrsa -out "$1" "$rsa_bits" 2>/dev/null
  fi
  chmod 600 "$1"
}

work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
umask 077

# --- 1. certificate authority (created once, then reused) -------------------

if [[ -f "$ca_cert" && -f "$ca_key" ]]; then
  [[ "$(openssl x509 -in "$ca_cert" -noout -pubkey)" == "$(openssl pkey -in "$ca_key" -pubout)" ]] \
    || die "existing CA key does not match $ca_cert"
  openssl x509 -in "$ca_cert" -noout -ext basicConstraints | grep -q 'CA:TRUE' \
    || die "existing $ca_cert is not a CA certificate"
  echo "ca:            reused $ca_cert"
elif [[ -f "$ca_cert" || -f "$ca_key" ]]; then
  die "incomplete CA in $dir: exactly one of $(basename "$ca_cert") / $(basename "$ca_key") exists"
else
  new_key "$work/ca.key"
  {
    echo '[req]'
    echo 'distinguished_name = dn'
    echo 'prompt = no'
    echo '[dn]'
    echo 'CN = placeholder'
    echo '[v3_ca]'
    echo 'basicConstraints = critical,CA:TRUE,pathlen:0'
    echo 'keyUsage = critical,keyCertSign,cRLSign'
    echo 'subjectKeyIdentifier = hash'
    echo 'authorityKeyIdentifier = keyid:always'
  } > "$work/ca.cnf"
  openssl req -x509 -new -sha256 -key "$work/ca.key" -subj "/CN=$ca_cn" -days "$ca_days" \
    -config "$work/ca.cnf" -extensions v3_ca -out "$work/ca.crt" 2>/dev/null \
    || die "could not create the CA certificate"
  install -m 600 "$work/ca.key" "$ca_key"
  install -m 644 "$work/ca.crt" "$ca_cert"
  echo "ca:            created $ca_cert (CN=$ca_cn, $ca_days days, $key_type)"
fi
echo "ca sha256:     $(openssl x509 -in "$ca_cert" -noout -fingerprint -sha256 | sed 's/.*=//')"

# --- 2. leaf: skip when the deployed one already satisfies the request -------

leaf_ok=0
if [[ -f "$leaf_cert" && -f "$leaf_key" && $force -eq 0 ]]; then
  have_san="$(openssl x509 -in "$leaf_cert" -noout -ext subjectAltName 2>/dev/null \
    | sed -n '2,$p' | tr -d ' ' | sed 's/IPAddress:/IP:/g' | normalise_san || true)"
  same_key="$( [[ "$(openssl x509 -in "$leaf_cert" -noout -pubkey)" == "$(openssl pkey -in "$leaf_key" -pubout)" ]] && echo yes || echo no )"
  chains="$(openssl verify -CAfile "$ca_cert" "$leaf_cert" >/dev/null 2>&1 && echo yes || echo no)"
  fresh="$(openssl x509 -in "$leaf_cert" -noout -checkend $((30*86400)) >/dev/null 2>&1 && echo yes || echo no)"
  [[ "$have_san" == "$want_san" && "$same_key" == yes && "$chains" == yes && "$fresh" == yes ]] && leaf_ok=1
fi

if [[ $leaf_ok -eq 1 ]]; then
  echo "leaf:          unchanged ($leaf_cert already carries the requested SAN list, chains to the CA and is not near expiry)"
  echo "leaf sha256:   $(openssl x509 -in "$leaf_cert" -noout -fingerprint -sha256 | sed 's/.*=//')"
  echo "result: unchanged"
  exit 0
fi

new_key "$work/leaf.key"
{
  echo '[v3_leaf]'
  echo 'basicConstraints = critical,CA:FALSE'
  echo 'keyUsage = critical,digitalSignature,keyEncipherment'
  echo 'extendedKeyUsage = serverAuth'
  echo 'subjectKeyIdentifier = hash'
  echo 'authorityKeyIdentifier = keyid,issuer'
  echo "subjectAltName = $(echo "$want_san" | paste -sd, -)"
} > "$work/leaf.cnf"

openssl req -new -sha256 -key "$work/leaf.key" -subj "/CN=$leaf_cn" -out "$work/leaf.csr" 2>/dev/null \
  || die "could not create the leaf CSR"
openssl x509 -req -sha256 -in "$work/leaf.csr" \
  -CA "$ca_cert" -CAkey "$ca_key" -CAcreateserial -CAserial "$dir/$(basename "$ca_cert" .crt).srl" \
  -days "$leaf_days" -extfile "$work/leaf.cnf" -extensions v3_leaf \
  -out "$work/leaf.crt" 2>/dev/null \
  || die "could not sign the leaf certificate"

# --- proofs, before anything is installed -----------------------------------

[[ "$(openssl x509 -in "$work/leaf.crt" -noout -pubkey)" == "$(openssl pkey -in "$work/leaf.key" -pubout)" ]] \
  || die "leaf key does not match the issued leaf certificate"
verify_out="$(openssl verify -CAfile "$ca_cert" "$work/leaf.crt" 2>&1)" \
  || { echo "$verify_out" >&2; die "openssl verify FAILED; nothing was installed"; }
echo "openssl verify -CAfile <ca> <leaf>: $verify_out"

# Serve leaf + issuer, so a client that trusts only the CA completes the chain.
cat "$work/leaf.crt" "$ca_cert" > "$work/leaf.chain.crt"

# --- install ----------------------------------------------------------------

backup "$leaf_cert"; backup "$leaf_key"
install -m 600 "$work/leaf.key" "$leaf_key"
install -m 644 "$work/leaf.chain.crt" "$leaf_cert"

echo "leaf:          $leaf_cert (chain: leaf + CA), key $leaf_key (0600)"
openssl x509 -in "$leaf_cert" -noout -subject -serial -startdate -enddate -ext subjectAltName | tr -d '\r'
echo "leaf sha256:   $(openssl x509 -in "$leaf_cert" -noout -fingerprint -sha256 | sed 's/.*=//')"
echo "result: issued"
