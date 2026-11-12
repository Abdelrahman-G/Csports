import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const stagingDirectory = dirname(fileURLToPath(import.meta.url));
const fixture = readFileSync(join(stagingDirectory, '.env.example'), 'utf8')
  .replace('POSTGRES_PASSWORD=', 'POSTGRES_PASSWORD=isolated-test-password')
  .replace('APP_DB_PASSWORD=', 'APP_DB_PASSWORD=isolated-app-password')
  .replace('MIGRATION_DB_PASSWORD=', 'MIGRATION_DB_PASSWORD=isolated-migration-password')
  .replace('JWT_SECRET=', `JWT_SECRET=${'ab'.repeat(32)}`);

function config(environmentFile) {
  const temporaryDirectory = mkdtempSync(join(tmpdir(), 'csports-staging-config-'));
  try {
    const environmentPath = join(temporaryDirectory, '.env');
    writeFileSync(environmentPath, environmentFile);
    const environment = { ...process.env };
    // Shell variables take precedence over --env-file; keep this test reproducible.
    for (const key of Object.keys(environment)) {
      if (/^(COMPOSE_|POSTGRES_|REDIS_|APP_DB_|MIGRATION_DB_|BACKEND_PORT$|FRONTEND_PORT$|STAGING_|JWT_)/i.test(key)) {
        delete environment[key];
      }
    }
    return spawnSync('docker', [
      'compose', '--env-file', environmentPath,
      '-f', join(stagingDirectory, 'compose.yml'), 'config', '--format', 'json',
    ], { env: environment, encoding: 'utf8' });
  } finally {
    rmSync(temporaryDirectory, { recursive: true, force: true });
  }
}

test('staging keeps its data, images and internal services separate', () => {
  const result = config(fixture);
  assert.equal(result.status, 0, result.stderr);
  const model = JSON.parse(result.stdout);
  assert.equal(model.name, 'csports-staging');
  assert.deepEqual(Object.keys(model.services).sort(), ['backend', 'database-migrate', 'database-setup', 'frontend', 'postgres', 'redis']);
  assert.equal(model.services.frontend.image, 'csports-frontend:staging-local');
  assert.equal(model.services.backend.image, 'csports-backend:staging-local');
  assert.equal(model.services.frontend.build.context, resolve(stagingDirectory, '../../frontend'));
  assert.equal(model.services.backend.build.context, resolve(stagingDirectory, '../../backend'));
  assert.deepEqual(model.services.frontend.ports.map(({ host_ip, published, target }) =>
    [host_ip, published, target]), [['127.0.0.1', '3100', 80]]);
  for (const service of ['backend', 'database-migrate', 'database-setup', 'postgres', 'redis']) {
    assert.equal(model.services[service].ports?.length ?? 0, 0, `${service} must remain internal`);
  }
  assert.equal(model.services.backend.environment.SPRING_PROFILES_ACTIVE, 'prod');
  assert.equal(model.services.backend.environment.DATABASE_URL, 'jdbc:postgresql://postgres:5432/csports_staging');
  assert.equal(model.services.backend.environment.DATABASE_USERNAME, 'csports_app');
  assert.equal(model.services.backend.environment.DATABASE_PASSWORD, 'isolated-app-password');
  assert.equal(model.services.backend.environment.SPRING_FLYWAY_ENABLED, 'false');
  for (const key of ['SPRING_FLYWAY_USER', 'SPRING_FLYWAY_PASSWORD', 'SPRING_FLYWAY_URL', 'MIGRATION_DB_PASSWORD', 'PGPASSWORD']) {
    assert.equal(model.services.backend.environment[key], undefined);
  }
  assert.equal(Object.values(model.services.backend.environment).includes('isolated-migration-password'), false);
  assert.equal(model.services['database-migrate'].image, model.services.backend.image);
  assert.deepEqual(model.services['database-migrate'].environment, {
    DATABASE_URL: model.services.backend.environment.DATABASE_URL,
    DATABASE_USERNAME: 'csports_migrator',
    DATABASE_PASSWORD: 'isolated-migration-password',
  });
  assert.equal(model.services['database-migrate'].healthcheck.disable, true);
  assert.equal(model.services['database-migrate'].restart, 'no');
  assert.equal(model.services['database-migrate'].depends_on['database-setup'].condition, 'service_completed_successfully');
  assert.equal(model.services['database-setup'].environment.PGUSER, model.services.postgres.environment.POSTGRES_USER);
  assert.equal(model.services['database-setup'].environment.PGPASSWORD, model.services.postgres.environment.POSTGRES_PASSWORD);
  assert.equal(model.services.backend.depends_on['database-migrate'].condition, 'service_completed_successfully');
  assert.equal(model.services.backend.environment.POSTGRES_PASSWORD, undefined);
  assert.equal(model.services.backend.volumes?.length ?? 0, 0);
  assert.equal(model.services.backend.environment.JWT_SECRET, 'ab'.repeat(32));
  for (const name of ['postgres_data', 'redis_data']) {
    assert.equal(model.volumes[name].name, `csports-staging_${name}`);
    assert.equal(model.volumes[name].external ?? false, false);
  }
  for (const [service, target] of [['postgres', '/var/lib/postgresql/data'], ['redis', '/data']]) {
    assert.deepEqual(model.services[service].volumes.map(({ type, source, target }) =>
      [type, source, target]), [['volume', `${service}_data`, target]]);
  }
  for (const service of Object.values(model.services)) {
    assert.deepEqual(Object.keys(service.networks), ['default']);
    assert.equal(service.network_mode, undefined);
  }
  assert.deepEqual(Object.keys(model.networks), ['default']);
  assert.equal(model.networks.default.name, 'csports-staging_default');
  assert.equal(model.networks.default.external ?? false, false);
});

test('staging bounds memory and logs while preserving Redis persistence and security keys', () => {
  const result = config(fixture);
  assert.equal(result.status, 0, result.stderr);
  const { services } = JSON.parse(result.stdout);
  const limits = { backend: 1024, postgres: 512, redis: 128, frontend: 64, 'database-migrate': 512, 'database-setup': 128 };
  for (const [name, mebibytes] of Object.entries(limits)) {
    assert.equal(Number(services[name].mem_limit), mebibytes * 1024 * 1024, name);
    assert.deepEqual(services[name].logging, {
      driver: 'local', options: { 'max-size': '10m', 'max-file': '3' },
    }, name);
  }
  assert.deepEqual(services.redis.command, [
    'redis-server', '--appendonly', 'yes', '--maxmemory', '64mb', '--maxmemory-policy', 'noeviction',
  ]);
});

for (const variable of ['POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD', 'APP_DB_PASSWORD', 'MIGRATION_DB_PASSWORD', 'JWT_SECRET']) {
  test(`staging rejects an empty ${variable}`, () => {
    const result = config(fixture.replace(new RegExp(`^${variable}=.*$`, 'm'), `${variable}=`));
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, new RegExp(variable));
  });
}
