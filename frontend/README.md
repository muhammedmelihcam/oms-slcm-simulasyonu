# OMS/SLCM Simulasyonu — Frontend

React + Vite. Backend'den tamamen ayrı, decoupled bir proje. Proje kökündeki `README.md`'de genel mimari ve çalıştırma talimatları var.

## Geliştirme

```bash
npm install
npm run dev
```

Vite dev server `5173` portunda açılır ve `/api/*` isteklerini `vite.config.js`'deki proxy ile `http://localhost:8080`'deki backend'e yönlendirir (bkz. backend'i ayrıca `mvn spring-boot:run` ile çalıştırman gerekiyor).

## Build

```bash
npm run build
```

`dist/` altına statik dosyalar üretilir; production'da bunlar Nginx üzerinden sunulur (bkz. `Containerfile` ve `nginx.conf`).
