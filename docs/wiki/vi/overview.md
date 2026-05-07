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

Vật phẩm nguyên liệu giữ nguyên tên và lore gốc. OmniCraft chỉ thêm một dòng requirement. Đỏ là thiếu. Xanh là đủ.

## Lệnh admin

| Lệnh | Công dụng |
| --- | --- |
| `/oc settings` | Mở GUI settings |
| `/oc browse` | Browse recipe |
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
  enchantments:
    advanced:
      telepathy:
        level: 1
        success-rate: 100.0
        destroy-rate: 0.0
        tier: COMMON
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
  conditions:
    - "%player_world% == world"
craft-time:
  enabled: true
  seconds: 5
extraction:
  enchant: EXTRACT
  gemstone: EXTRACT
  level: DESTROY
  success-rate: 1.0
limits:
  daily: 5
  weekly: 20
options:
  enabled: true
  hidden: false
  rare-broadcast: true
  source-hints:
    essence: "Rơi từ boss dungeon."
```

## AdvancedEnchantments

OmniCraft có thể apply custom enchant vào output bằng AdvancedEnchantments. Dùng `output.enchantments.advanced`. Nếu server không cài AdvancedEnchantments, OmniCraft có thể bỏ qua custom enchant hoặc disable recipe đó theo `advanced-enchantments.missing-hook-disables-ae-recipes`.

`extraction.enchant` nghĩa là custom enchant của AdvancedEnchantments trên nguyên liệu nền bị consume.

- `EXTRACT`: tách AE enchant thành sách AE bằng `/ae givebook`.
- `DESTROY`: AE enchant bị mất.
- `KEEP`: hiện chỉ cảnh báo, vì item nền đã bị consume.

Sách trả lại dùng `advanced-enchantments.extraction.fixed-success-rate`, `fixed-destroy-rate` và override trong `per-enchant` nếu có.

## Chống dupe

OmniCraft không tin click từ client. GUI click bị cancel, click type rủi ro bị bỏ qua, inventory được scan server-side, mỗi player/recipe có lock riêng, nguyên liệu bị trừ trước khi phát output, và rollback nếu không phát được output.

## Xử lý lỗi

Chạy `/oc validate` sau khi sửa recipe. Chạy `/oc reload` sau khi sửa xong. Xem `logs/craft-history.log` để biết craft thành công, fail hoặc rollback vì lý do gì.
