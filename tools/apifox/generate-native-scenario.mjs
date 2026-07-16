import { readFileSync, writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const scenarioName = 'HRPM 非绩效接口回归测试';
const scenarioDescription = '自动生成：覆盖 69 个非绩效接口映射，共 90 个按依赖顺序执行的请求场景。';
const scenarioOptions = { onError: 'end', saveReportDetail: 'error' };

function flattenItems(items, result = []) {
  for (const item of items ?? []) {
    if (Array.isArray(item.item)) {
      flattenItems(item.item, result);
    } else if (item.request) {
      result.push(item);
    }
  }
  return result;
}

function scriptData(event) {
  return (event?.script?.exec ?? []).join('\n').trim();
}

function processors(events, listen) {
  return (events ?? [])
    .filter((event) => event.listen === listen)
    .map(scriptData)
    .filter(Boolean)
    .map((data) => ({
      type: 'customScript',
      enable: true,
      defaultEnable: true,
      data
    }));
}

function requestBody(body) {
  if (body?.mode === 'raw') {
    return {
      type: body.options?.raw?.language === 'json' ? 'json' : 'raw',
      data: body.raw ?? ''
    };
  }

  return { type: 'none', data: '' };
}

function customHttpStep(item, projectId, number) {
  const request = item.request;
  const rawUrl = typeof request.url === 'string' ? request.url : request.url.raw;

  if (!rawUrl?.startsWith('{{baseUrl}}/')) {
    throw new Error(`请求 ${item.name} 未使用 {{baseUrl}} 变量：${rawUrl ?? '<empty>'}`);
  }

  return {
    id: `custom-http-${number}`,
    number,
    type: 'customHttp',
    disable: false,
    customHttpRequest: {
      id: 0,
      name: item.name,
      folderId: 0,
      projectId,
      path: rawUrl,
      method: request.method.toLowerCase(),
      parameters: {
        path: [],
        query: [],
        header: (request.header ?? []).map((header) => ({
          name: header.key,
          value: header.value,
          enable: !header.disabled
        })),
        cookie: []
      },
      requestBody: requestBody(request.body),
      preProcessors: processors(item.event, 'prerequest'),
      postProcessors: processors(item.event, 'test'),
      auth: {},
      advancedSettings: {},
      commonParameters: {}
    }
  };
}

export function buildScenarioCreate() {
  return {
    name: scenarioName,
    description: scenarioDescription,
    folderId: 0,
    priority: 1,
    tags: ['HRPM', '非绩效', '回归'],
    options: scenarioOptions
  };
}

export function buildScenarioUpdate(collection, projectId) {
  if (!Number.isInteger(projectId) || projectId <= 0) {
    throw new Error('projectId 必须是正整数。');
  }

  const requests = flattenItems(collection.item);
  const steps = [
    {
      id: 'bootstrap-run-stamp',
      number: 1,
      type: 'script',
      disable: false,
      parameters: {
        type: 'customScript',
        enable: true,
        defaultEnable: true,
        data: "if (!pm.environment.get('runStamp')) pm.environment.set('runStamp', Date.now().toString());"
      }
    },
    ...requests.map((item, index) => customHttpStep(item, projectId, index + 2))
  ];

  return {
    name: scenarioName,
    description: scenarioDescription,
    priority: 1,
    tags: ['HRPM', '非绩效', '回归'],
    options: scenarioOptions,
    steps
  };
}

function parseProjectId(args) {
  const optionIndex = args.indexOf('--project-id');
  if (optionIndex === -1 || !args[optionIndex + 1]) {
    throw new Error('请通过 --project-id <Apifox项目ID> 指定项目 ID。');
  }

  const projectId = Number(args[optionIndex + 1]);
  if (!Number.isInteger(projectId) || projectId <= 0) {
    throw new Error('--project-id 必须是正整数。');
  }
  return projectId;
}

function main() {
  const projectId = parseProjectId(process.argv.slice(2));
  const directory = new URL('.', import.meta.url);
  const collection = JSON.parse(readFileSync(new URL('./hrpm-non-performance-api-tests.postman_collection.json', directory)));
  const create = buildScenarioCreate();
  const update = buildScenarioUpdate(collection, projectId);

  writeFileSync(new URL('./hrpm-non-performance-api-scenario.create.json', directory), `${JSON.stringify(create, null, 2)}\n`);
  writeFileSync(new URL('./hrpm-non-performance-api-scenario.update.json', directory), `${JSON.stringify(update, null, 2)}\n`);
  console.log(`已生成 Apifox 原生场景文件：${update.steps.length} 个步骤（${update.steps.length - 1} 个 HTTP 请求）。`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
