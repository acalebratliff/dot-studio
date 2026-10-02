'use strict';

importScripts('viz-js/viz-global.js');

const vizReady = Viz.instance();

onmessage = async (event) => {
  const { id, dot } = event.data;
  try {
    const viz = await vizReady;
    const result = viz.render(dot, { format: 'svg' });
    if (result.status === 'success') {
      postMessage({ id, kind: 'svg', body: result.output });
    } else {
      const lines = result.errors
        .filter((error) => error.level !== 'warning')
        .map((error) => error.message.replace(/\s+/g, ' ').trim());
      postMessage({ id, kind: 'errors', body: lines.join('\n') });
    }
  } catch (error) {
    postMessage({ id, kind: 'crash', body: String(error) || 'unknown error' });
  }
};
