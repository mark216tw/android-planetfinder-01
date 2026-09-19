# 技術文件

## 技術棧

| 項目 | 版本／設定 |
|---|---|
| 語言 | Kotlin 2.1.20 |
| UI | Jetpack Compose、Material 3 |
| Android Gradle Plugin | 8.9.2 |
| Gradle Wrapper | 8.13 |
| JDK | 17 |
| compileSdk / targetSdk | 35 / 35 |
| minSdk | 26 |
| 天文函式庫 | Astronomy Engine 2.1.19 |
| 狀態／導覽 | ViewModel、StateFlow、Navigation Compose 2.9.0 |
| 測試 | JUnit 4.13.2 |

## 天文計算

`AstronomyRepository` 使用觀測者緯度、經度、海拔及 UTC 時間建立 `Observer` 和 `Time`：

1. `equator()` 計算視赤經與赤緯，使用日期當下赤道座標及光行差修正。
2. `horizon()` 將赤道座標轉換為地平座標，並套用一般大氣折射。
3. `illumination()` 取得照明比例及視星等。
4. `searchRiseSet()` 搜尋未來兩日內的升起與落下時間。

結果每 60 秒更新一次。APP 本身不需要執行階段網路連線即可完成上述計算。

月球與行星的夜間資料以裝置時區定義 18:00 至隔日 06:00，每 20 分鐘取樣一次，共 37 點。太陽則以當地日期搜尋日出與日落，建立可變長度的白天時間軸；極晝時使用完整當地日期，極夜時不建立日照時間軸。每個取樣點包含方位、仰角、太陽高度、月球高度與照明比例，以及和太陽、月球的角距。

可觀測性依下列因素加權為「推薦」、「普通」、「困難」或「不可見」：

- 仰角與是否位於地平線上。
- 日光、民用暮光、航海暮光、天文暮光或完整暗夜。
- 視星等及與太陽的角距。
- 月球位於地平線上時的照明比例及與目標角距。
- 太陽固定顯示「需濾鏡」與安全限制，不套用一般夜空推薦語意；月球不套用自身月光干擾。

畫面不顯示內部分數，只顯示等級、最多兩項主要原因及器材建議。夜空目標的最佳連續區間優先選擇最長的「推薦」區段，若沒有則選擇最長的「普通」區段。太陽的區間只表示日照存在，所有觀測仍需合格太陽濾鏡。同一星體、位置與觀測時段的結果使用最多 36 筆記憶體快取，在相同時段移動模擬時間時不重算時間軸。

## 方位與仰角

`HeadingSensor` 優先使用 `TYPE_ROTATION_VECTOR`。若裝置不支援，改用加速度計與磁力計建立旋轉矩陣。

- 旋轉矩陣依目前螢幕方向重新映射。
- 磁方位加上 `GeomagneticField.declination` 轉為真北方位。
- Android pitch 轉為以手機頂端為瞄準軸的仰角，範圍限制為 -90° 至 90°。
- 方位採最短角度差進行環狀平滑，仰角採線性低通平滑，係數為 0.18。
- 方位與仰角誤差都小於 3° 時視為對準。
- 感測器回報不可用、不可靠、低、中或高精度；對準狀態由未對準變為對準時觸發一次 120 ms 震動。
- 羅盤刻度以真北為基準，依手機方位反向旋轉；目標箭頭則使用目標方位與手機方位的最短角度差，避免重複旋轉。

感測器精度取決於硬體、校準及周遭磁場，不應用於安全關鍵用途。

## 定位

APP 宣告 `ACCESS_COARSE_LOCATION` 與 `ACCESS_FINE_LOCATION`，只在使用者點選位置時請求：

- API 30 以上使用 `LocationManager.getCurrentLocation()`。
- API 26–29 使用 `requestSingleUpdate()`。
- 優先採用最新的 GPS 或網路位置。
- 可選擇城市或輸入自訂經緯度。
- 最後位置與來源由 `LocationStore` 保存。
- 首次啟動且沒有保存位置時回退至台北 `25.0330, 121.5654`。

## 本機資料

收藏資料位於名為 `favorites` 的 SharedPreferences；位置位於 `observer_location`；顯示模式位於 `display_settings`。目前沒有帳號、資料庫、雲端同步或遙測服務。

## 顯示模式

`DisplayMode` 提供 `SYSTEM`、`LIGHT` 與 `DARK`。ViewModel 啟動時同步載入保存值，因此第一個 Compose 畫面即可使用正確主題；無保存值或值無效時回退至 `SYSTEM`。

選擇變更後會立即更新 StateFlow。`PlanetFinderApp` 將系統模式與 `isSystemInDarkTheme()` 合併為實際深淺色，再套用 Material 3 色彩並通知 Activity 更新 Edge-to-edge 狀態列及導覽列圖示。顯示模式變更不會觸發天文資料重算。

## Build Types

### `debug`

供日常開發及 Compose tooling 使用。

### `release`

啟用 R8 最佳化與資源縮減，未在公開儲存庫設定正式簽署資訊。

### `prerelease`

- 繼承 `release`。
- `isDebuggable = false`。
- 啟用 R8 與資源縮減。
- 使用 Android Debug signingConfig。
- 版本名稱：`1.2.0-prerelease`，版本代碼 `3`。

此簽署方式只適用公開測試，不適合商店正式發行。

## 常用指令

```powershell
.\gradlew.bat testPrereleaseUnitTest
.\gradlew.bat lintPrerelease
.\gradlew.bat assemblePrerelease
```

R8 mapping 輸出：

```text
app/build/outputs/mapping/prerelease/mapping.txt
```
