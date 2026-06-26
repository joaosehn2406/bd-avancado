# Rastreio Logístico — Cassandra

Sistema de rastreio de encomendas construído sobre **Apache Cassandra**, explorando suas
forças: escrita append-only, leitura O(1) por chave de partição e múltiplos *access patterns*
via tabelas denormalizadas.

> Projeto da disciplina de **Banco de Dados Avançado** — FURB.

**Stack:** Java 17 · Spring Boot 3.2 · Cassandra 4.1 · Angular 21

---

## Pré-requisitos

- **Docker** + Docker Compose
- **JDK 17+**
- **Node.js 20+** (npm 11)

> Não é preciso instalar Maven — o backend usa o wrapper (`mvnw`).

---

## Como rodar

Clone o repositório:

```bash
git clone https://github.com/joaosehn2406/rastreio-log-stico.git
cd rastreio-log-stico
```

Suba as 3 peças, nesta ordem:

### 1. Cassandra (Docker)

```bash
docker compose up -d
```

Sobe o Cassandra na porta `9042` e cria o keyspace `rastreio` automaticamente.
Na primeira vez leva ~30–60s para ficar pronto — acompanhe com `docker compose logs -f cassandra`.

### 2. Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows
```

Sobe em `http://localhost:8080`. As tabelas são criadas automaticamente na inicialização.

### 3. Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

Sobe em `http://localhost:4200` (as chamadas `/api/*` já são redirecionadas para o backend).

---

## Acessar

- **Aplicação / painel admin:** http://localhost:4200
- **Rastreio público:** http://localhost:4200/rastreio/{codigo}

---

## Encerrar

```bash
docker compose down          # para o Cassandra (mantém os dados)
docker compose down -v       # para e apaga os dados
```
