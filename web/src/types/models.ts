export interface Stage {
  id: string;
  name: string;
  durationSeconds: number; // Duración en segundos
  notes?: string;
  isOptional?: boolean;
  colorHex?: string;
}

export interface ActivityItem {
  id: string;
  title: string;
  description: string;
  stages: Stage[];
  isTemplate: boolean;
  category: string;
  createdAt: number;
}

export interface RoutineStep {
  id: string;
  activityId: string;
  activityTitle: string;
  stages: Stage[];
  autoAdvance: boolean; // true = avance automático al finalizar, false = esperar confirmación del usuario
}

export interface RoutineItem {
  id: string;
  title: string;
  description: string;
  steps: RoutineStep[];
  isTemplate: boolean;
  createdAt: number;
}

export interface ProjectItem {
  id: string;
  title: string;
  description: string;
  activityIds: string[];
  routineIds: string[];
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
