package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TokenStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Compilador GitHub", appName)
  }

  @Test
  fun `token storage save and masking`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tokenStorage = TokenStorage(context)
    tokenStorage.clear()
    assertFalse(tokenStorage.hasToken())

    val sampleToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
    tokenStorage.saveToken(sampleToken)
    assertTrue(tokenStorage.hasToken())
    assertEquals(sampleToken, tokenStorage.getToken())

    val masked = tokenStorage.getMaskedToken()
    assertTrue(masked.startsWith("ghp_"))
    assertTrue(masked.contains("••••••••"))
    assertTrue(masked.endsWith("wxyz"))

    tokenStorage.clear()
    assertFalse(tokenStorage.hasToken())
  }
}
