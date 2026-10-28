import { execFileSync, spawnSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const DOMAIN = 'prueba.somosmakers.com';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const CERTS_DIR = path.join(ROOT, 'certs');
const CERT_FILE = path.join(CERTS_DIR, `${DOMAIN}.pem`);
const KEY_FILE = path.join(CERTS_DIR, `${DOMAIN}-key.pem`);
const ENV_FILE = path.join(ROOT, '.env');
const HOSTS_ENTRY = `127.0.0.1 ${DOMAIN}`;
const HOSTS_FILE = process.platform === 'win32'
  ? path.join(process.env.SystemRoot ?? 'C:\\Windows', 'System32', 'drivers', 'etc', 'hosts')
  : '/etc/hosts';

function run(command, args) {
  return execFileSync(command, args, { cwd: ROOT, stdio: 'inherit' });
}

function commandExists(command, args = ['--version']) {
  const result = spawnSync(command, args, { stdio: 'ignore' });
  return !result.error && result.status === 0;
}

function canRunMkcert(command) {
  const result = spawnSync(command, ['-CAROOT'], { stdio: 'ignore' });
  return !result.error && result.status === 0;
}

function findMkcert() {
  if (canRunMkcert('mkcert')) return 'mkcert';

  const candidates = [];
  if (process.platform === 'win32') {
    const localAppData = process.env.LOCALAPPDATA ?? '';
    candidates.push(
      path.join(localAppData, 'Microsoft', 'WinGet', 'Links', 'mkcert.exe'),
      path.join(process.env.ChocolateyInstall ?? 'C:\\ProgramData\\chocolatey', 'bin', 'mkcert.exe'),
    );
    const wingetPackages = path.join(localAppData, 'Microsoft', 'WinGet', 'Packages');
    if (fs.existsSync(wingetPackages)) {
      for (const entry of fs.readdirSync(wingetPackages)) {
        if (entry.toLowerCase().includes('mkcert')) {
          candidates.push(path.join(wingetPackages, entry, 'mkcert.exe'));
        }
      }
    }
  } else {
    candidates.push('/opt/homebrew/bin/mkcert', '/usr/local/bin/mkcert', path.join(os.homedir(), '.local', 'bin', 'mkcert'));
  }

  return candidates.find((candidate) => fs.existsSync(candidate) && canRunMkcert(candidate)) ?? null;
}

function ensureMkcert() {
  const existing = findMkcert();
  if (existing) return existing;

  console.log('mkcert no está disponible; intentando instalarlo...');

  if (process.platform === 'win32' && commandExists('winget')) {
    try {
      run('winget', [
        'install',
        '--id',
        'FiloSottile.mkcert',
        '--exact',
        '--silent',
        '--accept-package-agreements',
        '--accept-source-agreements',
      ]);
    } catch {
      console.warn('winget no pudo completar la instalación; revisa el mensaje anterior.');
    }
  } else if (process.platform === 'darwin' && commandExists('brew')) {
    try {
      run('brew', ['install', 'mkcert']);
    } catch {
      console.warn('Homebrew no pudo completar la instalación; revisa el mensaje anterior.');
    }
  } else {
    throw new Error(
      'mkcert no está instalado. Instálalo y vuelve a ejecutar el arranque:\n' +
        '  Windows: winget install FiloSottile.mkcert\n' +
        '  macOS:   brew install mkcert\n' +
        '  Linux:   https://github.com/FiloSottile/mkcert#installation',
    );
  }

  const installed = findMkcert();
  if (!installed) {
    throw new Error('mkcert se instaló pero no se encontró en PATH. Cierra y abre la terminal, o instálalo manualmente.');
  }
  return installed;
}

function ensureCaTrusted(mkcert) {
  try {
    run(mkcert, ['-install']);
  } catch {
    console.warn('No se pudo confiar automáticamente en la CA local; el navegador puede mostrar una advertencia.');
  }
}

function certificateIsUsable() {
  try {
    if (!fs.existsSync(CERT_FILE) || !fs.existsSync(KEY_FILE)) return false;

    const certificate = new crypto.X509Certificate(fs.readFileSync(CERT_FILE));
    const now = Date.now();
    if (new Date(certificate.validFrom).getTime() > now) return false;
    if (new Date(certificate.validTo).getTime() - now < 24 * 60 * 60 * 1000) return false;

    const alternativeNames = (certificate.subjectAltName ?? '')
      .split(',')
      .map((name) => name.trim());
    if (!alternativeNames.includes(`DNS:${DOMAIN}`)) return false;

    const privateKey = crypto.createPrivateKey(fs.readFileSync(KEY_FILE));
    return certificate.checkPrivateKey(privateKey);
  } catch {
    return false;
  }
}

function generateCertificate(mkcert) {
  fs.mkdirSync(CERTS_DIR, { recursive: true, mode: 0o700 });
  run(mkcert, [
    '-cert-file',
    CERT_FILE,
    '-key-file',
    KEY_FILE,
    DOMAIN,
    'localhost',
    '127.0.0.1',
  ]);
}

function ensureEnvFile() {
  if (fs.existsSync(ENV_FILE)) return;

  const env = [
    '# Generado localmente. No commitear. Rotar antes de produccion.',
    'POSTGRES_DB=prestamos',
    'POSTGRES_USER=postgres',
    `POSTGRES_PASSWORD=${crypto.randomBytes(24).toString('base64url')}`,
    'APP_DB_USER=prestamos_app',
    `APP_DB_PASSWORD=${crypto.randomBytes(24).toString('base64url')}`,
    `JWT_SECRET=${crypto.randomBytes(48).toString('base64')}`,
    'JWT_ISSUER=somosmakers-prestamos',
    `CORS_ALLOWED_ORIGINS=http://localhost:4200,https://${DOMAIN}`,
    '',
  ].join('\n');

  fs.writeFileSync(ENV_FILE, env, { mode: 0o600 });
  console.log('Creado .env local con secretos aleatorios (ignorado por Git).');
}

function readHosts() {
  return fs.readFileSync(HOSTS_FILE, 'utf8');
}

function hasHostsEntry() {
  return readHosts()
    .split(/\r?\n/)
    .some((line) => line.replace(/#.*$/, '').trim().split(/\s+/).includes(DOMAIN));
}

function appendHostsEntry() {
  const content = readHosts();
  fs.appendFileSync(HOSTS_FILE, `${content.endsWith('\n') ? '' : '\n'}${HOSTS_ENTRY}\n`);
}

function elevateHostsEntry() {
  if (process.platform === 'win32') {
    const windowsPowerShell = spawnSync('powershell', ['-NoProfile', '-Command', 'exit 0'], { stdio: 'ignore' });
    if (windowsPowerShell.error || windowsPowerShell.status !== 0) {
      throw new Error(`Añade manualmente "${HOSTS_ENTRY}" a ${HOSTS_FILE} como administrador.`);
    }

    console.log('Se necesitan permisos de administrador para añadir el dominio a hosts. Se abrirá UAC...');
    const script = path.join(os.tmpdir(), `somosmakers-hosts-${process.pid}.ps1`);
    const content = [
      `$path = '${HOSTS_FILE.replace(/'/g, "''")}'`,
      `$line = '${HOSTS_ENTRY}'`,
      'if (-not (Select-String -Path $path -SimpleMatch $line -Quiet)) {',
      '  Add-Content -Path $path -Value $line',
      '}',
      '',
    ].join('\n');
    fs.writeFileSync(script, content, { mode: 0o600 });

    try {
      const command = `Start-Process powershell -Verb RunAs -Wait -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File','"${script.replace(/'/g, "''")}"'`;
      execFileSync('powershell', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', command], {
        stdio: 'inherit',
      });
    } catch {
      throw new Error(`No se pudo elevar para editar ${HOSTS_FILE}. Añade "${HOSTS_ENTRY}" manualmente y vuelve a intentar.`);
    } finally {
      fs.rmSync(script, { force: true });
    }
    return;
  }

  if (!commandExists('sudo')) {
    throw new Error(`Añade manualmente "${HOSTS_ENTRY}" a ${HOSTS_FILE} como administrador.`);
  }

  console.log('Se necesitan permisos de administrador para añadir el dominio a hosts. sudo pedirá la contraseña...');
  execFileSync(
    'sudo',
    ['sh', '-c', `grep -qxF '${HOSTS_ENTRY}' ${HOSTS_FILE} || printf '\\n%s\\n' '${HOSTS_ENTRY}' >> ${HOSTS_FILE}`],
    { stdio: 'inherit' },
  );
}

function ensureHostsEntry() {
  if (hasHostsEntry()) {
    console.log(`Dominio ya presente en ${HOSTS_FILE}.`);
    return;
  }

  try {
    appendHostsEntry();
    console.log(`Añadido ${HOSTS_ENTRY} a ${HOSTS_FILE}.`);
  } catch (error) {
    if (error.code !== 'EACCES' && error.code !== 'EPERM') throw error;
    elevateHostsEntry();
  }

  if (!hasHostsEntry()) {
    throw new Error(`No se pudo añadir "${HOSTS_ENTRY}" a ${HOSTS_FILE}. Añádelo manualmente y vuelve a intentar.`);
  }
}

export function ensureLocalHttps() {
  console.log(`\n==> Preparando HTTPS local para ${DOMAIN}`);
  ensureEnvFile();

  const mkcert = findMkcert();
  const reusable = certificateIsUsable();
  let regenerated = false;

  if (reusable) {
    console.log('Certificado existente válido: se reutiliza.');
    if (mkcert) ensureCaTrusted(mkcert);
  } else {
    const installedMkcert = mkcert ?? ensureMkcert();
    ensureCaTrusted(installedMkcert);
    console.log('Generando certificado local...');
    generateCertificate(installedMkcert);
    regenerated = true;
    if (!certificateIsUsable()) {
      throw new Error('El certificado se generó pero no es válido para el dominio. Revisa mkcert.');
    }
  }

  ensureHostsEntry();
  console.log(`HTTPS local listo: https://${DOMAIN}`);
  return { regenerated };
}
