import React from 'react';
import {
  Play,
  Pause,
  SkipForward,
  Maximize2,
  Volume2,
  VolumeX,
  Mic,
  MicOff,
  Square
} from 'lucide-react';
import { useTimer } from '../context/TimerContext';
import { PieChart } from './PieChart';

export const LiveTimerDock: React.FC = () => {
  const {
    timerState,
    pause,
    resume,
    skipCurrentStage,
    advanceManualNextActivity,
    stopSession,
    toggleVoice,
    toggleSound,
    setIsSessionModalOpen
  } = useTimer();

  if (timerState.status === 'IDLE') return null;

  const currentStep = timerState.routineSteps[timerState.currentActivityIndex];
  const currentStage = currentStep?.stages[timerState.currentStageIndex];

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const isWaitingManual = timerState.status === 'WAITING_MANUAL_ADVANCE';

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '20px',
        left: 'calc(280px + (100vw - 280px) / 2)',
        transform: 'translateX(-50%)',
        width: 'min(90%, 820px)',
        zIndex: 50,
        background: 'var(--bg-dock)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        border: '1px solid var(--border-medium)',
        borderRadius: 'var(--radius-xl)',
        boxShadow: 'var(--shadow-lg), var(--shadow-glow)',
        padding: '12px 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '20px',
        animation: 'fadeIn 0.3s ease'
      }}
    >
      {/* Left: Mini Donut & Current Stage info */}
      <div
        style={{ display: 'flex', alignItems: 'center', gap: '16px', cursor: 'pointer', flex: 1, minWidth: 0 }}
        onClick={() => setIsSessionModalOpen(true)}
      >
        <div style={{ flexShrink: 0 }}>
          {currentStep && (
            <PieChart
              stages={currentStep.stages}
              activeStageIndex={timerState.currentStageIndex}
              size={56}
              donut={true}
              innerRadiusRatio={0.55}
              interactive={false}
              centerContent={
                <span style={{ fontSize: '0.65rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  {timerState.currentStageIndex + 1}/{currentStep.stages.length}
                </span>
              }
            />
          )}
        </div>

        <div style={{ minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span
              style={{
                fontSize: '0.7rem',
                fontWeight: 700,
                color: isWaitingManual ? 'var(--accent-amber)' : 'var(--accent-primary)',
                textTransform: 'uppercase',
                letterSpacing: '0.04em'
              }}
            >
              {isWaitingManual ? 'Pausa Manual' : currentStep?.activityTitle || 'Sesión'}
            </span>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.72rem' }}>•</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Etapa {timerState.currentStageIndex + 1} de {currentStep?.stages.length || 1}
            </span>
          </div>

          <h4
            style={{
              fontSize: '1.05rem',
              color: 'var(--text-primary)',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}
          >
            {isWaitingManual
              ? 'Esperando confirmación para iniciar el siguiente bloque'
              : currentStage?.name || 'Etapa'}
          </h4>
        </div>
      </div>

      {/* Center: Digits countdown */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <div
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: '1.75rem',
            fontWeight: 700,
            color: timerState.status === 'RUNNING' ? 'var(--text-primary)' : 'var(--text-muted)',
            letterSpacing: '-0.02em',
            minWidth: '95px',
            textAlign: 'center'
          }}
        >
          {isWaitingManual ? '00:00' : formatTime(timerState.remainingSeconds)}
        </div>
      </div>

      {/* Right: Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        {/* Voice & Sound toggles */}
        <button
          onClick={toggleVoice}
          className="btn-ghost btn-sm btn-icon"
          title={timerState.isVoiceEnabled ? 'Desactivar voz (M)' : 'Activar voz (M)'}
          style={{ color: timerState.isVoiceEnabled ? 'var(--accent-primary)' : 'var(--text-muted)' }}
        >
          {timerState.isVoiceEnabled ? <Mic size={18} /> : <MicOff size={18} />}
        </button>

        <button
          onClick={toggleSound}
          className="btn-ghost btn-sm btn-icon"
          title={timerState.isSoundEnabled ? 'Silenciar alertas' : 'Activar alertas'}
          style={{ color: timerState.isSoundEnabled ? 'var(--accent-primary)' : 'var(--text-muted)' }}
        >
          {timerState.isSoundEnabled ? <Volume2 size={18} /> : <VolumeX size={18} />}
        </button>

        {isWaitingManual ? (
          <button
            onClick={advanceManualNextActivity}
            className="btn-primary btn-sm"
            style={{ gap: '6px', background: 'var(--accent-amber)' }}
            title="Avanzar al siguiente bloque (N)"
          >
            <Play size={15} fill="#ffffff" />
            <span>Siguiente</span>
          </button>
        ) : timerState.status === 'RUNNING' ? (
          <button
            onClick={pause}
            className="btn btn-secondary btn-icon"
            title="Pausar (Espacio)"
          >
            <Pause size={18} />
          </button>
        ) : (
          <button
            onClick={resume}
            className="btn-primary btn-icon"
            title="Reanudar (Espacio)"
          >
            <Play size={18} fill="#ffffff" />
          </button>
        )}

        <button
          onClick={skipCurrentStage}
          className="btn btn-ghost btn-icon"
          title="Saltar etapa (S)"
          disabled={isWaitingManual}
        >
          <SkipForward size={18} />
        </button>

        <button
          onClick={() => {
            if (confirm('¿Detener y salir de la sesión actual?')) {
              stopSession();
            }
          }}
          className="btn btn-ghost btn-icon"
          title="Detener sesión"
          style={{ color: 'var(--accent-coral)' }}
        >
          <Square size={16} fill="var(--accent-coral)" />
        </button>

        <button
          onClick={() => setIsSessionModalOpen(true)}
          className="btn-secondary btn-icon"
          title="Pantalla completa interactiva (F)"
          style={{ marginLeft: '4px' }}
        >
          <Maximize2 size={17} />
        </button>
      </div>
    </div>
  );
};
