# run-verification.ps1 — full quality gate for MCP-AI-Chat
#
# Usage (from project root):
#   powershell -ExecutionPolicy Bypass -File .\run-verification.ps1
#
# Runs: spotlessCheck -> detekt -> testDebugUnitTest -> assembleDebug
# and writes the complete log plus a test summary to verification-results.txt
# (same folder). Share that file back for review.

$ErrorActionPreference = "Continue"
$root = $PSScriptRoot
$log = Join-Path $root "verification-results.txt"

function Log($msg) { Add-Content -LiteralPath $log -Value $msg -Encoding utf8 }

"=== MCP-AI-Chat verification run ===" | Out-File -LiteralPath $log -Encoding utf8
Log "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# Workaround for stale Spotless JVM-local cache (diffplug/spotless#987).
# Must run BEFORE any spotless task; forces one slower re-configuration.
$cc = Join-Path $root ".gradle\configuration-cache"
if (Test-Path -LiteralPath $cc) {
    Log "[prep] Removing stale Spotless configuration cache (.gradle\configuration-cache)"
    Remove-Item -Recurse -Force -LiteralPath $cc -ErrorAction SilentlyContinue
}

# Single worker keeps the machine responsive while Android Studio is open.
Log "[run] gradlew :app:spotlessCheck :app:detekt :app:testDebugUnitTest :app:assembleDebug"
Log "---------------------------------------------------------------------"
# Add-Content -Encoding utf8 (Tee-Object would write UTF-16 on PowerShell 5.1
# and corrupt the log for other tools).
& (Join-Path $root "gradlew.bat") `
    :app:spotlessCheck :app:detekt :app:testDebugUnitTest :app:assembleDebug `
    --max-workers=1 --console=plain 2>&1 |
    ForEach-Object {
        $line = "$_"
        Add-Content -LiteralPath $log -Value $line -Encoding utf8
        $line
    }
$code = $LASTEXITCODE
Log "---------------------------------------------------------------------"

# Test outcome summary parsed from the JUnit XML Gradle emits per class.
$resultsDir = Join-Path $root "app\build\test-results\testDebugUnitTest"
if (Test-Path -LiteralPath $resultsDir) {
    $total = 0; $failed = 0; $errors = 0; $skipped = 0
    Get-ChildItem -LiteralPath $resultsDir -Filter "*.xml" | ForEach-Object {
        try {
            [xml]$x = Get-Content -LiteralPath $_.FullName -Raw
            $suites = if ($x.DocumentElement.Name -eq "testsuites") { $x.testsuites.testsuite } else { @($x.testsuite) }
            foreach ($s in @($suites)) {
                $total  += [int]$s.tests
                $failed += [int]$s.failures
                $errors += [int]$s.errors
                $skipped += [int]$s.skipped
            }
        } catch {
            Log "[tests] could not parse $($_.Name)"
        }
    }
    Log ("[tests] total={0} failures={1} errors={2} skipped={3}" -f $total, $failed, $errors, $skipped)
    if ($failed -gt 0 -or $errors -gt 0) {
        # List the failing test classes so failures are easy to spot in the log.
        Get-ChildItem -LiteralPath $resultsDir -Filter "*.xml" | ForEach-Object {
            [xml]$x = Get-Content -LiteralPath $_.FullName -Raw
            $suites = if ($x.DocumentElement.Name -eq "testsuites") { $x.testsuites.testsuite } else { @($x.testsuite) }
            foreach ($s in @($suites)) {
                if ([int]$s.failures -gt 0 -or [int]$s.errors -gt 0) {
                    Log ("[tests] FAILING CLASS: {0} (failures={1} errors={2})" -f $s.name, $s.failures, $s.errors)
                }
            }
        }
    }
} else {
    Log "[tests] no JUnit XML results found (tests did not run)"
}

Log "Gradle exit code: $code"
Log "Finished: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
if ($code -eq 0) { Log "RESULT: SUCCESS" } else { Log "RESULT: FAILURE" }

Write-Host ""
Write-Host "Done. Full log written to: $log"
exit $code
