import React, { useState } from 'react';
import { X, Save } from 'lucide-react';
import type { ProjectItem } from '../types/models';
import { useData } from '../context/DataContext';

interface ProjectEditorModalProps {
  initialProject?: ProjectItem | null;
  onClose: () => void;
}

export const ProjectEditorModal: React.FC<ProjectEditorModalProps> = ({ initialProject, onClose }) => {
  const { activities, routines, saveProject } = useData();

  const [title, setTitle] = useState(initialProject?.title || '');
  const [description, setDescription] = useState(initialProject?.description || '');
  const [selectedActivityIds, setSelectedActivityIds] = useState<string[]>(
    initialProject?.activityIds || []
  );
  const [selectedRoutineIds, setSelectedRoutineIds] = useState<string[]>(
    initialProject?.routineIds || []
  );

  const toggleActivity = (id: string) => {
    if (selectedActivityIds.includes(id)) {
      setSelectedActivityIds(selectedActivityIds.filter((item) => item !== id));
    } else {
      setSelectedActivityIds([...selectedActivityIds, id]);
    }
  };

  const toggleRoutine = (id: string) => {
    if (selectedRoutineIds.includes(id)) {
      setSelectedRoutineIds(selectedRoutineIds.filter((item) => item !== id));
    } else {
      setSelectedRoutineIds([...selectedRoutineIds, id]);
    }
  };

  const handleSave = async () => {
    if (!title.trim()) {
      alert('Por favor ingresa un título para el proyecto.');
      return;
    }

    const projectToSave: ProjectItem = {
      id: initialProject?.id || (crypto.randomUUID ? crypto.randomUUID() : `proj_${Date.now()}`),
      title: title.trim(),
      description: description.trim(),
      activityIds: selectedActivityIds,
      routineIds: selectedRoutineIds,
      createdAt: initialProject?.createdAt || Date.now()
    };

    await saveProject(projectToSave);
    onClose();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 100,
        background: 'rgba(9, 13, 22, 0.82)',
        backdropFilter: 'blur(16px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px'
      }}
    >
      <div
        className="glass-panel"
        style={{
          width: '100%',
          maxWidth: '680px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          borderRadius: 'var(--radius-xl)',
          overflow: 'hidden',
          boxShadow: 'var(--shadow-lg)'
        }}
      >
        <div
          style={{
            padding: '20px 28px',
            borderBottom: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div>
            <h3 style={{ fontSize: '1.3rem', color: 'var(--text-primary)' }}>
              {initialProject ? 'Editar Proyecto' : 'Nuevo Proyecto'}
            </h3>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Agrupa actividades y rutinas en un contenedor temático
            </p>
          </div>
          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={20} />
          </button>
        </div>

        <div style={{ padding: '24px 28px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div>
            <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Título del Proyecto *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ej. Escribir mi Novela, Lanzamiento de Producto..."
              style={{ marginTop: '6px' }}
            />
          </div>

          <div>
            <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Descripción (opcional)
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              placeholder="Describe el objetivo y alcance del proyecto..."
              style={{ marginTop: '6px', resize: 'none' }}
            />
          </div>

          {/* Activities Multi-select */}
          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              Incluir Actividades ({selectedActivityIds.length})
            </label>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '8px',
                marginTop: '8px',
                maxHeight: '160px',
                overflowY: 'auto'
              }}
            >
              {activities.map((a) => {
                const isSelected = selectedActivityIds.includes(a.id);
                return (
                  <div
                    key={a.id}
                    onClick={() => toggleActivity(a.id)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-sm)',
                      background: isSelected ? 'rgba(99, 102, 241, 0.15)' : 'var(--bg-tertiary)',
                      border: `1px solid ${isSelected ? 'var(--accent-primary)' : 'var(--border-subtle)'}`,
                      cursor: 'pointer',
                      fontSize: '0.84rem',
                      color: isSelected ? 'var(--text-primary)' : 'var(--text-secondary)'
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={isSelected}
                      readOnly
                      style={{ width: '14px', height: '14px' }}
                    />
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {a.title}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Routines Multi-select */}
          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              Incluir Rutinas ({selectedRoutineIds.length})
            </label>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '8px',
                marginTop: '8px',
                maxHeight: '160px',
                overflowY: 'auto'
              }}
            >
              {routines.map((r) => {
                const isSelected = selectedRoutineIds.includes(r.id);
                return (
                  <div
                    key={r.id}
                    onClick={() => toggleRoutine(r.id)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-sm)',
                      background: isSelected ? 'rgba(168, 85, 247, 0.15)' : 'var(--bg-tertiary)',
                      border: `1px solid ${isSelected ? 'var(--accent-secondary)' : 'var(--border-subtle)'}`,
                      cursor: 'pointer',
                      fontSize: '0.84rem',
                      color: isSelected ? 'var(--text-primary)' : 'var(--text-secondary)'
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={isSelected}
                      readOnly
                      style={{ width: '14px', height: '14px' }}
                    />
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {r.title}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <div
          style={{
            padding: '16px 28px',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '12px',
            background: 'rgba(0,0,0,0.1)'
          }}
        >
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Cancelar
          </button>
          <button type="button" onClick={handleSave} className="btn btn-primary" style={{ gap: '8px' }}>
            <Save size={16} />
            <span>Guardar Proyecto</span>
          </button>
        </div>
      </div>
    </div>
  );
};
