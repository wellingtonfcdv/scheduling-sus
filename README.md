# 🏥 Sistema de Agendamento de Retornos Médicos

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

## 1. Visão Geral

O **Sistema de Agendamento de Retornos Médicos** foi desenvolvido para otimizar e automatizar o processo de marcação de consultas de retorno no sistema de saúde. A solução elimina a necessidade de anotações manuais e retrabalho administrativo, permitindo que o profissional de saúde registre a necessidade de retorno diretamente no sistema durante a consulta.

### Principais Benefícios:
- ✅ **Priorização:** Organização automática por nível de urgência (ALTA, MÉDIA, BAIXA).
- ✅ **Automação:** Cálculo de disponibilidade real cruzando agendas.
- ✅ **Comunicação:** Disparo automático de e-mails de confirmação.
- ✅ **Controle:** Fluxo de aprovação administrativa para evitar conflitos.

---

## 2. Arquitetura e Tecnologias

A aplicação segue o padrão **MVC (Model-View-Controller)** com foco em princípios de Clean Code e separação de responsabilidades.

*   **Linguagem:** Java 21 (LTS)
*   **Framework:** Spring Boot 3.2.4
*   **Persistência:** Spring Data JPA / Hibernate 6
*   **Banco de Dados:** PostgreSQL 15
*   **Gerenciamento de Dependências:** Maven
*   **Conteinerização:** Docker e Docker Compose
*   **Notificações:** Spring Boot Starter Mail (SMTP)

---

## 3. Estrutura do Projeto

```text
src/main/java/com/fiap/scheduling/
├── controller/   # Endpoints da API REST
├── dto/          # Objetos de Transferência de Dados (Request/Response)
├── entity/       # Entidades JPA (Modelo de Banco de Dados)
├── enums/        # Enumeradores (Prioridade, Status)
├── repository/   # Interfaces de acesso ao banco (Spring Data)
└── service/      # Regras de negócio e integração (E-mail, Agendamento)
```

---

## 4. Como Executar o Projeto

### Pré-requisitos
*   Docker e Docker Compose instalados.

### Passos para Execução

1. Clone o repositório.
2. Na raiz do projeto, execute:
   ```bash
   docker-compose up --build -d
   ```
3. A API estará disponível em `http://localhost:8080`.

### Variáveis de Ambiente
As configurações de banco de dados e e-mail estão centralizadas no `docker-compose.yml`. Para produção ou testes com e-mail real, configure:
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`

---

## 5. Endpoints Principais (API)

### 📋 Solicitações de Retorno (`/api/return-requests`)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/` | Cria uma nova solicitação. |
| `GET` | `/pending` | Lista solicitações pendentes (Ordenadas por prioridade). |

**Exemplo de Request (POST):**
```json
{
  "patientId": 1,
  "professionalId": 1,
  "priority": "ALTA",
  "reason": "Retorno pós-cirúrgico",
  "daysLimit": 15
}
```

### 📅 Agendamentos (`/api/appointments`)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/available-slots` | Consulta horários livres. |
| `POST` | `/request` | Solicita uma reserva de horário. |
| `POST` | `/{id}/confirm` | Confirma o agendamento e envia e-mail. |

---

## 6. Testando a API

Para facilitar o teste, incluímos uma coleção do Postman na raiz do projeto:
`scheduling-system-postman-collection.json`

1. Abra o Postman.
2. Clique em **Import**.
3. Arraste o arquivo da coleção.
4. Configure a variável de ambiente `baseUrl` para `http://localhost:8080`.

---

## 7. Considerações Finais.
Esta arquitetura fornece uma base sólida, escalável e de fácil manutenção. A utilização do Spring Boot em conjunto com o Java 21 traz as mais recentes melhorias de performance e segurança da plataforma Java. A conteinerização com Docker garante que a aplicação execute de forma consistente em qualquer ambiente.
