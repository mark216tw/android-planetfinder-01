# Pre-release 發行說明

> **此版本為 Pre-release 版本，不是正式上線版本，僅供功能驗證與測試。**

## 版本資訊

- 版本名稱：`1.0.0-prerelease`
- 版本代碼：`1`
- 套件名稱：`com.planetfinder.app`
- Build Type：`prerelease`
- 最低 Android：8.0（API 26）
- Target SDK：35
- 簽署：Android Debug 金鑰
- 發行狀態：Pre-release，非正式上線版本

## APK

GitHub Release 附件名稱：`app-prerelease.apk`

本次文件與 GitHub 發行作業使用既有 APK，未重新執行建置、系統測試或程式碼掃描流程。

既有 APK 資訊：

```text
大小：953,232 bytes
SHA-256：E5990D13758E9740B7AADCF4E23E29CED1C1D8270AACBE4FC04103052E70A421
```

## 已知限制

- Debug 金鑰不適合正式發行，也無法直接升級為不同金鑰簽署的正式版本。
- 羅盤與仰角尚未涵蓋所有 Android 實機型號測試。
- 磁場干擾可能造成方向偏移。
- 可見狀態只依幾何地平線判斷，不包含天候、地形、建築及光害。
- 位置拒絕時固定使用台北座標。
- 目前沒有手動城市選擇、通知、星座或 AR 相機模式。

## 安裝提醒

使用者可能需要允許未知來源安裝。此版本只供功能驗證與回饋，不應作為正式產品散布或依賴。
