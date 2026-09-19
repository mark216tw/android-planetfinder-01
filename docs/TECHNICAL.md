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
| 測試 | JUnit 4.13.2 |

## 天文計算

`AstronomyRepository` 使用觀測者緯度、經度、海拔及 UTC 時間建立 `Observer` 和 `Time`：

1. `equator()` 計算視赤經與赤緯，使用日期當下赤道座標及光行差修正。
2. `horizon()` 將赤道座標轉換為地平座標，並套用一般大氣折射。
3. `illumination()` 取得照明比例及視星等。
4. `searchRiseSet()` 搜尋未來兩日內的升起與落下時間。

結果每 60 秒更新一次。APP 本身不需要執行階段網路連線即可完成上述計算。

## 方位與仰角

`HeadingSensor` 優先使用 `TYPE_ROTATION_VECTOR`。若裝置不支援，改用加速度計與磁力計建立旋轉矩陣。

- 旋轉矩陣依目前螢幕方向重新映射。
- 磁方位加上 `GeomagneticField.declination` 轉為真北方位。
- Android pitch 轉為以手機頂端為瞄準軸的仰角，範圍限制為 -90° 至 90°。
- 方位採最短角度差進行環狀平滑，仰角採線性低通平滑，係數為 0.18。
- 方位與仰角誤差都小於 3° 時視為對準。

感測器精度取決於硬體、校準及周遭磁場，不應用於安全關鍵用途。

## 定位

APP 宣告 `ACCESS_COARSE_LOCATION` 與 `ACCESS_FINE_LOCATION`，只在使用者點選位置時請求：

- API 30 以上使用 `LocationManager.getCurrentLocation()`。
- API 26–29 使用 `requestSingleUpdate()`。
- 優先採用最新的 GPS 或網路位置。
- 無權限、無提供者或無結果時回退至台北 `25.0330, 121.5654`。

## 本機資料

收藏資料位於名為 `favorites` 的 SharedPreferences，鍵值為 `body_ids`。目前沒有帳號、資料庫、雲端同步或遙測服務。

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
- 版本名稱：`1.0.0-prerelease`。

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
