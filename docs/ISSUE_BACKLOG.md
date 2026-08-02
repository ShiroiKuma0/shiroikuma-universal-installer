# Issue Backlog

Chốt ngày 2026-08-01 — 18 issue đang mở, không cái nào có label.
Link: `https://github.com/pass-with-high-score/universal-installer/issues/<số>`

Con trỏ code bên dưới chỉ ghi ở những issue đã thực sự mở file ra xem. Chỗ nào ghi
"chưa điều tra" nghĩa là chưa đọc code, đừng tin phỏng đoán.

---

## Chờ đóng

- [x] **#104** — Cảnh báo tương thích + theme méo trên Xiaomi
  - Đã fix ở `d92ba4d`. `InstallErrorHelper.withDeviceHint()` gắn hướng dẫn MIUI vào lỗi
    `Aborted`/`Blocked`/`Generic`; thêm trang onboarding gate theo `DeviceCompat.isXiaomi`.
  - Phần "theme méo" **không phải bug của app** — đó là hệ quả của việc tắt MIUI optimization
    (phải khởi động lại *máy* mới hết, tức là state cấp hệ thống). Chỉ cảnh báo, không sửa.
  - **Còn lại: đóng issue kèm comment tóm tắt.**

---

## Bug

- [x] **#100** — "Delete apk after installation" không chạy (Shizuku mode) — **ĐÃ ĐÓNG** 2026-08-01, fix ở `dd65b0e`

  Đã điều tra 2026-08-01. **Không liên quan gì tới Shizuku** — reporter nhầm biến số.

  Đã xác minh bằng cách đọc code:
  - Pref nối đúng: `SettingViewModel` ghi `delete_apk_after_install`,
    `InstallViewModel.readDeleteApkPref()` (dòng 1631) đọc đúng key đó. Toggle không hỏng.
  - Toàn bộ việc xoá nằm ở `BaseInstallController.deleteSourceFileIfNeeded()` và
    `deleteSourceDocument()`, và **chỉ gọi `DocumentsContract.deleteDocument`** — không có
    nhánh nào cho `file://`, không có fallback.
  - `DialogInstallActivity.collectIncomingUris()` nhận cả `scheme == "file"`. URI `file://`
    chắc chắn không xoá được bằng `deleteDocument` → **lỗi chắc chắn, không cần suy đoán**.
  - URI từ intent ngoài **không bao giờ** đi qua `takePersistableUriPermission` lúc nhận;
    chỉ picker trong app mới gọi.
  - Picker 1 file (`InstallScreen.kt:298`) lấy `READ or WRITE` → đường này *có lẽ chạy được*.
  - Picker nhiều file (`InstallScreen.kt:287`) chỉ lấy **READ**, nhưng batch install vẫn
    truyền `deleteAfterInstall` (`InstallViewModel.kt:1019`) → nghi ngờ đường batch cũng hỏng.
  - Log reporter có `VRI[DialogInstallActivity]` + `com.mixplorer` → họ cài từ file manager
    ngoài, tức là đúng đường bị hỏng.
  - Cả hai chỗ lỗi đều **nuốt exception** (`catch (_: Exception)` + `Timber.e`), nên user
    không thấy gì và tưởng tính năng không hoạt động.

  Suy đoán (chưa chạy thử):
  - `DocumentsContract.deleteDocument` fail với URI của FileProvider bên thứ ba
    (`content://com.mixplorer.fileprovider/...`) vì đó không phải document URI của một
    DocumentsProvider.

  Đã làm (1 + 2 + 4):
  1. [x] `BaseInstallController.deleteSourceFile()` thêm nhánh `file://` → `File.delete()`.
  2. [x] Picker batch (`InstallScreen.kt:287`) lấy `READ or WRITE`.
  4. [x] Xoá thất bại → toast `install_delete_source_failed` (en/zh/vi) thay vì im lặng.
  3. [ ] FileProvider bên thứ ba: **cố ý không làm**. Về nguyên tắc không xoá được, và đi dò
     `_data` column để map ra đường dẫn thật rồi xoá là mong manh + xoá file app khác bằng
     cách đoán. Giờ user ít nhất được báo và tự xoá trong file manager.

  Còn hở, chưa làm: `ManualInstallController.installTargeted()` (cài vào profile riêng) **không
  hề nhận** `originalUri`/`deleteAfterInstall` — đường này chưa bao giờ xoá file dù bật toggle.
  Sửa cần đổi chữ ký hàm + chỗ gọi, nằm ngoài phạm vi đã thống nhất.

  Reporter xác nhận 2026-08-01: file manager là **MiXplorer** → đúng trường hợp (3) không xoá
  được. Họ sẽ chỉ nhận được toast báo, không phải file bị xoá. Đã comment nói rõ.

  Ghi riêng, không thuộc #100: `Timber` chỉ `plant` khi `BuildConfig.DEBUG`
  (`Application.kt:45`), nên diagnostics report của bản release **không có một dòng log nào
  của app**. Đó là lý do issue này không có bằng chứng nào để lần. Đáng mở issue riêng.

- [ ] **#92** — Parsing error khi cài ReAppzuku 1.8.4 — **KHÔNG TÁI HIỆN ĐƯỢC** (2026-08-01)

  File: `https://github.com/gree1d/ReAppzuku` release 1.8.4, asset `ReAppzuku_1.8.4.apk`.

  Đã kiểm chứng:
  - APK **hợp lệ**. `aapt2 dump badging` đọc ngon: `com.gree1d.reappzuku`, versionCode 25,
    minSdk 23, targetSdk 36, compileSdk 36. `apksigner` xác thực v1 + v2 (không v3/v4).
    1964 entry, `resources.arsc` để `Stored`, `AndroidManifest.xml` deflate. Không có gì lạ.
  - **Chạy thử trên máy thật** (Samsung SM-N986N, Android 13 / SDK 33, build từ `main`):
    mở qua `DialogInstallActivity` bằng intent VIEW → dialog hiện đúng
    `ReAppzuku / com.gree1d.reappzuku / 1.8.4 (25) / 5.29 MB / Using Package Installer`.
    Logcat **không có** `SplitPackage enumerated 0 entries`, **không có** `Failed to parse`.
    Parser của mình chạy đúng.
  - Log có một warning lành tính: `PackageParser: Unknown element under <service>: property`
    — tag `<property>` cần API 31+, trên API 33 chỉ cảnh báo.

  Chưa kết luận được, thiếu thông tin từ reporter:
  - Chữ "parsing error" có thể là **của hệ thống** ("There was a problem parsing the package")
    chứ không phải string của mình (`install_no_splits_error`). Nếu đúng vậy thì lỗi nằm ở
    system PackageInstaller, không phải parser của app.
  - Reporter nói "Install with Options" (dùng Shizuku) cài được → nghi vấn khác biệt nằm ở
    **install mode**, không phải ở parse.

  **Cần hỏi reporter:** phiên bản Android, install mode đang dùng (Package Installer / Shizuku /
  Root), và ảnh chụp đúng thông báo lỗi. Đừng sửa mò khi chưa có mấy thông tin này.

- [x] **#58** — Shizuku: `Session does not belong to uid` khi cài vào profile riêng (Android 11)
  — **ĐÃ ĐÓNG** 2026-08-02. Reporter test bản debug: *"the issues are resolved and the app is
  working perfectly"*. Fix ở `d0bad0f`.

  Follow-up từ **solrudev** (tác giả ackpine) trong issue: *"Ackpine now supports targeted installs
  for privileged backends."* → có thể **bỏ hẳn** `HiddenApiHacks` + `ManualTargetedInstaller` và
  dùng API sẵn có. Xoá luôn cả lớp bug này. Đáng làm, xem mục dưới.

  Điều tra 2026-08-01. Nguyên nhân:

  Nghi vấn ban đầu (commit sai uid) **sai**. Lỗi nằm ở bước **ghi**, xảy ra trước commit:
  - `ManualTargetedInstaller` gọi `targetedInstaller.openSession(sessionId)`. Hàm này của
    framework làm `new Session(mInstaller.openSession(id))` — lời gọi *mở* đi qua binder đã bọc
    Shizuku (chạy dưới shell), nhưng **session binder trả về được dùng thô**. Mọi lời gọi sau đó,
    đặc biệt là `openWrite`, transact từ uid của app (10xxx) tới một session do shell (2000) sở
    hữu → `PackageInstallerSession.assertCallerIsOwnerOrRoot()` ném
    `SecurityException("Session does not belong to uid ...")`. Khớp đúng chuỗi lỗi reporter gửi.
  - Workaround `pm install-commit` qua Shizuku shell đã có sẵn trong code là đúng hướng nhưng
    **không bao giờ chạy tới** vì `openWrite` chết trước.

  Bằng chứng: ackpine (thư viện app đang dùng, Shizuku install thường của nó chạy tốt) bọc
  binder session **thêm lần nữa** — `PackageInstallerProxy.openSession()`:
  ```kotlin
  val remoteSession = IPackageInstallerSession.Stub.asInterface(
      wrapBinder(remotePackageInstaller.openSession(sessionId).asBinder()))
  ```
  Danh sách hidden-API exemption của nó cũng có `Landroid/content/pm/IPackageInstallerSession`,
  cái mà `HiddenApiHacks` đang thiếu.

  Đã sửa: thêm `HiddenApiHacks.openWrappedSession()` bọc binder session qua `ShizukuBinderWrapper`
  rồi dựng `PackageInstaller.Session` bằng reflection; `ManualTargetedInstaller` dùng hàm này.

  **Chưa verify.** Cần Android 11 + Shizuku + work profile. Đường targeted chỉ chạy khi user chọn
  profile cụ thể (`targetedUserId != null`, `InstallViewModel.kt:453`) nên không đụng luồng cài
  thường — hiện tại nó hỏng 100%, sửa không thể làm tệ hơn.

- [ ] **#30** — Đổi installation source không ăn, app info vẫn hiện "Universal Installer"

  Điều tra lại 2026-08-01. Issue đã có comment cũ kết luận "on hold vì ackpine khoá
  `initiatingPackageName` vào `com.android.shell`, Android 14+ bỏ qua `installingPackageName`".
  **Kết luận đó ít nhất là quá rộng.**

  Kiểm chứng trên máy thật (Samsung SM-N986N, Android 13):
  ```
  adb shell pm install -i com.android.vending /data/local/tmp/t.apk   → Success
  dumpsys package <pkg> → installerPackageName=com.android.vending
  ```
  Shizuku chạy dưới shell, đúng ngữ cảnh này. Tức là **cơ chế spoof có hoạt động**, ít nhất
  tới Android 13. Chưa test được Android 14+ (không có máy).

  Nguyên nhân nhiều khả năng nhất cho reporter — **fallback im lặng**:
  - `activeController()` (`InstallViewModel.kt:1578-1605`) tự nâng lên root/Shizuku khi bật
    spoof, nhưng nếu **không có backend nào READY** thì rơi về `defaultController` và chỉ
    `Timber.w(...)`. Mà Timber chỉ plant ở debug build → user bản release không thấy gì.
  - Kết quả: cài bằng system installer, installer thật sự **là** Universal Installer, và không
    có gì báo rằng cài đặt vừa bị bỏ qua.
  - `SettingScreen.kt:365` mở section Shizuku theo `uiState.useShizuku`, **không** theo Shizuku
    có thật sự chạy được không — dòng 349 còn ghi rõ "Don't gate on shizukuAvailable here".
    Nên toggle bật được kể cả khi Shizuku chưa sẵn sàng.

  Hai trường khác nhau, chỉ một cái đổi được:
  - `installingPackageName` — shell đặt được (đã kiểm chứng ở trên).
  - `initiatingPackageName` — là process tạo session, không spoof được. Qua Shizuku sẽ là
    `com.android.shell`; ở default mode là Universal Installer. Tuỳ ROM/app mà UI đọc trường nào.

  Phần Play Store trong issue: **không sửa được**. Đặt đúng `installingPackageName` không làm Play
  coi app là được cài từ Play — Play có kiểm tra riêng phía nó. Nên tách ra và trả lời dứt khoát.

  Hướng sửa, chưa làm:
  1. Báo cho user khi spoof bị bỏ qua (backend đặc quyền không sẵn sàng) thay vì log debug-only.
  2. Gate/ghi rõ setting này cần Shizuku hoặc root.

- [ ] **#93** — History hiện tên app **2 lần**; không xoá được log của lần cài lỗi

  Điều tra 2026-08-01. Đúng là hai bug rời nhau.

  **(b) Không xoá được lần cài lỗi — ĐÃ XÁC ĐỊNH, đọc code là thấy:**
  - `SessionCard.kt` chỉ hiện nút huỷ khi `if (!hasError && sessionData.isCancellable && !isComplete)`.
    Session lỗi vì thế **chỉ có nút Retry**, không có đường nào bỏ nó đi.
  - `BaseInstallController.handleError()` chỉ gọi `setError()`, **không** `removeSessionData()`.
    Nên card lỗi nằm lại trong list vĩnh viễn.
  - Sửa: cho phép dismiss khi `hasError` (gọi `cancel()` hoặc `removeSessionData()` trực tiếp).

  **(a) Tên hiện 2 lần — hai cơ chế khả dĩ, chưa phân định được:**
  1. **Hai DÒNG trong history** (khớp nghĩa đen "shown 2 times"). Mỗi `InstallViewModel` tự dựng
     controller riêng, nên map `activeSessions` là **per-instance**. `restoreSessionsFromSavedState()`
     chạy trong `.onStart` của uiState (`InstallViewModel.kt:226`) ở **mọi** VM. Dialog flow chạy
     session trên `appScope` nên nó sống lâu hơn VM của dialog; khi `InstallActivity` mở, VM thứ hai
     restore đúng session đó và `awaitSession` lần nữa → `Succeeded` hai lần → **`saveHistory` hai
     lần → hai dòng**. Guard `if (activeSessions.containsKey(...))` không chặn được vì khác instance.
  2. **Một dòng nhưng hai dòng chữ giống nhau.** `saveHistory` dùng
     `appName.ifEmpty { sessionData.name }`, còn `HistoryCard.kt:129,136` render `appName` rồi
     `fileName`. Khi không parse được label, cả hai đều là tên file.

  Chưa phân định được vì máy test không có `sqlite3`, và pull DB về là copy lịch sử cài đặt thật
  của người dùng. **Cần hỏi reporter ảnh chụp history** — hai card riêng biệt hay một card hai dòng
  chữ — là biết ngay đường nào.

- [x] **#96** — Sai ký tự khi extract tên app: `DAVx⁵` → `DAVx_` — **ĐÃ TÌM RA NGUYÊN NHÂN**

  `ApkExtractor.sanitize()` (`core/.../install/ApkExtractor.kt:322`) dùng whitelist:
  ```kotlin
  c.isLetterOrDigit() -> c
  c == ' ' || c == '-' || c == '_' || c == '.' || c == '(' || c == ')' -> c
  else -> '_'
  ```
  `⁵` là U+2075 SUPERSCRIPT FIVE, Unicode category **No** (Number-other), không phải **Nd**.
  `Character.isDigit()` chỉ đúng với Nd. Kiểm chứng bằng Java thật:
  ```
  char=⁵ isLetter=false isDigit=false isLetterOrDigit=false
  ```
  → rơi vào `else` → `_`. Đúng y báo cáo.

  Không chỉ mỗi `⁵`: mọi ký hiệu ngoài chữ-số đều bị nuốt (`™`, `®`, emoji, ký hiệu toán). CJK và
  tiếng Việt thì an toàn vì chúng là **Lo/Ll** nên `isLetter()` = true.

  Hướng sửa: **đảo lại thành blacklist**. Chỉ thay những ký tự thật sự bất hợp lệ trên filesystem
  (`/ \ : * ? " < > |` và ký tự điều khiển), giữ nguyên phần còn lại. FAT32/exFAT/ext4 đều nhận
  Unicode bình thường.

---

## Feature

- [ ] **#102** — Mục Help/Tutorial cho Settings
  - Từ review Play Store: settings "very sophisticated", khó hiểu.
  - **Nối thẳng vào việc vừa làm** (trang onboarding VirusTotal, `35108d1`), nhưng khác chỗ:
    #102 xin help **trong Settings**, mà onboarding thì user cũ không bao giờ thấy lại
    (`ONBOARDING_COMPLETED` đã `true`). Cần chỗ khác: `SettingScreen.kt`.
  - Ứng viên đáng làm tiếp nhất.

- [ ] **#59** — VirusTotal cho app đã cài
  - Khiên xanh cho app đã quét sạch, cảnh báo đỏ khi phát hiện, ngay trong App Manager.
  - `ManageViewModel.kt` / `ManageScreen.kt` đã có tham chiếu VirusTotal sẵn — xem lại
    trước khi dựng mới. Hạ tầng có rồi: `VirusTotalService`, `VtResult`, `VtStatus`.

- [ ] **#103** — Cài app ra thẻ nhớ ngoài, nhất là app có OBB
  - Đã có sẵn đường OBB: `ObbCopyWorker`, `SafObbWriter`, `ShizukuObbWriter`, `ObbExtractor`.

- [ ] **#101** — Dhizuku support + custom authorizer (tham khảo InstallerX Revived)
  - Repo có sẵn `InstallerX-Revived-main/` để đối chiếu.

- [ ] **#95** — Thêm tuỳ chọn cài đặt, kiểu "Install with Options"
  - Trùng ý một phần với **#73**. Cân nhắc gộp.

- [ ] **#73** — Bỏ nút "menu", gộp thành một trang dài; thêm phát hiện tracker
  - Hai yêu cầu rời nhau. Phần tracker detection là việc lớn (cần database tracker).

- [ ] **#97** — Tự đóng app sau khi đóng context menu cài đặt
  - `DialogInstallActivity` **đã** có `android:excludeFromRecents="true"` +
    `launchMode="singleInstance"` trong manifest, vậy mà reporter vẫn thấy trong Recents.
    Tìm hiểu tại sao trước khi thêm `finishAndRemoveTask()`.

- [ ] **#94** — Đăng ký file type cho bản TV
  - Phone có "Open with" → Universal Installer, TV thì không. Thêm intent-filter vào
    manifest của module `tv`.

- [ ] **#70** — Shizuku cho bản Android TV
  - `tv/.../ReceiveViewModel.kt` hiện chỉ có đường root + đường thường.

---

## Không phải task kỹ thuật

- [ ] **#31** và **#13** — cùng chủ đề: 09/2026 Google chặn sideload app của developer
  chưa verify. User muốn banner / lên tiếng phản đối.
  - Đây là quyết định lập trường của maintainer, không phải bug. Trả lời rồi đóng, hoặc
    chuyển sang Discussions.

---

## Chưa có issue — tìm ra trong lúc làm (2026-08-01/02)

- [ ] **Bỏ `HiddenApiHacks` + `ManualTargetedInstaller`, dùng targeted install của ackpine.**
  solrudev xác nhận trong #58 là ackpine đã hỗ trợ targeted install cho privileged backend. Hiện
  mình tự reflection vào `IPackageInstaller`. Bỏ được thì xoá luôn cả lớp bug kiểu #58, và có thể
  cả hai lỗ hổng của `installTargeted` bên dưới.

- [ ] **`ManualInstallController.installTargeted()` thiếu `originalUri`/`deleteAfterInstall`/
  `allowDowngrade`** (dòng 58-65). Cài vào profile riêng: không bao giờ xoá file nguồn dù bật
  toggle, và không nhận consent downgrade. Cả hai fix hôm qua đều không với tới đường này.

- [ ] **Double-await cross-VM → history ghi 2 lần.** `activeSessions` là `private val` per-instance
  (`BaseInstallController.kt:42`), mà guard `if (activeSessions.containsKey(...)) continue`
  (dòng 167) dựa vào nó. Hai `InstallViewModel` cùng restore một session → cùng `awaitSession` →
  `saveHistory` hai lần. Đây là giả thuyết số 1 của **#93**; fix retry (`65ba753`) **không** chạm
  tới nó.

- [ ] **`Timber` chỉ plant ở debug** (`Application.kt:45`) → Diagnostics report của bản release
  không có dòng log nào của app. Là lý do #100 và #92 gửi kèm nguyên trang log mà không manh mối.

- [ ] **Spoof install source thất bại im lặng** (#30) — không có Shizuku/root thì rơi về default
  installer, chỉ `Timber.w`, tức vô hình ở release.

- [ ] **CI "Build and Test" chỉ chạy `assembleDebug`.** Unit test không configure được trên JDK 24
  (`Type T not present`, kể cả `ExampleUnitTest` có sẵn).

## Fix đã lên main nhưng CHƯA verify

| Fix | Cần gì để verify |
|---|---|
| Uninstall qua Shizuku/root (`4f9fa83`) | Máy có Shizuku hoặc root |
| `allowDowngrade` consent (`5c79a08`) | Shizuku hoặc root |
| Dismiss + Retry (`bb9edb5`, `65ba753`) | Chỉ cần cắm máy — verify được ngay |
| MIUI hint (`d92ba4d`, #104) | Máy Xiaomi/Redmi/POCO |
