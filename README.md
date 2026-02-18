# 💈 Barber Pro API - Backend

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Security](https://img.shields.io/badge/Spring%20Security-JWT-red)

> API RESTful robusta desenvolvida para gestão completa de barbearias no modelo SaaS (Software as a Service).

## 📋 Sobre o Projeto

Este é o backend do sistema **Barber Pro**, responsável por toda a lógica de negócios, segurança e persistência de dados. O sistema permite que donos de barbearias gerenciem sua equipe, serviços, agenda e financeiro, enquanto clientes podem agendar horários online.

O projeto foi desenvolvido com foco em **Clean Code**, arquitetura em camadas e segurança **Stateless** via Tokens JWT.

---

## 🚀 Tecnologias Utilizadas

* **Linguagem:** Java 17+
* **Framework Principal:** Spring Boot 3
* **Segurança:** Spring Security + JWT (JSON Web Token)
* **Banco de Dados:** PostgreSQL (Produção) / H2 (Dev)
* **ORM:** Spring Data JPA (Hibernate)
* **Validação:** Bean Validation (Jakarta Validation)
* **Utilitários:** Lombok (Redução de boilerplate)
* **Notificações:** Integração com OneSignal (Push Notifications)
* **Deploy:** Railway / Docker

---

## ⚙️ Funcionalidades Principais

### 🔐 Segurança & Autenticação
* Login via Token JWT (Stateless).
* Filtro de segurança personalizado (`SecurityFilter`) para interceptar requisições.
* Proteção contra ataques CORS.
* Controle de acesso baseado em Roles (ADMIN, BARBEIRO, CLIENTE).

### 📅 Gestão de Agenda Inteligente
* **Agendamento:** Validação automática de conflitos de horário.
* **Expediente Dinâmico:** Cada barbeiro configura seus dias e horários de trabalho (tabela `tb_expediente`).
* **Bloqueios Administrativos:** O barbeiro pode bloquear horários (almoço, médico) sem precisar de um cliente.
* **Cálculo de Slots:** O sistema gera automaticamente os horários disponíveis baseados na duração do serviço escolhido.

### 💰 Financeiro & Gestão
* **Multi-tenancy:** Suporte a múltiplos barbeiros e donos.
* **Comissionamento:** Cálculo automático da divisão de valor (Parte do Barbeiro / Parte da Casa).
* **Dashboard:** Endpoints otimizados para gráficos de faturamento e métricas diárias.

---

## 🗄️ Modelo de Dados (Resumo)

O banco de dados foi modelado para garantir integridade e performance. Principais entidades:

* **Usuario/Barbeiro:** Dados de login, perfil e configuração.
* **Agendamento:** Centraliza atendimentos e bloqueios (status: AGENDADO, CONCLUIDO, CANCELADO, BLOQUEADO).
* **Expediente:** Define a grade de horário (Dia da Semana, Abre, Fecha, Trabalha?).
* **Servico:** Catálogo de cortes e preços.

---

## 🛠️ Como Rodar o Projeto

### Pré-requisitos
* Java JDK 17 ou superior.
* Maven.
* PostgreSQL instalado (ou usar H2 em memória).

### 1. Clone o repositório
```bash
git clone [https://github.com/u-santos1/barbearia-backend.git](https://github.com/u-santos1/barbearia-backend.git)
cd barbearia-backend
