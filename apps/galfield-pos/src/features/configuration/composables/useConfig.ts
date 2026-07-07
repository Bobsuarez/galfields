import { reactive, ref, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import type { ConfigSettings, ConfigTab, ColorPreset } from '../../../types'
import { useToast } from '../../../composables/useToast'
import { useAppConfig } from '../../../composables/useAppConfig'
import { DEFAULT_SETTINGS, applyRecord, settingsToRecord } from '../../../utils/settingsMapper'

export const CONFIG_TABS: { id: ConfigTab; label: string }[] = [
  { id: 'empresa',    label: 'Empresa'     },
  { id: 'perifericos', label: 'Periféricos' },
  { id: 'usuarios',  label: 'Usuarios'    },
  { id: 'impuestos', label: 'Impuestos'   },
  { id: 'avanzado',  label: 'Avanzado'    },
  { id: 'seguridad', label: 'Seguridad'   },
  { id: 'sistema',   label: 'Sistema'     },
  { id: 'idioma',    label: 'Idioma'      },
]

export const COLOR_PRESETS: ColorPreset[] = [
  { name: 'Garfield', primary: '#F28D35' },
  { name: 'Rojo',     primary: '#E53935' },
  { name: 'Azul',     primary: '#1976D2' },
  { name: 'Verde',    primary: '#388E3C' },
  { name: 'Morado',   primary: '#7B1FA2' },
  { name: 'Dorado',   primary: '#C9A227' },
]

export function useConfig() {
  const { show }          = useToast()
  const { reloadConfig }  = useAppConfig()

  const activeTab = ref<ConfigTab>('empresa')
  const isLoading = ref(false)
  const isSaving  = ref(false)

  // Local editable copy — isolated from the global singleton so the user can
  // discard unsaved changes without affecting the running app.
  const settings = reactive<ConfigSettings>(JSON.parse(JSON.stringify(DEFAULT_SETTINGS)))

  async function loadSettings(): Promise<void> {
    isLoading.value = true
    try {
      const record = await invoke<Record<string, string>>('get_settings')
      applyRecord(settings, record)
    } catch (e) {
      console.error('[config-form] load failed:', e)
    } finally {
      isLoading.value = false
    }
  }

  function setTab(tab: ConfigTab) {
    activeTab.value = tab
  }

  function applyPreset(preset: ColorPreset) {
    settings.styles.primaryColor = preset.primary
  }

  async function save(): Promise<void> {
    isSaving.value = true
    try {
      await invoke('save_settings', { settings: settingsToRecord(settings) })
      await reloadConfig()   // propagate changes to CSS vars + global reactive state
      show('Configuración guardada con éxito', 'success')
    } catch (e) {
      console.error('[config-form] save failed:', e)
      show('No se pudo guardar la configuración', 'error')
    } finally {
      isSaving.value = false
    }
  }

  onMounted(loadSettings)

  return { activeTab, settings, isLoading, isSaving, setTab, applyPreset, save }
}
