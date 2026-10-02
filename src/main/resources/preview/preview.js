'use strict';

// Called by JcefPreviewBrowser. Renders run one at a time in a worker; replies go back through
// window.dotStudioReply, which JcefPreviewBrowser injects once the page has loaded.
window.dotStudio = (function () {
  let worker = null;
  let busyId = null;

  function stopWorker() {
    if (worker !== null) worker.terminate();
    worker = null;
    busyId = null;
  }

  function reply(id, kind, body) {
    window.dotStudioReply(id + '\n' + kind + '\n' + body);
  }

  function startWorker() {
    worker = new Worker('render-worker.js');
    worker.onmessage = (event) => {
      const { id, kind, body } = event.data;
      if (id !== busyId) return;
      busyId = null;
      // After a crash (often a stack overflow) the Graphviz instance may be left broken, so start afresh.
      if (kind === 'crash') stopWorker();
      reply(id, kind, body);
    };
    worker.onerror = (event) => {
      const id = busyId;
      stopWorker();
      if (id !== null) reply(id, 'crash', event.message || 'the render worker stopped');
    };
  }

  function element(id) {
    return document.getElementById(id);
  }

  return {
    render(id, dot) {
      // A Graphviz layout cannot be interrupted, so a busy worker is replaced rather than queued behind.
      if (busyId !== null) stopWorker();
      if (worker === null) startWorker();
      busyId = id;
      worker.postMessage({ id, dot });
    },

    cancel(id) {
      if (busyId === id) stopWorker();
    },

    // Colours arrive as data from JcefPreviewBrowser; setting them through the CSSOM needs no inline styles under the CSP.
    setTheme(theme) {
      const style = document.documentElement.style;
      style.setProperty('--background', theme.background);
      style.setProperty('--foreground', theme.foreground);
      style.setProperty('--error-background', theme.errorBackground);
      style.setProperty('--error-border', theme.errorBorder);
      style.colorScheme = theme.colorScheme;
    },

    showRendering() {
      document.body.classList.add('rendering');
    },

    showSvg(svg) {
      const parsed = new DOMParser().parseFromString(svg, 'image/svg+xml');
      element('graph').replaceChildren(document.importNode(parsed.documentElement, true));
      element('message').hidden = true;
      document.body.classList.remove('rendering');
    },

    showMessage(text) {
      element('message').textContent = text;
      element('message').hidden = false;
      document.body.classList.remove('rendering');
    },
  };
})();
