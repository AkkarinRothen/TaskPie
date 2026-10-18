import React from 'react';
import { Play, Edit3, Trash2, Clock, Sparkles } from 'lucide-react';
import type { ActivityItem } from '../types/models';
import { PieChart } from './PieChart';
import { useTimer } from '../context/TimerContext';
import { useData } from '../context/DataContext';

interface ActivityCardProps {
  activity: ActivityItem;
  onEdit: (activity: ActivityItem) => void;
}

export const ActivityCard: React.FC<ActivityCardProps> = ({ activity, onEdit }) => {
  const { startSingleActivity } = useTimer();
  const { deleteActivity } = useData();

  const totalSeconds = activity.stages.reduce((acc, s) => acc + s.durationSeconds, 0);
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
      {/* Top Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '12px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
              <span
                className="badge"
                style={{
                  background: 'rgba(99, 102, 241, 0.15)',
                  color: 'var(--accent-primary)',
                  border: '1px solid rgba(99, 102, 241, 0.3)'
                }}
              >
                {activity.category || 'General'}
              </span>
              {activity.tags && activity.tags.map((t) => (
                <span
                  key={t}
                  style={{
                    fontSize: '0.7rem',
                    padding: '2px 6px',
                    borderRadius: '4px',
                    background: 'rgba(255,255,255,0.06)',
                    color: 'var(--text-secondary)',
                    border: '1px solid var(--border-subtle)'
                  }}
                >
                  #{t}
                </span>
              ))}
              {activity.isTemplate && (
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
              {activity.title}
            </h3>
          </div>

          {/* Mini Pie Preview */}
          <div style={{ flexShrink: 0 }}>
            <PieChart
              stages={activity.stages}
              size={64}
              donut={true}
              innerRadiusRatio={0.55}
              interactive={false}
            />
          </div>
        </div>

        {activity.description && (
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
            {activity.description}
          </p>
        )}

        {/* Stages pills */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '14px' }}>
          {activity.stages.map((stage) => (
            <span
              key={stage.id}
              style={{
                fontSize: '0.72rem',
                padding: '3px 8px',
                borderRadius: '6px',
                background: 'var(--bg-tertiary)',
                color: 'var(--text-secondary)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
                border: '1px solid var(--border-subtle)'
              }}
            >
              <span
                style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  background: stage.colorHex || '#3B82F6'
                }}
              />
              <span>{stage.name}</span>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.68rem' }}>
                ({Math.round(stage.durationSeconds / 60)}m)
              </span>
            </span>
          ))}
        </div>
      </div>

      {/* Footer Info & Actions */}
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
          <span>{activity.stages.length} etapas</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <button
            onClick={() => onEdit(activity)}
            className="btn-ghost btn-sm"
            title="Editar actividad"
          >
            <Edit3 size={15} />
          </button>
          {!activity.isTemplate && (
            <button
              onClick={() => {
                if (confirm(`¿Eliminar actividad "${activity.title}"?`)) {
                  deleteActivity(activity.id);
                }
              }}
              className="btn-ghost btn-sm"
              title="Eliminar actividad"
            >
              <Trash2 size={15} color="#f43f5e" />
            </button>
          )}
          <button
            onClick={() => startSingleActivity(activity)}
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
