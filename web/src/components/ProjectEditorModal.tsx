import React, { useState } from 'react';
import {
  X,
  Save,
  Plus,
  Copy,
  Trash2,
  Layers,
  Clock,
  ChevronDown,
  ChevronUp,
  RotateCcw,
  GripVertical
} from 'lucide-react';
import type { ProjectItem, ProjectRoutineItem, RoutineStep } from '../types/models';
import { calculateRoutineTotalSeconds } from '../types/models';
import { useData } from '../context/DataContext';
import { TagInput } from './TagInput';

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
  const [tags, setTags] = useState<string[]>(initialProject?.tags || []);
  const [selectedActivityIds, setSelectedActivityIds] = useState<string[]>(
    initialProject?.activityIds || []
  );

  // Inicializar secuencias de rutinas con snapshots desacoplados
  const [projectRoutines, setProjectRoutines] = useState<ProjectRoutineItem[]>(() => {
    if (initialProject?.projectRoutines && initialProject.projectRoutines.length > 0) {
      return initialProject.projectRoutines;
    }
    // Compatibilidad hacia atrás: migrar de routineIds simples
    if (initialProject?.routineIds && initialProject.routineIds.length > 0) {
      const mapped: ProjectRoutineItem[] = [];
      for (let idx = 0; idx < initialProject.routineIds.length; idx++) {
        const rId = initialProject.routineIds[idx];
        const rout = routines.find((r) => r.id === rId);
        if (rout) {
          const clonedSteps: RoutineStep[] = rout.steps.map((st) => ({
            ...st,
            stages: st.stages.map((s) => ({ ...s, isEnabled: s.isEnabled !== false }))
          }));
          mapped.push({
            id: `proj_r_${Date.now()}_${idx}_${Math.random()}`,
            routineId: rout.id,
            title: rout.title,
            steps: clonedSteps,
            isCustomized: false
          });
        }
      }
      return mapped;
    }
    return [];
  });

  const [selectedRoutineToAdd, setSelectedRoutineToAdd] = useState<string>(
    routines[0]?.id || ''
  );

  const [expandedRoutineId, setExpandedRoutineId] = useState<string | null>(null);
  const [draggedRoutineIndex, setDraggedRoutineIndex] = useState<number | null>(null);

  // Sugerencias de tags del sistema
  const allExistingTags = Array.from(
    new Set(routines.flatMap((r) => r.tags || []))
  );

  // Calcular duración total acumulada de todas las rutinas (solo etapas activas)
  const totalRoutinesSeconds = projectRoutines.reduce((acc, pr) => {
    return acc + calculateRoutineTotalSeconds(pr.steps);
  }, 0);
  const totalRoutinesMinutes = Math.floor(totalRoutinesSeconds / 60);
  const remainingSeconds = totalRoutinesSeconds % 60;

  // Añadir rutina al proyecto con clonación profunda (snapshot)
  const handleAddRoutine = () => {
    if (!selectedRoutineToAdd) return;
    const rout = routines.find((r) => r.id === selectedRoutineToAdd);
    if (!rout) return;

    const clonedSteps: RoutineStep[] = rout.steps.map((st) => ({
      ...st,
      stages: st.stages.map((s) => ({
        ...s,
        isEnabled: s.isEnabled !== false
      }))
    }));

    const newProjectRoutine: ProjectRoutineItem = {
      id: `proj_r_${Date.now()}_${Math.random()}`,
      routineId: rout.id,
      title: rout.title,
      steps: clonedSteps,
      isCustomized: false
    };

    setProjectRoutines([...projectRoutines, newProjectRoutine]);
  };

  const handleRemoveRoutine = (index: number) => {
    setProjectRoutines(projectRoutines.filter((_, i) => i !== index));
  };

  const handleDuplicateRoutine = (index: number) => {
    const target = projectRoutines[index];
    if (!target) return;
    const copy: ProjectRoutineItem = {
      ...target,
      id: `proj_r_${Date.now()}_${Math.random()}`,
      steps: target.steps.map((st) => ({
        ...st,
        stages: st.stages.map((s) => ({ ...s }))
      }))
    };
    const nextList = [...projectRoutines];
    nextList.splice(index + 1, 0, copy);
    setProjectRoutines(nextList);
  };

  // Toggle stage inclusion inside a project routine
  const handleToggleStageInProject = (
    routineIndex: number,
    stepIndex: number,
    stageIndex: number
  ) => {
    setProjectRoutines(
      projectRoutines.map((pr, rIdx) => {
        if (rIdx !== routineIndex) return pr;
        const stepsCopy = pr.steps.map((st, sIdx) => {
          if (sIdx !== stepIndex) return st;
          const stagesCopy = st.stages.map((stage, stgIdx) => {
            if (stgIdx !== stageIndex) return stage;
            return { ...stage, isEnabled: stage.isEnabled === false ? true : false };
          });
          return { ...st, stages: stagesCopy };
        });
        return { ...pr, steps: stepsCopy, isCustomized: true };
      })
    );
  };

  // Toggle an entire activity step inside a project routine
  const handleToggleStepInProject = (routineIndex: number, stepIndex: number) => {
    setProjectRoutines(
      projectRoutines.map((pr, rIdx) => {
        if (rIdx !== routineIndex) return pr;
        const targetStep = pr.steps[stepIndex];
        if (!targetStep) return pr;
        const allCurrentlyEnabled = targetStep.stages.every((s) => s.isEnabled !== false);
        const nextState = !allCurrentlyEnabled;

        const stepsCopy = pr.steps.map((st, sIdx) => {
          if (sIdx !== stepIndex) return st;
          return {
            ...st,
            stages: st.stages.map((stage) => ({ ...stage, isEnabled: nextState }))
          };
        });
        return { ...pr, steps: stepsCopy, isCustomized: true };
      })
    );
  };

  // Restablecer rutina del proyecto a la original de la biblioteca
  const handleResetRoutineToOriginal = (routineIndex: number) => {
    const pr = projectRoutines[routineIndex];
    if (!pr) return;
    const original = routines.find((r) => r.id === pr.routineId);
    if (!original) {
      alert('No se encontró la rutina original en la biblioteca.');
      return;
    }

    const restoredSteps: RoutineStep[] = original.steps.map((st) => ({
      ...st,
      stages: st.stages.map((s) => ({ ...s, isEnabled: true }))
    }));

    setProjectRoutines(
      projectRoutines.map((item, idx) => {
        if (idx !== routineIndex) return item;
        return {
          ...item,
          title: original.title,
          steps: restoredSteps,
          isCustomized: false
        };
      })
    );
  };

  // Drag and drop para la secuencia de rutinas
  const handleDragStart = (e: React.DragEvent, index: number) => {
    setDraggedRoutineIndex(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (draggedRoutineIndex === null || draggedRoutineIndex === index) return;
    const copy = [...projectRoutines];
    const moved = copy[draggedRoutineIndex];
    copy.splice(draggedRoutineIndex, 1);
    copy.splice(index, 0, moved);
    setProjectRoutines(copy);
    setDraggedRoutineIndex(index);
  };

  const handleDragEnd = () => {
    setDraggedRoutineIndex(null);
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
      tags,
      activityIds: selectedActivityIds,
      routineIds: projectRoutines.map((pr) => pr.routineId),
      projectRoutines,
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
          maxWidth: '820px',
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
              {initialProject ? 'Editar Proyecto' : 'Nuevo Proyecto'}
            </h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
              Secuencia ordenada de rutinas con exclusión granular y snapshots desacoplados
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
            <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Título del Proyecto *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ej. Sprint de Programación, Jornada de Estudio..."
              style={{ marginTop: '6px' }}
              autoFocus
            />
          </div>

          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Tags y Etiquetas
            </label>
            <div style={{ marginTop: '6px' }}>
              <TagInput
                tags={tags}
                onChange={setTags}
                suggestions={allExistingTags}
                placeholder="Añadir tags al proyecto..."
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
                <span>Secuencia de Rutinas ({projectRoutines.length})</span>
              </label>

              {projectRoutines.length > 0 && (
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
                  <span>
                    {totalRoutinesMinutes}m {remainingSeconds > 0 ? `${remainingSeconds}s` : ''} total
                  </span>
                </span>
              )}
            </div>

            {/* Lista de rutinas en la secuencia */}
            {projectRoutines.length === 0 ? (
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
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '12px' }}>
                {projectRoutines.map((pr, idx) => {
                  const isExpanded = expandedRoutineId === pr.id;
                  const routSecs = calculateRoutineTotalSeconds(pr.steps);
                  const routMins = Math.floor(routSecs / 60);
                  const routRemSecs = routSecs % 60;

                  return (
                    <div
                      key={pr.id || idx}
                      draggable
                      onDragStart={(e) => handleDragStart(e, idx)}
                      onDragOver={(e) => handleDragOver(e, idx)}
                      onDragEnd={handleDragEnd}
                      style={{
                        borderRadius: 'var(--radius-md)',
                        background: 'var(--bg-tertiary)',
                        border: `1px solid ${isExpanded ? 'var(--accent-secondary)' : 'var(--border-subtle)'}`,
                        overflow: 'hidden',
                        opacity: draggedRoutineIndex === idx ? 0.4 : 1,
                        transition: 'all var(--transition-fast)'
                      }}
                    >
                      {/* Cabecera de la Rutina en la secuencia */}
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '10px 14px',
                          gap: '12px'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0, flex: 1 }}>
                          <span style={{ cursor: 'grab', color: 'var(--text-muted)', display: 'flex' }}>
                            <GripVertical size={14} />
                          </span>

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
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <span
                                style={{
                                  fontSize: '0.9rem',
                                  fontWeight: 600,
                                  color: 'var(--text-primary)',
                                  whiteSpace: 'nowrap',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis'
                                }}
                              >
                                {pr.title}
                              </span>
                              {pr.isCustomized && (
                                <span
                                  style={{
                                    fontSize: '0.68rem',
                                    padding: '2px 6px',
                                    background: 'rgba(245, 158, 11, 0.15)',
                                    color: 'var(--accent-amber)',
                                    borderRadius: '4px',
                                    fontWeight: 700
                                  }}
                                >
                                  Personalizada
                                </span>
                              )}
                            </div>
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                              {routMins}m {routRemSecs > 0 ? `${routRemSecs}s` : ''} • {pr.steps.length} pasos
                            </span>
                          </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          {/* Botón para expandir/contraer personalización granular */}
                          <button
                            type="button"
                            onClick={() => setExpandedRoutineId(isExpanded ? null : pr.id)}
                            className="btn-ghost btn-sm"
                            style={{
                              fontSize: '0.75rem',
                              gap: '4px',
                              padding: '4px 8px',
                              color: isExpanded ? 'var(--accent-secondary)' : 'var(--text-secondary)'
                            }}
                          >
                            <span>{isExpanded ? 'Ocultar partes' : 'Personalizar partes'}</span>
                            {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                          </button>

                          {/* Duplicar rutina en la secuencia */}
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

                      {/* Acordeón de Personalización Granular por Rutina */}
                      {isExpanded && (
                        <div
                          style={{
                            padding: '12px 16px',
                            background: 'rgba(0,0,0,0.2)',
                            borderTop: '1px solid var(--border-subtle)',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '12px'
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                              Desmarca las actividades o etapas que desees omitir en este proyecto
                            </span>

                            {pr.isCustomized && (
                              <button
                                type="button"
                                onClick={() => handleResetRoutineToOriginal(idx)}
                                className="btn-ghost btn-sm"
                                style={{ fontSize: '0.72rem', color: 'var(--accent-amber)', gap: '4px' }}
                                title="Restablecer la rutina completa a su versión original"
                              >
                                <RotateCcw size={12} />
                                <span>Restablecer a original</span>
                              </button>
                            )}
                          </div>

                          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            {pr.steps.map((st, stepIdx) => {
                              const isStepActive = st.stages.some((s) => s.isEnabled !== false);

                              return (
                                <div
                                  key={st.id || stepIdx}
                                  style={{
                                    background: 'var(--bg-secondary)',
                                    padding: '8px 12px',
                                    borderRadius: '6px',
                                    border: '1px solid var(--border-subtle)',
                                    opacity: isStepActive ? 1 : 0.4
                                  }}
                                >
                                  {/* Cabecera del paso de la rutina */}
                                  <div
                                    style={{
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'space-between',
                                      marginBottom: '6px'
                                    }}
                                  >
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                      <input
                                        type="checkbox"
                                        checked={isStepActive}
                                        onChange={() => handleToggleStepInProject(idx, stepIdx)}
                                        style={{ cursor: 'pointer' }}
                                      />
                                      <span
                                        style={{
                                          fontSize: '0.84rem',
                                          fontWeight: 700,
                                          color: 'var(--text-primary)',
                                          textDecoration: isStepActive ? 'none' : 'line-through'
                                        }}
                                      >
                                        {st.activityTitle}
                                      </span>
                                    </div>
                                    <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                                      {st.stages.filter((s) => s.isEnabled !== false).length}/{st.stages.length} etapas
                                    </span>
                                  </div>

                                  {/* Lista de etapas del paso */}
                                  <div
                                    style={{
                                      display: 'flex',
                                      flexWrap: 'wrap',
                                      gap: '6px',
                                      paddingLeft: '22px'
                                    }}
                                  >
                                    {st.stages.map((stage, stgIdx) => {
                                      const isStageEnabled = stage.isEnabled !== false;
                                      const sMins = Math.floor(stage.durationSeconds / 60);
                                      const sSecs = stage.durationSeconds % 60;

                                      return (
                                        <button
                                          key={stage.id || stgIdx}
                                          type="button"
                                          onClick={() => handleToggleStageInProject(idx, stepIdx, stgIdx)}
                                          style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '5px',
                                            padding: '3px 8px',
                                            borderRadius: '4px',
                                            fontSize: '0.74rem',
                                            background: isStageEnabled
                                              ? 'rgba(99, 102, 241, 0.12)'
                                              : 'rgba(255, 255, 255, 0.05)',
                                            border: `1px solid ${isStageEnabled ? 'rgba(99, 102, 241, 0.3)' : 'var(--border-subtle)'}`,
                                            color: isStageEnabled ? 'var(--text-primary)' : 'var(--text-muted)',
                                            textDecoration: isStageEnabled ? 'none' : 'line-through',
                                            cursor: 'pointer'
                                          }}
                                        >
                                          <span
                                            style={{
                                              width: '6px',
                                              height: '6px',
                                              borderRadius: '50%',
                                              background: stage.colorHex || '#6366F1',
                                              opacity: isStageEnabled ? 1 : 0.3
                                            }}
                                          />
                                          <span>{stage.name}</span>
                                          <span style={{ opacity: 0.6 }}>
                                            ({sMins}m{sSecs > 0 ? ` ${sSecs}s` : ''})
                                          </span>
                                        </button>
                                      );
                                    })}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      )}
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
            background: 'rgba(0,0,0,0.15)',
            marginTop: 'auto'
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
