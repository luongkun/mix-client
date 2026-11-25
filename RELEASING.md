# Phát hành Mix Client trên Windows

Yêu cầu: JDK 8 (`java` và `javac` trên PATH), Node.js 20+, dependencies trong
`launcher-desktop/node_modules`, Minecraft 1.8.9 và MCP mappings đã được cài theo
`HANDOFF.md`. Cần JAR OptiFine trong `optifine/`; thư mục này không lưu trong Git.
Đóng gói cần thêm JRE ở `launcher-desktop/jre/bin/java.exe`.

Chạy từ `launcher-desktop`:

```powershell
# Build và kiểm tra tài nguyên, chưa đóng gói
npm run release:prepare -- --offline

# Build và tạo thư mục ứng dụng Windows
npm run pack -- --offline

# Build và tạo bộ cài NSIS
npm run dist:win -- --offline
```

Bỏ `--offline` nếu cần tải dependencies Gradle. `npm run build` chỉ build giao
diện để phát triển; lệnh phát hành là `pack` hoặc `dist:win`.

Pipeline xóa `build/dist` cũ, chạy Gradle `dist`, kiểm tra phiên bản client,
các JAR chính và OptiFine, rồi build renderer trước khi chạy electron-builder.
Nếu một bước lỗi, pipeline dừng và không chạy bước đóng gói tiếp theo.
Không chạy pipeline đồng thời với một lần build hay khởi chạy game khác.

`build/release-receipt.json` ghi phiên bản client/launcher và SHA-256 của các
tệp game, renderer vừa chuẩn bị. Mỗi lần đóng gói dùng thư mục riêng
`launcher-desktop/release/<launcher-version>-<uuid>/` và in đường dẫn khi xong,
tránh ghi đè `app.asar` của bản launcher đang mở.
Phiên bản client lấy từ `build.gradle`; phiên bản launcher lấy từ
`launcher-desktop/package.json`, hai phiên bản không cần trùng nhau.

Đây là kiểm tra build và tài nguyên. Trước khi phát hành, vẫn cần smoke test
plain, Forge, OptiFine và kiểm tra đăng nhập/khởi chạy từ ứng dụng đã đóng gói.
Các lệnh trên không tự upload hoặc công bố bản phát hành.

## Kiểm tra runtime

Sau khi chuẩn bị bản build, chạy từ `launcher-desktop`:

```powershell
npm run smoke:matrix
```

Lệnh chạy plain → Forge → OptiFine lần lượt, mỗi chế độ tối đa 180 giây.
Khi quá hạn, chỉ cây tiến trình của lượt test đó bị dừng. Forge phải được cài
trước bằng launcher. Test dùng world và dữ liệu ở `%APPDATA%/.mixclient/smoke`,
không dùng thư mục chơi chính. Không chạy đồng thời nhiều smoke test.

Kết quả ở `build/smoke/<timestamp>/results.json`; từng thư mục chế độ chứa
log của lượt chạy và ảnh mới được tạo. Một lượt chỉ đạt khi exit code bằng 0,
có `MIXCLIENT_SMOKE_OK`, vào world thành công, có các ảnh Home/Modules/Settings/
Music/HUD và không ghi nhận `Mixin apply failed` hay `MIXCLIENT_SMOKE_FAIL`.
`build.json` lưu phiên bản và SHA-256 các JAR được kiểm tra. Cờ
`dangoUnavailable` ghi nhận riêng tình trạng Dango bridge; `diagnostics` ghi
cảnh báo sealing LWJGL, lỗi quét JAR của Forge cũ và lỗi Display khi tắt world.
Các chẩn đoán này không tự làm trượt chuỗi menu/HUD, nên phải đọc cùng kết quả
`passed`, không dùng riêng cờ đó để kết luận runtime không có lỗi.

Smoke xác minh khởi động và chuỗi menu/HUD tự động. Nó chưa xác minh đăng nhập
Microsoft, kết nối máy chủ, kéo thả HUD, hoặc lưu rồi khôi phục profile qua một
lần khởi động mới; cần kiểm tra riêng trước khi gọi bản phát hành là ổn định.
