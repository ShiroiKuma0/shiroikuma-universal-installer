#!/bin/bash
# ==============================================================================
# Universal Installer - Android TV Screenshot Capture Script
# Tự động và bán tự động chụp ảnh màn hình Android TV chuẩn Google Play Store
# ==============================================================================

set -e

OUTPUT_DIR="fastlane_tv/metadata/android/en-US/images/tvScreenshots"
mkdir -p "$OUTPUT_DIR"

echo "========================================================================"
echo "    📺 ANDROID TV SCREENSHOT CAPTURE TOOL (GOOGLE PLAY / FASTLANE)     "
echo "========================================================================"
echo "Thư mục lưu ảnh: $OUTPUT_DIR"
echo ""

list_devices() {
    adb devices | grep -w "device" | awk '{print $1}'
}

DEVICES=($(list_devices))

if [ ${#DEVICES[@]} -eq 0 ]; then
    echo "⚠️  Chưa tìm thấy thiết bị nào kết nối qua ADB."
    read -p "👉 Nhập địa chỉ IP của Android TV (ví dụ: 192.168.1.128): " TV_IP
    if [ -n "$TV_IP" ]; then
        echo "Đang kết nối tới $TV_IP:5555..."
        adb connect "$TV_IP:5555"
        sleep 2
        DEVICES=($(list_devices))
    fi
fi

if [ ${#DEVICES[@]} -eq 0 ]; then
    echo "❌ Lỗi: Không thể kết nối với thiết bị TV nào. Vui lòng kiểm tra lại Wi-Fi/Cáp và bật ADB Debugging trên TV."
    exit 1
fi

SELECTED_DEVICE=""
if [ ${#DEVICES[@]} -eq 1 ]; then
    SELECTED_DEVICE="${DEVICES[0]}"
    echo "✅ Đã nhận diện 1 thiết bị: $SELECTED_DEVICE"
else
    echo "Tìm thấy nhiều thiết bị ADB:"
    for i in "${!DEVICES[@]}"; do
        MODEL=$(adb -s "${DEVICES[$i]}" shell getprop ro.product.model 2>/dev/null || echo "Unknown")
        echo "  [$i] ${DEVICES[$i]} ($MODEL)"
    done
    read -p "👉 Chọn số thứ tự thiết bị TV [0-$((${#DEVICES[@]}-1))]: " DEV_IDX
    SELECTED_DEVICE="${DEVICES[$DEV_IDX]}"
fi

echo ""
echo "🎯 Đang làm việc với thiết bị: $SELECTED_DEVICE"
MODEL=$(adb -s "$SELECTED_DEVICE" shell getprop ro.product.model 2>/dev/null || echo "Android TV")
WM_SIZE=$(adb -s "$SELECTED_DEVICE" shell wm size 2>/dev/null | awk '{print $3}' || echo "1920x1080")
echo "   - Tên thiết bị: $MODEL"
echo "   - Độ phân giải gốc: $WM_SIZE"
echo ""

read -p "🚀 Bạn có muốn mở app Universal Installer trên TV ngay bây giờ? (y/n) [y]: " LAUNCH_APP
LAUNCH_APP=${LAUNCH_APP:-y}
if [ "$LAUNCH_APP" = "y" ] || [ "$LAUNCH_APP" = "Y" ]; then
    echo "Đang mở app trên TV..."
    adb -s "$SELECTED_DEVICE" shell am start -n app.pwhs.universalinstaller.tv/app.pwhs.tv.MainActivity
    sleep 2
fi

capture_scene() {
    local index="$1"
    local filename="$2"
    local title="$3"
    local guide="$4"
    local target_path="$OUTPUT_DIR/$filename"

    while true; do
        echo "------------------------------------------------------------------------"
        echo "📸 CẢNH $index: $title"
        echo "👉 Hướng dẫn: $guide"
        echo "------------------------------------------------------------------------"
        echo "   [Enter]         : Chụp ảnh màn hình ngay"
        echo "   [s]             : Bỏ qua cảnh này"
        echo "   [u/d/l/r/ok/b]  : Phím điều hướng remote (Up/Down/Left/Right/Center/Back)"
        read -p "Chọn thao tác [Enter để chụp]: " ACTION

        case "$ACTION" in
            "s"|"S")
                echo "⏭️  Đã bỏ qua cảnh $index."
                return 0
                ;;
            "u"|"U")
                adb -s "$SELECTED_DEVICE" shell input keyevent 19
                ;;
            "d"|"D")
                adb -s "$SELECTED_DEVICE" shell input keyevent 20
                ;;
            "l"|"L")
                adb -s "$SELECTED_DEVICE" shell input keyevent 21
                ;;
            "r"|"R")
                adb -s "$SELECTED_DEVICE" shell input keyevent 22
                ;;
            "ok"|"OK")
                adb -s "$SELECTED_DEVICE" shell input keyevent 23
                ;;
            "b"|"B")
                adb -s "$SELECTED_DEVICE" shell input keyevent 4
                ;;
            *)
                echo "⏳ Đang chụp màn hình..."
                adb -s "$SELECTED_DEVICE" exec-out screencap -p > "$target_path"
                
                if [ -s "$target_path" ]; then
                    FILE_SIZE=$(ls -lh "$target_path" | awk '{print $5}')
                    echo "✅ Chụp thành công: $target_path ($FILE_SIZE)"
                    read -p "Bạn có muốn chụp lại cảnh này không? (y/n) [n]: " RETAKE
                    if [ "$RETAKE" != "y" ] && [ "$RETAKE" != "Y" ]; then
                        break
                    fi
                else
                    echo "❌ Lỗi: Không thể chụp hoặc file trống. Vui lòng thử lại!"
                fi
                ;;
        esac
    done
    echo ""
}

capture_scene "1/5" "1_wireless_receive.png" \
    "Màn hình Nhận File Không Dây (Receive Tab)" \
    "Dùng remote giữ ở Tab đầu tiên (Receive). Đảm bảo QR code và IP hiển thị rõ ràng trên màn hình."

capture_scene "2/5" "2_local_apks.png" \
    "Quét & Cài Đặt File APK Cục Bộ (Local Files Tab)" \
    "Chuyển sang tab phụ 'Local Files' (hiển thị danh sách file APK/XAPK trên bộ nhớ hoặc USB). Focus vào 1 card file."

capture_scene "3/5" "3_install_details.png" \
    "Dialog Chi Tiết Cài Đặt APK" \
    "Bấm phím OK/Center vào một file APK để mở hộp thoại phân tích chi tiết (Permissions, Package name, nút Install)."

capture_scene "4/5" "4_app_manager.png" \
    "Quản Lý Ứng Dụng Đã Cài (Manage Screen)" \
    "Bấm điều hướng sang cột Side Nav -> chọn Tab thứ 2 (Manage). Hiển thị lưới ứng dụng và bộ nhớ."

capture_scene "5/5" "5_settings.png" \
    "Cài Đặt & Tùy Biến Giao Diện (Settings Screen)" \
    "Bấm điều hướng sang Side Nav -> chọn Tab thứ 3 (Settings). Hiển thị tùy chọn Theme, Ngôn ngữ, Cổng mạng."

while true; do
    echo "------------------------------------------------------------------------"
    read -p "➕ Bạn có muốn chụp thêm cảnh tùy chọn nào nữa không? (y/n) [n]: " EXTRA
    if [ "$EXTRA" != "y" ] && [ "$EXTRA" != "Y" ]; then
        break
    fi
    read -p "Nhập tên file (ví dụ: 6_extra.png): " EXTRA_NAME
    if [ -n "$EXTRA_NAME" ]; then
        capture_scene "+" "$EXTRA_NAME" "Cảnh bổ sung" "Điều khiển remote đến màn hình cần chụp rồi bấm Enter."
    fi
done

echo "========================================================================"
echo "🎉 HOÀN TẤT CHỤP SCREENSHOT CHO ANDROID TV!"
echo "Tất cả ảnh đã được lưu tại: $OUTPUT_DIR"
ls -lh "$OUTPUT_DIR"
echo "========================================================================"
