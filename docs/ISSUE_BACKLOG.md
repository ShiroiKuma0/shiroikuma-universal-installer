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

- [ ] **#100** — "Delete apk after installation" không chạy (Shizuku mode)

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

  Hướng sửa, cần chọn:
  1. Thêm nhánh `file://` → `File.delete()`. App đã có `MANAGE_EXTERNAL_STORAGE`. Rẻ, chắc ăn.
  2. Sửa picker batch lấy `READ or WRITE` như nhánh 1 file.
  3. FileProvider bên thứ ba: **về nguyên tắc không xoá được**. Đừng đi dò `_data` column để
     map ra đường dẫn thật rồi xoá — mong manh và là xoá file của app khác bằng cách đoán.
  4. Dù chọn gì cũng nên **báo cho user khi xoá thất bại** thay vì im lặng.

  Ghi riêng, không thuộc #100: `Timber` chỉ `plant` khi `BuildConfig.DEBUG`
  (`Application.kt:45`), nên diagnostics report của bản release **không có một dòng log nào
  của app**. Đó là lý do issue này không có bằng chứng nào để lần. Đáng mở issue riêng.

- [ ] **#92** — Parsing error khi cài ReAppzuku 1.8.4
  - "Install with Options" cài được cùng file → parser của mình sai, không phải file hỏng.
  - Tải đúng release đó về tái hiện trước khi đoán.

- [ ] **#58** — Shizuku: `Session does not belong to uid` khi cài vào profile riêng (Android 11)
  - Đường targeted: `ManualInstallController.installTargeted()` → `ManualTargetedInstaller`,
    và `HiddenApiHacks.createPackageInstallerForUser()`.
  - Nghi vấn: session tạo bằng uid này nhưng commit bằng uid khác. Cần máy Android 11.

- [ ] **#30** — Đổi installation source không ăn, app info vẫn hiện "Universal Installer"
  - `InstallerOverrides.kt`, cờ `ROOT_SET_INSTALL_SOURCE` / `installerPackageName` trong
    `RootInstallController.createSession()`, `DEFAULT_INSTALLER_PACKAGE_NAME`.
  - Issue có **hai phần**: (a) source không đổi, (b) app cần tải từ Play Store vẫn không đi
    qua Play. (b) có thể là giới hạn của hệ thống chứ không sửa được — xác minh trước.

- [ ] **#93** — History hiện tên app **2 lần**; không xoá được log của lần cài lỗi
  - Hai bug trong một issue, tách ra khi làm.
  - Tên lặp: xem `BaseInstallController.saveHistory()` —
    `sessionData.appName.ifEmpty { sessionData.name }` — và cách `HistoryCard.kt` render.

- [ ] **#96** — Sai ký tự khi extract tên app: `DAVx⁵` → `DAVx_`
  - Tái hiện bằng `at.bitfire.davdroid`.
  - **Món dễ ăn nhất trong danh sách** — phạm vi hẹp, tái hiện được ngay. Chưa điều tra.

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
