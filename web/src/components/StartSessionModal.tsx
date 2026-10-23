import React, { useState } from 'react';
import { Play, X, RotateCw, Coffee, Clock } from 'lucide-react';

interface StartSessionModalProps {
  isOpen: boolean;
  title: string;
  defaultRepeatCount?: number;
  defaultCycleBreakSeconds?: number;
  cycleDurationSeconds: number;
  onDismiss: () => void;
  onStart: (sessionName: string, repeatCount: number, cycleBreakSeconds: number) => void;
}

export const StartSessionModal: React.FC<StartSessionModalProps> = ({
  isOpen,
  title,
  defaultRepeatCount = 1,
  defaultCycleBreakSeconds = 0,
  cycleDurationSeconds,
  onDismiss,
  onStart
}) => {
  const [sessionName, setSessionName] = useState(title);
  const [repeatCount, setRepeatCount] = useState(Math.max(1, defaultRepeatCount));
  const [cycleBreakSeconds, setCycleBreakSeconds] = useState(defaultCycleBreakSeconds);

  if (!isOpen) return null;

  const totalSeconds = cycleDurationSeconds * repeatCount + (cycleBreakSeconds * Math.max(0, repeatCount - 1));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const durationText = hours > 0 ? `${hours}h ${minutes}m` : `${minutes} min`;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(0, 0, 0, 0.65)',
        backdropFilter: 'blur(10px)',
        padding: '16px'
      }}
      onClick={onDismiss}
    >
      <div
        className="glass-panel"
        style={{
          width: '100%',
          maxWidth: '480px',
          background: 'var(--bg-secondary)',
          border: '1px solid var(--border-glow)',
          borderRadius: 'var(--radius-xl)',
          padding: '28px',
          display: 'flex',
          flexDirection: 'column',
          gap: '20px',
          boxShadow: 'var(--shadow-xl)',
          animation: 'fadeIn 0.2s ease'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  padding: '2px 8px',
                  borderRadius: '12px',
                  background: 'rgba(99, 102, 241, 0.15)',
                  color: 'var(--accent-primary)',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}
              >
                <Clock size={12} /> {durationText}
              </span>
            </div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              Iniciar Sesión
            </h2>
          </div>
          <button
            onClick={onDismiss}
            className="btn-ghost btn-sm"
            style={{ borderRadius: '50%', padding: '6px' }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Session Name Field */}
        <div>
          <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
            Nombre para esta sesión (opcional)
          </label>
          <input
            type="text"
            className="input-field"
            value={sessionName}
            onChange={(e) => setSessionName(e.target.value)}
            placeholder="Ej: Tesis bloque 1, Capítulo 3..."
            style={{ width: '100%' }}
          />
        </div>

        {/* Repetitions Section */}
        <div
          style={{
            padding: '16px',
            borderRadius: 'var(--radius-lg)',
            background: 'var(--bg-tertiary)',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <RotateCw size={16} color="var(--accent-primary)" />
              <div>
                <div style={{ fontSize: '0.88rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  Repeticiones del Ciclo
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Ejecutar la secuencia completa varias veces
                </div>
              </div>
            </div>

            {/* Stepper Buttons */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                type="button"
                onClick={() => setRepeatCount((prev) => Math.max(1, prev - 1))}
                className="btn btn-secondary btn-sm"
                style={{ width: '32px', height: '32px', padding: 0, justifyContent: 'center' }}
                disabled={repeatCount <= 1}
              >
                -
              </button>
              <span
                style={{
                  minWidth: '38px',
                  textAlign: 'center',
                  fontWeight: 800,
                  fontSize: '1rem',
                  color: repeatCount > 1 ? 'var(--accent-primary)' : 'var(--text-primary)'
                }}
              >
                x{repeatCount}
              </span>
              <button
                type="button"
                onClick={() => setRepeatCount((prev) => Math.min(99, prev + 1))}
                className="btn btn-secondary btn-sm"
                style={{ width: '32px', height: '32px', padding: 0, justifyContent: 'center' }}
              >
                +
              </button>
            </div>
          </div>

          {/* Cycle Break Selector (visible if repeatCount > 1) */}
          {repeatCount > 1 && (
            <div style={{ paddingTop: '8px', borderTop: '1px solid var(--border-subtle)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                <Coffee size={14} color="var(--accent-amber)" />
                <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  Descanso entre cada ciclo:
                </span>
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {[
                  { label: 'Sin descanso', sec: 0 },
                  { label: '1 min', sec: 60 },
                  { label: '2 min', sec: 120 },
                  { label: '3 min', sec: 180 },
                  { label: '5 min', sec: 300 }
                ].map((opt) => (
                  <button
                    key={opt.sec}
                    type="button"
                    onClick={() => setCycleBreakSeconds(opt.sec)}
                    className={`btn btn-xs ${cycleBreakSeconds === opt.sec ? 'btn-primary' : 'btn-secondary'}`}
                    style={{
                      fontSize: '0.75rem',
                      padding: '4px 10px',
                      borderRadius: '8px',
                      borderColor: cycleBreakSeconds === opt.sec ? 'var(--accent-primary)' : undefined
                    }}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '10px', marginTop: '6px' }}>
          <button
            type="button"
            onClick={onDismiss}
            className="btn btn-secondary btn-md"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={() => onStart(sessionName, repeatCount, cycleBreakSeconds)}
            className="btn btn-primary btn-md"
            style={{ gap: '8px', padding: '10px 24px', fontWeight: 700 }}
          >
            <Play size={16} fill="#ffffff" />
            <span>Comenzar</span>
          </button>
        </div>
      </div>
    </div>
  );
};
