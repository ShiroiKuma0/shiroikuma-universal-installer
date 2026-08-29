src_file = "app/src/main/java/app/pwhs/universalinstaller/presentation/setting/SettingScreen.kt"

with open(src_file, "r") as f:
    lines = f.readlines()

with open(src_file, "w") as f:
    f.writelines(lines[:855])
