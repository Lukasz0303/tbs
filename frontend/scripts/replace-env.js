const fs = require('fs');
const path = require('path');

const supabaseUrl = process.env.SUPABASE_URL || '';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || '';

const envProdPath = path.join(__dirname, '../src/environments/environment.prod.ts');

let content = fs.readFileSync(envProdPath, 'utf8');

content = content.replace(/{{SUPABASE_URL}}/g, supabaseUrl);
content = content.replace(/{{SUPABASE_ANON_KEY}}/g, supabaseAnonKey);

fs.writeFileSync(envProdPath, content, 'utf8');

console.log('Environment variables replaced successfully');
if (!supabaseUrl || !supabaseAnonKey) {
  console.warn('Warning: SUPABASE_URL or SUPABASE_ANON_KEY is not set. Values will be empty strings.');
}

