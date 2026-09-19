#!/usr/bin/env python3
"""Creates and removes throwaway OIDC test clients in an existing realm, and
turns each one into a token request file the probe can use.

Runs ON the identity-provider host. It never receives, prints or returns a
client secret: a generated secret is read inside the provider container and
written straight into a 0600 token request file next to the probe. The realm
may be shared with other environments, so every action is bounded by a name
prefix: the script refuses to touch a client whose id does not carry it, and
`delete` verifies afterwards that nothing with the prefix is left.

Nothing here names a project, realm, claim or role: the spec file does.

  keycloak_test_clients.py --spec spec.json --action create --tokenreq-dir DIR
  keycloak_test_clients.py --spec spec.json --action verify
  keycloak_test_clients.py --spec spec.json --action delete
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys


class Admin:
    """Runs provider admin commands inside the provider container, so the
    administrator credential is read from the container's own environment and
    never appears in a command line, a log or this process."""

    def __init__(self, spec: dict) -> None:
        provider = spec["provider"]
        self.container = provider["container"]
        self.cli = provider["adminCli"]
        self.realm = provider["realm"]
        login = (f'{self.cli} config credentials --server {provider["adminServer"]}'
                 f' --realm {provider["adminRealm"]}'
                 f' --user "${provider["adminUserEnv"]}"'
                 f' --password "${provider["adminPasswordEnv"]}" >/dev/null')
        self.login = login

    def run(self, command: str, stdin: bytes | None = None, check: bool = True) -> str:
        argv = ["docker", "exec"]
        if stdin is not None:
            argv.append("-i")
        argv += [self.container, "sh", "-c", f"{self.login} && {command}"]
        done = subprocess.run(argv, input=stdin, capture_output=True)
        out = done.stdout.decode("utf-8", "replace")
        if check and done.returncode != 0:
            raise RuntimeError(f"admin command failed: {done.stderr.decode('utf-8', 'replace')[-400:]}")
        return out

    def put_file(self, path: str, document: dict) -> None:
        subprocess.run(["docker", "exec", "-i", self.container, "sh", "-c",
                        f"umask 077 && cat > {path}"],
                       input=json.dumps(document).encode(), capture_output=True, check=True)

    def client_id_of(self, client_id: str) -> str | None:
        out = self.run(f"{self.cli} get clients -r {self.realm} -q clientId={client_id}"
                       f" --fields id --format csv --noquotes")
        value = out.strip().splitlines()
        return value[-1].strip() if value and value[-1].strip() else None


def guard(client_id: str, prefix: str) -> None:
    if not client_id.startswith(prefix):
        raise SystemExit(f"refusing to act on '{client_id}': it does not carry the "
                         f"throwaway prefix '{prefix}'")


def create(admin: Admin, spec: dict, tokenreq_dir: str) -> list[dict]:
    prefix = spec["prefix"]
    os.makedirs(tokenreq_dir, mode=0o700, exist_ok=True)
    created = []
    for client in spec["clients"]:
        client_id = client["clientId"]
        guard(client_id, prefix)
        if admin.client_id_of(client_id):
            raise SystemExit(f"'{client_id}' already exists; refusing to modify an existing client")
        definition = {
            "clientId": client_id,
            "enabled": True,
            "protocol": "openid-connect",
            "publicClient": False,
            "serviceAccountsEnabled": True,
            "standardFlowEnabled": False,
            "directAccessGrantsEnabled": False,
            "defaultClientScopes": client.get("defaultClientScopes", []),
            "optionalClientScopes": [],
            "attributes": client.get("attributes", {}),
            "protocolMappers": client.get("protocolMappers", []),
            "description": spec.get("description", "throwaway security acceptance test client"),
        }
        admin.put_file("/tmp/zz-sectest-client.json", definition)
        admin.run(f"{admin.cli} create clients -r {admin.realm}"
                  f" -f /tmp/zz-sectest-client.json >/dev/null")
        internal = admin.client_id_of(client_id)
        if not internal:
            raise SystemExit(f"'{client_id}' was not created")
        for role in client.get("realmRoles", []):
            admin.run(f"{admin.cli} add-roles -r {admin.realm}"
                      f" --uusername service-account-{client_id.lower()} --rolename {role}")
        # The generated secret is read and consumed inside this step only.
        secret_json = admin.run(f"{admin.cli} get clients/{internal}/client-secret"
                                f" -r {admin.realm} --fields value")
        secret = json.loads(secret_json[secret_json.index("{"):]).get("value", "")
        if not secret:
            raise SystemExit(f"no secret was generated for '{client_id}'")
        path = os.path.join(tokenreq_dir, client_id + ".tokenreq")
        flags = os.O_WRONLY | os.O_CREAT | os.O_TRUNC
        handle = os.open(path, flags, 0o600)
        with os.fdopen(handle, "w") as out:
            out.write(f"grant_type=client_credentials&client_id={client_id}&client_secret={secret}")
        secret = ""
        created.append({"clientId": client_id, "tokenRequestFile": path,
                        "realmRoles": client.get("realmRoles", []),
                        "purpose": client.get("purpose", "")})
    subprocess.run(["docker", "exec", admin.container, "rm", "-f", "/tmp/zz-sectest-client.json"],
                   capture_output=True)
    return created


def delete(admin: Admin, spec: dict) -> list[dict]:
    prefix = spec["prefix"]
    removed = []
    for client in spec["clients"]:
        client_id = client["clientId"]
        guard(client_id, prefix)
        internal = admin.client_id_of(client_id)
        if internal:
            admin.run(f"{admin.cli} delete clients/{internal} -r {admin.realm}")
        removed.append({"clientId": client_id,
                        "existedBeforeDelete": bool(internal),
                        "existsAfterDelete": bool(admin.client_id_of(client_id))})
    return removed


def verify(admin: Admin, spec: dict) -> dict:
    prefix = spec["prefix"]
    out = admin.run(f"{admin.cli} get clients -r {admin.realm} --fields clientId")
    ids = [c["clientId"] for c in json.loads(out[out.index("["):])]
    return {"clientsWithPrefix": sorted(i for i in ids if i.startswith(prefix)),
            "totalClientsInRealm": len(ids)}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--spec", required=True)
    parser.add_argument("--action", required=True, choices=["create", "delete", "verify"])
    parser.add_argument("--tokenreq-dir")
    parser.add_argument("--out")
    args = parser.parse_args()

    with open(args.spec, "r", encoding="utf-8") as handle:
        spec = json.load(handle)
    admin = Admin(spec)
    if args.action == "create":
        if not args.tokenreq_dir:
            raise SystemExit("--tokenreq-dir is required for create")
        result = {"action": "create", "clients": create(admin, spec, args.tokenreq_dir)}
    elif args.action == "delete":
        result = {"action": "delete", "clients": delete(admin, spec),
                  "realmAfter": verify(admin, spec)}
    else:
        result = {"action": "verify", "realm": verify(admin, spec)}
    text = json.dumps(result, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(text)
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
