import os
import re

files = []
for root, dirs, filenames in os.walk('app/src/main/res'):
    for filename in filenames:
        if filename == 'strings.xml' and 'values-' in root:
            files.append(os.path.join(root, filename))

valid_escapes = ["'", '"', 'n', 't', 'r', 'u', '\\']

suspects = []
for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Find all backslashes
    for i, char in enumerate(content):
        if char == '\\':
            next_char = content[i+1] if i+1 < len(content) else None
            if next_char not in valid_escapes:
                # Check if it's a placeholder like %s or %1$s
                # Actually \ is not used for placeholders, % is.
                # Is it \&quot; ? No, & is the start there.
                suspects.append((file_path, content[max(0, i-10):min(len(content), i+10)]))

if suspects:
    print(f"Found {len(suspects)} suspect backslashes:")
    for s in suspects:
        print(f"{s[0]}: ...{repr(s[1])}...")
else:
    print("No suspect backslashes found.")
