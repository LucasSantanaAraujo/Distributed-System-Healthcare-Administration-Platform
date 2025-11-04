#!/bin/bash

NAMESPACE="clinic"
SERVICE="auth-service"
HPA_NAME="auth-service"
IMAGE="curlimages/curl"

echo "🚀 A lançar teste automático de autoscaling no namespace '$NAMESPACE'..."
echo "📦 A criar pod gerador de carga..."

kubectl run load-generator --image=$IMAGE -n $NAMESPACE --restart=Never -- \
/bin/sh -c "while true; do \
curl -s -H 'Content-Type: application/json' \
-d '{\"email\":\"admin@clinica.pt\",\"password\":\"Admin1234!\"}' \
http://$SERVICE.$NAMESPACE.svc.cluster.local:8080/login > /dev/null; \
done" &

echo "✅ Pod 'load-generator' criado. A monitorizar o HPA..."
echo "Pressiona CTRL+C para parar o teste e apagar o pod."

kubectl get hpa -n $NAMESPACE -w

echo "🧹 A apagar pod de carga..."
kubectl delete pod load-generator -n $NAMESPACE --ignore-not-found
echo "✅ Teste concluído!"


#kubectl delete pod load-generator -n clinic
