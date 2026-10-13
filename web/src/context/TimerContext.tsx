import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { AudioService } from '../services/audio';
import { NotificationService } from '../services/notifications';
import { SpeechService } from '../services/speech';
import { StorageService } from '../services/storage';
import { PipService } from '../services/pipService';
import { PipFloatingTimer } from '../components/PipFloatingTimer';
import type {
  ActivityItem,
  ProjectItem,
  RoutineItem,
  RoutineStep,
  SessionSummary,
  Stage,
  StageExecutionRecord,
  TimerState
} from '../types/models';

interface TimerContextValue {
  timerState: TimerState;
  startSingleActivity: (activity: ActivityItem, customSessionName?: string) => void;
  startRoutine: (routine: RoutineItem) => void;
  startProject: (project: ProjectItem, activities: ActivityItem[], routines: RoutineItem[]) => void;
  pause: () => void;
  resume: () => void;
  skipCurrentStage: () => void;
  advanceManualNextActivity: () => void;
  stopSession: () => void;
  toggleVoice: () => void;
  toggleSound: () => void;
  closeCompletedSummary: () => void;
  isSessionModalOpen: boolean;
  setIsSessionModalOpen: (open: boolean) => void;
  isPipActive: boolean;
  isPipSupported: boolean;
  openPip: () => Promise<void>;
  closePip: () => void;
  togglePip: () => void;
}

const initialTimerState: TimerState = {
  status: 'IDLE',
  sessionTitle: '',
  contextType: 'ACTIVITY',
  parentTitle: null,
  routineSteps: [],
  currentActivityIndex: 0,
  currentStageIndex: 0,
  remainingSeconds: 0,
  elapsedSecondsInCurrentStage: 0,
  totalElapsedSecondsSession: 0,
  totalPlannedSecondsSession: 0,
  records: [],
  completedSummary: null,
  isVoiceEnabled: localStorage.getItem('taskpie_voice') !== 'false',
  isSoundEnabled: localStorage.getItem('taskpie_sound') !== 'false'
};

const TimerContext = createContext<TimerContextValue | null>(null);

export const TimerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<TimerState>(initialTimerState);
  const [isSessionModalOpen, setIsSessionModalOpen] = useState(false);
  const [pipWindow, setPipWindow] = useState<Window | null>(null);
  const [isPipActive, setIsPipActive] = useState(false);
  const workerRef = useRef<Worker | null>(null);
  const stateRef = useRef(state);
  stateRef.current = state;

  // Inicializar Web Worker
  useEffect(() => {
    try {
      workerRef.current = new Worker(new URL('../workers/timerWorker.ts', import.meta.url), {
        type: 'module'
      });

      workerRef.current.onmessage = (e) => {
        if (e.data.type === 'TICK') {
          handleTick();
        }
      };
    } catch {
      // Fallback si web workers están restringidos
      const interval = setInterval(() => {
        if (stateRef.current.status === 'RUNNING') {
          handleTick();
        }
      }, 1000);
      return () => clearInterval(interval);
    }

    return () => {
      workerRef.current?.terminate();
    };
  }, []);

  // Notificaciones de permisos al primer inicio
  useEffect(() => {
    NotificationService.requestPermission();
  }, []);

  // Atajos de teclado de escritorio
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Evitar interceptar cuando el usuario escribe en un campo de texto
      const target = e.target as HTMLElement;
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable) {
        return;
      }

      const current = stateRef.current;

      if (e.code === 'Space') {
        e.preventDefault();
        if (current.status === 'RUNNING') {
          pause();
        } else if (current.status === 'PAUSED') {
          resume();
        }
      } else if (e.key === 's' || e.key === 'S') {
        if (current.status === 'RUNNING') {
          e.preventDefault();
          skipCurrentStage();
        }
      } else if (e.key === 'n' || e.key === 'N') {
        if (current.status === 'WAITING_MANUAL_ADVANCE') {
          e.preventDefault();
          advanceManualNextActivity();
        }
      } else if (e.key === 'm' || e.key === 'M') {
        e.preventDefault();
        toggleVoice();
      } else if (e.key === 'p' || e.key === 'P') {
        if (current.status !== 'IDLE') {
          e.preventDefault();
          togglePip();
        }
      } else if (e.key === 'f' || e.key === 'F') {
        if (current.status !== 'IDLE') {
          e.preventDefault();
          setIsSessionModalOpen(!isSessionModalOpen);
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isSessionModalOpen, isPipActive]);

  const postWorker = (cmd: 'START' | 'PAUSE' | 'STOP') => {
    workerRef.current?.postMessage({ command: cmd });
  };

  const handleTick = () => {
    const current = stateRef.current;
    if (current.status !== 'RUNNING') return;

    const remaining = current.remainingSeconds - 1;
    const elapsedStage = current.elapsedSecondsInCurrentStage + 1;
    const totalElapsed = current.totalElapsedSecondsSession + 1;

    const currentStep = current.routineSteps[current.currentActivityIndex];
    const currentStage = currentStep?.stages[current.currentStageIndex];

    if (remaining <= 0) {
      // Etapa terminada
      if (currentStage && currentStep) {
        const record: StageExecutionRecord = {
          stageId: currentStage.id,
          stageName: currentStage.name,
          activityName: currentStep.activityTitle,
          plannedSeconds: currentStage.durationSeconds,
          actualSeconds: elapsedStage,
          isSkipped: false,
          isCompleted: true
        };

        setState((prev) => ({
          ...prev,
          records: [...prev.records, record],
          totalElapsedSecondsSession: totalElapsed
        }));
      }

      if (current.isSoundEnabled) {
        AudioService.playStageTransition();
      }

      advanceToNextStage(false);
    } else {
      // Tick regular
      setState((prev) => ({
        ...prev,
        remainingSeconds: remaining,
        elapsedSecondsInCurrentStage: elapsedStage,
        totalElapsedSecondsSession: totalElapsed
      }));

      // Verificación de anuncios por voz y alertas acústicas
      checkVoiceAnnouncements(currentStage, remaining, current.isVoiceEnabled, current.isSoundEnabled);
    }
  };

  const checkVoiceAnnouncements = (
    stage: Stage | undefined,
    remainingSeconds: number,
    voiceEnabled: boolean,
    soundEnabled: boolean
  ) => {
    if (!stage) return;
    const total = stage.durationSeconds;
    const half = Math.floor(total / 2);

    if (total > 15 && remainingSeconds <= 5 && remainingSeconds >= 1) {
      if (soundEnabled) AudioService.playTick();
      if (voiceEnabled) SpeechService.speak(String(remainingSeconds));
      return;
    }

    if (!voiceEnabled) return;

    if (total > 15 && remainingSeconds === 10) {
      SpeechService.speak('10 segundos');
    } else if (remainingSeconds === 60 && total > 60) {
      SpeechService.speak(half === 60 ? 'Último minuto, mitad del tiempo' : 'Último minuto');
    } else if (remainingSeconds === 300 && total > 300) {
      SpeechService.speak(half === 300 ? 'Quedan 5 minutos, mitad del tiempo' : 'Quedan 5 minutos');
    } else if (total >= 120 && remainingSeconds === half && remainingSeconds !== 60 && remainingSeconds !== 300) {
      SpeechService.speak('Mitad del tiempo');
    }
  };

  const advanceToNextStage = (fromSkip: boolean) => {
    const current = stateRef.current;
    const currentStep = current.routineSteps[current.currentActivityIndex];
    if (!currentStep) return;

    const nextStageIdx = current.currentStageIndex + 1;

    if (nextStageIdx < currentStep.stages.length) {
      // Siguiente etapa en la misma actividad
      const nextStage = currentStep.stages[nextStageIdx];
      setState((prev) => ({
        ...prev,
        currentStageIndex: nextStageIdx,
        remainingSeconds: nextStage.durationSeconds,
        elapsedSecondsInCurrentStage: 0
      }));

      if (current.isVoiceEnabled) {
        setTimeout(
          () => {
            SpeechService.speak(`Iniciando: ${nextStage.name}`);
          },
          fromSkip ? 50 : 600
        );
      }

      NotificationService.notify(`Siguiente etapa: ${nextStage.name}`, {
        body: `${currentStep.activityTitle} (${nextStageIdx + 1}/${currentStep.stages.length})`
      });
    } else {
      // Fin de la actividad actual en la rutina
      const nextActIdx = current.currentActivityIndex + 1;
      if (nextActIdx < current.routineSteps.length) {
        const nextActivity = current.routineSteps[nextActIdx];
        const firstStage = nextActivity.stages[0];

        if (firstStage) {
          if (currentStep.autoAdvance) {
            // Avance automático
            setState((prev) => ({
              ...prev,
              currentActivityIndex: nextActIdx,
              currentStageIndex: 0,
              remainingSeconds: firstStage.durationSeconds,
              elapsedSecondsInCurrentStage: 0
            }));

            if (current.isVoiceEnabled) {
              setTimeout(() => {
                SpeechService.speak(`Iniciando: ${firstStage.name}`);
              }, 600);
            }

            NotificationService.notify(`Iniciando: ${nextActivity.activityTitle}`, {
              body: `Etapa: ${firstStage.name}`
            });
          } else {
            // Esperar confirmación manual del usuario
            postWorker('PAUSE');
            setState((prev) => ({
              ...prev,
              status: 'WAITING_MANUAL_ADVANCE'
            }));

            if (current.isVoiceEnabled) {
              SpeechService.speak(`Pausa completada. Pulsa siguiente para continuar con ${nextActivity.activityTitle}`);
            }

            NotificationService.notify(`Pausa completada`, {
              body: `Pulsa continuar para iniciar: ${nextActivity.activityTitle}`
            });
          }
        } else {
          finishSession();
        }
      } else {
        // Toda la sesión finalizó con éxito
        finishSession();
      }
    }
  };

  const finishSession = () => {
    postWorker('STOP');
    closePip();
    const current = stateRef.current;

    const completedCount = current.records.filter((r) => r.isCompleted && !r.isSkipped).length;
    const skippedCount = current.records.filter((r) => r.isSkipped).length;

    if (current.isSoundEnabled) {
      AudioService.playSessionComplete();
    }
    if (current.isVoiceEnabled) {
      setTimeout(() => {
        SpeechService.speak('Sesión completada');
      }, 700);
    }

    const summary: SessionSummary = {
      id: crypto.randomUUID ? crypto.randomUUID() : `sum_${Date.now()}`,
      title: current.sessionTitle,
      contextType: current.contextType,
      parentTitle: current.parentTitle,
      totalPlannedSeconds: current.totalPlannedSecondsSession,
      totalActualSeconds: current.totalElapsedSecondsSession,
      completedStagesCount: completedCount,
      skippedStagesCount: skippedCount,
      stageRecords: current.records,
      completedAt: Date.now()
    };

    // Guardar en la base de datos persistente IndexedDB
    StorageService.saveSummary(summary);

    setState((prev) => ({
      ...prev,
      status: 'COMPLETED',
      completedSummary: summary
    }));

    NotificationService.notify('¡Sesión completada!', {
      body: `Has finalizado "${current.sessionTitle}" con éxito.`
    });
  };

  const startSession = (
    title: string,
    contextType: 'ACTIVITY' | 'ROUTINE' | 'PROJECT',
    parentTitle: string | null,
    steps: RoutineStep[]
  ) => {
    const validSteps = steps.filter((s) => s.stages.length > 0);
    if (validSteps.length === 0) return;

    const firstStage = validSteps[0].stages[0];
    const totalPlanned = validSteps.reduce(
      (sum, step) => sum + step.stages.reduce((sSum, stage) => sSum + stage.durationSeconds, 0),
      0
    );

    const newState: TimerState = {
      ...stateRef.current,
      status: 'RUNNING',
      sessionTitle: title,
      contextType,
      parentTitle,
      routineSteps: validSteps,
      currentActivityIndex: 0,
      currentStageIndex: 0,
      remainingSeconds: firstStage.durationSeconds,
      elapsedSecondsInCurrentStage: 0,
      totalElapsedSecondsSession: 0,
      totalPlannedSecondsSession: totalPlanned,
      records: [],
      completedSummary: null
    };

    setState(newState);
    setIsSessionModalOpen(true);
    postWorker('START');

    if (newState.isVoiceEnabled) {
      SpeechService.speak(`Iniciando: ${firstStage.name}`);
    }
  };

  const startSingleActivity = (activity: ActivityItem, customSessionName?: string) => {
    const step: RoutineStep = {
      id: crypto.randomUUID ? crypto.randomUUID() : `step_${Date.now()}`,
      activityId: activity.id,
      activityTitle: activity.title,
      stages: activity.stages,
      autoAdvance: true
    };
    startSession(customSessionName?.trim() || activity.title, 'ACTIVITY', null, [step]);
  };

  const startRoutine = (routine: RoutineItem) => {
    startSession(routine.title, 'ROUTINE', null, routine.steps);
  };

  const startProject = (
    project: ProjectItem,
    activities: ActivityItem[],
    routines: RoutineItem[]
  ) => {
    const steps: RoutineStep[] = [];

    // Agregar actividades del proyecto
    project.activityIds.forEach((id) => {
      const act = activities.find((a) => a.id === id);
      if (act) {
        steps.push({
          id: crypto.randomUUID ? crypto.randomUUID() : `p_step_${Date.now()}_${id}`,
          activityId: act.id,
          activityTitle: act.title,
          stages: act.stages,
          autoAdvance: true
        });
      }
    });

    // Agregar rutinas del proyecto
    project.routineIds.forEach((id) => {
      const rout = routines.find((r) => r.id === id);
      if (rout) {
        steps.push(...rout.steps);
      }
    });

    startSession(project.title, 'PROJECT', project.title, steps);
  };

  const pause = () => {
    if (stateRef.current.status === 'RUNNING') {
      postWorker('PAUSE');
      SpeechService.stop();
      setState((prev) => ({ ...prev, status: 'PAUSED' }));
    }
  };

  const resume = () => {
    if (stateRef.current.status === 'PAUSED') {
      postWorker('START');
      setState((prev) => ({ ...prev, status: 'RUNNING' }));
    }
  };

  const skipCurrentStage = () => {
    const current = stateRef.current;
    const currentStep = current.routineSteps[current.currentActivityIndex];
    const currentStage = currentStep?.stages[current.currentStageIndex];
    if (!currentStep || !currentStage) return;

    SpeechService.stop();

    const record: StageExecutionRecord = {
      stageId: currentStage.id,
      stageName: currentStage.name,
      activityName: currentStep.activityTitle,
      plannedSeconds: currentStage.durationSeconds,
      actualSeconds: current.elapsedSecondsInCurrentStage,
      isSkipped: true,
      isCompleted: false
    };

    setState((prev) => ({
      ...prev,
      records: [...prev.records, record]
    }));

    advanceToNextStage(true);
  };

  const advanceManualNextActivity = () => {
    const current = stateRef.current;
    if (current.status !== 'WAITING_MANUAL_ADVANCE') return;

    const nextActIdx = current.currentActivityIndex + 1;
    if (nextActIdx < current.routineSteps.length) {
      const nextActivity = current.routineSteps[nextActIdx];
      const firstStage = nextActivity.stages[0];
      if (firstStage) {
        setState((prev) => ({
          ...prev,
          status: 'RUNNING',
          currentActivityIndex: nextActIdx,
          currentStageIndex: 0,
          remainingSeconds: firstStage.durationSeconds,
          elapsedSecondsInCurrentStage: 0
        }));

        postWorker('START');
        if (current.isVoiceEnabled) {
          SpeechService.speak(`Iniciando: ${firstStage.name}`);
        }
      }
    }
  };

  const openPip = async () => {
    const current = stateRef.current;
    if (current.status === 'IDLE') return;

    if (PipService.isDocumentPipSupported()) {
      const win = await PipService.requestDocumentPip(340, 190, () => {
        setPipWindow(null);
        setIsPipActive(false);
      });
      if (win) {
        setPipWindow(win);
        setIsPipActive(true);
      }
    } else if (PipService.isVideoPipSupported()) {
      const success = await PipService.requestVideoPip(
        () => stateRef.current,
        {
          onPlay: () => resume(),
          onPause: () => pause(),
          onSkip: () => skipCurrentStage(),
          onClose: () => {
            setIsPipActive(false);
          }
        }
      );
      if (success) {
        setIsPipActive(true);
      }
    }
  };

  const closePip = () => {
    PipService.closePip();
    setPipWindow(null);
    setIsPipActive(false);
  };

  const togglePip = () => {
    if (isPipActive || PipService.isPipActive()) {
      closePip();
    } else {
      openPip();
    }
  };

  // Cerrar ventana flotante automáticamente al volver a la pestaña de TaskPie
  useEffect(() => {
    const handleWindowFocus = () => {
      if (PipService.isPipActive()) {
        closePip();
      }
    };

    window.addEventListener('focus', handleWindowFocus);
    return () => window.removeEventListener('focus', handleWindowFocus);
  }, []);

  // Intentar abrir la ventana flotante automáticamente al minimizar la pestaña
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        const current = stateRef.current;
        const isTimerActive =
          current.status === 'RUNNING' ||
          current.status === 'PAUSED' ||
          current.status === 'WAITING_MANUAL_ADVANCE';

        if (isTimerActive && !PipService.isPipActive()) {
          openPip().catch(() => {});
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, []);

  const stopSession = () => {
    postWorker('STOP');
    closePip();
    SpeechService.stop();
    setState((prev) => ({
      ...prev,
      status: 'IDLE',
      records: [],
      routineSteps: []
    }));
    setIsSessionModalOpen(false);
  };

  const toggleVoice = () => {
    const next = !stateRef.current.isVoiceEnabled;
    localStorage.setItem('taskpie_voice', String(next));
    if (!next) SpeechService.stop();
    setState((prev) => ({ ...prev, isVoiceEnabled: next }));
  };

  const toggleSound = () => {
    const next = !stateRef.current.isSoundEnabled;
    localStorage.setItem('taskpie_sound', String(next));
    setState((prev) => ({ ...prev, isSoundEnabled: next }));
  };

  const closeCompletedSummary = () => {
    setState((prev) => ({ ...prev, status: 'IDLE', completedSummary: null }));
    setIsSessionModalOpen(false);
  };

  return (
    <TimerContext.Provider
      value={{
        timerState: state,
        startSingleActivity,
        startRoutine,
        startProject,
        pause,
        resume,
        skipCurrentStage,
        advanceManualNextActivity,
        stopSession,
        toggleVoice,
        toggleSound,
        closeCompletedSummary,
        isSessionModalOpen,
        setIsSessionModalOpen,
        isPipActive,
        isPipSupported: PipService.isPipSupported(),
        openPip,
        closePip,
        togglePip
      }}
    >
      {children}
      {pipWindow &&
        createPortal(
          <PipFloatingTimer
            timerState={state}
            onTogglePlayPause={() => {
              if (state.status === 'RUNNING') pause();
              else resume();
            }}
            onSkipStage={skipCurrentStage}
            onAdvanceManual={advanceManualNextActivity}
            onRestoreApp={() => {
              window.focus();
              setIsSessionModalOpen(true);
              closePip();
            }}
            onStopSession={() => {
              if (confirm('¿Detener y salir de la sesión actual?')) {
                stopSession();
              }
            }}
          />,
          pipWindow.document.body
        )}
    </TimerContext.Provider>
  );
};

export const useTimer = () => {
  const ctx = useContext(TimerContext);
  if (!ctx) throw new Error('useTimer must be used within a TimerProvider');
  return ctx;
};
