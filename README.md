# Kurumsal Telekom OMS & SLCM Simülasyonu

Bir telekom operatörünün Order Management System (OMS) ve Subscriber Lifecycle Management (SLCM) süreçlerini simüle eden, uçtan uca bir örnek uygulama: sipariş alımı, arka planda asenkron doğrulama/provisioning, ve bunu izleyen bir React arayüzü.

## Mimari

```
┌─────────────┐        /api/v1/*        ┌──────────────────────┐
│   React     │ ───────────────────────▶ │   Spring Boot        │
│  (frontend) │  (Nginx reverse proxy /  │   (backend)           │
│             │   Vite dev proxy)         │                       │
└─────────────┘                          │  Auth (Bearer token)  │
                                          │  Catalog / Eligibility│
                                          │  Order state machine  │
                                          │  (async, @Async)      │
                                          └───────────┬───────────┘
                                                       │
                                                  SQLite (dosya,
                                                  host'a bind-mount)
```

- **Backend** (`backend/`): Spring Boot 3.3.4, Java 17, SQLite (dosya tabanlı, tek-yazarlı — Hikari pool boyutu bilinçli olarak 1). Katmanlar: `domain`/`repository` (JPA), `service` (iş mantığı + state machine), `controller` (REST), `config` (async/auth altyapısı), `exception` (hata yönetimi).
- **Frontend** (`frontend/`): React + Vite, backend'den tamamen ayrı/decoupled bir proje. Kod her zaman göreli `/api/v1/...` path'lerine istek atar; dev'de Vite proxy, production'da Nginx bu isteği backend'e yönlendirir — CORS config'e hiç gerek yok.

## Sipariş yaşam döngüsü (state machine)

```
PENDING ──(async)──▶ VALIDATING ──▶ PROVISIONING ──▶ COMPLETED
                          │                              
                          └──(kural ihlali)──▶ FAILED
```

1. **Intake** (senkron): `POST /api/v1/orders` isteği anında `PENDING` kaydı yazar ve `orderId` döner.
2. **Validation** (arka planda, `@Async`): `VALIDATING`'e geçer, üç kural sırayla kontrol edilir — MSISDN `BARRED` mi, ürünün `target_segment`'i abone tipiyle eşleşiyor mu, abonenin bu ürüne zaten aktif bir aboneliği var mı. Herhangi biri tutarsa `FAILED` + sebep.
3. **Provisioning**: Kurallar geçilirse `PROVISIONING`'e geçer, 2-5 saniye rastgele bekleme (şebeke/billing entegrasyonu simülasyonu) sonrası `ACTIVE_SUBSCRIPTIONS`'a kayıt düşer ve `COMPLETED` olur.

Her durum geçişi kendi kısa transaction'ında yazılır; provisioning'deki bekleme hiçbir transaction'ın içinde değildir — SQLite'ın tek bağlantılı havuzu bir bekleme süresince kilitlenmesin diye.

## Kimlik doğrulama

Gerçek (mock olmayan) minimal bir auth katmanı: `POST /api/v1/auth/signup` ve `/signin`, şifreler `BCrypt` ile hash'lenir, `signin` bir bearer token döner. `/api/v1/catalog/**`, `/eligibility/**`, `/orders/**` bu token'ı ister (`Authorization: Bearer <token>`); `/auth/**` ve Swagger açıktır.

## API özeti

| Endpoint | Açıklama |
|---|---|
| `POST /api/v1/auth/signup` | Kullanıcı kaydı |
| `POST /api/v1/auth/signin` | Giriş, bearer token döner |
| `GET /api/v1/catalog/products` | Ürün kataloğu |
| `GET /api/v1/eligibility/{msisdn}` | Abone tipi/durumu |
| `POST /api/v1/orders` | Sipariş oluştur (202 Accepted) |
| `GET /api/v1/orders/{orderId}` | Sipariş durumu (polling) |

Swagger UI: `http://localhost:8080/swagger-ui.html` (backend ayaktayken).

## Yerel geliştirme

```bash
# Backend (JDK 17+, Maven gerekli)
cd backend
mvn spring-boot:run

# Frontend (ayrı bir terminalde)
cd frontend
npm install
npm run dev   # http://localhost:5173, /api isteklerini backend'e proxy'ler
```

## Podman ile çalıştırma

```bash
podman-compose -f podman-compose.yml up --build
```

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:8081` (host tarafında 80 değil 8081 — rootless Podman ayrıcalıklı `<1024` portları host'ta yayınlayamıyor; Nginx container içinde yine 80'de dinliyor)
- SQLite verisi `./backend/data/` dizinine (host bind mount) kalıcı olarak yazılır — konteyner yeniden başlatıldığında kaybolmaz. Gerçek `podman restart` ile test edildi: aynı kullanıcı/sipariş verisi sağlam kalıyor.
- JVM bellek/Hikari pool ayarları `podman-compose.yml`'de `JAVA_TOOL_OPTIONS` ile doğrudan verilir (standart env var yerine).
- Nginx'in backend'e giden proxy'si `backend` servis adını her istekte yeniden çözer (`${NGINX_LOCAL_RESOLVERS}` + `resolver ... valid=10s`) — backend container'ı yeniden başlayıp IP değiştirse bile frontend'i ayrıca yeniden başlatmaya gerek kalmaz.

Bu kurulum gerçek `podman-compose up --build` ile uçtan uca test edildi (signup→signin→katalog→sipariş oluştur→polling→COMPLETED, ayrıca backend restart sonrası veri kalıcılığı ve proxy'nin kendiliğinden toparlanması dahil).

## Test

```bash
cd backend
mvn test
```

`OrderProcessingServiceTest` (state machine kuralları), `ApiIntegrationTest` (4 OMS endpoint'i, gerçek HTTP), `AuthIntegrationTest` (signup/signin/token) — hepsi izole in-memory SQLite üzerinde çalışır.
