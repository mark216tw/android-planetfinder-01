# 找到星球

「找到星球」是一款原生 Android 天文定位工具，依據使用者的位置、時間與手機姿態，計算太陽、月球及七大行星的即時方位角與仰角，並透過羅盤引導使用者朝向目標。

> 目前版本為 `1.0.0-prerelease`，屬於測試發行版本，不代表正式上線品質。

## 主要功能

- 顯示太陽、月球、水星、金星、火星、木星、土星、天王星與海王星。
- 計算即時方位角、仰角、升起與落下時間、照明比例及視星等。
- 使用手機真北方向及仰角提供尋星指引。
- 顯示需要向左、向右、抬高或降低的角度。
- 收藏常用星體，資料保存在本機。
- 支援系統淺色與深色主題、Edge-to-edge 系統列及 Android 返回導覽。
- 未取得定位權限時，以台北座標作為預設觀測位置。

## 安裝需求

- Android 8.0（API 26）以上。
- 建議裝置具備 Rotation Vector 感測器；否則使用加速度計與磁力計估算方向。
- 定位權限為選用，但未授權時只會使用台北預設位置。

測試 APK 請由 GitHub Releases 下載。由於 Pre-release APK 使用 Android Debug 金鑰簽署，Android 可能顯示未知來源或測試應用程式提示。

## 開發環境

- JDK 17
- Android SDK 35
- Gradle 8.13
- Kotlin 2.1.20
- Jetpack Compose + Material 3
- Astronomy Engine Kotlin 2.1.19

```powershell
.\gradlew.bat testPrereleaseUnitTest
.\gradlew.bat lintPrerelease
.\gradlew.bat assemblePrerelease
```

APK 預設輸出至：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 文件

- [文件索引](docs/README.md)
- [使用指南](docs/USER_GUIDE.md)
- [系統架構](docs/ARCHITECTURE.md)
- [技術文件](docs/TECHNICAL.md)
- [系統設計](docs/SYSTEM_DESIGN.md)
- [隱私說明](docs/PRIVACY.md)
- [發行說明](docs/RELEASE.md)
- [第三方套件](docs/THIRD_PARTY.md)
- [參與貢獻](CONTRIBUTING.md)
- [安全政策](SECURITY.md)
- [版本紀錄](CHANGELOG.md)

## 安全提醒

- 切勿以肉眼、相機或未安裝合格太陽濾鏡的望遠鏡直視太陽。
- 手機磁力計容易受金屬、磁性保護殼、車輛及電器干擾。
- 本 APP 適合一般目視尋星，不適用於精密望遠鏡校準、航海或其他安全關鍵用途。

## 授權

本專案採用 [MIT License](LICENSE)。第三方套件仍依[各自授權條款](docs/THIRD_PARTY.md)提供。

Copyright (c) 2026 mark216tw
