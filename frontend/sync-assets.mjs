import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const destination = resolve(root, '../demo/src/main/resources/static/vendor');
const files = {
  'materialize/materialize.min.js': '@materializecss/materialize/dist/js/materialize.min.js',
  'materialize/materialize.min.css': '@materializecss/materialize/dist/css/materialize.min.css',
  'materialize/materialize.colors.min.css': '@materializecss/materialize/dist/css/materialize.colors.min.css',
  'materialize/LICENSE': '@materializecss/materialize/LICENSE',
  'jquery/jquery.min.js': 'jquery/dist/jquery.min.js',
  'jquery/LICENSE.txt': 'jquery/LICENSE.txt',
  'font-awesome/css/font-awesome.min.css': 'font-awesome/css/font-awesome.min.css',
  'font-awesome/LICENSE-README.md': 'font-awesome/README.md'
};
for (const font of ['FontAwesome.otf', 'fontawesome-webfont.eot', 'fontawesome-webfont.svg', 'fontawesome-webfont.ttf', 'fontawesome-webfont.woff', 'fontawesome-webfont.woff2']) {
  files['font-awesome/fonts/' + font] = 'font-awesome/fonts/' + font;
}
const manifest = { packages: JSON.parse(await readFile(resolve(root, 'package.json'), 'utf8')).dependencies, sha256: {} };
for (const [target, source] of Object.entries(files)) {
  const path = resolve(destination, target);
  await mkdir(dirname(path), { recursive: true });
  await copyFile(resolve(root, 'node_modules', source), path);
  manifest.sha256[target] = createHash('sha256').update(await readFile(path)).digest('hex');
}
await writeFile(resolve(destination, 'ASSETS.json'), JSON.stringify(manifest, null, 2) + '\n');
console.log('Synchronized pinned browser assets and license notices');
