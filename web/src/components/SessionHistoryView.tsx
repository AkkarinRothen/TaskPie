import React, { useState } from 'react';
import { History, Clock, CheckCircle2, XCircle, Trash2, Award, ChevronDown, ChevronUp } from 'lucide-react';
import { useData } from '../context/DataContext';

export const SessionHistoryView: React.FC = () => {
  const { summaries, deleteSummary } = useData();
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const formatSeconds = (secs: number) => {
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    if (h > 0) return `${h}h ${m}m ${s}s`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
  };

  const formatDate = (timestamp: number) => {
    const d = new Date(timestamp);
    return d.toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (summaries.length === 0) {
    return (
      <div
        className="glass-panel"
        style={{
          padding: '60px 40px',
          textAlign: 'center',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '16px'
        }}
      >
        <div
          style={{
            width: '60px',
            height: '60px',
            borderRadius: '50%',
            background: 'var(--bg-tertiary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <History size={30} color="var(--text-muted)" />
        </div>
        <h3 style={{ fontSize: '1.25rem', color: 'var(--text-primary)' }}>Sin sesiones registradas aún</h3>
        <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', maxWidth: '400px' }}>
          Inicia una actividad o rutina y, al finalizar, tus métricas de tiempo y etapas cumplidas aparecerán aquí.
        </p>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {summaries.map((sum) => {
        const isExpanded = expandedId === sum.id;
        return (
          <div
            key={sum.id}
            className="glass-panel"
            style={{
              padding: '20px 24px',
              borderRadius: 'var(--radius-lg)',
              transition: 'all var(--transition-fast)'
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                cursor: 'pointer'
              }}
              onClick={() => setExpandedId(isExpanded ? null : sum.id)}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div
                  style={{
                    width: '42px',
                    height: '42px',
                    borderRadius: '12px',
                    background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(6, 182, 212, 0.2) 100%)',
                    border: '1px solid rgba(16, 185, 129, 0.3)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0
                  }}
                >
                  <Award size={22} color="var(--accent-emerald)" />
                </div>

                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span
                      className="badge"
                      style={{
                        background: 'rgba(99, 102, 241, 0.15)',
                        color: 'var(--accent-primary)',
                        fontSize: '0.7rem'
                      }}
                    >
                      {sum.contextType}
                    </span>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                      {formatDate(sum.completedAt)}
                    </span>
                  </div>
                  <h4 style={{ fontSize: '1.1rem', color: 'var(--text-primary)', marginTop: '4px' }}>
                    {sum.title}
                  </h4>
                </div>
              </div>

              {/* Right Summary Badges */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  <Clock size={15} color="var(--accent-primary)" />
                  <span>{formatSeconds(sum.totalActualSeconds)}</span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', color: 'var(--accent-emerald)' }}>
                  <CheckCircle2 size={15} />
                  <span>{sum.completedStagesCount} etapas</span>
                </div>

                {sum.skippedStagesCount > 0 && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', color: 'var(--accent-coral)' }}>
                    <XCircle size={15} />
                    <span>{sum.skippedStagesCount} saltadas</span>
                  </div>
                )}

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    if (confirm(`¿Eliminar registro de "${sum.title}"?`)) {
                      deleteSummary(sum.id);
                    }
                  }}
                  className="btn-ghost btn-sm"
                  title="Eliminar registro"
                >
                  <Trash2 size={15} color="#f43f5e" />
                </button>

                <div style={{ color: 'var(--text-muted)' }}>
                  {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                </div>
              </div>
            </div>

            {/* Expanded stage breakdown */}
            {isExpanded && (
              <div
                style={{
                  marginTop: '18px',
                  paddingTop: '16px',
                  borderTop: '1px solid var(--border-subtle)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px'
                }}
              >
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Desglose por Etapas
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {sum.stageRecords.map((r, i) => (
                    <div
                      key={r.stageId || i}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '6px 12px',
                        borderRadius: '6px',
                        background: 'var(--bg-tertiary)',
                        fontSize: '0.82rem'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        {r.isSkipped ? (
                          <XCircle size={14} color="var(--accent-coral)" />
                        ) : (
                          <CheckCircle2 size={14} color="var(--accent-emerald)" />
                        )}
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.stageName}</span>
                        <span style={{ color: 'var(--text-muted)', fontSize: '0.72rem' }}>({r.activityName})</span>
                      </div>
                      <span style={{ color: 'var(--text-secondary)' }}>
                        {formatSeconds(r.actualSeconds)} / {formatSeconds(r.plannedSeconds)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};
