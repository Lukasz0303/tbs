export const environment = {
  production: true,
  // These should be set from your actual Supabase project settings
  supabaseUrl: process.env['SUPABASE_URL'] || '',
  supabaseAnonKey: process.env['SUPABASE_ANON_KEY'] || ''
};

