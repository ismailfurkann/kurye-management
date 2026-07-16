# Backend Implementation Plan

Tarih: 2026-07-16

Bu plan mevcut backend yapısını koruyarak küçük ve geri alınabilir fazlarla ilerler. Var olan sınıflar aynı işi yapıyorsa yeni sınıf oluşturulmaz; alias endpointler mevcut service'leri çağırır. Flyway tarafından yönetilen tablolar için mevcut migration dosyaları değiştirilmez, gereken durumda yeni migration eklenir.

## Faz 0 - Build altyapısını doğrula

- Değiştirilecek dosyalar: `mvnw.cmd` gerekirse; mümkünse önce `.mvn/wrapper` eksikliği doğrulanır.
- Yeni dosyalar: Gerekirse Maven wrapper metadata.
- Migration: Hayır.
- Tamamlanma kriteri: `.\mvnw.cmd clean compile` ve `.\mvnw.cmd clean test` Java derleme aşamasına ulaşır.
- Test edilecek endpoint: Yok.
- Beklenen response: Yok.
- Frontend doğrulaması: Yok.

## Faz 1 - Ortak response ve pagination

- Değiştirilecek dosyalar:
  - `common/response/ApiResponse.java`
  - Page dönen controller'lar: restaurant, branch, region, courier, delivery, commission.
- Yeni dosyalar:
  - `common/response/PageResponse.java` veya mevcut stile uygun eşdeğer record.
- Migration: Hayır.
- Tamamlanma kriteri: Page endpointleri `data.content`, `totalElements`, `totalPages`, `size`, `number`, `first`, `last` döner.
- Test edilecek endpoint: `GET /api/v1/regions?page=0&size=20`.
- Beklenen örnek response:

```json
{
  "success": true,
  "message": "Regions retrieved",
  "data": {
    "content": [],
    "totalElements": 0,
    "totalPages": 0,
    "size": 20,
    "number": 0,
    "first": true,
    "last": true
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Region listesi veya herhangi bir paginated admin liste ekranı.

## Faz 2 - Auth ve mevcut kullanıcı bilgisi

- Değiştirilecek dosyalar:
  - `auth/web/AuthController.java`
  - `auth/api/UserDto.java`
  - `auth/impl/AuthServiceImpl.java`
  - Gerekirse `auth/api/UserMapper.java`.
- Yeni dosyalar: Gerekirse `auth/api/CurrentUserDto.java`; yalnızca mevcut `UserDto` yeterli olmazsa.
- Migration: Hayır.
- Tamamlanma kriteri: `/auth/logout` no-op başarı döner; `/auth/me` frontend'in beklediği user alanlarını döner.
- Test edilecek endpoint: `GET /api/v1/auth/me`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fullName": "Demo User",
    "email": "user@example.com",
    "phone": null,
    "role": "COURIER",
    "restaurantId": null,
    "branchIds": [],
    "courierId": "uuid"
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Login sonrası session/profile state ve role-based navigation.

## Faz 3 - Restaurant ve Branch uyumu

- Değiştirilecek dosyalar:
  - `restaurant/web/RestaurantController.java`
  - `restaurant/api/RestaurantDto.java`
  - `restaurant/api/RestaurantMapper.java`
  - `restaurant/impl/RestaurantServiceImpl.java`
  - `branch/web/BranchController.java`
  - `branch/api/BranchDto.java`
  - `branch/api/BranchMapper.java`
  - `branch/impl/BranchServiceImpl.java`.
- Yeni dosyalar: Gerekirse alias request DTO yok; mevcut request'ler kullanılmalı.
- Migration: Evet, `restaurants.logo_url` gerekiyorsa.
- Tamamlanma kriteri: Beklenen `/restaurants/*` ve `/branches/*` endpointleri mevcut service'leri kullanır; DTO alanları frontend adlarıyla döner.
- Test edilecek endpoint: `GET /api/v1/restaurants?page=0&size=20` veya alias olarak seçilen path.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "Restaurant",
        "status": "ACTIVE",
        "ownerUserId": "uuid",
        "ownerName": "Owner",
        "branchCount": 0,
        "activeCourierCount": 0,
        "todayOrderCount": 0,
        "totalCommission": 0,
        "logoUrl": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0,
    "first": true,
    "last": true
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Restaurant list/detail, branch list/detail.

## Faz 4 - Region endpoint aliasları

- Değiştirilecek dosyalar:
  - `region/web/RegionController.java`
  - Gerekirse `region/api/RegionDto.java`.
- Yeni dosyalar: Yok.
- Migration: Hayır.
- Tamamlanma kriteri: `POST /regions`, `PUT /regions/{id}`, `PATCH /regions/{id}/toggle-status` mevcut service akışını kullanır.
- Test edilecek endpoint: `PATCH /api/v1/regions/{id}/toggle-status`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Merkez",
    "city": "Istanbul",
    "district": "Kadikoy",
    "status": "ACTIVE"
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Region management.

## Faz 5 - Courier uyumu

- Değiştirilecek dosyalar:
  - `courier/web/CourierController.java`
  - `courier/api/CourierDto.java`
  - `courier/api/CourierMapper.java`
  - `courier/impl/Courier.java`
  - `courier/impl/CourierServiceImpl.java`.
- Yeni dosyalar: Gerekirse request DTO kullanılmaz; mevcut `CourierApprovalRequest`, `CourierAvailabilityRequest`, `CourierLocationRequest` korunur.
- Migration: Evet; `plate_number`, `region_id`, `avatar_url`, `approved_at`, `total_deliveries` gibi kalıcı alanlar gerekiyorsa.
- Tamamlanma kriteri: `/couriers/*` aliasları, ayrı approve/reject/block endpointleri ve frontend field adları çalışır.
- Test edilecek endpoint: `GET /api/v1/couriers?page=0&size=20`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "userId": "uuid",
        "fullName": "Courier",
        "email": "courier@example.com",
        "phone": null,
        "vehicleType": "MOTORBIKE",
        "availability": "AVAILABLE",
        "approvalStatus": "APPROVED",
        "activeAssignmentCount": 0,
        "totalDeliveries": 0
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0,
    "first": true,
    "last": true
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Courier list/detail, approval flow, courier availability widget.

## Faz 6 - Order endpoint ve DTO uyumu

- Değiştirilecek dosyalar:
  - `delivery/web/DeliveryOrderController.java`
  - `delivery/api/DeliveryOrderDto.java`
  - `delivery/api/DeliveryOrderMapper.java`
  - `delivery/api/DeliveryOrderStatus.java`
  - `delivery/impl/DeliveryOrderServiceImpl.java`.
- Yeni dosyalar: Gerekirse frontend alias request DTO; önce mevcut request'ler denenmeli.
- Migration: DOĞRULANACAK. `ACCEPTED` order status DB check constraint'e eklenecekse yeni migration gerekir; alternatif olarak `ACCEPTED` sadece assignment status olarak tutulur ve order DTO'da aktif assignment üzerinden sunulur.
- Tamamlanma kriteri: `/orders` CRUD/list/history/ready endpointleri çalışır; `totalAmount`, `notes`, `statusHistory`, `activeAssignment` uyumu sağlanır.
- Test edilecek endpoint: `POST /api/v1/orders`.
- Beklenen örnek response:

```json
{
  "success": true,
  "message": "Order created",
  "data": {
    "id": "uuid",
    "orderCode": "ORD-1",
    "branchId": "uuid",
    "restaurantId": "uuid",
    "customerName": "Customer",
    "customerAddress": "Address",
    "orderAmount": 100,
    "deliveryFee": 20,
    "totalAmount": 120,
    "notes": null,
    "status": "CREATED"
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Order create/list/detail.

## Faz 7 - Status history

- Değiştirilecek dosyalar:
  - `delivery/api/DeliveryStatusHistoryDto.java`
  - `delivery/api/DeliveryStatusHistoryMapper.java`
  - `delivery/impl/DeliveryStatusHistory.java`
  - `delivery/impl/DeliveryOrderServiceImpl.java`.
- Yeni dosyalar: Yok.
- Migration: Gerekmez; actor name user join ile üretilebilir.
- Tamamlanma kriteri: History response `orderId`, `actorUserId`, `actorName`, `occurredAt` alanlarıyla döner.
- Test edilecek endpoint: `GET /api/v1/orders/{id}/history`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "orderId": "uuid",
      "oldStatus": "CREATED",
      "newStatus": "READY_FOR_DISPATCH",
      "actorUserId": "uuid",
      "actorName": "Staff",
      "note": null,
      "occurredAt": "2026-07-16T10:00:00Z"
    }
  ]
}
```

- Frontend üzerinde doğrulanacak ekran: Order detail status timeline.

## Faz 8 - Assignment ve durum geçişleri

- Değiştirilecek dosyalar:
  - `dispatch/web/AssignmentController.java`
  - `dispatch/web/DispatchController.java`
  - `dispatch/api/DeliveryAssignmentDto.java`
  - `dispatch/api/DeliveryAssignmentMapper.java`
  - `dispatch/impl/AssignmentServiceImpl.java`
  - `dispatch/impl/DispatchServiceImpl.java`.
- Yeni dosyalar: Gerekirse service query DTO yok; mapper genişletme tercih edilmeli.
- Migration: Hayır.
- Tamamlanma kriteri: `/assignments/*` aliasları çalışır; assignment DTO frontend'in beklediği order/customer/courier özetini döner.
- Test edilecek endpoint: `POST /api/v1/assignments/{assignmentId}/accept`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "orderId": "uuid",
    "orderCode": "ORD-1",
    "courierId": "uuid",
    "courierName": "Courier",
    "status": "ACCEPTED",
    "restaurantName": "Restaurant",
    "branchName": "Branch",
    "customerAddress": "Address"
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Courier assignment detail and action buttons, manager active assignments.

## Faz 9 - POS

- Değiştirilecek dosyalar: Yok veya sadece config yok; yeni modül eklenecek.
- Yeni dosyalar:
  - `pos/api/*Dto.java`
  - `pos/web/PosController.java`
  - `pos/impl/*Entity.java`
  - `pos/impl/*Repository.java`
  - `pos/impl/PosServiceImpl.java`
  - `pos/api/PosService.java`
  - MapStruct mapper gerekiyorsa `pos/api/PosMapper.java`.
- Migration: Evet; POS tabloları için yeni Flyway migration.
- Tamamlanma kriteri: POS endpointleri gerçek DB state'i okur/yazar; mock/fake veri yok.
- Test edilecek endpoint: `GET /api/v1/pos/categories?branchId={id}`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Food",
      "icon": "utensils"
    }
  ]
}
```

- Frontend üzerinde doğrulanacak ekran: POS products, tables, pickup orders.

## Faz 10 - Commission

- Değiştirilecek dosyalar:
  - `commission/web/CommissionController.java`
  - `commission/api/CommissionDto.java`
  - `commission/api/CommissionMapper.java`
  - `commission/impl/CommissionServiceImpl.java`.
- Yeni dosyalar: Yok.
- Migration: Hayır.
- Tamamlanma kriteri: `/commissions/{id}`, `/commissions/{id}/pay`, `/commissions/me`, `/commissions/me/summary` uyumlu döner.
- Test edilecek endpoint: `GET /api/v1/commissions/me/summary`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": {
    "totalDeliveries": 0,
    "pendingAmount": 0,
    "paidAmount": 0
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Courier earnings and admin commissions.

## Faz 11 - Reports

- Değiştirilecek dosyalar:
  - `report/web/ReportController.java`
  - `report/impl/ReportServiceImpl.java`
  - `report/impl/ReportRepository.java`.
- Yeni dosyalar:
  - `report/api/DashboardStatsDto.java`
  - `report/api/AdminDashboardStatsDto.java`
  - Gerekirse restaurant dashboard DTO.
- Migration: Hayır.
- Tamamlanma kriteri: Manager, restaurant ve admin dashboard endpointleri beklenen alanları döner.
- Test edilecek endpoint: `GET /api/v1/reports/admin/dashboard`.
- Beklenen örnek response:

```json
{
  "success": true,
  "data": {
    "totalRestaurants": 0,
    "totalBranches": 0,
    "totalCouriers": 0,
    "activeCouriers": 0,
    "pendingApprovalCouriers": 0,
    "todayDeliveries": 0,
    "totalCommission": 0,
    "failedDeliveries": 0
  }
}
```

- Frontend üzerinde doğrulanacak ekran: Admin dashboard, manager dashboard, restaurant dashboard.

## Faz 12 - WebSocket eventleri

- Değiştirilecek dosyalar:
  - `realtime/api/OrderStatusEvent.java`
  - `realtime/api/AssignmentEvent.java`
  - `realtime/api/CourierLocationEvent.java`
  - `realtime/impl/RedisMessageHandler.java`
  - `realtime/impl/NoOpRealtimePublisher.java`
  - Gerekirse yeni local broker publisher.
- Yeni dosyalar:
  - `realtime/impl/LocalRealtimePublisher.java` veya mevcut `NoOpRealtimePublisher` davranışının değiştirilmesi; mevcut stile göre karar verilmeli.
- Migration: Hayır.
- Tamamlanma kriteri: Frontend destinations yayın alır: `/topic/orders/ready`, `/topic/orders/status`, `/topic/assignments`, `/topic/couriers/location`, `/user/queue/assignments`.
- Test edilecek endpoint: `POST /api/v1/orders/{orderId}/submit` sonrası STOMP `/topic/orders/ready`.
- Beklenen örnek event:

```json
{
  "type": "ORDER_STATUS_CHANGED",
  "orderId": "uuid",
  "orderCode": "ORD-1",
  "oldStatus": "CREATED",
  "newStatus": "READY_FOR_DISPATCH",
  "occurredAt": "2026-07-16T10:00:00Z"
}
```

- Frontend üzerinde doğrulanacak ekran: Dispatch queue realtime update, courier assignment notification, courier map.

## Faz 13 - Entegrasyon testleri

- Değiştirilecek dosyalar: Mevcut testler gerekirse küçük güncellenir.
- Yeni dosyalar:
  - Controller integration tests for auth/restaurant/branch/courier/order/assignment/commission/report.
  - WebSocket smoke test mümkünse.
- Migration: Hayır.
- Tamamlanma kriteri: `.\mvnw.cmd clean test` başarılı.
- Test edilecek endpoint: En az bir happy path per module.
- Beklenen response: Her modülde `ApiResponse` ve DTO contract assertion.
- Frontend üzerinde doğrulanacak ekran: Login -> restaurant/order flow -> dispatch -> courier assignment uçtan uca akışı.

## İlk uygulanacak modül ve ilk E2E test

- İlk uygulanması gereken modül: Ortak response/page formatı ve Auth `/me` zenginleştirme.
- İlk gerçek uçtan uca test: `POST /api/v1/auth/login` ile token al, ardından `GET /api/v1/auth/me` ile frontend session objesini doğrula.
