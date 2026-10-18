import React, { useState } from 'react';
import {
  Plus,
  Trash2,
  X,
  Save,
  Layers,
  Repeat,
  Clock,
  ChevronDown,
  ChevronUp,
  RotateCcw,
  Sparkles,
  GripVertical
} from 'lucide-react';
import type { RoutineBlock, RoutineItem, RoutineStep, Stage } from '../types/models';
import {
  stepsToBlocks,
  blocksToSteps,
  calculateRoutineTotalSeconds
} from '../types/models';
import { useData } from '../context/DataContext';
import { TagInput } from './TagInput';
import { ActivityEditorModal } from './ActivityEditorModal';

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
  const [tags, setTags] = useState<string[]>(initialRoutine?.tags || []);
  const [blocks, setBlocks] = useState<RoutineBlock[]>(() =>
    stepsToBlocks(initialRoutine?.steps || [])
  );

  const [selectedActivityToAdd, setSelectedActivityToAdd] = useState<string>(
    activities[0]?.id || ''
  );

  // Estados de interfaz y acordeones
  const [expandedStepId, setExpandedStepId] = useState<string | null>(null);
  const [isInlineActivityModalOpen, setIsInlineActivityModalOpen] = useState(false);
  const [draggedBlockIndex, setDraggedBlockIndex] = useState<number | null>(null);
  const [draggedStepPos, setDraggedStepPos] = useState<{ blockIdx: number; stepIdx: number } | null>(null);

  // Sugerencias de tags del sistema
  const allExistingTags = Array.from(
    new Set(activities.flatMap((a) => a.tags || []))
  );

  // Calcula la duración total en segundos tomando en cuenta solo etapas habilitadas
  const totalSeconds = calculateRoutineTotalSeconds(blocksToSteps(blocks));
  const totalMinutes = Math.floor(totalSeconds / 60);
  const remainingSeconds = totalSeconds % 60;

  // Añadir una actividad individual como paso
  const handleAddSingleActivity = (actId?: string) => {
    const targetId = actId || selectedActivityToAdd;
    const act = activities.find((a) => a.id === targetId);
    if (!act) return;

    // Clonar etapas para que sea un snapshot desacoplado
    const stagesClone: Stage[] = act.stages.map((st) => ({
      ...st,
      isEnabled: st.isEnabled !== false
    }));

    const newStep: RoutineStep = {
      id: crypto.randomUUID ? crypto.randomUUID() : `rs_${Date.now()}_${Math.random()}`,
      activityId: act.id,
      activityTitle: act.title,
      stages: stagesClone,
      autoAdvance: true,
      originalActivityId: act.id,
      isCustomized: false
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
            stages: act.stages.map((st) => ({ ...st, isEnabled: st.isEnabled !== false })),
            autoAdvance: true,
            originalActivityId: act.id,
            isCustomized: false
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

  // Eliminar un bloque completo
  const handleRemoveBlock = (index: number) => {
    setBlocks(blocks.filter((_, i) => i !== index));
  };

  // Añadir actividad dentro de un bloque específico
  const handleAddActivityToBlock = (blockIndex: number, activityId: string) => {
    const act = activities.find((a) => a.id === activityId);
    if (!act) return;

    const newStep: RoutineStep = {
      id: crypto.randomUUID ? crypto.randomUUID() : `rs_${Date.now()}_${Math.random()}`,
      activityId: act.id,
      activityTitle: act.title,
      stages: act.stages.map((st) => ({ ...st, isEnabled: st.isEnabled !== false })),
      autoAdvance: true,
      originalActivityId: act.id,
      isCustomized: false
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
      }).filter((b) => b.steps.length > 0)
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

  // Personalización de etapas dentro de un RoutineStep (Acordeón)
  const handleToggleStageEnabled = (blockIndex: number, stepIndex: number, stageIndex: number) => {
    setBlocks(
      blocks.map((b, bIdx) => {
        if (bIdx !== blockIndex) return b;
        const stepsCopy = b.steps.map((step, sIdx) => {
          if (sIdx !== stepIndex) return step;
          const stagesCopy = step.stages.map((st, stIdx) => {
            if (stIdx !== stageIndex) return st;
            const nextEnabled = st.isEnabled === false ? true : false;
            return { ...st, isEnabled: nextEnabled };
          });
          return { ...step, stages: stagesCopy, isCustomized: true };
        });
        return { ...b, steps: stepsCopy };
      })
    );
  };

  const handleUpdateStageDuration = (
    blockIndex: number,
    stepIndex: number,
    stageIndex: number,
    newDurationSeconds: number
  ) => {
    setBlocks(
      blocks.map((b, bIdx) => {
        if (bIdx !== blockIndex) return b;
        const stepsCopy = b.steps.map((step, sIdx) => {
          if (sIdx !== stepIndex) return step;
          const stagesCopy = step.stages.map((st, stIdx) => {
            if (stIdx !== stageIndex) return st;
            return { ...st, durationSeconds: Math.max(5, newDurationSeconds) };
          });
          return { ...step, stages: stagesCopy, isCustomized: true };
        });
        return { ...b, steps: stepsCopy };
      })
    );
  };

  // Restablecer un paso a su versión original de la biblioteca
  const handleResetStepToOriginal = (blockIndex: number, stepIndex: number) => {
    const currentStep = blocks[blockIndex]?.steps[stepIndex];
    if (!currentStep) return;

    const originalAct = activities.find((a) => a.id === (currentStep.originalActivityId || currentStep.activityId));
    if (!originalAct) {
      alert('No se encontró la actividad original en la biblioteca.');
      return;
    }

    const restoredStages: Stage[] = originalAct.stages.map((st) => ({
      ...st,
      isEnabled: true
    }));

    setBlocks(
      blocks.map((b, bIdx) => {
        if (bIdx !== blockIndex) return b;
        const stepsCopy = b.steps.map((step, sIdx) => {
          if (sIdx !== stepIndex) return step;
          return {
            ...step,
            activityTitle: originalAct.title,
            stages: restoredStages,
            isCustomized: false
          };
        });
        return { ...b, steps: stepsCopy };
      })
    );
  };

  // Drag and drop de Bloques
  const handleBlockDragStart = (e: React.DragEvent, index: number) => {
    setDraggedBlockIndex(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleBlockDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (draggedBlockIndex === null || draggedBlockIndex === index) return;
    const copy = [...blocks];
    const item = copy[draggedBlockIndex];
    copy.splice(draggedBlockIndex, 1);
    copy.splice(index, 0, item);
    setBlocks(copy);
    setDraggedBlockIndex(index);
  };

  const handleBlockDragEnd = () => {
    setDraggedBlockIndex(null);
  };

  // Drag and drop de Pasos dentro de un bloque
  const handleStepDragStart = (e: React.DragEvent, blockIdx: number, stepIdx: number) => {
    e.stopPropagation();
    setDraggedStepPos({ blockIdx, stepIdx });
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleStepDragOver = (e: React.DragEvent, targetBlockIdx: number, targetStepIdx: number) => {
    e.preventDefault();
    e.stopPropagation();
    if (!draggedStepPos) return;
    if (draggedStepPos.blockIdx === targetBlockIdx && draggedStepPos.stepIdx === targetStepIdx) return;

    const copy = [...blocks];
    const sourceBlock = copy[draggedStepPos.blockIdx];
    const targetBlock = copy[targetBlockIdx];
    if (!sourceBlock || !targetBlock) return;

    const [movedStep] = sourceBlock.steps.splice(draggedStepPos.stepIdx, 1);
    targetBlock.steps.splice(targetStepIdx, 0, movedStep);

    setBlocks(copy);
    setDraggedStepPos({ blockIdx: targetBlockIdx, stepIdx: targetStepIdx });
  };

  const handleStepDragEnd = () => {
    setDraggedStepPos(null);
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
      tags,
      steps: flatSteps,
      isTemplate: initialRoutine?.isTemplate || false,
      createdAt: initialRoutine?.createdAt || Date.now()
    };

    await saveRoutine(routineToSave);
    onClose();
  };

  return (
    <>
      <div className="modal-overlay">
        <div
          className="glass-panel modal-container"
          style={{
            maxWidth: '840px',
            display: 'flex',
            flexDirection: 'column',
            maxHeight: '92vh'
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
              <h3 style={{ fontSize: '1.25rem', color: 'var(--text-primary)', margin: 0 }}>
                {initialRoutine ? 'Editar Rutina' : 'Nueva Rutina'}
              </h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
                Organiza secuencias de actividades, personaliza etapas por paso o crea bloques repetitivos
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
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Título de la Rutina *
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ej. Circuito Matutino, Pomodoro Estudio..."
                style={{ marginTop: '6px' }}
                autoFocus
              />
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Tags y Etiquetas de Color
              </label>
              <div style={{ marginTop: '6px' }}>
                <TagInput
                  tags={tags}
                  onChange={setTags}
                  suggestions={allExistingTags}
                  placeholder="Añadir tags (ej: mañana, gym, productivo)..."
                />
              </div>
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
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

            {/* Selector de añadir actividades o bloques + botón de crear inline */}
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
                  Añadir Actividades a la Secuencia
                </label>
                <button
                  type="button"
                  onClick={() => setIsInlineActivityModalOpen(true)}
                  className="btn btn-secondary btn-sm"
                  style={{ gap: '6px', fontSize: '0.78rem' }}
                  title="Crea una actividad nueva sin salir del editor de rutinas"
                >
                  <Sparkles size={14} color="var(--accent-primary)" />
                  <span>+ Crear Nueva Actividad</span>
                </button>
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
                  onClick={() => handleAddSingleActivity()}
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
                  background: 'rgba(99, 102, 241, 0.12)',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  border: '1px solid rgba(99, 102, 241, 0.25)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
              >
                <Clock size={14} />
                <span>
                  Tiempo total: {totalMinutes}m {remainingSeconds > 0 ? `${remainingSeconds}s` : ''}
                </span>
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
                    const activeStages = st.stages.filter((s) => s.isEnabled !== false);
                    return acc + activeStages.reduce((sAcc, s) => sAcc + s.durationSeconds, 0);
                  }, 0);
                  const blockTotalSeconds = blockBaseSeconds * Math.max(1, b.repeatCount || 1);
                  const bMins = Math.floor(blockTotalSeconds / 60);
                  const bSecs = blockTotalSeconds % 60;

                  return (
                    <div
                      key={b.id || bIdx}
                      draggable
                      onDragStart={(e) => handleBlockDragStart(e, bIdx)}
                      onDragOver={(e) => handleBlockDragOver(e, bIdx)}
                      onDragEnd={handleBlockDragEnd}
                      style={{
                        borderRadius: 'var(--radius-lg)',
                        background: isRepeatedBlock ? 'rgba(30, 41, 59, 0.65)' : 'var(--bg-tertiary)',
                        border: isRepeatedBlock
                          ? '1px solid rgba(139, 92, 246, 0.35)'
                          : '1px solid var(--border-subtle)',
                        boxShadow: isRepeatedBlock ? '0 4px 16px rgba(139, 92, 246, 0.08)' : 'none',
                        padding: '16px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '12px',
                        opacity: draggedBlockIndex === bIdx ? 0.4 : 1,
                        cursor: 'default'
                      }}
                    >
                      {/* Cabecera del bloque */}
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          gap: '12px',
                          flexWrap: 'wrap'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '220px' }}>
                          <span style={{ cursor: 'grab', color: 'var(--text-muted)', display: 'flex' }}>
                            <GripVertical size={16} />
                          </span>

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
                                ({bMins}m {bSecs > 0 ? `${bSecs}s` : ''})
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
                            = {bMins}m {bSecs > 0 ? `${bSecs}s` : ''}
                          </span>

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

                      {/* Lista de actividades dentro del bloque con soporte de Acordeón */}
                      <div
                        style={{
                          display: 'flex',
                          flexDirection: 'column',
                          gap: '8px',
                          paddingLeft: isRepeatedBlock ? '12px' : '0px',
                          borderLeft: isRepeatedBlock ? '2px solid rgba(139, 92, 246, 0.35)' : 'none'
                        }}
                      >
                        {b.steps.map((st, sIdx) => {
                          const isExpanded = expandedStepId === st.id;
                          const activeStages = st.stages.filter((s) => s.isEnabled !== false);
                          const stepTotalSecs = activeStages.reduce((sum, s) => sum + s.durationSeconds, 0);
                          const stepMins = Math.floor(stepTotalSecs / 60);
                          const stepSecs = stepTotalSecs % 60;

                          return (
                            <div
                              key={st.id || sIdx}
                              draggable
                              onDragStart={(e) => handleStepDragStart(e, bIdx, sIdx)}
                              onDragOver={(e) => handleStepDragOver(e, bIdx, sIdx)}
                              onDragEnd={handleStepDragEnd}
                              style={{
                                borderRadius: 'var(--radius-md)',
                                background: 'var(--bg-secondary)',
                                border: `1px solid ${isExpanded ? 'var(--accent-primary)' : 'var(--border-subtle)'}`,
                                overflow: 'hidden',
                                transition: 'all var(--transition-fast)'
                              }}
                            >
                              {/* Barra principal del paso */}
                              <div
                                style={{
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'space-between',
                                  padding: '10px 14px',
                                  gap: '10px'
                                }}
                              >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                                  <span style={{ cursor: 'grab', color: 'var(--text-muted)', display: 'flex' }}>
                                    <GripVertical size={14} />
                                  </span>

                                  <span
                                    style={{
                                      fontSize: '0.88rem',
                                      fontWeight: 600,
                                      color: 'var(--text-primary)',
                                      whiteSpace: 'nowrap',
                                      overflow: 'hidden',
                                      textOverflow: 'ellipsis'
                                    }}
                                  >
                                    {st.activityTitle}
                                  </span>

                                  <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                                    ({stepMins}m {stepSecs > 0 ? `${stepSecs}s` : ''} • {activeStages.length}/{st.stages.length} etapas)
                                  </span>

                                  {st.isCustomized && (
                                    <span
                                      style={{
                                        fontSize: '0.68rem',
                                        padding: '2px 6px',
                                        background: 'rgba(245, 158, 11, 0.15)',
                                        color: 'var(--accent-amber)',
                                        borderRadius: '4px',
                                        fontWeight: 700,
                                        border: '1px solid rgba(245, 158, 11, 0.3)'
                                      }}
                                    >
                                      Personalizada
                                    </span>
                                  )}
                                </div>

                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                  {/* Botón para expandir/contraer acordeón de etapas */}
                                  <button
                                    type="button"
                                    onClick={() => setExpandedStepId(isExpanded ? null : st.id)}
                                    className="btn-ghost btn-sm"
                                    style={{
                                      fontSize: '0.75rem',
                                      gap: '4px',
                                      padding: '4px 8px',
                                      color: isExpanded ? 'var(--accent-primary)' : 'var(--text-secondary)'
                                    }}
                                  >
                                    <span>{isExpanded ? 'Ocultar etapas' : 'Personalizar etapas'}</span>
                                    {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                                  </button>

                                  {/* Toggle Auto Advance */}
                                  <button
                                    type="button"
                                    onClick={() => handleToggleAutoAdvance(bIdx, sIdx)}
                                    className={`badge ${st.autoAdvance ? 'badge-active' : ''}`}
                                    style={{
                                      cursor: 'pointer',
                                      border: 'none',
                                      fontSize: '0.72rem',
                                      padding: '3px 8px',
                                      background: st.autoAdvance
                                        ? 'rgba(16, 185, 129, 0.18)'
                                        : 'rgba(245, 158, 11, 0.18)',
                                      color: st.autoAdvance ? 'var(--accent-emerald)' : 'var(--accent-amber)'
                                    }}
                                    title={
                                      st.autoAdvance
                                        ? 'Avance automático al terminar'
                                        : 'Requiere confirmación manual para avanzar'
                                    }
                                  >
                                    {st.autoAdvance ? 'Auto' : 'Pausa'}
                                  </button>

                                  {/* Eliminar actividad del bloque */}
                                  <button
                                    type="button"
                                    onClick={() => handleRemoveStepFromBlock(bIdx, sIdx)}
                                    className="btn-ghost btn-sm btn-icon"
                                    style={{ color: 'var(--accent-coral)' }}
                                    title="Quitar actividad"
                                  >
                                    <X size={14} />
                                  </button>
                                </div>
                              </div>

                              {/* Contenido Desplegable del Acordeón: Etapas, Exclusiones y Tiempos */}
                              {isExpanded && (
                                <div
                                  style={{
                                    padding: '12px 16px',
                                    background: 'rgba(0,0,0,0.18)',
                                    borderTop: '1px solid var(--border-subtle)',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    gap: '10px'
                                  }}
                                >
                                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                                      Etapas incluidas para esta rutina (snapshot independiente)
                                    </span>

                                    {st.isCustomized && (
                                      <button
                                        type="button"
                                        onClick={() => handleResetStepToOriginal(bIdx, sIdx)}
                                        className="btn-ghost btn-sm"
                                        style={{ fontSize: '0.72rem', color: 'var(--accent-amber)', gap: '4px' }}
                                        title="Restablece las etapas y duraciones a la versión original de la biblioteca"
                                      >
                                        <RotateCcw size={12} />
                                        <span>Restablecer a original</span>
                                      </button>
                                    )}
                                  </div>

                                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                    {st.stages.map((stage, stageIdx) => {
                                      const isEnabled = stage.isEnabled !== false;
                                      const mins = Math.floor(stage.durationSeconds / 60);
                                      const secs = stage.durationSeconds % 60;

                                      return (
                                        <div
                                          key={stage.id || stageIdx}
                                          style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'space-between',
                                            padding: '6px 10px',
                                            borderRadius: '6px',
                                            background: isEnabled ? 'var(--bg-tertiary)' : 'rgba(255,255,255,0.03)',
                                            border: `1px solid ${isEnabled ? 'var(--border-subtle)' : 'transparent'}`,
                                            opacity: isEnabled ? 1 : 0.45,
                                            gap: '10px'
                                          }}
                                        >
                                          {/* Checkbox de Inclusión */}
                                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: 0 }}>
                                            <input
                                              type="checkbox"
                                              checked={isEnabled}
                                              onChange={() => handleToggleStageEnabled(bIdx, sIdx, stageIdx)}
                                              style={{ cursor: 'pointer', width: '15px', height: '15px' }}
                                              id={`st_${st.id}_${stageIdx}`}
                                            />
                                            <label
                                              htmlFor={`st_${st.id}_${stageIdx}`}
                                              style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '8px',
                                                cursor: 'pointer',
                                                minWidth: 0,
                                                textDecoration: isEnabled ? 'none' : 'line-through'
                                              }}
                                            >
                                              <span
                                                style={{
                                                  width: '8px',
                                                  height: '8px',
                                                  borderRadius: '50%',
                                                  background: stage.colorHex || '#6366F1'
                                                }}
                                              />
                                              <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                                                {stage.name}
                                              </span>
                                            </label>
                                          </div>

                                          {/* Inputs de Duración mm:ss específicos para este paso */}
                                          {isEnabled ? (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                              <input
                                                type="number"
                                                min={0}
                                                max={180}
                                                value={mins}
                                                onChange={(e) => {
                                                  const newM = Math.max(0, parseInt(e.target.value) || 0);
                                                  handleUpdateStageDuration(bIdx, sIdx, stageIdx, newM * 60 + secs);
                                                }}
                                                style={{ width: '45px', padding: '3px 6px', textAlign: 'center', fontSize: '0.8rem' }}
                                              />
                                              <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>m</span>

                                              <input
                                                type="number"
                                                min={0}
                                                max={59}
                                                step={5}
                                                value={secs}
                                                onChange={(e) => {
                                                  const newS = Math.max(0, Math.min(59, parseInt(e.target.value) || 0));
                                                  handleUpdateStageDuration(bIdx, sIdx, stageIdx, mins * 60 + newS);
                                                }}
                                                style={{ width: '45px', padding: '3px 6px', textAlign: 'center', fontSize: '0.8rem' }}
                                              />
                                              <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>s</span>
                                            </div>
                                          ) : (
                                            <span style={{ fontSize: '0.74rem', color: 'var(--accent-coral)' }}>
                                              Omitida
                                            </span>
                                          )}
                                        </div>
                                      );
                                    })}
                                  </div>
                                </div>
                              )}
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
                                  {act.title} ({Math.round(act.stages.reduce((sum, s) => sum + s.durationSeconds, 0) / 60)} min)
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
              background: 'rgba(0,0,0,0.15)',
              marginTop: 'auto'
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

      {/* Modal Inline para Crear Actividad sin salir */}
      {isInlineActivityModalOpen && (
        <ActivityEditorModal
          initialActivity={null}
          onClose={() => setIsInlineActivityModalOpen(false)}
          onCreatedInline={(savedAct) => {
            handleAddSingleActivity(savedAct.id);
            setSelectedActivityToAdd(savedAct.id);
            setIsInlineActivityModalOpen(false);
          }}
        />
      )}
    </>
  );
};
