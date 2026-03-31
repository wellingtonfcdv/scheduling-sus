# Sistema de Agendamento de Retornos Médicos

## 1. Visão Geral

O **Sistema de Agendamento de Retornos Médicos** foi desenvolvido para otimizar e automatizar o processo de marcação de consultas de retorno. A solução elimina a necessidade de anotações manuais e retrabalho administrativo, permitindo que o profissional de saúde registre a necessidade de retorno diretamente no sistema durante a consulta. O sistema organiza as solicitações por prioridade, gera opções de horários baseadas na agenda disponível e envia um link para o paciente escolher o melhor horário. A confirmação final garante o controle da agenda pelo setor administrativo.

## 2. Arquitetura e Tecnologias

A aplicação foi construída utilizando uma arquitetura baseada no padrão **Model-View-Controller (MVC)** e o padrão **DTO (Data Transfer Object)**, garantindo a separação de responsabilidades entre a camada de apresentação (API REST), a lógica de negócio e o acesso a dados.

### Tecnologias Utilizadas:
*   **Linguagem:** Java 21
*   **Framework:** Spring Boot 3.2.4
*   **Persistência:** Spring Data JPA / Hibernate 6
*   **Banco de Dados:** PostgreSQL 15
*   **Gerenciamento de Dependências:** Maven
*   **Conteinerização:** Docker e Docker Compose
*   **Notificações:** Spring Boot Starter Mail (Integração SMTP com suporte a `@Async`)

## 3. Modelagem de Dados

O banco de dados relacional foi estruturado com as seguintes entidades principais. Todas as entidades utilizam **UUID** como identificador primário, gerados de forma resiliente através de `@PrePersist`.

| Entidade | Descrição | Relacionamentos |
| :--- | :--- | :--- |
| **Professional** | Representa o médico ou profissional de saúde. | Possui muitas `ReturnRequest` e `Appointment`. |
| **Patient** | Representa o paciente que necessita de retorno. | Possui muitas `ReturnRequest` e `Appointment`. |
| **ReturnRequest** | Solicitação de retorno criada pelo profissional. | Relaciona-se com `Professional` e `Patient`. |
| **Appointment** | Agendamento de fato, com data e hora definidos. | Relaciona-se com `ReturnRequest`, `Professional` e `Patient`. |

## 4. Fluxo de Funcionamento

1.  **Solicitação:** Durante a consulta, o profissional cria uma `ReturnRequest` informando o paciente, a prioridade (ALTA, MEDIA, BAIXA) e o prazo desejado.
2.  **Disponibilidade:** O sistema calcula os horários disponíveis (`Available Slots`) cruzando a agenda do profissional com os agendamentos já confirmados.
3.  **Agendamento:** O paciente (ou o administrativo em nome dele) seleciona um horário disponível, criando um `Appointment` com status `PENDENTE_CONFIRMACAO`. O sistema valida se a solicitação já possui um agendamento para evitar duplicidade.
4.  **Confirmação:** O administrativo valida e confirma o agendamento. O status muda para `CONFIRMADO` e o horário fica indisponível para outros pacientes.
5.  **Notificação:** Um e-mail de confirmação é disparado de forma **assíncrona** para o paciente, garantindo que falhas na rede de e-mail não interrompam o fluxo de negócio.

## 5. Endpoints da API

A API REST utiliza **DTOs** para todas as respostas, evitando problemas de serialização de proxies do Hibernate e garantindo uma interface limpa.

### Solicitações de Retorno (`/api/return-requests`)
*   `POST /`: Cria uma nova solicitação de retorno (Aceita `ReturnRequestDTO`).
*   `GET /pending`: Retorna a lista de solicitações pendentes (Retorna `List<ReturnRequestDTO>`), ordenadas por prioridade (ALTA primeiro) e data de criação.

### Agendamentos (`/api/appointments`)
*   `GET /available-slots`: Retorna os horários disponíveis para um profissional em uma data específica.
*   `POST /request`: Solicita a reserva de um horário (Retorna `AppointmentDTO`).
*   `POST /{id}/confirm`: Confirma um agendamento e dispara o e-mail (Retorna `AppointmentDTO`).

## 6. Como Executar o Projeto

O projeto está configurado para ser executado facilmente utilizando Docker.

### Pré-requisitos
*   Docker
*   Docker Compose

### Passos para Execução

1.  Navegue até o diretório raiz do projeto.
2.  Execute o comando do Docker Compose para construir e iniciar os contêineres:
    ```bash
    docker-compose up --build -d
    ```
3.  A aplicação estará disponível em `http://localhost:8080`.
4.  O banco de dados PostgreSQL estará rodando na porta `5432`.

### Configuração de E-mail
O sistema está configurado para utilizar o **Gmail** como servidor SMTP por padrão, mas pode ser facilmente adaptado para Mailtrap ou outros. As credenciais devem ser configuradas no arquivo `docker-compose.yml`:

```yaml
environment:
  MAIL_HOST: smtp.gmail.com
  MAIL_PORT: 587
  MAIL_USER: seu-email@gmail.com
  MAIL_PASSWORD: "sua-senha-de-app" # Use aspas se a senha contiver espaços
```

## 7. Resiliência e Melhores Práticas

*   **Padrão DTO:** Implementado para desacoplar a camada de persistência da camada de visualização, resolvendo erros de `ByteBuddyInterceptor`.
*   **Processamento Assíncrono:** O envio de e-mails é executado em threads separadas, aumentando a performance da API.
*   **Validações de Negócio:** Proteção contra agendamentos duplicados e verificações rigorosas de status de solicitações.
*   **UUIDs Flexíveis:** Sistema preparado para aceitar IDs externos ou gerá-los automaticamente de forma segura.
