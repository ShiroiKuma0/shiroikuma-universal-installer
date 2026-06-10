const fs = require('fs');
const path = require('path');

function parseStrings(filePath) {
    if (!fs.existsSync(filePath)) return {};
    const content = fs.readFileSync(filePath, 'utf8');
    const regex = /<string name="([^"]+)"[^>]*>(.*?)<\/string>/gs;
    const strings = {};
    let match;
    while ((match = regex.exec(content)) !== null) {
        strings[match[1]] = match[2];
    }
    return strings;
}

const resDir = process.argv[2] || 'mobile/src/main/res';
const refDir = process.argv[3]; // Optional reference directory to get target languages from

console.log(`Checking resource directory: ${resDir}`);
if (refDir) console.log(`Using reference languages from: ${refDir}`);

const baseStrings = parseStrings(path.join(resDir, 'values', 'strings.xml'));

let targetLangs;
if (refDir) {
    targetLangs = fs.readdirSync(refDir)
        .filter(d => d.startsWith('values-') && d !== 'values-night' && fs.existsSync(path.join(refDir, d, 'strings.xml')))
        .map(d => d.replace('values-', ''));
} else {
    targetLangs = fs.readdirSync(resDir)
        .filter(d => d.startsWith('values-') && d !== 'values-night' && fs.existsSync(path.join(resDir, d, 'strings.xml')))
        .map(d => d.replace('values-', ''));
}

const untranslated = [];

targetLangs.forEach(lang => {
    const langStrings = parseStrings(path.join(resDir, `values-${lang}`, 'strings.xml'));
    Object.keys(baseStrings).forEach(key => {
        if (!langStrings[key]) {
            untranslated.push({
                key,
                base: baseStrings[key],
                lang: lang
            });
        }
    });
});

function escapeCsv(str) {
    if (str === null || str === undefined) return '""';
    const escaped = str.toString().replace(/"/g, '""');
    return `"${escaped}"`;
}

const csvRows = [
    'Key,Base,Language,Translation',
    ...untranslated.map(item => `${escapeCsv(item.key)},${escapeCsv(item.base)},${escapeCsv(item.lang)},""`)
];

fs.writeFileSync('untranslated_strings.csv', csvRows.join('\n'));
console.log(`Exported ${untranslated.length} untranslated strings to untranslated_strings.csv`);
