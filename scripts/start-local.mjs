#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { ensureLocalHttps } from './lib/local-https.mjs';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

function ensureDockerCompose() {
  const result = spawnSync('docker', ['compose', 'version'], { stdio: 'ignore' });
  if (result.error || result.status !== 0) {
    throw new Error('Docker Compose no está disponible. Instala Docker Desktop y vuelve a intentar.');
  }
}

try {
  ensureDockerCompose();
  const { regenerated } = ensureLocalHttps();

  if (regenerated) {
    const restart = spawnSync('docker', ['compose', 'restart', 'frontend'], {
      cwd: ROOT,
      stdio: 'ignore',
    });
    if (restart.status === 0) {
      console.log('Certificado regenerado: frontend reiniciado para cargarlo.');
    }
  }

  console.log('\n==> Levantando el stack local...\n');

  const result = spawnSync('docker', ['compose', 'up', '--build', ...process.argv.slice(2)], {
    cwd: ROOT,
    stdio: 'inherit',
  });
  process.exit(result.status ?? 1);
} catch (error) {
  console.error(`\n[!] ${error instanceof Error ? error.message : error}`);
  process.exit(1);
}
