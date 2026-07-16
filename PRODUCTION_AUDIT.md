# Production Audit

Audit tarihi: 2026-07-16

Kapsam: Spring Boot backend kodu, migration dosyalari, controller/service/repository/security/exception/test yapisi. Bu rapor kod degisikligi yapmadan hazirlandi.

## Yonetici Ozeti

| Kategori | Durum | En Kritik Bulgu |
| --- | --- | --- |
| N+1 query | VAR | Ready order ve courier assignment response mappinglerinde lazy association zinciri liste uzerinde tetiklenebilir. |
| LazyInitializationException riski | ORTA | `open-in-view=false` dogru, ancak DTO mapping transaction disina tasinirsa lazy alanlara bagimli mapperlar kirilir. |
| Transactional eksikleri | KISMEN | Genel olarak iyi; fakat report/commission/disptach bazi akislarda transaction ve propagation kararlarinin test kapsami zayif. |
| Pageable yanlislari | VAR | Commission endpointleri Spring `Page` objesini direkt response'a sizdiriyor. |
| Missing index | VAR | Case-insensitive search ve composite filtreler icin eksik indexler var. |
| Missing validation | VAR | `period`, tarih araligi, enum conversion ve bazi action reason alanlari eksik/gevsek. |
| Security aciklari | VAR | Self-register role secimi, wildcard CORS + credentials, staff data isolation boslugu. |
| Response entity hatalari | VAR | Legacy commission/order/courier endpointlerinde raw `Page<T>` donuyor. |
| DTO yerine Entity donen endpoint | BULUNMADI | Controller response'lari DTO/response wrapper uzerinden donuyor. |
| Gereksiz eager fetch | KISMEN | Specification fetch joinleri DTO ihtiyaclari icin makul; fakat count query guard'i `long.class` icin her yerde tutarli degil. |
| Gereksiz select * | VAR | Native ready dispatch query `SELECT d.*` kullaniyor. |
| Duplicate repository query | VAR | Eski/yeni aliaslar ve DTO/response ayrimi nedeniyle duplicate sorgu desenleri var. |
| Duplicate mapper | VAR | MapStruct mapperlarin yaninda manual response mapperlar daginik. |
| SonarLint kod kokulari | VAR | Mojibake yorumlar, uzun service siniflari, unused import, magic strings, impl injection. |
| Exception handling eksikleri | VAR | Enum/type mismatch, constraint violation, servlet validation, data integrity ozel handle edilmiyor. |
| Logging eksikleri | VAR | Security-sensitive login/register loglari ve bazi write actionlarda audit/log eksigi var. |
| Swagger/OpenAPI | EKSIK | `springdoc-openapi` dependency ve endpoint annotation yok. |
| Unit test coverage | KISMEN | Auth/restaurant/branch/region/courier/order alias iyi; commission/report/repository specs zayif. |
| Integration test coverage | DUSUK | Sadece auth security context agirlikli; full HTTP/RBAC/DB integration eksik. |

## Kritik Bulgular

### P0/P1 Security

| ID | Bulgu | Risk | Kanit | Oneri |
| --- | --- | --- | --- | --- |
| SEC-001 | Public register endpoint kullanicinin role secmesine izin veriyor. | Dis kullanici `PLATFORM_ADMIN`, `PLATFORM_MANAGER` veya owner role ile self-register deneyebilir. Service bazi rolleri engellemiyorsa privilege escalation. | `SecurityConfig.java:38-39`, `RegisterRequest.java` role zorunlu. | Public register sadece `COURIER` veya onayli self-service rollerine izin vermeli. Admin/manager/owner yaratma sadece admin endpoint/seed ile olmali. |
| SEC-002 | CORS `allowedOriginPatterns("*")` ve `allowCredentials(true)`. | Credential/JWT kullanan frontendlerde tum originlerden browser request kabul riski. | `SecurityConfig.java:50-55` | Ortam bazli explicit allowed origins kullan. Prod'da wildcard kapat. |
| SEC-003 | `RESTAURANT_STAFF` branch/order izolasyonu modelde yok ve kodda role-level trust var. | Staff baska branch/order verisine erisebilir veya `branchId` ile scope disina cikabilir. | `DeliveryOrderServiceImpl.java:194`, `DeliveryOrderServiceImpl.java:431-433`; `BranchServiceImpl.java:163-166` staff icin bos liste donuyor. | Staff-branch mapping tablosu ve repository-level scope zorunlu. O zamana kadar staff order list/create/update kapatilmali veya explicit business error donmeli. |
| SEC-004 | WebSocket handshake `/ws/**` permitAll; auth STOMP CONNECT'e birakilmis. | Handshake origin ve rate limit acik kalir; CONNECT validator bypass olmasa da attack surface buyur. | `SecurityConfig.java:40-41`, `StompAuthChannelInterceptor.java` | Origin allowlist, handshake interceptor/rate limit ve failed CONNECT metric ekle. |
| SEC-005 | Authentication error response ayrintili mesaj donduruyor. | Token/login hatalarinda gereksiz bilgi sizintisi. | `GlobalExceptionHandler.java:49-54` | Client'a generic mesaj, detaylari sadece loga yaz. |

## N+1 Query Bulgulari

| ID | Bulgu | Kanit | Etki | Oneri |
| --- | --- | --- | --- | --- |
| N1-001 | Ready-for-dispatch order listesi native `SELECT d.*` ile branch/restaurant fetch etmeden entity donduruyor; mapper `branch.restaurant` okuyor. | `DeliveryOrderRepository.java:43-50`, `DeliveryOrderServiceImpl.java:328-342`, `DeliveryOrderMapper.java` branch/restaurant mapping. | Ready dispatch listesinde order sayisi kadar branch/restaurant lazy select. | Native query yerine JPQL projection/fetch join veya DTO projection kullan. Gerekirse priority ordering icin computed sort/projection. |
| N1-002 | Courier active assignments listesi `deliveryOrder` fetch ediyor ama order->branch->restaurant fetch etmiyor; frontend mapper restaurant/branch alanlarini okuyor. | `DeliveryAssignmentRepository.findActiveByCourierId`, `AssignmentServiceImpl.java:getMyActiveAssignmentResponses`, `AssignmentResponseMapper.java`. | Courier `/assignments/me` ve `/assignments/active` listelerinde N+1. | Query'ye `JOIN FETCH o.branch b JOIN FETCH b.restaurant` ekle. |
| N1-003 | Commission creation `assignmentRepository.findById` ile assignment aliyor; sonra order/courier lazy alanlari kullaniliyor. Transaction icinde oldugu icin LIE yok ama ek selectler var. | `CommissionServiceImpl.java:44-56` | Delivery completion path'inde ekstra selectler. | `findByIdWithDetails` benzeri fetch query kullan. |
| N1-004 | Report native queryleri raw `Object[]` donuyor; N+1 yok ama DTO projection yerine manual mapping var. | `ReportRepository.java`, `ReportServiceImpl.java` | Refactor edilmezse alan sirasi hatalari runtime'a kalir. | Interface projection veya record projection kullan. |

## LazyInitializationException Riski

| ID | Bulgu | Kanit | Risk | Oneri |
| --- | --- | --- | --- | --- |
| LZY-001 | `spring.jpa.open-in-view=false` dogru; DTO mapperlar lazy association alanlarina bagimli. | `application.yml`, `BranchMapper`, `RestaurantMapper`, `CourierMapper`, `DeliveryOrderMapper`, `CommissionMapper`. | Mapping herhangi bir controller/helper katmanina tasinirsa LIE. | DTO mapping her zaman service transaction icinde kalsin; repository seviyesinde fetch/projection kontrati yazilsin. |
| LZY-002 | Manual response mapper `AssignmentResponseMapper` entity graph bekliyor. | `AssignmentResponseMapper.java` order/branch/restaurant/courier.user okuyor. | Yanlis repository metodu ile kullanilinca N+1 veya LIE. | Mapper dokumante edilmeli veya mapper input'u DTO projection olmali. |

## Transactional Bulgulari

| ID | Bulgu | Kanit | Risk | Oneri |
| --- | --- | --- | --- | --- |
| TRX-001 | Service siniflarinda class-level `@Transactional` yaygin; read-only metodlarda override iyi ama cok uzun transaction kapsamli helper kullanimi var. | `DeliveryOrderServiceImpl.java:56`, `CourierServiceImpl.java:50`, `BranchServiceImpl.java`, `RestaurantServiceImpl.java`. | Gereksiz write transaction veya lock suresi. | Read/write service metodlarini daha net ayir; class-level yerine method-level tercih et. |
| TRX-002 | `CommissionServiceImpl.createForDelivery` `REQUIRES_NEW` mantikli, fakat duplicate commission unique constraint hatasi business exception'a cevrilmiyor. | `CommissionServiceImpl.java:39-66`, `V8` unique assignment constraint. | Delivery tamamlanir ama commission failure sadece loglanir; tekrar deneme/uzlasma mekanizmasi belirsiz. | DataIntegrityViolationException handle + reconciliation job/endpoint ekle. |
| TRX-003 | `DispatchServiceImpl.assign` aktif assignment kontrolu + insert DB partial unique index ile korunuyor, iyi; ama concurrency hata tipi sadece optimistic lock handler'a bagli. | `DispatchServiceImpl`, `V7 uq_da_one_active_per_order`. | Unique violation generic 500 olabilir. | `DataIntegrityViolationException` -> 409 `ASSIGNMENT_CONFLICT` handler ekle. |

## Pageable ve Response Format Bulgulari

| ID | Bulgu | Kanit | Risk | Oneri |
| --- | --- | --- | --- | --- |
| PAG-001 | Commission controller direkt `Page<CommissionDto>` donduruyor. | `CommissionController.java:28-35`, `CommissionController.java:49-59` | Spring `pageable`, `sort`, `empty`, `numberOfElements` gibi internal alanlar frontend'e sizar. | `ApiResponse<PageResponse<CommissionDto>>` kullan. |
| PAG-002 | Eski delivery/courier admin endpointleri hala direkt Page donuyor. | `DeliveryOrderController.java:150`, `DeliveryOrderController.java:204`, `CourierController.java:105` | Legacy endpointlerde response tutarsizligi. | Eski endpointler korunacaksa bile wrapper `PageResponse` ile normalize edilmeli veya deprecation dokumante edilmeli. |
| PAG-003 | Commission service public methodlari page/size normalize etmiyor; controller validation'a guveniyor. | `CommissionServiceImpl.java:70-99` | Service baska yerden cagrilirsa negatif page/size exception. | `PageRequest` olusturmadan once normalize veya `Pageable` parametre kabul et. |

## Missing Index Bulgulari

| ID | Eksik Index | Kanit | Oneri |
| --- | --- | --- | --- |
| IDX-001 | `delivery_orders` search alanlari icin lower/trigram index yok. | `DeliveryOrderServiceImpl.java:494-505` `lower(orderCode/customerName/customerPhone/customerAddressText)` | PostgreSQL `pg_trgm` + GIN veya lower btree indexleri ekle. |
| IDX-002 | Order filtre kombinasyonlari icin composite index eksik. | `branchId`, `restaurantId via branch`, `status`, `priority`, `createdAt` birlikte kullaniliyor. | `(branch_id, status, priority, created_at DESC)` ve gerekirse `(status, created_at DESC)` ekle. |
| IDX-003 | `delivery_status_history` icin `(delivery_order_id, changed_at)` composite index yok. | History query `findAllByDeliveryOrder_IdOrderByChangedAtAsc`. Migration sadece ayri indexler ekliyor. | `CREATE INDEX idx_dsh_order_changed ON delivery_status_history(delivery_order_id, changed_at);` |
| IDX-004 | `delivery_assignments` courier assignment listesi icin `(courier_id, status, assigned_at DESC)` composite index yok. | `findPageByCourierIdAndStatus` courier+status+sort kullanir. | Composite index ekle. |
| IDX-005 | `commissions` search/pay icin `(courier_id, status, payment_period)` composite index yok. | `CommissionRepository.search`, `markAsPaid`. | Composite index ekle. |
| IDX-006 | `restaurants` search email/legalName lower indexleri yok. | `RestaurantSpecification` lower(name/email/legalName), migration sadece lower(name). | `lower(email)`, `lower(legal_name)` indexleri veya trigram. |
| IDX-007 | `couriers` search plateNumber ve user fullName/email/phone lower indexleri yok. | `CourierSpecification` joins user and lower search. | `lower(couriers.plate_number)`, `lower(users.full_name/email/phone)` veya trigram. |

## Missing Validation Bulgulari

| ID | Bulgu | Kanit | Oneri |
| --- | --- | --- | --- |
| VAL-001 | `period` string format validation yok. | `CommissionController.java:40-44`, `54`, `65-66`. | `@Pattern("\\d{4}-\\d{2}")` veya `YearMonth` converter kullan. |
| VAL-002 | Report date araliginda `dateFrom <= dateTo` kontrolu yok. | `ReportController.java:30-43`, `ReportServiceImpl`. | Cross-field validation veya service-level business exception. |
| VAL-003 | Rejection reason optional. | `RejectionRequest.java` sadece `@Size`. | Business kural gerektiriyorsa `@NotBlank`; opsiyonelse dokumante et. |
| VAL-004 | Enum conversion hatalari icin ozel handler yok. | RequestParam enumlar tum controllerlarda var. | `MethodArgumentTypeMismatchException` handler ile 400 validation response. |
| VAL-005 | UUID path/request param parse hatalari icin ozel handler yok. | Cok sayida `@PathVariable UUID`. | `MethodArgumentTypeMismatchException`/`ConversionFailedException` handler ekle. |

## Response Entity ve DTO Bulgulari

| ID | Bulgu | Kanit | Oneri |
| --- | --- | --- | --- |
| RSP-001 | DTO yerine Entity donen controller endpoint bulunmadi. | Controller taramasi. | Mevcut pratik korunmali. |
| RSP-002 | Legacy endpointlerde response format tutarsiz: `Page<T>` direkt donuyor. | `CommissionController`, `DeliveryOrderController`, `CourierController`. | Tum paged endpointlerde `PageResponse<T>` standardi. |
| RSP-003 | `ApiResponse.timestamp` `LocalDateTime` timezone bilgisiz. | `ApiResponse.java` | Prod API icin `OffsetDateTime`/UTC kullan. |
| RSP-004 | Auth error response message detayli. | `GlobalExceptionHandler.java:49-54` | Generic public message. |

## Query Kalitesi

| ID | Bulgu | Kanit | Oneri |
| --- | --- | --- | --- |
| QRY-001 | Native `SELECT d.*` kullanimi. | `DeliveryOrderRepository.java:43-49` | Gereken kolonlari secen projection veya JPQL. |
| QRY-002 | `CommissionRepository` Page + fetch join count query belirtmiyor. | `CommissionRepository.java:17-37` | Explicit `countQuery` ekle; Hibernate count/fetch join problemini engelle. |
| QRY-003 | Specification icinde fetch + search join ayni association icin duplicate join uretebilir. | `CourierSpecification`, `DeliveryOrderServiceImpl.buildSpec`. | Fetch ve join referanslarini ayri yonet; `query.distinct(true)` tutarli kullan. |
| QRY-004 | Raw `Object[]` aggregation yaygin. | `ReportRepository`, `CommissionRepository.aggregateEarnings`, restaurant metrics. | Interface projection/record projection. |

## Duplicate Repository Query ve Mapper Bulgulari

| ID | Bulgu | Kanit | Oneri |
| --- | --- | --- | --- |
| DUP-001 | Aynı domain icin legacy DTO ve frontend response akislari paralel repository/service metodlari olusturuyor. | `DeliveryOrderService` `search` + `searchResponses`, `getMyOrders` + `getMyOrderResponses`; courier/restaurant benzeri. | Ortak internal projection/query + iki mapper yerine tek response adapter. |
| DUP-002 | MapStruct mapperlar ve manual response mapperlar birlikte daginik. | `DeliveryOrderMapper`, `CommissionMapper`, `AssignmentResponseMapper`, `BranchResponse.from`, `RestaurantServiceImpl.toResponse`, `CourierServiceImpl.toResponse`. | Response mapping icin tutarli strateji: MapStruct decorators veya dedicated response assembler package. |
| DUP-003 | Eski ve alias controller metodlari ayni isleri tekrarliyor. | Restaurant/Branch/Region/Order/Courier controllers. | Aliaslar ince delegate olarak kalmali; dokumantasyonla deprecate edilecek eski yollar belirlenmeli. |

## SonarLint Seviyesi Kod Kokulari

| ID | Bulgu | Kanit | Oneri |
| --- | --- | --- | --- |
| SONAR-001 | Mojibake/bozuk karakterli yorumlar cok sayida dosyada var. | Controller/service yorumlari `â”€` vb. | UTF-8 normalize et veya yorumlari sadeleştir. |
| SONAR-002 | Uzun service siniflari ve cok sorumluluk. | `DeliveryOrderServiceImpl` 600+ satir, `CourierServiceImpl` 400+ satir. | Query, response assembly, authorization, state transition ayrilmali. |
| SONAR-003 | Direct implementation injection. | `ReportController.java:26` `ReportServiceImpl`. | `ReportService` interface kullan. |
| SONAR-004 | Magic string role/security expressions yaygin. | Controller `@PreAuthorize` stringleri. | Permission constants her modulde tutarli kullan. |
| SONAR-005 | Unused import var. | `DeliveryOrderServiceImpl.java:50` `Function`. | Temizle. |
| SONAR-006 | Duplicate blank line/minor formatting. | `RestaurantServiceImpl.update` civari. | Formatter/Checkstyle ekle. |

## Exception Handling Eksikleri

| ID | Eksik | Etki | Oneri |
| --- | --- | --- | --- |
| EXC-001 | `MethodArgumentTypeMismatchException` yok. | Gecersiz enum/UUID/date 500 veya standart disi response riski. | 400 + validation response. |
| EXC-002 | `ConstraintViolationException` yok. | `@RequestParam @Min/@Max` ihlalleri tutarsiz donebilir. | 400 + field/message map. |
| EXC-003 | `DataIntegrityViolationException` yok. | Unique/FK violation generic 500 olur. | 409/400 domain code mapping. |
| EXC-004 | `HttpMessageNotReadableException` yok. | Malformed JSON/enum body errors standard API wrapper disina cikabilir. | 400 wrapper. |
| EXC-005 | WebSocket auth exceptionlari global REST handler disinda. | STOMP client'a tutarsiz hata. | STOMP error handler ekle. |

## Logging ve Audit Eksikleri

| ID | Bulgu | Kanit | Oneri |
| --- | --- | --- | --- |
| LOG-001 | Login/register/change-password email ile info loglaniyor. | `AuthServiceImpl`. | Prod'da PII azalt; userId ve event id kullan. |
| LOG-002 | Order create/update/cancel/dispatch log var ama structured correlation id yok. | Service loglari. | Request id/MDC ekle. |
| LOG-003 | Commission failure delivery transaction'ini rollback etmiyor; sadece error log var. | `AssignmentServiceImpl` commission catch. | Outbox/retry table veya reconciliation alert ekle. |
| LOG-004 | Approval/reassign/fail gibi kritik actionlarda audit coverage tutarsiz. | Dispatch assign/reassign audit var; courier assignment actions audit yok. | AuditService tum kritik state transitionlara yayilmali. |

## Swagger / OpenAPI Eksikleri

| ID | Bulgu | Kanit | Oneri |
| --- | --- | --- | --- |
| API-001 | `springdoc-openapi` dependency yok. | `pom.xml` dependency listesi. | `springdoc-openapi-starter-webmvc-ui` ekle. |
| API-002 | `@Tag`, `@Operation`, schema annotation yok. | Repo taramasinda sonuc yok. | Moduler OpenAPI annotation ve example response ekle. |
| API-003 | Security scheme dokumantasyonu yok. | OpenAPI config yok. | Bearer JWT security scheme tanimla. |
| API-004 | Legacy ve alias endpointlerin deprecation durumu dokumante degil. | Controllerlar. | OpenAPI `deprecated=true` ve migration guide. |

## Unit Test Coverage Dusuk Alanlar

| Alan | Mevcut Durum | Eksik |
| --- | --- | --- |
| Commission | Test dosyasi yok. | `CommissionServiceImpl`, `CommissionController`, duplicate commission, markAsPaid, summary aggregation. |
| Reports | Test dosyasi yok. | Date range, branch/restaurant filter, raw projection mapping. |
| Repositories/specifications | Unit/integration coverage sinirli. | Actual SQL/fetch plan, count query, filter combinations. |
| Security RBAC | Auth security test var, modul bazli az. | Restaurant owner/staff/courier/admin endpoint matrix. |
| WebSocket | Test yok. | STOMP CONNECT auth, destinations, payloads. |
| Exception handler | Test yok veya sinirli. | Validation/type mismatch/data integrity/global wrapper. |
| Flyway | Runtime testle dolayli calisiyor. | Migration validation + index existence tests. |

## Integration Test Eksikleri

| ID | Eksik Akis | Oneri |
| --- | --- | --- |
| INT-001 | Full auth -> owner restaurant/branch -> order create/submit -> manager assign -> courier accept/pickup/deliver. | Testcontainers PostgreSQL + MockMvc veya RestAssured. |
| INT-002 | RBAC negative tests. | Her rol icin izinli/izin disi endpoint matrisi. |
| INT-003 | Pagination JSON contract tests. | Tum paged endpointlerde `pageable/sort/empty` sizmadigini assert et. |
| INT-004 | Concurrency duplicate assignment test. | Paralel assign requestleri ile tek aktif assignment assert. |
| INT-005 | Flyway index/performance smoke. | `pg_indexes` assertion veya migration test. |
| INT-006 | WebSocket integration. | STOMP client ile CONNECT JWT ve destination payload testleri. |

## DTO Yerine Entity Donen Endpointler

Controller taramasinda entity class'larini dogrudan response olarak donen endpoint bulunmadi. Mevcut endpointler DTO, response record veya wrapper kullaniyor. Bu alan iyi durumda; korunmali.

## Gereksiz Eager Fetch

Global entity seviyesinde `FetchType.EAGER` bulunmadi. Bu iyi. Fetch joinler genelde DTO mapping icin gerekli. Dikkat edilmesi gerekenler:

- Page querylerde fetch join count query uyumlulugu kontrol edilmeli.
- Specification icinde fetch kullanildiginda count query guard'i tum primitive/wrapper result tiplerini kapsamali.
- DTO projection ile fetch join ihtiyaci azaltabilir.

## Oncelikli Aksiyon Plani

1. Security hardening: self-register role kisiti, CORS allowlist, staff isolation.
2. Exception handler genisletme: type mismatch, constraint violation, data integrity, malformed JSON.
3. Commission pagination ve PageResponse standardi.
4. N+1 fixleri: ready orders, courier active assignments, commission creation fetch.
5. Composite/search index migrationlari.
6. OpenAPI/springdoc ekleme ve endpoint deprecation dokumani.
7. Integration test altyapisi: Testcontainers PostgreSQL + RBAC matrix.
8. Report/Commission unit testleri.
9. Mapper stratejisini tekilleştirme.
10. Logging/audit standardizasyonu ve PII azaltma.
