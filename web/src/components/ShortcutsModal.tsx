import React from 'react';
import { X, Keyboard } from 'lucide-react';

interface ShortcutsModalProps {
  onClose: () => void;
}

export const ShortcutsModal: React.FC<ShortcutsModalProps> = ({ onClose }) => {
  const shortcuts = [
    { key: 'Espacio', desc: 'Pausar o reanudar el temporizador activo' },
    { key: 'P', desc: 'Abrir / alternar ventana flotante reducida (Picture-in-Picture)' },
    { key: 'S', desc: 'Saltar inmediatamente a la siguiente etapa' },
    { key: 'N', desc: 'Avanzar al siguiente bloque cuando hay pausa manual' },
    { key: 'M', desc: 'Activar o desactivar anuncios de voz (TTS en español)' },
    { key: 'F', desc: 'Alternar entre vista de pantalla completa y modo ventana' },
    { key: 'Esc', desc: 'Minimizar sesión activa al dock flotante / Cerrar diálogos' }
  ];

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
          maxWidth: '520px',
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
            <Keyboard size={20} color="var(--accent-primary)" />
            <h3 style={{ fontSize: '1.2rem', color: 'var(--text-primary)' }}>Atajos de Teclado</h3>
          </div>
          <button onClick={onClose} className="btn-ghost btn-icon">
            <X size={18} />
          </button>
        </div>

        <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {shortcuts.map((sc) => (
            <div
              key={sc.key}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                background: 'var(--bg-tertiary)',
                border: '1px solid var(--border-subtle)'
              }}
            >
              <span style={{ fontSize: '0.88rem', color: 'var(--text-secondary)' }}>{sc.desc}</span>
              <kbd
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: '0.84rem',
                  fontWeight: 700,
                  color: 'var(--text-primary)',
                  background: 'var(--bg-card)',
                  border: '1px solid var(--border-medium)',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                }}
              >
                {sc.key}
              </kbd>
            </div>
          ))}
        </div>

        <div
          style={{
            padding: '14px 24px',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'flex-end',
            background: 'rgba(0,0,0,0.1)'
          }}
        >
          <button onClick={onClose} className="btn btn-primary btn-sm">
            Entendido
          </button>
        </div>
      </div>
    </div>
  );
};
