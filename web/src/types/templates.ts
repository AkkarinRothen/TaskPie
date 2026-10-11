import type { ActivityItem, FolderItem, ProjectItem, RoutineItem } from './models';

export const DEFAULT_FOLDERS: FolderItem[] = [
  { id: 'f_anti_inertia', name: 'Anti-Inercia', colorHex: '#EC4899', createdAt: Date.now() },
  { id: 'f_writing', name: 'Escritura', colorHex: '#3B82F6', createdAt: Date.now() },
  { id: 'f_wellness', name: 'Bienestar', colorHex: '#10B981', createdAt: Date.now() },
  { id: 'f_home', name: 'Hogar', colorHex: '#F97316', createdAt: Date.now() },
  { id: 'f_study', name: 'Estudio', colorHex: '#6366F1', createdAt: Date.now() },
  { id: 'f_work', name: 'Trabajo', colorHex: '#8B5CF6', createdAt: Date.now() }
];

export const WRITING_SPRINT: ActivityItem = {
  id: 'tpl_writing_sprint',
  title: 'Sprint de Escritura Profunda',
  description: 'Estructura probada para sesiones de escritura creativa de 50 minutos.',
  category: 'Escritura',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_w1',
      name: 'Lluvia de ideas y notas',
      durationSeconds: 5 * 60,
      notes: 'Anota ideas sueltas, objetivos del capítulo o escena.',
      colorHex: '#6366F1'
    },
    {
      id: 's_w2',
      name: 'Estructura / Outline rápido',
      durationSeconds: 5 * 60,
      notes: 'Define qué personajes intervienen y el giro principal.',
      colorHex: '#3B82F6'
    },
    {
      id: 's_w3',
      name: 'Borrador sin censura (Freewriting)',
      durationSeconds: 30 * 60,
      notes: 'Escribe sin editar ni corregir palabras. El flujo es lo primero.',
      colorHex: '#10B981'
    },
    {
      id: 's_w4',
      name: 'Revisión rápida y pulido',
      durationSeconds: 10 * 60,
      notes: 'Corrige errores obvios y deja notas para la próxima sesión.',
      isOptional: true,
      colorHex: '#F59E0B'
    }
  ]
};

export const RAPID_FREEWRITING: ActivityItem = {
  id: 'tpl_rapid_freewriting',
  title: 'Ráfaga de Escritura Corta',
  description: 'Sesión express de 20 minutos para romper el bloqueo creativo.',
  category: 'Escritura',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_rf1',
      name: 'Calentamiento mental',
      durationSeconds: 3 * 60,
      notes: 'Respira y escribe cualquier pensamiento libre.',
      colorHex: '#8B5CF6'
    },
    {
      id: 's_rf2',
      name: 'Escritura continua sin parar',
      durationSeconds: 15 * 60,
      notes: 'Manos en el teclado / cuaderno sin detenerse.',
      colorHex: '#EC4899'
    },
    {
      id: 's_rf3',
      name: 'Cierre y resumen en una frase',
      durationSeconds: 2 * 60,
      notes: 'Sintetiza lo escrito en una sola idea clave.',
      colorHex: '#06B6D4'
    }
  ]
};

export const REST_ACTIVITY: ActivityItem = {
  id: 'tpl_rest_activity',
  title: 'Descanso Consciente',
  description: 'Pausa activa para despejar la vista y recuperar energía.',
  category: 'Bienestar',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_r1',
      name: 'Desconexión de pantalla y agua',
      durationSeconds: 3 * 60,
      notes: 'Levántate, mira a lo lejos y toma un vaso de agua.',
      colorHex: '#14B8A6'
    },
    {
      id: 's_r2',
      name: 'Estiramientos y respiración',
      durationSeconds: 7 * 60,
      notes: 'Estira cuello, hombros y espalda.',
      colorHex: '#10B981'
    }
  ]
};

export const CLEAN_KITCHEN_ACTIVITY: ActivityItem = {
  id: 'tpl_clean_kitchen',
  title: 'Limpieza de la Cocina',
  description: 'Bloque de tareas del hogar organizado por etapas rápidas.',
  category: 'Hogar',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_k1',
      name: 'Recoger y despejar mesadas',
      durationSeconds: 5 * 60,
      notes: 'Guardar ingredientes y despejar superficies.',
      colorHex: '#F97316'
    },
    {
      id: 's_k2',
      name: 'Lavar platos y utensilios',
      durationSeconds: 10 * 60,
      notes: 'Lavar o cargar lavavajillas.',
      colorHex: '#0EA5E9'
    },
    {
      id: 's_k3',
      name: 'Limpiar superficies y barrer',
      durationSeconds: 5 * 60,
      notes: 'Desinfectar y dejar todo listo.',
      colorHex: '#84CC16'
    }
  ]
};

export const STUDY_SESSION: ActivityItem = {
  id: 'tpl_study_session',
  title: 'Sesión de Estudio Activo',
  description: 'Técnica de lectura, asimilación y repaso espaciado.',
  category: 'Estudio',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_st1',
      name: 'Lectura activa y subrayado',
      durationSeconds: 20 * 60,
      notes: 'Identificar conceptos centrales.',
      colorHex: '#6366F1'
    },
    {
      id: 's_st2',
      name: 'Creación de mapa mental / flashcards',
      durationSeconds: 15 * 60,
      notes: 'Resumir con tus propias palabras.',
      colorHex: '#8B5CF6'
    },
    {
      id: 's_st3',
      name: 'Auto-evaluación rápida',
      durationSeconds: 10 * 60,
      notes: 'Responder preguntas clave de memoria.',
      colorHex: '#EC4899'
    }
  ]
};

export const FOCUS_SESSION_WITH_LANDING: ActivityItem = {
  id: 'tpl_focus_landing_med',
  title: 'Foco con Aterrizaje Protegido',
  description: 'Bloque de 35 min con producción enfocada y cierre asistido para no quedar atrapado.',
  category: 'Anti-Inercia',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_flm1',
      name: 'Producción enfocada',
      durationSeconds: 25 * 60,
      notes: 'Avanza en tu tarea actual sin preocuparte por terminarla por completo.',
      colorHex: '#3B82F6'
    },
    {
      id: 's_flm2',
      name: 'Marcapáginas mental',
      durationSeconds: 5 * 60,
      notes: 'Detén la producción. Escribe una nota con la próxima acción exacta para no perder el hilo.',
      colorHex: '#8B5CF6'
    },
    {
      id: 's_flm3',
      name: 'Traspaso de antorcha',
      durationSeconds: 5 * 60,
      notes: 'Cierra o minimiza este archivo. Abre y deja listo en pantalla el material de la siguiente tarea.',
      colorHex: '#EC4899'
    }
  ]
};

export const DEEP_WORK_WITH_LANDING: ActivityItem = {
  id: 'tpl_deep_work_landing',
  title: 'Trabajo Profundo con Desaceleración',
  description: 'Sesión inmersiva de 60 min con 15 minutos dedicados a un aterrizaje suave y sin ansiedad.',
  category: 'Anti-Inercia',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_dwl1',
      name: 'Inmersión profunda',
      durationSeconds: 45 * 60,
      notes: 'Enfócate en la parte compleja de tu proyecto sin interrupciones.',
      colorHex: '#6366F1'
    },
    {
      id: 's_dwl2',
      name: 'Desaceleración y notas de continuidad',
      durationSeconds: 8 * 60,
      notes: 'Para las manos. Registra cabos sueltos, ideas y decisiones tomadas en una nota de continuidad.',
      colorHex: '#A855F7'
    },
    {
      id: 's_dwl3',
      name: 'Preparación del relevo',
      durationSeconds: 7 * 60,
      notes: 'Guarda cambios, limpia pestañas y deja todo preparado para cambiar de contexto con calma.',
      colorHex: '#F43F5E'
    }
  ]
};

export const UNBLOCK_RELEGATED_TASK: ActivityItem = {
  id: 'tpl_unblock_relegated',
  title: 'Desbloqueo de Tarea Relegada',
  description: 'Estructura de 25 min con micropaso sin presión para vencer la pereza de arranque.',
  category: 'Anti-Inercia',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_unb1',
      name: 'Micropaso sin presión',
      durationSeconds: 5 * 60,
      notes: 'Solo ponte frente a la tarea sin exigirte terminarla. Haz el gesto más pequeño (leer un párrafo, abrir 1 archivo).',
      colorHex: '#14B8A6'
    },
    {
      id: 's_unb2',
      name: 'Tracción continua',
      durationSeconds: 15 * 60,
      notes: 'Aprovecha la inercia positiva ya iniciada. Fluye a tu propio ritmo.',
      colorHex: '#10B981'
    },
    {
      id: 's_unb3',
      name: 'Cierre y victoria del día',
      durationSeconds: 5 * 60,
      notes: 'Felicítate por romper la procrastinación. Deja la tarea ordenada para la próxima vez.',
      colorHex: '#F59E0B'
    }
  ]
};

export const PHYSICAL_RESET_PAUSE: ActivityItem = {
  id: 'tpl_physical_reset',
  title: 'Reseteo Físico y Desconexión',
  description: 'Pausa activa de 7 min para romper la inercia corporal y separar el cerebro de la pantalla.',
  category: 'Anti-Inercia',
  isTemplate: true,
  createdAt: Date.now(),
  stages: [
    {
      id: 's_phy1',
      name: 'Separación de pantalla y agua',
      durationSeconds: 3 * 60,
      notes: 'Levántate inmediatamente de la silla, bebe agua fresca y mira por la ventana.',
      colorHex: '#06B6D4'
    },
    {
      id: 's_phy2',
      name: 'Descarga muscular y respiración',
      durationSeconds: 4 * 60,
      notes: 'Mueve los hombros, estira la espalda y respira profundamente 5 veces.',
      colorHex: '#3B82F6'
    }
  ]
};

export const DEFAULT_ACTIVITIES: ActivityItem[] = [
  WRITING_SPRINT,
  RAPID_FREEWRITING,
  REST_ACTIVITY,
  CLEAN_KITCHEN_ACTIVITY,
  STUDY_SESSION,
  FOCUS_SESSION_WITH_LANDING,
  DEEP_WORK_WITH_LANDING,
  UNBLOCK_RELEGATED_TASK,
  PHYSICAL_RESET_PAUSE
];

export const MASTER_WRITING_ROUTINE: RoutineItem = {
  id: 'tpl_routine_writing_day',
  title: 'Bloque Creativo: Escritura + Descanso + Tareas',
  description: 'Secuencia fluida para escribir sin quemarse y completar pendientes del hogar.',
  isTemplate: true,
  createdAt: Date.now(),
  steps: [
    {
      id: 'rs_1',
      activityId: WRITING_SPRINT.id,
      activityTitle: WRITING_SPRINT.title,
      stages: WRITING_SPRINT.stages,
      autoAdvance: true
    },
    {
      id: 'rs_2',
      activityId: REST_ACTIVITY.id,
      activityTitle: REST_ACTIVITY.title,
      stages: REST_ACTIVITY.stages,
      autoAdvance: false
    },
    {
      id: 'rs_3',
      activityId: CLEAN_KITCHEN_ACTIVITY.id,
      activityTitle: CLEAN_KITCHEN_ACTIVITY.title,
      stages: CLEAN_KITCHEN_ACTIVITY.stages,
      autoAdvance: true
    }
  ]
};

export const ROUTINE_ANTI_INERTIA_MEDIUM: RoutineItem = {
  id: 'tpl_routine_transicion_mediana',
  title: 'Transición Consciente: Bloque Mediano',
  description: 'Foco de 35 min con marcapáginas ➔ Reseteo físico ➔ Desbloqueo suave de tarea relegada.',
  isTemplate: true,
  createdAt: Date.now(),
  steps: [
    {
      id: 'step_aim_1',
      activityId: FOCUS_SESSION_WITH_LANDING.id,
      activityTitle: FOCUS_SESSION_WITH_LANDING.title,
      stages: FOCUS_SESSION_WITH_LANDING.stages,
      autoAdvance: true
    },
    {
      id: 'step_aim_2',
      activityId: PHYSICAL_RESET_PAUSE.id,
      activityTitle: PHYSICAL_RESET_PAUSE.title,
      stages: PHYSICAL_RESET_PAUSE.stages,
      autoAdvance: false
    },
    {
      id: 'step_aim_3',
      activityId: UNBLOCK_RELEGATED_TASK.id,
      activityTitle: UNBLOCK_RELEGATED_TASK.title,
      stages: UNBLOCK_RELEGATED_TASK.stages,
      autoAdvance: true
    }
  ]
};

export const ROUTINE_ANTI_INERTIA_DEEP: RoutineItem = {
  id: 'tpl_routine_transicion_profunda',
  title: 'Transición Consciente: Trabajo Profundo',
  description: 'Sesión inmersiva de 60 min con desaceleración ➔ Reseteo ➔ Avance en tarea relegada.',
  isTemplate: true,
  createdAt: Date.now(),
  steps: [
    {
      id: 'step_aid_1',
      activityId: DEEP_WORK_WITH_LANDING.id,
      activityTitle: DEEP_WORK_WITH_LANDING.title,
      stages: DEEP_WORK_WITH_LANDING.stages,
      autoAdvance: true
    },
    {
      id: 'step_aid_2',
      activityId: PHYSICAL_RESET_PAUSE.id,
      activityTitle: PHYSICAL_RESET_PAUSE.title,
      stages: PHYSICAL_RESET_PAUSE.stages,
      autoAdvance: false
    },
    {
      id: 'step_aid_3',
      activityId: UNBLOCK_RELEGATED_TASK.id,
      activityTitle: UNBLOCK_RELEGATED_TASK.title,
      stages: UNBLOCK_RELEGATED_TASK.stages,
      autoAdvance: true
    }
  ]
};

export const DEFAULT_ROUTINES: RoutineItem[] = [
  MASTER_WRITING_ROUTINE,
  ROUTINE_ANTI_INERTIA_MEDIUM,
  ROUTINE_ANTI_INERTIA_DEEP
];

export const DEFAULT_PROJECTS: ProjectItem[] = [
  {
    id: 'tpl_project_novel',
    title: 'Mi Libro / Proyecto de Escritura',
    description: 'Espacio dedicado para organizar todos los bloques de redacción y revisión.',
    activityIds: [WRITING_SPRINT.id, RAPID_FREEWRITING.id],
    routineIds: [MASTER_WRITING_ROUTINE.id],
    createdAt: Date.now()
  },
  {
    id: 'tpl_project_balanced_day',
    title: 'Día Equilibrado: Sin Tareas Relegadas',
    description: 'Estructura para alternar sesiones de alto foco con el cuidado de tus otras prioridades.',
    activityIds: [
      FOCUS_SESSION_WITH_LANDING.id,
      DEEP_WORK_WITH_LANDING.id,
      UNBLOCK_RELEGATED_TASK.id,
      PHYSICAL_RESET_PAUSE.id
    ],
    routineIds: [ROUTINE_ANTI_INERTIA_MEDIUM.id, ROUTINE_ANTI_INERTIA_DEEP.id],
    createdAt: Date.now()
  }
];
