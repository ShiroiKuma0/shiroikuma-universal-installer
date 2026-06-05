package app.pwhs.universalinstaller

import app.pwhs.universalinstaller.ui.theme.ButtonStyle
import app.pwhs.universalinstaller.ui.theme.SurfaceTheme
import app.pwhs.universalinstaller.ui.theme.SurfaceThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceThemeStoreTest {
    @Test
    fun buttonsRoundTrip() {
        val theme = SurfaceTheme(
            background = -16777216,
            borderColor = -256,
            borderWidth = 1f,
            buttons = mapOf(
                "menu" to ButtonStyle(bg = -16777216, content = -256, borderColor = -256, borderWidth = 1f),
            ),
        )
        val json = SurfaceThemeStore.serialize(theme)
        println("SERIALIZED=$json")
        val back = SurfaceThemeStore.parse(json)
        println("PARSED.buttons=${back.buttons}")
        assertTrue("buttons map should contain 'menu'", back.buttons.containsKey("menu"))
        assertEquals(theme.buttons, back.buttons)
        assertEquals(theme.background, back.background)
    }
}
