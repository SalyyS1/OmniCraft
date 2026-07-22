# OmniCraft — Hướng dẫn vận hành

OmniCraft là trạm chế tạo RPG/MMO cho Paper. Plugin kiểm tra nguyên liệu, tiền, giới hạn và hook ở phía server trước khi thay đổi inventory.

## Bắt đầu nhanh

1. Chọn đúng JAR: `legacy` cho Paper 1.20–1.21.11 / Java 21; `26` cho Paper 26.1+ / Java 25.
2. Chép JAR vào `plugins/`, khởi động server một lần.
3. Chỉnh `plugins/OmniCraft/config.yml` và recipe trong `category/<id>/`.
4. Dùng `/oc validate`, sau đó `/oc reload`.

## Người chơi

- `/oc` mở menu chính.
- Chuột trái craft số lượng thường; chuột phải craft theo batch; Shift craft tối đa trong giới hạn recipe.
- Recipe có thời gian chế tạo sẽ kiểm tra lại nguyên liệu và điều kiện khi hoàn tất.
- Favorites, tìm kiếm và lọc recipe có thể craft nằm trong menu category.

## Quản trị viên

- `/oc browse`: tạo/sửa recipe trong GUI.
- `/oc validate`: phát hiện recipe lỗi trước khi public.
- `/oc debug recipe <id>`: xem khả năng craft mà không tiêu nguyên liệu.
- `/oc export` và `/oc import`: chuyển category qua file zip.

## Tích hợp

- **MMOItems 6.10.1-SNAPSHOT** và **MythicLib 1.7.1-SNAPSHOT** là soft-dependency: thiếu plugin thì OmniCraft vẫn khởi động; recipe cần MMOItems sẽ fail an toàn.
- Vault dùng cho tiền, PlaceholderAPI dùng cho điều kiện và speed modifier, AdvancedEnchantments dùng cho KEEP/DESTROY/EXTRACT.
- AuraSkills chưa được bật như một integration runtime trong bản này; không cấu hình XP AuraSkills cho đến khi adapter hoàn chỉnh được phát hành.

## An toàn chống dupe

- Không một slot inventory nào được phân bổ hai lần cho nhiều ingredient.
- Output giao không đủ sẽ phục hồi inventory snapshot và tạo refund an toàn.
- Daily/weekly quota được reserve trước, rồi release chính xác khi rollback.
- Countdown/job chỉ cho phép một job active trên mỗi người chơi.
- Refund Vault lỗi được lưu bền vững để retry khi người chơi online; quá trình retry ưu tiên không trả trùng tiền.

## Recipe mẫu

```yml
id: steel_sword
display:
  name: "#7cf5ffSteel Sword"
output:
  mode: VANILLA
  material: IRON_SWORD
  amount: 1
ingredients:
  iron:
    amount: 24
    item:
      mode: VANILLA
      material: IRON_INGOT
      amount: 1
requirements:
  level: 5
  money: 250.0
craft-time:
  enabled: true
  seconds: 5
  quantity-scaling: LINEAR
  minimum-seconds: 1
  maximum-seconds: 3600
auto-craft:
  enabled: false
  priority: 0
```

AutoCraft bật mặc định nhưng có thể tắt toàn cục bằng `features.auto-craft: false`. Đánh dấu **chỉ recipe trung gian** bằng `auto-craft.enabled: true`, rồi người chơi bấm nút **AutoCraft** trong màn recipe hoặc dùng `/oc autocraft <recipe> [amount]`. Hệ thống ưu tiên nguyên liệu đang có trong inventory, chọn source theo `priority` rồi theo key, phát hiện cycle/depth limit và hủy queue khi logout hoặc reload. Mỗi node vẫn đi qua cùng transaction, quota, Vault và craft-time như craft thường. `auto-craft.max-active-runs` và `max-target-crafts` giữ tải server trong giới hạn cố định.

AuraSkills là soft-depend: có thể dùng placeholder của AuraSkills trong cấu hình Fast Craft qua PlaceholderAPI, nhưng plugin không gọi API AuraSkills trực tiếp nên thiếu AuraSkills không làm OmniCraft lỗi khởi động.

Craft có thời gian và AutoCraft đều là online-only: logout, reload hoặc sửa recipe sẽ hủy job trước khi có transaction mới. Không có offline craft hay offline claim.
