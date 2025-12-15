# 🏛️ Documentação de Arquitetura

Este documento detalha as decisões arquiteturais e fluxos de dados do HAP.

## 1. Comunicação Assíncrona (RabbitMQ)

Utilizamos o RabbitMQ para desacoplar os serviços. O fluxo principal é o **"Appointment Created"**.

* **Exchange:** `appointments.exchange` 
* **Routing Key:** `appointment.created`

### Fluxo de Dados
Quando uma consulta é criada no `appointments-service`, uma mensagem é publicada. Três serviços reagem simultaneamente:

1.  **Doctors Service:** Incrementa o contador de consultas do médico e atualiza a data da última consulta.
2.  **Clients Service:** Incrementa o contador do paciente e atualiza a última visita.
3.  **Notes Service:** Cria um `AppointmentRecord` vazio (stub) pronto para o médico preencher.

## 2. Padrão SAGA 

Utilizamos SAGA Coreografada (sem orquestrador central) para garantir a consistência eventual.

### Fluxo de Sucesso (Success Path)
Cenário: Criação de Consulta bem-sucedida.
1.  User posta consulta -> `Appointments` salva (PENDING/CONFIRMED).
2.  `Appointments` publica evento.
3.  Consumidores (`Doctors`, `Clients`, `Notes`) atualizam seus dados locais.

### Fluxo de Compensação (Failure Path)
Cenário: Médico torna-se indisponível.
1.  Admin marca médico como indisponível no `Doctors-Service`.
2.  `Doctors` publica `PhysicianUnavailableEvent` no RabbitMQ.
3.  `Appointments-Service` escuta este evento.
4.  **Ação Compensatória:** `Appointments` busca todas as consultas afetadas e altera status para `CANCELLED`.

## 3. Estratégia de Dados (Database-per-Service)

Abandonamos o banco compartilhado H2.
* **PostgreSQL:** Dados relacionais e transacionais (`Auth`, `Appointments`, `Clients`).
* **MongoDB:** Dados orientados a documentos e leitura intensiva (`Doctors` profile, `Notes` prontuário).

**Nota sobre Integridade:** Não existem Foreign Keys entre bancos de serviços diferentes. A integridade é mantida via IDs lógicos e consistência eventual.
