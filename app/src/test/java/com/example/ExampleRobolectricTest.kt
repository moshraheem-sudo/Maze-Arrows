package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context and verify reverse level generator`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Amaze Arrows", appName)

    val level1 = com.example.engine.SmartLevelGenerator.generateCampaignLevel(1, 0x12345678L)
    val report = com.example.engine.ArrowEscapeSolver.solve(level1.boardWidth, level1.boardHeight, level1.arrows)
    assertEquals(true, report.isSolvable)
  }
}
