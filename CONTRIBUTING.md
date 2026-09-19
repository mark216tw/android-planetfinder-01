# 參與貢獻

感謝協助改善「找到星球」。提交變更前，請先建立 Issue 說明問題或功能需求。

## 開發流程

1. Fork 本儲存庫並建立功能分支。
2. 使用 JDK 17 與 Android SDK 35。
3. 保持介面文字為繁體中文，並維持既有 Material 3 視覺語言。
4. 為可獨立測試的角度或天文邏輯補上單元測試。
5. 提交 Pull Request 前執行：

```powershell
.\gradlew.bat testPrereleaseUnitTest lintPrerelease assemblePrerelease
```

## 提交原則

- 提交內容應小而聚焦。
- 不提交 `local.properties`、金鑰、憑證、IDE 快取或 `build/` 產物。
- 不使用正式簽署憑證處理公開測試版本。
- 修改定位、感測器或天文計算時，需說明裝置與測試條件。

提交貢獻即表示同意依本專案的 MIT License 授權該貢獻。
