import os

src_file = "app/src/main/java/app/pwhs/universalinstaller/presentation/setting/SettingScreen.kt"

with open(src_file, "r") as f:
    lines = f.readlines()

# Extract lines 856-1148 (index 855 to end)
components_lines = lines[855:]

# Remaining lines for SettingScreen.kt
setting_lines = lines[:855]

# Clean trailing braces if any
while setting_lines[-1].strip() == "":
    setting_lines.pop()
if setting_lines[-1].strip() == "}":
    # Wait, the last brace of what?
    pass

# We must keep the imports. We can just copy the first 110 lines of SettingScreen.kt
header = lines[:110]

dest_file = "app/src/main/java/app/pwhs/universalinstaller/presentation/setting/components/SettingComponents.kt"
os.makedirs(os.path.dirname(dest_file), exist_ok=True)

with open(dest_file, "w") as f:
    f.writelines(header)
    for line in components_lines:
        f.write(line.replace("private fun ", "internal fun "))

# Modify SettingScreen.kt to remove these lines
# We must ensure we close the SettingUi composable or if 855 is the right cut off.
with open("dump_end_of_setting.kt", "w") as f:
    f.writelines(lines[845:865])

