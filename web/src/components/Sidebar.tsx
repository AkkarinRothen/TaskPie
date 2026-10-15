import React, { useState } from 'react';
import {
  Activity,
  Layers,
  Folder,
  History,
  Download,
  Upload,
  Keyboard,
  Sun,
  Moon,
  PieChart as PieIcon,
  Trash2,
  FolderPlus,
  X
} from 'lucide-react';
import { useData } from '../context/DataContext';

interface SidebarProps {
  onOpenFolderModal: () => void;
  onOpenShortcutsModal: () => void;
  isOpen?: boolean;
  onClose?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  onOpenFolderModal,
  onOpenShortcutsModal,
  isOpen = false,
  onClose
}) => {
  const {
    activities,
    routines,
    projects,
    folders,
    summaries,
    selectedFolderId,
    setSelectedFolderId,
    activeTab,
    setActiveTab,
    deleteFolder,
    exportBackup,
    importBackup
  } = useData();

  const [theme, setTheme] = useState<'dark' | 'light'>(
    document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark'
  );

  const toggleTheme = () => {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    document.documentElement.setAttribute('data-theme', next);
  };

  const handleFileInput = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await importBackup(file);
      alert(`¡Copia de seguridad importada con éxito!\nActividades: ${res.activities}, Rutinas: ${res.routines}, Proyectos: ${res.projects}, Carpetas: ${res.folders}`);
    } catch {
      alert('Error al leer el archivo JSON de copia de seguridad.');
    }
    e.target.value = '';
  };

  const navItems = [
    { id: 'ACTIVITIES' as const, label: 'Actividades', icon: Activity, count: activities.length },
    { id: 'ROUTINES' as const, label: 'Rutinas', icon: Layers, count: routines.length },
    { id: 'PROJECTS' as const, label: 'Proyectos', icon: Folder, count: projects.length },
    { id: 'HISTORY' as const, label: 'Historial', icon: History, count: summaries.length }
  ];

  return (
    <aside className={`app-sidebar ${isOpen ? 'open' : ''}`}>
      {/* Brand Header */}
      <div
        style={{
          padding: '24px 20px',
          borderBottom: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '12px',
              background: 'var(--gradient-brand)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: 'var(--shadow-glow)'
            }}
          >
            <PieIcon size={24} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em' }}>TaskPie</h1>
              <span
                style={{
                  fontSize: '0.65rem',
                  fontWeight: 700,
                  background: 'rgba(99, 102, 241, 0.2)',
                  color: 'var(--accent-primary)',
                  padding: '2px 6px',
                  borderRadius: '6px'
                }}
              >
                WEB
              </span>
            </div>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Gestión visual de foco</p>
          </div>
        </div>

        {onClose && (
          <button
            onClick={onClose}
            className="btn-ghost btn-icon mobile-close-btn"
            title="Cerrar menú"
          >
            <X size={20} />
          </button>
        )}
      </div>

      {/* Main Navigation */}
      <div style={{ padding: '16px 12px' }}>
        <p
          style={{
            fontSize: '0.72rem',
            fontWeight: 700,
            textTransform: 'uppercase',
            color: 'var(--text-muted)',
            padding: '0 8px 8px 8px',
            letterSpacing: '0.05em'
          }}
        >
          Navegación
        </p>
        <nav style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          {navItems.map((item) => {
            const Icon = item.icon;
            const isSelected = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveTab(item.id);
                  if (onClose && window.innerWidth <= 900) onClose();
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-md)',
                  background: isSelected ? 'var(--gradient-brand)' : 'transparent',
                  color: isSelected ? '#ffffff' : 'var(--text-secondary)',
                  fontWeight: isSelected ? 600 : 500,
                  fontSize: '0.9rem',
                  boxShadow: isSelected ? '0 4px 12px rgba(99, 102, 241, 0.3)' : 'none',
                  transition: 'all var(--transition-fast)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Icon size={18} />
                  <span>{item.label}</span>
                </div>
                <span
                  style={{
                    fontSize: '0.75rem',
                    background: isSelected ? 'rgba(255, 255, 255, 0.25)' : 'var(--bg-tertiary)',
                    color: isSelected ? '#ffffff' : 'var(--text-muted)',
                    padding: '2px 8px',
                    borderRadius: 'var(--radius-full)',
                    fontWeight: 600
                  }}
                >
                  {item.count}
                </span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Folders Section */}
      <div style={{ flex: 1, padding: '8px 12px', overflowY: 'auto' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '4px 8px 8px 8px'
          }}
        >
          <span
            style={{
              fontSize: '0.72rem',
              fontWeight: 700,
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              letterSpacing: '0.05em'
            }}
          >
            Carpetas
          </span>
          <button
            onClick={onOpenFolderModal}
            className="btn-ghost"
            title="Añadir carpeta"
            style={{ padding: '2px 6px', borderRadius: '4px' }}
          >
            <FolderPlus size={15} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          {/* Todas las carpetas */}
          <button
            onClick={() => {
              setSelectedFolderId(null);
              if (onClose && window.innerWidth <= 900) onClose();
            }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '8px 12px',
              borderRadius: 'var(--radius-sm)',
              background: selectedFolderId === null ? 'var(--bg-tertiary)' : 'transparent',
              color: selectedFolderId === null ? 'var(--text-primary)' : 'var(--text-secondary)',
              fontSize: '0.86rem',
              fontWeight: selectedFolderId === null ? 600 : 400
            }}
          >
            <div
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: 'var(--accent-primary)'
              }}
            />
            <span>Todas</span>
          </button>

          {/* Lista de carpetas */}
          {folders.map((folder) => {
            const isSelected = selectedFolderId === folder.id;
            const count = activities.filter((a) => a.category.toLowerCase() === folder.name.toLowerCase()).length;
            return (
              <div
                key={folder.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  borderRadius: 'var(--radius-sm)',
                  background: isSelected ? 'var(--bg-tertiary)' : 'transparent',
                  padding: '2px 4px 2px 12px'
                }}
              >
                <button
                  onClick={() => {
                    setSelectedFolderId(folder.id);
                    if (onClose && window.innerWidth <= 900) onClose();
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    flex: 1,
                    textAlign: 'left',
                    color: isSelected ? 'var(--text-primary)' : 'var(--text-secondary)',
                    fontSize: '0.86rem',
                    fontWeight: isSelected ? 600 : 400,
                    padding: '6px 0'
                  }}
                >
                  <div
                    style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '50%',
                      background: folder.colorHex || '#3B82F6'
                    }}
                  />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {folder.name}
                  </span>
                </button>
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', padding: '0 4px' }}>
                    {count}
                  </span>
                  <button
                    onClick={() => {
                      if (confirm(`¿Eliminar carpeta "${folder.name}"?`)) {
                        deleteFolder(folder.id);
                      }
                    }}
                    title="Eliminar carpeta"
                    style={{
                      color: 'var(--text-muted)',
                      padding: '4px',
                      borderRadius: '4px',
                      opacity: 0.6
                    }}
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Footer Actions */}
      <div
        style={{
          padding: '16px 14px',
          borderTop: '1px solid var(--border-subtle)',
          display: 'flex',
          flexDirection: 'column',
          gap: '6px',
          background: 'rgba(0,0,0,0.1)'
        }}
      >
        <button
          onClick={onOpenShortcutsModal}
          className="btn-ghost"
          style={{ width: '100%', justifyContent: 'flex-start', gap: '8px', fontSize: '0.82rem' }}
        >
          <Keyboard size={16} />
          <span>Atajos de Teclado</span>
        </button>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            onClick={exportBackup}
            className="btn-secondary btn-sm"
            style={{ flex: 1, fontSize: '0.78rem', gap: '6px' }}
            title="Exportar respaldo JSON compatible con Android"
          >
            <Download size={14} />
            <span>Exportar</span>
          </button>

          <label
            className="btn-secondary btn-sm"
            style={{
              flex: 1,
              fontSize: '0.78rem',
              gap: '6px',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
            title="Importar respaldo JSON"
          >
            <Upload size={14} />
            <span>Importar</span>
            <input type="file" accept=".json" onChange={handleFileInput} style={{ display: 'none' }} />
          </label>
        </div>

        <button
          onClick={toggleTheme}
          className="btn-ghost btn-sm"
          style={{ width: '100%', justifyContent: 'center', gap: '8px', marginTop: '4px', fontSize: '0.8rem' }}
        >
          {theme === 'dark' ? <Sun size={15} /> : <Moon size={15} />}
          <span>{theme === 'dark' ? 'Modo Claro' : 'Modo Oscuro'}</span>
        </button>
      </div>
    </aside>
  );
};
