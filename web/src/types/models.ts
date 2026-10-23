export interface Stage {
  id: string;
  name: string;
  durationSeconds: number; // Duración en segundos
  notes?: string;
  isOptional?: boolean;
  colorHex?: string;
  isEnabled?: boolean; // true por defecto; si es false, se omite en la corrida y pastel
}

export interface ActivityItem {
  id: string;
  title: string;
  description: string;
  stages: Stage[];
  isTemplate: boolean;
  category: string;
  tags?: string[];
  createdAt: number;
}

export interface RoutineStep {
  id: string;
  activityId: string;
  activityTitle: string;
  stages: Stage[];
  autoAdvance: boolean; // true = avance automático al finalizar, false = esperar confirmación del usuario
  blockId?: string;
  blockTitle?: string;
  blockRepeatCount?: number;
  iterationIndex?: number;
  totalIterations?: number;
  isCustomized?: boolean; // Indica si las etapas fueron personalizadas para esta rutina
  originalActivityId?: string; // ID original para poder restablecer
}

export interface RoutineBlock {
  id: string;
  title: string;
  repeatCount: number;
  steps: RoutineStep[];
}

export function stepsToBlocks(steps: RoutineStep[]): RoutineBlock[] {
  const blocks: RoutineBlock[] = [];
  let currentBlock: RoutineBlock | null = null;

  for (const step of steps) {
    if (step.blockId) {
      if (currentBlock && currentBlock.id === step.blockId) {
        currentBlock.steps.push(step);
      } else {
        currentBlock = {
          id: step.blockId,
          title: step.blockTitle || 'Bloque',
          repeatCount: Math.max(1, step.blockRepeatCount || 1),
          steps: [step]
        };
        blocks.push(currentBlock);
      }
    } else {
      currentBlock = {
        id: `block_${step.id}`,
        title: '',
        repeatCount: 1,
        steps: [step]
      };
      blocks.push(currentBlock);
    }
  }

  return blocks;
}

export function blocksToSteps(blocks: RoutineBlock[]): RoutineStep[] {
  const steps: RoutineStep[] = [];
  for (const b of blocks) {
    const isRepeatedBlock =
      b.steps.length > 1 || (b.title && b.title.trim().length > 0) || (b.repeatCount && b.repeatCount > 1);
    const bId = isRepeatedBlock ? b.id : undefined;
    const bTitle = isRepeatedBlock ? (b.title.trim() || 'Bloque') : undefined;
    const bRepeat = isRepeatedBlock ? Math.max(1, b.repeatCount || 1) : 1;

    for (const s of b.steps) {
      steps.push({
        ...s,
        blockId: bId,
        blockTitle: bTitle,
        blockRepeatCount: bRepeat
      });
    }
  }
  return steps;
}

export function calculateRoutineTotalSeconds(steps: RoutineStep[]): number {
  const blocks = stepsToBlocks(steps);
  return blocks.reduce((acc, b) => {
    const blockBaseSeconds = b.steps.reduce((sAcc, s) => {
      const activeStages = s.stages.filter((st) => st.isEnabled !== false);
      return sAcc + activeStages.reduce((stAcc, st) => stAcc + st.durationSeconds, 0);
    }, 0);
    return acc + blockBaseSeconds * Math.max(1, b.repeatCount || 1);
  }, 0);
}

export function unrollRoutineSteps(steps: RoutineStep[]): RoutineStep[] {
  const blocks = stepsToBlocks(steps);
  const unrolled: RoutineStep[] = [];

  for (const b of blocks) {
    const repeats = Math.max(1, b.repeatCount || 1);
    for (let r = 0; r < repeats; r++) {
      for (const s of b.steps) {
        // Filtrar etapas que no estén habilitadas
        const activeStages = s.stages.filter((st) => st.isEnabled !== false);
        if (activeStages.length === 0) continue;

        unrolled.push({
          ...s,
          stages: activeStages,
          id: `${s.id}_iter_${r}_${Date.now()}_${Math.random()}`,
          blockId: b.steps.length > 1 || repeats > 1 ? b.id : undefined,
          blockTitle: b.title.trim() ? b.title : (repeats > 1 ? 'Bloque' : undefined),
          blockRepeatCount: repeats,
          iterationIndex: r,
          totalIterations: repeats
        });
      }
    }
  }

  return unrolled;
}

export interface RoutineItem {
  id: string;
  title: string;
  description: string;
  steps: RoutineStep[];
  isTemplate: boolean;
  tags?: string[];
  defaultRepeatCount?: number;
  defaultCycleBreakSeconds?: number;
  createdAt: number;
}

export interface ProjectRoutineItem {
  id: string; // ID único de la instancia dentro del proyecto
  routineId: string; // Referencia a la rutina original
  title: string;
  steps: RoutineStep[]; // Copia snapshot de los pasos/etapas para este proyecto
  isCustomized?: boolean;
}

export interface ProjectItem {
  id: string;
  title: string;
  description: string;
  activityIds: string[];
  routineIds: string[]; // Para compatibilidad
  projectRoutines?: ProjectRoutineItem[]; // Rutinas desacopladas configuradas
  tags?: string[];
  defaultRepeatCount?: number;
  defaultCycleBreakSeconds?: number;
  createdAt: number;
}

export interface FolderItem {
  id: string;
  name: string;
  colorHex: string;
  createdAt: number;
}

export interface StageExecutionRecord {
  stageId: string;
  stageName: string;
  activityName: string;
  plannedSeconds: number;
  actualSeconds: number;
  isSkipped: boolean;
  isCompleted: boolean;
}

export interface SessionSummary {
  id: string;
  title: string;
  contextType: 'ACTIVITY' | 'ROUTINE' | 'PROJECT';
  parentTitle?: string | null;
  totalPlannedSeconds: number;
  totalActualSeconds: number;
  completedStagesCount: number;
  skippedStagesCount: number;
  stageRecords: StageExecutionRecord[];
  completedAt: number;
}

export type TimerStatus = 'IDLE' | 'RUNNING' | 'PAUSED' | 'WAITING_MANUAL_ADVANCE' | 'COMPLETED';

export interface TimerState {
  status: TimerStatus;
  sessionTitle: string;
  contextType: 'ACTIVITY' | 'ROUTINE' | 'PROJECT';
  parentTitle: string | null;
  routineSteps: RoutineStep[];
  currentActivityIndex: number;
  currentStageIndex: number;
  remainingSeconds: number;
  elapsedSecondsInCurrentStage: number;
  totalElapsedSecondsSession: number;
  totalPlannedSecondsSession: number;
  records: StageExecutionRecord[];
  completedSummary: SessionSummary | null;
  isVoiceEnabled: boolean;
  isSoundEnabled: boolean;
  isMateModeEnabled: boolean;
  mateIntervalMinutes: number;
  mateRemainingSeconds: number;
  lastMateAlertTime: number;
  totalCycles: number;
  currentCycle: number;
  cycleBreakSeconds: number;
  isInCycleBreak: boolean;
  cycleBreakRemainingSeconds: number;
}

export interface BackupData {
  version: number;
  exportedAt: number;
  activities: ActivityItem[];
  routines: RoutineItem[];
  projects: ProjectItem[];
  folders: FolderItem[];
  summaries: SessionSummary[];
}
