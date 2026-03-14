# File Transfer Software (Java)

## 1. Giới thiệu
Phần mềm **File Transfer** được xây dựng bằng ngôn ngữ **Java** nhằm cho phép người dùng gửi và nhận file thông qua mạng.  
Hệ thống hoạt động theo mô hình **Client – Server** và sử dụng **Socket Programming** để truyền dữ liệu.

Đây là đồ án môn **Điện Toán Di Động**.

## 2. Công nghệ sử dụng

- Ngôn ngữ: Java
- Giao diện: Java Swing + FlatLaf
- Cơ sở dữ liệu: SQL Server
- Kết nối mạng: Java Socket
- IDE: NetBeans
- Quản lý mã nguồn: GitHub


## 3. Chức năng chính

### Server (Admin)
- Quản lý người dùng (CRUD)
- Tìm kiếm người dùng
- Quản lý file đã gửi
- Xóa file
- Xem log hoạt động
- Start / Stop server

### Client (User)
- Đăng nhập hệ thống
- Chọn file từ máy
- Gửi file lên server
- Nhận file từ server
- Xem lịch sử gửi file


## 4. Kiến trúc hệ thống

Hệ thống gồm 2 phần:

### Server
Quản lý:
- kết nối client
- cơ sở dữ liệu
- lưu file
- ghi log

### Client
Thực hiện:
- đăng nhập
- gửi file
- nhận file

Mô hình hoạt động:


Client → Server → Database



## 5. Cơ sở dữ liệu

Database sử dụng **SQL Server**

### Bảng users

| Field | Type |
|------|------|
| user_id | int |
| username | varchar |
| password_hash | varchar |
| full_name | varchar |
| status | varchar |
| created_at | datetime |

### Bảng files

| Field | Type |
|------|------|
| file_id | int |
| file_name | varchar |
| file_path | varchar |
| sender | varchar |
| created_at | datetime |


## 6. Cách chạy chương trình

### Bước 1
Clone project từ GitHub


git clone https://github.com/kaitoru888/file-transfer-java


### Bước 2
Mở project bằng **NetBeans**

### Bước 3
Tạo database SQL Server và import file SQL

### Bước 4
Chỉnh thông tin database trong file:


AppConfig.java


### Bước 5
Chạy Server


FileTransferServer


### Bước 6
Chạy Client


FileTransferClient



## 7. Tài khoản test

| Username | Password |
|--------|--------|
| admin | 123456 |
| user1 | 123456 |
| user2 | 123456 |


## 8. Tác giả

Sinh viên: Hoàng Thế Anh  
Môn học: Điện Toán Di Động  
Năm: 2026
