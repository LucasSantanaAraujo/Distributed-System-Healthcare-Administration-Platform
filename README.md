![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green) ![Kubernetes](https://img.shields.io/badge/Orchestration-Kubernetes-blue) ![RabbitMQ](https://img.shields.io/badge/Messaging-RabbitMQ-orange)

# LETI-SISDIS_2025_G5
## 👨‍💻 Equipa (G5)
| Estudante | Número |
| :--- | :--- |
| **Carlos Cunha** | 1230717 |
| **Lucas Araújo** | 1230550 |
| **Eduardo Coelho** | 1230720 |

## 🚀 Visão Geral do Projeto

O **HAP** é uma plataforma distribuída para gestão de clínicas médicas, refatorada de uma arquitetura monolítica para **Microserviços**.

O sistema resolve problemas de escalabilidade e resiliência utilizando orquestração via **Kubernetes** e um modelo de comunicação híbrido:
- **Síncrono (REST):** Para autenticação, leituras críticas e interações diretas com o frontend.
- **Assíncrono (AMQP/RabbitMQ):** Para consistência eventual, processamento de estatísticas e gestão de transações distribuídas (Saga).

### 🏛️ Padrões Arquiteturais Implementados

* **Database-per-Service:** Isolamento total de dados. Cada microserviço opera o seu próprio banco (PostgreSQL ou MongoDB), sem partilha de tabelas.
* **Saga Pattern (Coreografia):** Gestão de transações distribuídas sem orquestrador central.
    * *Exemplo:* Se um médico fica indisponível (Doctors Service), o evento dispara o cancelamento automático das consultas (Appointments Service).
* **CQRS (Leve):** Segregação de responsabilidade. Serviços como `Clients` e `Doctors` mantêm projeções de dados (read models) atualizadas via eventos para evitar latência de rede em leituras frequentes.
* **Event-Driven Architecture:** Uso intensivo de **RabbitMQ** com *Topic Exchanges* para broadcasting de eventos de domínio (`AppointmentCreated`, `PhysicianUnavailable`).

---


## 📦 Mapa dos Serviços

| Serviço | Porta (Interna) | Tech Stack | Responsabilidade | Persistência |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | 8080 | Spring Boot | Autenticação, JWT, Gateway de Segurança | PostgreSQL (Schema: `auth`) |
| **Appointments** | 8080 | Spring Boot | Core Business, Gestão de Sagas | PostgreSQL (Schema: `appts`) |
| **Doctors** | 8080 | Spring Boot | Gestão de Médicos, Estatísticas | MongoDB (Collection: `physicians`) |
| **Clients** | 8080 | Spring Boot | Gestão de Pacientes, Histórico | PostgreSQL (Schema: `clients`) |
| **Notes** | 8080 | Spring Boot | Prontuário Médico Eletrónico | MongoDB (Collection: `records`) |

---

## 🎨 Documentação Visual

A documentação completa da arquitetura encontra-se na pasta `/docs`.

### Diagramas Principais (C4 Model)
- **[System Context Diagram](documentation/svg/SystemContextDiagram.svg):** Visão macro das interações com pacientes e médicos.
- **[Container Diagram (V2)](documentation2/svg/C4_Container_V2.svg):** Detalhe dos microserviços, filas RabbitMQ e bases de dados separadas.
- **[Physical View](documentation/svg/PhysicalViewDiagram.svg):** Implantação no Kubernetes (Pods, Services, Ingress).

### Fluxos de Negócio (UML Sequence)
- **[Saga: Agendamento](documentation/svg/SD_Appointment_Scheduling.svg):** O fluxo "Happy Path" assíncrono.
- **[Saga: Compensação](documentation2/svg/SD_Compensation.svg):** O fluxo de reversão em caso de falha.
- **[Registo Síncrono](documentation2/svg/SD_Patient_Registration.svg):** Criação de utilizadores via REST.

---

## 💡 Decisões Arquiteturais e Evolução

| Decisão                    | Justificação | Estado Atual |
|:---------------------------| :--- | :--- |
| **Kubernetes (K8s)**       | Necessidade de orquestração, *Service Discovery* e *Self-healing*. | ✅ Implementado (Deployments, Services, Secrets). |
| **Persistência**           | Adequar o banco ao tipo de dado (Relacional vs Documental). | ✅ Migrado de H2 para Postgres/Mongo em containers. |
| **RabbitMQ**               | Desacoplar produtores de consumidores e permitir *Replay* de eventos. | ✅ Implementado (Topic Exchange). |
| **Segurança Centralizada** | Evitar duplicação de lógica. Auth gera JWT, outros validam. | ✅ Implementado com `X-INTERNAL-KEY` para S2S. |
| **API Gateway**            | Ponto único de entrada. | ✅ Implementado via K8s **Ingress**. |

---
## Instruções de Deployment

### Pré-requisitos
- JDK 21 (ou superior).
- Maven 3.8+.
- Git para versionamento.
- (Para K8s) Minikube ou cluster K8s, Skaffold para CI/CD

### Rodar Localmente
1. Clone o repositório: `git clone <url>`.
2. Para cada serviço (e.g., clients-service):
    - Navegue: `cd clients-service`.
    - Rode: `mvn spring-boot:run`.
    - Acesse: http://localhost:8082 (porta varia por serviço).
3. Ordem sugerida: Auth-Service primeiro, depois Clients, Doctors, etc.



