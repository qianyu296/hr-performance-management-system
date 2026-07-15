import assert from 'node:assert/strict';
import { existsSync, rmSync } from 'node:fs';
import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { buildScenarioUpdate } from './generate-native-scenario.mjs';

const collection = JSON.parse(readFileSync(new URL('./hrpm-non-performance-api-tests.postman_collection.json', import.meta.url)));
const scenario = buildScenarioUpdate(collection, 8587615);
const httpSteps = scenario.steps.filter((step) => step.type === 'customHttp');

assert.equal(scenario.options.onError, 'end');
assert.equal(httpSteps.length, 90);
assert.ok(scenario.steps.some((step) => step.type === 'script'));
assert.ok(httpSteps.every((step) => step.customHttpRequest.path.startsWith('{{baseUrl}}/')));
assert.ok(httpSteps.every((step) => !step.customHttpRequest.path.includes('/performance')));
assert.ok(httpSteps.some((step) => step.customHttpRequest.postProcessors.some((processor) => processor.type === 'customScript')));

const createFile = new URL('./hrpm-non-performance-api-scenario.create.json', import.meta.url);
const updateFile = new URL('./hrpm-non-performance-api-scenario.update.json', import.meta.url);
rmSync(createFile, { force: true });
rmSync(updateFile, { force: true });

const generation = spawnSync(process.execPath, ['tools/apifox/generate-native-scenario.mjs', '--project-id', '8587615'], {
  cwd: new URL('../..', import.meta.url),
  encoding: 'utf8'
});

assert.equal(generation.status, 0, generation.stderr);
assert.ok(existsSync(createFile));
assert.ok(existsSync(updateFile));
assert.equal(JSON.parse(readFileSync(createFile)).name, 'HRPM 非绩效接口回归测试');
assert.equal(JSON.parse(readFileSync(updateFile)).steps.length, 91);

console.log('Native Apifox scenario generator tests passed');
