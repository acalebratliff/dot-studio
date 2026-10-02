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

  // Chromium silently returns an empty "data:," past 32767 px a side or 268M px in all, and a canvas near the area
  // limit takes about 1 GB and many seconds to encode, so the area cap stays well inside it.
  const MAX_CANVAS_SIDE = 32767;
  const MAX_CANVAS_AREA = 50000000;
  const MAX_PNG_DATA_URL_LENGTH = 16 * 1024 * 1024;

  function canvasFits(width, height) {
    return width <= MAX_CANVAS_SIDE && height <= MAX_CANVAS_SIDE &&
      width * height <= MAX_CANVAS_AREA;
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

    // The SVG comes from our own origin (export-<id>.svg), so the canvas is not tainted and toDataURL works under
    // the CSP without blob: or data: image sources. Failures reply with a code that the plugin maps to a message.
    rasterise(id, scale) {
      const image = new Image();
      image.onload = () => {
        const width = Math.ceil(image.naturalWidth * scale);
        const height = Math.ceil(image.naturalHeight * scale);
        if (!canvasFits(width, height)) {
          const fitsAt1x = scale > 1 && canvasFits(Math.ceil(image.naturalWidth), Math.ceil(image.naturalHeight));
          reply(id, 'png-error', (fitsAt1x ? 'too-large-scale' : 'too-large') + '\n' + width + '\u00d7' + height);
          return;
        }
        try {
          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const context = canvas.getContext('2d');
          context.scale(scale, scale);
          context.drawImage(image, 0, 0);
          const url = canvas.toDataURL('image/png');
          // A detailed graph can compress badly; a reply this long would be slow to copy across the JSQuery bridge.
          if (url.length > MAX_PNG_DATA_URL_LENGTH) {
            reply(id, 'png-error', 'too-large\n' + width + '\u00d7' + height);
          } else {
            reply(id, 'png', url.substring(url.indexOf(',') + 1));
          }
        } catch (error) {
          reply(id, 'png-error', 'draw\n' + String(error));
        }
      };
      image.onerror = () => reply(id, 'png-error', 'image\n');
      image.src = 'export-' + id + '.svg';
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
