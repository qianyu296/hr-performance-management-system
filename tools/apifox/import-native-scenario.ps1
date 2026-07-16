param(
  [Parameter(Mandatory = $true)]
  [ValidateRange(1, [long]::MaxValue)]
  [long]$ProjectId,
  [string]$CliCommand = 'apifox'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-ApifoxCli {
  param([Parameter(Mandatory = $true)][string[]]$Arguments)

  $output = & $CliCommand @Arguments 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "Apifox CLI 执行失败：$($output | Out-String)"
  }
  return ($output | Out-String).Trim()
}

if (-not (Get-Command $CliCommand -ErrorAction SilentlyContinue)) {
  throw "未找到 Apifox CLI。请先执行 npm install -g apifox-cli，然后执行 apifox auth login --with-token <访问令牌>。"
}

try {
  Invoke-ApifoxCli @('team', 'list') | Out-Null
} catch {
  throw "Apifox CLI 尚未登录。请先执行 apifox auth login --with-token <访问令牌>，再重新运行本脚本。原始错误：$($_.Exception.Message)"
}

$directory = $PSScriptRoot
$createFile = Join-Path $directory 'hrpm-non-performance-api-scenario.create.json'
$updateFile = Join-Path $directory 'hrpm-non-performance-api-scenario.update.json'

& node (Join-Path $directory 'generate-native-scenario.mjs') '--project-id' $ProjectId
if ($LASTEXITCODE -ne 0) {
  throw '原生场景 JSON 生成失败。'
}

Invoke-ApifoxCli @('cli-schema', 'validate', 'test-scenario-create', '--file', $createFile) | Out-Null
Invoke-ApifoxCli @('cli-schema', 'validate', 'test-scenario-update', '--file', $updateFile) | Out-Null

$createResult = Invoke-ApifoxCli @('test-scenario', 'create', '--project', $ProjectId, '--file', $createFile) | ConvertFrom-Json
$scenarioId = $createResult.data.id
if (-not $scenarioId) {
  throw 'Apifox 没有返回新建场景的 ID。'
}

Invoke-ApifoxCli @('test-scenario', 'update', $scenarioId, '--project', $ProjectId, '--file', $updateFile) | Out-Null

$detail = Invoke-ApifoxCli @('test-scenario', 'get', $scenarioId, '--project', $ProjectId, '--with-case-detail') | ConvertFrom-Json
$steps = $detail.data.steps
if (-not $steps) { $steps = $detail.data.caseDetail.steps }
if (-not $steps) { $steps = $detail.data.detail.steps }

if (@($steps).Count -ne 91) {
  throw "场景已创建，但读取到的步骤数不是预期的 91。请在 Apifox 中检查场景 ID $scenarioId。"
}

Write-Host ''
Write-Host "已在项目 $ProjectId 创建 Apifox 原生场景用例：HRPM 非绩效接口回归测试（ID: $scenarioId）。"
Write-Host '已确认包含 91 个步骤：1 个初始化脚本和 90 个接口请求。'
Write-Host '本脚本不会自动运行场景。请回到 Apifox 的“自动化测试 -> 场景用例”，选择该场景并选择 HRPM Local Development 环境后手动运行。'
