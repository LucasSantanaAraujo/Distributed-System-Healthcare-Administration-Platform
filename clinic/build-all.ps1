$services = @(
    "auth-service",
    "clients-service",
    "doctors-service",
    "appointments-service",
    "notes-service"
)

Write-Host "A construir todas as imagens do projeto Clinic..."
Write-Host ""

foreach ($service in $services) {
    Write-Host "A construir $service..."

    $path = ".\$service"

    if (!(Test-Path $path)) {
        Write-Host "ERRO: Pasta '$service' não encontrada. A ignorar."
        Write-Host ""
        continue
    }

    Set-Location $path

    Write-Host "A limpar e compilar o projeto..."
    mvn clean package -DskipTests

    Write-Host "A construir imagem Docker..."
    docker build -t "$service:latest" .

    Set-Location ".."

    Write-Host ""
}

Write-Host "Todas as imagens foram criadas."
