#!/usr/bin/env pwsh
# =====================================================
# Script: Generate Flyway Migration from Hibernate DDL
# =====================================================
# Mục đích: Tự động export schema từ Hibernate entities
#           và tạo Flyway migration file

param(
    [Parameter(Mandatory=$false)]
    [string]$MigrationName = "Schema_Update",

    [Parameter(Mandatory=$false)]
    [int]$Version = 0,

    [Parameter(Mandatory=$false)]
    [switch]$AutoIncrement
)

$ErrorActionPreference = "Stop"

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Hibernate to Flyway Migration Tool" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# =====================================================
# Bước 1: Backup application.yaml
# =====================================================
Write-Host "[1/6] Backing up application.yaml..." -ForegroundColor Yellow

$appYaml = "src/main/resources/application.yaml"
$backupYaml = "src/main/resources/application.yaml.backup"

if (Test-Path $appYaml) {
    Copy-Item $appYaml $backupYaml -Force
    Write-Host "  ✓ Backup created" -ForegroundColor Green
} else {
    Write-Host "  ✗ application.yaml not found!" -ForegroundColor Red
    exit 1
}

# =====================================================
# Bước 2: Tạm thời cấu hình để export schema
# =====================================================
Write-Host "[2/6] Configuring Hibernate for schema export..." -ForegroundColor Yellow

$exportConfig = @"
spring:
  application:
    name: SMD
  datasource:
    url: jdbc:postgresql://localhost:5432/smd_db_temp
    username: postgres
    password: postgres
  jpa:
    properties:
      jakarta.persistence.schema-generation.scripts.action: create
      jakarta.persistence.schema-generation.scripts.create-target: target/generated-schema.sql
      jakarta.persistence.schema-generation.scripts.create-source: metadata
    hibernate:
      ddl-auto: none
  flyway:
    enabled: false
"@

Set-Content -Path $appYaml -Value $exportConfig
Write-Host "  ✓ Configuration updated" -ForegroundColor Green

# =====================================================
# Bước 3: Compile và generate schema
# =====================================================
Write-Host "[3/6] Compiling project and generating schema..." -ForegroundColor Yellow

try {
    mvn clean compile -DskipTests | Out-Null

    if (Test-Path "target/generated-schema.sql") {
        Write-Host "  ✓ Schema generated successfully" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Schema file not found, trying alternative method..." -ForegroundColor Yellow

        # Alternative: Run application briefly to trigger schema generation
        Start-Job -ScriptBlock {
            mvn spring-boot:run
            Start-Sleep -Seconds 10
        } | Out-Null

        Get-Job | Stop-Job
        Get-Job | Remove-Job
    }
} catch {
    Write-Host "  ✗ Compilation failed!" -ForegroundColor Red
    # Restore backup
    Copy-Item $backupYaml $appYaml -Force
    exit 1
}

# =====================================================
# Bước 4: Xác định version number
# =====================================================
Write-Host "[4/6] Determining migration version..." -ForegroundColor Yellow

$migrationDir = "src/main/resources/db/migration"

if (-not (Test-Path $migrationDir)) {
    New-Item -ItemType Directory -Path $migrationDir -Force | Out-Null
    Write-Host "  ✓ Created migration directory" -ForegroundColor Green
}

if ($AutoIncrement -or $Version -eq 0) {
    $existingMigrations = Get-ChildItem -Path $migrationDir -Filter "V*.sql" |
                         ForEach-Object {
                             if ($_.Name -match "^V(\d+)__") {
                                 [int]$matches[1]
                             }
                         } | Sort-Object -Descending

    if ($existingMigrations) {
        $Version = $existingMigrations[0] + 1
    } else {
        $Version = 1
    }
}

Write-Host "  ✓ Using version: V$Version" -ForegroundColor Green

# =====================================================
# Bước 5: Tạo migration file
# =====================================================
Write-Host "[5/6] Creating migration file..." -ForegroundColor Yellow

$migrationFileName = "V${Version}__${MigrationName}.sql"
$migrationPath = Join-Path $migrationDir $migrationFileName

if (Test-Path "target/generated-schema.sql") {
    $schemaContent = Get-Content "target/generated-schema.sql" -Raw

    # Format and add header
    $migrationContent = @"
-- =====================================================
-- Migration: $MigrationName
-- Version: V$Version
-- Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
-- =====================================================
-- Description:
-- This migration was auto-generated from Hibernate entities
-- Please review and modify as needed before applying
-- =====================================================

$schemaContent

-- =====================================================
-- Post-migration tasks (if any)
-- =====================================================
-- Add indexes, constraints, or seed data here

"@

    Set-Content -Path $migrationPath -Value $migrationContent
    Write-Host "  ✓ Migration created: $migrationFileName" -ForegroundColor Green
} else {
    Write-Host "  ✗ Schema file not found!" -ForegroundColor Red
}

# =====================================================
# Bước 6: Restore application.yaml
# =====================================================
Write-Host "[6/6] Restoring application.yaml..." -ForegroundColor Yellow

Copy-Item $backupYaml $appYaml -Force
Remove-Item $backupYaml -Force
Write-Host "  ✓ Configuration restored" -ForegroundColor Green

# =====================================================
# Summary
# =====================================================
Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Migration Generated Successfully!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "File: $migrationFileName" -ForegroundColor White
Write-Host "Location: $migrationPath" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Review the generated migration file" -ForegroundColor White
Write-Host "  2. Test migration: mvn flyway:migrate" -ForegroundColor White
Write-Host "  3. Commit to version control" -ForegroundColor White
Write-Host ""

# =====================================================
# Offer to test migration
# =====================================================
$testMigration = Read-Host "Do you want to test the migration now? (y/n)"

if ($testMigration -eq "y" -or $testMigration -eq "Y") {
    Write-Host ""
    Write-Host "Testing migration..." -ForegroundColor Yellow
    mvn flyway:info
    mvn flyway:migrate
}

Write-Host ""
Write-Host "Done! 🚀" -ForegroundColor Green
