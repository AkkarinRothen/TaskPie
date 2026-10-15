import React, { useState } from 'react';
import {
  Play,
  Pause,
  SkipForward,
  Minimize2,
  PictureInPicture2,
  Volume2,
  VolumeX,
  Mic,
  MicOff,
  Square,
  ArrowRight
} from 'lucide-react';
import { useTimer } from '../context/TimerContext';
import { PieChart } from './PieChart';

export const ActiveSessionModal: React.FC = () => {
  const {
    timerState,
    pause,
    resume,
    skipCurrentStage,
    advanceManualNextActivity,
    stopSession,
    toggleVoice,
    toggleSound,
    toggleMateMode,
    setMateInterval,
    resetMateCountdown,
    isSessionModalOpen,
    setIsSessionModalOpen,
    isPipActive,
    isPipSupported,
    togglePip
  } = useTimer();

  const [showMateMenu, setShowMateMenu] = useState(false);

  if (!isSessionModalOpen || timerState.status === 'IDLE') return null;

  const currentStep = timerState.routineSteps[timerState.currentActivityIndex];
  const currentStage = currentStep?.stages[timerState.currentStageIndex];

  // Calcular siguiente etapa
  let nextStageTitle: string | null = null;
  let nextStageMinutes: number | null = null;
  if (currentStep) {
    if (timerState.currentStageIndex + 1 < currentStep.stages.length) {
      const nextS = currentStep.stages[timerState.currentStageIndex + 1];
      nextStageTitle = nextS.name;
      nextStageMinutes = Math.round(nextS.durationSeconds / 60);
    } else if (timerState.currentActivityIndex + 1 < timerState.routineSteps.length) {
      const nextAct = timerState.routineSteps[timerState.currentActivityIndex + 1];
      const nextS = nextAct.stages[0];
      if (nextS) {
        nextStageTitle = `${nextAct.activityTitle} ➔ ${nextS.name}`;
        nextStageMinutes = Math.round(nextS.durationSeconds / 60);
      }
    }
  }

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const isWaitingManual = timerState.status === 'WAITING_MANUAL_ADVANCE';

  // Progreso de etapa
  const stageTotalSeconds = currentStage?.durationSeconds || 1;
  const stageProgress = Math.min(
    1,
    Math.max(0, timerState.elapsedSecondsInCurrentStage / stageTotalSeconds)
  );

  // Progreso de toda la sesión
  const totalSessionProgress = timerState.totalPlannedSecondsSession > 0
    ? Math.min(1, timerState.totalElapsedSecondsSession / timerState.totalPlannedSecondsSession)
    : 0;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 100,
        background: 'rgba(9, 13, 22, 0.94)',
        backdropFilter: 'blur(28px)',
        WebkitBackdropFilter: 'blur(28px)',
        display: 'flex',
        flexDirection: 'column',
        animation: 'fadeIn 0.25s ease'
      }}
    >
      {/* Top Header Bar */}
      <header
        style={{
          padding: '24px 36px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid var(--border-subtle)'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div
            style={{
              padding: '6px 14px',
              borderRadius: 'var(--radius-full)',
              background: 'rgba(99, 102, 241, 0.15)',
              border: '1px solid rgba(99, 102, 241, 0.3)',
              color: 'var(--accent-primary)',
              fontSize: '0.8rem',
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.05em'
            }}
          >
            {timerState.contextType === 'ACTIVITY'
              ? 'Actividad en curso'
              : timerState.contextType === 'ROUTINE'
              ? 'Rutina en curso'
              : 'Proyecto en curso'}
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {timerState.sessionTitle}
          </h2>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {/* Mate Companion Button & Popover */}
          <div style={{ position: 'relative' }}>
            <button
              onClick={() => setShowMateMenu(!showMateMenu)}
              className={`btn btn-sm ${timerState.isMateModeEnabled ? 'btn-primary' : 'btn-secondary'}`}
              style={{
                background: timerState.isMateModeEnabled ? 'rgba(16, 185, 129, 0.2)' : undefined,
                borderColor: timerState.isMateModeEnabled ? '#10b981' : undefined,
                color: timerState.isMateModeEnabled ? '#34d399' : 'var(--text-muted)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
              title="Modo Compañero de Mate"
            >
              <span>🧉</span>
              <span>{timerState.isMateModeEnabled ? `Mate (${timerState.mateIntervalMinutes}m)` : 'Mate OFF'}</span>
            </button>

            {showMateMenu && (
              <div
                className="glass-panel"
                style={{
                  position: 'absolute',
                  top: 'calc(100% + 8px)',
                  right: 0,
                  zIndex: 120,
                  width: '240px',
                  padding: '14px',
                  borderRadius: 'var(--radius-lg)',
                  background: 'var(--bg-surface)',
                  border: '1px solid var(--border-medium)',
                  boxShadow: 'var(--shadow-xl)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '10px'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                  <div style={{ fontWeight: 700, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span>🧉</span> Modo Mate
                  </div>
                  <button
                    onClick={() => {
                      toggleMateMode();
                      setShowMateMenu(false);
                    }}
                    className={`btn btn-xs ${timerState.isMateModeEnabled ? 'btn-danger' : 'btn-primary'}`}
                    style={{ fontSize: '0.72rem' }}
                  >
                    {timerState.isMateModeEnabled ? 'Desactivar' : 'Activar'}
                  </button>
                </div>

                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Frecuencia de aviso
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '6px' }}>
                  {[5, 8, 10, 15].map((m) => (
                    <button
                      key={m}
                      onClick={() => {
                        setMateInterval(m);
                        if (!timerState.isMateModeEnabled) toggleMateMode();
                      }}
                      className={`btn btn-xs ${timerState.mateIntervalMinutes === m ? 'btn-primary' : 'btn-secondary'}`}
                      style={{
                        padding: '4px 0',
                        fontSize: '0.75rem',
                        fontWeight: timerState.mateIntervalMinutes === m ? 700 : 500
                      }}
                    >
                      {m}m
                    </button>
                  ))}
                </div>

                {timerState.isMateModeEnabled && (
                  <button
                    onClick={() => {
                      resetMateCountdown();
                      setShowMateMenu(false);
                    }}
                    className="btn btn-secondary btn-xs"
                    style={{ marginTop: '2px', justifyContent: 'center', color: '#34d399', borderColor: 'rgba(16, 185, 129, 0.4)' }}
                  >
                    Reiniciar contador (Cebé uno)
                  </button>
                )}
              </div>
            )}
          </div>

          <button
            onClick={toggleVoice}
            className="btn btn-secondary btn-sm"
            style={{ color: timerState.isVoiceEnabled ? 'var(--accent-primary)' : 'var(--text-muted)' }}
          >
            {timerState.isVoiceEnabled ? <Mic size={16} /> : <MicOff size={16} />}
            <span>{timerState.isVoiceEnabled ? 'Voz Activada (M)' : 'Voz Silenciada (M)'}</span>
          </button>

          <button
            onClick={toggleSound}
            className="btn btn-secondary btn-sm"
            style={{ color: timerState.isSoundEnabled ? 'var(--accent-primary)' : 'var(--text-muted)' }}
          >
            {timerState.isSoundEnabled ? <Volume2 size={16} /> : <VolumeX size={16} />}
            <span>{timerState.isSoundEnabled ? 'Alertas ON' : 'Alertas OFF'}</span>
          </button>

          {isPipSupported && (
            <button
              onClick={togglePip}
              className={`btn ${isPipActive ? 'btn-primary' : 'btn-secondary'} btn-sm`}
              title={isPipActive ? 'Cerrar ventana flotante (P)' : 'Ventana flotante / PiP (P)'}
              style={{
                color: isPipActive ? 'var(--accent-primary)' : 'inherit',
                borderColor: isPipActive ? 'var(--accent-primary)' : undefined,
                background: isPipActive ? 'rgba(99, 102, 241, 0.16)' : undefined,
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <PictureInPicture2 size={16} />
              <span>{isPipActive ? 'PiP Activo' : 'PiP (P)'}</span>
            </button>
          )}

          <button
            onClick={() => setIsSessionModalOpen(false)}
            className="btn-ghost btn-icon"
            title="Minimizar al dock (Esc)"
          >
            <Minimize2 size={20} />
          </button>
        </div>
      </header>

      {/* Main Center Area */}
      <main
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px 40px',
          maxWidth: '1100px',
          margin: '0 auto',
          width: '100%',
          gap: '24px'
        }}
      >
        {/* Mate Alert Banner (when recently triggered within 12s) */}
        {timerState.isMateModeEnabled && Date.now() - timerState.lastMateAlertTime < 12000 && (
          <div
            className="glass-panel"
            style={{
              maxWidth: '680px',
              width: '100%',
              padding: '12px 20px',
              borderRadius: 'var(--radius-lg)',
              background: 'rgba(16, 185, 129, 0.15)',
              border: '1px solid rgba(16, 185, 129, 0.45)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              animation: 'fadeIn 0.25s ease'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
              <span style={{ fontSize: '1.8rem' }}>🧉</span>
              <div>
                <div style={{ fontWeight: 800, color: '#34d399', fontSize: '0.95rem' }}>
                  ¡Momento de mate!
                </div>
                <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                  Que no se te enfríe. Tomate un matecito antes de seguir enfocado.
                </div>
              </div>
            </div>
            <button
              onClick={resetMateCountdown}
              className="btn btn-sm"
              style={{ background: '#10b981', color: '#fff', fontWeight: 700 }}
            >
              Cebado
            </button>
          </div>
        )}

        {/* Giant Donut Pie Chart Center */}
        <div style={{ position: 'relative', display: 'flex', justifyContent: 'center' }}>
          {currentStep && (
            <PieChart
              stages={currentStep.stages}
              activeStageIndex={timerState.currentStageIndex}
              activeStageProgress={stageProgress}
              size={360}
              donut={true}
              innerRadiusRatio={0.72}
              interactive={true}
              centerContent={
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
                  <span
                    style={{
                      fontSize: '0.85rem',
                      fontWeight: 700,
                      color: 'var(--text-muted)',
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em'
                    }}
                  >
                    {isWaitingManual ? 'En Espera' : timerState.status === 'PAUSED' ? 'En Pausa' : 'Tiempo Restante'}
                  </span>
                  <div
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: '3.6rem',
                      fontWeight: 800,
                      color: timerState.status === 'RUNNING' ? 'var(--text-primary)' : 'var(--text-muted)',
                      lineHeight: 1,
                      letterSpacing: '-0.03em'
                    }}
                  >
                    {isWaitingManual ? '00:00' : formatTime(timerState.remainingSeconds)}
                  </div>
                  <span
                    style={{
                      fontSize: '0.85rem',
                      fontWeight: 600,
                      color: 'var(--text-secondary)',
                      marginTop: '4px'
                    }}
                  >
                    Etapa {timerState.currentStageIndex + 1} de {currentStep.stages.length}
                  </span>
                </div>
              }
            />
          )}
        </div>

        {/* Mate Companion Widget Pill if enabled */}
        {timerState.isMateModeEnabled && (
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '12px',
              padding: '6px 16px',
              borderRadius: '20px',
              background: 'rgba(16, 185, 129, 0.12)',
              border: '1px solid rgba(16, 185, 129, 0.3)',
              marginTop: '-10px',
              marginBottom: '-6px'
            }}
          >
            <span style={{ fontSize: '1rem' }}>🧉</span>
            <span style={{ fontSize: '0.84rem', fontWeight: 700, color: '#34d399' }}>
              Mate en {formatTime(timerState.mateRemainingSeconds)}
            </span>
            <button
              onClick={resetMateCountdown}
              className="btn-ghost"
              style={{
                padding: '3px 10px',
                borderRadius: '12px',
                fontSize: '0.75rem',
                fontWeight: 700,
                color: '#6ee7b7',
                background: 'rgba(16, 185, 129, 0.25)',
                border: 'none',
                cursor: 'pointer'
              }}
              title="Reiniciar contador de mate si te acabas de cebar uno"
            >
              Cebé uno
            </button>
          </div>
        )}

        {/* Current Stage Information Card */}
        <div
          className="glass-panel"
          style={{
            width: '100%',
            maxWidth: '680px',
            padding: '24px 30px',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '10px',
            border: `1px solid ${currentStage?.colorHex ? currentStage.colorHex + '40' : 'var(--border-subtle)'}`,
            boxShadow: `0 8px 30px ${currentStage?.colorHex ? currentStage.colorHex + '20' : 'rgba(0,0,0,0.3)'}`
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span
              style={{
                width: '10px',
                height: '10px',
                borderRadius: '50%',
                background: currentStage?.colorHex || '#3b82f6'
              }}
            />
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>
              {currentStep?.activityTitle}
            </span>
            {currentStep?.totalIterations && currentStep.totalIterations > 1 && (
              <>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>•</span>
                <span
                  style={{
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    color: 'var(--accent-secondary)',
                    background: 'rgba(139, 92, 246, 0.15)',
                    padding: '2px 8px',
                    borderRadius: 'var(--radius-full)',
                    border: '1px solid rgba(139, 92, 246, 0.3)'
                  }}
                >
                  {currentStep.blockTitle ? `${currentStep.blockTitle} • ` : ''}Ronda{' '}
                  {(currentStep.iterationIndex || 0) + 1} de {currentStep.totalIterations}
                </span>
              </>
            )}
          </div>

          <h3 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {isWaitingManual
              ? 'Pausa completada: ¿Listo para el siguiente bloque?'
              : currentStage?.name || 'Etapa'}
          </h3>

          {currentStage?.notes && !isWaitingManual && (
            <p
              style={{
                fontSize: '0.98rem',
                color: 'var(--text-secondary)',
                maxWidth: '560px',
                lineHeight: 1.5,
                background: 'rgba(255, 255, 255, 0.03)',
                padding: '10px 16px',
                borderRadius: 'var(--radius-md)',
                marginTop: '4px'
              }}
            >
              💡 {currentStage.notes}
            </p>
          )}

          {/* Next stage hint */}
          {nextStageTitle && !isWaitingManual && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '0.84rem',
                color: 'var(--text-muted)',
                marginTop: '6px'
              }}
            >
              <ArrowRight size={14} />
              <span>A continuación:</span>
              <strong style={{ color: 'var(--text-primary)' }}>{nextStageTitle}</strong>
              {nextStageMinutes && <span>({nextStageMinutes} min)</span>}
            </div>
          )}
        </div>

        {/* Action Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          {isWaitingManual ? (
            <button
              onClick={advanceManualNextActivity}
              className="btn btn-primary"
              style={{ padding: '14px 28px', fontSize: '1.1rem', background: 'var(--accent-amber)' }}
            >
              <Play size={20} fill="#ffffff" />
              <span>Comenzar Siguiente Bloque (N)</span>
            </button>
          ) : timerState.status === 'RUNNING' ? (
            <button
              onClick={pause}
              className="btn btn-secondary"
              style={{ padding: '14px 28px', fontSize: '1.05rem', minWidth: '150px' }}
            >
              <Pause size={20} />
              <span>Pausar (Espacio)</span>
            </button>
          ) : (
            <button
              onClick={resume}
              className="btn btn-primary"
              style={{ padding: '14px 28px', fontSize: '1.05rem', minWidth: '150px' }}
            >
              <Play size={20} fill="#ffffff" />
              <span>Reanudar (Espacio)</span>
            </button>
          )}

          <button
            onClick={skipCurrentStage}
            className="btn btn-secondary"
            style={{ padding: '14px 22px', fontSize: '0.98rem' }}
            disabled={isWaitingManual}
            title="Saltar etapa actual (S)"
          >
            <SkipForward size={18} />
            <span>Saltar Etapa (S)</span>
          </button>

          <button
            onClick={() => {
              if (confirm('¿Finalizar y descartar la sesión actual?')) {
                stopSession();
              }
            }}
            className="btn btn-danger"
            style={{ padding: '14px 20px', fontSize: '0.95rem' }}
          >
            <Square size={16} />
            <span>Detener</span>
          </button>
        </div>

        {/* Global Session Linear Progress Bar */}
        <div style={{ width: '100%', maxWidth: '680px' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              fontSize: '0.78rem',
              color: 'var(--text-muted)',
              marginBottom: '6px'
            }}
          >
            <span>Progreso Total de la Sesión</span>
            <span>{Math.round(totalSessionProgress * 100)}%</span>
          </div>
          <div
            style={{
              width: '100%',
              height: '8px',
              borderRadius: 'var(--radius-full)',
              background: 'var(--bg-tertiary)',
              overflow: 'hidden'
            }}
          >
            <div
              style={{
                width: `${totalSessionProgress * 100}%`,
                height: '100%',
                background: 'var(--gradient-brand)',
                borderRadius: 'var(--radius-full)',
                transition: 'width 0.3s ease'
              }}
            />
          </div>
        </div>
      </main>

      {/* Keyboard shortcuts bottom bar */}
      <footer
        style={{
          padding: '16px 36px',
          borderTop: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '24px',
          fontSize: '0.8rem',
          color: 'var(--text-muted)'
        }}
      >
        <div>
          <kbd style={{ background: 'var(--bg-tertiary)', padding: '2px 6px', borderRadius: '4px' }}>Espacio</kbd> Pausar/Reanudar
        </div>
        <div>
          <kbd style={{ background: 'var(--bg-tertiary)', padding: '2px 6px', borderRadius: '4px' }}>S</kbd> Saltar Etapa
        </div>
        <div>
          <kbd style={{ background: 'var(--bg-tertiary)', padding: '2px 6px', borderRadius: '4px' }}>N</kbd> Siguiente Bloque
        </div>
        <div>
          <kbd style={{ background: 'var(--bg-tertiary)', padding: '2px 6px', borderRadius: '4px' }}>M</kbd> Voz TTS
        </div>
        <div>
          <kbd style={{ background: 'var(--bg-tertiary)', padding: '2px 6px', borderRadius: '4px' }}>Esc</kbd> Minimizar
        </div>
      </footer>
    </div>
  );
};
