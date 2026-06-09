const fs = require('fs');
const path = require('path');

function parseStrings(filePath) {
    if (!fs.existsSync(filePath)) {
        console.log(`File not found: ${filePath}`);
        return {};
    }
    const content = fs.readFileSync(filePath, 'utf8');
    const regex = /<string name="([^"]+)"[^>]*>(.*?)<\/string>/gs;
    const strings = {};
    let match;
    while ((match = regex.exec(content)) !== null) {
        strings[match[1]] = match[2];
    }
    return strings;
}

const resDir = 'mobile/src/main/res';
const baseStrings = parseStrings(path.join(resDir, 'values', 'strings.xml'));
console.log(`Base strings: ${Object.keys(baseStrings).length}`);

const viStrings = parseStrings(path.join(resDir, 'values-vi', 'strings.xml'));
console.log(`Vietnamese strings: ${Object.keys(viStrings).length}`);

const missingVi = Object.keys(baseStrings).filter(key => !viStrings[key]);
console.log(`Missing in Vietnamese: ${missingVi.length}`);
if (missingVi.length > 0) {
    console.log(`First missing: ${missingVi[0]}`);
}
