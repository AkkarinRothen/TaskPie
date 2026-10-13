import React from 'react';
import { Play, Edit3, Trash2, Clock, Sparkles, Layers } from 'lucide-react';
import type { RoutineItem } from '../types/models';
import { useTimer } from '../context/TimerContext';
import { useData } from '../context/DataContext';

import { calculateRoutineTotalSeconds } from '../types/models';

interface RoutineCardProps {
  routine: RoutineItem;
  onEdit: (routine: RoutineItem) => void;
}

export const RoutineCard: React.FC<RoutineCardProps> = ({ routine, onEdit }) => {
  const { startRoutine } = useTimer();
  const { deleteRoutine } = useData();

  const totalSeconds = calculateRoutineTotalSeconds(routine.steps);
  const totalStages = routine.steps.reduce((acc, step) => acc + step.stages.length, 0);

  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const formattedDuration = hours > 0 ? `${hours}h ${minutes}m` : `${minutes} min`;

  return (
    <div
      className="glass-panel"
      style={{
        padding: '22px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        position: 'relative',
        transition: 'transform var(--transition-normal), box-shadow var(--transition-normal), border-color var(--transition-normal)',
        minHeight: '260px'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-3px)';
        e.currentTarget.style.borderColor = 'var(--border-glow)';
        e.currentTarget.style.boxShadow = 'var(--shadow-lg)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.borderColor = 'var(--border-subtle)';
        e.currentTarget.style.boxShadow = 'none';
      }}
    >
      <div>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '12px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
              <span
                className="badge"
                style={{
                  background: 'rgba(168, 85, 247, 0.15)',
                  color: 'var(--accent-secondary)',
                  border: '1px solid rgba(168, 85, 247, 0.3)'
                }}
              >
                <Layers size={11} /> Rutina Secuencial
              </span>
              {routine.isTemplate && (
                <span
                  className="badge"
                  style={{
                    background: 'rgba(16, 185, 129, 0.15)',
                    color: 'var(--accent-emerald)',
                    border: '1px solid rgba(16, 185, 129, 0.3)'
                  }}
                >
                  <Sparkles size={11} /> Plantilla
                </span>
              )}
            </div>
            <h3 style={{ fontSize: '1.15rem', color: 'var(--text-primary)', marginTop: '6px' }}>
              {routine.title}
            </h3>
          </div>
        </div>

        {routine.description && (
          <p
            style={{
              fontSize: '0.84rem',
              color: 'var(--text-secondary)',
              marginTop: '8px',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden'
            }}
          >
            {routine.description}
          </p>
        )}

        {/* Steps sequence */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '16px' }}>
          {routine.steps.map((step, idx) => (
            <div
              key={step.id || idx}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '0.8rem',
                color: 'var(--text-secondary)',
                background: 'var(--bg-tertiary)',
                padding: '6px 10px',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)'
              }}
            >
              <span
                style={{
                  width: '18px',
                  height: '18px',
                  borderRadius: '50%',
                  background: 'var(--border-medium)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  color: 'var(--text-primary)'
                }}
              >
                {idx + 1}
              </span>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{step.activityTitle}</span>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.72rem' }}>
                ({step.stages.length} etapas)
              </span>
              {!step.autoAdvance && (
                <span
                  style={{
                    marginLeft: 'auto',
                    fontSize: '0.68rem',
                    color: 'var(--accent-amber)',
                    background: 'rgba(245, 158, 11, 0.1)',
                    padding: '2px 6px',
                    borderRadius: '4px'
                  }}
                >
                  Pausa manual
                </span>
              )}
            </div>
          ))}
        </div>
      </div>

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginTop: '20px',
          paddingTop: '14px',
          borderTop: '1px solid var(--border-subtle)'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.82rem' }}>
          <Clock size={15} />
          <span>{formattedDuration}</span>
          <span style={{ margin: '0 4px' }}>•</span>
          <span>{routine.steps.length} bloques ({totalStages} etapas)</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <button
            onClick={() => onEdit(routine)}
            className="btn-ghost btn-sm"
            title="Editar rutina"
          >
            <Edit3 size={15} />
          </button>
          {!routine.isTemplate && (
            <button
              onClick={() => {
                if (confirm(`¿Eliminar rutina "${routine.title}"?`)) {
                  deleteRoutine(routine.id);
                }
              }}
              className="btn-ghost btn-sm"
              title="Eliminar rutina"
            >
              <Trash2 size={15} color="#f43f5e" />
            </button>
          )}
          <button
            onClick={() => startRoutine(routine)}
            className="btn-primary btn-sm"
            style={{ gap: '6px' }}
          >
            <Play size={14} fill="#ffffff" />
            <span>Iniciar</span>
          </button>
        </div>
      </div>
    </div>
  );
};
