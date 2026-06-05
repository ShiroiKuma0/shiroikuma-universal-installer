package app.pwhs.universalinstaller

import app.pwhs.universalinstaller.ui.theme.ButtonStyle
import app.pwhs.universalinstaller.ui.theme.SurfaceTheme
import app.pwhs.universalinstaller.ui.theme.SurfaceThemeStore
import app.pwhs.universalinstaller.ui.theme.TextStyleOverride
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

    @Test
    fun textsRoundTrip() {
        val theme = SurfaceTheme(
            texts = mapOf(
                "version" to TextStyleOverride(color = -256, fontWeight = 700, fontScale = 1.2f),
                "app_label" to TextStyleOverride(fontFamily = "Roboto.ttf"),
            ),
        )
        val back = SurfaceThemeStore.parse(SurfaceThemeStore.serialize(theme))
        assertEquals(theme.texts, back.texts)
        assertEquals(-256, back.texts["version"]?.color)
        assertEquals("Roboto.ttf", back.texts["app_label"]?.fontFamily)
    }
}
