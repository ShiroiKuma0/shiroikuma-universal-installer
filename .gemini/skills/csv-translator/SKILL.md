---
name: csv-translator
description: Specialized skill for translating large CSV files. It follows a multi-step process of planning, counting lines, splitting large files into manageable chunks, translating chunks, merging, and finally importing translations into Android resource files. Use when the user needs to translate a CSV file, especially large ones.
---

# CSV Translator Workflow

This skill provides a robust way to translate CSV files while managing context window limits by splitting large files into smaller chunks.

## Workflow Steps

### 1. Planning & Analysis
- **Identify Target:** Determine the CSV file to be translated and the target language(s).
- **Count Lines:** Use `run_shell_command` with `wc -l` to determine the size of the file.
- **Set Chunk Size:** Based on the file size and complexity, decide on a target number of lines per chunk (default recommendation: 100-200 lines).

### 2. Splitting (If Large)
- If the file exceeds the manageable limit, split it into smaller files.
- Use a script (e.g., `scripts/split_csv.cjs`) to ensure data integrity (keeping headers in each chunk or handling them correctly).
- **Recursive Check:** If a split file is still considered too large for a single translation turn, split it further.

### 3. Translation
- Translate each chunk individually.
- Maintain the CSV structure and ensure that keys/IDs (if any) are preserved.
- Use a consistent tone and terminology across all chunks.

### 4. Merging
- Once all chunks are translated, merge them back into a single final CSV file.
- Use a script (e.g., `scripts/merge_csv.cjs`) to combine the files, ensuring only one header row exists.

### 5. Importing (Android Specific)
- Use `scripts/import_translations.cjs` to automatically take the merged translations and update the project's `strings.xml` files.
- Command: `node scripts/import_translations.cjs <merged_csv_path> <res_directory>`

### 6. Validation
- Verify the final CSV file structure and line count matches the original.
- Run a build check (e.g., `./gradlew assembleDebug`) to ensure the imported strings don't break the build.

## Guardrails
- **Preserve Structure:** Never change the number of columns or the meaning of specific fields (like IDs).
- **Header Integrity:** Ensure the header row is handled correctly during splitting and merging.
- **Encoding:** Maintain UTF-8 encoding throughout the process.
