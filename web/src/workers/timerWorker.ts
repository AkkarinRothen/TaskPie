// Web Worker for high-precision, background-safe ticking

let timerId: number | null = null;
let lastTickTime = 0;

self.onmessage = (event: MessageEvent<{ command: 'START' | 'PAUSE' | 'STOP' }>) => {
  const { command } = event.data;

  if (command === 'START') {
    if (timerId !== null) clearInterval(timerId);
    lastTickTime = performance.now();
    timerId = self.setInterval(() => {
      const now = performance.now();
      const delta = now - lastTickTime;
      // Enviar tick
      self.postMessage({ type: 'TICK', delta });
      lastTickTime = now;
    }, 1000);
  } else if (command === 'PAUSE' || command === 'STOP') {
    if (timerId !== null) {
      clearInterval(timerId);
      timerId = null;
    }
  }
};
