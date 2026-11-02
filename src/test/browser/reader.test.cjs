// Runs the actual reader template and its JavaScript in a real browser, without a live database.
const {test, before, after, beforeEach, afterEach} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {createRequire} = require('node:module');
const dependencyRequire = process.env.RUNTIME_NODE_MODULES
    ? createRequire(path.join(process.env.RUNTIME_NODE_MODULES, 'package.json')) : require;
const {chromium} = dependencyRequire('playwright');
const template = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/books/read.html'), 'utf8');
const script = template.match(/<script th:inline="javascript">([\s\S]*?)<\/script>/)[1];
const html = template.replace(/<script\b[^>]*>[\s\S]*?<\/script>/g, '');
let browser, context, page, errors;

before(async () => { browser = await chromium.launch({channel: 'msedge', headless: true}); });
after(async () => { if (browser) await browser.close(); });
beforeEach(async () => {
    context = await browser.newContext(); page = await context.newPage(); errors = [];
    page.on('pageerror', e => errors.push(e.message));
    await page.route('http://reader.test/**', route => route.fulfill({contentType:'text/html', body:html}));
    await page.goto('http://reader.test/');
});
afterEach(async () => { try { assert.deepEqual(errors, [], 'Reader must not produce JavaScript errors'); } finally { await context.close(); } });

async function load(text = 'Первая книга. Вторая КНИГА. Третья книга.') {
    await page.locator('#fullTextSource').fill(text);
    await page.locator('#readerConfig').evaluate(el => el.setAttribute('data-initial-page', '1'));
    await page.addScriptTag({content:script});
}
async function search(query) {
    await page.locator('#btnToggleSearch').click();
    await page.locator('#readerSearchInput').fill(query);
    await page.locator('#readerSearchForm').evaluate(form => form.requestSubmit());
}
test('Search is closed initially and toggle opens and closes it', async () => {
    await load();
    assert.equal(await page.locator('#readerSearch').isVisible(), false);
    await page.locator('#btnToggleSearch').click(); assert.equal(await page.locator('#readerSearch').isVisible(), true);
    await page.locator('#btnToggleSearch').click(); assert.equal(await page.locator('#readerSearch').isVisible(), false);
});
test('Opening another panel closes search and restores it on demand', async () => {
    await load(); await page.locator('#btnToggleSearch').click();
    await page.locator('#btnToggleReaderSettings').click();
    assert.equal(await page.locator('#readerSearch').isVisible(), false);
    await page.locator('#btnToggleSearch').click();
    assert.equal(await page.locator('#readerSettings').isVisible(), false);
    assert.equal(await page.locator('#readerSearch').isVisible(), true);
});
test('Russian search ignores letter case', async () => {
    await load(); await search('кНиГа');
    assert.equal(await page.locator('.reader-search-result').count(), 3);
});
test('Empty query displays a prompt without matches', async () => {
    await load(); await search('   ');
    assert.equal(await page.locator('.reader-search-result').count(), 0);
    assert.match(await page.locator('#readerSearchStatus').textContent(), /Введите/);
});
test('Missing phrase displays no-results status', async () => {
    await load(); await search('несуществующая фраза');
    assert.equal(await page.locator('.reader-search-result').count(), 0);
    assert.match(await page.locator('#readerSearchStatus').textContent(), /не найдено/);
});
test('Search finds a phrase beyond the currently displayed page', async () => {
    await load('x'.repeat(5000) + 'Искомая фраза'); await search('Искомая фраза');
    assert.equal(await page.locator('.reader-search-result-link').textContent(), 'Стр. 3');
});
test('Selecting a match changes page and closes the search panel', async () => {
    await load('x'.repeat(5000) + 'Искомая фраза'); await search('Искомая фраза');
    await page.locator('.reader-search-result-link').click();
    assert.equal(await page.locator('#pageInput').inputValue(), '3');
    assert.equal(await page.locator('#readerSearch').isVisible(), false);
    assert.match(await page.locator('#readerContent').textContent(), /Искомая фраза/);
});
test('Search results are limited to 200', async () => {
    await load('слово '.repeat(250)); await search('слово');
    assert.equal(await page.locator('.reader-search-result').count(), 200);
    assert.match(await page.locator('#readerSearchStatus').textContent(), /первые 200/);
});
test('Book markup in snippets is displayed as text', async () => {
    await load('<img src=x onerror="window.readerInjected=true"> цитата'); await search('цитата');
    assert.equal(await page.locator('#readerSearchResults img').count(), 0);
    assert.equal(await page.evaluate(() => window.readerInjected), undefined);
    assert.match(await page.locator('#readerSearchResults').textContent(), /<img/);
});
test('Repeated search replaces previous matches', async () => {
    await load('книга книга заметка'); await search('книга');
    assert.equal(await page.locator('.reader-search-result').count(), 2);
    await page.locator('#readerSearchInput').fill('заметка');
    await page.locator('#readerSearchForm').evaluate(form => form.requestSubmit());
    assert.equal(await page.locator('.reader-search-result').count(), 1);
});
