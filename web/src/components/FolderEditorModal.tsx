import React, { useState } from 'react';
import { X, Save, FolderPlus } from 'lucide-react';
import type { FolderItem } from '../types/models';
import { useData } from '../context/DataContext';

interface FolderEditorModalProps {
  onClose: () => void;
}

const FOLDER_COLORS = [
  '#EC4899', // Pink (Anti-Inertia)
  '#3B82F6', // Blue (Writing)
  '#10B981', // Emerald (Wellness)
  '#F97316', // Orange (Home)
  '#6366F1', // Indigo (Study)
  '#8B5CF6', // Purple (Work)
  '#06B6D4', // Cyan
  '#F59E0B'  // Amber
];

export const FolderEditorModal: React.FC<FolderEditorModalProps> = ({ onClose }) => {
  const { saveFolder } = useData();

  const [name, setName] = useState('');
  const [colorHex, setColorHex] = useState(FOLDER_COLORS[0]);

  const handleSave = async () => {
    if (!name.trim()) {
      alert('Ingresa el nombre de la carpeta.');
      return;
    }

    const newFolder: FolderItem = {
      id: crypto.randomUUID ? crypto.randomUUID() : `folder_${Date.now()}`,
      name: name.trim(),
      colorHex,
      createdAt: Date.now()
    };

    await saveFolder(newFolder);
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
          maxWidth: '440px',
          borderRadius: 'var(--radius-xl)',
          overflow: 'hidden',
          boxShadow: 'var(--shadow-lg)'
        }}
      >
        <div
          style={{
            padding: '20px 24px',
            borderBottom: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <FolderPlus size={20} color="var(--accent-primary)" />
            <h3 style={{ fontSize: '1.2rem', color: 'var(--text-primary)' }}>Nueva Carpeta</h3>
          </div>
          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={18} />
          </button>
        </div>

        <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '18px' }}>
          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Nombre de la Carpeta
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ej. Proyectos Personales, Salud..."
              style={{ marginTop: '6px' }}
              autoFocus
            />
          </div>

          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Color Distintivo
            </label>
            <div style={{ display: 'flex', gap: '10px', marginTop: '8px', flexWrap: 'wrap' }}>
              {FOLDER_COLORS.map((hex) => (
                <button
                  key={hex}
                  type="button"
                  onClick={() => setColorHex(hex)}
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '50%',
                    background: hex,
                    border: colorHex === hex ? '3px solid #ffffff' : 'none',
                    boxShadow: colorHex === hex ? '0 0 12px ' + hex : 'none'
                  }}
                />
              ))}
            </div>
          </div>
        </div>

        <div
          style={{
            padding: '16px 24px',
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
            <span>Crear Carpeta</span>
          </button>
        </div>
      </div>
    </div>
  );
};
