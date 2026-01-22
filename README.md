# 💈 Barbearia API (Backend)

Esta é a API RESTful responsável por gerenciar as regras de negócio, persistência de dados e segurança do Sistema de Agendamento da Barbearia.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Railway](https://img.shields.io/badge/Deploy-Railway-0B0D0E)

## 🔗 Links

- **URL da API (Produção):** [Acessar no Railway](https://barbearia-backend-production-0dfc.up.railway.app/barbeiros)
- **Repositório do Frontend:** [https://github.com/u-santos1/barbearia-frontend]

## 🛠️ Tecnologias

- **Spring Boot 3:** Framework principal.
- **Spring Data JPA:** Manipulação do banco de dados.
- **Spring Security:** Autenticação Basic Auth e configuração de CORS.
- **PostgreSQL:** Banco de dados relacional.
- **Flyway/Hibernate:** Gerenciamento de tabelas (DDL).

## 📝 Principais Endpoints

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/barbeiros` | Lista a equipe | Pública |
| `GET` | `/servicos` | Lista preços e tempos | Pública |
| `POST` | `/agendamentos` | Cliente cria agendamento | Pública |
| `PUT` | `/agendamentos/{id}/confirmar` | Barbeiro aceita horário | **Admin** |
| `PUT` | `/agendamentos/{id}/concluir` | Serviço finalizado e pago | **Admin** |
| `DELETE`| `/agendamentos/{id}/barbeiro` | Cancelamento administrativo | **Admin** |

## ⚙️ Como rodar localmente

1. Clone o repositório.
2. Configure o banco de dados no `src/main/resources/application.properties`.
3. Execute o comando:
   ```bash
   ./mvnw spring-boot:run
