const fs = require('fs');
const path = require('path');

const supabaseUrl = process.env.SUPABASE_URL || '';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || '';
const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:4333/api';

const envProdPath = path.join(__dirname, '../src/environments/environment.prod.ts');

const envProdTemplate = `export const environment = {
  production: true,
  supabaseUrl: '{{SUPABASE_URL}}',
  supabaseAnonKey: '{{SUPABASE_ANON_KEY}}',
  apiBaseUrl: '{{API_BASE_URL}}'
};

`;

let content;
if (fs.existsSync(envProdPath)) {
  content = fs.readFileSync(envProdPath, 'utf8');
} else {
  content = envProdTemplate;
}

content = content.replace(/{{SUPABASE_URL}}/g, supabaseUrl);
content = content.replace(/{{SUPABASE_ANON_KEY}}/g, supabaseAnonKey);
content = content.replace(/{{API_BASE_URL}}/g, apiBaseUrl);

fs.writeFileSync(envProdPath, content, 'utf8');

console.log('Environment variables replaced successfully');
if (!supabaseUrl || !supabaseAnonKey) {
  console.warn('Warning: SUPABASE_URL or SUPABASE_ANON_KEY is not set. Values will be empty strings.');
}
if (!apiBaseUrl) {
  console.warn('Warning: API_BASE_URL is not set. Using default: http://localhost:4333/api');
}

