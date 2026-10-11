// Web Speech API for Spanish Voice Announcements (VoiceAnnouncementManager parity)

export const SpeechService = {
  isSupported(): boolean {
    return 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window;
  },

  speak(text: string): void {
    if (!this.isSupported()) return;

    try {
      window.speechSynthesis.cancel(); // Detener cualquier locución en curso

      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'es-ES';
      utterance.rate = 1.05; // Mismo ritmo ágil configurado en Android

      // Seleccionar una voz natural en español si está disponible
      const voices = window.speechSynthesis.getVoices();
      const spanishVoice = voices.find(v => v.lang.startsWith('es') || v.lang.includes('Spanish'));
      if (spanishVoice) {
        utterance.voice = spanishVoice;
      }

      window.speechSynthesis.speak(utterance);
    } catch {
      // Manejo silencioso en caso de restricciones de permisos del navegador
    }
  },

  stop(): void {
    if (this.isSupported()) {
      try {
        window.speechSynthesis.cancel();
      } catch {
        // no-op
      }
    }
  }
};
