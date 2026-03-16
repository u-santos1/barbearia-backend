# 💈 Barber Pro API

> SaaS completo para gestão de barbearias — em produção

[![Java](https://img.shields.io/badge/Java-17+-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Deploy](https://img.shields.io/badge/Deploy-Railway-purple)](https://railway.app/)

🔗 **[Ver sistema em produção](https://barbearia-frontend-rose.vercel.app/)**

---

## 📋 Sobre o Projeto

API RESTful backend de um SaaS para gestão completa de barbearias.
Construído do zero com foco em Clean Code, segurança e arquitetura escalável.

O sistema permite que donos de barbearias gerenciem sua equipe, serviços
e agenda — enquanto clientes agendam horários online sem precisar de cadastro.

---

## ✨ Funcionalidades

### 🔐 Segurança
- Autenticação stateless com JWT (HMAC256)
- Controle de acesso por roles — ADMIN, BARBEIRO, CLIENTE
- Spring Security com filtro customizado
- Senhas com hash BCrypt

### 📅 Agenda Inteligente
- Validação automática de conflitos de horário
- Expediente dinâmico por barbeiro (dias e horários configuráveis)
- Bloqueios administrativos (almoço, consulta médica)
- Cálculo automático de slots baseado na duração do serviço

### 💰 Financeiro
- Integração com Mercado Pago
- Comissionamento automático (parte do barbeiro / parte da casa)
- Relatórios de faturamento com filtro por período
- Dashboard com métricas diárias

### 🏢 Multi-tenancy
- Suporte a múltiplos barbeiros por barbearia
- Cada dono gerencia sua própria equipe e agenda

---

## 🛠️ Tech Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3 |
| Segurança | Spring Security + JWT |
| Persistência | Spring Data JPA (Hibernate) |
| Banco de dados | PostgreSQL |
| Migrations | Flyway |
| Pagamentos | Mercado Pago API |
| Notificações | OneSignal (Push) |
| Deploy | Railway + Docker |

---

## 🗄️ Modelo de Dados

```
Barbeiro (dono ou funcionário)
    ↓
Expediente (grade de horários por dia da semana)
    ↓
Agendamento (AGENDADO → CONFIRMADO → CONCLUÍDO / CANCELADO)
    ↑               ↑
  Cliente        Serviço
```

Decisões técnicas:
- FetchType.LAZY em todos os relacionamentos — evita N+1
- @Version no Agendamento — controle de concorrência otimista
- Índices nas colunas de busca frequente — performance em produção
- BigDecimal para valores financeiros — precisão em cálculos monetários

---

## 🚀 Como Rodar Localmente

### Pré-requisitos
- Java 17+
- Docker e Docker Compose

### 1. Clone o repositório
```bash
git clone https://github.com/u-santos1/barbearia-backend.git
cd barbearia-backend
```

### 2. Configure as variáveis de ambiente
Crie um arquivo `.env` na raiz com as variáveis abaixo.

### 3. Suba o banco com Docker
```bash
docker-compose up -d
```

### 4. Rode a aplicação
```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`

---

## 🔑 Variáveis de Ambiente

```env
PGHOST=localhost
PGPORT=5432
PGDATABASE=barbearia
PGUSER=postgres
PGPASSWORD=sua_senha
JWT_SECRET=seu_secret_aqui
MP_ACCESS_TOKEN=seu_token_mercadopago
ONESIGNAL_APP_ID=seu_app_id
ONESIGNAL_API_KEY=sua_api_key
```

---

## 📡 Principais Endpoints

### Autenticação
```
POST /auth/login                           → Login do barbeiro/admin
```

### Agendamento (público — cliente anônimo)
```
POST   /agendamentos                       → Criar agendamento
GET    /agendamentos/disponibilidade       → Consultar horários disponíveis
GET    /agendamentos/buscar?telefone=      → Buscar por telefone
DELETE /agendamentos/cliente/{id}          → Cancelar agendamento
```

### Gestão (autenticado)
```
GET  /agendamentos/admin/todos             → Listar todos (dono)
PUT  /agendamentos/{id}/confirmar          → Confirmar
PUT  /agendamentos/{id}/concluir           → Concluir
GET  /agendamentos/admin/financeiro        → Relatório financeiro
```

### Barbeiros e Serviços
```
GET  /barbeiros/**                         → Listar barbeiros
GET  /servicos/**                          → Listar serviços
POST /barbeiros/registro                   → Cadastrar barbearia
```

---

## 🏗️ Arquitetura

```
src/
├── controller/        → Endpoints REST
├── service/           → Regras de negócio
├── repository/        → Acesso ao banco
├── model/             → Entidades JPA
├── dtos/              → DTOs de request
├── dtosResponse/      → DTOs de response
└── infra/
    ├── security/      → JWT, Filtro, Spring Security
    └── TratadorDeErros.java
```

Decisões de arquitetura:
- Entidades nunca expostas diretamente na API — sempre via DTOs
- Exceções de negócio tratadas globalmente com @RestControllerAdvice
- Separação clara entre DTOs de request e response

---

## 👨‍💻 Autor

**Wesley (Uerles Santos)**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Uerles%20Santos-blue)](https://www.linkedin.com/in/uerles-santos-099337313)
[![GitHub](https://img.shields.io/badge/GitHub-u--santos1-black)](https://github.com/u-santos1)
