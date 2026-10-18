import React, { useState } from 'react';
import { Tag as TagIcon, X, Plus } from 'lucide-react';

interface TagInputProps {
  tags: string[];
  onChange: (tags: string[]) => void;
  suggestions?: string[];
  placeholder?: string;
}

const TAG_PALETTE = [
  '#6366F1', // Indigo
  '#3B82F6', // Blue
  '#06B6D4', // Cyan
  '#10B981', // Emerald
  '#F59E0B', // Amber
  '#EC4899', // Pink
  '#8B5CF6', // Purple
  '#F97316'  // Orange
];

export const getTagColor = (tag: string): string => {
  let hash = 0;
  for (let i = 0; i < tag.length; i++) {
    hash = tag.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % TAG_PALETTE.length;
  return TAG_PALETTE[index];
};

export const TagInput: React.FC<TagInputProps> = ({
  tags,
  onChange,
  suggestions = [],
  placeholder = 'Añadir tag (Enter o coma)...'
}) => {
  const [inputValue, setInputValue] = useState('');

  const handleAddTag = (rawText: string) => {
    const clean = rawText.trim().replace(/^#/, '').toLowerCase();
    if (!clean) return;
    if (!tags.includes(clean)) {
      onChange([...tags, clean]);
    }
    setInputValue('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      handleAddTag(inputValue);
    } else if (e.key === 'Backspace' && !inputValue && tags.length > 0) {
      onChange(tags.slice(0, -1));
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    onChange(tags.filter((t) => t !== tagToRemove));
  };

  const unselectedSuggestions = suggestions
    .map((s) => s.toLowerCase())
    .filter((s) => !tags.includes(s))
    .slice(0, 5);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '6px',
          padding: '8px 12px',
          background: 'var(--bg-tertiary)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)',
          minHeight: '44px',
          alignItems: 'center'
        }}
      >
        <TagIcon size={14} color="var(--text-muted)" style={{ marginRight: '2px' }} />

        {tags.map((tag) => {
          const color = getTagColor(tag);
          return (
            <span
              key={tag}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
                padding: '3px 8px',
                borderRadius: '6px',
                fontSize: '0.78rem',
                fontWeight: 600,
                background: `${color}20`,
                color: color,
                border: `1px solid ${color}40`,
                lineHeight: 1
              }}
            >
              <span
                style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  background: color
                }}
              />
              #{tag}
              <button
                type="button"
                onClick={() => handleRemoveTag(tag)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'inherit',
                  cursor: 'pointer',
                  padding: 0,
                  display: 'flex',
                  alignItems: 'center',
                  opacity: 0.7
                }}
              >
                <X size={12} />
              </button>
            </span>
          );
        })}

        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={() => {
            if (inputValue.trim()) {
              handleAddTag(inputValue);
            }
          }}
          placeholder={tags.length === 0 ? placeholder : ''}
          style={{
            flex: '1 1 120px',
            background: 'transparent',
            border: 'none',
            outline: 'none',
            color: 'var(--text-primary)',
            fontSize: '0.84rem',
            padding: '2px 4px'
          }}
        />

        {inputValue.trim() && (
          <button
            type="button"
            onClick={() => handleAddTag(inputValue)}
            className="btn-ghost btn-sm"
            style={{ padding: '2px 6px', fontSize: '0.75rem', gap: '2px' }}
          >
            <Plus size={12} /> Añadir
          </button>
        )}
      </div>

      {unselectedSuggestions.length > 0 && (
        <div style={{ display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Sugerencias:</span>
          {unselectedSuggestions.map((sug) => (
            <button
              key={sug}
              type="button"
              onClick={() => handleAddTag(sug)}
              style={{
                fontSize: '0.72rem',
                padding: '2px 6px',
                borderRadius: '4px',
                background: 'rgba(255,255,255,0.05)',
                border: '1px solid var(--border-subtle)',
                color: 'var(--text-secondary)',
                cursor: 'pointer'
              }}
            >
              +# {sug}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
