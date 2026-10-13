import React, { useState } from 'react';
import {
  Plus,
  Trash2,
  ArrowUp,
  ArrowDown,
  X,
  Save,
  Layers,
  Repeat,
  Clock
} from 'lucide-react';
import type { RoutineBlock, RoutineItem, RoutineStep } from '../types/models';
import {
  stepsToBlocks,
  blocksToSteps,
  calculateRoutineTotalSeconds
} from '../types/models';
import { useData } from '../context/DataContext';

interface RoutineEditorModalProps {
  initialRoutine?: RoutineItem | null;
  onClose: () => void;
}

export const RoutineEditorModal: React.FC<RoutineEditorModalProps> = ({
  initialRoutine,
  onClose
}) => {
  const { activities, saveRoutine } = useData();

  const [title, setTitle] = useState(initialRoutine?.title || '');
  const [description, setDescription] = useState(initialRoutine?.description || '');
  const [blocks, setBlocks] = useState<RoutineBlock[]>(() =>
    stepsToBlocks(initialRoutine?.steps || [])
  );

  const [selectedActivityToAdd, setSelectedActivityToAdd] = useState<string>(
    activities[0]?.id || ''
  );

  // Calcula la duración total en segundos tomando en cuenta los multiplicadores de repetición
  const totalSeconds = calculateRoutineTotalSeconds(blocksToSteps(blocks));
  const totalMinutes = Math.round(totalSeconds / 60);

  // Añadir una actividad individual como paso
  const handleAddSingleActivity = () => {
    const act = activities.find((a) => a.id === selectedActivityToAdd);
    if (!act) return;

    const newStep: RoutineStep = {
      id: crypto.randomUUID ? crypto.randomUUID() : `rs_${Date.now()}_${Math.random()}`,
      activityId: act.id,
      activityTitle: act.title,
      stages: act.stages,
      autoAdvance: true
    };

    const newBlock: RoutineBlock = {
      id: `block_${Date.now()}_${Math.random()}`,
      title: '',
      repeatCount: 1,
      steps: [newStep]
    };

    setBlocks([...blocks, newBlock]);
  };

  // Crear un nuevo Bloque contenedor repetitivo
  const handleCreateBlock = () => {
    const act = activities.find((a) => a.id === selectedActivityToAdd) || activities[0];
    const initialSteps: RoutineStep[] = act
      ? [
          {
            id: crypto.randomUUID ? crypto.randomUUID() : `rs_${Date.now()}_${Math.random()}`,
            activityId: act.id,
            activityTitle: act.title,
            stages: act.stages,
            autoAdvance: true
          }
        ]
      : [];

    const newBlock: RoutineBlock = {
      id: `block_${Date.now()}_${Math.random()}`,
      title: `Bloque ${blocks.length + 1}`,
      repeatCount: 2,
      steps: initialSteps
    };

    setBlocks([...blocks, newBlock]);
  };

  // Reordenar bloques completos
  const handleMoveBlock = (index: number, direction: 'UP' | 'DOWN') => {
    const targetIdx = direction === 'UP' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= blocks.length) return;
    const copy = [...blocks];
    const temp = copy[index];
    copy[index] = copy[targetIdx];
    copy[targetIdx] = temp;
    setBlocks(copy);
  };

  // Eliminar un bloque completo
  const handleRemoveBlock = (index: number) => {
    setBlocks(blocks.filter((_, i) => i !== index));
  };

  // Modificar repeticiones de un bloque
  const handleUpdateBlockRepeats = (blockIndex: number, delta: number) => {
    setBlocks(
      blocks.map((b, i) => {
        if (i !== blockIndex) return b;
        const next = Math.max(1, Math.min(99, (b.repeatCount || 1) + delta));
        return { ...b, repeatCount: next };
      })
    );
  };

  // Modificar título de un bloque
  const handleUpdateBlockTitle = (blockIndex: number, newTitle: string) => {
    setBlocks(
      blocks.map((b, i) => (i === blockIndex ? { ...b, title: newTitle } : b))
    );
  };

  // Añadir actividad dentro de un bloque específico
  const handleAddActivityToBlock = (blockIndex: number, activityId: string) => {
    const act = activities.find((a) => a.id === activityId);
    if (!act) return;

    const newStep: RoutineStep = {
      id: crypto.randomUUID ? crypto.randomUUID() : `rs_${Date.now()}_${Math.random()}`,
      activityId: act.id,
      activityTitle: act.title,
      stages: act.stages,
      autoAdvance: true
    };

    setBlocks(
      blocks.map((b, i) => {
        if (i !== blockIndex) return b;
        return { ...b, steps: [...b.steps, newStep] };
      })
    );
  };

  // Quitar una actividad de un bloque
  const handleRemoveStepFromBlock = (blockIndex: number, stepIndex: number) => {
    setBlocks(
      blocks.map((b, i) => {
        if (i !== blockIndex) return b;
        const newSteps = b.steps.filter((_, sIdx) => sIdx !== stepIndex);
        return { ...b, steps: newSteps };
      }).filter((b) => b.steps.length > 0) // Si queda vacío, se retira el bloque
    );
  };

  // Reordenar actividad dentro de un bloque
  const handleMoveStepInsideBlock = (
    blockIndex: number,
    stepIndex: number,
    direction: 'UP' | 'DOWN'
  ) => {
    setBlocks(
      blocks.map((b, i) => {
        if (i !== blockIndex) return b;
        const targetIdx = direction === 'UP' ? stepIndex - 1 : stepIndex + 1;
        if (targetIdx < 0 || targetIdx >= b.steps.length) return b;
        const stepsCopy = [...b.steps];
        const temp = stepsCopy[stepIndex];
        stepsCopy[stepIndex] = stepsCopy[targetIdx];
        stepsCopy[targetIdx] = temp;
        return { ...b, steps: stepsCopy };
      })
    );
  };

  // Alternar auto-advance de un paso
  const handleToggleAutoAdvance = (blockIndex: number, stepIndex: number) => {
    setBlocks(
      blocks.map((b, i) => {
        if (i !== blockIndex) return b;
        const stepsCopy = b.steps.map((st, sIdx) => {
          if (sIdx !== stepIndex) return st;
          return { ...st, autoAdvance: !st.autoAdvance };
        });
        return { ...b, steps: stepsCopy };
      })
    );
  };

  const handleSave = async () => {
    if (!title.trim()) {
      alert('Por favor ingresa un título para la rutina.');
      return;
    }

    const flatSteps = blocksToSteps(blocks);
    if (flatSteps.length === 0) {
      alert('Agrega al menos una actividad a la rutina.');
      return;
    }

    const routineToSave: RoutineItem = {
      id: initialRoutine?.id || (crypto.randomUUID ? crypto.randomUUID() : `rout_${Date.now()}`),
      title: title.trim(),
      description: description.trim(),
      steps: flatSteps,
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
          maxWidth: '780px',
          maxHeight: '92vh',
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
              Organiza actividades individuales o bloques repetitivos
            </p>
          </div>
          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={20} />
          </button>
        </div>

        {/* Form Body */}
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
              Título de la Rutina *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ej. Circuito Matutino, Pomodoro Estudio..."
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
              placeholder="Describe el propósito o dinámica de esta rutina..."
              style={{ marginTop: '6px', resize: 'none' }}
            />
          </div>

          {/* Selector de añadir actividades o bloques */}
          <div
            style={{
              background: 'var(--bg-tertiary)',
              padding: '16px 20px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Añadir a la Secuencia
              </label>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                Disponible: {activities.length} actividades
              </span>
            </div>

            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <select
                value={selectedActivityToAdd}
                onChange={(e) => setSelectedActivityToAdd(e.target.value)}
                style={{ flex: '1 1 240px' }}
              >
                {activities.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.title} ({Math.round(a.stages.reduce((sum, s) => sum + s.durationSeconds, 0) / 60)} min)
                  </option>
                ))}
              </select>

              <button
                type="button"
                onClick={handleAddSingleActivity}
                className="btn btn-secondary"
                style={{ gap: '6px' }}
              >
                <Plus size={15} />
                <span>+ Actividad Simple</span>
              </button>

              <button
                type="button"
                onClick={handleCreateBlock}
                className="btn btn-primary"
                style={{ gap: '6px' }}
                title="Crea un grupo de actividades que se repite N veces"
              >
                <Layers size={15} />
                <span>+ Nuevo Bloque Repetitivo</span>
              </button>
            </div>
          </div>

          {/* Header de la lista de bloques */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <label
              style={{
                fontSize: '0.82rem',
                fontWeight: 700,
                color: 'var(--text-secondary)',
                textTransform: 'uppercase',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}
            >
              <span>Estructura de la Rutina ({blocks.length} elementos)</span>
            </label>
            <span
              style={{
                fontSize: '0.84rem',
                fontWeight: 700,
                color: 'var(--accent-primary)',
                background: 'rgba(99, 102, 241, 0.1)',
                padding: '4px 10px',
                borderRadius: '6px',
                border: '1px solid rgba(99, 102, 241, 0.25)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <Clock size={14} />
              <span>Tiempo total: {totalMinutes} min</span>
            </span>
          </div>

          {blocks.length === 0 ? (
            <div
              style={{
                padding: '36px',
                textAlign: 'center',
                background: 'var(--bg-tertiary)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--text-muted)',
                fontSize: '0.9rem'
              }}
            >
              Añade una actividad simple o un bloque repetitivo para comenzar a estructurar tu rutina.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {blocks.map((b, bIdx) => {
                const isRepeatedBlock =
                  b.steps.length > 1 || (b.title && b.title.trim().length > 0) || (b.repeatCount && b.repeatCount > 1);

                const blockBaseSeconds = b.steps.reduce((acc, st) => {
                  return acc + st.stages.reduce((sAcc, s) => sAcc + s.durationSeconds, 0);
                }, 0);
                const blockTotalMinutes = Math.round(
                  (blockBaseSeconds * Math.max(1, b.repeatCount || 1)) / 60
                );

                return (
                  <div
                    key={b.id || bIdx}
                    style={{
                      borderRadius: 'var(--radius-lg)',
                      background: isRepeatedBlock ? 'rgba(30, 41, 59, 0.55)' : 'var(--bg-tertiary)',
                      border: isRepeatedBlock
                        ? '1px solid rgba(139, 92, 246, 0.35)'
                        : '1px solid var(--border-subtle)',
                      boxShadow: isRepeatedBlock ? '0 4px 16px rgba(139, 92, 246, 0.08)' : 'none',
                      padding: '16px',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '12px'
                    }}
                  >
                    {/* Cabecera del elemento / bloque */}
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: '12px',
                        flexWrap: 'wrap'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1, minWidth: '220px' }}>
                        <span
                          style={{
                            fontFamily: 'var(--font-mono)',
                            fontSize: '0.85rem',
                            fontWeight: 700,
                            color: 'var(--text-muted)',
                            width: '24px'
                          }}
                        >
                          #{bIdx + 1}
                        </span>

                        {isRepeatedBlock ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1 }}>
                            <Layers size={16} color="var(--accent-secondary)" />
                            <input
                              type="text"
                              value={b.title}
                              onChange={(e) => handleUpdateBlockTitle(bIdx, e.target.value)}
                              placeholder="Nombre del bloque (ej. Pomodoro, Serie A)"
                              style={{
                                padding: '6px 10px',
                                fontSize: '0.9rem',
                                fontWeight: 600,
                                flex: 1,
                                background: 'rgba(0,0,0,0.25)',
                                borderColor: 'rgba(139, 92, 246, 0.3)'
                              }}
                            />
                          </div>
                        ) : (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                              {b.steps[0]?.activityTitle || 'Actividad'}
                            </span>
                            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                              ({Math.round(blockBaseSeconds / 60)} min)
                            </span>
                          </div>
                        )}
                      </div>

                      {/* Controles de repetición y acciones */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        {/* Contador de repeticiones */}
                        <div
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '4px',
                            background: 'rgba(0,0,0,0.25)',
                            padding: '4px 8px',
                            borderRadius: '8px',
                            border: '1px solid var(--border-subtle)'
                          }}
                        >
                          <Repeat size={13} color="var(--accent-primary)" />
                          <span style={{ fontSize: '0.76rem', color: 'var(--text-secondary)', marginRight: '4px' }}>
                            Repetir:
                          </span>
                          <button
                            type="button"
                            onClick={() => handleUpdateBlockRepeats(bIdx, -1)}
                            disabled={b.repeatCount <= 1}
                            style={{
                              width: '22px',
                              height: '22px',
                              borderRadius: '4px',
                              background: 'var(--bg-secondary)',
                              border: 'none',
                              color: 'var(--text-primary)',
                              cursor: b.repeatCount <= 1 ? 'not-allowed' : 'pointer',
                              fontWeight: 700
                            }}
                          >
                            -
                          </button>
                          <span
                            style={{
                              fontFamily: 'var(--font-mono)',
                              fontSize: '0.88rem',
                              fontWeight: 800,
                              minWidth: '24px',
                              textAlign: 'center',
                              color: b.repeatCount > 1 ? 'var(--accent-secondary)' : 'var(--text-primary)'
                            }}
                          >
                            x{b.repeatCount || 1}
                          </span>
                          <button
                            type="button"
                            onClick={() => handleUpdateBlockRepeats(bIdx, 1)}
                            style={{
                              width: '22px',
                              height: '22px',
                              borderRadius: '4px',
                              background: 'var(--bg-secondary)',
                              border: 'none',
                              color: 'var(--text-primary)',
                              cursor: 'pointer',
                              fontWeight: 700
                            }}
                          >
                            +
                          </button>
                        </div>

                        {/* Duración subtotal */}
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', minWidth: '65px' }}>
                          = {blockTotalMinutes} min
                        </span>

                        {/* Reordenar bloque */}
                        <button
                          type="button"
                          onClick={() => handleMoveBlock(bIdx, 'UP')}
                          disabled={bIdx === 0}
                          className="btn-ghost btn-sm btn-icon"
                          title="Subir bloque"
                        >
                          <ArrowUp size={15} />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleMoveBlock(bIdx, 'DOWN')}
                          disabled={bIdx === blocks.length - 1}
                          className="btn-ghost btn-sm btn-icon"
                          title="Bajar bloque"
                        >
                          <ArrowDown size={15} />
                        </button>

                        <button
                          type="button"
                          onClick={() => handleRemoveBlock(bIdx)}
                          className="btn-ghost btn-sm btn-icon"
                          title="Eliminar bloque"
                          style={{ color: 'var(--accent-coral)' }}
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </div>

                    {/* Lista de actividades dentro del bloque */}
                    <div
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '6px',
                        paddingLeft: isRepeatedBlock ? '12px' : '0px',
                        borderLeft: isRepeatedBlock ? '2px solid rgba(139, 92, 246, 0.35)' : 'none'
                      }}
                    >
                      {b.steps.map((st, sIdx) => {
                        const stepMins = Math.round(
                          st.stages.reduce((sum, s) => sum + s.durationSeconds, 0) / 60
                        );

                        return (
                          <div
                            key={st.id || sIdx}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              padding: '8px 12px',
                              borderRadius: '6px',
                              background: 'var(--bg-secondary)',
                              border: '1px solid var(--border-subtle)',
                              gap: '10px'
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                              <span
                                style={{
                                  fontSize: '0.84rem',
                                  fontWeight: 600,
                                  color: 'var(--text-primary)',
                                  whiteSpace: 'nowrap',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis'
                                }}
                              >
                                {st.activityTitle}
                              </span>
                              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                ({stepMins} min)
                              </span>
                            </div>

                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              {/* Toggle Auto Advance */}
                              <button
                                type="button"
                                onClick={() => handleToggleAutoAdvance(bIdx, sIdx)}
                                className={`badge ${st.autoAdvance ? 'badge-active' : ''}`}
                                style={{
                                  cursor: 'pointer',
                                  border: 'none',
                                  fontSize: '0.72rem',
                                  padding: '2px 8px',
                                  background: st.autoAdvance ? 'rgba(16, 185, 129, 0.18)' : 'rgba(245, 158, 11, 0.18)',
                                  color: st.autoAdvance ? 'var(--accent-emerald)' : 'var(--accent-amber)'
                                }}
                                title={
                                  st.autoAdvance
                                    ? 'Avance automático al terminar'
                                    : 'Requiere confirmación manual para avanzar'
                                }
                              >
                                {st.autoAdvance ? 'Auto' : 'Pausa manual'}
                              </button>

                              {/* Reordenar dentro del bloque si tiene más de 1 actividad */}
                              {b.steps.length > 1 && (
                                <>
                                  <button
                                    type="button"
                                    onClick={() => handleMoveStepInsideBlock(bIdx, sIdx, 'UP')}
                                    disabled={sIdx === 0}
                                    className="btn-ghost btn-sm btn-icon"
                                    style={{ width: '24px', height: '24px', padding: 0 }}
                                  >
                                    <ArrowUp size={12} />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => handleMoveStepInsideBlock(bIdx, sIdx, 'DOWN')}
                                    disabled={sIdx === b.steps.length - 1}
                                    className="btn-ghost btn-sm btn-icon"
                                    style={{ width: '24px', height: '24px', padding: 0 }}
                                  >
                                    <ArrowDown size={12} />
                                  </button>
                                </>
                              )}

                              {/* Eliminar actividad del bloque */}
                              <button
                                type="button"
                                onClick={() => handleRemoveStepFromBlock(bIdx, sIdx)}
                                className="btn-ghost btn-sm btn-icon"
                                style={{ width: '24px', height: '24px', padding: 0, color: 'var(--accent-coral)' }}
                                title="Quitar actividad"
                              >
                                <X size={13} />
                              </button>
                            </div>
                          </div>
                        );
                      })}

                      {/* Selector rápido para añadir otra actividad al bloque */}
                      {isRepeatedBlock && (
                        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                          <select
                            defaultValue=""
                            onChange={(e) => {
                              if (e.target.value) {
                                handleAddActivityToBlock(bIdx, e.target.value);
                                e.target.value = '';
                              }
                            }}
                            style={{
                              fontSize: '0.8rem',
                              padding: '4px 8px',
                              background: 'rgba(0,0,0,0.2)',
                              color: 'var(--text-secondary)'
                            }}
                          >
                            <option value="" disabled>
                              + Añadir otra actividad a este bloque...
                            </option>
                            {activities.map((act) => (
                              <option key={act.id} value={act.id}>
                                {act.title} (
                                {Math.round(act.stages.reduce((sum, s) => sum + s.durationSeconds, 0) / 60)} min)
                              </option>
                            ))}
                          </select>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div
          style={{
            padding: '16px 28px',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: '12px',
            background: 'rgba(0,0,0,0.1)'
          }}
        >
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Cancelar
          </button>
          <button type="button" onClick={handleSave} className="btn btn-primary" style={{ gap: '6px' }}>
            <Save size={16} />
            <span>Guardar Rutina</span>
          </button>
        </div>
      </div>
    </div>
  );
};
