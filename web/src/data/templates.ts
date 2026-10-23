import type { ActivityItem } from '../types/models';

export interface ActivityTemplate {
  id: string;
  title: string;
  description: string;
  category: string;
  tags: string[];
  totalMinutes: number;
  stages: {
    name: string;
    durationSeconds: number;
    notes?: string;
    colorHex: string;
    isOptional?: boolean;
  }[];
}

export const BUILTIN_ACTIVITY_TEMPLATES: ActivityTemplate[] = [
  {
    id: 'tpl_pomodoro_classic',
    title: 'Pomodoro Clásico',
    description: 'El clásico intervalo de 25 minutos de enfoque con 5 de pausa.',
    category: 'Productividad',
    tags: ['foco', 'pomodoro', 'estudio'],
    totalMinutes: 30,
    stages: [
      {
        name: 'Foco Intenso',
        durationSeconds: 25 * 60,
        notes: 'Sin distracciones ni pestañas abiertas ajenas a la tarea.',
        colorHex: '#6366F1'
      },
      {
        name: 'Pausa Corta',
        durationSeconds: 5 * 60,
        notes: 'Respira, estira el cuerpo y toma agua.',
        colorHex: '#10B981'
      }
    ]
  },
  {
    id: 'tpl_writing_sprint',
    title: 'Sprint de Escritura Profunda',
    description: 'Estructura probada para sesiones de redacción creativa y desbloqueo.',
    category: 'Escritura',
    tags: ['escritura', 'creatividad', 'sprint'],
    totalMinutes: 50,
    stages: [
      {
        name: 'Lluvia de ideas y notas',
        durationSeconds: 5 * 60,
        notes: 'Anota ideas sueltas y objetivos de la escena o texto.',
        colorHex: '#8B5CF6'
      },
      {
        name: 'Estructura rápida',
        durationSeconds: 5 * 60,
        notes: 'Define puntos clave y argumento.',
        colorHex: '#3B82F6'
      },
      {
        name: 'Borrador sin censura (Freewriting)',
        durationSeconds: 30 * 60,
        notes: 'Escribe sin frenar a editar. El flujo es lo principal.',
        colorHex: '#10B981'
      },
      {
        name: 'Revisión y pulido',
        durationSeconds: 10 * 60,
        notes: 'Corrige erratas evidentes y deja notas.',
        colorHex: '#F59E0B',
        isOptional: true
      }
    ]
  },
  {
    id: 'tpl_solo_roleplay',
    title: 'Sesión de Rol en Solitario',
    description: 'Un bloque de una hora para jugar una aventura de rol a tu ritmo, sin necesitar grupo ni agentes.',
    category: 'Juego',
    tags: ['rol', 'juego', 'creatividad', 'solo'],
    totalMinutes: 60,
    stages: [
      {
        name: 'Preparar la partida',
        durationSeconds: 5 * 60,
        notes: 'Elige el juego, prepara dados o materiales y define una intención sencilla para la sesión.',
        colorHex: '#8B5CF6'
      },
      {
        name: 'Jugar la aventura',
        durationSeconds: 45 * 60,
        notes: 'Explora, interpreta y deja que las decisiones de tu personaje muevan la historia.',
        colorHex: '#EC4899'
      },
      {
        name: 'Anotar y cerrar',
        durationSeconds: 10 * 60,
        notes: 'Registra qué pasó, qué queda pendiente y una posible idea para la próxima partida.',
        colorHex: '#F59E0B'
      }
    ]
  },
  {
    id: 'tpl_deep_work_60',
    title: 'Estudio y Trabajo Profundo (60 min)',
    description: 'Sesión integral para asimilar conceptos complejos o programación.',
    category: 'Estudio',
    tags: ['deepwork', 'programacion', 'concentracion'],
    totalMinutes: 60,
    stages: [
      {
        name: 'Calentamiento y planificación',
        durationSeconds: 5 * 60,
        notes: 'Prepara el entorno, silencia notificaciones y fija una meta.',
        colorHex: '#06B6D4'
      },
      {
        name: 'Inmersión y resolución',
        durationSeconds: 45 * 60,
        notes: 'Máxima concentración en el problema central.',
        colorHex: '#6366F1'
      },
      {
        name: 'Síntesis y notas finales',
        durationSeconds: 10 * 60,
        notes: 'Escribe qué aprendiste y qué queda pendiente.',
        colorHex: '#EC4899'
      }
    ]
  },
  {
    id: 'tpl_express_burst',
    title: 'Ráfaga Express de 15 Minutos',
    description: 'Para tareas pequeñas o vencer la procrastinación inmediata.',
    category: 'Productividad',
    tags: ['rapido', 'antiprocrastinacion'],
    totalMinutes: 15,
    stages: [
      {
        name: 'Acción Inmediata',
        durationSeconds: 12 * 60,
        notes: 'Solo haz la primera parte sin juzgar.',
        colorHex: '#F97316'
      },
      {
        name: 'Check y orden',
        durationSeconds: 3 * 60,
        notes: 'Guarda avances y ordena el espacio.',
        colorHex: '#14B8A6'
      }
    ]
  },
  {
    id: 'tpl_active_rest',
    title: 'Pausa Activa y Bienestar',
    description: 'Desconexión de pantallas para relajar vista y articulaciones.',
    category: 'Bienestar',
    tags: ['salud', 'pausa', 'estiramiento'],
    totalMinutes: 10,
    stages: [
      {
        name: 'Desconexión y agua',
        durationSeconds: 3 * 60,
        notes: 'Levántate, mira por la ventana y bebe un vaso de agua.',
        colorHex: '#10B981'
      },
      {
        name: 'Movilidad y estiramiento',
        durationSeconds: 7 * 60,
        notes: 'Cuello, hombros, muñecas y espalda baja.',
        colorHex: '#06B6D4'
      }
    ]
  },
  {
    id: 'tpl_tabata_workout',
    title: 'Circuito Físico / Tabata Express',
    description: 'Rondas de ejercicio con intervalos intensos y descansos breves.',
    category: 'Entrenamiento',
    tags: ['deporte', 'tabata', 'fitness'],
    totalMinutes: 12,
    stages: [
      {
        name: 'Calentamiento articular',
        durationSeconds: 2 * 60,
        notes: 'Movilidad ligera para activar pulsaciones.',
        colorHex: '#F59E0B'
      },
      {
        name: 'Bloque Intensivo 1',
        durationSeconds: 4 * 60,
        notes: 'Alta intensidad controlada.',
        colorHex: '#EF4444'
      },
      {
        name: 'Recuperación activa',
        durationSeconds: 2 * 60,
        notes: 'Camina y respira hondo.',
        colorHex: '#3B82F6'
      },
      {
        name: 'Bloque Intensivo 2',
        durationSeconds: 4 * 60,
        notes: 'Último esfuerzo del circuito.',
        colorHex: '#EF4444'
      }
    ]
  }
];

export function templateToActivity(template: ActivityTemplate): ActivityItem {
  return {
    id: `act_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
    title: template.title,
    description: template.description,
    category: template.category,
    tags: [...template.tags],
    stages: template.stages.map((st, idx) => ({
      id: `st_${Date.now()}_${idx}`,
      name: st.name,
      durationSeconds: st.durationSeconds,
      notes: st.notes || '',
      colorHex: st.colorHex,
      isOptional: st.isOptional || false,
      isEnabled: true
    })),
    isTemplate: false,
    createdAt: Date.now()
  };
}
