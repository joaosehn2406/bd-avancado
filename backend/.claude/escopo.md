# Sistema de Rastreio Logístico — Escopo e Features

## Visão Geral

Sistema de rastreio de packages modelado para explorar as forças do Apache Cassandra:
append-only, alta disponibilidade de leitura por chave, e múltiplos access patterns via tabelas denormalizadas.

Stack: **Java (Spring Boot) · Angular · Cassandra**

---

## Por que Cassandra faz sentido aqui

O núcleo do sistema é um log de eventos imutáveis — cada vez que uma encomenda passa por
um hub, um evento é registrado com cidade, status, timestamp e coordenadas. Esse padrão é
exatamente o que Cassandra foi projetado para suportar:

- Eventos são **append-only** — nunca se edita um rastreio, só se adiciona
- A consulta mais frequente é **"todos os eventos desse código"** — leitura por partition key
- Timestamp como clustering key entrega os eventos em ordem cronológica sem custo extra
- Escrita distribuída por partition (cada código é isolado) — sem hot spots

---

## Modelo de Tabelas Cassandra

```cql
-- Access pattern 1: histórico completo por código de rastreio
CREATE TABLE events_by_code (
  tracking_code TEXT,
  timestamp     TIMESTAMP,
  city          TEXT,
  status        TEXT,     -- COLLECTED | IN_TRANSIT | IN_SEPARATION | OUT_FOR_DELIVERY | DELIVERED
  latitude      DOUBLE,
  longitude     DOUBLE,
  notes         TEXT,
  PRIMARY KEY (tracking_code, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Access pattern 2: pacotes que passaram por uma cidade num dado dia
CREATE TABLE events_by_city (
  city          TEXT,
  date_bucket   DATE,     -- partition por dia — evita partition infinita
  timestamp     TIMESTAMP,
  tracking_code TEXT,
  status        TEXT,
  PRIMARY KEY ((city, date_bucket), timestamp, tracking_code)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Access pattern 3: pacotes com determinado status num dado dia
CREATE TABLE events_by_status (
  status        TEXT,
  date_bucket   DATE,
  timestamp     TIMESTAMP,
  tracking_code TEXT,
  city          TEXT,
  PRIMARY KEY ((status, date_bucket), timestamp, tracking_code)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Dados cadastrais do pacote — uma linha por pacote, não cresce dentro da partição
CREATE TABLE packages (
  tracking_code TEXT PRIMARY KEY,
  sender        TEXT,
  recipient     TEXT,
  origin        TEXT,
  destination   TEXT,
  created_at    TIMESTAMP,
  weight_kg     DECIMAL
);

-- Usuários admin — pré-populado via seed, sem tela de cadastro
CREATE TABLE users (
  username      TEXT PRIMARY KEY,
  password_hash TEXT,   -- bcrypt
  role          TEXT    -- ADMIN
);
```

---

## Entidades Java

Regra geral: **record** quando a partition key é simples. **Classe imutável com `@PersistenceCreator`** quando a partition key é composta.

Entidades nunca são serializadas para o front — toda saída de API passa por um DTO (`*Response`).

```java
// ─── Shipment.java — record (partition key simples) ───────────────────────
@Table("packages")
public record Shipment(
    @PrimaryKey String trackingCode,
    String sender,
    String recipient,
    String origin,
    String destination,
    Instant createdAt,
    BigDecimal weightKg
) {}

// ─── User.java — record (partition key simples) ───────────────────────────
@Table("users")
public record User(
    @PrimaryKey String username,
    String passwordHash,
    String role
) {}

// ─── EventByCode.java — classe imutável (partition simples + clustering) ──
@Table("events_by_code")
public class EventByCode {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String trackingCode;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    private final String city;
    private final String status;
    private final Double latitude;
    private final Double longitude;
    private final String notes;

    @PersistenceCreator
    public EventByCode(String trackingCode, Instant timestamp, String city,
                       String status, Double latitude, Double longitude, String notes) {
        this.trackingCode = trackingCode;
        this.timestamp    = timestamp;
        this.city         = city;
        this.status       = status;
        this.latitude     = latitude;
        this.longitude    = longitude;
        this.notes        = notes;
    }

    public String getTrackingCode() { return trackingCode; }
    public Instant getTimestamp()   { return timestamp; }
    public String getCity()         { return city; }
    public String getStatus()       { return status; }
    public Double getLatitude()     { return latitude; }
    public Double getLongitude()    { return longitude; }
    public String getNotes()        { return notes; }
}

// ─── EventByCity.java — classe imutável (partition key composta) ──────────
@Table("events_by_city")
public class EventByCity {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String city;

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private final LocalDate dateBucket;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final String trackingCode;

    private final String status;

    @PersistenceCreator
    public EventByCity(String city, LocalDate dateBucket, Instant timestamp,
                       String trackingCode, String status) {
        this.city          = city;
        this.dateBucket    = dateBucket;
        this.timestamp     = timestamp;
        this.trackingCode  = trackingCode;
        this.status        = status;
    }

    public String getCity()             { return city; }
    public LocalDate getDateBucket()    { return dateBucket; }
    public Instant getTimestamp()       { return timestamp; }
    public String getTrackingCode()     { return trackingCode; }
    public String getStatus()           { return status; }
}

// ─── EventByStatus.java — classe imutável (partition key composta) ────────
@Table("events_by_status")
public class EventByStatus {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String status;

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private final LocalDate dateBucket;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final String trackingCode;

    private final String city;

    @PersistenceCreator
    public EventByStatus(String status, LocalDate dateBucket, Instant timestamp,
                         String trackingCode, String city) {
        this.status        = status;
        this.dateBucket    = dateBucket;
        this.timestamp     = timestamp;
        this.trackingCode  = trackingCode;
        this.city          = city;
    }

    public String getStatus()           { return status; }
    public LocalDate getDateBucket()    { return dateBucket; }
    public Instant getTimestamp()       { return timestamp; }
    public String getTrackingCode()     { return trackingCode; }
    public String getCity()             { return city; }
}
```

---

## Features Core

### 1. Rastreio Público por Código
Consulta principal do sistema — o usuário digita o código e vê a timeline completa de eventos
em ordem cronológica, com cidade, status e horário de cada etapa.

**Por que é bom no Cassandra:** leitura por partition key, O(1) independente do volume total.

---

### 2. Mapa da Rota Percorrida
Cada evento carrega latitude e longitude do hub onde foi registrado. A rota é desenhada
no mapa conectando os pontos em ordem cronológica.

**Feature extra — animação da rota:** reproduzir o percurso da encomenda como uma animação
no mapa (Angular + Leaflet), mostrando o pacote "se movendo" entre cidades ao longo do tempo.

---

### 3. Registro de Eventos com Logged Batch
Quando o admin registra que um pacote passou por um hub, a escrita acontece
simultaneamente em três tabelas (`events_by_code`, `events_by_city`, `events_by_status`)
dentro de um **Cassandra Logged Batch** — garantia de atomicidade mesmo entre tabelas.

**Por que mostrar isso:** demonstra entendimento de que denormalização no Cassandra exige
consistência nas escritas multi-tabela.

---

### 4. Criação de Encomenda com Lightweight Transaction
O código de rastreio único é garantido com `INSERT INTO packages ... IF NOT EXISTS` —
Lightweight Transaction (LWT) do Cassandra. Se dois operadores tentarem cadastrar o mesmo
código ao mesmo tempo, apenas um vai passar.

**Por que mostrar isso:** o Cassandra não tem sequências nem UUIDs auto-incrementados nativos
como bancos relacionais. LWT é a resposta distribuída para unicidade.

---

### 5. QR Code por Encomenda
Ao cadastrar uma encomenda, o sistema gera automaticamente um QR Code (ZXing — já na pom.xml)
que aponta para a URL de rastreio público. O admin pode imprimir ou exportar.

---

### 6. Atividade por Hub (Cidade) Hoje
Tela admin: seleciona uma cidade e vê todos os pacotes que passaram por lá no dia atual.
Possível graças à tabela `events_by_city` com `data_bucket = today()`.

**Por que é interessante:** mostra que no Cassandra você não faz `WHERE cidade = X` em
qualquer tabela — você cria uma tabela específica para esse access pattern.

---

### 7. Pacotes por Status em Tempo Real
Tela admin: lista de pacotes com status `EM_TRANSITO` ou `SAIU_PARA_ENTREGA` hoje.
Usa a tabela `events_by_status` — mesmo conceito de access pattern explícito.

---

### 8. Atualização em Tempo Real com SSE
Quando o usuário abre a página de rastreio de um pacote, o navegador estabelece uma
conexão SSE (Server-Sent Events) com o servidor. Quando o admin registra um novo evento,
o servidor empurra a atualização para todos os clientes que estão acompanhando aquele código
— a timeline atualiza sozinha, sem F5.

Fluxo:
```
Usuário abre /rastreio/BR123
  → navegador abre GET /rastreio/BR123/stream (conexão SSE fica aberta)

Admin registra novo evento em BR123
  → POST /packages/BR123/eventos
  → Logged Batch no Cassandra
  → servidor empurra evento pelo canal SSE de BR123
  → timeline do usuário atualiza em tempo real
```

**Implementação no Spring Boot:** `SseEmitter` — nativo, sem dependência extra, sem Redis,
sem WebSocket. Cada código de rastreio tem seu próprio canal de emitters ativos.

**Por que é bom pro demo:** abre a página de rastreio num navegador e o painel admin em outro.
O professor vê a timeline atualizar em tempo real enquanto o admin registra eventos.

---

### 9. Autenticação com JWT de curta duração
Tabela `users` no Cassandra com o admin pré-cadastrado via script de seed — sem tela
de cadastro, sem fluxo de criação de conta. A senha é armazenada com hash bcrypt.

```cql
-- executado na inicialização do projeto (idempotente)
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '$2a$...', 'ADMIN')
IF NOT EXISTS;
```

Fluxo de login:
```
POST /auth/login { username, password }
  → busca usuário no Cassandra por username (partition key — O(1))
  → bcrypt.verify(password, hash)
  → retorna JWT (15 min) + refresh token se válido
```

JWT de curta duração elimina a necessidade de blacklist — o token expira sozinho,
tornando o logout seguro sem nenhum armazenamento extra.

---

### 10. TTL Automático em Eventos Antigos
Eventos de packages entregues há mais de 90 dias são configurados com TTL nativo do
Cassandra — o banco apaga automaticamente sem nenhum job de limpeza.

```cql
INSERT INTO events_by_code (...) VALUES (...) USING TTL 7776000; -- 90 dias
```

**Por que mostrar isso:** feature que não existe em bancos relacionais nativamente.
Útil para compliance e economia de armazenamento.

---

### 11. Idempotência no Scanner
Se um scanner de hub registrar o mesmo evento duas vezes (falha de rede, duplo clique),
o Cassandra simplesmente sobrescreve com os mesmos dados — comportamento de upsert nativo.
A API de registro de eventos é naturalmente idempotente.

**Por que mostrar isso:** em sistemas distribuídos, idempotência é crítica. O Cassandra
entrega isso sem código extra.

---

## Features Secundárias (se sobrar tempo)

- **Estimativa de entrega:** baseado no tempo médio entre eventos de packages similares
  (origem → destino), calculado na camada de aplicação
- **Histórico de rotas populares:** quais pares origem-destino têm mais volume — via tabela
  `rotas_por_volume` com counter columns do Cassandra
- **Notificação por e-mail:** quando status muda para `ENTREGUE`, dispara e-mail ao destinatário

---

## O que foi removido e por quê

| Feature | Motivo |
|---|---|
| Dashboard com métricas globais (totais, tempo médio) | Requer aggregações que o Cassandra não suporta — forçaria lógica complexa na aplicação sem ganho |
| Sistema de avaliações com média por rota | `GROUP BY` + `AVG` são anti-patterns em Cassandra |
| Relatórios administrativos | Consultas ad-hoc não funcionam bem com tabelas de access pattern fixo |
| Cache de rastreio no Redis | Cassandra já é O(1) por partition key — não há gargalo real a resolver. Cache criaria complexidade de invalidação sem benefício concreto |
| Redis (JWT blacklist) | Tokens de curta duração (15 min) resolvem o problema de logout sem precisar de infraestrutura extra |

---

## Estrutura do Projeto

```
src/main/java/com/jps/jps/
│
├── JpsApplication.java
│
├── shipment/                             # cadastro de pacotes (admin)
│   ├── Shipment.java                     @Table("packages") — record
│   ├── ShipmentRequest.java              DTO de entrada (POST)
│   ├── ShipmentResponse.java             DTO de saída (admin — inclui sender/recipient)
│   ├── ShipmentRepository.java
│   ├── ShipmentService.java
│   ├── ShipmentController.java           /shipments
│   └── ShipmentNotFoundException.java    @ResponseStatus(NOT_FOUND)
│
├── tracking/                             # rastreio público (sem auth)
│   ├── TrackingResponse.java             agregado (Shipment + timeline) — sem sender/recipient (LGPD)
│   ├── TrackingService.java              orquestra ShipmentService + EventByCodeService
│   └── TrackingController.java           /rastreio/{trackingCode}
│
├── user/
│   ├── User.java                         @Table("users") — record
│   ├── Role.java                         enum com Integer id + fromId()
│   └── RoleConverter.java                Read (Integer→Role) + Write (Role→Integer)
│
└── event/
    ├── eventByCode/
    │   ├── EventByCode.java              @Table("events_by_code") — classe imutável
    │   ├── EventByCodeRepository.java    findByTrackingCode(String) — partition key
    │   ├── EventByCodeService.java       devolve List<TimelineEventResponse>
    │   └── TimelineEventResponse.java    DTO de um item da timeline (sem trackingCode)
    ├── eventByCity/
    │   └── EventByCity.java              @Table("events_by_city") — classe imutável
    └── eventByStatus/
        └── EventByStatus.java            @Table("events_by_status") — classe imutável
```

> **Observação:** `RoleConverter` contém um typo — método `covert` deve ser `convert`.

---

## Contratos de API públicos

### `GET /rastreio/{trackingCode}` — rastreio público (sem auth)

Resposta agregada combinando pacote + timeline. **Não inclui `sender`/`recipient`** por LGPD —
quem precisa ver dados pessoais é o admin via `/shipments/{trackingCode}`.

`currentStatus` é derivado do primeiro evento (clustering `timestamp DESC` já entrega ordenado).
Se o pacote existe mas ainda não tem eventos, `currentStatus = "REGISTERED"` e `events = []`.
Se o `trackingCode` não existe, retorna `404` via `ShipmentNotFoundException`.

```json
{
  "trackingCode": "BR123ABC",
  "origin": "São Paulo",
  "destination": "Recife",
  "createdAt": "2026-06-20T14:00:00Z",
  "currentStatus": "IN_TRANSIT",
  "events": [
    { "timestamp": "...", "city": "Campinas", "status": "IN_TRANSIT",
      "latitude": -22.9, "longitude": -47.0, "notes": "..." },
    { "timestamp": "...", "city": "São Paulo", "status": "COLLECTED",
      "latitude": -23.5, "longitude": -46.6, "notes": "..." }
  ]
}
```

---

## Fluxo Principal do Sistema

```
[Usuário público]
  digita código → GET /rastreio/{codigo}
    → Cassandra events_by_code → retorna timeline
  abre página      → GET /rastreio/{codigo}/stream (SSE — conexão fica aberta)
    → quando admin registra evento, servidor empurra atualização → timeline atualiza sem F5

[Admin]
  login              → POST /auth/login → JWT (15 min) + refresh token
  logout             → cliente descarta o token — expira sozinho
  cadastra encomenda → POST /packages (LWT IF NOT EXISTS + gera QR Code)
  registra evento    → POST /packages/{codigo}/eventos
                          → Logged Batch: escreve nas 3 tabelas simultaneamente
                          → empurra evento via SSE para clientes conectados nesse código
  vê hub activity    → GET /hubs/{cidade}/hoje → events_by_city
  vê por status      → GET /status/{status}/hoje → events_by_status
```
