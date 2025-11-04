#!/bin/bash

echo "🚀 A construir todas as imagens do projeto Clinic..."

services=(
  "auth-service"
  "doctors-service"
  "clients-service"
  "appointments-service"
  "notes-service"
)

for service in "${services[@]}"; do
  echo ""
  echo "🔧 A construir $service..."
  cd "$service" || exit
  mvn clean package -DskipTests
  docker build -t clinic-$service:latest .
  cd ..
done

echo ""
echo "✅ Todas as imagens foram criadas com sucesso!"
