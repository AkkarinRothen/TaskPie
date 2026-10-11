import React, { useState } from 'react';
import { DataProvider, useData } from './context/DataContext';
import { TimerProvider } from './context/TimerContext';
import { Sidebar } from './components/Sidebar';
import { ActivityCard } from './components/ActivityCard';
import { RoutineCard } from './components/RoutineCard';
import { ProjectCard } from './components/ProjectCard';
import { SessionHistoryView } from './components/SessionHistoryView';
import { LiveTimerDock } from './components/LiveTimerDock';
import { ActiveSessionModal } from './components/ActiveSessionModal';
import { SessionSummaryModal } from './components/SessionSummaryModal';
import { ActivityEditorModal } from './components/ActivityEditorModal';
import { RoutineEditorModal } from './components/RoutineEditorModal';
import { ProjectEditorModal } from './components/ProjectEditorModal';
import { FolderEditorModal } from './components/FolderEditorModal';
import { ShortcutsModal } from './components/ShortcutsModal';
import { Search, Plus } from 'lucide-react';
import type { ActivityItem, ProjectItem, RoutineItem } from './types/models';

const MainDashboard: React.FC = () => {
  const {
    activities,
    routines,
    projects,
    activeTab,
    selectedFolderId,
    folders,
    searchQuery,
    setSearchQuery
  } = useData();

  // Modals state
  const [editingActivity, setEditingActivity] = useState<ActivityItem | null | undefined>(undefined);
  const [editingRoutine, setEditingRoutine] = useState<RoutineItem | null | undefined>(undefined);
  const [editingProject, setEditingProject] = useState<ProjectItem | null | undefined>(undefined);
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false);
  const [isShortcutsModalOpen, setIsShortcutsModalOpen] = useState(false);

  // Filter activities by folder and query
  const selectedFolder = folders.find((f) => f.id === selectedFolderId);
  const filteredActivities = activities.filter((act) => {
    const matchesFolder =
      !selectedFolder || act.category.toLowerCase() === selectedFolder.name.toLowerCase();
    const matchesSearch =
      !searchQuery.trim() ||
      act.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      act.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      act.stages.some((s) => s.name.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesFolder && matchesSearch;
  });

  const filteredRoutines = routines.filter((rout) => {
    return (
      !searchQuery.trim() ||
      rout.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      rout.description.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  const filteredProjects = projects.filter((proj) => {
    return (
      !searchQuery.trim() ||
      proj.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      proj.description.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  return (
    <div className="app-container">
      {/* Responsive Desktop Sidebar */}
      <Sidebar
        onOpenFolderModal={() => setIsFolderModalOpen(true)}
        onOpenShortcutsModal={() => setIsShortcutsModalOpen(true)}
      />

      {/* Main Content Area */}
      <main className="app-main">
        <div className="main-content">
          {/* Top Header & Search Bar */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '20px',
              marginBottom: '32px'
            }}
          >
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <h2 style={{ fontSize: '1.9rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  {activeTab === 'ACTIVITIES'
                    ? selectedFolder
                      ? `Actividades: ${selectedFolder.name}`
                      : 'Todas las Actividades'
                    : activeTab === 'ROUTINES'
                    ? 'Rutinas de Enfoque'
                    : activeTab === 'PROJECTS'
                    ? 'Proyectos Contenedores'
                    : 'Historial de Sesiones'}
                </h2>
              </div>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
                {activeTab === 'ACTIVITIES'
                  ? 'Gestiona tus bloques de tiempo divididos en pasteles visuales interactivos.'
                  : activeTab === 'ROUTINES'
                  ? 'Secuencias programadas para fluir entre producción y descansos sin fatiga.'
                  : activeTab === 'PROJECTS'
                  ? 'Organiza múltiples bloques de trabajo en una sola meta global.'
                  : 'Revisa tu consistencia y etapas ejecutadas con registro de tiempos.'}
              </p>
            </div>

            {/* Actions & Search */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              {activeTab !== 'HISTORY' && (
                <div style={{ position: 'relative', width: '260px' }}>
                  <Search
                    size={16}
                    color="var(--text-muted)"
                    style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
                  />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Buscar..."
                    style={{ paddingLeft: '36px', height: '42px', fontSize: '0.88rem' }}
                  />
                </div>
              )}

              {activeTab === 'ACTIVITIES' && (
                <button
                  onClick={() => setEditingActivity(null)}
                  className="btn btn-primary"
                  style={{ height: '42px', gap: '8px' }}
                >
                  <Plus size={18} />
                  <span>Nueva Actividad</span>
                </button>
              )}

              {activeTab === 'ROUTINES' && (
                <button
                  onClick={() => setEditingRoutine(null)}
                  className="btn btn-primary"
                  style={{ height: '42px', gap: '8px' }}
                >
                  <Plus size={18} />
                  <span>Nueva Rutina</span>
                </button>
              )}

              {activeTab === 'PROJECTS' && (
                <button
                  onClick={() => setEditingProject(null)}
                  className="btn btn-primary"
                  style={{ height: '42px', gap: '8px' }}
                >
                  <Plus size={18} />
                  <span>Nuevo Proyecto</span>
                </button>
              )}
            </div>
          </div>

          {/* Grid Content */}
          {activeTab === 'ACTIVITIES' && (
            <div>
              {filteredActivities.length === 0 ? (
                <div
                  className="glass-panel"
                  style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}
                >
                  No se encontraron actividades con los filtros actuales.
                </div>
              ) : (
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
                    gap: '20px'
                  }}
                >
                  {filteredActivities.map((activity) => (
                    <ActivityCard
                      key={activity.id}
                      activity={activity}
                      onEdit={(act) => setEditingActivity(act)}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'ROUTINES' && (
            <div>
              {filteredRoutines.length === 0 ? (
                <div
                  className="glass-panel"
                  style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}
                >
                  No se encontraron rutinas. Crea tu primera secuencia con el botón superior.
                </div>
              ) : (
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
                    gap: '20px'
                  }}
                >
                  {filteredRoutines.map((routine) => (
                    <RoutineCard
                      key={routine.id}
                      routine={routine}
                      onEdit={(r) => setEditingRoutine(r)}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'PROJECTS' && (
            <div>
              {filteredProjects.length === 0 ? (
                <div
                  className="glass-panel"
                  style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}
                >
                  No se encontraron proyectos. Crea uno nuevo para agrupar tus tareas.
                </div>
              ) : (
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
                    gap: '20px'
                  }}
                >
                  {filteredProjects.map((project) => (
                    <ProjectCard
                      key={project.id}
                      project={project}
                      onEdit={(p) => setEditingProject(p)}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'HISTORY' && <SessionHistoryView />}
        </div>
      </main>

      {/* Floating Live Timer Dock */}
      <LiveTimerDock />

      {/* Active Session Fullscreen Experience */}
      <ActiveSessionModal />

      {/* Celebratory Session Summary Modal */}
      <SessionSummaryModal />

      {/* Modals for Editing */}
      {editingActivity !== undefined && (
        <ActivityEditorModal
          initialActivity={editingActivity}
          onClose={() => setEditingActivity(undefined)}
        />
      )}

      {editingRoutine !== undefined && (
        <RoutineEditorModal
          initialRoutine={editingRoutine}
          onClose={() => setEditingRoutine(undefined)}
        />
      )}

      {editingProject !== undefined && (
        <ProjectEditorModal
          initialProject={editingProject}
          onClose={() => setEditingProject(undefined)}
        />
      )}

      {isFolderModalOpen && (
        <FolderEditorModal onClose={() => setIsFolderModalOpen(false)} />
      )}

      {isShortcutsModalOpen && (
        <ShortcutsModal onClose={() => setIsShortcutsModalOpen(false)} />
      )}
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <DataProvider>
      <TimerProvider>
        <MainDashboard />
      </TimerProvider>
    </DataProvider>
  );
};

export default App;
