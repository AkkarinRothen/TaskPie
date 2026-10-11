import React, { createContext, useContext, useEffect, useState } from 'react';
import { StorageService } from '../services/storage';
import type {
  ActivityItem,
  BackupData,
  FolderItem,
  ProjectItem,
  RoutineItem,
  SessionSummary
} from '../types/models';

type MainTab = 'ACTIVITIES' | 'ROUTINES' | 'PROJECTS' | 'HISTORY';

interface DataContextValue {
  activities: ActivityItem[];
  routines: RoutineItem[];
  projects: ProjectItem[];
  folders: FolderItem[];
  summaries: SessionSummary[];
  selectedFolderId: string | null;
  setSelectedFolderId: (id: string | null) => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  activeTab: MainTab;
  setActiveTab: (tab: MainTab) => void;
  saveActivity: (activity: ActivityItem) => Promise<void>;
  deleteActivity: (id: string) => Promise<void>;
  saveRoutine: (routine: RoutineItem) => Promise<void>;
  deleteRoutine: (id: string) => Promise<void>;
  saveProject: (project: ProjectItem) => Promise<void>;
  deleteProject: (id: string) => Promise<void>;
  saveFolder: (folder: FolderItem) => Promise<void>;
  deleteFolder: (id: string) => Promise<void>;
  deleteSummary: (id: string) => Promise<void>;
  exportBackup: () => Promise<void>;
  importBackup: (file: File) => Promise<{ activities: number; routines: number; projects: number; folders: number }>;
  refreshData: () => Promise<void>;
}

const DataContext = createContext<DataContextValue | null>(null);

export const DataProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [activities, setActivities] = useState<ActivityItem[]>([]);
  const [routines, setRoutines] = useState<RoutineItem[]>([]);
  const [projects, setProjects] = useState<ProjectItem[]>([]);
  const [folders, setFolders] = useState<FolderItem[]>([]);
  const [summaries, setSummaries] = useState<SessionSummary[]>([]);

  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [activeTab, setActiveTab] = useState<MainTab>('ACTIVITIES');

  const refreshData = async () => {
    const [act, rout, proj, fold, sum] = await Promise.all([
      StorageService.getActivities(),
      StorageService.getRoutines(),
      StorageService.getProjects(),
      StorageService.getFolders(),
      StorageService.getSummaries()
    ]);

    setActivities(act);
    setRoutines(rout);
    setProjects(proj);
    setFolders(fold);
    setSummaries(sum.sort((a, b) => b.completedAt - a.completedAt));
  };

  useEffect(() => {
    StorageService.initStorage().then(() => {
      refreshData();
    });
  }, []);

  const saveActivity = async (activity: ActivityItem) => {
    await StorageService.saveActivity(activity);
    await refreshData();
  };

  const deleteActivity = async (id: string) => {
    await StorageService.deleteActivity(id);
    await refreshData();
  };

  const saveRoutine = async (routine: RoutineItem) => {
    await StorageService.saveRoutine(routine);
    await refreshData();
  };

  const deleteRoutine = async (id: string) => {
    await StorageService.deleteRoutine(id);
    await refreshData();
  };

  const saveProject = async (project: ProjectItem) => {
    await StorageService.saveProject(project);
    await refreshData();
  };

  const deleteProject = async (id: string) => {
    await StorageService.deleteProject(id);
    await refreshData();
  };

  const saveFolder = async (folder: FolderItem) => {
    await StorageService.saveFolder(folder);
    await refreshData();
  };

  const deleteFolder = async (id: string) => {
    await StorageService.deleteFolder(id);
    if (selectedFolderId === id) {
      setSelectedFolderId(null);
    }
    await refreshData();
  };

  const deleteSummary = async (id: string) => {
    await StorageService.deleteSummary(id);
    await refreshData();
  };

  const exportBackup = async () => {
    const data = await StorageService.exportBackup();
    const jsonStr = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const dateStr = new Date().toISOString().split('T')[0];
    link.href = url;
    link.download = `taskpie-backup-${dateStr}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const importBackup = async (file: File) => {
    return new Promise<{ activities: number; routines: number; projects: number; folders: number }>(
      (resolve, reject) => {
        const reader = new FileReader();
        reader.onload = async (e) => {
          try {
            const parsed = JSON.parse(e.target?.result as string) as BackupData;
            const res = await StorageService.importBackup(parsed);
            await refreshData();
            resolve(res);
          } catch (err) {
            reject(err);
          }
        };
        reader.onerror = () => reject(reader.error);
        reader.readAsText(file);
      }
    );
  };

  return (
    <DataContext.Provider
      value={{
        activities,
        routines,
        projects,
        folders,
        summaries,
        selectedFolderId,
        setSelectedFolderId,
        searchQuery,
        setSearchQuery,
        activeTab,
        setActiveTab,
        saveActivity,
        deleteActivity,
        saveRoutine,
        deleteRoutine,
        saveProject,
        deleteProject,
        saveFolder,
        deleteFolder,
        deleteSummary,
        exportBackup,
        importBackup,
        refreshData
      }}
    >
      {children}
    </DataContext.Provider>
  );
};

export const useData = () => {
  const ctx = useContext(DataContext);
  if (!ctx) throw new Error('useData must be used within a DataProvider');
  return ctx;
};
