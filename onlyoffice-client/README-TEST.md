# OnlyOffice Test Client (Angular 20)

Client tối giản để test chức năng OnlyOffice của backend Spring: liệt kê,
upload và mở tài liệu trong editor OnlyOffice.

## Cấu hình

Sửa `src/app/api.config.ts`, trỏ tới backend đang chạy trên server:

```ts
export const API_BASE = 'http://<SERVER_IP>:8081';
```

## Chạy

```bash
npm install        # nếu chưa cài
npx ng serve       # http://localhost:4200
```

## Luồng test

1. Mở http://localhost:4200 → thấy danh sách tài liệu (gọi `GET /api/documents`).
2. **Upload** một file `.docx/.xlsx/.pptx` → `POST /api/documents`.
3. Bấm **Edit** → client gọi `GET /api/documents/{name}/config`, nhận
   `documentServerApiUrl` + `config` (đã ký JWT), nạp `api.js` từ Document
   Server và khởi tạo `DocsAPI.DocEditor`.
4. Sửa nội dung, đóng/lưu → Document Server gọi callback về backend, file được
   ghi lại vào MinIO.

## Yêu cầu quan trọng

- Backend phải đã deploy **bản có REST API** (`DocumentApiController`, CORS) —
  các endpoint `/api/documents*` là mới, phải merge vào `develop` để CI deploy.
- Trình duyệt phải truy cập được cả hai:
  - Backend: `http://<SERVER_IP>:8081`
  - Document Server (api.js): `http://<SERVER_IP>:8880` (đúng port đã publish).
- `ONLYOFFICE_DOCSERVER_URL` trên server phải là `http://<SERVER_IP>:8880` để
  `documentServerApiUrl` trả về đúng địa chỉ trình duyệt gọi được.
