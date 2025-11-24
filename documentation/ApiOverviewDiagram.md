# HAP API Overview

## Endpoints Principais
- **Auth-Service**:
  - POST /auth/register: Registra usuário/paciente. Body: {name, email, password, role}.
  - POST /auth/login: Login. Body: {email, password}. Returns: JWT token.
  - POST /auth/register/physician: Registra médico (admin only).

- **Clients-Service**:
  - GET /patients: Lista pacientes (admin).
  - GET /patients/{id}: Detalhes paciente (internal/admin).
  - PUT /patients/{id}: Atualiza perfil (patient).

- **Doctors-Service**:
  - POST /physicians: Cria médico (admin).
  - GET /physicians/{id}: Detalhes médico.
  - GET /physicians/search: Busca médicos.

- **Appointments-Service**:
  - POST /appointments/admin: Agenda consulta (admin).
  - GET /appointments/admin/upcoming: Lista próximas.
  - PUT /appointments/{id}/status: Atualiza status.

- **Notes-Service**:
  - PUT /records/appointment/{id}: Cria/atualiza registro (physician).

## Segurança
- JWT para usuários externos.
- X-INTERNAL-KEY para comunicação interna.
- HTTPS via K8s Ingress (configurável).
