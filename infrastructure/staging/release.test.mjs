import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const directory = dirname(fileURLToPath(import.meta.url));
const workflow = readFileSync(resolve(directory, '../../.github/workflows/release.yml'), 'utf8');
const block = workflow.match(/      - name: Publish tested images and record their identifiers\r?\n[\s\S]*?        run: \|\r?\n([\s\S]*?)(?=\r?\n      - name:)/);
assert.ok(block, 'Release publishing step must exist');
const script = block[1].replace(/^ {10}/gm, '');
const bash = process.platform === 'win32' ? join(process.env.ProgramFiles, 'Git/bin/bash.exe') : 'bash';
const commit = 'c'.repeat(40);
const backendDigest = `sha256:${'a'.repeat(64)}`;
const frontendDigest = `sha256:${'b'.repeat(64)}`;

// Stub Docker so the real workflow shell runs without publishing images or requiring registry credentials.
const docker = String.raw`
docker() {
  case "$1" in
    tag|push) return 0 ;;
    image) printf 'docker.io/library/csports-backend@%s\n' "$TEST_BACKEND_DIGEST" ;;
    buildx)
      [[ "$2" == imagetools && "$3" == inspect &&
         "$5" == --format && "$6" == '{{.Manifest.Digest}}' ]] || return 2
      if [[ "$TEST_REGISTRY_FAILURE" == true ]]; then
        echo 'Registry lookup failed' >&2
        return 9
      fi
      case "$4" in
        "ghcr.io/abdelrahman-g/csports-backend:sha-$GITHUB_SHA") printf '%s\n' "$TEST_BACKEND_DIGEST" ;;
        "ghcr.io/abdelrahman-g/csports-frontend:sha-$GITHUB_SHA") printf '%s\n' "$TEST_FRONTEND_DIGEST" ;;
        *) return 2 ;;
      esac ;;
    *) return 2 ;;
  esac
}
`;

function publish(t, overrides = {}) {
  const root = resolve(tmpdir());
  const temporaryDirectory = mkdtempSync(join(root, 'csports-release-test-'));
  assert.equal(dirname(temporaryDirectory), root);
  t.after(() => rmSync(temporaryDirectory, { recursive: true, force: true }));
  const result = spawnSync(bash, ['--noprofile', '--norc', '-s'], {
    cwd: temporaryDirectory, input: `${docker}\n${script}`, encoding: 'utf8', timeout: 10_000,
    env: {
      ...process.env, BASH_ENV: '', GITHUB_REPOSITORY_OWNER: 'Abdelrahman-G', GITHUB_SHA: commit,
      GITHUB_STEP_SUMMARY: 'summary.md', TEST_BACKEND_DIGEST: backendDigest,
      TEST_FRONTEND_DIGEST: frontendDigest, TEST_REGISTRY_FAILURE: 'false', ...overrides,
    },
  });
  assert.equal(result.error, undefined, result.error?.message);
  return { result, manifest: join(temporaryDirectory, 'release-images.env') };
}

test('release records registry digests even when a local image digest names another repository', (t) => {
  const { result, manifest } = publish(t);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(readFileSync(manifest, 'utf8'),
    `RELEASE_COMMIT=${commit}\nBACKEND_IMAGE=ghcr.io/abdelrahman-g/csports-backend@${backendDigest}\nFRONTEND_IMAGE=ghcr.io/abdelrahman-g/csports-frontend@${frontendDigest}\n`);
});

for (const component of ['BACKEND', 'FRONTEND']) {
  test(`release refuses a malformed ${component.toLowerCase()} digest`, (t) => {
    const { result, manifest } = publish(t, { [`TEST_${component}_DIGEST`]: 'not-a-digest' });
    assert.notEqual(result.status, 0);
    assert.equal(existsSync(manifest), false);
  });
}

test('release stops when the registry lookup fails without writing a release manifest', (t) => {
  const { result, manifest } = publish(t, { TEST_REGISTRY_FAILURE: 'true' });
  assert.equal(result.status, 9);
  assert.match(result.stderr, /Registry lookup failed/);
  assert.equal(existsSync(manifest), false);
});
