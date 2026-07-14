# 💈 Barbearia-BackEnd API

> SaaS completo para gestão de barbearias — em produção

[![Java](https://img.shields.io/badge/Java-17+-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Deploy](https://img.shields.io/badge/Deploy-Railway-purple)](https://railway.app/)

🔗 **[Ver sistema em produção](https://barbearia-frontend-rose.vercel.app/)**

---

## 📋 Sobre o Projeto

API RESTful backend de um SaaS para gestão completa de barbearias.
Construído do zero com foco em Clean Code, segurança e arquitetura escalável (Padrão 3 Camadas).

O sistema permite que donos de barbearias gerenciem sua equipe, serviços, planos de assinatura
e agenda — enquanto clientes agendam horários online sem precisar de cadastro prévio.

---

## ✨ Funcionalidades

### 🔐 Segurança Avançada
- Autenticação stateless com JWT (HMAC256).
- Controle de acesso rigoroso por perfis (ADMIN, BARBEIRO, CLIENTE).
- Proteção dupla de **Rate Limiting** (Caffeine + Bucket4j):
  - Global por IP (Prevenção contra DDoS).
  - Específico por e-mail no login (Prevenção contra Força Bruta).
- Senhas com hash BCrypt e fluxo seguro de recuperação de senha.

### 📅 Agenda Inteligente
- Validação automática de conflitos de horário.
- Expediente dinâmico por barbeiro (dias e horários configuráveis).
- Bloqueios administrativos (almoço, consulta médica, férias).
- Cálculo automático de slots baseado na duração específica de cada serviço.

### 💰 Financeiro e SaaS
- Motor de assinaturas SaaS para os donos de barbearias.
- Bloqueio automático de acesso à plataforma por inadimplência (integrado direto na camada de Segurança).
- Integração de pagamentos via Webhooks (Mercado Pago).
- Geração de relatórios financeiros em PDF.
- Comissionamento automático (divisão entre parte do barbeiro e parte da casa).

### 📱 Mensageria e Lembretes
- Disparo de notificações e lembretes para clientes via WhatsApp.
- Logs e rastreabilidade de mensagens enviadas.
- Alertas instantâneos para o barbeiro a cada novo agendamento.

### 🏢 Multi-tenancy
- Suporte a múltiplos barbeiros trabalhando na mesma barbearia (Dono e Funcionários).
- Isolamento de dados: cada dono gerencia sua própria equipe e agenda.

---

## 🛠️ Tech Stack

| Camada | Tecnologia |
|---|---|
| **Linguagem** | Java 17 |
| **Framework** | Spring Boot 3.x |
| **Segurança** | Spring Security + JWT |
| **Rate Limit** | Bucket4j + Caffeine Cache |
| **Persistência** | Spring Data JPA (Hibernate) |
| **Banco de dados** | PostgreSQL 15 |
| **Migrations** | Flyway |
| **Integrações** | Mercado Pago API, WhatsApp API |
| **Deploy** | Railway + Docker |

---

## 🗄️ Modelo de Dados (Resumo)

```text
Barbeiro (Dono/Admin ou Funcionário)
    ├── Assinatura (Plano SaaS)
    └── Expediente (Grade de horários por dia da semana)
          ↓
     Agendamento (AGENDADO → CONFIRMADO → CONCLUÍDO / CANCELADO)
          ↑               ↑
       Cliente         Serviço
```

**Decisões técnicas de destaque:**
- `FetchType.LAZY` em todos os relacionamentos para evitar o problema N+1.
- Uso de `@Version` no Agendamento para controle de concorrência otimista (evita overbooking simultâneo).
- Índices nas colunas de busca frequente otimizando a performance em produção.
- `BigDecimal` padronizado para valores financeiros, garantindo precisão absoluta em cálculos monetários e comissões.

---

## 🚀 Como Rodar Localmente

### Pré-requisitos
- Java 17+
- Docker e Docker Compose (para rodar o banco de dados facilmente)

### 1. Clone o repositório
```bash
git clone https://github.com/u-santos1/barbearia-backend.git
cd barbearia-backend
```

### 2. Configure as variáveis de ambiente
Crie um arquivo `.env` na raiz copiando as propriedades essenciais.

### 3. Suba o banco de dados com Docker
```bash
docker-compose up -d
```

### 4. Rode a aplicação
```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`

---

## 🔑 Variáveis de Ambiente Necessárias

```env
PGHOST=localhost
PGPORT=5432
PGDATABASE=barbearia
PGUSER=postgres
PGPASSWORD=sua_senha
JWT_SECRET=seu_secret_aqui
MP_ACCESS_TOKEN=seu_token_mercadopago
# Adicione também chaves de APIs de notificação/WhatsApp conforme seu provedor
```

---

## 📡 Principais Endpoints

### Autenticação
```http
POST /auth/login                           → Login do barbeiro/admin
```

### Agendamento (Fluxo do Cliente Final - Público)
```http
POST   /agendamentos                       → Criar agendamento
GET    /agendamentos/disponibilidade       → Consultar horários vagos (cálculo dinâmico)
GET    /agendamentos/buscar?telefone=      → Acompanhar agendamento por telefone
DELETE /agendamentos/cliente/{id}          → Cancelar agendamento pelo cliente
```

### Gestão (Fluxo do Barbeiro Logado - Protegido)
```http
GET  /agendamentos/admin/todos             → Listar todos os agendamentos da casa
PUT  /agendamentos/{id}/confirmar          → Marcar como confirmado
PUT  /agendamentos/{id}/concluir           → Marcar como concluído
GET  /agendamentos/admin/financeiro        → Extrair relatório financeiro
GET  /agendamentos/admin/pdf               → Gerar PDF com fechamento do dia/mês
```

---

## 🏗️ Arquitetura

O projeto segue rigorosamente a **Arquitetura em 3 Camadas** (Controllers, Services, Repositories).

```text
src/
├── controller/        → Camada de Apresentação (Endpoints REST HTTP)
├── service/           → Camada de Negócio (Onde mora toda a inteligência e validações)
├── repository/        → Camada de Acesso a Dados (Queries e JPA)
├── model/             → Entidades JPA mapeadas para o PostgreSQL
├── dtos/              → DTOs de entrada (Requests e validações de formulário)
├── dtosResponse/      → DTOs de saída (Responses limpos, sem vazar dados sensíveis)
└── infra/
    ├── security/      → Configuração do JWT, Filtros Customizados, Rate Limit, Spring Security
    └── TratadorDeErros.java → Global Exception Handler (@RestControllerAdvice)
```

**Benefícios Alcançados com esta Arquitetura:**
- Entidades do banco nunca são expostas diretamente na API (uso estrito de DTOs).
- Exceções de negócio são centralizadas e tratadas globalmente retornando JSONs amigáveis em vez de StackTraces.
- Baixo acoplamento facilitando testes unitários e manutenções futuras.

---

## 👨‍💻 Autor

**(Uerles Santos)**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Uerles%20Santos-blue)](https://www.linkedin.com/in/uerles-santos-099337313)
[![GitHub](https://img.shields.io/badge/GitHub-u--santos1-black)](https://github.com/u-santos1)
