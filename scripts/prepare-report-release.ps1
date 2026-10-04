# Produces an offline upgrade bundle only; never connects to production.
param([string]$PreviousRelease = '38f8ed2')
$ErrorActionPreference = 'Stop'
$repoPath = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ($PreviousRelease -notmatch '^[a-f0-9]{7,40}$') { throw 'Invalid previous release tag' }
if (git -C $repoPath status --porcelain) { throw 'Commit reviewed changes before preparing a release' }
$revision = (git -C $repoPath rev-parse HEAD).Trim()
$release = $revision.Substring(0,7)
$jarPath = Join-Path $repoPath 'disciplinary_construction/target/disciplinary_construction-1.0-SNAPSHOT.jar'
$distPath = Join-Path $repoPath 'index/dist'
if (!(Test-Path $jarPath) -or !(Test-Path (Join-Path $distPath 'index.html'))) { throw 'Build and test backend and frontend first' }
$buildRoot = Join-Path $repoPath "_download/report-$release-build"
$backendBuild = Join-Path $buildRoot 'backend'
$frontendBuild = Join-Path $buildRoot 'frontend'
$bundlePath = Join-Path $repoPath "_download/mis-report-$release"
New-Item -ItemType Directory -Force -Path $backendBuild,$frontendBuild,$bundlePath | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $backendBuild 'app.jar')
Copy-Item -LiteralPath $distPath -Destination $frontendBuild -Recurse -Force
Copy-Item -LiteralPath (Join-Path $repoPath 'index/nginx.conf') -Destination $frontendBuild
foreach ($service in @('backend','frontend')) {
    $context = if ($service -eq 'backend') { $backendBuild } else { $frontendBuild }
    docker build --platform linux/amd64 --label "org.opencontainers.image.revision=$revision" --label "org.opencontainers.image.source=https://github.com/athatgirls/student-management-system" -f (Join-Path $PSScriptRoot "release-$service.Dockerfile") -t "mis-${service}:$release" $context
    if ($LASTEXITCODE) { throw "$service image build failed" }
}
$storageId = 'sha256:1803faef57627e2d9c2e7d89d655d712ddded5389040054987163043fecb6a3c'
if ((docker image inspect mis-rustfs:1.0.1 --format '{{.Id}}').Trim() -ne $storageId) { throw 'Load the reviewed amd64 RustFS 1.0.1 image first' }
docker save -o (Join-Path $bundlePath 'images.tar') "mis-backend:$release" "mis-frontend:$release" 'mis-rustfs:1.0.1'
if ($LASTEXITCODE) { throw 'Image export failed' }
$utf8 = [Text.UTF8Encoding]::new($false)
$overlay = @'
services:
  rustfs:
    image: mis-rustfs:1.0.1
    pull_policy: never
    restart: unless-stopped
    env_file: ["${RUSTFS_ENV_FILE:?Set RUSTFS_ENV_FILE}"]
    command: ["rustfs", "/data"]
    volumes: ["rustfs_data:/data"]
    networks: [backend]
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:9000/health/ready || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 20
      start_period: 10s
  backend:
    image: mis-backend:RELEASE_TAG
    pull_policy: never
    env_file: ["${RUSTFS_ENV_FILE:?Set RUSTFS_ENV_FILE}"]
    depends_on:
      rustfs:
        condition: service_healthy
  frontend:
    image: mis-frontend:RELEASE_TAG
    pull_policy: never
volumes:
  rustfs_data:
'@
$overlay = $overlay.Replace('RELEASE_TAG', $release).Replace("`r`n", "`n") + "`n"
[IO.File]::WriteAllText((Join-Path $bundlePath 'compose.release.yaml'), $overlay, $utf8)
# Normalize the shipped shell script to LF even on a CRLF checkout.
$deploySource = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'deploy-report-release.sh')).Replace("`r`n", "`n")
[IO.File]::WriteAllText((Join-Path $bundlePath 'deploy.sh'), $deploySource, $utf8)
$pageHash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $distPath 'index.html')).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $bundlePath 'frontend-index.sha256'), "$pageHash`n", $utf8)
$releaseInfo = "Release: $release`nCommit: $revision`nExpected previous release: $PreviousRelease`nServer command after extraction and version confirmation:`nsudo bash deploy.sh $release $revision $PreviousRelease`nNo production changes have been made by preparing this bundle.`n"
[IO.File]::WriteAllText((Join-Path $bundlePath 'RELEASE.txt'), $releaseInfo, $utf8)
Copy-Item -LiteralPath (Join-Path $repoPath 'TEST-REPORT-V2.md') -Destination $bundlePath
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'backup.sh') -Destination $bundlePath
$checksumLines = Get-ChildItem -LiteralPath $bundlePath -File | Where-Object Name -ne 'SHA256SUMS' | Sort-Object Name | ForEach-Object {
    '{0}  {1}' -f (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash.ToLowerInvariant(), $_.Name
}
[IO.File]::WriteAllText((Join-Path $bundlePath 'SHA256SUMS'), (($checksumLines -join "`n")+"`n"), $utf8)
$archivePath = Join-Path $repoPath "_download/mis-report-$release.tar.gz"
tar -czf $archivePath -C (Join-Path $repoPath '_download') "mis-report-$release"
if ($LASTEXITCODE) { throw 'Bundle compression failed' }
Get-Item -LiteralPath $archivePath | Select-Object FullName,Length
Get-FileHash -Algorithm SHA256 -LiteralPath $archivePath
