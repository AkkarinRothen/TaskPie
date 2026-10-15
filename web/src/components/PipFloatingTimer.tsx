import React from 'react';
import {
  Play,
  Pause,
  SkipForward,
  Square,
  Maximize2
} from 'lucide-react';
import type { TimerState } from '../types/models';

interface PipFloatingTimerProps {
  timerState: TimerState;
  onTogglePlayPause: () => void;
  onSkipStage: () => void;
  onAdvanceManual: () => void;
  onRestoreApp: () => void;
  onStopSession: () => void;
}

export const PipFloatingTimer: React.FC<PipFloatingTimerProps> = ({
  timerState,
  onTogglePlayPause,
  onSkipStage,
  onAdvanceManual,
  onRestoreApp,
  onStopSession
}) => {
  const currentStep = timerState.routineSteps[timerState.currentActivityIndex];
  const currentStage = currentStep?.stages[timerState.currentStageIndex];

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const isPaused = timerState.status === 'PAUSED';
  const isWaiting = timerState.status === 'WAITING_MANUAL_ADVANCE';
  const isRunning = timerState.status === 'RUNNING';

  const stageColor = currentStage?.colorHex || 'var(--accent-primary)';
  const totalStages = currentStep?.stages?.length || 1;
  const stageIndex = timerState.currentStageIndex + 1;
  const stageTitle = currentStage?.name || 'TaskPie';

  const stageTotalSeconds = currentStage?.durationSeconds || 1;
  const stageProgress = Math.min(
    100,
    Math.max(0, (timerState.elapsedSecondsInCurrentStage / stageTotalSeconds) * 100)
  );

  return (
    <div
      style={{
        width: '100vw',
        height: '100vh',
        boxSizing: 'border-box',
        background: 'var(--bg-primary, #090d16)',
        color: 'var(--text-primary, #f8fafc)',
        fontFamily: 'var(--font-body, system-ui, -apple-system, sans-serif)',
        padding: '10px 14px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        userSelect: 'none',
        overflow: 'hidden',
        border: '1px solid var(--border-subtle, rgba(255,255,255,0.1))',
        position: 'relative'
      }}
    >
      {/* Top Header: Dot + Stage Name + Index */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '8px',
          width: '100%'
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            minWidth: 0,
            flex: 1
          }}
        >
          <span
            style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: stageColor,
              flexShrink: 0,
              boxShadow: `0 0 6px ${stageColor}`
            }}
          />
          <span
            style={{
              fontSize: '0.74rem',
              fontWeight: 600,
              color: 'var(--text-secondary, #94a3b8)',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}
          >
            {currentStep?.totalIterations && currentStep.totalIterations > 1
              ? `[R${(currentStep.iterationIndex || 0) + 1}/${currentStep.totalIterations}] `
              : ''}
            {totalStages > 1 ? `${stageIndex}/${totalStages} • ` : ''}
            {stageTitle}
          </span>
        </div>

        {/* Small Status Badge for Mate */}
        {timerState.isMateModeEnabled && (
          <span
            style={{
              fontSize: '0.65rem',
              fontWeight: 700,
              padding: '2px 5px',
              borderRadius: '4px',
              background: 'rgba(16, 185, 129, 0.2)',
              color: '#34d399',
              border: '1px solid rgba(16, 185, 129, 0.4)'
            }}
            title="Modo Mate Activo"
          >
            🧉 {formatTime(timerState.mateRemainingSeconds)}
          </span>
        )}

        {/* Small Status Badge if Paused or Waiting */}
        {isPaused && (
          <span
            style={{
              fontSize: '0.65rem',
              fontWeight: 800,
              letterSpacing: '0.04em',
              padding: '2px 6px',
              borderRadius: '4px',
              background: 'rgba(245, 158, 11, 0.18)',
              color: 'var(--accent-amber, #f59e0b)',
              border: '1px solid rgba(245, 158, 11, 0.35)',
              textTransform: 'uppercase'
            }}
          >
            PAUSA
          </span>
        )}
        {isWaiting && (
          <span
            style={{
              fontSize: '0.65rem',
              fontWeight: 800,
              letterSpacing: '0.04em',
              padding: '2px 6px',
              borderRadius: '4px',
              background: 'rgba(16, 185, 129, 0.18)',
              color: 'var(--accent-emerald, #10b981)',
              border: '1px solid rgba(16, 185, 129, 0.35)',
              textTransform: 'uppercase'
            }}
          >
            SIGUIENTE
          </span>
        )}
      </div>

      {/* Center: Large Countdown */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flex: 1,
          margin: '2px 0'
        }}
      >
        <div
          style={{
            fontFamily: 'var(--font-mono, "JetBrains Mono", monospace)',
            fontSize: '2.5rem',
            fontWeight: 800,
            lineHeight: 1,
            letterSpacing: '-0.03em',
            color: isPaused
              ? 'var(--accent-amber, #f59e0b)'
              : isWaiting
              ? 'var(--accent-emerald, #10b981)'
              : 'var(--accent-primary, #6366f1)',
            textShadow: '0 2px 10px rgba(0,0,0,0.4)'
          }}
        >
          {isWaiting ? '00:00' : formatTime(timerState.remainingSeconds)}
        </div>
      </div>

      {/* Progress Bar */}
      <div
        style={{
          width: '100%',
          height: '4px',
          backgroundColor: 'var(--border-subtle, rgba(255,255,255,0.1))',
          borderRadius: '2px',
          overflow: 'hidden',
          marginBottom: '8px'
        }}
      >
        <div
          style={{
            width: `${isWaiting ? 100 : stageProgress}%`,
            height: '100%',
            backgroundColor: stageColor,
            transition: 'width 0.3s ease',
            borderRadius: '2px'
          }}
        />
      </div>

      {/* Bottom Controls Row */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '8px',
          paddingTop: '2px'
        }}
      >
        {/* Main Action: Play / Pause / Siguiente */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          {isWaiting ? (
            <button
              onClick={onAdvanceManual}
              title="Avanzar (N)"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                padding: '4px 10px',
                background: 'var(--accent-emerald, #10b981)',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                fontSize: '0.75rem',
                fontWeight: 700,
                cursor: 'pointer'
              }}
            >
              <Play size={12} fill="#ffffff" />
              <span>Siguiente</span>
            </button>
          ) : isRunning ? (
            <button
              onClick={onTogglePlayPause}
              title="Pausar (Espacio)"
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '30px',
                height: '30px',
                borderRadius: '8px',
                background: 'var(--bg-tertiary, #1e293b)',
                border: '1px solid var(--border-medium, rgba(255,255,255,0.15))',
                color: 'var(--text-primary, #f8fafc)',
                cursor: 'pointer'
              }}
            >
              <Pause size={14} />
            </button>
          ) : (
            <button
              onClick={onTogglePlayPause}
              title="Reanudar (Espacio)"
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '30px',
                height: '30px',
                borderRadius: '8px',
                background: 'var(--accent-primary, #6366f1)',
                border: 'none',
                color: '#ffffff',
                cursor: 'pointer'
              }}
            >
              <Play size={14} fill="#ffffff" />
            </button>
          )}

          <button
            onClick={onSkipStage}
            disabled={isWaiting}
            title="Saltar etapa (S)"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '28px',
              height: '28px',
              borderRadius: '6px',
              background: 'transparent',
              border: 'none',
              color: isWaiting ? 'var(--text-muted, #64748b)' : 'var(--text-secondary, #94a3b8)',
              cursor: isWaiting ? 'not-allowed' : 'pointer'
            }}
          >
            <SkipForward size={14} />
          </button>
        </div>

        {/* Secondary: Stop session & Restore / Focus tab */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <button
            onClick={onStopSession}
            title="Detener sesión"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '28px',
              height: '28px',
              borderRadius: '6px',
              background: 'transparent',
              border: 'none',
              color: 'var(--accent-coral, #f43f5e)',
              cursor: 'pointer'
            }}
          >
            <Square size={13} fill="currentColor" />
          </button>

          <button
            onClick={onRestoreApp}
            title="Volver a la aplicación completa"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '28px',
              height: '28px',
              borderRadius: '6px',
              background: 'var(--bg-tertiary, #1e293b)',
              border: '1px solid var(--border-subtle, rgba(255,255,255,0.1))',
              color: 'var(--text-primary, #f8fafc)',
              cursor: 'pointer'
            }}
          >
            <Maximize2 size={13} />
          </button>
        </div>
      </div>
    </div>
  );
};
