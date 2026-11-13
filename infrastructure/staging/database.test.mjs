import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const directory = dirname(fileURLToPath(import.meta.url));

test('a separate migration job gates startup and leaves the runtime connection restricted', { timeout: 780_000 }, async (t) => {
  const temporaryDirectory = mkdtempSync(join(tmpdir(), 'csports-db-test-'));
  const project = `csports-db-test-${randomBytes(5).toString('hex')}`;
  const passwords = {
    POSTGRES_PASSWORD: randomBytes(32).toString('hex'),
    APP_DB_PASSWORD: randomBytes(32).toString('hex'),
    MIGRATION_DB_PASSWORD: randomBytes(32).toString('hex'),
    JWT_SECRET: randomBytes(32).toString('hex'),
  };
  const environmentPath = join(temporaryDirectory, '.env');
  let fixture = readFileSync(join(directory, '.env.example'), 'utf8');
  for (const [key, value] of Object.entries(passwords)) {
    fixture = fixture.replace(new RegExp(`^${key}=.*$`, 'm'), `${key}=${value}`);
  }
  writeFileSync(environmentPath, fixture.replace('STAGING_FRONTEND_PORT=3100', 'STAGING_FRONTEND_PORT=0'));
  const environment = { ...process.env };
  for (const key of Object.keys(environment)) {
    if (/^(COMPOSE_|POSTGRES_|REDIS_|APP_DB_|MIGRATION_DB_|BACKEND_PORT$|FRONTEND_PORT$|STAGING_|JWT_)/i.test(key)) {
      delete environment[key];
    }
  }
  const composeArguments = ['compose', '--env-file', environmentPath, '-p', project, '-f', join(directory, 'compose.yml')];
  const compose = (...args) => spawnSync('docker', [...composeArguments, ...args], {
    encoding: 'utf8', env: environment, timeout: 720_000, maxBuffer: 8 * 1024 * 1024,
  });
  function success(result) {
    assert.equal(result.status, 0, result.stderr);
    return result.stdout.trim();
  }
  function query(role, sql, password = role === 'csports_app' ? passwords.APP_DB_PASSWORD
    : role === 'csports_migrator' ? passwords.MIGRATION_DB_PASSWORD : passwords.POSTGRES_PASSWORD) {
    // The Docker-network address requires password authentication, unlike the local Unix socket.
    return compose('exec', '-T', '-e', `PGPASSWORD=${password}`, 'postgres',
      'psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1', '-v', 'VERBOSITY=verbose',
      '-h', 'postgres', '-U', role, '-d', 'csports_staging', '-c', sql);
  }
  function denied(sql) {
    const result = query('csports_app', sql);
    assert.notEqual(result.status, 0, 'Runtime account unexpectedly accepted a forbidden statement');
    assert.match(result.stderr, /42501/, result.stderr);
  }

  try {
    const failureOverride = join(temporaryDirectory, 'failed-migration.yml');
    writeFileSync(failureOverride, 'services:\n  database-migrate:\n    environment:\n      DATABASE_PASSWORD: incorrect-password\n');
    const failedDeployment = compose('-f', failureOverride, 'up', '-d', '--build', '--wait', '--wait-timeout', '60');
    assert.notEqual(failedDeployment.status, 0);
    assert.match(success(compose('logs', '--no-color', 'database-migrate')), /password authentication failed/);
    const runningAfterFailure = success(compose('ps', '--status', 'running', '--services')).split(/\r?\n/);
    assert.equal(runningAfterFailure.includes('backend'), false);
    assert.equal(runningAfterFailure.includes('frontend'), false);
    assert.equal(success(query('csports_staging_admin',
      "SELECT to_regclass('public.flyway_schema_history') IS NULL;")), 't');

    success(compose('up', '-d', '--no-build', '--wait', '--wait-timeout', '180'));
    const migrationState = JSON.parse(success(compose('ps', '--all', '--format', 'json', 'database-migrate')));
    assert.equal(migrationState.State, 'exited');
    assert.equal(migrationState.ExitCode, 0);
    const memoryLimits = { backend: 1024, postgres: 512, redis: 128, frontend: 64, 'database-migrate': 512, 'database-setup': 128 };
    for (const [service, mebibytes] of Object.entries(memoryLimits)) {
      const id = success(compose('ps', '--all', '--quiet', service));
      const settings = JSON.parse(success(spawnSync('docker', ['inspect', '--format',
        '{"memory":{{.HostConfig.Memory}},"logging":{{json .HostConfig.LogConfig}},"oom":{{.State.OOMKilled}}}', id,
      ], { encoding: 'utf8', env: environment })));
      assert.equal(settings.memory, mebibytes * 1024 * 1024, service);
      assert.equal(settings.logging.Type, 'local', service);
      assert.equal(settings.logging.Config['max-size'], '10m', service);
      assert.equal(settings.logging.Config['max-file'], '3', service);
      assert.equal(settings.oom, false, service);
    }
    const redis = (...args) => success(compose('exec', '-T', 'redis', 'redis-cli', '--raw', ...args));
    assert.equal(redis('CONFIG', 'GET', 'maxmemory'), 'maxmemory\n67108864');
    assert.equal(redis('CONFIG', 'GET', 'maxmemory-policy'), 'maxmemory-policy\nnoeviction');
    assert.equal(redis('CONFIG', 'GET', 'appendonly'), 'appendonly\nyes');
    const backendEnvironment = success(compose('exec', '-T', 'backend', 'env')).split(/\r?\n/);
    assert.equal(backendEnvironment.includes('DATABASE_USERNAME=csports_app'), true);
    assert.equal(backendEnvironment.includes('SPRING_FLYWAY_ENABLED=false'), true);
    assert.equal(backendEnvironment.some((line) => line.includes(passwords.MIGRATION_DB_PASSWORD)), false);
    assert.equal(backendEnvironment.some((line) => /^(SPRING_FLYWAY_(USER|PASSWORD|URL)|MIGRATION_DB_PASSWORD|PGPASSWORD)=/.test(line)), false);
    const missingConfiguration = compose('run', '--rm', '--no-deps',
      '-e', 'DATABASE_PASSWORD=', 'database-migrate');
    assert.notEqual(missingConfiguration.status, 0);
    assert.match(missingConfiguration.stderr, /Missing required environment variable: DATABASE_PASSWORD/);
    assert.equal(success(query('csports_app', 'SELECT current_user;')), 'csports_app');
    const invalidLogin = query('csports_app', 'SELECT 1;', 'incorrect-password');
    assert.notEqual(invalidLogin.status, 0);
    assert.match(invalidLogin.stderr, /password authentication failed/);
    assert.equal(success(query('csports_staging_admin',
      'SELECT count(*) FROM public.flyway_schema_history WHERE success;')), '11');
    assert.equal(success(query('csports_staging_admin',
      'SELECT DISTINCT installed_by FROM public.flyway_schema_history;')), 'csports_migrator');
    assert.equal(success(query('csports_staging_admin',
      "SELECT pg_get_userbyid(extowner) FROM pg_extension WHERE extname = 'postgis';")), 'csports_staging_admin');
    assert.equal(success(query('csports_staging_admin',
      "SELECT EXISTS (SELECT FROM pg_stat_activity WHERE usename = 'csports_app');")), 't');
    assert.equal(success(query('csports_app',
      "SELECT rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls FROM pg_roles WHERE rolname = current_user;")), 'f');
    denied('BEGIN; CREATE TABLE public.forbidden_table (id int); ROLLBACK;');
    denied('BEGIN; ALTER TABLE public.users ADD COLUMN forbidden_column int; ROLLBACK;');
    denied('BEGIN; DROP TABLE public.users; ROLLBACK;');
    denied('BEGIN; TRUNCATE public.users CASCADE; ROLLBACK;');
    denied('SELECT * FROM public.flyway_schema_history;');
    denied('SET ROLE csports_migrator;');
    const forbiddenSetup = compose('run', '--rm', '--no-deps',
      '-e', 'PGUSER=csports_app', '-e', `PGPASSWORD=${passwords.APP_DB_PASSWORD}`, 'database-setup');
    assert.notEqual(forbiddenSetup.status, 0);
    assert.match(forbiddenSetup.stderr, /Provisioning requires the database administrator/);

    success(query('csports_migrator',
      'CREATE TABLE public.permission_probe (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, value text);'));
    assert.equal(success(query('csports_app',
      "INSERT INTO public.permission_probe (value) VALUES ('created') RETURNING id;")), '1');
    success(query('csports_app', "UPDATE public.permission_probe SET value = 'updated' WHERE id = 1;"));
    assert.equal(success(query('csports_app', 'SELECT value FROM public.permission_probe WHERE id = 1;')), 'updated');
    success(query('csports_app', 'DELETE FROM public.permission_probe WHERE id = 1;'));
    assert.equal(success(query('csports_app', 'SELECT count(*) FROM public.permission_probe;')), '0');
    success(query('csports_migrator', 'ALTER TABLE public.permission_probe ADD COLUMN extra int;'));

    // Simulate an existing administrator-owned table, then reapply setup without resetting data.
    success(query('csports_staging_admin',
      'CREATE TABLE public.permission_legacy_probe (id int); INSERT INTO public.permission_legacy_probe VALUES (7);'));
    success(compose('run', '--rm', '--no-deps', 'database-setup'));
    assert.equal(success(query('csports_app', 'SELECT id FROM public.permission_legacy_probe;')), '7');
    assert.equal(success(query('csports_staging_admin',
      "SELECT pg_get_userbyid(relowner) FROM pg_class WHERE oid = 'public.permission_legacy_probe'::regclass;")), 'csports_migrator');
    denied('SELECT * FROM public.flyway_schema_history;');

    const baseUrl = `http://${success(compose('port', 'frontend', '80'))}`;
    const email = 'permission.test@gmail.com';
    const password = randomBytes(16).toString('hex');
    const registration = await fetch(`${baseUrl}/api/v1/auth/register/user`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Permission Test', email, phoneNumber: '01000000198', password, age: 24 }),
    });
    assert.equal(registration.status, 200, await registration.text());
    const login = await fetch(`${baseUrl}/api/v1/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identifier: email, password }),
    });
    assert.equal(login.status, 200);
    const authentication = await login.json();
    const profile = await fetch(`${baseUrl}/api/v1/users/me`, {
      headers: { Authorization: `Bearer ${authentication.accessToken}` },
    });
    assert.equal(profile.status, 200);
    assert.equal((await profile.json()).email, email);

    async function jsonRequest(path, options = {}, expectedStatus = 200) {
      const response = await fetch(`${baseUrl}${path}`, options);
      const text = await response.text();
      assert.equal(response.status, expectedStatus, text);
      return JSON.parse(text);
    }
    const sports = await jsonRequest('/api/v1/sports/list');
    const regions = await jsonRequest('/api/v1/regions');
    const trainerEmail = 'resource.trainer@gmail.com';
    const trainerRegistration = await fetch(`${baseUrl}/api/v1/auth/register/trainer`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Resource Test Coach', email: trainerEmail,
        phoneNumber: '01000000298', password, age: 28, experienceYears: 4, sportId: sports[0].id }),
    });
    assert.equal(trainerRegistration.status, 200, await trainerRegistration.text());
    const trainerAuthentication = await jsonRequest('/api/v1/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identifier: trainerEmail, password }),
    });
    const trainerHeaders = { Authorization: `Bearer ${trainerAuthentication.accessToken}`, 'Content-Type': 'application/json' };
    const traineeHeaders = { Authorization: `Bearer ${authentication.accessToken}`, 'Content-Type': 'application/json' };
    const from = new Date(Date.now() + 7 * 86_400_000).toISOString().slice(0, 10);
    const to = new Date(Date.now() + 21 * 86_400_000).toISOString().slice(0, 10);
    const course = await jsonRequest('/api/v1/courses', {
      method: 'POST', headers: trainerHeaders,
      body: JSON.stringify({ title: 'Resource Limit Course', description: 'Disposable deployment smoke test',
        locationName: 'Practice court', regionId: regions[0].id, latitude: 30.05, longitude: 31.25,
        startDate: from, endDate: to, startTime: '18:00', days: ['SATURDAY'],
        durationMinutes: 60, maxParticipants: 3, price: 100 }),
    }, 201);
    const discovery = await jsonRequest('/api/v1/courses');
    assert.equal(discovery.content.some((item) => item.id === course.id), true);
    const booking = await jsonRequest(`/api/v1/bookings/${course.id}`, {
      method: 'POST', headers: traineeHeaders,
    }, 201);
    assert.equal(booking.bookingStatus, 'CONFIRMED');
    const schedule = await jsonRequest(`/api/v1/bookings/me/schedule?from=${from}&to=${to}`, { headers: traineeHeaders });
    assert.equal(schedule.entries.some((entry) => entry.courseId === course.id), true);
    const announcement = await jsonRequest(`/api/v1/courses/${course.id}/announcements`, {
      method: 'POST', headers: trainerHeaders,
      body: JSON.stringify({ title: 'Bring water', content: 'Remember your water bottle for practice.' }),
    }, 201);
    const noticeboard = await jsonRequest(`/api/v1/courses/${course.id}/announcements`, { headers: traineeHeaders });
    assert.equal(noticeboard.announcements.content.some((item) => item.id === announcement.id), true);
    const notifications = await jsonRequest('/api/v1/notifications', { headers: traineeHeaders });
    assert.equal(notifications.content.some((item) => item.announcementId === announcement.id), true);
    const logout = await fetch(`${baseUrl}/api/v1/auth/logout`, {
      method: 'POST', headers: traineeHeaders,
      body: JSON.stringify({ refreshToken: authentication.refreshToken }),
    });
    assert.equal(logout.status, 200, await logout.text());
    assert.equal((await fetch(`${baseUrl}/api/v1/users/me`, { headers: traineeHeaders })).status, 401);

    for (const service of ['backend', 'postgres', 'redis', 'frontend']) {
      const peak = Number(success(compose('exec', '-T', service, 'cat', '/sys/fs/cgroup/memory.peak')));
      t.diagnostic(`${service}: retained total-memory peak ${(peak / 1024 / 1024).toFixed(1)} MiB; configured cap ${memoryLimits[service]} MiB`);
    }
    const userCount = success(query('csports_staging_admin', 'SELECT count(*) FROM public.users;'));
    success(compose('run', '--rm', '--no-deps', 'database-migrate'));
    assert.equal(success(query('csports_staging_admin', 'SELECT count(*) FROM public.users;')), userCount);
    assert.equal(success(query('csports_staging_admin',
      'SELECT count(*) FROM public.flyway_schema_history WHERE success;')), '11');

    const checksum = success(query('csports_staging_admin',
      "SELECT checksum FROM public.flyway_schema_history WHERE version = '11';"));
    success(query('csports_staging_admin',
      "UPDATE public.flyway_schema_history SET checksum = checksum + 1 WHERE version = '11';"));
    try {
      const invalidHistory = compose('run', '--rm', '--no-deps', 'database-migrate');
      assert.notEqual(invalidHistory.status, 0);
      assert.match(invalidHistory.stderr, /checksum mismatch/i);
    } finally {
      success(query('csports_staging_admin',
        `UPDATE public.flyway_schema_history SET checksum = ${Number(checksum)} WHERE version = '11';`));
    }

    // Exercise memory pressure only in this disposable project, never in the user's staging Redis.
    assert.equal(redis('SET', 'resource-test:keep', 'retained', 'EX', '60'), 'OK');
    try {
      assert.equal(redis('CONFIG', 'SET', 'maxmemory', '1'), 'OK');
      assert.match(redis('SET', 'resource-test:blocked', 'new-value'), /^OOM /);
      assert.equal(redis('GET', 'resource-test:keep'), 'retained');
    } finally {
      assert.equal(redis('CONFIG', 'SET', 'maxmemory', '67108864'), 'OK');
      redis('DEL', 'resource-test:keep', 'resource-test:blocked');
    }
    assert.equal(redis('CONFIG', 'GET', 'maxmemory'), 'maxmemory\n67108864');
  } finally {
    // Only the random project created above is removed, including its disposable volumes.
    try { success(compose('down', '--volumes', '--remove-orphans')); }
    finally { rmSync(temporaryDirectory, { recursive: true, force: true }); }
  }
});
