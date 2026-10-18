import React, { useState } from 'react';
import {
  Plus,
  Trash2,
  X,
  Save,
  Clock,
  GripVertical,
  ChevronRight,
  ChevronLeft,
  Sparkles,
  CheckCircle2
} from 'lucide-react';
import type { ActivityItem, Stage } from '../types/models';
import { PieChart } from './PieChart';
import { useData } from '../context/DataContext';
import { TagInput } from './TagInput';
import { BUILTIN_ACTIVITY_TEMPLATES, templateToActivity } from '../data/templates';

interface ActivityEditorModalProps {
  initialActivity?: ActivityItem | null;
  onClose: () => void;
  onCreatedInline?: (savedActivity: ActivityItem) => void;
}

const COLOR_PRESETS = [
  '#6366F1', // Indigo
  '#3B82F6', // Blue
  '#06B6D4', // Cyan
  '#10B981', // Emerald
  '#F59E0B', // Amber
  '#F97316', // Orange
  '#EC4899', // Pink
  '#8B5CF6'  // Purple
];

type WizardStep = 1 | 2 | 3;

export const ActivityEditorModal: React.FC<ActivityEditorModalProps> = ({
  initialActivity,
  onClose,
  onCreatedInline
}) => {
  const { saveActivity, folders, activities } = useData();

  const [currentStep, setCurrentStep] = useState<WizardStep>(1);
  const [title, setTitle] = useState(initialActivity?.title || '');
  const [description, setDescription] = useState(initialActivity?.description || '');
  const [category, setCategory] = useState(initialActivity?.category || folders[0]?.name || 'General');
  const [tags, setTags] = useState<string[]>(initialActivity?.tags || []);
  const [stages, setStages] = useState<Stage[]>(
    initialActivity?.stages || [
      {
        id: crypto.randomUUID ? crypto.randomUUID() : `s_${Date.now()}_1`,
        name: 'Etapa 1',
        durationSeconds: 25 * 60,
        notes: '',
        colorHex: '#6366F1',
        isEnabled: true
      }
    ]
  );

  const [editingStageIndex, setEditingStageIndex] = useState<number>(0);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

  // Recolectar todos los tags sugeridos del sistema
  const allExistingTags = Array.from(
    new Set(activities.flatMap((a) => a.tags || []))
  );

  const totalSeconds = stages
    .filter((s) => s.isEnabled !== false)
    .reduce((acc, s) => acc + s.durationSeconds, 0);
  const totalMinutes = Math.floor(totalSeconds / 60);
  const remainingSeconds = totalSeconds % 60;

  // Cargar una plantilla predefinida
  const handleApplyTemplate = (tplId: string) => {
    const found = BUILTIN_ACTIVITY_TEMPLATES.find((t) => t.id === tplId);
    if (!found) return;
    const act = templateToActivity(found);
    setTitle(act.title);
    setDescription(act.description);
    setCategory(act.category);
    setTags(act.tags || []);
    setStages(act.stages);
    setEditingStageIndex(0);
  };

  const handleAddStage = () => {
    const nextColor = COLOR_PRESETS[stages.length % COLOR_PRESETS.length];
    const newStage: Stage = {
      id: crypto.randomUUID ? crypto.randomUUID() : `s_${Date.now()}_${stages.length + 1}`,
      name: `Etapa ${stages.length + 1}`,
      durationSeconds: 10 * 60,
      notes: '',
      colorHex: nextColor,
      isEnabled: true
    };
    setStages([...stages, newStage]);
    setEditingStageIndex(stages.length);
  };

  const handleRemoveStage = (index: number) => {
    if (stages.length <= 1) {
      alert('Una actividad debe tener al menos una etapa.');
      return;
    }
    const filtered = stages.filter((_, i) => i !== index);
    setStages(filtered);
    setEditingStageIndex(Math.max(0, index - 1));
  };

  const handleUpdateStage = (index: number, patch: Partial<Stage>) => {
    const copy = [...stages];
    copy[index] = { ...copy[index], ...patch };
    setStages(copy);
  };

  // Drag and drop handlers
  const handleDragStart = (e: React.DragEvent, index: number) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === index) return;
    const copy = [...stages];
    const draggedItem = copy[draggedIndex];
    copy.splice(draggedIndex, 1);
    copy.splice(index, 0, draggedItem);
    setStages(copy);
    setDraggedIndex(index);
    setEditingStageIndex(index);
  };

  const handleDragEnd = () => {
    setDraggedIndex(null);
  };

  const validateStep1 = (): boolean => {
    if (!title.trim()) {
      alert('Por favor ingresa un título para la actividad.');
      return false;
    }
    return true;
  };

  const validateStep2 = (): boolean => {
    if (stages.length === 0) {
      alert('Agrega al menos una etapa.');
      return false;
    }
    const hasZeroDuration = stages.some((s) => s.durationSeconds <= 0);
    if (hasZeroDuration) {
      alert('Todas las etapas deben tener una duración mayor a cero.');
      return false;
    }
    return true;
  };

  const handleNext = () => {
    if (currentStep === 1) {
      if (validateStep1()) setCurrentStep(2);
    } else if (currentStep === 2) {
      if (validateStep2()) setCurrentStep(3);
    }
  };

  const handlePrev = () => {
    if (currentStep > 1) {
      setCurrentStep((prev) => (prev - 1) as WizardStep);
    }
  };

  const handleSave = async () => {
    if (!validateStep1() || !validateStep2()) return;

    const activityToSave: ActivityItem = {
      id: initialActivity?.id || (crypto.randomUUID ? crypto.randomUUID() : `act_${Date.now()}`),
      title: title.trim(),
      description: description.trim(),
      category: category.trim() || 'General',
      tags,
      stages,
      isTemplate: initialActivity?.isTemplate || false,
      createdAt: initialActivity?.createdAt || Date.now()
    };

    await saveActivity(activityToSave);
    if (onCreatedInline) {
      onCreatedInline(activityToSave);
    }
    onClose();
  };

  const currentEditingStage = stages[editingStageIndex] || stages[0];

  return (
    <div className="modal-overlay">
      <div
        className="glass-panel modal-container"
        style={{
          maxWidth: '920px',
          display: 'flex',
          flexDirection: 'column',
          maxHeight: '92vh'
        }}
      >
        {/* Header con Indicador de Pasos del Wizard */}
        <div
          style={{
            padding: '18px 28px',
            borderBottom: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '16px'
          }}
        >
          <div>
            <h3 style={{ fontSize: '1.25rem', color: 'var(--text-primary)', margin: 0 }}>
              {initialActivity ? 'Editar Actividad' : 'Nueva Actividad'}
            </h3>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Paso {currentStep} de 3:{' '}
              {currentStep === 1
                ? 'Información Básica'
                : currentStep === 2
                ? 'Diseño de Etapas'
                : 'Revisión y Confirmación'}
            </span>
          </div>

          {/* Stepper Visual */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {[
              { num: 1, label: 'Info' },
              { num: 2, label: 'Etapas' },
              { num: 3, label: 'Revisar' }
            ].map((st, i) => {
              const isActive = currentStep === st.num;
              const isPast = currentStep > st.num;
              return (
                <React.Fragment key={st.num}>
                  {i > 0 && (
                    <div
                      style={{
                        width: '20px',
                        height: '2px',
                        background: isPast ? 'var(--accent-primary)' : 'var(--border-subtle)'
                      }}
                    />
                  )}
                  <button
                    type="button"
                    onClick={() => {
                      if (st.num < currentStep) setCurrentStep(st.num as WizardStep);
                      else if (st.num === 2 && validateStep1()) setCurrentStep(2);
                      else if (st.num === 3 && validateStep1() && validateStep2()) setCurrentStep(3);
                    }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      background: isActive
                        ? 'var(--accent-primary)'
                        : isPast
                        ? 'rgba(99, 102, 241, 0.2)'
                        : 'var(--bg-tertiary)',
                      color: isActive ? '#fff' : isPast ? 'var(--accent-primary)' : 'var(--text-muted)',
                      border: `1px solid ${isActive || isPast ? 'var(--accent-primary)' : 'var(--border-subtle)'}`,
                      padding: '4px 10px',
                      borderRadius: '20px',
                      fontSize: '0.78rem',
                      fontWeight: 700,
                      cursor: 'pointer'
                    }}
                  >
                    <span>{st.num}</span>
                    <span>{st.label}</span>
                  </button>
                </React.Fragment>
              );
            })}
          </div>

          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={20} />
          </button>
        </div>

        {/* Wizard Step 1: Info Básica y Plantillas */}
        {currentStep === 1 && (
          <div
            style={{
              padding: '24px 28px',
              overflowY: 'auto',
              display: 'flex',
              flexDirection: 'column',
              gap: '20px'
            }}
          >
            {/* Galería de plantillas rápidas si es nueva actividad */}
            {!initialActivity && (
              <div
                style={{
                  background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(168, 85, 247, 0.08))',
                  border: '1px solid rgba(99, 102, 241, 0.25)',
                  borderRadius: 'var(--radius-lg)',
                  padding: '16px'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
                  <Sparkles size={16} color="var(--accent-primary)" />
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    Comenzar con una Plantilla Predefinida
                  </span>
                </div>
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                    gap: '10px'
                  }}
                >
                  {BUILTIN_ACTIVITY_TEMPLATES.map((tpl) => (
                    <button
                      key={tpl.id}
                      type="button"
                      onClick={() => handleApplyTemplate(tpl.id)}
                      style={{
                        textAlign: 'left',
                        padding: '10px 12px',
                        background: 'var(--bg-tertiary)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)',
                        cursor: 'pointer',
                        transition: 'all var(--transition-fast)'
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--accent-primary)')}
                      onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--border-subtle)')}
                    >
                      <div style={{ fontSize: '0.84rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                        {tpl.title}
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        {tpl.totalMinutes} min • {tpl.stages.length} etapas
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Título de la Actividad *
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ej. Sprint de Escritura, Rutina de Foco, Tabata..."
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
                  placeholder="Escribe un tag y presiona Enter (ej: trabajo, gimnasio, pomodoro)..."
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  Carpeta / Categoría
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  style={{ marginTop: '6px' }}
                >
                  {folders.map((f) => (
                    <option key={f.id} value={f.name}>
                      {f.name}
                    </option>
                  ))}
                  {folders.every((f) => f.name !== 'General') && <option value="General">General</option>}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  Tiempo Estimado Actual
                </label>
                <div
                  style={{
                    marginTop: '6px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '10px 14px',
                    background: 'var(--bg-tertiary)',
                    borderRadius: 'var(--radius-md)',
                    color: 'var(--text-primary)',
                    fontSize: '0.9rem',
                    fontWeight: 600
                  }}
                >
                  <Clock size={16} color="var(--accent-primary)" />
                  <span>
                    {totalMinutes}m {remainingSeconds > 0 ? `${remainingSeconds}s` : ''} ({stages.length} etapas)
                  </span>
                </div>
              </div>
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Descripción (opcional)
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
                placeholder="Describe el propósito o notas de esta actividad..."
                style={{ marginTop: '6px', resize: 'none' }}
              />
            </div>
          </div>
        )}

        {/* Wizard Step 2: Diseño de Etapas (Drag & Drop + Minutos y Segundos) */}
        {currentStep === 2 && (
          <div className="modal-two-columns" style={{ overflowY: 'auto' }}>
            {/* Columna Izquierda: Lista de Etapas Reordenables */}
            <div className="modal-column-left">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                <div>
                  <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                    Etapas ({stages.length})
                  </label>
                  <p style={{ fontSize: '0.74rem', color: 'var(--text-muted)', margin: 0 }}>
                    Arrastra para reordenar la secuencia del temporizador
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleAddStage}
                  className="btn btn-secondary btn-sm"
                  style={{ gap: '6px' }}
                >
                  <Plus size={14} />
                  <span>Añadir Etapa</span>
                </button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {stages.map((st, idx) => {
                  const isSelected = editingStageIndex === idx;
                  const mins = Math.floor(st.durationSeconds / 60);
                  const secs = st.durationSeconds % 60;
                  const timeLabel = secs > 0 ? `${mins}m ${secs}s` : `${mins} min`;

                  return (
                    <div
                      key={st.id || idx}
                      draggable
                      onDragStart={(e) => handleDragStart(e, idx)}
                      onDragOver={(e) => handleDragOver(e, idx)}
                      onDragEnd={handleDragEnd}
                      onClick={() => setEditingStageIndex(idx)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px 12px',
                        borderRadius: 'var(--radius-md)',
                        background: isSelected ? 'rgba(99, 102, 241, 0.14)' : 'var(--bg-tertiary)',
                        border: `1px solid ${isSelected ? 'var(--accent-primary)' : 'var(--border-subtle)'}`,
                        cursor: 'grab',
                        transition: 'all var(--transition-fast)',
                        opacity: draggedIndex === idx ? 0.4 : 1
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
                        <span style={{ color: 'var(--text-muted)', cursor: 'grab', display: 'flex' }}>
                          <GripVertical size={16} />
                        </span>
                        <span
                          style={{
                            width: '12px',
                            height: '12px',
                            borderRadius: '50%',
                            background: st.colorHex || '#3b82f6',
                            boxShadow: `0 0 8px ${st.colorHex}`
                          }}
                        />
                        <span
                          style={{
                            fontWeight: 600,
                            color: 'var(--text-primary)',
                            fontSize: '0.88rem',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis'
                          }}
                        >
                          {st.name}
                        </span>
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                          {timeLabel}
                        </span>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }} onClick={(e) => e.stopPropagation()}>
                        <button
                          type="button"
                          onClick={() => handleRemoveStage(idx)}
                          className="btn-ghost btn-sm"
                          style={{ padding: '4px' }}
                          title="Eliminar etapa"
                        >
                          <Trash2 size={14} color="#f43f5e" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Columna Derecha: Editor de Etapa Seleccionada + Previsualización de Pastel */}
            <div className="modal-column-right" style={{ background: 'rgba(0,0,0,0.1)' }}>
              {/* Previsualización del Pastel */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' }}>
                <span style={{ fontSize: '0.74rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Previsualización del Pastel
                </span>
                <PieChart
                  stages={stages}
                  activeStageIndex={editingStageIndex}
                  size={170}
                  donut={true}
                  innerRadiusRatio={0.6}
                  interactive={true}
                  onSliceClick={(idx) => setEditingStageIndex(idx)}
                  centerContent={
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                        {totalMinutes}m {remainingSeconds > 0 ? `${remainingSeconds}s` : ''}
                      </div>
                      <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>total</div>
                    </div>
                  }
                />
              </div>

              {/* Editor del detalle de la etapa activa */}
              {currentEditingStage && (
                <div
                  style={{
                    background: 'var(--bg-tertiary)',
                    padding: '16px',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-subtle)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '12px'
                  }}
                >
                  <h4 style={{ fontSize: '0.88rem', color: 'var(--text-primary)', margin: 0 }}>
                    Editar Etapa #{editingStageIndex + 1}
                  </h4>

                  <div>
                    <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Nombre</label>
                    <input
                      type="text"
                      value={currentEditingStage.name}
                      onChange={(e) => handleUpdateStage(editingStageIndex, { name: e.target.value })}
                      style={{ marginTop: '4px', padding: '8px 12px' }}
                    />
                  </div>

                  {/* Duración en Minutos y Segundos Separados */}
                  <div>
                    <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Duración</label>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginTop: '4px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <input
                          type="number"
                          min={0}
                          max={360}
                          value={Math.floor(currentEditingStage.durationSeconds / 60)}
                          onChange={(e) => {
                            const mins = Math.max(0, parseInt(e.target.value) || 0);
                            const secs = currentEditingStage.durationSeconds % 60;
                            const newTotal = mins * 60 + secs;
                            handleUpdateStage(editingStageIndex, { durationSeconds: Math.max(5, newTotal) });
                          }}
                          style={{ padding: '8px 10px', textAlign: 'center' }}
                        />
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>min</span>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <input
                          type="number"
                          min={0}
                          max={59}
                          step={5}
                          value={currentEditingStage.durationSeconds % 60}
                          onChange={(e) => {
                            const secs = Math.max(0, Math.min(59, parseInt(e.target.value) || 0));
                            const mins = Math.floor(currentEditingStage.durationSeconds / 60);
                            const newTotal = mins * 60 + secs;
                            handleUpdateStage(editingStageIndex, { durationSeconds: Math.max(5, newTotal) });
                          }}
                          style={{ padding: '8px 10px', textAlign: 'center' }}
                        />
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>seg</span>
                      </div>
                    </div>
                  </div>

                  {/* Selector de Color */}
                  <div>
                    <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Color de la Etapa</label>
                    <div style={{ display: 'flex', gap: '8px', marginTop: '6px', flexWrap: 'wrap' }}>
                      {COLOR_PRESETS.map((hex) => (
                        <button
                          key={hex}
                          type="button"
                          onClick={() => handleUpdateStage(editingStageIndex, { colorHex: hex })}
                          style={{
                            width: '24px',
                            height: '24px',
                            borderRadius: '50%',
                            background: hex,
                            border: currentEditingStage.colorHex === hex ? '3px solid #ffffff' : 'none',
                            boxShadow: currentEditingStage.colorHex === hex ? '0 0 8px ' + hex : 'none',
                            cursor: 'pointer'
                          }}
                        />
                      ))}
                    </div>
                  </div>

                  <div>
                    <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                      Instrucciones / Notas para el temporizador
                    </label>
                    <textarea
                      rows={2}
                      value={currentEditingStage.notes || ''}
                      onChange={(e) => handleUpdateStage(editingStageIndex, { notes: e.target.value })}
                      placeholder="Indicaciones visibles mientras corra el temporizador..."
                      style={{ marginTop: '4px', resize: 'none', fontSize: '0.8rem' }}
                    />
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Wizard Step 3: Revisión Final */}
        {currentStep === 3 && (
          <div
            style={{
              padding: '24px 28px',
              overflowY: 'auto',
              display: 'flex',
              flexDirection: 'column',
              gap: '20px'
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '16px',
                background: 'rgba(16, 185, 129, 0.08)',
                border: '1px solid rgba(16, 185, 129, 0.25)',
                borderRadius: 'var(--radius-md)'
              }}
            >
              <CheckCircle2 size={24} color="var(--accent-emerald)" />
              <div>
                <h4 style={{ fontSize: '0.95rem', color: 'var(--text-primary)', margin: 0 }}>
                  ¡Todo listo para guardar tu actividad!
                </h4>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
                  Revisa el resumen final antes de añadirla a tu biblioteca de actividades.
                </p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
              {/* Resumen de Datos */}
              <div
                style={{
                  background: 'var(--bg-tertiary)',
                  padding: '18px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '12px'
                }}
              >
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                    Título
                  </span>
                  <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {title}
                  </div>
                </div>

                {tags.length > 0 && (
                  <div>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                      Tags
                    </span>
                    <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: '4px' }}>
                      {tags.map((t) => (
                        <span
                          key={t}
                          style={{
                            padding: '2px 8px',
                            background: 'rgba(99, 102, 241, 0.15)',
                            color: 'var(--accent-primary)',
                            borderRadius: '4px',
                            fontSize: '0.78rem',
                            fontWeight: 600
                          }}
                        >
                          #{t}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                    Carpeta / Categoría
                  </span>
                  <div style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>{category}</div>
                </div>

                {description && (
                  <div>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                      Descripción
                    </span>
                    <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', margin: '4px 0 0 0' }}>
                      {description}
                    </p>
                  </div>
                )}

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                    Duración Total y Etapas
                  </span>
                  <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--accent-primary)', marginTop: '2px' }}>
                    {totalMinutes} min {remainingSeconds > 0 ? `${remainingSeconds}s` : ''} • {stages.length} etapas
                  </div>
                </div>
              </div>

              {/* Pastel Final y Lista de Etapas */}
              <div
                style={{
                  background: 'var(--bg-tertiary)',
                  padding: '18px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '14px'
                }}
              >
                <PieChart
                  stages={stages}
                  size={160}
                  donut={true}
                  innerRadiusRatio={0.6}
                  centerContent={
                    <div style={{ textAlign: 'center' }}>
                      <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                        {totalMinutes}m
                      </div>
                      <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>total</div>
                    </div>
                  }
                />

                <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {stages.map((st, i) => {
                    const mins = Math.floor(st.durationSeconds / 60);
                    const secs = st.durationSeconds % 60;
                    return (
                      <div
                        key={st.id || i}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '6px 10px',
                          background: 'var(--bg-secondary)',
                          borderRadius: '6px',
                          fontSize: '0.8rem'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span
                            style={{
                              width: '10px',
                              height: '10px',
                              borderRadius: '50%',
                              background: st.colorHex
                            }}
                          />
                          <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{st.name}</span>
                        </div>
                        <span style={{ color: 'var(--text-muted)' }}>
                          {mins}m {secs > 0 ? `${secs}s` : ''}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Footer Navigation Buttons */}
        <div
          style={{
            padding: '16px 28px',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
            background: 'rgba(0,0,0,0.15)',
            marginTop: 'auto'
          }}
        >
          <div>
            {currentStep > 1 && (
              <button type="button" onClick={handlePrev} className="btn btn-secondary" style={{ gap: '6px' }}>
                <ChevronLeft size={16} />
                <span>Anterior</span>
              </button>
            )}
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary">
              Cancelar
            </button>

            {currentStep < 3 ? (
              <button type="button" onClick={handleNext} className="btn btn-primary" style={{ gap: '6px' }}>
                <span>Siguiente</span>
                <ChevronRight size={16} />
              </button>
            ) : (
              <button type="button" onClick={handleSave} className="btn btn-primary" style={{ gap: '8px' }}>
                <Save size={16} />
                <span>Guardar Actividad</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
