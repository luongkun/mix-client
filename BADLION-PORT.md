# Port framework Badlion Client — dựa trên SOURCE GỐC

> ⛔ **TÀI LIỆU LỊCH SỬ — HƯỚNG NÀY ĐÃ BỊ BỎ (17/11/2026).**
> `src/badlion-port/`, task `compileBadlionPort` và source set `badlionPort` **không còn tồn tại**.
> Giao diện hiện tại là **MixUI-only**; engine native `mix/client/gl/` đã được xóa (xem `HANDOFF.md`).
> Giữ file này chỉ để tra cứu số đo theme và bài học reverse-engineer.

> Thay đổi hướng: trước đây giao diện được **dựng lại theo phỏng đoán** từ giá trị theme.
> Nay **source gốc đã được dịch ngược và nằm trong project** và **compile thật**.

---

## 0. Trạng thái hiện tại

| Việc | Trạng thái |
|---|---|
| **B1** — chép source gốc vào compile path | ✅ `src/main/java/net/badlion/` — **2.600 file**, package `net.badlion.a` + `net.badlion.clientcommon.*` |
| **B2** — vá ranh giới client | ✅ tự động trong `tools/port-badlion.ps1` |
| **B3** — compile | ⏳ đang chốt (xem mục 5) |
| **B4–B6** — dựng `Screen`/`MenuScreen`/`MainMenuScreen`/mod menu trên widget gốc | ⏳ chưa |
| **B7** — quyết định font (`KN` của Badlion vs `BlFont`) | ⏳ chưa |
| **B8** — gỡ frame dựng lại | ⏳ chưa |

**Script port (idempotent):** `tools/port-badlion.ps1`
Chạy lại = xoá sạch thư mục port → copy lại từ capture → vá lại → rename lại.
Không s�a tay file nào trong `net/badlion/`, mọi thay đổi nằm trong script.

---

## 1. Nguồn: capture nào dùng được

| Cây | Trạng thái | Dùng cho |
|---|---|---|
| `Badlion/captured/classes/` | 6.427 classfile gốc | nguồn chuẩn |
| `Badlion/captured/src/` | 5.615 file `.java`, **còn nguyên** | ✅ **nguồn của port** |
| `Badlion/captured/src-named/` | **HỎNG** | ❌ không dùng |

> **Phát hiện quan trọng:** script rename (`gui_rename.py`) **xoá mất dấu `/`** (phép chia) trong
> toàn bộ `src-named/`. Ví dụ `var3.Ji() / (var3 / this.width)` → `var3.Ji()  (var3  this.width)`
> → **không compile được**. Tree `captured/src/` (trước khi rename) còn nguyên.
> Script rename cần sửa lại nếu muốn dùng tên đọc được.

---

## 2. Ba lỗi hệ thống của source decompile (đã xử lý trong script)

### 2.1 Tên file chỉ khác hoa/thường — **1.364 cặp**

`do.java` vs `dO.java`, `k.java` vs `K.java`… Windows gộp file → mất class → không compile.
Windows NTFS có thuộc tính **case-sensitive per-directory** (`fsutil file setCaseSensitiveInfo <dir> enable`).

⇒ Thư mục port được bật case-sensitive ✓ **nhưng** thư mục build
(`build/sources/main/java/net/badlion`) **không** được bật (task copy của ForgeGradle 2.1 xoá dir),
nên **1.321 cặp được đổi tên trong port** (`k` → `k_c1`) để build copy không gộp nữa.

### 2.2 Tên trùng keyword của Java

`do()`, `if()`, class `do`, class `if` — javac từ chối.
⇒ Rename thêm `_` (`do()` → `do_()`, class `do` → `do_`).
Lưu ý: `do { ... } while (...)` là **câu lệnh**, không được đụng — script phân biệt bằng
lookahead `[ \t]*[({]`.

### 2.3 Inner class trùng tên outer class

`clientcommon/util/b.java`: `public abstract class b` chứa `protected static class b` → javac chỉ "class b is already defined".
⇒ Đổi inner thành `b_`, sửa mọi tham chiếu `net.badlion.clientcommon.util.b.b` → `...util.b.b_`.

### 2.4 Chuỗi literal bị rename

Rename theo word-boundary đụng cả char literal: `'k'` → `'k_c1'` → "unclosed character literal".
⇒ Script có hàm `Replace-Code` **chỉ thay trong code, không đụng string/char literal**.

---

## 3. Rào cản thật: ranh giới với client

Framework gốc **không độc lập** — nó gọi vào client của Badlion.

### 3.1 Shim `db` — `src/main/java/net/badlion/a/db.java`

Giữ **nguyên tên field** của Badlion (để widget gốc không phải sửa gì):

| Field | Kiểu | Ghi chú |
|---|---|---|
| `minecraft` | `Us` | qua `BlGameProxy` (xem 3.2) |
| `iW` | `VZ` | wrapper GL |
| `ja` | `Wg` | tessellator |
| `iY` | `Wm` | panorama |
| `jh` | `VY` | helper UI |
| `jp` | `Ly` | helper client |
| `jd` | `Vt` | resource pack |
| `jg` | `Vs` | game state |
| `badlionFontRenderer` | `util.h` | font |
| `jl` | `util.j` | sound |
| `customFontManager` | `KN` | **font engine thật của Badlion** |
| `jG` / `jM` / `jn` | `Xw` / `LD` / `util.bk` | helper |
| `iQ` / `iR` | `Gson` | json |

### 3.2 Cầu nối tới game — `mix/client/badlion/BlGameProxy.java`

`Us` là **interface 131 method** của Badlion (bọc MC). Port không có client đó ⇒ dùng
`java.lang.reflect.Proxy`:

* `UI()` → `displayWidth`, `UH()` → `displayHeight`, `kY()/kZ()` → chuột,
  `isUnicode()` → true, `getVersionName()` → "1.8.9", `UP()` → font renderer, `lT()` → player…
* method khác → giá trị mặc định của kiểu trả về; kiểu interface → proxy con.

Đây là **mối nối duy nhất** giữa GUI Badlion và Mix Client.

### 3.3 Các dòng vá tự động (B2)

| Gốc | Vá thành |
|---|---|
| `db.getInstance().fr().is1_XXAndUp()` | `false` (port chạy 1.8.9) |
| `db.getInstance().fr().equals(aBT.x)` | `false` |
| `db.getInstance().fu().hy().getScaleFactor()` | `1.0F` |
| `db.getInstance().fI().zX().cD(baseName)` | `null` |
| `db.getInstance().fI().zW()` | comment (port tự lo screen) |
| `db.getInstance().fI().c(new yG(x), true)` | comment (port chưa có modal stack) |
| `Jm.Jn()` (font của profile) | `MT.ROBOTO_REGULAR` |
| `yW.getDuration()` (tốc độ animation) | `this.UV` |
| `zu.AZ()` (đọc config) | `!this.Wi` |
| `zB.kf` (account manager) | `null` |
| `gI`/`yM` đọc profile của wrapper | `null` / `false` |

### 3.4 Thư viện ngoài

| Thư viện | Cách xử lý |
|---|---|
| `org.jetbrains.annotations` | **vendor** 4 annotation vào `src/main/java/org/jetbrains/annotations/` (`@NotNull`, `@Nullable`, `@TestOnly`, `@ApiStatus`) — không cần mạng |
| `net.hypixel.modapi` | copy nguyên tree từ capture (30 file) |
| `net.minecraft.*` | file nào import MC **không copy** (port có game riêng) |

---

## 4. Script port — `tools/port-badlion.ps1`

```
1. xoá + tạo thư mục port, bật case-sensitive
2. copy core (70 file) + api hypixel (30 file)
3. closure: kéo theo mọi class được tham chiếu (bỏ file import net.minecraft.*)
4. rename class trùng keyword (do, if) + sửa mọi usage
5. rename method trùng keyword (do(), if()) + sửa call site
6. vá ranh giới client (bảng 3.3)
7. giữ file nào không compile được ra reference/ported-badlion-excluded/
8. đổi tên các cặp chỉ khác hoa/thường (k → k_c1) để build copy không gộp
```

Chạy: `powershell -ExecutionPolicy Bypass -File tools\port-badlion.ps1`

---

## 5. Trạng thái compile (B3)

Nguồn đã được sửa đủ để javac chạy tới phần **type check**. Các lỗi còn lại là:

* class của client **không có trong capture** (`net.badlion.client.bde`…) → file đó bị giữ ra ngoài.
* API của Hypixel mà capture thiếu → giữ ngoài.
* Member của `db` mà widget gốc cần nhưng shim chưa có → bổ sung dần khi B4 viết screen.

---

## 6. Vì sao trước đây sai

Ta đọc `IR.java` để lấy **giá trị** (đúng) rồi **tự viết** engine layout (sai):
theme bảo "nút rộng 500, cao 50, bo góc 5" ✓ nhưng **ai đặt nút ở đâu, thứ tự ra sao,
bao nhiêu khoảng cách, hover animate thế nào** — đó là phần ta **phỏng đoán** ✗.

Phần đó nằm trong `zu.java` (Widget) + `IR.java` (theme) + `yc.java` (MenuScreen) + `yf.java` (MainMenuScreen)
— và nay **đã nằm trong project, compile thật** ✓.
