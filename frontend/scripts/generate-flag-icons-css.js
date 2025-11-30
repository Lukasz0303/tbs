const fs = require('fs');
const path = require('path');

const flagsDir = path.join(__dirname, '../node_modules/flag-icons/flags');
const outputDir = path.join(__dirname, '../src/styles');
const outputFile = path.join(outputDir, 'flag-icons.css');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const baseCSS = `.fib, .fi {
  background-size: contain;
  background-position: 50%;
  background-repeat: no-repeat;
}

.fi {
  position: relative;
  display: inline-block;
  width: 1.333333em;
  line-height: 1em;
}
.fi:before {
  content: " ";
}
.fi.fis {
  width: 1em;
}
`;

function getFlagFiles(dir) {
  const files = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  
  for (const entry of entries) {
    if (entry.isDirectory()) {
      files.push(...getFlagFiles(path.join(dir, entry.name)));
    } else if (entry.isFile() && entry.name.endsWith('.svg')) {
      const relativePath = path.relative(flagsDir, path.join(dir, entry.name));
      const code = entry.name.replace('.svg', '');
      const format = path.basename(dir);
      files.push({ code, format, relativePath });
    }
  }
  
  return files;
}

const flagFiles = getFlagFiles(flagsDir);
const flagsByCode = {};

for (const file of flagFiles) {
  if (!flagsByCode[file.code]) {
    flagsByCode[file.code] = {};
  }
  flagsByCode[file.code][file.format] = file.relativePath.replace(/\\/g, '/');
}

let css = baseCSS;

for (const [code, formats] of Object.entries(flagsByCode)) {
  if (formats['4x3']) {
    css += `.fi-${code} {\n  background-image: url(/assets/flags/${formats['4x3']});\n}\n`;
  }
  if (formats['1x1']) {
    css += `.fi-${code}.fis {\n  background-image: url(/assets/flags/${formats['1x1']});\n}\n`;
  }
}

fs.writeFileSync(outputFile, css);
console.log('Flag icons CSS generated successfully');

