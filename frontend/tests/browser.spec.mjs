import { test, expect } from '@playwright/test';

test('public forms load local assets without browser errors or external dependencies', async ({ page, baseURL }) => {
  const errors = [];
  const external = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('**/*', route => {
    if (new URL(route.request().url()).origin !== new URL(baseURL).origin) {
      external.push(route.request().url());
      return route.abort();
    }
    return route.continue();
  });
  for (const path of ['/', '/login', '/register']) {
    await page.goto(path, { waitUntil: 'networkidle' });
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('nav.navbar')).toBeVisible();
    expect(await page.locator('nav li').first().evaluate(el => getComputedStyle(el).listStyleType)).toBe('none');
    expect(await page.evaluate(() => typeof M.FormSelect.init)).toBe('function');
  }
  const manifest = await (await page.request.get('/vendor/ASSETS.json')).json();
  expect(manifest.packages['@materializecss/materialize']).toBe('2.4.0');
  await expect(page.locator('button.green')).toHaveCSS('background-color', 'rgb(76, 175, 80)');
  expect(external).toEqual([]);
  expect(errors).toEqual([]);
});

test('notification messages render attacker-controlled markup as plain text', async ({ page }) => {
  await page.goto('/login');
  const message = '<img src=x onerror="window.xssExecuted=true">';
  await page.evaluate(text => new M.Toast({ text, displayLength: 10000 }), message);
  await expect(page.locator('.toast')).toHaveText(message);
  await expect(page.locator('.toast img')).toHaveCount(0);
  expect(await page.evaluate(() => window.xssExecuted)).toBeUndefined();
});

test('browser registration/login and inventory rendering preserve safe component behavior', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const id = String(Date.now());
  const email = `browser-${id}@example.test`;
  await page.goto('/register');
  await page.locator('[name=user_name]').fill('Browser regression');
  await page.locator('[name=user_mail]').fill(email);
  await page.locator('[name=user_pass]').fill('Browser-regression-42');
  await page.locator('[name=user_id]').fill(id);
  await page.locator('button[type=submit]').click();
  await expect(page).toHaveURL(/\/login\?success=registration_successful/);
  await page.locator('[name=username]').fill(email);
  await page.locator('[name=password]').fill('Browser-regression-42');
  await page.locator('button[type=submit]').click();
  await expect(page).toHaveURL(/\/$/);
  const payload = '<img src=x onerror="window.xssExecuted=true">';
  // Exercise the DOM renderer with untrusted API text; server-side role checks
  // remain covered by the Java and HTTP suites.
  await page.route('**/guns/list', route => route.fulfill({ json: [
    { id: 42, gunName: payload, model: 'Test', quantity: 1, registeredBy: 'Browser check' }
  ] }));
  await page.goto('/inventory_management', { waitUntil: 'networkidle' });
  await expect(page.locator('#inventoryTable tbody')).toContainText(payload);
  await expect(page.locator('#inventoryTable tbody img')).toHaveCount(0);
  expect(await page.evaluate(() => window.xssExecuted)).toBeUndefined();
  expect(await page.locator('select').count()).toBeGreaterThan(0);
  expect(await page.evaluate(() => Array.from(document.querySelectorAll('select')).every(el => M.FormSelect.getInstance(el)))).toBeTruthy();
  expect(errors).toEqual([]);
});
