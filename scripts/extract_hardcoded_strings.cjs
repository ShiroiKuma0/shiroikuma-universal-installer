const fs = require('fs');
const path = require('path');

/**
 * Script to find hardcoded strings in Kotlin/Java files that likely should be in strings.xml.
 * Usage: node scripts/extract_hardcoded_strings.cjs <directory_path>
 */

const targetDir = process.argv[2] || 'app/src/main/java';

function getAllFiles(dirPath, arrayOfFiles) {
    if (!fs.existsSync(dirPath)) return [];
    const files = fs.readdirSync(dirPath);
    arrayOfFiles = arrayOfFiles || [];
    files.forEach(function(file) {
        if (fs.statSync(dirPath + "/" + file).isDirectory()) {
            arrayOfFiles = getAllFiles(dirPath + "/" + file, arrayOfFiles);
        } else {
            arrayOfFiles.push(path.join(dirPath, "/", file));
        }
    });
    return arrayOfFiles;
}

const files = getAllFiles(targetDir).filter(f => f.endsWith('.kt') || f.endsWith('.java'));
const hardcoded = [];

// Patterns that usually indicate UI text in Compose or traditional Android
const patterns = [
    { regex: /text\s*=\s*"([^"]+)"/g, type: 'Compose Text' },
    { regex: /contentDescription\s*=\s*"([^"]+)"/g, type: 'Content Description' },
    { regex: /title\s*=\s*"([^"]+)"/g, type: 'Title' },
    { regex: /subtitle\s*=\s*"([^"]+)"/g, type: 'Subtitle' },
    { regex: /label\s*=\s*"([^"]+)"/g, type: 'Label/Tab' },
    { regex: /placeholder\s*=\s*"([^"]+)"/g, type: 'Placeholder' },
    { regex: /hint\s*=\s*"([^"]+)"/g, type: 'Hint' },
    { regex: /error\s*=\s*"([^"]+)"/g, type: 'Error Message' },
];

files.forEach(file => {
    const content = fs.readFileSync(file, 'utf8');
    patterns.forEach(p => {
        let match;
        while ((match = p.regex.exec(content)) !== null) {
            const val = match[1];
            // Filter out obviously non-UI strings:
            // - Too short (<= 1 char)
            // - Looks like a key, path, or URL
            // - Just whitespace
            if (val.trim().length > 1 && 
                !val.includes('/') && 
                !val.startsWith('http') && 
                !val.includes('package:') &&
                !val.match(/^[a-z0-9_.]+$/) // Filter out keys like "splash_fade"
            ) {
                hardcoded.push({ file, val, type: p.type });
            }
        }
    });
});

if (hardcoded.length > 0) {
    console.log(`Found ${hardcoded.length} potentially hardcoded UI strings in ${targetDir}:`);
    const csvRows = ['File,Type,Value'];
    hardcoded.forEach(item => {
        console.log(`[${item.file}] ${item.type}: "${item.val}"`);
        csvRows.push(`"${item.file}","${item.type}","${item.val.replace(/"/g, '""')}"`);
    });
    
    fs.writeFileSync('hardcoded_strings.csv', csvRows.join('\n'));
    console.log('\nResults exported to hardcoded_strings.csv');
} else {
    console.log(`No hardcoded UI strings found in ${targetDir}.`);
}
