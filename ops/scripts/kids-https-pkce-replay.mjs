import fs from 'node:fs';
import crypto from 'node:crypto';
import { chromium } from 'playwright';

const base = process.env.KIDS_HTTPS_BASE ?? 'https://auth.geostat.internal';
const api = process.env.KIDS_API_BASE ?? 'https://api.geostat.internal/api/v1';
const tokenBase = process.env.KIDS_TOKEN_BASE ?? base;
const username = process.env.KIDS_TEST_USERNAME ?? 'kids-runtime-test';
const password = fs.readFileSync(process.env.KIDS_TEST_PASSWORD_FILE, 'utf8').trim();
const clientId = 'kids-runtime-test';
const redirectUri = 'http://localhost:18766/callback';
const b64 = value => value.toString('base64url');
const verifier = b64(crypto.randomBytes(32));
const challenge = b64(crypto.createHash('sha256').update(verifier).digest());
const auth = `${base}/realms/geostat/protocol/openid-connect/auth?client_id=${clientId}&response_type=code&scope=openid%20tenant&redirect_uri=${encodeURIComponent(redirectUri)}&code_challenge=${challenge}&code_challenge_method=S256`;

const callbackServer = await import('node:http').then(({ createServer }) => createServer((req, res) => {
  const callbackUrl = new URL(req.url, 'http://localhost');
  if (callbackUrl.pathname !== '/callback') {
    res.writeHead(404).end();
    return;
  }
  res.writeHead(200, { 'content-type': 'text/plain; charset=utf-8' }).end('PKCE callback received. You may close this tab.');
}));
callbackServer.listen(18766, '0.0.0.0');
const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--no-proxy-server', '--host-resolver-rules=MAP auth.geostat.internal 127.0.0.1,MAP api.geostat.internal 127.0.0.1'] });
const page = await browser.newPage();
await page.goto(auth, { waitUntil: 'domcontentloaded', timeout: 30000 });
await page.locator('input[name="username"]').fill(username);
await page.locator('input[name="password"]').fill(password);
const callbackPromise = new Promise((resolve, reject) => {
  const timer = setTimeout(() => reject(new Error('PKCE callback timed out')), 45000);
  callbackServer.once('request', req => {
    clearTimeout(timer);
    resolve(new URL(req.url, 'http://localhost').searchParams);
  });
});
await page.locator('input[type="submit"], #kc-login').first().click();
const callbackParams = await callbackPromise;
const code = callbackParams.get('code');
callbackServer.close();
if (!code) throw new Error(`PKCE callback error: ${callbackParams.get('error') ?? 'authorization code missing'}`);
await browser.close();

const tokenResponse = await fetch(`${tokenBase}/realms/geostat/protocol/openid-connect/token`, {
  method: 'POST',
  headers: { 'content-type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({ grant_type: 'authorization_code', client_id: clientId, code, redirect_uri: redirectUri, code_verifier: verifier }),
});
if (!tokenResponse.ok) throw new Error(`Token exchange failed: ${tokenResponse.status} ${await tokenResponse.text()}`);
const token = (await tokenResponse.json()).access_token;
if (!token) throw new Error('Token response did not contain access_token');

const headers = { authorization: `Bearer ${token}`, accept: 'application/json' };
const checks = [];
let page11Capabilities = null;
for (const path of ['/platform/contracts/KIDS_PORTAL_V1/revisions/8/introspection', '/platform/contracts/KIDS_PORTAL_V1/pages?revision=8', '/platform/contracts/KIDS_PORTAL_V1/pages/8/query-capabilities', '/platform/contracts/KIDS_PORTAL_V1/pages/11/query-capabilities?revision=8']) {
  const response = await fetch(api + path, { headers });
  if (path.includes('/pages/11/query-capabilities') && response.ok) page11Capabilities = await response.json();
  checks.push({ path, status: response.status, passed: response.status === 200 });
}
const page8 = await fetch(`${api}/platform/contracts/KIDS_PORTAL_V1/pages/8/query`, {
  method: 'POST', headers: { ...headers, 'content-type': 'application/json' },
  body: JSON.stringify({ filters: {}, sort: 'source_goal_id', descending: false, groupBy: [], aggregation: null, page: 1, limit: 5, select: ['source_goal_id', 'title_ka', 'title_en', 'path_ka', 'path_en', 'category_item_ref'], include: [], cursor: null, where: {}, orderBy: [], distinct: false, includeLimits: {} }),
});
checks.push({ path: '/platform/contracts/KIDS_PORTAL_V1/pages/8/query', status: page8.status, passed: page8.status === 200 });
const declaredIncludes = page11Capabilities?.capabilities?.allowedIncludes ?? [];
const page11Includes = ['INPUT_FOR_CARRIER', 'INPUT_RAW_LINEAGE'].filter(code => declaredIncludes.includes(code));
const page11Body = { filters: {}, sort: 'input_key', descending: false, groupBy: [], aggregation: null, page: 1, limit: 100, select: ['input_key', 'carrier_code', 'cell_ordinal', 'period_raw', 'period_normalized', 'dimension_key_raw', 'age_group_item_ref', 'value_lexical', 'value_decimal', 'json_path', 'source_encoding', 'source_row_key', 'operation'], include: page11Includes, cursor: null, where: {}, orderBy: [], distinct: false, includeLimits: {} };
const page11 = await fetch(`${api}/platform/contracts/KIDS_PORTAL_V1/pages/11/query`, {
  method: 'POST', headers: { ...headers, 'content-type': 'application/json' }, body: JSON.stringify(page11Body),
});
let page11Payload = null;
let page11Error = null;
if (page11.ok) page11Payload = await page11.json();
else page11Error = (await page11.text()).slice(0, 1200);
const page11Data = Array.isArray(page11Payload?.data) ? page11Payload.data : [];
const dataContainerKeys = page11Payload?.data && !Array.isArray(page11Payload.data) && typeof page11Payload.data === 'object' ? Object.keys(page11Payload.data).sort() : [];
const firstRow = page11Data.length ? page11Data[0] : null;
const firstRowKeys = firstRow && typeof firstRow === 'object' && !Array.isArray(firstRow) ? Object.keys(firstRow).sort() : [];
const page11Shape = Boolean(page11Payload && page11Payload.pageId === 11 && page11Payload.contractRevision === 8 && page11Payload.datasetCode === 'KIDS_STATISTICAL_INPUT' && Number(page11Payload.pagination?.returned) > 0 && Number(page11Payload.pagination?.returned) <= Number(page11Payload.pagination?.total) && Number(page11Payload.pagination?.total) > 0);
const includeContractPass = page11Includes.length === 2 && page11Includes.every(code => declaredIncludes.includes(code));
const rootProjectionPreserved = page11Data.length > 0 && firstRowKeys.length > 0;
const relationMatchCounts = Object.fromEntries(page11Includes.map(code => [code, page11Data.filter(row => { const value = row?.[code]; return Array.isArray(value) ? value.length > 0 : value !== null && typeof value === 'object'; }).length]));
const relationExpansionPass = page11Includes.length === 2 && page11Includes.every(code => firstRowKeys.includes(code) && relationMatchCounts[code] > 0);
checks.push({ path: '/platform/contracts/KIDS_PORTAL_V1/pages/11/query', status: page11.status, returned: page11Payload?.pagination?.returned ?? null, total: page11Payload?.pagination?.total ?? null, dataLength: page11Data.length, declaredIncludes: page11Body.include, firstRowKeys, relationMatchCounts, shapePass: page11Shape, includeContractPass, rootProjectionPreserved, relationExpansionPass, error: page11Error, passed: page11.ok && page11Shape && includeContractPass && rootProjectionPreserved && relationExpansionPass });
const evidence = { status: checks.every(check => check.passed) ? 'RUNTIME_REPLAY_PASS' : 'RUNTIME_REPLAY_FAIL', contractCode: 'KIDS_PORTAL_V1', revision: 8, pages: [8, 11], page11Capabilities: page11Capabilities ? { pageId: page11Capabilities.pageId, nodeCode: page11Capabilities.nodeCode, datasetCode: page11Capabilities.datasetCode, relations: page11Capabilities.capabilities?.relations?.map(({ code, fromDataset, fromField, toDataset, toField, cardinality, required }) => ({ code, fromDataset, fromField, toDataset, toField, cardinality, required })), allowedIncludes: page11Capabilities.capabilities?.allowedIncludes } : null, checks, anonymousBypass: false, kidsRefactoring: 'not-performed', tokenPersisted: false, certificateVerification: 'enabled-with-project-internal-ca-trust' };
const evidencePath = process.env.KIDS_EVIDENCE_PATH;
if (evidencePath) fs.writeFileSync(evidencePath, `${JSON.stringify(evidence, null, 2)}\n`, { mode: 0o600 });
console.log(JSON.stringify(evidence));
