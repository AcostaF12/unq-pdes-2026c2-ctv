// Resumen en Markdown para handleSummary. Se imprime por stdout y, si se define SUMMARY_FILE,
// también se escribe en ese archivo para que el workflow lo agregue a $GITHUB_STEP_SUMMARY.

const BUILTIN_COUNTERS = ['http_reqs', 'iterations', 'data_sent', 'data_received', 'dropped_iterations'];

function ms(value) {
  return value === undefined ? '-' : `${value.toFixed(1)} ms`;
}

function percent(value) {
  return value === undefined ? '-' : `${(value * 100).toFixed(2)} %`;
}

function metricValues(data, name) {
  return (data.metrics[name] && data.metrics[name].values) || {};
}

function thresholdRows(data) {
  const rows = [];
  Object.keys(data.metrics).forEach((name) => {
    const thresholds = data.metrics[name].thresholds || {};
    Object.keys(thresholds).forEach((expression) => {
      rows.push(`| \`${name}\` | \`${expression}\` | ${thresholds[expression].ok ? 'OK' : 'FALLÓ'} |`);
    });
  });
  return rows;
}

function allChecks(group) {
  return (group.checks || []).concat(...(group.groups || []).map(allChecks));
}

export function markdownSummary(title, data) {
  const thresholds = thresholdRows(data);
  const passed = thresholds.every((row) => row.endsWith('| OK |'));
  const reqs = metricValues(data, 'http_reqs');
  const duration = metricValues(data, 'http_req_duration');
  const lines = [
    `### ${title}: ${passed ? 'PASÓ' : 'FALLÓ'}`,
    '',
    `Target: \`${__ENV.BASE_URL || 'http://localhost:8080'}\` · duración: ${(data.state.testRunDurationMs / 1000).toFixed(0)} s · VUs máx.: ${metricValues(data, 'vus_max').max || '-'}`,
    '',
    '| Métrica | Valor |',
    '|---------|-------|',
    `| Requests | ${reqs.count || 0} (${(reqs.rate || 0).toFixed(1)}/s) |`,
    `| Errores (\`http_req_failed\`) | ${percent(metricValues(data, 'http_req_failed').rate)} |`,
    `| Latencia promedio | ${ms(duration.avg)} |`,
    `| Latencia p90 | ${ms(duration['p(90)'])} |`,
    `| Latencia p95 | ${ms(duration['p(95)'])} |`,
    `| Latencia máxima | ${ms(duration.max)} |`,
  ];

  Object.keys(data.metrics)
    .filter((name) => data.metrics[name].type === 'counter' && !BUILTIN_COUNTERS.includes(name))
    .forEach((name) => lines.push(`| \`${name}\` | ${metricValues(data, name).count} |`));

  lines.push('', '| Umbral | Condición | Resultado |', '|--------|-----------|-----------|', ...thresholds);

  const checks = allChecks(data.root_group);
  if (checks.length > 0) {
    lines.push('', '| Check | OK | Fallas |', '|-------|----|--------|');
    checks.forEach((c) => lines.push(`| ${c.name} | ${c.passes} | ${c.fails} |`));
  }
  return `${lines.join('\n')}\n`;
}

export function summaryOutputs(title, data) {
  const markdown = markdownSummary(title, data);
  const outputs = { stdout: markdown };
  if (__ENV.SUMMARY_FILE) {
    outputs[__ENV.SUMMARY_FILE] = markdown;
  }
  return outputs;
}
