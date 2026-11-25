# Runtime smoke — 25/11/2026

Client 1.8.2, Java 8u512, Windows x64. Build bằng `gradlew.bat dist --offline`,
chạy `npm run smoke:matrix` trong `launcher-desktop`.

Lượt cuối: `build/smoke/2026-11-25T07-24-42-262Z/`. `build.json` lưu hash JAR;
`results.json` và thư mục từng chế độ lưu chẩn đoán, log và ảnh. Đây là đầu ra
cục bộ bị bỏ qua bởi Git; chạy lại lệnh trên để tạo bằng chứng mới.

| Chế độ | Chuỗi smoke | Thời gian | Ảnh | Lỗi áp Mixin / crash native |
|---|---|---:|---:|---|
| Plain | Đạt | 151,5 giây | 11 | Không ghi nhận |
| Forge | Đạt | 167,7 giây | 11 | Không ghi nhận |
| OptiFine | Đạt | 147,2 giây | 11 | Không ghi nhận |

Cả ba lượt exit 0, có marker thành công và world ready. Đã xem trực tiếp ảnh
Home của plain và HUD trong world của Forge trên bản cuối.

## Các sửa đổi được xác minh

- Hook `renderSky` từng nhắm cả overload `(float, int)` và
  `(WorldRenderer, float, boolean)`, gây `InvalidInjectionException`.
  Chỉ định `renderSky(FI)V` loại bỏ lỗi áp Mixin ở cả ba chế độ.
- Một lượt Forge crash tại `lwjgl_stb.dll`, stack qua
  `ScreenshotManager → AssetManager.loadImage → STBImage.stbi_load_from_memory`.
  Ảnh bitmap hiện giải mã bằng Java ImageIO; buffer RGBA upload NanoVG dùng
  cấp phát/giải phóng tường minh trong `finally`. Lượt cuối không còn đi qua
  đường giải mã STB này và không ghi nhận crash native. Chưa xác định đầy đủ
  nguyên nhân bên trong bộ giải mã native cũ.
- Công cụ smoke lưu riêng ảnh/log mới, kiểm tra world, marker, ảnh bắt buộc,
  lỗi Mixin và crash report; giới hạn 180 giây cho mỗi chế độ.

## Vấn đề còn lại và giới hạn

- Cả ba chế độ vẫn ghi cảnh báo package sealing LWJGL.
- Forge cũ báo lỗi đọc một số entry `META-INF/versions` của thư viện mới.
- OptiFine ghi Dango bridge `available=false`; chuỗi menu/HUD vẫn hoàn tất.
  Kết quả này không xác nhận chức năng Dango hoạt động trong OptiFine.
- Một lượt Forge trước đó có `Display not created` lúc server tắt; lượt cuối
  không tái hiện, chưa coi lỗi đó đã được sửa.
- Thời gian plain tăng từ khoảng 98 lên 152 giây giữa các lượt trước/sau
  thay đổi giải mã ảnh. Cần đo riêng chi phí preload screenshot trước khi
  kết luận về hiệu năng; chưa có benchmark có kiểm soát.
- Chưa kiểm tra lại fullscreen/Fast Render, đăng nhập Microsoft, máy chủ,
  kéo thả HUD hoặc khôi phục profile qua lần khởi động mới.
- Bộ cài tạo ở bước phát hành trước chưa chứa các sửa đổi Java của lượt này;
  cần đóng gói lại qua `npm run dist:win` trước khi phân phối.
