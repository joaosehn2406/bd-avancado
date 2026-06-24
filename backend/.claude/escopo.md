# Sistema de Rastreio Logístico — Escopo e Features

## Visão Geral

Sistema de rastreio de encomendas modelado para explorar as forças do Apache Cassandra:
append-only, alta disponibilidade de leitura por chave, e múltiplos access patterns via tabelas denormalizadas.

Stack: **Java (Spring Boot) · Angular 21 · Cassandra**

**Nota:** autenticação/JWT foi removida do escopo. O painel admin é público.

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

> **Atenção:** Spring Data Cassandra auto-schema converte campos camelCase para
> **lowercase sem underscore** — `dateBucket` → `datebucket`, `trackingCode` → `trackingcode`.
> As queries `@Query` e os CQL de referência abaixo usam os nomes reais das colunas.

```cql
-- Access pattern 1: histórico completo por código de rastreio
CREATE TABLE events_by_code (
  trackingcode TEXT,
  timestamp    TIMESTAMP,
  state        TEXT,
  city         TEXT,
  status       INT,      -- 0=REGISTERED | 1=COLLECTED | 2=IN_SEPARATION | 3=IN_TRANSIT | 4=OUT_FOR_DELIVERY | 5=DELIVERED
  latitude     DOUBLE,
  longitude    DOUBLE,
  notes        TEXT,
  PRIMARY KEY (trackingcode, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Access pattern 2: encomendas que passaram por uma cidade num dado dia
CREATE TABLE events_by_city (
  city         TEXT,
  datebucket   DATE,     -- partition por dia — evita partition infinita
  timestamp    TIMESTAMP,
  trackingcode TEXT,
  status       INT,
  PRIMARY KEY ((city, datebucket), timestamp, trackingcode)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Access pattern 3: encomendas com determinado status num dado dia
CREATE TABLE events_by_status (
  status       INT,
  datebucket   DATE,
  timestamp    TIMESTAMP,
  trackingcode TEXT,
  city         TEXT,
  PRIMARY KEY ((status, datebucket), timestamp, trackingcode)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Dados cadastrais do pacote — uma linha por pacote, não cresce dentro da partição
CREATE TABLE packages (
  trackingcode TEXT PRIMARY KEY,
  sender       TEXT,
  recipient    TEXT,
  origin       TEXT,
  destination  TEXT,
  createdat    TIMESTAMP,
  weightkg     DECIMAL
);
```

---

## Entidades Java

Regra geral: **record** quando a partition key é simples. **Classe imutável com `@PersistenceCreator`** quando há partition key composta ou clustering key.

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

// ─── EventByCode.java — classe imutável (partition simples + clustering) ──
@Table("events_by_code")
public class EventByCode {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String trackingCode;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    private final String state;
    private final String city;
    private final EventStatus status;   // armazenado como INT via EventStatusConverter
    private final Double latitude;
    private final Double longitude;
    private final String notes;

    @PersistenceCreator
    public EventByCode(String trackingCode, Instant timestamp, String state, String city,
                       EventStatus status, Double latitude, Double longitude, String notes) { ... }
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

    private final EventStatus status;

    @PersistenceCreator
    public EventByCity(String city, LocalDate dateBucket, Instant timestamp,
                       String trackingCode, EventStatus status) { ... }
}

// ─── EventByStatus.java — classe imutável (partition key composta) ────────
@Table("events_by_status")
public class EventByStatus {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final Integer status;       // raw INT — partition key não passa pelo converter

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private final LocalDate dateBucket;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final String trackingCode;

    private final String city;

    @PersistenceCreator
    public EventByStatus(Integer status, LocalDate dateBucket, Instant timestamp,
                         String trackingCode, String city) { ... }
}
```

---

## Features Core (todas implementadas)

### 1. Rastreio Público por Código
Consulta principal do sistema — o usuário digita o código e vê a timeline completa de eventos
em ordem cronológica, com estado, cidade, status e horário de cada etapa.

**Por que é bom no Cassandra:** leitura por partition key, O(1) independente do volume total.

---

### 2. Registro de Eventos com Logged Batch
Quando o admin registra que um pacote passou por um hub, a escrita acontece
simultaneamente em três tabelas (`events_by_code`, `events_by_city`, `events_by_status`)
dentro de um **Cassandra Logged Batch** — garantia de atomicidade mesmo entre tabelas.

```java
cassandraTemplate.batchOps()
    .insert(eventByCode, ttlOptions)
    .insert(eventByCity, ttlOptions)
    .insert(eventByStatus, ttlOptions)
    .execute();
```

**Por que mostrar isso:** demonstra entendimento de que denormalização no Cassandra exige
consistência nas escritas multi-tabela.

---

### 3. Criação de Encomenda com Lightweight Transaction
O código de rastreio único é garantido com `INSERT INTO packages ... IF NOT EXISTS` —
Lightweight Transaction (LWT) do Cassandra. Se dois operadores tentarem cadastrar o mesmo
código ao mesmo tempo, apenas um vai passar.

```java
InsertOptions lwtOptions = InsertOptions.builder().withIfNotExists().build();
var result = cassandraTemplate.insert(shipment, lwtOptions);
if (!result.wasApplied()) throw new DuplicateTrackingCodeException(shipment.trackingCode());
```

O código de rastreio é auto-gerado pelo servidor (`BR` + 9 chars UUID hex em maiúsculas).
Retorna HTTP 409 se a inserção LWT falhar.

**Por que mostrar isso:** o Cassandra não tem sequências nem UUIDs auto-incrementados nativos
como bancos relacionais. LWT é a resposta distribuída para unicidade.

---

### 4. QR Code por Encomenda
Ao cadastrar uma encomenda, o sistema gera automaticamente um QR Code (ZXing)
que aponta para a URL de rastreio público (`{public-url}/rastreio/{code}`).
O QR Code é retornado como base64 PNG no campo `qrCode` da resposta de criação.

---

### 5. Atividade por Hub (Cidade) Hoje
Tela admin: seleciona uma cidade e vê todos os pacotes que passaram por lá no dia atual.
Usa `events_by_city` com `datebucket = LocalDate.now()`.

**Por que é interessante:** mostra que no Cassandra você não faz `WHERE city = X` em
qualquer tabela — você cria uma tabela específica para esse access pattern.

---

### 6. Pacotes por Status Hoje
Tela admin: lista de pacotes com determinado status no dia atual.
Usa `events_by_status` com `datebucket = LocalDate.now()`.

---

### 7. Atualização em Tempo Real com SSE
Quando o usuário abre a página de rastreio, o navegador estabelece uma conexão SSE
com o servidor. Quando o admin registra um novo evento, o servidor empurra a atualização
para todos os clientes que estão acompanhando aquele código — a timeline atualiza
sozinha, sem F5.

Fluxo:
```
Usuário abre /rastreio/BR123
  → GET /rastreio/BR123/stream (conexão SSE fica aberta)

Admin registra novo evento em BR123
  → POST /shipments/BR123/eventos
  → Logged Batch no Cassandra
  → SseService.publish(code, evento) → emite para todos os emitters desse código
  → timeline atualiza em tempo real
```

Implementação: `SseEmitter` nativo do Spring MVC. Cada código tem sua lista de emitters
em `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>`.
Emitters quebrados são removidos automaticamente no momento da falha de envio.

---

### 8. TTL Automático em Eventos Antigos
Eventos são inseridos com TTL de 90 dias. O Cassandra apaga automaticamente sem job de limpeza.

```java
InsertOptions ttlOptions = InsertOptions.builder().ttl(Duration.ofDays(90)).build();
```

---

### 9. Idempotência no Scanner
Se um scanner de hub registrar o mesmo evento duas vezes (falha de rede, duplo clique),
o Cassandra simplesmente sobrescreve com os mesmos dados — comportamento de upsert nativo.
A API de registro de eventos é naturalmente idempotente.

---

## Estrutura do Projeto

```
src/main/java/com/jps/jps/
│
├── JpsApplication.java
│
├── config/
│   ├── CassandraConfig.java          @Bean CassandraCustomConversions (EventStatusConverter)
│   └── GlobalExceptionHandler.java   @RestControllerAdvice — 404/409/400 padronizados
│
├── shipment/                          # cadastro de encomendas (admin)
│   ├── Shipment.java                  @Table("packages") — record
│   ├── ShipmentRequest.java           DTO entrada: sender, recipient, origin, destination, weightKg
│   ├── ShipmentResponse.java          DTO saída: inclui qrCode (base64 PNG)
│   ├── ShipmentRepository.java
│   ├── ShipmentService.java           LWT + QR Code generation
│   ├── ShipmentController.java        POST /shipments · GET /shipments/{code} · DELETE /shipments/{code}
│   │                                  POST /shipments/{code}/eventos
│   ├── ShipmentNotFoundException.java → 404
│   └── DuplicateTrackingCodeException.java → 409
│
├── tracking/                          # rastreio público (sem auth)
│   ├── TrackingResponse.java          agregado (Shipment + timeline) — sem sender/recipient (LGPD)
│   ├── TrackingService.java           orquestra ShipmentService + EventByCodeService
│   ├── TrackingController.java        GET /rastreio/{code} · GET /rastreio/{code}/stream (SSE)
│   └── SseService.java                ConcurrentHashMap de emitters por código
│
└── event/
    ├── eventByCode/
    │   ├── EventByCode.java           @Table("events_by_code") — classe imutável
    │   ├── EventByCodeRepository.java findByTrackingCode(String) — partition key
    │   ├── EventByCodeService.java    Logged Batch + TTL + SSE publish
    │   ├── EventRequest.java          DTO entrada: state, city, status (INT), lat, lng, notes
    │   ├── TimelineEventResponse.java DTO de um item da timeline
    │   ├── EventStatus.java           enum (id, name pt-BR) + fromId(int)
    │   ├── EventStatusResponse.java   DTO {id, name}
    │   └── EventStatusConverter.java  Read INT→EventStatus · Write EventStatus→INT
    │
    ├── eventByCity/
    │   ├── EventByCity.java           @Table("events_by_city") — partition composta (city, dateBucket)
    │   ├── EventByCityRepository.java @Query com datebucket (sem underscore)
    │   ├── EventByCityService.java    devolve CityActivityResponse
    │   ├── EventByCityController.java GET /hubs/{city}/hoje
    │   ├── CityEventItem.java         DTO de um item na lista de hub
    │   └── CityActivityResponse.java  DTO {city, date, total, events}
    │
    └── eventByStatus/
        ├── EventByStatus.java         @Table("events_by_status") — partition composta (status INT, dateBucket)
        ├── EventByStatusRepository.java @Query com datebucket (sem underscore)
        ├── EventByStatusService.java  devolve StatusActivityResponse
        ├── EventByStatusController.java GET /status/{statusId}/hoje
        ├── StatusEventItem.java       DTO de um item na lista de status
        └── StatusActivityResponse.java DTO {statusId, statusName, date, total, events}
```

---

## Contratos de API

### `POST /shipments` — cadastrar encomenda

```json
// Request
{ "sender": "...", "recipient": "...", "origin": "...", "destination": "...", "weightKg": 2.5 }

// Response 201
{
  "trackingCode": "BR9A3C12F40",
  "sender": "...", "recipient": "...", "origin": "...", "destination": "...",
  "createdAt": "2026-06-24T10:00:00Z",
  "weightKg": 2.5,
  "qrCode": "<base64 PNG>"
}
// 409 se LWT falhar (colisão de código — extremamente raro)
```

### `POST /shipments/{trackingCode}/eventos` — registrar evento

```json
// Request
{ "state": "SP", "city": "Campinas", "status": 3, "latitude": -22.9, "longitude": -47.1, "notes": "..." }

// Response 201
{
  "timestamp": "2026-06-24T10:05:00Z",
  "state": "SP",
  "city": "Campinas",
  "status": { "id": 3, "name": "Em trânsito" },
  "latitude": -22.9, "longitude": -47.1, "notes": "..."
}
```

### `GET /rastreio/{trackingCode}` — rastreio público

Não inclui `sender`/`recipient` (LGPD). `currentStatus` derivado do evento mais recente.
Se sem eventos → `currentStatus = {id:0, name:"Registrado"}`.

```json
{
  "trackingCode": "BR9A3C12F40",
  "origin": "Campinas SP",
  "destination": "Rio de Janeiro RJ",
  "createdAt": "2026-06-24T10:00:00Z",
  "currentStatus": { "id": 3, "name": "Em trânsito" },
  "events": [
    { "timestamp": "...", "state": "SP", "city": "Campinas",
      "status": { "id": 3, "name": "Em trânsito" },
      "latitude": -22.9, "longitude": -47.0, "notes": "..." }
  ]
}
// 404 → {"error": "Shipment not found: BR9A3C12F40"}
```

### `GET /rastreio/{trackingCode}/stream` — SSE

`Content-Type: text/event-stream`. Cada evento empurra o mesmo JSON de `TimelineEventResponse`.

### `GET /hubs/{city}/hoje` — atividade de hub

```json
{ "city": "Campinas", "date": "2026-06-24", "total": 3, "events": [ ... ] }
```

### `GET /status/{statusId}/hoje` — atividade por status

```json
{ "statusId": 3, "statusName": "Em trânsito", "date": "2026-06-24", "total": 2, "events": [ ... ] }
```

---

## Status Enum

| id | constant         | name (pt-BR)      |
|----|------------------|-------------------|
| 0  | REGISTERED       | Registrado        |
| 1  | COLLECTED        | Coletado          |
| 2  | IN_SEPARATION    | Em separação      |
| 3  | IN_TRANSIT       | Em trânsito       |
| 4  | OUT_FOR_DELIVERY | Saiu para entrega |
| 5  | DELIVERED        | Entregue          |

Armazenado como `INT` no Cassandra. Conversão `INT ↔ EventStatus` via `EventStatusConverter`
registrado em `CassandraConfig`. Status é serializado como `{id, name}` em toda resposta de API.

---

## Fluxo Principal do Sistema

```
[Usuário público]
  digita código → GET /rastreio/{codigo}
    → Cassandra events_by_code → retorna timeline agregada
  abre página   → GET /rastreio/{codigo}/stream (SSE — conexão fica aberta)
    → quando admin registra evento, servidor empurra update → timeline atualiza sem F5

[Admin (sem auth)]
  cadastra encomenda → POST /shipments
                         → LWT IF NOT EXISTS (garante uniqueness)
                         → gera QR Code ZXing → retorna base64 PNG
  registra evento    → POST /shipments/{codigo}/eventos
                         → Logged Batch: escreve nas 3 tabelas simultaneamente (TTL 90d)
                         → SseService.publish → empurra para clientes SSE conectados
  vê hub activity    → GET /hubs/{cidade}/hoje → events_by_city
  vê por status      → GET /status/{statusId}/hoje → events_by_status
```

---

## O que foi removido e por quê

| Feature | Motivo |
|---|---|
| Autenticação JWT / tabela `users` | Removida a pedido — admin público para fins de demonstração |
| Dashboard com métricas globais | Requer aggregações que o Cassandra não suporta — anti-pattern |
| Relatórios administrativos | Consultas ad-hoc não funcionam com tabelas de access pattern fixo |
| Cache Redis | Cassandra já é O(1) por partition key — sem gargalo real a resolver |
| Mapa da rota (Leaflet) | Fora do escopo do frontend implementado |
