# RikkeiBank API - Phân tán Microservices (Fintech Banking System)

Dự án Backend Ngân hàng số **RikkeiBank API** được xây dựng theo kiến trúc Microservices phân tán (MSA) trên nền tảng **Java 17, Spring Boot 3.3.4, Spring Cloud 2023.0.3, Apache Kafka, Redis, Resilience4j, Database-per-service (H2/JPA)**.

---

## 1. Cấu trúc Danh mục Microservices & Ports

| STT | Tên Microservice | Cổng (Port) | Vai trò & Trách nhiệm chính |
| :---: | :--- | :---: | :--- |
| 1 | **`discovery-server`** | `8761` | Eureka Service Registry (Đăng ký và định vị dịch vụ theo tên) |
| 2 | **`config-server`** | `8888` | Spring Cloud Config Server tập trung cấu hình |
| 3 | **`api-gateway`** | `8080` | Điểm vào duy nhất (API Gateway), định tuyến, xác thực JWT, kiểm tra Blacklist Redis |
| 4 | **`identity-service`** | `8081` | Quản lý người dùng, đăng ký, đăng nhập, cấp JWT (Access/Refresh), Admin ép đăng xuất |
| 5 | **`customer-service`** | `8082` | Quản lý hồ sơ Khách hàng (Customer) & Nhân viên (Staff/Teller) |
| 6 | **`account-service`** | `8083` | Quản lý Loại tài khoản (AccountType), Tài khoản (Account), Số dư, Redis Cache-Aside |
| 7 | **`transaction-service`** | `8084` | Xử lý chuyển khoản phân tán (Saga Orchestrator), Resilience4j Circuit Breaker, Bắn Kafka Event |
| 8 | **`notification-service`**| `8085` | Nhận sự kiện biến động số dư từ Kafka (Consumer WebFlux Reactive), in log cảnh báo |

---

## 2. Hướng dẫn Khởi chạy Hệ thống

### Bước 1: Khởi động Hạ tầng Docker (Redis & Kafka)
Tại thư mục gốc dự án:
```bash
docker compose up -d
```
* **Redis**: chạy tại `localhost:6379` (Phục vụ Cache-Aside và Blacklist Token thu hồi quyền)
* **Kafka Broker**: chạy tại `localhost:9092` (Phục vụ truyền tin sự kiện biến động số dư)

### Bước 2: Build toàn bộ mã nguồn & Chạy Unit Tests
```bash
.\gradlew test
```
* Báo cáo độ bao phủ mã kiểm thử (JaCoCo) được tự động tạo tại thư mục `build/reports/jacoco/test/html/index.html` của từng module.

### Bước 3: Thứ tự Khởi chạy các Service (Khuyên dùng)
1. `discovery-server` (Cổng 8761 - http://localhost:8761)
2. `config-server` (Cổng 8888)
3. `api-gateway` (Cổng 8080)
4. `identity-service` (Cổng 8081)
5. `customer-service` (Cổng 8082)
6. `account-service` (Cổng 8083)
7. `transaction-service` (Cổng 8084)
8. `notification-service` (Cổng 8085)

---

## 3. Tài khoản & Dữ liệu Mẫu (Seed Data)

| Username | Password | Vai trò (Role) | Mã liên kết | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| `admin` | `admin123` | `ADMIN` | `ADMIN001` | Toàn quyền quản trị, ép buộc đăng xuất tài khoản |
| `teller1` | `teller123` | `TELLER` | `STAFF001` | Giao dịch viên, xem giao dịch trong ngày của mình |
| `teller2` | `teller123` | `TELLER` | `STAFF002` | Giao dịch viên chi nhánh khác |
| `customer1` | `cust123` | `CUSTOMER` | `CIF001` | Sở hữu tài khoản `1001234567` (Số dư ban đầu: 50,000,000 VND) |
| `customer2` | `cust123` | `CUSTOMER` | `CIF002` | Sở hữu tài khoản `1009876543` (Số dư ban đầu: 10,000,000 VND) |

---

## 4. Minh chứng Nghiệp vụ & Kịch bản Kiểm thử

### 4.1. Kịch bản Saga Chuyển khoản Thành công (Happy Path)
* **Request:** `POST http://localhost:8080/api/v1/transfers`
  ```json
  {
    "fromAccount": "1001234567",
    "toAccount": "1009876543",
    "amount": 500000,
    "description": "Chuyen tien an trua"
  }
  ```
* **Luồng xử lý:**
  1. Transaction Service tạo bản ghi `PENDING`.
  2. Bước 1: Trừ 500,000 VND từ tk `1001234567` (Số dư mới: 49,500,000 VND).
  3. Bước 2: Cộng 500,000 VND vào tk `1009876543` (Số dư mới: 10,500,000 VND).
  4. Cập nhật trạng thái `SUCCESS`.
  5. Bắn `TransferCompletedEvent` lên Kafka topic `transfer-events`.
  6. `notification-service` tiêu thụ sự kiện và ghi nhận log biến động số dư.

### 4.2. Kịch bản Saga Bù trừ Rollback (Compensating Flow)
* **Request:** `POST http://localhost:8080/api/v1/transfers` kèm header `X-Simulate-Error: true` hoặc body `simulateError: true`.
* **Luồng bù trừ:**
  1. Trừ tiền tk nguồn `1001234567` thành công.
  2. Bước Credit tk đích gặp lỗi (do lỗi giả lập hoặc tk đích không tồn tại/bị khóa).
  3. Saga Orchestrator lập tức gọi `account-service` API `/api/v1/accounts/compensate-debit` để cộng hoàn trả số tiền về lại tk nguồn.
  4. Trạng thái Transaction chuyển thành `COMPENSATED`.
  5. Số dư hai bên không hề bị hao hụt, bảo toàn tính nhất quán tuyệt đối.

### 4.3. Kịch bản Bảo mật & Ép Buộc Đăng Xuất (Force Logout)
* Quản trị viên gọi: `POST http://localhost:8080/api/v1/admin/users/customer2/force-logout`.
* `identity-service` lưu cờ thu hồi quyền vào Redis (`blacklist:user:customer2`).
* Khi `customer2` sử dụng token cũ gọi bất kỳ API tài chính nào qua API Gateway $\rightarrow$ Gateway trả về ngay lập tức HTTP `401 Unauthorized`.

### 4.4. Kịch bản Kháng lỗi Hệ thống (Resilience4j Circuit Breaker)
* Khi `account-service` bị ngắt kết nối (Down), nếu có request chuyển khoản gửi tới `transaction-service`, Circuit Breaker chuyển trạng thái sang `OPEN` và kích hoạt `fallbackMethod`:
  * Trả về HTTP JSON thân thiện: `"Dịch vụ tài khoản tạm thời không khả dụng. Circuit Breaker đã chặn giao dịch an toàn, không có tài khoản nào bị trừ tiền."`
  * Chống hoàn toàn hiện tượng lỗi dây chuyền (Cascading Failure).

### 4.5. Kịch bản Redis Cache-Aside
* Gọi API tra cứu thông tin tài khoản `GET http://localhost:8080/api/v1/accounts/1001234567`:
  * Lần 1: Log xuất hiện `[REDIS CACHE MISS]` -> đọc từ DB và lưu vào Redis.
  * Lần 2+: Phản hồi ngay từ Redis cache trong thời gian < 2ms.
  * Khi có giao dịch biến động số dư: `@CacheEvict` tự động làm mới bộ nhớ đệm, chống dữ liệu cũ (dirty read).

---

## 5. File Postman Collection
Import file [RikkeiBank_API_Postman_Collection.json](file:///d:/IT215/SS18/Miniprj/RikkeiBank_API_Postman_Collection.json) vào Postman để kiểm thử đầy đủ 7 thư mục API và các flow nghiệp vụ.
