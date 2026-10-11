import React, { useState } from 'react';
import { Plus, Trash2, ArrowUp, ArrowDown, X, Save } from 'lucide-react';
import type { RoutineItem, RoutineStep } from '../types/models';
import { useData } from '../context/DataContext';

interface RoutineEditorModalProps {
  initialRoutine?: RoutineItem | null;
  onClose: () => void;
}

export const RoutineEditorModal: React.FC<RoutineEditorModalProps> = ({ initialRoutine, onClose }) => {
  const { activities, saveRoutine } = useData();

  const [title, setTitle] = useState(initialRoutine?.title || '');
  const [description, setDescription] = useState(initialRoutine?.description || '');
  const [steps, setSteps] = useState<RoutineStep[]>(
    initialRoutine?.steps || []
  );

  const [selectedActivityToAdd, setSelectedActivityToAdd] = useState<string>(
    activities[0]?.id || ''
  );

  const totalSeconds = steps.reduce(
    (acc, step) => acc + step.stages.reduce((sAcc, s) => sAcc + s.durationSeconds, 0),
    0
  );
  const totalMinutes = Math.round(totalSeconds / 60);

  const handleAddStep = () => {
    const act = activities.find((a) => a.id === selectedActivityToAdd);
    if (!act) return;

    const newStep: RoutineStep = {
      id: crypto.randomUUID ? crypto.randomUUID() : `rs_${Date.now()}_${steps.length + 1}`,
      activityId: act.id,
      activityTitle: act.title,
      stages: act.stages,
      autoAdvance: true
    };

    setSteps([...steps, newStep]);
  };

  const handleRemoveStep = (index: number) => {
    setSteps(steps.filter((_, i) => i !== index));
  };

  const handleMoveStep = (index: number, direction: 'UP' | 'DOWN') => {
    const targetIdx = direction === 'UP' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= steps.length) return;
    const copy = [...steps];
    const temp = copy[index];
    copy[index] = copy[targetIdx];
    copy[targetIdx] = temp;
    setSteps(copy);
  };

  const handleToggleAutoAdvance = (index: number) => {
    const copy = [...steps];
    copy[index] = { ...copy[index], autoAdvance: !copy[index].autoAdvance };
    setSteps(copy);
  };

  const handleSave = async () => {
    if (!title.trim()) {
      alert('Por favor ingresa un título para la rutina.');
      return;
    }
    if (steps.length === 0) {
      alert('Agrega al menos una actividad como paso de la rutina.');
      return;
    }

    const routineToSave: RoutineItem = {
      id: initialRoutine?.id || (crypto.randomUUID ? crypto.randomUUID() : `rout_${Date.now()}`),
      title: title.trim(),
      description: description.trim(),
      steps,
      isTemplate: initialRoutine?.isTemplate || false,
      createdAt: initialRoutine?.createdAt || Date.now()
    };

    await saveRoutine(routineToSave);
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
          maxWidth: '740px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          borderRadius: 'var(--radius-xl)',
          overflow: 'hidden',
          boxShadow: 'var(--shadow-lg)'
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
              {initialRoutine ? 'Editar Rutina' : 'Nueva Rutina'}
            </h3>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Encadena múltiples actividades en una secuencia fluida
            </p>
          </div>
          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={20} />
          </button>
        </div>

        {/* Form Body */}
        <div style={{ padding: '24px 28px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div>
            <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Título de la Rutina *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ej. Bloque Mañanero, Escritura + Descanso..."
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
              placeholder="Describe el objetivo de esta rutina..."
              style={{ marginTop: '6px', resize: 'none' }}
            />
          </div>

          {/* Add Activity Step Selector */}
          <div
            style={{
              background: 'var(--bg-tertiary)',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
              display: 'flex',
              flexDirection: 'column',
              gap: '10px'
            }}
          >
            <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Añadir Paso a la Rutina
            </label>
            <div style={{ display: 'flex', gap: '10px' }}>
              <select
                value={selectedActivityToAdd}
                onChange={(e) => setSelectedActivityToAdd(e.target.value)}
                style={{ flex: 1 }}
              >
                {activities.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.title} ({Math.round(a.stages.reduce((sum, s) => sum + s.durationSeconds, 0) / 60)} min)
                  </option>
                ))}
              </select>
              <button
                type="button"
                onClick={handleAddStep}
                className="btn btn-primary"
                style={{ gap: '6px' }}
              >
                <Plus size={16} />
                <span>Añadir</span>
              </button>
            </div>
          </div>

          {/* Steps List */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                Secuencia de Pasos ({steps.length})
              </label>
              <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                Duración total: {totalMinutes} min
              </span>
            </div>

            {steps.length === 0 ? (
              <div
                style={{
                  padding: '30px',
                  textAlign: 'center',
                  background: 'var(--bg-tertiary)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--text-muted)',
                  fontSize: '0.88rem'
                }}
              >
                No has añadido actividades todavía. Selecciona una arriba y pulsa "Añadir".
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {steps.map((st, idx) => {
                  const stepMinutes = Math.round(
                    st.stages.reduce((acc, s) => acc + s.durationSeconds, 0) / 60
                  );
                  return (
                    <div
                      key={st.id || idx}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '12px 16px',
                        borderRadius: 'var(--radius-md)',
                        background: 'var(--bg-tertiary)',
                        border: '1px solid var(--border-subtle)'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <span
                          style={{
                            width: '24px',
                            height: '24px',
                            borderRadius: '50%',
                            background: 'var(--border-medium)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '0.78rem',
                            fontWeight: 700,
                            color: 'var(--text-primary)'
                          }}
                        >
                          {idx + 1}
                        </span>
                        <div>
                          <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.92rem' }}>
                            {st.activityTitle}
                          </div>
                          <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>
                            {st.stages.length} etapas • {stepMinutes} min
                          </div>
                        </div>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        {/* Auto Advance toggle */}
                        <label
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            cursor: 'pointer',
                            fontSize: '0.78rem',
                            color: st.autoAdvance ? 'var(--text-secondary)' : 'var(--accent-amber)'
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={st.autoAdvance}
                            onChange={() => handleToggleAutoAdvance(idx)}
                            style={{ width: '14px', height: '14px' }}
                          />
                          <span>{st.autoAdvance ? 'Avance automático' : 'Pausa manual'}</span>
                        </label>

                        {/* Reorder & Delete */}
                        <div style={{ display: 'flex', gap: '2px' }}>
                          <button
                            type="button"
                            onClick={() => handleMoveStep(idx, 'UP')}
                            disabled={idx === 0}
                            className="btn-ghost btn-sm"
                            style={{ padding: '4px', opacity: idx === 0 ? 0.3 : 1 }}
                          >
                            <ArrowUp size={14} />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleMoveStep(idx, 'DOWN')}
                            disabled={idx === steps.length - 1}
                            className="btn-ghost btn-sm"
                            style={{ padding: '4px', opacity: idx === steps.length - 1 ? 0.3 : 1 }}
                          >
                            <ArrowDown size={14} />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleRemoveStep(idx)}
                            className="btn-ghost btn-sm"
                            style={{ padding: '4px' }}
                          >
                            <Trash2 size={14} color="#f43f5e" />
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
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
            <span>Guardar Rutina</span>
          </button>
        </div>
      </div>
    </div>
  );
};
