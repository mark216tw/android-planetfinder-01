package com.planetfinder.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun homeAndFavoritesNavigationAreVisible() {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("找到星球").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("找到星球").assertIsDisplayed()

        composeRule.onNodeWithText("收藏").performClick()
        composeRule.onNodeWithText("收藏星體").assertIsDisplayed()
    }

    @Test fun tonightTabReturnsHomeAfterOpeningCompass() {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("⌖  用羅盤尋找").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("⌖  用羅盤尋找").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("⌖  用羅盤尋找").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithText("今晚").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("此刻，在你頭頂").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("此刻，在你頭頂").assertIsDisplayed()
    }

    @Test fun displayModeChangesImmediatelyAndPersistsAfterRecreation() {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("找到星球").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("開啟顯示設定").performClick()
        composeRule.onNodeWithContentDescription("深色顯示模式").performClick()
        composeRule.onNodeWithContentDescription("深色顯示模式").assertIsSelected()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("找到星球").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("開啟顯示設定").performClick()
        composeRule.onNodeWithContentDescription("深色顯示模式").assertIsSelected()

        composeRule.onNodeWithContentDescription("系統顯示模式").performClick()
    }
}
