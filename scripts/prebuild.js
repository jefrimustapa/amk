const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const pkgPath = path.resolve(__dirname, '../package.json');
const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
const baseVersion = pkg.version || '1.0.0';

const isCI = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';

function getGitOutput(cmd) {
  try {
    return execSync(cmd, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch (e) {
    return '';
  }
}

function getTimestampBuildNumber() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const yyyy = d.getUTCFullYear();
  const mm = pad(d.getUTCMonth() + 1);
  const dd = pad(d.getUTCDate());
  const hh = pad(d.getUTCHours());
  const min = pad(d.getUTCMinutes());
  return `${yyyy}${mm}${dd}-${hh}${min}`;
}

let channel = 'dev';
let devName = 'local';
let branchName = 'main';
let buildNumber = getTimestampBuildNumber();
let versionName = baseVersion;
let fullVersionString = baseVersion;

if (!isCI) {
  channel = 'dev';
  const rawDev = process.env.DEV_NAME || 
                 getGitOutput('git config user.name') || 
                 process.env.USERNAME || 
                 process.env.USER || 
                 'dev';
  devName = rawDev.toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 12) || 'dev';
  const rawBranch = getGitOutput('git rev-parse --abbrev-ref HEAD') || 'main';
  branchName = rawBranch.toLowerCase().replace(/[^a-z0-9._-]/g, '-').slice(0, 20) || 'main';
  buildNumber = getTimestampBuildNumber();

  versionName = `${baseVersion}-dev.${devName}.${buildNumber}`;
  fullVersionString = `${baseVersion}-dev.${devName}.${branchName}.${buildNumber}`;
} else {
  const ciEvent = process.env.GITHUB_EVENT_NAME || '';
  const refName = process.env.GITHUB_REF_NAME || '';
  const runNumber = parseInt(process.env.GITHUB_RUN_NUMBER || '1', 10);
  buildNumber = getTimestampBuildNumber();

  if (process.env.BUILD_CHANNEL === 'nightly' || ciEvent === 'schedule') {
    channel = 'nightly';
    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
    versionName = `${baseVersion}-nightly.${dateStr}.${runNumber}`;
    fullVersionString = versionName;
  } else if (refName.includes('-rc.') || refName.includes('-rc')) {
    channel = 'rc';
    const cleanTag = refName.replace(/^v/, '');
    versionName = cleanTag;
    fullVersionString = cleanTag;
  } else if (refName.startsWith('v')) {
    channel = 'stable';
    const cleanTag = refName.replace(/^v/, '');
    versionName = cleanTag;
    fullVersionString = cleanTag;
  } else {
    channel = 'ci';
    versionName = `${baseVersion}-ci.${buildNumber}`;
    fullVersionString = versionName;
  }
}

function calculateVersionCode(version, buildNum) {
  const parts = version.split('-')[0].split('.');
  const major = parseInt(parts[0] || '1', 10);
  const minor = parseInt(parts[1] || '0', 10);
  const patch = parseInt(parts[2] || '0', 10);

  const digits = String(buildNum).replace(/\D/g, '');
  if (digits.length >= 8) {
    const timeCode = parseInt(digits.slice(2, 10), 10) || 1;
    return major * 100000000 + timeCode;
  }
  return major * 100000 + minor * 1000 + patch * 100 + (parseInt(buildNum, 10) % 1000);
}

const versionCode = calculateVersionCode(baseVersion, buildNumber);
const gitCommit = getGitOutput('git rev-parse --short HEAD') || 'head';

// Build info JSON for Gradle
const buildInfoDir = path.resolve(__dirname, '../app');
if (!fs.existsSync(buildInfoDir)) {
  fs.mkdirSync(buildInfoDir, { recursive: true });
}
const buildInfoPath = path.resolve(buildInfoDir, 'build-info.json');
const buildInfo = {
  version: baseVersion,
  versionName: versionName,
  versionCode: versionCode,
  fullVersionString: fullVersionString,
  channel: channel,
  devName: devName,
  branchName: branchName,
  buildNumber: buildNumber,
  gitCommit: gitCommit
};
fs.writeFileSync(buildInfoPath, JSON.stringify(buildInfo, null, 2) + '\n', 'utf8');

console.log(`[AMK Versioning] 🚀 Prepared build: ${fullVersionString} (Channel: ${channel}, Code: ${versionCode}, Git: ${gitCommit})`);
