import React, { useEffect } from 'react';
import confetti from 'canvas-confetti';
import { Award, CheckCircle2, XCircle, Clock, Check, X } from 'lucide-react';
import { useTimer } from '../context/TimerContext';

export const SessionSummaryModal: React.FC = () => {
  const { timerState, closeCompletedSummary } = useTimer();

  const summary = timerState.completedSummary;

  useEffect(() => {
    if (summary) {
      try {
        confetti({
          particleCount: 100,
          spread: 70,
          origin: { y: 0.6 }
        });
      } catch {
        // no-op if confetti fails
      }
    }
  }, [summary]);

  if (!summary) return null;

  const formatSeconds = (secs: number) => {
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    if (h > 0) return `${h}h ${m}m ${s}s`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
  };

  return (
    <div className="modal-overlay" style={{ zIndex: 110 }}>
      <div
        className="glass-panel modal-container animate-scale-up"
        style={{
          maxWidth: '620px',
          padding: 'clamp(18px, 4vw, 32px)',
          border: '1px solid var(--border-medium)',
          boxShadow: 'var(--shadow-lg), 0 0 40px rgba(16, 185, 129, 0.2)',
          gap: '24px',
          overflowY: 'auto'
        }}
      >
        {/* Celebration Header */}
        <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 25px rgba(16, 185, 129, 0.4)'
            }}
          >
            <Award size={36} color="#ffffff" />
          </div>

          <h2 style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: '8px' }}>
            ¡Sesión Completada!
          </h2>
          <p style={{ fontSize: '0.92rem', color: 'var(--text-secondary)' }}>
            Has completado con éxito <strong>"{summary.title}"</strong>
          </p>
        </div>

        {/* Metrics Grid */}
        <div className="summary-metrics-grid">
          <div
            style={{
              background: 'var(--bg-tertiary)',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              textAlign: 'center',
              border: '1px solid var(--border-subtle)'
            }}
          >
            <Clock size={18} color="var(--accent-primary)" style={{ margin: '0 auto 6px' }} />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Tiempo Total</div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '2px' }}>
              {formatSeconds(summary.totalActualSeconds)}
            </div>
          </div>

          <div
            style={{
              background: 'var(--bg-tertiary)',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              textAlign: 'center',
              border: '1px solid var(--border-subtle)'
            }}
          >
            <CheckCircle2 size={18} color="var(--accent-emerald)" style={{ margin: '0 auto 6px' }} />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Etapas Cumplidas</div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--accent-emerald)', marginTop: '2px' }}>
              {summary.completedStagesCount}
            </div>
          </div>

          <div
            style={{
              background: 'var(--bg-tertiary)',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              textAlign: 'center',
              border: '1px solid var(--border-subtle)'
            }}
          >
            <XCircle size={18} color="var(--accent-coral)" style={{ margin: '0 auto 6px' }} />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Etapas Saltadas</div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--accent-coral)', marginTop: '2px' }}>
              {summary.skippedStagesCount}
            </div>
          </div>
        </div>

        {/* Stage Records Breakdown */}
        <div>
          <h4 style={{ fontSize: '0.85rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '8px' }}>
            Desglose de Etapas
          </h4>
          <div
            style={{
              maxHeight: '180px',
              overflowY: 'auto',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px'
            }}
          >
            {summary.stageRecords.map((rec, i) => (
              <div
                key={rec.stageId || i}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-sm)',
                  background: 'var(--bg-tertiary)',
                  fontSize: '0.84rem'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {rec.isSkipped ? (
                    <X size={15} color="var(--accent-coral)" />
                  ) : (
                    <Check size={15} color="var(--accent-emerald)" />
                  )}
                  <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>
                    {rec.stageName}
                  </span>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.74rem' }}>
                    ({rec.activityName})
                  </span>
                </div>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                  {formatSeconds(rec.actualSeconds)} / {formatSeconds(rec.plannedSeconds)}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Action Button */}
        <button
          onClick={closeCompletedSummary}
          className="btn btn-primary"
          style={{ width: '100%', padding: '12px', fontSize: '1rem' }}
        >
          Aceptar y Guardar
        </button>
      </div>
    </div>
  );
};
