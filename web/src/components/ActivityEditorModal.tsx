import React, { useState } from 'react';
import { Plus, Trash2, ArrowUp, ArrowDown, X, Save, Clock } from 'lucide-react';
import type { ActivityItem, Stage } from '../types/models';
import { PieChart } from './PieChart';
import { useData } from '../context/DataContext';

interface ActivityEditorModalProps {
  initialActivity?: ActivityItem | null;
  onClose: () => void;
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

export const ActivityEditorModal: React.FC<ActivityEditorModalProps> = ({ initialActivity, onClose }) => {
  const { saveActivity, folders } = useData();

  const [title, setTitle] = useState(initialActivity?.title || '');
  const [description, setDescription] = useState(initialActivity?.description || '');
  const [category, setCategory] = useState(initialActivity?.category || folders[0]?.name || 'General');
  const [stages, setStages] = useState<Stage[]>(
    initialActivity?.stages || [
      {
        id: crypto.randomUUID ? crypto.randomUUID() : `s_${Date.now()}_1`,
        name: 'Etapa 1',
        durationSeconds: 25 * 60,
        notes: '',
        colorHex: '#6366F1'
      }
    ]
  );

  const [editingStageIndex, setEditingStageIndex] = useState<number>(0);

  const totalSeconds = stages.reduce((acc, s) => acc + s.durationSeconds, 0);
  const totalMinutes = Math.round(totalSeconds / 60);

  const handleAddStage = () => {
    const nextColor = COLOR_PRESETS[stages.length % COLOR_PRESETS.length];
    const newStage: Stage = {
      id: crypto.randomUUID ? crypto.randomUUID() : `s_${Date.now()}_${stages.length + 1}`,
      name: `Etapa ${stages.length + 1}`,
      durationSeconds: 10 * 60,
      notes: '',
      colorHex: nextColor
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

  const handleMoveStage = (index: number, direction: 'UP' | 'DOWN') => {
    const targetIdx = direction === 'UP' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= stages.length) return;
    const copy = [...stages];
    const temp = copy[index];
    copy[index] = copy[targetIdx];
    copy[targetIdx] = temp;
    setStages(copy);
    setEditingStageIndex(targetIdx);
  };

  const handleUpdateStage = (index: number, patch: Partial<Stage>) => {
    const copy = [...stages];
    copy[index] = { ...copy[index], ...patch };
    setStages(copy);
  };

  const handleSave = async () => {
    if (!title.trim()) {
      alert('Por favor ingresa un título para la actividad.');
      return;
    }
    if (stages.length === 0) {
      alert('Agrega al menos una etapa.');
      return;
    }

    const activityToSave: ActivityItem = {
      id: initialActivity?.id || (crypto.randomUUID ? crypto.randomUUID() : `act_${Date.now()}`),
      title: title.trim(),
      description: description.trim(),
      category: category.trim() || 'General',
      stages,
      isTemplate: initialActivity?.isTemplate || false,
      createdAt: initialActivity?.createdAt || Date.now()
    };

    await saveActivity(activityToSave);
    onClose();
  };

  const currentEditingStage = stages[editingStageIndex] || stages[0];

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
          maxWidth: '920px',
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
              {initialActivity ? 'Editar Actividad' : 'Nueva Actividad'}
            </h3>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Define el pastel de etapas circulares y sus tiempos de enfoque
            </p>
          </div>
          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={20} />
          </button>
        </div>

        {/* Body 2 columns */}
        <div style={{ display: 'flex', flex: 1, overflowY: 'auto' }}>
          {/* Left Form: Basic info and Stage list */}
          <div
            style={{
              flex: 1.2,
              padding: '24px 28px',
              borderRight: '1px solid var(--border-subtle)',
              display: 'flex',
              flexDirection: 'column',
              gap: '18px',
              overflowY: 'auto'
            }}
          >
            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Título de la Actividad *
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ej. Sprint de Escritura, Rutina de Foco..."
                style={{ marginTop: '6px' }}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
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
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  Tiempo Total
                </label>
                <div
                  style={{
                    marginTop: '6px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '10px 14px',
                    background: 'var(--bg-tertiary)',
                    borderRadius: 'var(--radius-md)',
                    color: 'var(--text-primary)',
                    fontSize: '0.92rem',
                    fontWeight: 600
                  }}
                >
                  <Clock size={16} color="var(--accent-primary)" />
                  <span>{totalMinutes} minutos ({stages.length} etapas)</span>
                </div>
              </div>
            </div>

            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Descripción (opcional)
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
                placeholder="Describe el propósito o notas de esta actividad..."
                style={{ marginTop: '6px', resize: 'none' }}
              />
            </div>

            {/* Stages List */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                  Etapas ({stages.length})
                </label>
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
                  const mins = Math.round(st.durationSeconds / 60);
                  return (
                    <div
                      key={st.id || idx}
                      onClick={() => setEditingStageIndex(idx)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px 14px',
                        borderRadius: 'var(--radius-md)',
                        background: isSelected ? 'rgba(99, 102, 241, 0.12)' : 'var(--bg-tertiary)',
                        border: `1px solid ${isSelected ? 'var(--accent-primary)' : 'var(--border-subtle)'}`,
                        cursor: 'pointer',
                        transition: 'all var(--transition-fast)'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <span
                          style={{
                            width: '12px',
                            height: '12px',
                            borderRadius: '50%',
                            background: st.colorHex || '#3b82f6',
                            boxShadow: `0 0 8px ${st.colorHex}`
                          }}
                        />
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.9rem' }}>
                          {st.name}
                        </span>
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                          {mins} min
                        </span>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }} onClick={(e) => e.stopPropagation()}>
                        <button
                          type="button"
                          onClick={() => handleMoveStage(idx, 'UP')}
                          disabled={idx === 0}
                          className="btn-ghost btn-sm"
                          style={{ padding: '4px', opacity: idx === 0 ? 0.3 : 1 }}
                        >
                          <ArrowUp size={14} />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleMoveStage(idx, 'DOWN')}
                          disabled={idx === stages.length - 1}
                          className="btn-ghost btn-sm"
                          style={{ padding: '4px', opacity: idx === stages.length - 1 ? 0.3 : 1 }}
                        >
                          <ArrowDown size={14} />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleRemoveStage(idx)}
                          className="btn-ghost btn-sm"
                          style={{ padding: '4px' }}
                        >
                          <Trash2 size={14} color="#f43f5e" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Right Column: Selected Stage Editor & Real-time Pie Preview */}
          <div
            style={{
              flex: 1,
              padding: '24px 28px',
              display: 'flex',
              flexDirection: 'column',
              gap: '20px',
              background: 'rgba(0,0,0,0.1)'
            }}
          >
            {/* Live Pie Preview */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                Previsualización del Pastel
              </span>
              <PieChart
                stages={stages}
                activeStageIndex={editingStageIndex}
                size={180}
                donut={true}
                innerRadiusRatio={0.6}
                interactive={true}
                onSliceClick={(idx) => setEditingStageIndex(idx)}
                centerContent={
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                      {totalMinutes}m
                    </div>
                    <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>total</div>
                  </div>
                }
              />
            </div>

            {/* Stage Editor details for currentEditingStage */}
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
                <h4 style={{ fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                  Editar Etapa #{editingStageIndex + 1}
                </h4>

                <div>
                  <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Nombre de la Etapa</label>
                  <input
                    type="text"
                    value={currentEditingStage.name}
                    onChange={(e) => handleUpdateStage(editingStageIndex, { name: e.target.value })}
                    style={{ marginTop: '4px', padding: '8px 12px' }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                    Duración: {Math.floor(currentEditingStage.durationSeconds / 60)} min {currentEditingStage.durationSeconds % 60} s
                  </label>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                    <input
                      type="number"
                      min={1}
                      max={180}
                      value={Math.floor(currentEditingStage.durationSeconds / 60)}
                      onChange={(e) => {
                        const mins = Math.max(1, parseInt(e.target.value) || 1);
                        handleUpdateStage(editingStageIndex, { durationSeconds: mins * 60 });
                      }}
                      style={{ padding: '8px 12px' }}
                    />
                    <span style={{ alignSelf: 'center', fontSize: '0.82rem', color: 'var(--text-muted)' }}>minutos</span>
                  </div>
                </div>

                {/* Color presets */}
                <div>
                  <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Color de la Etapa</label>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '6px', flexWrap: 'wrap' }}>
                    {COLOR_PRESETS.map((hex) => (
                      <button
                        key={hex}
                        type="button"
                        onClick={() => handleUpdateStage(editingStageIndex, { colorHex: hex })}
                        style={{
                          width: '26px',
                          height: '26px',
                          borderRadius: '50%',
                          background: hex,
                          border: currentEditingStage.colorHex === hex ? '3px solid #ffffff' : 'none',
                          boxShadow: currentEditingStage.colorHex === hex ? '0 0 10px ' + hex : 'none'
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
                    placeholder="Instrucciones que verás mientras corre el temporizador..."
                    style={{ marginTop: '4px', resize: 'none', fontSize: '0.82rem' }}
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions */}
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
            <span>Guardar Actividad</span>
          </button>
        </div>
      </div>
    </div>
  );
};
