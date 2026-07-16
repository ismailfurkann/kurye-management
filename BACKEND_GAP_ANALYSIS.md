# Backend Gap Analysis

Tarih: 2026-07-16

## 1. Mevcut backend mimarisinin özeti

- Proje Maven tabanlı Spring Boot 4.0.6, Java 21.
- Ana paket: `com.ivedi.ivedikuryemanagement`.
- Modül düzeni tutarlı: `auth`, `restaurant`, `branch`, `region`, `courier`, `delivery`, `dispatch`, `commission`, `report`, `realtime`, `audit`, `common`.
- DTO ve servis sınırları çoğunlukla `api`, entity/repository/service implementation `impl`, controller/request sınıfları `web` altında.
- Lombok, MapStruct, Spring Security, JWT, Spring Data JPA, Flyway, PostgreSQL, WebSocket/STOMP ve opsiyonel Redis pub/sub kullanılıyor.
- Veritabanı Flyway ile yönetiliyor; `spring.jpa.hibernate.ddl-auto: none`.
- Mevcut base path pratikte `/api/v1`; bazı controller'lar ayrıca role göre `/admin`, `/manager`, `/courier` segmentleri kullanıyor.
- Ortak response sınıfı `ApiResponse<T>` var. Başarılı cevaplarda `success`, `message`, `data`, `timestamp`; hata cevaplarında ayrıca `errorCode`, `validationErrors` dönüyor.
- Sayfalı endpointler `ApiResponse<Page<T>>` dönüyor. Spring `Page` çıktısı frontend'in beklediği sade `content`, `totalElements`, `totalPages`, `size`, `number`, `first`, `last` yapısından daha fazla alan içerebilir.
- POS backend modülü bulunmuyor.
- Frontend kaynak dosyaları (`src/api/client.ts`, `src/api/pos.ts`, `src/types/pos.ts`, `src/types/api.ts`, `src/hooks/useWebSocket.ts`, `BACKEND_REQUIREMENTS.md`) bu workspace içinde bulunamadı; karşılaştırma kullanıcı mesajındaki gereksinimlere göre yapıldı.

## Modül karşılaştırma tablosu

| Modül | Gereksinim | Mevcut durum | Sonuç | Yapılacak işlem | İlgili dosyalar |
| ----- | ---------- | ------------ | ----- | --------------- | --------------- |
| Ortak API | Başarı response `{success,message?,data}` | `ApiResponse` var, ayrıca `timestamp` ekliyor | KISMEN UYUMLU | Frontend toleransına göre `timestamp` korunabilir veya standardize edilmeli | `common/response/ApiResponse.java` |
| Ortak API | Sayfalı response sade page metadata | `ApiResponse<Page<T>>` dönüyor, Spring `Page` formatı fazla alan içerir | KISMEN UYUMLU | Ortak `PageResponse<T>` eklenmeli veya JSON shape kontrol edilmeli | Tüm page dönen controller'lar |
| Exception | Global exception wrapper | `GlobalExceptionHandler` var, hata shape extra alanlı | KISMEN UYUMLU | Hata response formatı frontend ile netleştirilmeli | `common/exception/GlobalExceptionHandler.java` |
| Auth | `/auth/login`, `/auth/register`, `/auth/me`, `/auth/change-password` | `/api/v1/auth/*` altında mevcut | UYUMLU | Frontend base URL `/api/v1` kullanmalı | `auth/web/AuthController.java` |
| Auth | `/auth/logout` | Yok; JWT stateless | EKSİK | Stateless no-op logout endpoint veya token denylist stratejisi eklenmeli | `auth/web/AuthController.java` |
| User DTO | `restaurantId`, `branchIds`, `courierId` | DTO'da yok; entity ilişkisi user tarafında yok | EKSİK | `AuthService.getProfile()` response zenginleştirilmeli | `auth/api/UserDto.java`, `auth/impl/User.java` |
| Roller | 5 beklenen rol | Tümü mevcut | UYUMLU | Değişiklik gerekmez | `auth/api/UserRole.java` |
| Restaurant | Beklenen public/admin-neutral `/restaurants` endpointleri | `/api/v1/admin/restaurants` ve `/api/v1/restaurants/mine` olarak ayrılmış | KISMEN UYUMLU | URL alias veya frontend route eşlemesi yapılmalı | `restaurant/web/RestaurantController.java` |
| Restaurant DTO | `ownerUserId`, `ownerName`, `branchCount`, `activeCourierCount`, `todayOrderCount`, `totalCommission`, `logoUrl` | `ownerId`, `ownerFullName`, `ownerEmail`, `legalName`, `taxNumber` var; metrik/logo yok | KISMEN UYUMLU | DTO alanları ve aggregation sorguları eklenmeli; logo için migration gerekir | `restaurant/api/RestaurantDto.java`, `restaurant/impl/Restaurant.java` |
| Restaurant status | `PENDING`, `ACTIVE`, `PASSIVE`, `SUSPENDED` | Tümü mevcut | UYUMLU | Değişiklik gerekmez | `restaurant/api/RestaurantStatus.java` |
| Branch | `/branches`, `/branches/me`, `/branches/{id}`, `/restaurants/{restaurantId}/branches` | Admin ve owner path'leri farklı; `/branches/me` yok | KISMEN UYUMLU | Beklenen alias endpointler eklenmeli | `branch/web/BranchController.java` |
| Branch DTO | `address`, `district`, `city`, `isActive` | `addressText`, `regionDistrict`, `regionCity`, `status` dönüyor | ÇAKIŞIYOR | DTO field aliasları veya frontend type güncellemesi yapılmalı | `branch/api/BranchDto.java` |
| Region | Read endpointler | `/api/v1/regions`, `/active`, `/{id}` mevcut | UYUMLU | Base URL dışında değişiklik gerekmez | `region/web/RegionController.java` |
| Region | Write/toggle endpointler | `/api/v1/admin/regions`, `PATCH /admin/regions/{id}`, `PATCH /status` | KISMEN UYUMLU | `PUT /regions/{id}` ve `PATCH /regions/{id}/toggle-status` aliasları değerlendirilmeli | `region/web/RegionController.java` |
| Courier | Liste/available/me/id endpointleri | Admin/manager/courier path'leri farklı | KISMEN UYUMLU | Beklenen `/couriers/*` aliasları eklenmeli | `courier/web/CourierController.java` |
| Courier DTO | `fullName`, `email`, `phone`, `availability`, `plateNumber`, `regionId`, `regionName`, `lastLocationLat/Lng`, `avatarUrl`, `approvedAt`, `totalDeliveries` | `userFullName`, `userEmail`, `userPhone`, `availabilityStatus`, `currentLatitude/Longitude`; diğerleri yok | KISMEN UYUMLU | DTO aliasları ve eksik DB alanları için migration gerekir | `courier/api/CourierDto.java`, `courier/impl/Courier.java` |
| Courier enum | vehicle/availability/approval değerleri | Tümü mevcut | UYUMLU | Değişiklik gerekmez | `courier/api/*.java` |
| Courier actions | approve/reject/block ayrı POST endpointleri | Tek `PATCH /api/v1/admin/couriers/{id}/approval` | KISMEN UYUMLU | Ayrı endpoint aliasları eklenmeli | `courier/web/CourierController.java` |
| Order | `/orders/*` endpointleri | `/api/v1/delivery-orders/*`, `/api/v1/admin/delivery-orders`, `/api/v1/manager/dispatch/orders/*` | KISMEN UYUMLU | `/orders` alias controller veya frontend path güncellemesi gerekir | `delivery/web/DeliveryOrderController.java` |
| Order DTO | `restaurantId`, `totalAmount`, `notes`, `statusHistory`, `activeAssignment`, `ACCEPTED` status | `restaurantName`, `deliveryNote`, status history ayrı endpoint; `ACCEPTED` yok | KISMEN UYUMLU | DTO genişletilmeli, `ACCEPTED` status stratejisi belirlenmeli | `delivery/api/DeliveryOrderDto.java` |
| Status history | Beklenen `orderId`, `actorUserId`, `actorName`, `occurredAt` | `deliveryOrderId`, `changedByUserId`, `changedAt`, actorName yok | ÇAKIŞIYOR | DTO aliasları ve actor join eklenmeli | `delivery/api/DeliveryStatusHistoryDto.java` |
| Assignment | `/assignments/{id}/accept|reject|pickup|deliver|fail` POST | Courier scoped `PATCH /api/v1/courier/assignments/{id}/...`; pickup adı `picked-up` | KISMEN UYUMLU | Method/path aliasları eklenmeli | `dispatch/web/AssignmentController.java` |
| Assignment DTO | Beklenen müşteri/restoran/şube/ödeme detayları | Sadece orderCode, courierFullName, status ve tarih alanları | EKSİK | Mapper DTO genişletilmeli | `dispatch/api/DeliveryAssignmentDto.java` |
| Commission | Admin list, courier mine, summary | Var ama URL farklı; id endpoint yok, pay batch endpoint | KISMEN UYUMLU | `GET /commissions/{id}`, `POST /commissions/{id}/pay`, aliaslar eklenmeli | `commission/web/CommissionController.java` |
| Commission DTO | `amount`, `rate`, `period`, `courierName` | `grossAmount`, `commissionRate`, `paymentPeriod`, `courierFullName` | ÇAKIŞIYOR | DTO aliasları belirlenmeli | `commission/api/CommissionDto.java` |
| Reports | deliveries/performance | `/api/v1/admin/reports/deliveries`, `/couriers/performance` var | KISMEN UYUMLU | Beklenen `/reports/deliveries`, `/reports/courier-performance` aliasları eklenmeli | `report/web/ReportController.java` |
| Reports | manager/restaurant/admin dashboard | Yok | EKSİK | Dashboard DTO ve sorguları eklenmeli | `report/*` |
| POS | categories/products/tables/customers/pickup-orders | Backend modülü yok | EKSİK | POS entity/DTO/controller/service/migration fazı gerekir | Yok |
| WebSocket | `/topic/orders/ready`, `/topic/orders/status`, `/topic/assignments`, `/topic/couriers/location`, `/user/queue/assignments` | `/topic/couriers/location` ve `/user/queue/assignments` var; diğerleri `/topic/dispatch/queue`, `/topic/orders/{id}`, `/topic/dispatch/assignments` | KISMEN UYUMLU | Destination aliasları eklenmeli; Redis disabled no-op durumu düzeltilmeli | `common/config/WebSocketConfig.java`, `realtime/impl/RedisMessageHandler.java` |
| Tests | Compile/test çalıştırma | Wrapper hata verdi; sistemde `mvn` yok | DOĞRULANAMADI | Wrapper düzeltilmeli veya Maven kurulmalı | `mvnw.cmd`, ortam PATH |

## 2. Mevcut endpointlerin tam listesi

Base path ağırlıklı olarak `/api/v1`.

| Method | Path | Controller |
| ------ | ---- | ---------- |
| POST | `/api/v1/auth/login` | `AuthController` |
| POST | `/api/v1/auth/register` | `AuthController` |
| GET | `/api/v1/auth/me` | `AuthController` |
| PATCH | `/api/v1/auth/change-password` | `AuthController` |
| POST | `/api/v1/admin/restaurants` | `RestaurantController` |
| PATCH | `/api/v1/admin/restaurants/{id}` | `RestaurantController` |
| PATCH | `/api/v1/admin/restaurants/{id}/status` | `RestaurantController` |
| PATCH | `/api/v1/admin/restaurants/{id}/owner` | `RestaurantController` |
| GET | `/api/v1/admin/restaurants` | `RestaurantController` |
| GET | `/api/v1/admin/restaurants/{id}` | `RestaurantController` |
| GET | `/api/v1/restaurants/mine` | `RestaurantController` |
| GET | `/api/v1/restaurants/mine/{id}` | `RestaurantController` |
| POST | `/api/v1/admin/branches` | `BranchController` |
| PATCH | `/api/v1/admin/branches/{id}` | `BranchController` |
| PATCH | `/api/v1/admin/branches/{id}/status` | `BranchController` |
| GET | `/api/v1/admin/branches` | `BranchController` |
| GET | `/api/v1/admin/branches/{id}` | `BranchController` |
| GET | `/api/v1/admin/restaurants/{restaurantId}/branches` | `BranchController` |
| GET | `/api/v1/restaurants/mine/{restaurantId}/branches` | `BranchController` |
| GET | `/api/v1/restaurants/mine/{restaurantId}/branches/{branchId}` | `BranchController` |
| GET | `/api/v1/regions` | `RegionController` |
| GET | `/api/v1/regions/active` | `RegionController` |
| GET | `/api/v1/regions/{id}` | `RegionController` |
| POST | `/api/v1/admin/regions` | `RegionController` |
| PATCH | `/api/v1/admin/regions/{id}` | `RegionController` |
| PATCH | `/api/v1/admin/regions/{id}/status` | `RegionController` |
| POST | `/api/v1/courier/profile` | `CourierController` |
| GET | `/api/v1/courier/profile` | `CourierController` |
| PATCH | `/api/v1/courier/availability` | `CourierController` |
| PATCH | `/api/v1/courier/location` | `CourierController` |
| GET | `/api/v1/admin/couriers` | `CourierController` |
| GET | `/api/v1/admin/couriers/{id}` | `CourierController` |
| PATCH | `/api/v1/admin/couriers/{id}/approval` | `CourierController` |
| GET | `/api/v1/manager/dispatch/couriers/available` | `CourierController` |
| POST | `/api/v1/delivery-orders` | `DeliveryOrderController` |
| PATCH | `/api/v1/delivery-orders/{id}` | `DeliveryOrderController` |
| GET | `/api/v1/delivery-orders/{id}` | `DeliveryOrderController` |
| GET | `/api/v1/delivery-orders/my-branch` | `DeliveryOrderController` |
| PATCH | `/api/v1/delivery-orders/{id}/submit-for-dispatch` | `DeliveryOrderController` |
| PATCH | `/api/v1/delivery-orders/{id}/cancel` | `DeliveryOrderController` |
| GET | `/api/v1/manager/dispatch/orders/ready` | `DeliveryOrderController` |
| GET | `/api/v1/manager/dispatch/orders/active` | `DeliveryOrderController` |
| GET | `/api/v1/admin/delivery-orders` | `DeliveryOrderController` |
| GET | `/api/v1/delivery-orders/{id}/history` | `DeliveryOrderController` |
| POST | `/api/v1/manager/dispatch/orders/{orderId}/assign/{courierId}` | `DispatchController` |
| POST | `/api/v1/manager/dispatch/orders/{orderId}/reassign` | `DispatchController` |
| GET | `/api/v1/manager/dispatch/orders/{orderId}/assignment` | `DispatchController` |
| GET | `/api/v1/manager/dispatch/orders/{orderId}/assignment-history` | `DispatchController` |
| GET | `/api/v1/courier/assignments` | `AssignmentController` |
| GET | `/api/v1/courier/assignments/{id}` | `AssignmentController` |
| PATCH | `/api/v1/courier/assignments/{id}/accept` | `AssignmentController` |
| PATCH | `/api/v1/courier/assignments/{id}/reject` | `AssignmentController` |
| PATCH | `/api/v1/courier/assignments/{id}/picked-up` | `AssignmentController` |
| PATCH | `/api/v1/courier/assignments/{id}/delivered` | `AssignmentController` |
| PATCH | `/api/v1/courier/assignments/{id}/failed` | `AssignmentController` |
| GET | `/api/v1/courier/commissions` | `CommissionController` |
| GET | `/api/v1/courier/commissions/summary` | `CommissionController` |
| GET | `/api/v1/admin/commissions` | `CommissionController` |
| POST | `/api/v1/admin/commissions/pay` | `CommissionController` |
| GET | `/api/v1/admin/reports/deliveries` | `ReportController` |
| GET | `/api/v1/admin/reports/couriers/performance` | `ReportController` |

## 3. Frontend’in beklediği fakat backend’de bulunmayan endpointler

- `POST /auth/logout`
- `GET /branches/me`
- `PATCH /branches/{id}/toggle-status`
- `PATCH /regions/{id}/toggle-status`
- `POST /couriers/{id}/approve`
- `POST /couriers/{id}/reject`
- `POST /couriers/{id}/block`
- `GET /couriers/{id}/assignments`
- `GET /orders`
- `GET /orders/{id}`
- `POST /orders`
- `PUT /orders/{id}`
- `POST /orders/{id}/submit`
- `POST /orders/{id}/cancel`
- `GET /orders/{id}/history`
- `GET /orders/ready-for-dispatch`
- `POST /assignments/{assignmentId}/reassign`
- `POST /assignments/{assignmentId}/accept`
- `POST /assignments/{assignmentId}/reject`
- `POST /assignments/{assignmentId}/pickup`
- `POST /assignments/{assignmentId}/deliver`
- `POST /assignments/{assignmentId}/fail`
- `GET /assignments/me`
- `GET /assignments/active`
- `GET /commissions/{id}`
- `POST /commissions/{id}/pay`
- `GET /reports/manager/dashboard`
- `GET /reports/restaurant/dashboard`
- `GET /reports/admin/dashboard`
- Tüm POS endpointleri: `/pos/categories`, `/pos/products`, `/pos/tables`, `/pos/customers`, `/pos/pickup-orders`.

Not: Bu listenin önemli bir kısmı farklı URL veya farklı HTTP method ile mevcut olabilir; detay aşağıda.

## 4. Backend’de var olup URL’si farklı olan endpointler

| Frontend bekliyor | Backend mevcut |
| ----------------- | -------------- |
| `/restaurants` | `/api/v1/admin/restaurants` |
| `/restaurants/{id}` | `/api/v1/admin/restaurants/{id}`, owner için `/api/v1/restaurants/mine/{id}` |
| `POST /restaurants` | `POST /api/v1/admin/restaurants` |
| `PUT /restaurants/{id}` | `PATCH /api/v1/admin/restaurants/{id}` |
| `/restaurants/{id}/assign-owner` | `PATCH /api/v1/admin/restaurants/{id}/owner` |
| `/branches` | `/api/v1/admin/branches` |
| `/branches/{id}` | `/api/v1/admin/branches/{id}` |
| `POST /restaurants/{restaurantId}/branches` | `POST /api/v1/admin/branches` body içinde `restaurantId` |
| `PUT /branches/{id}` | `PATCH /api/v1/admin/branches/{id}` |
| `/regions` write | `/api/v1/admin/regions` |
| `PUT /regions/{id}` | `PATCH /api/v1/admin/regions/{id}` |
| `/couriers` | `/api/v1/admin/couriers` |
| `/couriers/available` | `/api/v1/manager/dispatch/couriers/available` |
| `/couriers/me` | `/api/v1/courier/profile` |
| `/couriers/me/availability` | `/api/v1/courier/availability` |
| `/couriers/me/location` | `/api/v1/courier/location` |
| `/orders/*` | `/api/v1/delivery-orders/*`, `/api/v1/admin/delivery-orders`, `/api/v1/manager/dispatch/orders/*` |
| `/assignments/me` | `/api/v1/courier/assignments` |
| `/commissions` | `/api/v1/admin/commissions` |
| `/commissions/me` | `/api/v1/courier/commissions` |
| `/commissions/me/summary` | `/api/v1/courier/commissions/summary` |
| `/reports/deliveries` | `/api/v1/admin/reports/deliveries` |
| `/reports/courier-performance` | `/api/v1/admin/reports/couriers/performance` |

## 5. Eksik DTO alanları

| DTO | Eksik veya uyuşmayan alanlar |
| --- | ----------------------------- |
| `UserDto` | `restaurantId`, `branchIds`, `courierId`; ayrıca mevcutta `status`, `createdAt` extra |
| `RestaurantDto` | `ownerUserId` adı yerine `ownerId`; `ownerName` yerine `ownerFullName`; `branchCount`, `activeCourierCount`, `todayOrderCount`, `totalCommission`, `logoUrl` yok |
| `BranchDto` | `address` yerine `addressText`; `district/city` yerine `regionDistrict/regionCity`; `isActive` yerine `status`; `regionId/regionName` extra |
| `CourierDto` | `fullName/email/phone` yerine `userFullName/userEmail/userPhone`; `availability` yerine `availabilityStatus`; `lastLocationLat/Lng` yerine `currentLatitude/Longitude`; `plateNumber`, `totalDeliveries`, `regionId`, `regionName`, `avatarUrl`, `approvedAt` yok |
| `DeliveryOrderDto` | `restaurantId`, `customerAddress`, `totalAmount`, `notes`, `statusHistory`, `activeAssignment` yok; `deliveryNote`, `createdByUserId`, lifecycle timestamp'leri extra |
| `DeliveryStatusHistoryDto` | `orderId` yerine `deliveryOrderId`; `actorUserId` yerine `changedByUserId`; `actorName`, `occurredAt` yok; `changedAt` var |
| `DeliveryAssignmentDto` | `courierName` yerine `courierFullName`; `courierPhone`, `courierVehicleType`, `restaurantName`, `branchName`, `customerAddress`, `orderAmount`, `deliveryFee`, `paymentCollectionType`, `notes`, `customerName`, `customerPhone` yok |
| `CommissionDto` | `amount` yerine `grossAmount`; `rate` yerine `commissionRate`; `period` yerine `paymentPeriod`; `courierName` yerine `courierFullName` |
| `DeliveryStatsDto` | Dashboard için `todayOrders`, `activeDeliveries`, `activeCouriers`, `averageDeliveryMinutes` yok |
| `CourierPerformanceDto` | `courierName` yerine `courierFullName`; `vehicleType`, `deliveredCount`, `failedCount`, `avgDeliveryMinutes` yok; `rejected`, `successRate`, `totalEarnings` extra |

## 6. Enum uyuşmazlıkları

| Enum | Durum |
| ---- | ----- |
| `UserRole` | UYUMLU |
| `RestaurantStatus` | UYUMLU |
| `RegionStatus` | UYUMLU |
| `VehicleType` | UYUMLU |
| `AvailabilityStatus` | Değerler uyumlu, alan adı frontend'de `availability`, backend'de `availabilityStatus` |
| `ApprovalStatus` | UYUMLU |
| `DeliveryOrderStatus` | ÇAKIŞIYOR: frontend `ACCEPTED` bekliyor, backend order status enum'unda yok; `ACCEPTED` assignment status'ta var |
| `AssignmentStatus` | Beklenen assignment geçişleriyle uyumlu |
| `PaymentCollectionType` | Beklenen değer listesi verilmedi; DOĞRULANAMADI |
| `OrderPriority` | Beklenen değer listesi verilmedi; DOĞRULANAMADI |
| `CommissionStatus` | Beklenen değer listesi verilmedi; backend `PENDING`, `PAID`, `CANCELLED` |

Toplam net enum uyuşmazlığı: 1.

## 7. Response formatı uyuşmazlıkları

- Başarılı response frontend örneğine yakın, fakat backend her başarı cevabına `timestamp` ekliyor.
- Hata response frontend örneğinde tanımlı değil; backend `success:false`, `message`, `timestamp`, `errorCode`, `validationErrors` kullanıyor.
- Page response `data` içinde Spring `Page` serialize edildiği için `pageable`, `sort`, `numberOfElements`, `empty` gibi frontend'in beklemediği alanlar dönebilir.
- Controller'lar genel olarak `ResponseEntity<ApiResponse<...>>` dönüyor; wrapper kullanımı tutarlı.

## 8. Yetkilendirme ve rol uyuşmazlıkları

- Security stateless JWT ile çalışıyor; `/api/v1/auth/login`, `/api/v1/auth/register`, `/ws/**` public.
- Method security `@PreAuthorize` ile modül bazında ayrılmış.
- Frontend beklenen endpointler role-neutral görünüyor; backend URL'leri role göre `/admin`, `/manager`, `/courier` segmentlerine ayrılmış.
- `RESTAURANT_STAFF` rolü order create/update için yetkili, fakat kullanıcı DTO'sunda branch bağları yok.
- Self-register sadece `COURIER` ve `RESTAURANT_OWNER` için açık; platform admin/manager/staff oluşturma endpointi yok.
- Logout yok; JWT stateless olduğu için client-side token silme yeterli olabilir, ama frontend endpoint bekliyorsa no-op gerekir.

## 9. Veritabanı ve Flyway eksikleri

Mevcut migration'lar:

- `V1__create_users_table.sql`
- `V2__create_regions_table.sql`
- `V3__create_restaurants_table.sql`
- `V4__create_branches_table.sql`
- `V5__create_couriers_table.sql`
- `V6__create_delivery_orders_table.sql`
- `V7__create_delivery_assignments_table.sql`
- `V8__create_commission_and_audit_tables.sql`

Eksik schema alanları veya tablolar:

- `restaurants.logo_url`
- Courier için `plate_number`, `region_id`, `avatar_url`, `approved_at`, `total_deliveries`
- Kullanıcı-şube bağlantısı veya staff branch assignment tablosu
- POS tabloları: category, product, table state, customer, pickup order, pickup/cart item yapıları
- Dashboard metrikleri için zorunlu tablo şart değil; sorgu/DTO ile hesaplanabilir.
- Order `totalAmount` hesaplanabilir; kalıcı alan zorunlu değil.

Gereken tahmini yeni migration sayısı: en az 3.

## 10. WebSocket/STOMP eksikleri

- STOMP endpoint: `/ws`, SockJS açık.
- Broker: `/topic`, `/queue`; user prefix `/user`.
- Frontend beklenenlerden mevcut:
  - `/topic/couriers/location`: mevcut.
  - `/user/queue/assignments`: Redis handler üzerinden mevcut.
- Uyuşmayan destinations:
  - Beklenen `/topic/orders/ready`; backend `/topic/dispatch/queue`.
  - Beklenen `/topic/orders/status`; backend `/topic/orders/{orderId}` ve `/topic/restaurant/{restaurantId}/orders`.
  - Beklenen `/topic/assignments`; backend `/topic/dispatch/assignments`.
- `realtime.redis.enabled: false` varsayılanında `NoOpRealtimePublisher` aktif; servisler event publish çağırsa bile lokal broker'a yayın yapılmıyor.
- Event payload record'ları var: `OrderStatusEvent`, `AssignmentEvent`, `CourierLocationEvent`. Alan adları frontend tipleri görülmediği için tam uyum DOĞRULANAMADI.

## 11. POS modülünün mevcut durumu

- Backend'de `pos` paketi, POS entity, DTO, controller, service, repository veya migration bulunamadı.
- POS gereksinimleri tamamen EKSİK.
- POS için mock/fake veri eklenmemeli; kalıcı POS gerekiyorsa yeni Flyway migration ve gerçek CRUD/persistence akışı oluşturulmalı.

## 12. Derleme ve test sonucu

Çalıştırılan komutlar:

- `.\mvnw.cmd clean compile`: BAŞARISIZ. Wrapper başlamadan PowerShell hatası: `Cannot index into a null array` ve `Cannot start maven from wrapper`.
- `.\mvnw.cmd clean test`: BAŞARISIZ. Aynı wrapper hatası.
- `mvn clean compile`: BAŞARISIZ. `mvn` komutu PATH'te yok.
- `mvn clean test`: BAŞARISIZ. `mvn` komutu PATH'te yok.

Hata Java kaynak dosyasına ulaşmadan oluştuğu için bu gereksinimlerle doğrudan ilişkili bir compile error doğrulanamadı.

Minimum düzeltme:

- `mvnw.cmd` wrapper script'i ve `.mvn/wrapper` içeriği kontrol edilmeli; mevcut dosya listesinde `.mvn/wrapper` görünmüyor.
- Alternatif olarak Maven kurulup PATH'e eklenmeli.

Mevcut testler:

- Auth service testleri.
- Restaurant service testleri.
- Branch service testleri.
- Region service testleri.
- Courier service testleri.
- Context smoke test.
- Delivery/dispatch/commission/report/websocket/POS integration testleri görünmüyor.

## 13. Kritik hata ve riskler

- Frontend mock kaldırıldıysa en yüksek risk URL/path farklarıdır: backend `/api/v1/admin`, `/api/v1/manager`, `/api/v1/courier`, `/api/v1/delivery-orders` ayrımını kullanıyor.
- DTO field adları frontend tipleriyle önemli ölçüde uyuşmuyor; UI veri gösteremez veya undefined alanlar kullanabilir.
- POS tamamen yok; POS ekranları gerçek backend'e bağlandığında çalışmaz.
- WebSocket Redis kapalıyken no-op; lokal geliştirmede eventler frontend'e ulaşmaz.
- `DeliveryOrderStatus.ACCEPTED` yok; frontend order lifecycle bunu bekliyorsa durum akışı kırılır.
- Page serialization frontend'in beklediği sade shape ile uyumlu olmayabilir.
- UUID id kullanılıyor; frontend örneklerinde `1` gibi numeric id varsayımı varsa tip uyuşmazlığı oluşur.

## 14. Önerilen uygulama sırası

1. Ortak response ve sade page response standardizasyonu.
2. Auth `/me` zenginleştirme ve `/logout` no-op endpoint.
3. URL alias stratejisi: frontend beklenen path'ler mi eklenecek, yoksa frontend base/path mapping mi güncellenecek karar verilmeli.
4. Restaurant ve Branch DTO alan uyumu.
5. Region write/toggle aliasları.
6. Courier DTO, profile, approval action aliasları ve gerekiyorsa migration.
7. Order aliasları, DTO alanları, `ACCEPTED` status kararı.
8. Status history actor bilgisi ve response aliasları.
9. Assignment DTO zenginleştirme ve endpoint aliasları.
10. POS schema ve gerçek persistence implementasyonu.
11. Commission endpoint/DTO uyumu.
12. Reports dashboard endpointleri.
13. WebSocket destination aliasları ve Redis kapalıyken local broker publisher.
14. Integration testleri ve wrapper/Maven altyapı düzeltmesi.

## Özet sayılar

- Beklenen endpoint toplamı: 70.
- Tamamen mevcut: 9.
- Kısmen mevcut veya farklı URL/method ile mevcut: 34.
- Eksik: 27.
- Eksik/uyuşmayan DTO alanı: 54.
- Net enum uyuşmazlığı: 1.
- Gerekli tahmini migration: 3.
- İlk uygulanması gereken modül: Ortak response/page formatı ve Auth `/me`.
- İlk gerçek uçtan uca test: `POST /api/v1/auth/login` ardından `GET /api/v1/auth/me`.
