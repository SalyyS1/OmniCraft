package com.salyvn.omnicraft.core

class CraftMatcher {
    fun keyOf(item: CraftItem): ItemKey {
        return ItemKey(
            mode = item.mode,
            material = item.material.uppercase(),
            uid = item.uid,
            mmoType = item.mmoType?.uppercase(),
            mmoId = item.mmoId?.uppercase()
        )
    }

    fun matches(required: CraftItem, actual: InventoryEntry): Boolean {
        val expected = keyOf(required)
        if (expected.uid != null || actual.key.uid != null) {
            return expected.uid != null && expected.uid == actual.key.uid
        }
        if (expected.mode == ItemMode.MMOITEMS) {
            return actual.key.mode == ItemMode.MMOITEMS &&
                expected.mmoType == actual.key.mmoType &&
                expected.mmoId == actual.key.mmoId
        }
        return actual.key.mode == ItemMode.VANILLA && expected.material == actual.key.material
    }
}
