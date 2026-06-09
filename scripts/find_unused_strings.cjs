const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

function findUnused(resPath) {
    if (!fs.existsSync(resPath)) return;
    
    const content = fs.readFileSync(resPath, 'utf8');
    const stringRegex = /<string name="([^"]+)"[^>]*>/g;
    let match;
    const keys = new Set();
    while ((match = stringRegex.exec(content)) !== null) {
        keys.add(match[1]);
    }

    console.log(`\nChecking ${resPath} (${keys.size} strings)...`);

    // Scan all .kt, .java, .xml files for references
    const searchCmd = `grep -rE "@string/|R\\.string\\." . --include="*.kt" --include="*.java" --include="*.xml" --exclude-dir={.git,.gradle,build,node_modules} --exclude="*strings.xml"`;
    let searchOutput = '';
    try {
        searchOutput = execSync(searchCmd).toString();
    } catch (e) {
        searchOutput = e.stdout ? e.stdout.toString() : '';
    }

    const usedKeys = new Set();
    // Match @string/KEY
    const atStringRegex = /@string\/([a-zA-Z0-9_]+)/g;
    while ((match = atStringRegex.exec(searchOutput)) !== null) {
        usedKeys.add(match[1]);
    }

    // Match R.string.KEY
    const rStringRegex = /R\.string\.([a-zA-Z0-9_]+)/g;
    while ((match = rStringRegex.exec(searchOutput)) !== null) {
        usedKeys.add(match[1]);
    }

    const unused = [];
    for (const key of keys) {
        if (!usedKeys.has(key)) {
            unused.push(key);
        }
    }

    console.log(`Unused strings: ${unused.length}`);
    if (unused.length > 0) {
        unused.forEach(k => console.log(`  - ${k}`));
        if (process.argv.includes('--remove')) {
            console.log(`Removing from ${resPath}...`);
            let newContent = content;
            unused.forEach(key => {
                // Improved regex to match the entire line or tag
                const entryRegex = new RegExp(`\\s*<string name="${key}"[^>]*>.*?</string>`, 'gs');
                newContent = newContent.replace(entryRegex, '');
            });
            fs.writeFileSync(resPath, newContent);
        }
    }
}

findUnused('mobile/src/main/res/values/strings.xml');
findUnused('tv/src/main/res/values/strings.xml');

if (!process.argv.includes('--remove')) {
    console.log('\nRun with --remove to delete these strings.');
}
