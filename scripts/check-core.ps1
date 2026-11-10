$ErrorActionPreference = 'Stop'
$tvProjectRoot = Split-Path $PSScriptRoot -Parent
$tvJdkPath = $env:JAVA_HOME
if (-not $tvJdkPath) {
    $tvLocalJdk = Get-ChildItem -LiteralPath (Join-Path $tvProjectRoot '.tools/jdk') -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe') } | Select-Object -First 1
    if ($tvLocalJdk) { $tvJdkPath = $tvLocalJdk.FullName }
}
if (-not $tvJdkPath -or -not (Test-Path -LiteralPath (Join-Path $tvJdkPath 'bin/javac.exe'))) {
    throw 'Set JAVA_HOME to a JDK or run the local Android setup first.'
}
$tvCheckOutput = Join-Path $tvProjectRoot '.tools/core-check'
New-Item -ItemType Directory -Path $tvCheckOutput -Force | Out-Null
& (Join-Path $tvJdkPath 'bin/javac.exe') -d $tvCheckOutput `
    (Join-Path $tvProjectRoot 'app/src/main/java/com/bettertv/launcher/AppOrder.java') `
    (Join-Path $tvProjectRoot 'tests/com/bettertv/launcher/AppOrderCheck.java') `
    (Join-Path $tvProjectRoot 'app/src/main/java/com/bettertv/launcher/BroadcastRules.java') `
    (Join-Path $tvProjectRoot 'tests/com/bettertv/launcher/BroadcastRulesCheck.java') `
    (Join-Path $tvProjectRoot 'app/src/main/java/com/bettertv/launcher/SearchRules.java') `
    (Join-Path $tvProjectRoot 'tests/com/bettertv/launcher/SearchRulesCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'Core checks did not compile.' }
& (Join-Path $tvJdkPath 'bin/java.exe') -ea -cp $tvCheckOutput com.bettertv.launcher.AppOrderCheck
if ($LASTEXITCODE -ne 0) { throw 'Core checks failed.' }
foreach ($tvCheckClass in @('BroadcastRulesCheck', 'SearchRulesCheck')) {
    & (Join-Path $tvJdkPath 'bin/java.exe') -ea -cp $tvCheckOutput ('com.bettertv.launcher.' + $tvCheckClass)
    if ($LASTEXITCODE -ne 0) { throw ($tvCheckClass + ' failed.') }
}

[xml]$tvManifest = Get-Content -Raw -LiteralPath (Join-Path $tvProjectRoot 'app/src/main/AndroidManifest.xml')
$tvXmlNs = [System.Xml.XmlNamespaceManager]::new($tvManifest.NameTable)
$tvXmlNs.AddNamespace('a', 'http://schemas.android.com/apk/res/android')
$tvExpectedPermissions = @('android.permission.INTERNET', 'android.permission.READ_TV_LISTINGS')
foreach ($tvPermissionNode in $tvManifest.SelectNodes('/manifest/uses-permission')) {
    if ($tvPermissionNode.GetAttribute('name', $tvXmlNs.LookupNamespace('a')) -notin $tvExpectedPermissions) {
        throw 'Search may request only Internet and optional TV-listings access.'
    }
}
foreach ($tvExpectedPermission in $tvExpectedPermissions) {
    if (-not $tvManifest.SelectSingleNode('/manifest/uses-permission[@a:name="' + $tvExpectedPermission + '"]', $tvXmlNs)) {
        throw ('Missing required search capability: ' + $tvExpectedPermission)
    }
}
if ($tvManifest.DocumentElement.application.GetAttribute('usesCleartextTraffic', $tvXmlNs.LookupNamespace('a')) -ne 'false') {
    throw 'Search must disable cleartext network traffic.'
}
[xml]$tvDataRules = Get-Content -Raw -LiteralPath (Join-Path $tvProjectRoot 'app/src/main/res/xml/data_extraction_rules.xml')
foreach ($tvBackupKind in @('cloud-backup', 'device-transfer')) {
    if (-not $tvDataRules.SelectSingleNode('/data-extraction-rules/' + $tvBackupKind + '/exclude[@domain="sharedpref" and @path="search.xml"]')) {
        throw ('Private search credentials must be excluded from ' + $tvBackupKind)
    }
}
if (-not $tvManifest.SelectSingleNode('/manifest/queries/intent/category[@a:name="android.intent.category.LEANBACK_LAUNCHER"]', $tvXmlNs)) {
    throw 'Android 11+ app discovery requires narrow TV launcher package queries.'
}
$tvActivity = $tvManifest.SelectSingleNode('/manifest/application/activity[@a:name=".MainActivity" and @a:exported="true"]', $tvXmlNs)
if (-not $tvActivity -or -not $tvActivity.SelectSingleNode('intent-filter[category[@a:name="android.intent.category.HOME"] and category[@a:name="android.intent.category.DEFAULT"]]', $tvXmlNs)) {
    throw 'The main activity must be an exported Home candidate.'
}
if (-not $tvActivity.SelectSingleNode('intent-filter/category[@a:name="android.intent.category.LEANBACK_LAUNCHER"]', $tvXmlNs)) {
    throw 'The launcher must also be openable as a normal TV app.'
}
foreach ($tvSearchActivity in @('.SearchActivity', '.LocalTvActivity')) {
    if (-not $tvManifest.SelectSingleNode('/manifest/application/activity[@a:name="' + $tvSearchActivity + '" and @a:exported="false"]', $tvXmlNs)) {
        throw ('Search screens must remain private to the launcher: ' + $tvSearchActivity)
    }
}
Write-Output 'Manifest checks passed: narrow search permissions, HTTPS only, private search screens, Home and normal-TV entries.'

$tvHomeService = $tvManifest.SelectSingleNode('/manifest/application/service[@a:name=".HomeButtonService"]', $tvXmlNs)
if (-not $tvHomeService -or $tvHomeService.GetAttribute('permission', $tvXmlNs.LookupNamespace('a')) -ne 'android.permission.BIND_ACCESSIBILITY_SERVICE') {
    throw 'The optional Home-key service must be protected by Android accessibility binding permission.'
}
[xml]$tvHomeConfig = Get-Content -Raw -LiteralPath (Join-Path $tvProjectRoot 'app/src/main/res/xml/home_button_service.xml')
$tvHomeConfigRoot = $tvHomeConfig.DocumentElement
if ($tvHomeConfigRoot.GetAttribute('canRetrieveWindowContent', $tvXmlNs.LookupNamespace('a')) -ne 'false' -or
    $tvHomeConfigRoot.HasAttribute('accessibilityFlags', $tvXmlNs.LookupNamespace('a')) -or
    $tvHomeConfigRoot.HasAttribute('canRequestFilterKeyEvents', $tvXmlNs.LookupNamespace('a')) -or
    $tvHomeConfigRoot.GetAttribute('packageNames', $tvXmlNs.LookupNamespace('a')) -ne 'com.bettertv.launcher') {
    throw 'The optional Home-key service must not retrieve screen content or request unrelated capabilities.'
}
Write-Output 'Home-button service boundary checks passed: protected binding, no key filtering or screen-content retrieval.'
