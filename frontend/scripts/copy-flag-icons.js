const fs = require('fs');
const path = require('path');

const sourceDir = path.join(__dirname, '../node_modules/flag-icons/flags');
const targetDir = path.join(__dirname, '../src/assets/flags');

function copyDirectory(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }

  const entries = fs.readdirSync(src, { withFileTypes: true });

  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);

    if (entry.isDirectory()) {
      copyDirectory(srcPath, destPath);
    } else if (entry.isFile() && entry.name.endsWith('.svg')) {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

if (fs.existsSync(sourceDir)) {
  copyDirectory(sourceDir, targetDir);
  console.log('Flag icons copied successfully to src/assets/flags');
} else {
  console.error('Source directory not found:', sourceDir);
  process.exit(1);
}

