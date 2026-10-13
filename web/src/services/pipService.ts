import type { TimerState } from '../types/models';

export interface PipCallbacks {
  onPlay: () => void;
  onPause: () => void;
  onSkip: () => void;
  onClose: () => void;
}

export class PipService {
  private static pipWindow: Window | null = null;
  private static fallbackVideo: HTMLVideoElement | null = null;
  private static fallbackCanvas: HTMLCanvasElement | null = null;
  private static fallbackAnimFrame: number | null = null;
  private static callbacks: PipCallbacks | null = null;

  public static isDocumentPipSupported(): boolean {
    return typeof window !== 'undefined' && 'documentPictureInPicture' in window;
  }

  public static isVideoPipSupported(): boolean {
    return (
      typeof document !== 'undefined' &&
      'pictureInPictureEnabled' in document &&
      document.pictureInPictureEnabled
    );
  }

  public static isPipSupported(): boolean {
    return this.isDocumentPipSupported() || this.isVideoPipSupported();
  }

  public static isPipActive(): boolean {
    if (this.pipWindow && !this.pipWindow.closed) return true;
    if (typeof document !== 'undefined' && document.pictureInPictureElement) return true;
    return false;
  }

  public static getPipWindow(): Window | null {
    if (this.pipWindow && !this.pipWindow.closed) {
      return this.pipWindow;
    }
    return null;
  }

  /**
   * Abre la ventana flotante Document Picture-in-Picture (Chromium).
   */
  public static async requestDocumentPip(
    width = 340,
    height = 190,
    onClose?: () => void
  ): Promise<Window | null> {
    if (!this.isDocumentPipSupported()) return null;

    try {
      // Si ya hay una abierta, ciérrala antes
      if (this.pipWindow && !this.pipWindow.closed) {
        this.pipWindow.close();
      }

      const dPip = (window as unknown as {
        documentPictureInPicture: {
          requestWindow: (options: { width: number; height: number }) => Promise<Window>;
        };
      }).documentPictureInPicture;

      const pipWin = await dPip.requestWindow({ width, height });
      this.pipWindow = pipWin;

      // Configurar documento en pipWindow
      pipWin.document.title = 'TaskPie • Temporizador';

      // Copiar hojas de estilos, fuentes y favicon del documento original
      this.copyHeadElements(pipWin);

      // Sincronizar tema y propiedades básicas del body
      const rootTheme = document.documentElement.getAttribute('data-theme') || '';
      if (rootTheme) {
        pipWin.document.documentElement.setAttribute('data-theme', rootTheme);
      }
      pipWin.document.body.style.margin = '0';
      pipWin.document.body.style.padding = '0';
      pipWin.document.body.style.background = 'var(--bg-primary, #090d16)';
      pipWin.document.body.style.overflow = 'hidden';

      // Escuchar cierre de la ventana
      pipWin.addEventListener('pagehide', () => {
        this.pipWindow = null;
        if (onClose) onClose();
      });

      return pipWin;
    } catch (err) {
      console.warn('No se pudo abrir Document Picture-in-Picture:', err);
      return null;
    }
  }

  /**
   * Copia todas las hojas de estilo, fuentes, favicon y etiquetas <style> a la ventana de PiP.
   */
  private static copyHeadElements(targetWindow: Window) {
    try {
      const headElements = document.head.querySelectorAll(
        'link[rel="stylesheet"], link[rel="preconnect"], link[rel="icon"], style'
      );
      headElements.forEach((node) => {
        targetWindow.document.head.appendChild(node.cloneNode(true));
      });

      // Estilo de seguridad para asegurar dimensiones y fondo
      const baseStyle = targetWindow.document.createElement('style');
      baseStyle.textContent = `
        html, body {
          margin: 0;
          padding: 0;
          width: 100%;
          height: 100%;
          overflow: hidden;
          background: var(--bg-primary, #090d16);
          color: var(--text-primary, #f8fafc);
          font-family: var(--font-body, system-ui, sans-serif);
        }
      `;
      targetWindow.document.head.appendChild(baseStyle);
    } catch (e) {
      console.warn('Error copiando elementos de cabecera al PiP:', e);
    }
  }

  /**
   * Fallback: Abre Video PiP mediante un canvas animado (para navegadores como Firefox/Safari).
   */
  public static async requestVideoPip(
    getTimerState: () => TimerState,
    callbacks: PipCallbacks
  ): Promise<boolean> {
    if (!this.isVideoPipSupported()) return false;

    try {
      this.callbacks = callbacks;

      if (!this.fallbackCanvas) {
        this.fallbackCanvas = document.createElement('canvas');
        this.fallbackCanvas.width = 360;
        this.fallbackCanvas.height = 200;
      }

      if (!this.fallbackVideo) {
        this.fallbackVideo = document.createElement('video');
        this.fallbackVideo.muted = true;
        this.fallbackVideo.playsInline = true;
        this.fallbackVideo.style.position = 'fixed';
        this.fallbackVideo.style.top = '-9999px';
        this.fallbackVideo.style.left = '-9999px';
        this.fallbackVideo.style.opacity = '0';
        this.fallbackVideo.style.pointerEvents = 'none';
        document.body.appendChild(this.fallbackVideo);

        this.fallbackVideo.addEventListener('leavepictureinpicture', () => {
          this.stopFallbackLoop();
          if (this.callbacks?.onClose) {
            this.callbacks.onClose();
          }
        });
      }

      // Dibujar canvas inicial
      this.renderFallbackCanvas(getTimerState());

      // Conectar canvas a video mediante MediaStream
      const stream = this.fallbackCanvas.captureStream(10);
      this.fallbackVideo.srcObject = stream;
      await this.fallbackVideo.play();

      // Solicitar Picture-in-Picture
      await this.fallbackVideo.requestPictureInPicture();

      // Configurar controles multimedia en MediaSession
      if ('mediaSession' in navigator) {
        navigator.mediaSession.setActionHandler('play', () => {
          this.callbacks?.onPlay();
        });
        navigator.mediaSession.setActionHandler('pause', () => {
          this.callbacks?.onPause();
        });
        navigator.mediaSession.setActionHandler('nexttrack', () => {
          this.callbacks?.onSkip();
        });
      }

      // Iniciar bucle de renderizado periódico para mantener el canvas actualizado
      this.startFallbackLoop(getTimerState);

      return true;
    } catch (err) {
      console.warn('Error iniciando Video PiP fallback:', err);
      return false;
    }
  }

  private static startFallbackLoop(getTimerState: () => TimerState) {
    this.stopFallbackLoop();
    const update = () => {
      this.renderFallbackCanvas(getTimerState());
      if (document.pictureInPictureElement) {
        this.fallbackAnimFrame = window.setTimeout(update, 500);
      }
    };
    update();
  }

  private static stopFallbackLoop() {
    if (this.fallbackAnimFrame !== null) {
      clearTimeout(this.fallbackAnimFrame);
      this.fallbackAnimFrame = null;
    }
  }

  private static renderFallbackCanvas(state: TimerState) {
    if (!this.fallbackCanvas) return;
    const ctx = this.fallbackCanvas.getContext('2d');
    if (!ctx) return;

    const width = this.fallbackCanvas.width;
    const height = this.fallbackCanvas.height;

    // Fondo
    ctx.fillStyle = '#090d16';
    ctx.fillRect(0, 0, width, height);

    const currentStep = state.routineSteps[state.currentActivityIndex];
    const currentStage = currentStep?.stages[state.currentStageIndex];
    const stageColor = currentStage?.colorHex || '#6366f1';
    const isPaused = state.status === 'PAUSED';
    const isWaiting = state.status === 'WAITING_MANUAL_ADVANCE';

    // Cabecera: Punto de color + Nombre de etapa
    ctx.fillStyle = stageColor;
    ctx.beginPath();
    ctx.arc(24, 28, 6, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#94a3b8';
    ctx.font = 'bold 15px sans-serif';
    const stageName = currentStage?.name || 'TaskPie';
    ctx.fillText(stageName.slice(0, 28), 38, 33);

    // Cuenta regresiva MM:SS
    const m = Math.floor(state.remainingSeconds / 60);
    const s = state.remainingSeconds % 60;
    const timeStr = isWaiting
      ? '00:00'
      : `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;

    ctx.fillStyle = isPaused ? '#f59e0b' : isWaiting ? '#10b981' : '#6366f1';
    ctx.font = 'bold 64px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(timeStr, width / 2, 115);

    // Etiqueta de estado
    if (isPaused || isWaiting) {
      ctx.fillStyle = isPaused ? '#f59e0b' : '#10b981';
      ctx.font = 'bold 13px sans-serif';
      ctx.fillText(isPaused ? 'EN PAUSA' : 'SIGUIENTE ETAPA', width / 2, 140);
    }

    // Barra de progreso
    const stageTotal = currentStage?.durationSeconds || 1;
    const progress = Math.min(
      1,
      Math.max(0, state.elapsedSecondsInCurrentStage / stageTotal)
    );

    ctx.fillStyle = 'rgba(255, 255, 255, 0.12)';
    ctx.fillRect(24, 165, width - 48, 8);

    ctx.fillStyle = stageColor;
    ctx.fillRect(24, 165, (width - 48) * (isWaiting ? 1 : progress), 8);
    ctx.textAlign = 'start';
  }

  /**
   * Cierra cualquier ventana de PiP activa (Document PiP o Video PiP).
   */
  public static closePip() {
    if (this.pipWindow && !this.pipWindow.closed) {
      this.pipWindow.close();
      this.pipWindow = null;
    }
    if (typeof document !== 'undefined' && document.pictureInPictureElement) {
      document.exitPictureInPicture().catch(() => {});
    }
    this.stopFallbackLoop();
  }
}
