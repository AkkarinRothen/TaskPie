import React from 'react';
import type { Stage } from '../types/models';

interface PieChartProps {
  stages: Stage[];
  activeStageIndex?: number;
  activeStageProgress?: number; // 0 to 1
  size?: number;
  donut?: boolean;
  innerRadiusRatio?: number;
  interactive?: boolean;
  onSliceClick?: (index: number) => void;
  centerContent?: React.ReactNode;
}

export const PieChart: React.FC<PieChartProps> = ({
  stages,
  activeStageIndex,
  activeStageProgress,
  size = 200,
  donut = true,
  innerRadiusRatio = 0.6,
  interactive = true,
  onSliceClick,
  centerContent
}) => {
  const totalDuration = stages.reduce((acc, s) => acc + Math.max(s.durationSeconds, 1), 0);

  if (totalDuration === 0 || stages.length === 0) {
    return (
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle
          cx={size / 2}
          cy={size / 2}
          r={size * 0.45}
          fill="none"
          stroke="var(--border-subtle)"
          strokeWidth="6"
          strokeDasharray="4 4"
        />
        <text
          x={size / 2}
          y={size / 2}
          textAnchor="middle"
          dominantBaseline="central"
          fill="var(--text-muted)"
          fontSize={size * 0.08}
        >
          Sin etapas
        </text>
      </svg>
    );
  }

  const cx = size / 2;
  const cy = size / 2;
  const outerR = size * 0.46;
  const innerR = donut ? outerR * innerRadiusRatio : 0;

  // Calcular arcos polares
  let currentAngle = -90; // Empezar en las 12 en punto
  const slices = stages.map((stage, index) => {
    const fraction = Math.max(stage.durationSeconds, 1) / totalDuration;
    const angleDelta = fraction * 360;
    const startAngle = currentAngle;
    const endAngle = currentAngle + angleDelta;
    currentAngle = endAngle;

    const isActive = activeStageIndex === index;

    // Convertir coordenadas polares a cartesianas
    const radStart = (startAngle * Math.PI) / 180;
    const radEnd = (endAngle * Math.PI) / 180;

    const x1 = cx + outerR * Math.cos(radStart);
    const y1 = cy + outerR * Math.sin(radStart);
    const x2 = cx + outerR * Math.cos(radEnd);
    const y2 = cy + outerR * Math.sin(radEnd);

    const largeArcFlag = angleDelta > 180 ? 1 : 0;

    let pathData = '';
    if (donut) {
      const ix1 = cx + innerR * Math.cos(radEnd);
      const iy1 = cy + innerR * Math.sin(radEnd);
      const ix2 = cx + innerR * Math.cos(radStart);
      const iy2 = cy + innerR * Math.sin(radStart);

      pathData = `
        M ${x1} ${y1}
        A ${outerR} ${outerR} 0 ${largeArcFlag} 1 ${x2} ${y2}
        L ${ix1} ${iy1}
        A ${innerR} ${innerR} 0 ${largeArcFlag} 0 ${ix2} ${iy2}
        Z
      `;
    } else {
      pathData = `
        M ${cx} ${cy}
        L ${x1} ${y1}
        A ${outerR} ${outerR} 0 ${largeArcFlag} 1 ${x2} ${y2}
        Z
      `;
    }

    const color = stage.colorHex || '#3B82F6';

    return {
      index,
      stage,
      pathData,
      color,
      isActive,
      startAngle,
      endAngle,
      angleDelta
    };
  });

  return (
    <div style={{ position: 'relative', width: size, height: size, display: 'inline-block' }}>
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        style={{ overflow: 'visible', filter: 'drop-shadow(0 4px 10px rgba(0,0,0,0.25))' }}
      >
        <defs>
          <filter id="glow-slice" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

        {slices.map((slice) => {
          const isCurrentActive = slice.isActive;
          return (
            <path
              key={slice.stage.id || slice.index}
              d={slice.pathData}
              fill={slice.color}
              stroke="var(--bg-secondary)"
              strokeWidth={stages.length > 1 ? 2 : 0}
              filter={isCurrentActive ? 'url(#glow-slice)' : undefined}
              opacity={activeStageIndex !== undefined && !isCurrentActive ? 0.45 : 1}
              style={{
                cursor: interactive ? 'pointer' : 'default',
                transition: 'opacity 0.3s ease, transform 0.2s ease',
                transformOrigin: `${cx}px ${cy}px`,
                transform: isCurrentActive ? 'scale(1.035)' : 'scale(1)'
              }}
              onClick={() => onSliceClick && onSliceClick(slice.index)}
            >
              <title>{`${slice.stage.name} - ${Math.round(slice.stage.durationSeconds / 60)} min`}</title>
            </path>
          );
        })}

        {/* Anillo de progreso de la etapa activa sobrepuesta */}
        {donut && activeStageIndex !== undefined && activeStageProgress !== undefined && (
          <circle
            cx={cx}
            cy={cy}
            r={(outerR + innerR) / 2}
            fill="none"
            stroke="rgba(255, 255, 255, 0.2)"
            strokeWidth={outerR - innerR}
            opacity={0.3}
            pointerEvents="none"
          />
        )}
      </svg>

      {/* Contenido en el centro de la rosquilla (Donut Center) */}
      {donut && (
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: innerR * 1.8,
            height: innerR * 1.8,
            borderRadius: '50%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            pointerEvents: 'none'
          }}
        >
          {centerContent}
        </div>
      )}
    </div>
  );
};
