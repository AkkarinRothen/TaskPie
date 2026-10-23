import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { AudioService } from '../services/audio';
import { NotificationService } from '../services/notifications';
import { SpeechService } from '../services/speech';
import { StorageService } from '../services/storage';
import { PipService } from '../services/pipService';
import { PipFloatingTimer } from '../components/PipFloatingTimer';
import { unrollRoutineSteps } from '../types/models';
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
  startSingleActivity: (activity: ActivityItem, customSessionName?: string, repeatCount?: number, cycleBreakSeconds?: number) => void;
  startRoutine: (routine: RoutineItem, customSessionName?: string, repeatCount?: number, cycleBreakSeconds?: number) => void;
  startProject: (project: ProjectItem, activities: ActivityItem[], routines: RoutineItem[], customSessionName?: string, repeatCount?: number, cycleBreakSeconds?: number) => void;
  pause: () => void;
  resume: () => void;
  skipCurrentStage: () => void;
  advanceManualNextActivity: () => void;
  stopSession: () => void;
  toggleVoice: () => void;
  toggleSound: () => void;
  toggleMateMode: () => void;
  setMateInterval: (minutes: number) => void;
  resetMateCountdown: () => void;
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
  isSoundEnabled: localStorage.getItem('taskpie_sound') !== 'false',
  isMateModeEnabled: localStorage.getItem('taskpie_mate_enabled') === 'true',
  mateIntervalMinutes: parseInt(localStorage.getItem('taskpie_mate_interval') || '8', 10),
  mateRemainingSeconds: parseInt(localStorage.getItem('taskpie_mate_interval') || '8', 10) * 60,
  lastMateAlertTime: 0,
  totalCycles: 1,
  currentCycle: 1,
  cycleBreakSeconds: 0,
  isInCycleBreak: false,
  cycleBreakRemainingSeconds: 0
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

  const MATE_PHRASES = [
    '¡Che, tomate el mate que se enfría!',
    '¡Momento de mate!',
    '¡Cuidado que se te enfría el mate!',
    '¡Cebate otro para seguir!',
    '¡Pausa para un matecito!'
  ];

  const triggerMateAlert = () => {
    const current = stateRef.current;
    if (current.isSoundEnabled) {
      AudioService.playTick();
    }
    if (current.isVoiceEnabled) {
      const phrase = MATE_PHRASES[Math.floor(Math.random() * MATE_PHRASES.length)];
      SpeechService.speak(phrase);
    }
    NotificationService.notify('🧉 ¡Momento de mate!', {
      body: '¡Que no se te enfríe! Tomate un matecito antes de seguir.'
    });
  };

  const handleTick = () => {
    const current = stateRef.current;
    if (current.status !== 'RUNNING') return;

    // Si estamos en descanso entre ciclos
    if (current.isInCycleBreak) {
      const breakRemaining = current.remainingSeconds - 1;
      const totalElapsed = current.totalElapsedSecondsSession + 1;

      if (breakRemaining <= 0) {
        // Fin del descanso entre ciclos! Iniciar siguiente ciclo
        const nextCycle = current.currentCycle + 1;
        const firstStep = current.routineSteps[0];
        const firstStage = firstStep?.stages[0];

        if (firstStage) {
          setState((prev) => ({
            ...prev,
            isInCycleBreak: false,
            currentCycle: nextCycle,
            currentActivityIndex: 0,
            currentStageIndex: 0,
            remainingSeconds: firstStage.durationSeconds,
            elapsedSecondsInCurrentStage: 0,
            totalElapsedSecondsSession: totalElapsed
          }));

          if (current.isSoundEnabled) {
            AudioService.playStageTransition();
          }
          if (current.isVoiceEnabled) {
            SpeechService.speak(`Descanso finalizado. Iniciando Ciclo ${nextCycle} de ${current.totalCycles}. Iniciando: ${firstStage.name}`);
          }
          NotificationService.notify(`Iniciando Ciclo ${nextCycle} de ${current.totalCycles}`, {
            body: `Etapa: ${firstStage.name}`
          });
        } else {
          finishSession();
        }
      } else {
        setState((prev) => ({
          ...prev,
          remainingSeconds: breakRemaining,
          totalElapsedSecondsSession: totalElapsed
        }));

        if (breakRemaining === 10 && current.isVoiceEnabled) {
          SpeechService.speak('10 segundos de descanso restantes');
        }
      }
      return;
    }

    const remaining = current.remainingSeconds - 1;
    const elapsedStage = current.elapsedSecondsInCurrentStage + 1;
    const totalElapsed = current.totalElapsedSecondsSession + 1;

    let newMateRemaining = current.mateRemainingSeconds;
    let newLastMateAlertTime = current.lastMateAlertTime;

    if (current.isMateModeEnabled) {
      newMateRemaining -= 1;
      if (newMateRemaining <= 0) {
        triggerMateAlert();
        newMateRemaining = current.mateIntervalMinutes * 60;
        newLastMateAlertTime = Date.now();
      }
    }

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
          totalElapsedSecondsSession: totalElapsed,
          mateRemainingSeconds: newMateRemaining,
          lastMateAlertTime: newLastMateAlertTime
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
        totalElapsedSecondsSession: totalElapsed,
        mateRemainingSeconds: newMateRemaining,
        lastMateAlertTime: newLastMateAlertTime
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
        // Fin de toda la secuencia de este ciclo!
        const currentCycle = current.currentCycle;
        const totalCycles = current.totalCycles;

        if (currentCycle < totalCycles) {
          const nextCycle = currentCycle + 1;
          if (current.cycleBreakSeconds > 0) {
            // Entrar en descanso entre ciclos
            setState((prev) => ({
              ...prev,
              isInCycleBreak: true,
              remainingSeconds: current.cycleBreakSeconds,
              elapsedSecondsInCurrentStage: 0
            }));

            if (current.isVoiceEnabled) {
              const mins = Math.floor(current.cycleBreakSeconds / 60);
              const secs = current.cycleBreakSeconds % 60;
              const timeStr = mins > 0 ? `${mins} minutos` : `${secs} segundos`;
              setTimeout(() => {
                SpeechService.speak(`Ciclo ${currentCycle} completado. Descanso de ${timeStr}.`);
              }, 600);
            }

            NotificationService.notify(`Ciclo ${currentCycle} completado`, {
              body: `Descanso de ${Math.round(current.cycleBreakSeconds / 60)} min antes del Ciclo ${nextCycle}`
            });
          } else {
            // Reiniciar inmediatamente al primer paso del siguiente ciclo
            const firstStep = current.routineSteps[0];
            const firstStage = firstStep?.stages[0];

            if (firstStage) {
              setState((prev) => ({
                ...prev,
                currentCycle: nextCycle,
                currentActivityIndex: 0,
                currentStageIndex: 0,
                remainingSeconds: firstStage.durationSeconds,
                elapsedSecondsInCurrentStage: 0,
                isInCycleBreak: false
              }));

              if (current.isVoiceEnabled) {
                setTimeout(() => {
                  SpeechService.speak(`Ciclo ${currentCycle} completado. Iniciando Ciclo ${nextCycle} de ${totalCycles}. Iniciando: ${firstStage.name}`);
                }, 600);
              }

              NotificationService.notify(`Iniciando Ciclo ${nextCycle} de ${totalCycles}`, {
                body: `Etapa: ${firstStage.name}`
              });
            } else {
              finishSession();
            }
          }
        } else {
          // Toda la sesión finalizó con éxito
          finishSession();
        }
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
        if (current.totalCycles > 1) {
          SpeechService.speak(`¡Sesión completada! Has terminado todos los ${current.totalCycles} ciclos.`);
        } else {
          SpeechService.speak('Sesión completada');
        }
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
    steps: RoutineStep[],
    repeatCount: number = 1,
    cycleBreakSeconds: number = 0
  ) => {
    const validSteps = steps.filter((s) => s.stages.length > 0);
    if (validSteps.length === 0) return;

    const firstStage = validSteps[0].stages[0];
    const singleCyclePlanned = validSteps.reduce(
      (sum, step) => sum + step.stages.reduce((sSum, stage) => sSum + stage.durationSeconds, 0),
      0
    );
    const totalPlanned = singleCyclePlanned * repeatCount + (cycleBreakSeconds * Math.max(0, repeatCount - 1));

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
      completedSummary: null,
      mateRemainingSeconds: stateRef.current.mateIntervalMinutes * 60,
      lastMateAlertTime: 0,
      totalCycles: Math.max(1, repeatCount),
      currentCycle: 1,
      cycleBreakSeconds,
      isInCycleBreak: false,
      cycleBreakRemainingSeconds: 0
    };

    setState(newState);
    setIsSessionModalOpen(true);
    postWorker('START');

    if (newState.isVoiceEnabled) {
      if (repeatCount > 1) {
        SpeechService.speak(`Iniciando Ciclo 1 de ${repeatCount}. ${firstStage.name}`);
      } else {
        SpeechService.speak(`Iniciando: ${firstStage.name}`);
      }
    }
  };

  const startSingleActivity = (
    activity: ActivityItem,
    customSessionName?: string,
    repeatCount: number = 1,
    cycleBreakSeconds: number = 0
  ) => {
    const activeStages = activity.stages.filter((s) => s.isEnabled !== false);
    if (activeStages.length === 0) return;

    const step: RoutineStep = {
      id: crypto.randomUUID ? crypto.randomUUID() : `step_${Date.now()}`,
      activityId: activity.id,
      activityTitle: activity.title,
      stages: activeStages,
      autoAdvance: true
    };
    startSession(customSessionName?.trim() || activity.title, 'ACTIVITY', null, [step], repeatCount, cycleBreakSeconds);
  };

  const startRoutine = (
    routine: RoutineItem,
    customSessionName?: string,
    repeatCount: number = Math.max(1, routine.defaultRepeatCount || 1),
    cycleBreakSeconds: number = routine.defaultCycleBreakSeconds || 0
  ) => {
    const unrolledSteps = unrollRoutineSteps(routine.steps);
    startSession(customSessionName?.trim() || routine.title, 'ROUTINE', null, unrolledSteps, repeatCount, cycleBreakSeconds);
  };

  const startProject = (
    project: ProjectItem,
    activities: ActivityItem[],
    routines: RoutineItem[],
    customSessionName?: string,
    repeatCount: number = Math.max(1, project.defaultRepeatCount || 1),
    cycleBreakSeconds: number = project.defaultCycleBreakSeconds || 0
  ) => {
    const steps: RoutineStep[] = [];

    // Agregar actividades del proyecto
    project.activityIds.forEach((id) => {
      const act = activities.find((a) => a.id === id);
      if (act) {
        const activeStages = act.stages.filter((s) => s.isEnabled !== false);
        if (activeStages.length > 0) {
          steps.push({
            id: crypto.randomUUID ? crypto.randomUUID() : `p_step_${Date.now()}_${id}`,
            activityId: act.id,
            activityTitle: act.title,
            stages: activeStages,
            autoAdvance: true
          });
        }
      }
    });

    // Si el proyecto tiene projectRoutines (snapshots desacoplados con exclusiones granulares)
    if (project.projectRoutines && project.projectRoutines.length > 0) {
      project.projectRoutines.forEach((pr) => {
        const unrolled = unrollRoutineSteps(pr.steps);
        steps.push(...unrolled);
      });
    } else {
      // Compatibilidad con proyectos legados
      project.routineIds.forEach((id) => {
        const rout = routines.find((r) => r.id === id);
        if (rout) {
          const unrolled = unrollRoutineSteps(rout.steps);
          steps.push(...unrolled);
        }
      });
    }

    startSession(customSessionName?.trim() || project.title, 'PROJECT', project.title, steps, repeatCount, cycleBreakSeconds);
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
        setIsSessionModalOpen(false);
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
        setIsSessionModalOpen(false);
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

  const toggleMateMode = () => {
    const next = !stateRef.current.isMateModeEnabled;
    localStorage.setItem('taskpie_mate_enabled', String(next));
    const interval = stateRef.current.mateIntervalMinutes;
    setState((prev) => ({
      ...prev,
      isMateModeEnabled: next,
      mateRemainingSeconds: next && prev.mateRemainingSeconds <= 0 ? interval * 60 : prev.mateRemainingSeconds
    }));
    if (next && stateRef.current.isVoiceEnabled) {
      SpeechService.speak(`Compañero de mate activado, cada ${interval} minutos`);
    }
  };

  const setMateInterval = (minutes: number) => {
    const clamped = Math.max(1, Math.min(60, minutes));
    localStorage.setItem('taskpie_mate_interval', String(clamped));
    setState((prev) => ({
      ...prev,
      mateIntervalMinutes: clamped,
      mateRemainingSeconds: clamped * 60
    }));
  };

  const resetMateCountdown = () => {
    setState((prev) => ({
      ...prev,
      mateRemainingSeconds: prev.mateIntervalMinutes * 60
    }));
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
        toggleMateMode,
        setMateInterval,
        resetMateCountdown,
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
