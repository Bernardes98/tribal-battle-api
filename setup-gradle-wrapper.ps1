$ErrorActionPreference = 'Stop'

$GradleVersion = '9.1.0'
$ZipPath = Join-Path $env:TEMP "gradle-$GradleVersion-bin.zip"
$ExtractRoot = Join-Path $env:TEMP "gradle-$GradleVersion"
$GradleBat = Join-Path $ExtractRoot "gradle-$GradleVersion\bin\gradle.bat"

Write-Host "Preparing Gradle $GradleVersion..." -ForegroundColor Cyan

if (-not (Test-Path $GradleBat)) {
    if (-not (Test-Path $ZipPath)) {
        Write-Host 'Downloading Gradle...' -ForegroundColor Cyan
        Invoke-WebRequest `
            -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" `
            -OutFile $ZipPath
    }

    if (Test-Path $ExtractRoot) {
        Remove-Item -Recurse -Force $ExtractRoot
    }

    Write-Host 'Extracting Gradle...' -ForegroundColor Cyan
    Expand-Archive `
        -Path $ZipPath `
        -DestinationPath $ExtractRoot `
        -Force
}

Write-Host 'Generating Gradle Wrapper...' -ForegroundColor Cyan
& $GradleBat wrapper --gradle-version $GradleVersion

if ($LASTEXITCODE -ne 0) {
    throw "Gradle wrapper generation failed with exit code $LASTEXITCODE"
}

Write-Host ''
Write-Host 'Gradle Wrapper generated successfully.' -ForegroundColor Green
Write-Host 'Next commands:' -ForegroundColor Green
Write-Host '  .\gradlew.bat clean build'
Write-Host '  docker compose up -d'
Write-Host '  .\gradlew.bat bootRun'
