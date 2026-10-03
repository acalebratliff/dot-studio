'use strict';

// Called by JcefPreviewBrowser. Renders run one at a time in a worker; replies go back through
// window.dotStudioReply, which JcefPreviewBrowser injects once the page has loaded.
window.dotStudio = (function () {
  // The SVG keeps its viewBox and gets a CSS size of its actual size times `scale`, so it stays sharp at every zoom
  // and #graph's own scrollbars pan it. Export never reads this page's SVG, so zoom cannot change what is exported.
  const MIN_SCALE = 0.1;
  const MAX_SCALE = 8;
  const ZOOM_STEP = 1.25;
  // Ctrl+wheel zooms in proportion to the wheel distance, so a trackpad's many small deltas zoom smoothly: every 100
  // pixels of deltaY zoom by one ZOOM_STEP. A delta in lines or pages counts a third of a step per unit.
  const WHEEL_PIXELS_PER_STEP = 100;
  const WHEEL_PIXELS_PER_UNIT = WHEEL_PIXELS_PER_STEP / 3;

  // `fitted` holds until the user zooms: until then every render and window resize fits the graph again.
  // `fitPending` is a fit put off while #graph has no area (the preview is hidden), done once it has one again.
  const view = {
    scale: 1, fitted: true, fitPending: false, width: 0, height: 0, windowSize: '', reported: '', drag: null,
  };

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

  function shownSvg() {
    return element('graph').querySelector(':scope > svg');
  }

  // The actual size in CSS pixels: Graphviz writes width and height in points, which SVGLength converts.
  function measure(svg) {
    let width = svg.width.baseVal.value;
    let height = svg.height.baseVal.value;
    if (!(width > 0 && height > 0)) {
      const box = svg.viewBox.baseVal;
      width = box ? box.width : 0;
      height = box ? box.height : 0;
    }
    view.width = Math.max(width, 1);
    view.height = Math.max(height, 1);
  }

  // Fitting never enlarges past 100%: a three-node graph blown up to fill the preview helps nobody. The offset size
  // includes any scrollbars, which a fitted graph no longer needs. One pixel is kept spare so rounding never adds one.
  function fitArea() {
    const graph = element('graph');
    const style = getComputedStyle(graph);
    return {
      width: graph.offsetWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight) - 1,
      height: graph.offsetHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom) - 1,
    };
  }

  function hasArea() {
    const area = fitArea();
    return area.width > 0 && area.height > 0;
  }

  // Only meaningful while hasArea().
  function fitScale() {
    const area = fitArea();
    return Math.min(1, area.width / view.width, area.height / view.height);
  }

  // Zooming out stops at MIN_SCALE, or lower when the whole graph needs it to fit. Without an area there is nothing
  // to fit, so the scale on show is kept.
  function minScale() {
    return Math.min(MIN_SCALE, hasArea() ? fitScale() : view.scale);
  }

  function clampScale(scale) {
    const clamped = Math.min(MAX_SCALE, Math.max(minScale(), scale));
    // Repeated steps drift by a rounding error; 100% should stay exactly 100%.
    return Math.abs(clamped - 1) < 1e-9 ? 1 : clamped;
  }

  function applyScale(svg) {
    svg.style.width = view.width * view.scale + 'px';
    svg.style.height = view.height * view.scale + 'px';
  }

  function canPan() {
    const graph = element('graph');
    return graph.scrollWidth > graph.clientWidth || graph.scrollHeight > graph.clientHeight;
  }

  // Tells the plugin the zoom of the graph on show, so it can enable the toolbar actions. Unchanged states are not sent.
  function report() {
    document.body.classList.toggle('pannable', canPan());
    if (!hasArea()) return;
    const body = [view.scale, minScale(), MAX_SCALE, view.fitted ? 1 : 0].join('\n');
    if (body === view.reported || typeof window.dotStudioReply !== 'function') return;
    view.reported = body;
    reply(0, 'zoom', body);
  }

  // Zooms so that the graph point under (clientX, clientY) stays there, as far as scrolling allows.
  function zoomAround(scale, clientX, clientY) {
    const svg = shownSvg();
    if (svg === null) return;
    const graph = element('graph');
    const before = svg.getBoundingClientRect();
    const x = (clientX - before.left) / view.scale;
    const y = (clientY - before.top) / view.scale;
    view.scale = clampScale(scale);
    applyScale(svg);
    const after = svg.getBoundingClientRect();
    graph.scrollLeft += after.left + x * view.scale - clientX;
    graph.scrollTop += after.top + y * view.scale - clientY;
    report();
  }

  function zoomAroundCentre(scale, zoom = zoomAround) {
    const box = element('graph').getBoundingClientRect();
    zoom(scale, box.left + box.width / 2, box.top + box.height / 2);
  }

  function fit() {
    const svg = shownSvg();
    if (svg === null) return;
    view.fitted = true;
    view.fitPending = !hasArea();
    if (view.fitPending) return;
    view.scale = fitScale();
    applyScale(svg);
    report();
  }

  // A zoom the user asks for ends fitting, unless the scale cannot change (already at the limit, or a wheel delta
  // with no vertical part): then the graph stays fitted.
  function zoomByUser(scale, clientX, clientY) {
    if (clampScale(scale) === view.scale) return;
    view.fitted = false;
    zoomAround(scale, clientX, clientY);
  }

  function onWheel(event) {
    if (!event.ctrlKey && !event.metaKey) return;
    // Also keeps Chromium from zooming the whole page.
    event.preventDefault();
    if (shownSvg() === null) return;
    const pixels = event.deltaMode === WheelEvent.DOM_DELTA_PIXEL ? event.deltaY : event.deltaY * WHEEL_PIXELS_PER_UNIT;
    zoomByUser(view.scale * Math.pow(ZOOM_STEP, -pixels / WHEEL_PIXELS_PER_STEP), event.clientX, event.clientY);
  }

  function onPointerDown(event) {
    const graph = element('graph');
    if (event.button !== 0 || !canPan()) return;
    // A press on a scrollbar belongs to the scrollbar.
    const box = graph.getBoundingClientRect();
    if (event.clientX - box.left - graph.clientLeft >= graph.clientWidth ||
      event.clientY - box.top - graph.clientTop >= graph.clientHeight) return;
    view.drag = { pointer: event.pointerId, x: event.clientX, y: event.clientY, left: graph.scrollLeft, top: graph.scrollTop };
    graph.setPointerCapture(event.pointerId);
    document.body.classList.add('panning');
    event.preventDefault();
  }

  function onPointerMove(event) {
    const drag = view.drag;
    if (drag === null || drag.pointer !== event.pointerId) return;
    const graph = element('graph');
    graph.scrollLeft = drag.left - (event.clientX - drag.x);
    graph.scrollTop = drag.top - (event.clientY - drag.y);
  }

  function onPointerUp(event) {
    if (view.drag === null || view.drag.pointer !== event.pointerId) return;
    view.drag = null;
    document.body.classList.remove('panning');
  }

  // Runs whenever #graph changes size. Only a window resize (or a fit put off while hidden) refits, not the error
  // message appearing above the graph, so typing an error does not make the graph jump.
  function onGraphResize() {
    document.body.classList.toggle('pannable', canPan());
    const windowSize = window.innerWidth + 'x' + window.innerHeight;
    const resized = windowSize !== view.windowSize;
    view.windowSize = windowSize;
    if (shownSvg() === null || !hasArea() || !(resized || view.fitPending)) return;
    if (view.fitted) fit(); else zoomAroundCentre(view.scale);
  }

  document.addEventListener('DOMContentLoaded', () => {
    const graph = element('graph');
    graph.addEventListener('wheel', onWheel, { passive: false });
    graph.addEventListener('pointerdown', onPointerDown);
    graph.addEventListener('pointermove', onPointerMove);
    graph.addEventListener('pointerup', onPointerUp);
    graph.addEventListener('pointercancel', onPointerUp);
    new ResizeObserver(onGraphResize).observe(graph);
  });

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

    // A re-render keeps the zoom and scroll position; until the user zooms, it fits the new graph instead.
    showSvg(svg, notice) {
      const parsed = new DOMParser().parseFromString(svg, 'image/svg+xml');
      const graph = element('graph');
      const left = graph.scrollLeft;
      const top = graph.scrollTop;
      const shown = document.importNode(parsed.documentElement, true);
      element('message').hidden = true;
      element('notice').textContent = notice || '';
      element('notice').hidden = !notice;
      document.body.classList.remove('rendering');
      graph.replaceChildren(shown);
      measure(shown);
      if (view.fitted && !hasArea()) view.fitPending = true;
      else view.scale = view.fitted ? fitScale() : clampScale(view.scale);
      applyScale(shown);
      graph.scrollLeft = left;
      graph.scrollTop = top;
      report();
    },

    zoom(command) {
      if (shownSvg() === null) return;
      switch (command) {
        case 'in':
          zoomAroundCentre(view.scale * ZOOM_STEP, zoomByUser);
          break;
        case 'out':
          zoomAroundCentre(view.scale / ZOOM_STEP, zoomByUser);
          break;
        case 'actual':
          zoomAroundCentre(1, zoomByUser);
          break;
        case 'fit':
          fit();
          break;
      }
    },

    showMessage(text) {
      element('message').textContent = text;
      element('message').hidden = false;
      document.body.classList.remove('rendering');
    },
  };
})();
