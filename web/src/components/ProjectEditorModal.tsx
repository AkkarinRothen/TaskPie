import React, { useState } from 'react';
import {
  X,
  Save,
  Plus,
  ArrowUp,
  ArrowDown,
  Copy,
  Trash2,
  Layers,
  Clock
} from 'lucide-react';
import type { ProjectItem } from '../types/models';
import { calculateRoutineTotalSeconds } from '../types/models';
import { useData } from '../context/DataContext';

interface ProjectEditorModalProps {
  initialProject?: ProjectItem | null;
  onClose: () => void;
}

export const ProjectEditorModal: React.FC<ProjectEditorModalProps> = ({
  initialProject,
  onClose
}) => {
  const { activities, routines, saveProject } = useData();

  const [title, setTitle] = useState(initialProject?.title || '');
  const [description, setDescription] = useState(initialProject?.description || '');
  const [selectedActivityIds, setSelectedActivityIds] = useState<string[]>(
    initialProject?.activityIds || []
  );
  const [routineSequence, setRoutineSequence] = useState<string[]>(
    initialProject?.routineIds || []
  );

  const [selectedRoutineToAdd, setSelectedRoutineToAdd] = useState<string>(
    routines[0]?.id || ''
  );

  // Calcular duración total acumulada de todas las rutinas en la secuencia
  const totalRoutinesSeconds = routineSequence.reduce((acc, rId) => {
    const rout = routines.find((r) => r.id === rId);
    return acc + (rout ? calculateRoutineTotalSeconds(rout.steps) : 0);
  }, 0);
  const totalRoutinesMinutes = Math.round(totalRoutinesSeconds / 60);

  const handleAddRoutine = () => {
    if (!selectedRoutineToAdd) return;
    setRoutineSequence([...routineSequence, selectedRoutineToAdd]);
  };

  const handleRemoveRoutine = (index: number) => {
    setRoutineSequence(routineSequence.filter((_, i) => i !== index));
  };

  const handleDuplicateRoutine = (index: number) => {
    const copy = [...routineSequence];
    copy.splice(index + 1, 0, copy[index]);
    setRoutineSequence(copy);
  };

  const handleMoveRoutine = (index: number, direction: 'UP' | 'DOWN') => {
    const targetIdx = direction === 'UP' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= routineSequence.length) return;
    const copy = [...routineSequence];
    const temp = copy[index];
    copy[index] = copy[targetIdx];
    copy[targetIdx] = temp;
    setRoutineSequence(copy);
  };

  const toggleActivity = (id: string) => {
    if (selectedActivityIds.includes(id)) {
      setSelectedActivityIds(selectedActivityIds.filter((item) => item !== id));
    } else {
      setSelectedActivityIds([...selectedActivityIds, id]);
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
      routineIds: routineSequence,
      createdAt: initialProject?.createdAt || Date.now()
    };

    await saveProject(projectToSave);
    onClose();
  };

  return (
    <div className="modal-overlay">
      <div
        className="glass-panel modal-container"
        style={{
          maxWidth: '720px'
        }}
      >
        {/* Header */}
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
              Secuencia ordenada de rutinas con repetición libre
            </p>
          </div>
          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <div
          style={{
            padding: '24px 28px',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: '20px'
          }}
        >
          <div>
            <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Título del Proyecto *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ej. Sprint de Programación, Jornada de Estudio..."
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
              placeholder="Describe el objetivo general del proyecto..."
              style={{ marginTop: '6px', resize: 'none' }}
            />
          </div>

          {/* Secuencia Ordenada de Rutinas */}
          <div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '10px'
              }}
            >
              <label
                style={{
                  fontSize: '0.82rem',
                  fontWeight: 700,
                  color: 'var(--text-secondary)',
                  textTransform: 'uppercase',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
              >
                <Layers size={14} color="var(--accent-secondary)" />
                <span>Secuencia de Rutinas ({routineSequence.length})</span>
              </label>

              {routineSequence.length > 0 && (
                <span
                  style={{
                    fontSize: '0.82rem',
                    fontWeight: 700,
                    color: 'var(--accent-primary)',
                    background: 'rgba(99, 102, 241, 0.12)',
                    padding: '3px 8px',
                    borderRadius: '6px',
                    border: '1px solid rgba(99, 102, 241, 0.25)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px'
                  }}
                >
                  <Clock size={13} />
                  <span>{totalRoutinesMinutes} min total</span>
                </span>
              )}
            </div>

            {/* Lista de rutinas en la secuencia */}
            {routineSequence.length === 0 ? (
              <div
                style={{
                  padding: '24px',
                  textAlign: 'center',
                  background: 'var(--bg-tertiary)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--text-muted)',
                  fontSize: '0.86rem',
                  border: '1px dashed var(--border-medium)'
                }}
              >
                Aún no has añadido rutinas a este proyecto. Elige una rutina abajo y pulsa "+ Añadir a la Secuencia".
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '12px' }}>
                {routineSequence.map((rId, idx) => {
                  const rout = routines.find((r) => r.id === rId);
                  const routMins = rout
                    ? Math.round(calculateRoutineTotalSeconds(rout.steps) / 60)
                    : 0;

                  return (
                    <div
                      key={`${rId}_${idx}`}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px 14px',
                        borderRadius: 'var(--radius-md)',
                        background: 'var(--bg-tertiary)',
                        border: '1px solid var(--border-subtle)',
                        gap: '12px'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0, flex: 1 }}>
                        <span
                          style={{
                            fontFamily: 'var(--font-mono)',
                            fontSize: '0.82rem',
                            fontWeight: 700,
                            color: 'var(--accent-secondary)',
                            background: 'rgba(168, 85, 247, 0.12)',
                            padding: '2px 6px',
                            borderRadius: '4px'
                          }}
                        >
                          #{idx + 1}
                        </span>
                        <div style={{ minWidth: 0 }}>
                          <span
                            style={{
                              fontSize: '0.9rem',
                              fontWeight: 600,
                              color: 'var(--text-primary)',
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              display: 'block'
                            }}
                          >
                            {rout?.title || 'Rutina desconocida'}
                          </span>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            {routMins} min • {rout?.steps?.length || 0} pasos
                          </span>
                        </div>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        {/* Subir */}
                        <button
                          type="button"
                          onClick={() => handleMoveRoutine(idx, 'UP')}
                          disabled={idx === 0}
                          className="btn-ghost btn-sm btn-icon"
                          title="Mover arriba"
                        >
                          <ArrowUp size={14} />
                        </button>

                        {/* Bajar */}
                        <button
                          type="button"
                          onClick={() => handleMoveRoutine(idx, 'DOWN')}
                          disabled={idx === routineSequence.length - 1}
                          className="btn-ghost btn-sm btn-icon"
                          title="Mover abajo"
                        >
                          <ArrowDown size={14} />
                        </button>

                        {/* Duplicar (añadir otra instancia para repetición) */}
                        <button
                          type="button"
                          onClick={() => handleDuplicateRoutine(idx)}
                          className="btn-ghost btn-sm btn-icon"
                          title="Duplicar rutina en la secuencia"
                          style={{ color: 'var(--accent-primary)' }}
                        >
                          <Copy size={14} />
                        </button>

                        {/* Eliminar instancia */}
                        <button
                          type="button"
                          onClick={() => handleRemoveRoutine(idx)}
                          className="btn-ghost btn-sm btn-icon"
                          title="Quitar de la secuencia"
                          style={{ color: 'var(--accent-coral)' }}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Selector para añadir rutina a la secuencia */}
            {routines.length > 0 && (
              <div
                style={{
                  display: 'flex',
                  gap: '10px',
                  background: 'rgba(0,0,0,0.18)',
                  padding: '10px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)',
                  alignItems: 'center'
                }}
              >
                <select
                  value={selectedRoutineToAdd}
                  onChange={(e) => setSelectedRoutineToAdd(e.target.value)}
                  style={{ flex: 1, fontSize: '0.86rem' }}
                >
                  {routines.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.title} ({Math.round(calculateRoutineTotalSeconds(r.steps) / 60)} min)
                    </option>
                  ))}
                </select>

                <button
                  type="button"
                  onClick={handleAddRoutine}
                  className="btn btn-secondary btn-sm"
                  style={{ gap: '6px', whiteSpace: 'nowrap' }}
                >
                  <Plus size={15} />
                  <span>+ Añadir a Secuencia</span>
                </button>
              </div>
            )}
          </div>

          {/* Actividades adicionales individuales (opcional) */}
          {activities.length > 0 && (
            <div>
              <label
                style={{
                  fontSize: '0.82rem',
                  fontWeight: 700,
                  color: 'var(--text-secondary)',
                  textTransform: 'uppercase',
                  marginBottom: '8px',
                  display: 'block'
                }}
              >
                Actividades Adicionales ({selectedActivityIds.length})
              </label>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                  gap: '8px',
                  maxHeight: '140px',
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
                        fontSize: '0.82rem',
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
          )}
        </div>

        {/* Footer */}
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
