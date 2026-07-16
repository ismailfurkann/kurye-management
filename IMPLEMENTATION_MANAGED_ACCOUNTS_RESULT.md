# Managed Accounts Security Phase Result

## 1. Public register kapatma

`POST /api/v1/auth/register` artik public hesap olusturmaz.

Davranis:

```json
{
  "success": false,
  "message": "Public registration is disabled",
  "errorCode": "PUBLIC_REGISTRATION_DISABLED"
}
```

Security config tarafinda register icin `permitAll` kaldirildi ve endpoint `denyAll` olarak tutuldu. Spring Security'nin govdesiz `sendError` davranisini asmak icin `JwtAuthenticationFilter` register isteklerini security authorization asamasindan once 403 JSON wrapper ile kisa devreye alir. `AuthService.register` da service seviyesinde ayni business hatayi firlatir.

Login public kaldi. Logout ve change-password authenticated kaldi.

## 2. Degistirilen ve yeni dosyalar

Degistirilen ana dosyalar:

- `src/main/java/com/ivedi/ivedikuryemanagement/auth/impl/User.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/impl/AuthServiceImpl.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/impl/UserRepository.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/web/AuthController.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/web/AuthResponse.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/common/security/JwtAuthenticationFilter.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/common/security/JwtUtil.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/common/security/UserPrincipal.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/common/config/SecurityConfig.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/common/exception/BusinessException.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/restaurant/impl/RestaurantRepository.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/courier/impl/CourierRepository.java`

Yeni dosyalar:

- `src/main/java/com/ivedi/ivedikuryemanagement/auth/api/UserManagementResponse.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/api/UserManagementService.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/impl/UserManagementServiceImpl.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/impl/UserBranchAssignment.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/impl/UserBranchAssignmentId.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/impl/UserBranchAssignmentRepository.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/web/UserManagementController.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/web/ManagedUserRequest.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/web/ManagedCourierRequest.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/web/UserStatusRequest.java`
- `src/main/java/com/ivedi/ivedikuryemanagement/auth/web/ResetPasswordRequest.java`
- `src/main/resources/db/migration/V12__add_managed_user_accounts.sql`
- `src/test/java/com/ivedi/ivedikuryemanagement/auth/impl/UserManagementServiceImplTest.java`
- `src/test/java/com/ivedi/ivedikuryemanagement/auth/web/UserManagementControllerTest.java`

## 3. Migration

Migration: `V12__add_managed_user_accounts.sql`

Eklenenler:

- `users.must_change_password BOOLEAN NOT NULL DEFAULT false`
- `users.token_version INTEGER NOT NULL DEFAULT 0`
- `user_branch_assignments(user_id, branch_id, created_at)`
- `PRIMARY KEY (user_id, branch_id)`
- FK: `user_id -> users(id)`
- FK: `branch_id -> branches(id)`
- Index: `idx_uba_branch_id`

Mevcut kullanicilar `must_change_password=false` ve `token_version=0` ile korunur.

## 4. Kullanici olusturma endpointleri

Yeni endpointler:

```http
GET /api/v1/users?page=&size=&role=&status=&search=&restaurantId=
GET /api/v1/users/{id}
POST /api/v1/users
PUT /api/v1/users/{id}
PATCH /api/v1/users/{id}/status
POST /api/v1/users/{id}/reset-password
```

Liste response:

```java
ApiResponse<PageResponse<UserManagementResponse>>
```

Response sifre, hash, temporary password veya token detayi dondurmez.

## 5. Role gore olusturma kurallari

Yetki matrisi:

| Islem | PLATFORM_ADMIN | PLATFORM_MANAGER | RESTAURANT_OWNER |
| --- | --- | --- | --- |
| PLATFORM_MANAGER olusturma | Evet | Hayir | Hayir |
| RESTAURANT_OWNER olusturma | Evet | Hayir | Hayir |
| COURIER olusturma | Evet | Evet | Hayir |
| RESTAURANT_STAFF olusturma | Evet | Hayir | Yalniz kendi restaurant branchleri |
| Tum kullanicilari listeleme | Evet | Hayir | Hayir |
| Courier kullanicilarini listeleme | Evet | Evet | Hayir |
| Kendi staff listesini gorme | Evet | Hayir | Evet |
| Kullanici pasif yapma | Evet | Yalniz courier | Yalniz kendi staffi |
| Sifre resetleme | Evet | Yalniz courier | Yalniz kendi staffi |

Controller `@PreAuthorize` ile ilk bariyeri koyar, service katmani role ve resource scope kontrolunu tekrar yapar.

## 6. Courier account creation

Courier kullanicisi admin/manager tarafindan olusturulurken user ve courier profile ayni transaction icinde olusturulur.

Request icindeki courier bolumu:

```json
{
  "vehicleType": "MOTORBIKE",
  "plateNumber": "06 ABC 123",
  "regionId": "uuid"
}
```

Baslangic karari:

- `approvalStatus=APPROVED`
- `availabilityStatus=OFFLINE`

Kapali B2B modelde kullaniciyi platform yoneticisi olusturdugu icin courier onayi varsayilan olarak tamamlanmis kabul edildi. Mevcut approval endpointleri korunur.

## 7. Restaurant owner assignment

`RESTAURANT_OWNER` olustururken `restaurantId` verilirse mevcut restaurant kaydi user'a owner olarak baglanir.

Kurallar:

- Restaurant yoksa 404.
- Restaurant baska owner'a sahipse conflict/business error.
- Otomatik owner replace yapilmaz.

## 8. Staff branch mapping

Yeni `user_branch_assignments` tablosu staff scope icin eklendi.

Kurallar:

- `RESTAURANT_STAFF` en az bir branch'e baglanmak zorunda.
- Staff branchleri ayni restaurant'a ait olmali.
- Restaurant owner yalniz kendi restaurant branchlerine staff atayabilir.
- Duplicate mapping primary key ile engellenir.
- `GET /api/v1/auth/me` artik staff icin `branchIds` degerini bu mapping tablosundan dondurur.

Bu fazda staff order/POS yetkileri yeniden acilmadi; Phase 5A'daki deny politikasi korunur. Mapping sonraki fazlarda kontrollu staff scope icin kullanilabilir.

## 9. Temporary password politikasi

Secilen model: Admin temporary password gonderir.

Kurallar:

- `temporaryPassword` zorunlu.
- 8-72 karakter validation uygulanir.
- Email ile ayni sifre reddedilir.
- Response icinde sifre dondurulmez.
- DB'de sadece BCrypt hash saklanir.
- Request loglama veya response ile duz metin sifre cikisi eklenmedi.

## 10. mustChangePassword davranisi

Admin tarafindan olusturulan kullanici:

```text
mustChangePassword = true
```

Login response artik `mustChangePassword` alanini icerir.

`mustChangePassword=true` iken kullanici yalniz su endpointleri kullanabilir:

- `GET /api/v1/auth/me`
- `PATCH /api/v1/auth/change-password`
- `POST /api/v1/auth/logout`

Diger endpointler:

```json
{
  "success": false,
  "message": "Password change required",
  "errorCode": "PASSWORD_CHANGE_REQUIRED"
}
```

Basarili password change sonrasi:

- `mustChangePassword=false`
- `tokenVersion` artar.

## 11. tokenVersion

Token invalidation uygulandi.

Davranis:

- `users.token_version` eklendi.
- Login sirasinda JWT icine `tokenVersion` claim'i yazilir.
- `JwtUtil.isTokenValid` token claim'i ile current user tokenVersion degerini karsilastirir.
- Password change, admin password reset ve status degisikligi tokenVersion degerini artirir.
- Eski JWT'ler sonraki requestlerde gecersiz olur.

## 12. Password reset

Endpoint:

```http
POST /api/v1/users/{id}/reset-password
```

Body:

```json
{
  "temporaryPassword": "NewTempPass123!"
}
```

Davranis:

- BCrypt hash guncellenir.
- `mustChangePassword=true` yapilir.
- `tokenVersion` artirilir.
- Response sifre icermez.
- Audit action: `USER_PASSWORD_RESET`

## 13. User status

Endpoint:

```http
PATCH /api/v1/users/{id}/status
```

Body:

```json
{
  "status": "ACTIVE"
}
```

Kurallar:

- Kendi hesabini pasif yapma engellendi.
- Son aktif `PLATFORM_ADMIN` pasif yapilamaz.
- Status degisikligi tokenVersion artirir.
- JWT filter her requestte enabled/locked kontrolu yapar; pasif/bloke kullanicinin is endpointlerine erisimi kesilir.
- Audit action: `USER_STATUS_CHANGED`

## 14. Audit olaylari

Eklenen audit actionlari:

- `USER_CREATED`
- `USER_UPDATED`
- `USER_STATUS_CHANGED`
- `USER_PASSWORD_RESET`
- `USER_BRANCH_ASSIGNMENTS_CHANGED`
- `COURIER_PROFILE_CREATED_BY_ADMIN`
- `RESTAURANT_OWNER_ASSIGNED`

Audit metadata sifre veya token icermez.

## 15. Build ve test sonucu

Calistirilan komutlar:

```powershell
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd clean test
.\mvnw.cmd -DskipTests package
java -jar target\ivedi-kurye-management-0.0.1-SNAPSHOT.jar --server.port=8081
```

Sonuc:

- Compile: PASSED
- Tests: PASSED
- Package: PASSED
- Test sayisi: 134
- Failure: 0
- Flyway validate: 12 migration, schema version 12
- Startup: PASSED, Tomcat 8081 ve context path `/`

Gercek HTTP notu: foreground startup `Started IvediKuryeManagementApplication` satirina ulasti. Shell timeout komutu process'i sonlandirdigi icin eszamanli HTTP cagrisi yapilamadi. Public register 403 wrapper, CORS ve auth security davranislari MockMvc integration testleriyle dogrulandi.

## 16. Ertelenen maddeler

- Staff order/POS yetkilerini mapping uzerinden yeniden acma bu fazda yapilmadi.
- Production/default profile'a admin seed eklenmedi.
- E-posta/SMS ile temporary password iletimi eklenmedi.
- Full token revocation list/blacklist eklenmedi; tokenVersion yeterli stateless invalidation olarak uygulandi.
- User list response mapping ilk surumde iliski bilgilerini response basina repository ile toplar; buyuk admin listelerinde batch optimizasyon sonraki performans fazina alinabilir.

## 17. Frontend etkisi

Frontend tarafinda yapilmasi gerekenler:

- Register ekrani kaldirilmali veya erisilemez olmali.
- Login ekrani korunmali.
- Login response icindeki `mustChangePassword=true` durumunda zorunlu sifre degistirme ekranina yonlendirilmeli.
- Platform admin icin user management ekrani eklenmeli.
- Manager icin courier olusturma akisi eklenmeli.
- Restaurant owner icin staff olusturma ve branch assignment ekrani eklenmeli.
- Temporary password response'ta donmedigi icin admin formunda girilen sifre operasyonel olarak guvenli kanaldan kullaniciya iletilmeli.

## 18. Sonraki faz onerisi

Recommended next phase: POS persistence and API contract.
