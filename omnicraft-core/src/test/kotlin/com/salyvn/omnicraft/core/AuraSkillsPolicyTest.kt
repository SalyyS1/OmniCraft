package com.salyvn.omnicraft.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuraSkillsPolicyTest {
    @Test fun `contains the supported default AuraSkills ids`() {
        assertTrue("FORGING" in AuraSkillsPolicy.DEFAULT_SKILLS)
        assertTrue("MINING" in AuraSkillsPolicy.DEFAULT_SKILLS)
        assertFalse("CUSTOM_SKILL" in AuraSkillsPolicy.DEFAULT_SKILLS)
    }
}
