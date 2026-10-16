import { defineConfig } from '@playwright/test';
const baseURL = process.env.EKOTE_BROWSER_BASE_URL || 'http://127.0.0.1:18082';
if (!['127.0.0.1', 'localhost'].includes(new URL(baseURL).hostname)) {
  throw new Error('Browser regression checks accept only disposable local app instances');
}
export default defineConfig({
  testDir: './tests',
  workers: 1,
  retries: 0,
  use: { baseURL, headless: true, trace: 'retain-on-failure' },
  reporter: 'list'
});
