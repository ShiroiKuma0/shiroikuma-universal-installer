import os
import re

def fix_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Regex to find single quotes in XML string content that are NOT preceded by a backslash
    # and NOT already escaped. 
    # Also need to make sure we are inside the <string> tag content.
    
    def replace_unescaped_quote(match):
        tag_start = match.group(1)
        inner_content = match.group(2)
        tag_end = match.group(3)
        
        # In inner_content, replace ' with \' if not already preceded by \
        # Using a negative lookbehind in regex for the replacement
        fixed_inner = re.sub(r"(?<!\\)'", r"\'", inner_content)
        return f"{tag_start}{fixed_inner}{tag_end}"

    # Match <string ...>Content</string>
    pattern = re.compile(r'(<string[^>]*>)(.*?)(</string>)', re.DOTALL)
    new_content = pattern.sub(replace_unescaped_quote, content)

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

files = []
for root, dirs, filenames in os.walk('app/src/main/res'):
    for filename in filenames:
        if filename == 'strings.xml' and 'values-' in root:
            files.append(os.path.join(root, filename))

fixed_files = []
for file_path in files:
    if fix_file(file_path):
        fixed_files.append(file_path)

print(f"Fixed {len(fixed_files)} files: {', '.join(fixed_files)}")
