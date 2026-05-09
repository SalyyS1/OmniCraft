# Wiki sử dụng OmniCraft

[English](../en/overview.md)

## OmniCraft là gì

OmniCraft là plugin trạm chế tạo cho server Paper RPG. Member mở menu, chọn category, xem nguyên liệu cần có, rồi craft bằng chuột trái, chuột phải hoặc shift click.

## Hướng dẫn cho member

Dùng một trong các lệnh:

| Lệnh | Công dụng |
| --- | --- |
| `/oc` | Mở main menu |
| `/ocraft` | Mở main menu |
| `/craft` | Mở main menu |
| `/oc open <category>` | Mở thẳng một category |
| `/oc search <category> <text>` | Tìm recipe theo output, ingredient, MMOItems type hoặc id |

Vật phẩm nguyên liệu giữ nguyên tên và lore gốc. OmniCraft chỉ thêm một dòng requirement. Đỏ là thiếu. Xanh là đủ.

## Lệnh admin

| Lệnh | Công dụng |
| --- | --- |
| `/oc settings` | Mở GUI settings |
| `/oc browse` | Browse và edit recipe |
| `/oc reload` | Reload file |
| `/oc validate` | Kiểm tra lỗi recipe |
| `/oc debug recipe <id>` | Test recipe theo player hiện tại |
| `/oc export <category>` | Export recipe category |
| `/oc import <category> <file.zip>` | Import recipe category |

## Permission

| Permission | Công dụng |
| --- | --- |
| `omnicraft.use` | Mở menu |
| `omnicraft.category.<id>` | Vào category |
| `omnicraft.open.<id>` | Mở thẳng category |
| `omnicraft.admin` | Browse admin, import, export |
| `omnicraft.settings` | Menu settings |
| `omnicraft.reload` | Reload file |
| `omnicraft.debug` | Debug recipe |
| `omnicraft.validate` | Validate config |

## Cấu trúc file

```text
plugins/OmniCraft/
  config.yml
  messages.yml
  category/
    weapons/
      steel_sword.yml
  data/
    usage.yml
  logs/
    craft-history.log
  exports/
```

## Các file config

`config.yml` chỉnh menu, số lượng craft theo click, countdown, chống dupe, AdvancedEnchantments, log history, editor và tính năng tiện ích.

`messages.yml` chỉnh toàn bộ GUI line, title, warning, error, success message và broadcast.

Mỗi file recipe là một thành phẩm.

## Editor admin

Dùng `/oc browse`, chọn category, rồi click recipe để edit. Editor dùng cùng lưới ingredient 5x5 như GUI craft của member.

- Recipe hiển thị liên tục. Ô trống kế tiếp màu xanh dùng để tạo recipe mới. Click để browse output, hoặc cầm item trên cursor rồi click/drag vào ô xanh.
- Recipe mới được lưu ở trạng thái disabled để tránh craft nhầm khi chưa cấu hình xong.
- Cầm item trên cursor rồi click ô ingredient để serialize item đó làm nguyên liệu.
- Cầm item trên cursor rồi click ô output để serialize item đó làm thành phẩm.
- Ingredient dùng lưới 4x4. Click ô ingredient xanh còn trống để browse item Vanilla hoặc MMOItems.
- Click output preview để đổi output bằng item browser.
- Chuột trái vào ingredient để tăng số lượng yêu cầu.
- Chuột phải vào ingredient để giảm số lượng yêu cầu.
- Shift + chuột trái vào ingredient để tăng 16.
- Shift + chuột phải vào ingredient để xóa ingredient.
- Toggle enabled, craft time và AdvancedEnchantments extraction ở hàng dưới.
- Bật Delete Mode trong browse/category, rồi click recipe để xóa file YAML của recipe đó.

## Ví dụ recipe

```yaml
id: steel_blade
display:
  name: "#7cf5ffSteel Blade"
output:
  mode: MMOITEMS
  material: DIAMOND_SWORD
  amount: 1
  type: SWORD
  id: STEEL_BLADE
ingredients:
  base:
    amount: 1
    item:
      mode: VANILLA
      material: IRON_SWORD
  essence:
    amount: 100
    item:
      mode: VANILLA
      material: AMETHYST_SHARD
requirements:
  permission: omnicraft.recipe.steel_blade
  level: 25
  money: 10000
craft-time:
  enabled: true
  seconds: 5
extraction:
  enchant: EXTRACT
  gemstone: EXTRACT
  level: DESTROY
limits:
  daily: 5
  weekly: 20
options:
  enabled: true
  hidden: false
```

## AdvancedEnchantments

OmniCraft có thể apply custom enchant vào output bằng AdvancedEnchantments. Nếu server không cài AdvancedEnchantments, OmniCraft có thể bỏ qua custom enchant hoặc disable recipe đó theo `advanced-enchantments.missing-hook-disables-ae-recipes`.

`extraction.enchant` nghĩa là custom enchant của AdvancedEnchantments trên nguyên liệu nền bị consume.

- `EXTRACT`: tách AE enchant thành sách AE bằng `/ae givebook`.
- `DESTROY`: AE enchant bị mất.
- `KEEP`: chuyển AE enchant từ nguyên liệu nền sang output được craft.

## Chống dupe

OmniCraft không tin click từ client. GUI click bị cancel, click type rủi ro bị bỏ qua, inventory được scan server-side, mỗi player/recipe có lock riêng, nguyên liệu bị trừ trước khi phát output, và rollback nếu không phát được output.

## Xử lý lỗi

Chạy `/oc validate` sau khi sửa recipe. Chạy `/oc reload` sau khi sửa xong. Xem `logs/craft-history.log` để biết craft thành công, fail hoặc rollback vì lý do gì.
