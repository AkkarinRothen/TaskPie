import type { ActivityItem, BackupData, FolderItem, ProjectItem, RoutineItem, SessionSummary } from '../types/models';
import { DEFAULT_ACTIVITIES, DEFAULT_FOLDERS, DEFAULT_PROJECTS, DEFAULT_ROUTINES } from '../types/templates';

const DB_NAME = 'taskpie_db';
const DB_VERSION = 1;

const STORES = {
  ACTIVITIES: 'activities',
  ROUTINES: 'routines',
  PROJECTS: 'projects',
  FOLDERS: 'folders',
  SUMMARIES: 'summaries'
} as const;

let dbInstance: IDBDatabase | null = null;

function openDB(): Promise<IDBDatabase> {
  if (dbInstance) return Promise.resolve(dbInstance);

  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORES.ACTIVITIES)) {
        db.createObjectStore(STORES.ACTIVITIES, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(STORES.ROUTINES)) {
        db.createObjectStore(STORES.ROUTINES, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(STORES.PROJECTS)) {
        db.createObjectStore(STORES.PROJECTS, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(STORES.FOLDERS)) {
        db.createObjectStore(STORES.FOLDERS, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(STORES.SUMMARIES)) {
        db.createObjectStore(STORES.SUMMARIES, { keyPath: 'id' });
      }
    };

    request.onsuccess = () => {
      dbInstance = request.result;
      resolve(dbInstance);
    };

    request.onerror = () => {
      reject(request.error);
    };
  });
}

async function getAllFromStore<T>(storeName: string): Promise<T[]> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly');
    const store = tx.objectStore(storeName);
    const req = store.getAll();
    req.onsuccess = () => resolve(req.result as T[]);
    req.onerror = () => reject(req.error);
  });
}

async function putToStore<T>(storeName: string, item: T): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    const req = store.put(item);
    req.onsuccess = () => resolve();
    req.onerror = () => reject(req.error);
  });
}

async function deleteFromStore(storeName: string, id: string): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    const req = store.delete(id);
    req.onsuccess = () => resolve();
    req.onerror = () => reject(req.error);
  });
}

export const StorageService = {
  async initStorage(): Promise<void> {
    await openDB();

    // Check and seed Folders
    const existingFolders = await getAllFromStore<FolderItem>(STORES.FOLDERS);
    if (existingFolders.length === 0) {
      for (const folder of DEFAULT_FOLDERS) {
        await putToStore(STORES.FOLDERS, folder);
      }
    }

    // Check and seed Activities
    const existingActivities = await getAllFromStore<ActivityItem>(STORES.ACTIVITIES);
    if (existingActivities.length === 0) {
      for (const activity of DEFAULT_ACTIVITIES) {
        await putToStore(STORES.ACTIVITIES, activity);
      }
    }

    // Check and seed Routines
    const existingRoutines = await getAllFromStore<RoutineItem>(STORES.ROUTINES);
    if (existingRoutines.length === 0) {
      for (const routine of DEFAULT_ROUTINES) {
        await putToStore(STORES.ROUTINES, routine);
      }
    }

    // Check and seed Projects
    const existingProjects = await getAllFromStore<ProjectItem>(STORES.PROJECTS);
    if (existingProjects.length === 0) {
      for (const project of DEFAULT_PROJECTS) {
        await putToStore(STORES.PROJECTS, project);
      }
    }
  },

  // Activities
  getActivities(): Promise<ActivityItem[]> {
    return getAllFromStore<ActivityItem>(STORES.ACTIVITIES);
  },
  saveActivity(activity: ActivityItem): Promise<void> {
    return putToStore(STORES.ACTIVITIES, activity);
  },
  deleteActivity(id: string): Promise<void> {
    return deleteFromStore(STORES.ACTIVITIES, id);
  },

  // Routines
  getRoutines(): Promise<RoutineItem[]> {
    return getAllFromStore<RoutineItem>(STORES.ROUTINES);
  },
  saveRoutine(routine: RoutineItem): Promise<void> {
    return putToStore(STORES.ROUTINES, routine);
  },
  deleteRoutine(id: string): Promise<void> {
    return deleteFromStore(STORES.ROUTINES, id);
  },

  // Projects
  getProjects(): Promise<ProjectItem[]> {
    return getAllFromStore<ProjectItem>(STORES.PROJECTS);
  },
  saveProject(project: ProjectItem): Promise<void> {
    return putToStore(STORES.PROJECTS, project);
  },
  deleteProject(id: string): Promise<void> {
    return deleteFromStore(STORES.PROJECTS, id);
  },

  // Folders
  getFolders(): Promise<FolderItem[]> {
    return getAllFromStore<FolderItem>(STORES.FOLDERS);
  },
  saveFolder(folder: FolderItem): Promise<void> {
    return putToStore(STORES.FOLDERS, folder);
  },
  deleteFolder(id: string): Promise<void> {
    return deleteFromStore(STORES.FOLDERS, id);
  },

  // Summaries
  getSummaries(): Promise<SessionSummary[]> {
    return getAllFromStore<SessionSummary>(STORES.SUMMARIES);
  },
  saveSummary(summary: SessionSummary): Promise<void> {
    return putToStore(STORES.SUMMARIES, summary);
  },
  deleteSummary(id: string): Promise<void> {
    return deleteFromStore(STORES.SUMMARIES, id);
  },

  // Backup & Restore (Compatible with Android / TaskPie schemas)
  async exportBackup(): Promise<BackupData> {
    const [activities, routines, projects, folders, summaries] = await Promise.all([
      this.getActivities(),
      this.getRoutines(),
      this.getProjects(),
      this.getFolders(),
      this.getSummaries()
    ]);

    return {
      version: 1,
      exportedAt: Date.now(),
      activities,
      routines,
      projects,
      folders,
      summaries
    };
  },

  async importBackup(data: BackupData): Promise<{ activities: number; routines: number; projects: number; folders: number }> {
    let actCount = 0;
    let routCount = 0;
    let projCount = 0;
    let foldCount = 0;

    if (Array.isArray(data.folders)) {
      for (const folder of data.folders) {
        if (folder.id && folder.name) {
          await this.saveFolder(folder);
          foldCount++;
        }
      }
    }

    if (Array.isArray(data.activities)) {
      for (const act of data.activities) {
        if (act.id && act.title && Array.isArray(act.stages)) {
          await this.saveActivity(act);
          actCount++;
        }
      }
    }

    if (Array.isArray(data.routines)) {
      for (const routine of data.routines) {
        if (routine.id && routine.title) {
          await this.saveRoutine(routine);
          routCount++;
        }
      }
    }

    if (Array.isArray(data.projects)) {
      for (const project of data.projects) {
        if (project.id && project.title) {
          await this.saveProject(project);
          projCount++;
        }
      }
    }

    if (Array.isArray(data.summaries)) {
      for (const summary of data.summaries) {
        if (summary.id && summary.title) {
          await this.saveSummary(summary);
        }
      }
    }

    return { activities: actCount, routines: routCount, projects: projCount, folders: foldCount };
  }
};
