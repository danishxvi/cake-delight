# Builds the Docker image of every service and, optionally, pushes them.
#
# Usage:
#   .\scripts\build-images.ps1 -RegistryUser <docker-hub-user> [-Tag 1.1.0] [-Push]
#
# Example:
#   .\scripts\build-images.ps1 -RegistryUser myuser -Tag 1.1.0 -Push
#
# Then set the same user and tag in k8s/kustomization.yaml.
param(
    [Parameter(Mandatory = $true)][string]$RegistryUser,
    [string]$Tag = "1.1.0",
    [switch]$Push
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$services = @("eureka-server", "api-gateway", "cake-catalog-service", "order-service",
              "rating-service", "notification-service", "frontend")

foreach ($service in $services) {
    $image = "$RegistryUser/${service}:$Tag"
    Write-Host "Building $image"
    docker build -t $image (Join-Path $root $service)
    if ($LASTEXITCODE -ne 0) { throw "Build failed for $service" }
    if ($Push) {
        docker push $image
        if ($LASTEXITCODE -ne 0) { throw "Push failed for $service" }
    }
}
