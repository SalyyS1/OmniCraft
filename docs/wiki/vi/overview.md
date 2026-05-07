# Wiki OmniCraft

[English](../en/overview.md)

## Tổng quan

OmniCraft là plugin Paper tạo trạm chế tạo kiểu MMO. Plugin có menu category, preview recipe, yêu cầu nguyên liệu, permission, level, craft countdown tùy chọn và transaction server-side.

## Cài đặt

1. Build project.
2. Dùng `OmniCraft-legacy.jar` cho Paper 1.20-1.21.11.
3. Dùng `OmniCraft-26.jar` cho nhánh adapter 26.x.
4. Khởi động server một lần.
5. Chỉnh `config.yml`, `messages.yml` và file trong `category/`.

```powershell
.\gradlew.bat clean build
```

## Lệnh

| Lệnh | Quyền | Công dụng |
| --- | --- | --- |
| `/oc` | `omnicraft.use` | Mở main menu |
| `/oc open <category>` | `omnicraft.category.<id>` hoặc `omnicraft.open.<id>` | Mở category |
| `/oc settings` | `omnicraft.settings` | Mở menu settings |
| `/oc browse` | `omnicraft.admin` | Browse recipe |
| `/oc validate` | `omnicraft.validate` | Kiểm tra recipe đã load |
| `/oc export <category>` | `omnicraft.admin` | Export recipe category |
| `/oc import <category> <file.zip>` | `omnicraft.admin` | Import recipe category |

## Ví dụ recipe

```yaml
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
  conditions:
    - "%player_world% == world"
craft-time:
  enabled: true
  seconds: 5
extraction:
  enchant: EXTRACT
  gemstone: EXTRACT
  level: DESTROY
  success-rate: 0.75
```

## Cơ chế chống dupe

OmniCraft hủy click GUI, bỏ qua cursor item, chặn click type rủi ro, khóa transaction theo player/recipe, scan inventory server-side và rollback nếu không thể phát output.
