import React from 'react';
import { Play, Edit3, Trash2, Folder, Activity, Layers } from 'lucide-react';
import type { ProjectItem } from '../types/models';
import { useTimer } from '../context/TimerContext';
import { useData } from '../context/DataContext';

interface ProjectCardProps {
  project: ProjectItem;
  onEdit: (project: ProjectItem) => void;
}

export const ProjectCard: React.FC<ProjectCardProps> = ({ project, onEdit }) => {
  const { startProject } = useTimer();
  const { activities, routines, deleteProject } = useData();

  const linkedActivities = activities.filter((a) => project.activityIds.includes(a.id));
  const linkedRoutines = routines.filter((r) => project.routineIds.includes(r.id));

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
        minHeight: '240px'
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
            <span
              className="badge"
              style={{
                background: 'rgba(59, 130, 246, 0.15)',
                color: '#3b82f6',
                border: '1px solid rgba(59, 130, 246, 0.3)',
                marginBottom: '4px'
              }}
            >
              <Folder size={11} /> Proyecto Contenedor
            </span>
            <h3 style={{ fontSize: '1.15rem', color: 'var(--text-primary)', marginTop: '6px' }}>
              {project.title}
            </h3>
          </div>
        </div>

        {project.description && (
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
            {project.description}
          </p>
        )}

        {/* Linked elements tags */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '16px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.78rem',
              padding: '4px 10px',
              borderRadius: '6px',
              background: 'var(--bg-tertiary)',
              color: 'var(--text-secondary)',
              border: '1px solid var(--border-subtle)'
            }}
          >
            <Activity size={13} color="var(--accent-primary)" />
            <span>{linkedActivities.length} actividades</span>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.78rem',
              padding: '4px 10px',
              borderRadius: '6px',
              background: 'var(--bg-tertiary)',
              color: 'var(--text-secondary)',
              border: '1px solid var(--border-subtle)'
            }}
          >
            <Layers size={13} color="var(--accent-secondary)" />
            <span>{linkedRoutines.length} rutinas</span>
          </div>
        </div>
      </div>

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-end',
          marginTop: '20px',
          paddingTop: '14px',
          borderTop: '1px solid var(--border-subtle)',
          gap: '6px'
        }}
      >
        <button
          onClick={() => onEdit(project)}
          className="btn-ghost btn-sm"
          title="Editar proyecto"
        >
          <Edit3 size={15} />
        </button>
        <button
          onClick={() => {
            if (confirm(`¿Eliminar proyecto "${project.title}"?`)) {
              deleteProject(project.id);
            }
          }}
          className="btn-ghost btn-sm"
          title="Eliminar proyecto"
        >
          <Trash2 size={15} color="#f43f5e" />
        </button>
        <button
          onClick={() => startProject(project, activities, routines)}
          className="btn-primary btn-sm"
          style={{ gap: '6px' }}
        >
          <Play size={14} fill="#ffffff" />
          <span>Iniciar Todo</span>
        </button>
      </div>
    </div>
  );
};
